# 后端脚手架设计规格 — feat/base-zhou

> 日期：2026-06-11
> 负责人：周扬
> 分支：feat/base-zhou
> 任务周期：06-10 至 06-11
> 验收标准：Swagger 可访问，前端可按接口样例开发

## 1. 项目初始化

### 1.1 技术参数

| 项目 | 值 |
|------|------|
| Spring Boot | 3.x（最新稳定版） |
| Java | 17 |
| 构建工具 | Maven Wrapper（随项目提交，组员无需全局安装 Maven） |
| groupId | `com.archive` |
| artifactId | `archive-backend` |
| 基础包 | `com.archive` |

### 1.2 核心依赖

| 依赖 | 版本要求 | 用途 |
|------|----------|------|
| spring-boot-starter-web | Spring Boot 管理版本 | REST API |
| mybatis-plus-spring-boot3-starter | 3.5.x | ORM |
| sa-token-spring-boot3-starter | 1.40+ | 认证授权 |
| flyway-core + flyway-database-postgresql | 10+ | 数据库迁移 |
| postgresql driver | Spring Boot 管理版本 | 数据库连接 |
| springdoc-openapi-starter-webmvc-ui | 2.x | OpenAPI / Swagger UI |
| spring-boot-starter-validation | Spring Boot 管理版本 | 参数校验 |
| lombok | Spring Boot 管理版本 | 减少样板代码 |
| jackson-datatype-jsr310 | Spring Boot 管理版本 | 日期时间序列化 |

### 1.3 配置文件

- `application.yml` — 通用配置（server port、mybatis-plus、flyway baseline、jackson）
- `application-dev.yml` — 开发环境（从环境变量读取 DB/MinIO/ClamAV 连接信息，提供默认值）
- `.env.example` 已存在于 `doc/pre-environment/`，不重复创建

### 1.4 Maven Wrapper

项目根目录 `backend/` 下生成 `mvnw`、`mvnw.cmd`、`.mvn/wrapper/`，提交到 Git。

## 2. 包结构

```
com.archive/
├── ArchiveApplication.java              # 启动类
├── config/
│   ├── SaTokenConfig.java               # Sa-Token 拦截器、路由排除与权限校验
│   ├── CorsConfig.java                  # 跨域配置
│   ├── MyBatisPlusConfig.java           # 分页插件、自动填充 handler
│   └── JacksonConfig.java               # 日期格式、Long 转 String 防精度丢失
├── controller/
│   ├── AuthController.java              # POST /api/auth/login, logout; GET /api/auth/me
│   ├── UserController.java              # 用户管理 CRUD（管理员）
│   └── DictionaryController.java        # GET /api/dictionaries, /api/dictionaries/{dictCode}
├── service/
│   ├── AuthService.java                 # 登录/登出/获取当前用户
│   ├── UserService.java                 # 用户增删改查
│   └── DictionaryService.java           # 字典查询
├── mapper/
│   ├── UserMapper.java
│   ├── RoleMapper.java
│   ├── OrganizationMapper.java
│   ├── FondsMapper.java
│   └── CategoryMapper.java
├── entity/                              # 本阶段涉及的 6 张核心表
│   ├── User.java
│   ├── Role.java
│   ├── UserRole.java
│   ├── Organization.java
│   ├── Fonds.java
│   └── Category.java
├── dto/
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── UserCreateRequest.java
│   │   └── UserUpdateRequest.java
│   └── response/
│       ├── LoginResponse.java
│       ├── UserInfoResponse.java
│       └── DictionaryResponse.java
├── common/
│   ├── R.java                           # 统一响应 R<T>（code/message/data/traceId）
│   ├── PageResult.java                  # 分页响应（records/pageNo/pageSize/total/hasNext）
│   ├── PageRequest.java                 # 分页请求基类（pageNo/pageSize/sortBy/sortOrder）
│   ├── ErrorCode.java                   # 错误码枚举
│   └── AuthContext.java                 # 当前用户上下文（静态工具类）
├── enums/
│   ├── UserType.java                    # internal, public
│   ├── RoleCode.java                    # front_archivist, back_archivist, transfer_user, ...
│   ├── UserStatus.java                  # active, disabled
│   ├── SecurityLevel.java               # 0-4 非密到绝密
│   ├── DataScope.java                   # own_org, own_fonds, all
│   └── OrgType.java                     # archive_org, government, enterprise, public_institution
└── exception/
    └── BusinessException.java           # 业务异常基类（code + message）
```

## 3. 统一响应

### 3.1 R<T>

```java
public class R<T> {
    private String code;      // "OK" 或 ErrorCode 枚举名
    private String message;   // 提示信息
    private T data;           // 业务数据
    private String traceId;   // 请求追踪 ID
}
```

- `code = "OK"` 表示成功。
- `message` 用于页面提示，默认中文；前端不依赖 message 做逻辑判断。
- `traceId` 由 TraceFilter 在请求入口生成 UUID，写入 MDC、响应头 `X-Trace-Id` 和 `R.traceId`。

### 3.2 PageResult<T>

```java
public class PageResult<T> {
    private List<T> records;
    private Integer pageNo;
    private Integer pageSize;
    private Long total;
    private Boolean hasNext;
}
```

### 3.3 ErrorCode

| HTTP | code | 含义 |
|------|------|------|
| 200 | `OK` | 成功 |
| 400 | `BAD_REQUEST` | 参数错误 |
| 401 | `UNAUTHORIZED` | 未登录 |
| 403 | `FORBIDDEN` | 权限不足 |
| 404 | `NOT_FOUND` | 不存在 |
| 409 | `BUSINESS_CONFLICT` | 状态冲突 |
| 422 | `VALIDATION_FAILED` | 校验失败 |
| 500 | `INTERNAL_ERROR` | 系统错误 |
| 502 | `EXTERNAL_SERVICE_ERROR` | 外部服务异常 |

### 3.4 TraceFilter

`OncePerRequestFilter` 实现：
1. 生成 UUID 作为 traceId
2. 写入 MDC（`traceId` key）
3. 设置到请求属性供 Controller 使用
4. 将 traceId 写入 `R.traceId` 和响应头 `X-Trace-Id`

## 4. Flyway 迁移

### 4.1 命名约定

- 格式：`V{递增整数}__{kebab-case描述}.sql`（双下划线分隔版本号与描述）
- 只做增量迁移，不修改已有版本号
- 新增字段用 `ALTER TABLE`，不重建表
- 新增表追加下一个版本号
- 迁移文件放在 `backend/src/main/resources/db/migration/`

### 4.2 迁移版本

| 版本 | 文件名 | 内容 |
|------|--------|------|
| V1 | `V1__create-organizations-fonds.sql` | `organizations`、`fonds` |
| V2 | `V2__create-users-roles.sql` | `users`、`roles`、`user_roles` + CHECK 约束 + 唯一约束 |
| V3 | `V3__create-categories-tags.sql` | `categories`、`tags`、`archive_tags` |
| V4 | `V4__create-archives.sql` | `archives`、`archive_files`、`archive_change_logs` |
| V5 | `V5__create-intake.sql` | `intake_batches`、`intake_items`、`staging_files`、`business_attachments` |
| V6 | `V6__create-warehouse.sql` | `warehouse_rooms`、`storage_locations`、`archive_boxes`、`archive_box_items` |
| V7 | `V7__create-borrow-approval.sql` | `borrow_requests`、`approval_requests` |
| V8 | `V8__create-appraisal-destruction.sql` | `appraisal_batches`、`appraisal_items`、`destruction_lists`、`destruction_items` |
| V9 | `V9__create-inventory-compilation.sql` | `inventory_tasks`、`inventory_items`、`compilations`、`compilation_materials` |
| V10 | `V10__create-analysis-ai.sql` | `analysis_tasks`、`analysis_items`、`ai_tasks`、`ai_task_batches` |
| V11 | `V11__create-system.sql` | `backup_tasks`、`file_check_records`、`archive_access_logs`、`audit_logs`、`system_configs` |
| V12 | `V12__create-indexes.sql` | 全部唯一约束和查询索引（按数据库设计文档 14.1-14.3） |
| V13 | `V13__seed-roles-categories.sql` | 7 个预设角色 + 5 个固定分类 |
| V14 | `V14__seed-system-configs.sql` | 全部默认系统配置 |
| V15 | `V15__seed-demo-data.sql` | 示例组织、全宗、用户、标签、库房（源自 database/archive_demo.sql） |

### 4.3 DDL 规范

- 主键统一 `BIGSERIAL`，固定字典表用 `SMALLSERIAL`
- `created_at`、`updated_at` 默认 `now()`
- 枚举字段加 `CHECK` 约束
- 关系明确的字段建外键
- `pg_trgm` 扩展由 Docker init-db 脚本创建，不在 Flyway 中管理
- 通用审计字段（created_at、updated_at、created_by、updated_by）统一包含

## 5. Sa-Token 认证

### 5.1 已完成接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/auth/login` | 登录 | 放行 |
| POST | `/api/auth/logout` | 登出 | 登录 |
| GET | `/api/auth/me` | 当前用户信息 | 登录 |
| GET | `/api/dictionaries` | 全部字典 | 放行 |
| GET | `/api/dictionaries/{dictCode}` | 单个字典 | 放行 |
| GET | `/api/users` | 用户列表（分页） | admin |
| POST | `/api/users` | 新增用户 | admin |
| PUT | `/api/users/{id}` | 编辑用户 | admin |
| POST | `/api/users/{id}/reset-password` | 重置密码 | admin |

### 5.2 登录逻辑

1. 校验 `loginName` + `password`（BCrypt）
2. 校验 `portal` 与用户角色匹配（admin 门户需 front_archivist/back_archivist/sys_admin/director，transfer 门户需 transfer_user，等）
3. 校验用户 `status = active`
4. Sa-Token `StpUtil.login(userId)` 签发 token
5. 写入 Session：userId、roles、maxSecurityLevel、dataScope、organizationId
6. 记录审计日志
7. 返回 token + 用户信息 + defaultRoute

### 5.3 AuthContext 工具类

静态方法封装从 Sa-Token Session 获取：
- `getCurrentUserId()` → Long
- `getCurrentUserRoles()` → List<RoleCode>
- `getMaxSecurityLevel()` → int
- `getDataScope()` → DataScope
- `getOrganizationId()` → Long
- `isAuthenticated()` → boolean

Service 层直接调用 `AuthContext.getXxx()` 获取当前用户信息。

### 5.4 路由权限（SaTokenConfig）

| 路径模式 | 权限要求 |
|----------|----------|
| `/api/auth/login` | 放行 |
| `/api/public/**` | 放行 |
| `/api/dictionaries/**` | 放行 |
| `/api/admin/**` | `front_archivist` / `back_archivist` / `sys_admin` / `director` |
| `/api/transfer/**` | `transfer_user` |
| `/api/internal/**` | `internal_reader` |
| `/api/approval/**` | `director` |
| 其他 `/api/**` | 登录即可 |

## 6. 给刘星的约定（feat/base-liu）

周扬在 `feat/base-zhou` 完成骨架后合并到 `develop`，刘星从 `develop` 拉出 `feat/base-liu` 追加以下文件：

| 刘星负责 | 文件位置 | 说明 |
|----------|----------|------|
| OpenAPI 配置 | `config/OpenApiConfig.java` | 分组、标题、安全方案、服务器 URL |
| 全局异常处理器 | `exception/GlobalExceptionHandler.java` | 捕获 BusinessException、MethodArgumentNotValidException 等，转为 R<T> |
| AI Client | `config/AiModelConfig.java` + `service/AiClientService.java` | DeepSeek/OpenAI 兼容接口调用 |
| 接口样例 | `controller/ArchiveController.java` | 骨架 CRUD，含 DTO 和 VO，供前端参照 |
| 序列号工具 | `util/SequenceUtil.java` | ARC-、BAT-、BRW- 等业务编号生成 |

**约定规则：**
- 刘星不修改已有 Entity、Mapper 和 Config，只新增文件
- 如有冲突，优先在 develop 合并时解决
- Flyway 已包含全部表，刘星不需要新增迁移脚本

## 7. Entity 规范

### 7.1 基类

```java
public class BaseEntity {
    private Long id;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
```

MyBatis-Plus `MetaObjectHandler` 自动填充 `createdAt`、`updatedAt`、`createdBy`、`updatedBy`。

### 7.2 字段映射

- 数据库 `snake_case` → Java `camelCase`（MyBatis-Plus 全局配置 `map-underscore-to-camel-case: true`）
- 枚举字段用 Java enum + `@EnumValue` 注解
- 时间字段统一 `OffsetDateTime`（对应 PostgreSQL `TIMESTAMPTZ`）
- 软删除字段 `deletedAt` 仅在需要的 Entity 上声明

## 8. 验收标准

| 检查项 | 通过标准 |
|--------|----------|
| 项目编译 | `./mvnw compile` 成功 |
| Flyway 迁移 | 启动后端后自动执行 V1-V15，无报错 |
| Swagger UI | 访问 `http://localhost:8080/swagger-ui.html` 可看到 Auth、User、Dictionary 接口 |
| 登录闭环 | 用 demo 账号 `chen.front` / `123456` 登录成功，返回 token 和用户信息 |
| 权限拦截 | 未登录访问 `/api/users` 返回 401；非管理员携带 token 访问返回 403 |
| 统一响应 | 所有接口返回 `{code, message, data, traceId}` 结构 |
| 分页查询 | 用户列表支持 pageNo/pageSize 分页 |
| 字典接口 | `/api/dictionaries` 返回角色、分类等前端需要的枚举字典 |
