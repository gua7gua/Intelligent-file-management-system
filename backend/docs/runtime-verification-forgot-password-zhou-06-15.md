# 模块二运行时验证报告：忘记密码（feat/forgot-password-zhou）

- 验证人：周扬
- 验证日期：2026-06-15
- 分支：`feat/forgot-password-zhou`
- 环境：PostgreSQL 17（archive-postgres:5432）+ 后端 dev profile（8080，`aliyun.sms-auth.enabled` 默认 false → FallbackSmsCodeService 生效）

## 验证范围

§3.5 / §3.6：`POST /api/public/auth/sms-code`、`POST /api/public/auth/reset-password`。fallback 模式（固定码 123456）为主验证路径；阿里云实网因无凭据未验证。

## 启动与单测

- `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` 启动成功，FallbackSmsCodeService 被选中（enabled=false）。
- 新增单测：`FallbackSmsCodeServiceTest`(1)、`SmsRateLimiterTest`(4)、`PublicPasswordServiceTest`(4) 共 9 项全绿。

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

## 结论与未验证项

- fallback 路径运行时验证全部通过，端点行为符合 §3.5/§3.6 与设计。
- **阿里云实网路径（enabled=true + 真实 AK/SK + 大陆手机号）未验证**：运行环境无 AK/SK 与测试手机号。代码已实现并编译通过，verifyResult 字符串取值采用防御性解析，接入真实凭据后建议补一次实网发码/校验以确认取值。

## 已知无关问题（非本模块）

- 公众门户 `POST /api/auth/login`（portal=public）返回 INTERNAL_ERROR：AuthService 公众门户登录既有问题，与本模块无关；本模块走 `/api/public/auth/**`，重置后用 bcrypt 直接核对验证密码变更，不依赖该登录路径。
