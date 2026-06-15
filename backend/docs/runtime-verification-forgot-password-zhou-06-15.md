# 模块二运行时验证报告：公众认证（注册 + 忘记密码）（feat/forgot-password-zhou）

- 验证人：周扬
- 验证日期：2026-06-15（阿里云实网补充验证 2026-06-16）
- 分支：`feat/forgot-password-zhou`
- 环境：PostgreSQL 17（archive-postgres:5432）+ 后端 dev profile（8080）；fallback 与阿里云实网两条路径均已验证。

## 验证范围

§3.4 公众注册、§3.5 发送验证码、§3.6 公众找回密码：`POST /api/public/auth/{register,sms-code,reset-password}`。fallback（固定码 123456）与阿里云「短信认证」实网两条路径均完成运行时验证。

## 启动与单测

- `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` 启动成功，FallbackSmsCodeService 被选中（enabled=false）。
- 新增单测：`FallbackSmsCodeServiceTest`(1)、`SmsRateLimiterTest`(4)、`PublicPasswordServiceTest`(4)、`PublicRegistrationServiceTest`(3) 共 12 项全绿。

## 验证结果（真实 HTTP）

| # | 场景 | 期望 | 实际 | 结论 |
|---|------|------|------|------|
| 1 | POST sms-code（公众 13800000005） | 200 + fallback:true | code=OK，fallback=True，msg 含「fallback 固定码 123456」 | ✅ |
| 2 | 60s 内第二次 sms-code | 429 TOO_MANY_REQUESTS | TOO_MANY_REQUESTS 操作过于频繁 | ✅ 限流 |
| 3 | reset-password（固定码 123456） | 200 data=true | code=OK data=True | ✅ |
| 4 | 重置后 password_hash 变更 | 与原值不同 | 原 `$2b$10$KWYt...` → `$2a$10$jGKs...` | ✅ |
| 4b | bcrypt 核对新密码 | newPass123 匹配 | `bcrypt.checkpw(newPass123, hash)=True` | ✅ 强校验 |
| 5 | 内部账号(admin 13800000007) 重置 | 409 内部账号 | BUSINESS_CONFLICT 内部账号请联系系统管理员重置 | ✅ |
| 6 | 错误验证码 000000 | 422 VALIDATION_FAILED | VALIDATION_FAILED 验证码错误或已失效 | ✅ |

## 审计日志（audit_logs，module=M01）

- `actor_type=system`（未登录上下文，AuditService 兜底）、`operation_type=reset_password`、`business_type=user`、`business_id=5`、`detail={"phone":"13800000005"}` ✅

## 数据清理

`zhou.public` 密码已恢复为原始 123456 哈希。

## SDK 接入核对

`com.aliyun:dypnsapi20170525:1.2.3` 依赖解析正常，编译通过。对照真实 jar 反编译修正两处 API：
- `SendSmsVerifyCodeRequest.setCodeType` 入参为 `Long`（用 `1L`，非 int）；
- 校验结果位于 `body.getModel().getVerifyResult()`（String），非顶层 `getVerifyResult()`；采用防御性解析（`"true"`/`"pass"` 视为通过）。

## 阿里云实网补充验证（2026-06-16，真实手机号 18877600249）

环境变量 `ALIYUN_SMS_ACCESS_KEY`/`ALIYUN_SMS_SECRET` 已配置，`ALIYUN_SMS_ENABLED=true` 激活 `AliyunSmsCodeService`（响应 `fallback:false` 证实走实网）。

| # | 场景 | 结果 |
|---|------|------|
| L1 | `sms-code` scene=forgot_password 发码 | 阿里云返回 code=OK/message=OK，手机实收验证码 1357 |
| L2 | 用 1357 调 reset-password | `NOT_FOUND 账号不存在`（非"验证码错误"）→ 证明 `CheckSmsVerifyCode` 通过、`verifyResult` 防御性解析生效 |
| L3 | `sms-code` scene=register 发码 | 阿里云 code=OK，手机实收 2684 → 注册与找回**同一通道** |
| L4 | register 用 2684 注册 | `OK` 返回 id=9/userType=public/status=active；DB 核对：user_type=public、绑定 public_user 角色、password_hash bcrypt($2a$10$)、审计 M01/register |
| L5 | 新注册账号登录 portal=public | `OK` + token + roles=['public_user']（修复 AuthService 空指针后） |

**结论：阿里云「短信认证」发码(SendSmsVerifyCode)+校验(CheckSmsVerifyCode)端到端可用；之前标注的"未验证"项已补齐。**

### 附带修复：AuthService 公众登录空指针（既有 bug，非本模块引入）

实网注册验证时发现 `POST /api/auth/login`(portal=public) 抛 NPE：Sa-Token `SaSession` 内部为 `ConcurrentHashMap`（不允许 null 值），公众账号 `organization_id=null` → `Session.set("organizationId", null)` 触发 `ConcurrentHashMap.put` NPE（`AuthService.login:60`）。种子账号 `zhou.public` 同样无法登录，确认为既有 bug。

修复：对 `maxSecurityLevel`/`organizationId` 做 null 守卫（null → 0/0L）。该修复直接使注册功能可端到端使用，单测全绿无回归（见下）。提交 `fix(auth): 修复公众账号登录空指针异常`。

## 结论

- fallback 与阿里云实网两条路径运行时验证全部通过，端点行为符合 §3.4/§3.5/§3.6 与设计。
- 公众注册端到端可用（真实码注册 → 建号 → 绑角色 → 登录拿 token）。
- AuthService 公众登录既有 NPE 已附带修复（null 守卫），无回归。
- **无未验证项**（阿里云实网已补齐）。

## 已知无关问题（非本模块）

- `ArchiveApplicationTests.contextLoads`：默认 profile 无 datasource 导致，dev profile 下正常。
- ~~公众门户 `POST /api/auth/login`(portal=public) INTERNAL_ERROR~~：**已在本次附带修复**（AuthService null 守卫，见上"附带修复"段），公众账号现可正常登录。
