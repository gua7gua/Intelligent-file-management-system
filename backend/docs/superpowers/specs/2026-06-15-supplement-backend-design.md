# 补充后端开发设计（全宗+组织 / 忘记密码 / 日志审计）

- 编写人：周扬（后端开发）
- 编写日期：2026-06-15
- 依据：[doc/补充计划.md](../../../doc/补充计划.md) §3.2、§5.1–5.3
- 涉及接口规格：[doc/接口文档.md](../../../doc/接口文档.md) §3.5、§3.6、§17.1–17.6、§22.6、§23.1、§23.2
- 技术栈：Spring Boot 3.5.3 + Java 17 + MyBatis-Plus 3.5.7 + Sa-Token 1.44 + PostgreSQL + Flyway（BCrypt 来自 spring-security-crypto）

## 0. 总体策略

### 0.1 范围与优先级

补充计划为本轮（周扬）安排 3 块后端任务，按优先级顺序推进：

1. 全宗管理 + 组织维护（P1）→ `feat/fonds-zhou`
2. 忘记密码（公众找回密码，P1）→ `feat/forgot-password-zhou`
3. 日志审计查询/导出（P2）→ `feat/audit-log-zhou`

三块互不依赖，各自从最新 `develop` 切出新分支，完成后各自开 PR 合并回 `develop`（对应补充计划 §4）。P2 日志审计若 06-18 冻结前时间不足，可降级为「仅后端查询端点」（补充计划 §5.3）。

### 0.2 共同约定（来自代码现状核实）

- **分层**：无 Service 接口，均为具体 Service 类；以 [UserController](../../src/main/java/com/archive/controller/UserController.java)、[WarehouseController](../../src/main/java/com/archive/controller/WarehouseController.java) 为模板。
- **统一返回**：`common/R<T>`（`R.ok(data)` / `R.ok()` / `R.fail(ErrorCode, msg)`）。
- **分页（偏移）**：`common/PageRequest`（pageNo/pageSize/sortBy/sortOrder，带 `@Min/@Max`）+ `common/PageResult<T>`（records/pageNo/pageSize/total/hasNext）；`application.yml` 已开 `default-flat-param-object: true`，springdoc 把 PageRequest 扁平成独立 query 参数。
- **业务异常**：Service 抛 `exception/BusinessException`（携带 `ErrorCode`），由 `exception/GlobalExceptionHandler` 统一转 HTTP。Controller 不直接抛异常。
- **鉴权**：Sa-Token；白名单 `/api/public/**`、`/api/auth/login`、`/api/dictionaries/**`（[SaTokenConfig](../../src/main/java/com/archive/config/SaTokenConfig.java)）。细粒度权限用 `common/AuthContext`：`getCurrentUserId()`、`hasRole(RoleCode.X)`、`isAdmin()`、`getUserType()`。无 `@PreAuthorize`。
- **账号类型**：`User.userType` 枚举 `internal` / `public`（`@EnumValue` 映射）；种子已有公众账号 `zhou.public`（id=5）。
- **软删**：实体无 `@TableLogic`，`deleted_at` 手动管理，所有查询 QueryWrapper 必须显式 `isNull("deleted_at")`。
- **审计**：`service/AuditService.log(moduleName, operationType, businessType, businessId, detail)`，自动取 `AuthContext` 当前用户作为 actor，异常被吞（只 `log.error`），不影响主流程。
- **密码**：表字段 `password_hash`，`new BCryptPasswordEncoder()` 编码；默认密码 `"123456"`。
- **测试**：纯 Mockito 单测（无 `@SpringBootTest`），AssertJ + verify 审计写入，中文测试方法名，目录镜像 `src/main/java/com/archive`。

### 0.3 已就绪资产（无需重建）

| 资产 | 位置 | 说明 |
|------|------|------|
| `Fonds` 实体 + `FondsMapper` | entity/Fonds、mapper/FondsMapper | `fondsNo` 唯一（V13 `uk_fonds_fonds_no`）、`organization_id` 外键（V12）、`status`/`deletedAt` |
| `Organization` 实体 + Mapper | entity/Organization | `orgName` 唯一（V13 `uk_organizations_org_name`）、`orgType` 枚举、种子 4 条（V16） |
| `AuditLog` 实体 + Mapper | entity/AuditLog | `detail` JSONB→`Map`（`autoResultMap`），`actorType` CHECK(internal/public/system) |
| `ArchiveAccessLog` 实体 + Mapper | entity/ArchiveAccessLog | `accessType` CHECK(view_metadata/preview/download)、`userType` CHECK(internal/public/anonymous) |
| `AuditService` | service/AuditService | 只写不查 |
| POI `poi-ooxml 5.5.1` | pom.xml | 已存在，统计 20.3 导出已用；日志导出复用，无需新依赖 |

### 0.4 需新增资产

- 阿里云短信 SDK：`com.aliyun:dypnsapi20170525:1.2.3`（Maven Central 已核实最新稳定版，2024-10-24 发布）。
- 公众找回密码相关：Controller / Service / DTO / 配置类 / 限流组件。
- 全宗、组织、日志查询相关：Controller / Service / DTO。
- 通用游标分页：`common/CursorResult<T>` + `common/CursorPage`。

### 0.5 验证与交付

- 每块按 TDD 写 Mockito 单测后实现，单测全绿。
- 运行时验证：每块实现后启动后端，用真实 HTTP 请求验证端点闭环（详见各模块验收点），结果记录到 `backend/docs/` 下的运行验证报告。
- 仅在「运行时验证通过」后才视为该块基本完成。
- **push / 提 PR 前需经我（用户）查看**——本设计文档与后续实施计划落盘后，先完成实现与运行时验证，再等待查看。
- git 提交身份：周扬 `zhouyang <2686148374@qq.com>`（仓库 includeIf 机制已配置，`git config` 已核实）。

## 1. 模块一：全宗管理 + 组织维护（`feat/fonds-zhou`，P1）

对应 §17.1–17.6。补充计划 §5.2 明确：组织**不做独立页面与更新/删除接口**，仅查询 + 新增；全宗有业务关联时仅停用、不物理删除。

### 1.1 端点清单

| 方法 | 路径 | 角色 | 说明 |
|------|------|------|------|
| GET | `/api/admin/fonds` | back_archivist, sys_admin | 分页查询：`status` / `organizationId` / `keyword` + `PageRequest`；返回含归档数量 |
| POST | `/api/admin/fonds` | back_archivist, sys_admin | 新增；`fondsNo` 唯一校验 |
| PUT | `/api/admin/fonds/{fondsId}` | back_archivist, sys_admin | 更新；`fondsNo` 创建后不可改；停用走此接口改 `status` |
| GET | `/api/admin/organizations` | back_archivist, sys_admin | 分页查询：`orgType` / `status` / `keyword` + `PageRequest`；供下拉 |
| POST | `/api/admin/organizations` | sys_admin | 新增；`orgName` 唯一 |

> 不提供 DELETE 端点（不做物理删除）；不做 `PUT /api/admin/organizations/{id}`（组织无更新/删除需求）。

### 1.2 新增文件

- `controller/FondsController.java`
- `controller/OrganizationController.java`
- `service/FondsService.java`
- `service/OrganizationService.java`
- `dto/request/FondsCreateRequest.java`（`fondsNo` @NotBlank、`fondsName` @NotBlank、`organizationId` @NotNull、`description` @Size）
- `dto/request/FondsUpdateRequest.java`（`fondsName`、`organizationId`、`description`、`status`；**不含 `fondsNo`**——创建后不可改）
- `dto/request/FondsQuery.java`（status / organizationId / keyword + 继承 PageRequest）
- `dto/request/OrganizationCreateRequest.java`（`orgName` @NotBlank、`orgType` @NotNull、`contactName`、`contactPhone`）
- `dto/request/OrganizationQuery.java`（orgType / status / keyword + PageRequest）
- `dto/response/FondsResponse.java`、`dto/response/FondsDetailResponse.java`（含 `archiveCount` 归档数量）
- `dto/response/OrganizationResponse.java`
- 测试：`service/FondsServiceTest.java`、`service/OrganizationServiceTest.java`

### 1.3 关键逻辑

- **唯一校验**：新增/更新前 `selectOne` 预检 `fondsNo` / `orgName`（参考 `UserService` 处理 `loginName` 唯一的方式），冲突抛 `BusinessException(ErrorCode.BUSINESS_CONFLICT, "全宗号已存在"/"组织名称已存在")`；不依赖 DB 唯一约束兜底，给出明确错误信息。预检查询同样要加 `isNull("deleted_at")`，避免软删记录误判占用。
- **软删过滤**：所有列表/详情查询 QueryWrapper 显式 `.isNull("deleted_at")`。
- **归档数量**：`FondsResponse.archiveCount` —— 查 `archives` 表按 `fonds_id` 计数。实现时先确认 `ArchiveMapper` 是否已有按 fonds 计数方法；若无，在 `FondsService` 内用一次分组查询补齐（不污染 Archive 模块边界），或经确认后在 `ArchiveMapper` 加一个聚合查询方法（实现时定，倾向前者以保持本模块自洽）。
- **停用语义**：`PUT` 接受 `status` 字段，业务关联校验在 Service 内做（§17.3 校验「有业务数据关联时只能停用」——实现时确认是否查 archives/boxes 关联；若关联数>0 且请求试图改其他关键字段，仅允许改 `status=disabled`）。本课程项目数据量小，采用：更新时若该全宗已有归档/档案盒关联，则忽略对业务字段的修改、仅应用 `status` 变更，并在审计 detail 记录「有业务关联，仅更新状态」。
- **审计**：新增/更新全宗、新增组织后调 `auditService.log("M06", "create_fonds"/"update_fonds"/"create_organization", "fonds"/"organization", id, detail)`。模块号 `M06` 对齐模块说明。
- **权限**：方法首行 `if (!(AuthContext.hasRole(RoleCode.BACK_ARCHIVIST) || AuthContext.isAdmin())) throw new BusinessException(ErrorCode.FORBIDDEN, ...)`；组织新增额外限 `AuthContext.isAdmin()`（§17.5 角色仅 sys_admin）。权限不足统一抛 `FORBIDDEN`。

### 1.4 验收点（运行时验证）

- 全宗号新增重复 → 409/BUSINESS_CONFLICT，提示唯一；
- 全宗号创建后 PUT 不接受修改（DTO 无该字段，且即便传入也忽略）；
- 有归档关联的全宗 PUT 仅能停用；
- 列表筛选 status/organizationId/keyword 生效，软删记录不出现；
- 组织新增重名 → 冲突；下拉查询返回未删除组织；
- 新增/更新写审计日志（查 audit_logs 表或后续日志查询端点验证）。

## 2. 模块二：忘记密码（`feat/forgot-password-zhou`，P1）

对应 §3.5 / §3.6。**仅公众账号可经此接口找回**，内部账号由系统管理员经 §22.6 重置。验证码生成/存储/过期/一次性校验**全部由阿里云「短信认证」（Dypnsapi）托管**，后端不存验证码、不建 `sms_codes` 表、不改 DB 设计（补充计划 §5.1）。

### 2.1 端点清单

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/public/auth/sms-code` | `{phone, scene}`；scene ∈ {`register`, `forgot_password`}；调阿里云 `SendSmsVerifyCode` 发码 |
| POST | `/api/public/auth/reset-password` | `{phone, smsCode, newPassword}`；先 `CheckSmsVerifyCode` 校验码，再重置公众账号密码 |

路径落在 `/api/public/**` 白名单内，匿名可访问（[SaTokenConfig](../../src/main/java/com/archive/config/SaTokenConfig.java)）。

> scene 同时支持 `register` 与 `forgot_password`（§3.5 契约）；本轮仅 `reset-password` 消费验证码（公众注册后端不在补充计划范围，补充计划 §6 风险项）。`sms-code` 对两种 scene 均可发码（阿里云不区分），scene 仅用于限流/日志标签。

### 2.2 短信通道方案

**采用方案 A：抽象 `SmsCodeService` 接口 + 双实现 + 配置切换**

- 接口 `service/SmsCodeService`：
  - `void send(String phone, String scene)` —— 发送验证码
  - `boolean verify(String phone, String code)` —— 校验验证码（一次性，由阿里云侧管理）
- 实现 `service/AliyunSmsCodeService`（`@ConditionalOnProperty(prefix="aliyun.sms-auth", name="enabled", havingValue="true", matchIfMissing=true)`）：
  - 引入 `com.aliyun:dypnsapi20170525:1.2.3`（pom 新增依赖，版本已核实）。
  - `send`：构造 `com.aliyun.dypnsapi20170525.Client`（`Config` 设 endpoint `dypnsapi.aliyuncs.com` + AK/SK），调 `SendSmsVerifyCode`，参数 `PhoneNumber=phone`、`SignName=${aliyun.sms-auth.sign-name}`、`TemplateCode=${aliyun.sms-auth.template-code}`、`TemplateParam={"code":"##code##","min":"5"}`、`CodeType=1`（纯数字）。验证码由阿里云动态生成，后端不接触明文码。
  - `verify`：调 `CheckSmsVerifyCode`，参数 `PhoneNumber=phone`、`VerifyCode=code`，依据返回 `VerifyResult`（Boolean）判定。
  - SDK 调用异常 → 包装为 `BusinessException(ErrorCode.BUSINESS_CONFLICT, "短信服务暂不可用")`，不泄漏 SDK 内部错误。
- 实现 `service/FallbackSmsCodeService`（`@ConditionalOnProperty(prefix="aliyun.sms-auth", name="enabled", havingValue="false")`）：
  - `send`：不真正发码，仅 `log.warn` 输出「[SMS-FALLBACK] phone={} scene={} 固定验证码 123456」。
  - `verify`：比对固定码 `"123456"`（与项目默认密码一致），返回 boolean。
  - 用途：本地/CI 无 AK/SK 时联调；生产配置了 key 自动切阿里云实现。

### 2.3 新增文件

- `controller/PublicAuthController.java`
- `service/SmsCodeService.java`（接口）+ `AliyunSmsCodeService.java` + `FallbackSmsCodeService.java`
- `service/PublicPasswordService.java`（重置主流程：校验码 → 查公众账号 → 加密 → 更新 → 审计）
- `dto/request/SmsCodeRequest.java`（`phone` @NotBlank + 手机号格式校验、`scene` @NotBlank + 枚举校验）
- `dto/request/ResetPasswordRequest.java`（`phone` @NotBlank、`smsCode` @NotBlank、`newPassword` @NotBlank @Size(min=6)）
- `config/AliyunSmsProperties.java`（`@ConfigurationProperties("aliyun.sms-auth")`：accessKeyId、accessKeySecret、endpoint、signName、templateCode、enabled）
- `config/SmsRateLimiter.java`（内存限流 bean）
- pom 依赖：`com.aliyun:dypnsapi20170525:1.2.3`
- `application.yml` 新增 `aliyun.sms-auth` 配置块（按补充计划 §5.1 示例，AK/SK 走环境变量 `ALIYUN_SMS_ACCESS_KEY`/`ALIYUN_SMS_SECRET`，`enabled` 默认 true）
- 测试：`service/PublicPasswordServiceTest`、`service/FallbackSmsCodeServiceTest`、`config/SmsRateLimiterTest`（mock SmsCodeService 接口验证主流程；AliyunSmsCodeService 因依赖外部 SDK，单测只验证参数构造与异常包装，不发真实请求）

### 2.4 reset-password 主流程

1. `smsCodeService.verify(phone, smsCode)` → 失败抛 `BusinessException(ErrorCode.VALIDATION_FAILED, "验证码错误或已失效")`。
2. 按 `phone + userType=public + deletedAt IS NULL` 查用户：
   - 查不到 → `BusinessException(ErrorCode.NOT_FOUND, "账号不存在")`；
   - 命中但 `userType=internal` → `BusinessException(ErrorCode.BUSINESS_CONFLICT, "内部账号请联系管理员重置")`（对应 §3.6 校验，内部账号走 §22.6）；
   - 账号 `status=disabled` → `BusinessException(ErrorCode.FORBIDDEN, "账号已停用")`。
3. `passwordEncoder.encode(newPassword)` → 更新 `password_hash` + `updatedAt`。
4. `auditService.log("M01", "reset_password", "user", userId, Map.of("phone", phone))`（公众模块号 M01 对齐模块说明；actorType 自动为 `public`，因未登录无 session——此处 actorUserId 为 null，actorType 在无登录上下文时 AuditService 已兜底为 `system`，符合预期）。
5. 返回 `R.ok(true)`。

### 2.5 限流（§3.5 校验项）

`config/SmsRateLimiter`：基于 `ConcurrentHashMap<String, Long>` 的滑动窗口 / 冷却：
- 同手机号 + scene 60 秒冷却（防刷）；
- 同 IP（取请求 `X-Forwarded-For` 首段或 `RemoteAddr`）每小时上限 N 次（默认 10）。
- 命中限流抛 `BusinessException(ErrorCode.TOO_MANY_REQUESTS 或 BUSINESS_CONFLICT, "操作过于频繁，请稍后再试")`。

**限制声明**：单实例内存方案，多实例部署需换 Redis 共享存储。本项目暂不引入 Redis（YAGNI），在代码注释与本文档明确该局限。

### 2.6 验收点（运行时验证）

- fallback 模式（`aliyun.sms-auth.enabled=false`）：`sms-code` 返回成功且日志打印固定码；`reset-password` 用固定码 `123456` 重置 `zhou.public` 公众账号成功，DB `password_hash` 变更，可用新密码登录；
- `reset-password` 对内部账号（如 `zhou.admin`）→ 拒绝（提示走管理员重置）；
- 验证码错误 → 重置失败；
- 限流：同手机号 60s 内第二次 `sms-code` → 拒绝；
- 重置成功写 audit_logs（actorType=system，operation=reset_password）。

> 阿里云实网发送（`enabled=true` + 真实 AK/SK + 大陆手机号）为可选验证项：若运行验证环境有可用 AK/SK 与测试手机号，补做一次真实发码/校验；否则以 fallback 路径作为主验证，并在报告中标注阿里云实网路径未验证（环境依赖）。

## 3. 模块三：日志审计查询/导出（`feat/audit-log-zhou`，P2）

对应 §23.1 / §23.2 + 补充计划 §5.3 要求的「导出」。**只读、不可删改**（M15 L681）：不提供 POST/PUT/DELETE。

### 3.1 端点清单

| 方法 | 路径 | 角色 | 说明 |
|------|------|------|------|
| GET | `/api/admin/audit-logs` | sys_admin | 游标分页查询（§23.1 全部筛选字段） |
| GET | `/api/admin/audit-logs/export` | sys_admin | 同筛选条件导出 xlsx |
| GET | `/api/admin/archive-access-logs` | back_archivist, sys_admin | 游标分页查询（§23.2） |
| GET | `/api/admin/archive-access-logs/export` | back_archivist, sys_admin | 导出 xlsx |

> 导出端点是 §23 的合理扩展（§3.2 验收点 + 前端计划均要求「导出」，但 §23 接口文档未定义导出端点）。契约在本设计文档定义，**建议后续补充到 `doc/接口文档.md` §23**（该文档由项目经理方江苏维护，本轮不擅改，保持外科手术式范围）。

### 3.2 游标分页方案

**采用方案 A：通用游标分页组件**（你已选按 §23 契约实现）。

- `common/CursorPage`：请求侧，`limit`（@Min(1) @Max(100)，默认 20）+ `cursor`（String，可空，首页为空）。
- `common/CursorResult<T>`：响应侧，`records` / `nextCursor` / `hasNext`，对齐 §23.1 返回结构。
- 游标编码：`nextCursor = Base64( URLSafeEncode( "<sortValue>::<id>" ) )`。审计日志按 `operated_at DESC, id DESC` 排序，游标内含 `(operated_at, id)`；访问日志按 `accessed_at DESC, id DESC`，游标含 `(accessed_at, id)`。
- 查询：`WHERE (cursor 非空时) (operated_at, id) < (cursorValue) ORDER BY operated_at DESC, id DESC LIMIT limit+1`；取到 `limit+1` 条则 `hasNext=true`，`nextCursor` 由第 `limit` 条构造；否则 `hasNext=false`、`nextCursor=null`。
  - 复合比较用 MyBatis-Plus `QueryWrapper` 的 `(operated_at < ? OR (operated_at = ? AND id < ?))` 表达式拼接，或 Mapper 自定义 SQL（实现时择优，倾向 QueryWrapper 保持一致）。
- 筛选字段与游标解耦：筛选条件进 WHERE，游标仅负责「上一页最后一条之后」。

### 3.3 新增文件

- `controller/AuditLogController.java`
- `controller/ArchiveAccessLogController.java`
- `service/AuditLogQueryService.java`
- `service/ArchiveAccessLogQueryService.java`
- `common/CursorPage.java`
- `common/CursorResult.java`
- `dto/request/AuditLogQuery.java`（actorUserId / actorType / moduleName / operationType / businessType / businessId / startedAt / endedAt + CursorPage）
- `dto/request/ArchiveAccessLogQuery.java`（userId / archiveId / accessType / startedAt / endedAt + CursorPage）
- `dto/response/AuditLogResponse.java`、`dto/response/ArchiveAccessLogResponse.java`
- 导出：复用 POI `poi-ooxml 5.5.1`，Controller 直接写 `HttpServletResponse` 流式输出 xlsx（参考现有统计 20.3 导出实现风格，实现时对齐）。
- 测试：`service/AuditLogQueryServiceTest`、`service/ArchiveAccessLogQueryServiceTest`、`common/CursorResultTest`（游标编码/解码、hasNext 判定、筛选拼接）。

### 3.4 关键逻辑

- **筛选**：
  - 审计日志：`actorUserId`(eq) / `actorType`(eq, 校验枚举 internal/public/system) / `moduleName`(eq) / `operationType`(eq) / `businessType`(eq) / `businessId`(eq) / `startedAt`(operated_at >=) / `endedAt`(operated_at <=)，全部可选。
  - 访问日志：`userId`(eq) / `archiveId`(eq) / `accessType`(eq, 枚举) / `startedAt` / `endedAt`。
- **`detail` 字段**：`AuditLog.detail` 是 JSONB→`Map<String,Object>`，实体已 `autoResultMap=true`，MP 自动反序列化；`AuditLogResponse` 直接保留为 `Map`，导出 xlsx 时序列化为 JSON 字符串列。
- **导出**：与查询共用筛选条件，不限 limit（或设较大上限如 10000），POI 流式写 xlsx，列：审计日志（id/operatedAt/actorType/actorUserId/moduleName/operationType/businessType/businessId/detail/ipAddress）、访问日志（id/accessedAt/userType/userId/archiveId/archiveFileId/accessType/ipAddress）。响应头 `Content-Disposition: attachment; filename=audit-logs.xlsx`。
- **权限**：审计日志查询/导出限 `AuthContext.isAdmin()`（§23.1 角色 sys_admin）；访问日志查询/导出 `isAdmin() || hasRole(BACK_ARCHIVIST)`（§23.2）。
- **只读**：Controller 仅 GET；Service 无写方法。

### 3.5 验收点（运行时验证）

- 游标分页：插入若干测试日志后，首页 `cursor=null` 返回 limit 条 + `hasNext=true` + `nextCursor`；带 `nextCursor` 翻页连续，无重复无遗漏；末页 `hasNext=false`。
- 筛选：按 actorType / operationType / 时间范围筛选结果正确。
- 导出：下载 xlsx，行数与筛选结果一致，`detail` 列为 JSON 文本。
- 权限：非 sys_admin 访问 `/api/admin/audit-logs` → 403；back_archivist 可访问 `/api/admin/archive-access-logs`。
- 不可删改：无 DELETE/PUT 端点（Swagger 不出现）。

### 3.6 降级（补充计划 §5.3）

若 06-18 冻结前时间不足，本模块降级为「仅查询端点（审计日志 + 访问日志游标分页），导出延后」；查询与游标分页为最小交付，导出标注 TODO。

## 4. 横切：测试、运行时验证、文档、风险

### 4.1 测试策略

- 纯 Mockito 单测（无 `@SpringBootTest`），镜像 `src/main/java/com/archive` 目录结构到 `src/test/java/com/archive`。
- AssertJ 断言 + `verify(auditService).log(...)` 验证审计写入，中文测试方法名。
- 每块核心逻辑必有单测：
  - 全宗/组织：唯一冲突、软删过滤、停用语义、权限拒绝、审计写入。
  - 忘记密码：验证码校验通过/失败、公众账号重置、内部账号拒绝、停用账号拒绝、限流命中。
  - 日志：游标编码/解码、hasNext 判定、筛选拼接、权限。
- 外部依赖（阿里云 SDK）单测只验证参数构造与异常包装，不发真实请求；fallback 实现做完整逻辑测试。

### 4.2 运行时验证方式

- 启动依赖：PostgreSQL 17（项目 Docker Compose 已配，端口 5432）；MinIO、ClamAV 与本批任务无关，可不强依赖。
- 启动后端：`mvn spring-boot:run`（dev profile），端口 8080。
- 验证手段：`curl` 或 REST 客户端发起真实 HTTP 请求，覆盖各模块验收点；DB 变更用 SQL 核对（`password_hash`、`audit_logs`）。
- 忘记密码：默认 fallback 模式验证；阿里云实网为可选（依赖 AK/SK 与测试手机号，无则标注未验证）。
- 验证结果记录：`backend/docs/` 下生成运行时验证报告（每模块一份或合并一份），含命令、请求/响应、DB 核对截图或文本。

### 4.3 文档放置

- 本设计文档：`backend/docs/superpowers/specs/2026-06-15-supplement-backend-design.md`
- 实施计划：`backend/docs/superpowers/plans/`（writing-plans 阶段产出，分段写入）
- 运行时验证报告：`backend/docs/`（交付物性质，非临时分析）
- 不向项目根目录投放临时文件；不擅改共享的 `doc/接口文档.md`（导出端点契约仅在本设计文档定义并建议补充）。

### 4.4 风险与降级

| 风险 | 影响 | 应对 |
|------|------|------|
| 阿里云 SDK 引入带来传递依赖冲突 | 编译/启动失败 | 优先用 `-shaded` 变体或显式排除冲突依赖；实施时按编译结果定 |
| 阿里云实网无法验证（无 AK/SK/测试号） | 真实发码路径未验证 | fallback 固定码路径作为主验证；实网路径标注未验证（环境依赖），不谎报已验证 |
| 限流为单实例内存方案 | 多实例下限流失效 | 代码与文档明确局限；本项目单实例可用，不引 Redis |
| 游标分页为项目首个游标实现 | 与项目偏移分页风格不一致 | 通用组件 `CursorResult/CursorPage` 可复用；仅日志模块按 §23 契约用游标，其余模块不变 |
| 日志模块时间不足（P2） | 导出或查询未完成 | 降级为仅查询端点（§3.6），导出延后 |

### 4.5 自主决策的小实现假设（影响有限，如不认可请指出）

1. fallback 固定验证码 = `123456`（与项目默认密码一致）。
2. 限流：单实例内存；同手机号+scene 60s 冷却；同 IP 每小时 10 次。
3. 全宗归档数量：实现时确认 `ArchiveMapper` 是否有按 fonds 计数方法，无则在 `FondsService` 内自洽补查询，避免污染 Archive 模块边界。
4. 日志导出格式 xlsx（复用 POI）；导出行数上限 10000。
5. 全宗「有业务关联仅停用」：数据量小，采用「关联数>0 时 PUT 仅应用 status 变更，审计记录该事实」，不做复杂字段级 diff。
6. 审计模块号：全宗/组织用 `M06`、忘记密码用 `M01`（对齐模块说明），实现时若模块说明编号不同则以其为准。

---

## 附：分支与提交计划

| 模块 | 分支 | 顺序 | PR |
|------|------|------|----|
| 全宗+组织 | `feat/fonds-zhou` | 1 | 实现与运行时验证通过后，等用户查看再 push/提 PR |
| 忘记密码 | `feat/forgot-password-zhou` | 2 | 同上 |
| 日志审计 | `feat/audit-log-zhou` | 3 | 同上 |

每条分支从最新 `develop` 切出，本地用周扬身份提交；push 与 PR 待用户查看后进行。




