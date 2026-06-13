# feat/search-liu 设计规格

分支：`feat/search-liu`，从 `develop` 拉出。
负责人：刘星。
日期：2026-06-13 至 2026-06-15。
对应模块：M04 AI 辅助服务（接收清单补全入口）+ M08 检索与利用。
对应接口文档：第 5 章（5.2-5.6 公众检索）、第 9 章（9.3-9.5 AI 补全）、第 11 章（11.1-11.6 内部检索）。
对应 AI 交互架构：场景一（清单字段补全）、场景二（自然语言检索转查询条件）。

---

## 1 实现范围

### 1.1 本次实现（14 个接口）

| 接口 | 方法 | 路径 | Controller | Service |
|------|------|------|-----------|---------|
| 9.3 启动 AI 补全 | POST | /api/admin/pending-archive/batches/{batchId}/ai-completion | PendingArchiveController | AiTaskService + AiSuggestionService |
| 9.4 查询 AI 补全任务 | GET | /api/admin/ai-tasks/{taskId} | AiTaskController | AiTaskService |
| 9.5 重试失败 AI 批次 | POST | /api/admin/ai-tasks/{taskId}/retry-failed | AiTaskController | AiTaskService |
| 5.2 公开档案检索 | GET | /api/public/archives/search | PublicSearchController | SearchService |
| 5.3 公开档案详情 | GET | /api/public/archives/{archiveId} | PublicSearchController | SearchService |
| 5.4 公开档案预览 | GET | /api/public/archive-files/{fileId}/preview | PublicSearchController | SearchService |
| 5.5 公开档案下载 | GET | /api/public/archive-files/{fileId}/download | PublicSearchController | SearchService |
| 5.6 公众 AI 检索 JSON 生成 | POST | /api/public/archives/ai-query | PublicSearchController | SearchService |
| 11.1 内部工作台 | GET | /api/internal/dashboard | SearchController | SearchService |
| 11.2 内部档案检索 | GET | /api/internal/archives/search | SearchController | SearchService |
| 11.3 内部档案详情 | GET | /api/internal/archives/{archiveId} | SearchController | SearchService |
| 11.4 内部 AI 检索 JSON 生成 | POST | /api/internal/archives/ai-query | SearchController | SearchService |
| 11.5 内部预览电子文件 | GET | /api/internal/archive-files/{fileId}/preview | SearchController | SearchService |
| 11.6 内部下载电子文件 | GET | /api/internal/archive-files/{fileId}/download | SearchController | SearchService |

### 1.2 本次不实现

- 11.7-11.10 借阅申请相关接口 → 属于 `feat/borrow-liu`（06-15 至 06-16）。
- 档案研判 / 数据扫描（analysis_tasks）→ 属于 `feat/stats-liu`（06-16 至 06-18）。
- 11.1 内部工作台的借阅部分（我的借阅申请、当前借阅、逾期提示）→ 本次只实现「最近查阅」，借阅部分返回空结构占位，待 `feat/borrow-liu` 补齐。

### 1.3 数据库迁移

本次实现的所有表（`ai_tasks`、`ai_task_batches`、`archive_access_logs`、`archives`、`archive_files`、`categories`、`tags`、`archive_tags`、`intake_items.ai_suggestion`）和序列（`seq_ai_task_no`）均已在 V3-V14 迁移脚本中创建。**无需新增 Flyway 迁移脚本。**

---

## 2 文件结构

### 2.1 新增 Entity（3 个）

| 文件 | 对应表 | 说明 |
|------|--------|------|
| AiTask.java | ai_tasks | AI 任务主表，继承 BaseEntity |
| AiTaskBatch.java | ai_task_batches | AI 任务批次，继承 BaseEntity |
| ArchiveAccessLog.java | archive_access_logs | 档案访问日志，**不继承** BaseEntity（表无 created_by/updated_by/deleted_at 字段） |

AiTask 字段：`taskNo`、`taskType`（intake_completion）、`businessType`（intake_batch）、`businessId`（清单批次 ID）、`status`、`batchSize`、`totalBatches`、`successBatches`、`failedBatches`、`startedAt`、`completedAt`、`errorMessage`。

AiTaskBatch 字段：`taskId`、`batchNo`、`status`、`targetIds`（`List<Long>` + `JacksonTypeHandler`，本批 intake_item ID）、`requestContext`（`Map<String,Object>` + `JacksonTypeHandler`，注入上下文摘要，不含密钥）、`rawResponse`、`validatedResult`（`Map<String,Object>` + `JacksonTypeHandler`）、`errorMessage`、`attemptCount`、`startedAt`、`completedAt`。

ArchiveAccessLog 字段：`id`（`@TableId(AUTO)`）、`userId`、`userType`（internal/public/anonymous）、`archiveId`、`archiveFileId`、`accessType`（view_metadata/preview/download）、`ipAddress`（String，写入 PG `INET`）、`accessedAt`、`createdAt`（`@TableField(fill=INSERT)`）、`updatedAt`（`@TableField(fill=INSERT_UPDATE)`）。

### 2.2 新增 Mapper（3 个）

| 文件 | 说明 |
|------|------|
| AiTaskMapper.java | ai_tasks CRUD |
| AiTaskBatchMapper.java | ai_task_batches CRUD |
| ArchiveAccessLogMapper.java | archive_access_logs 插入 + dashboard 最近查阅查询 |

### 2.3 新增/改 Service（3 个）

| 文件 | 职责 | 预估行数 |
|------|------|----------|
| AiSuggestionService.java | 清单字段上下文组装、AI 提示词组装、白名单校验、枚举/ID 归属校验、写 `intake_items.ai_suggestion` | ~220 |
| AiTaskService.java | `ai_tasks`/`ai_task_batches` CRUD、`@Async` 异步逐批执行、状态汇总、失败重试、任务查询响应组装 | ~280 |
| SearchService.java | 公众/内部检索查询条件构造、权限过滤、详情、预览/下载预签名 URL、访问日志、AI 检索 JSON 生成（同步+重试）、dashboard 最近查阅 | ~420 |

### 2.4 新增/改 Controller（新增 2 + 改 2）

| 文件 | 路径前缀 | 状态 |
|------|----------|------|
| PublicSearchController.java | /api/public | 新增（5.2-5.6） |
| SearchController.java | /api/internal | 新增（11.1-11.6） |
| PendingArchiveController.java | /api/admin/pending-archive | 改：新增 9.3 `/batches/{batchId}/ai-completion` |
| AiTaskController.java | /api/admin/ai-tasks | 改：补全 9.4/9.5，去掉 501 空壳 |

### 2.5 新增 Config（1 个）

| 文件 | 职责 |
|------|------|
| AsyncConfig.java | `@EnableAsync` + AI 专用 `ThreadPoolTaskExecutor`（核心 2 / 最大 4 / 队列 10 / 线程名前缀 `ai-task-`），拒绝策略 `CallerRunsPolicy` |

### 2.6 新增 DTO

请求：

| 文件 | 用途 |
|------|------|
| AiQueryRequest.java | 5.6/11.4 AI 检索输入（`text`，必填，长度校验） |
| ArchiveSearchQuery.java | 5.2/11.2 检索查询参数（`@ModelAttribute` 绑定）：keyword、archiveNo、title、categoryId、formedYearStart/End、responsibleText、tagIds、sourceType、carrierStatus、hasElectronicFile、pageNo、pageSize；内部版（11.2）额外接受 securityLevel、openStatus |

响应：

| 文件 | 用途 |
|------|------|
| AiTaskStartResponse.java | 9.3 返回（aiTaskId, taskNo, status, batchSize, totalBatches） |
| AiTaskResponse.java | 9.4 返回（任务汇总 + successBatches/failedBatches/errorMessage） |
| AiQueryResponse.java | 5.6/11.4 返回（ruleType, conditions, rawJson） |
| ArchiveSummaryResponse.java | 5.2/11.2 检索列表项（archiveId, archiveNo, title, responsibleText, formedYear, categoryName, carrierStatus, hasElectronicFile） |
| ArchiveSearchDetailResponse.java | 5.3/11.3 详情（元数据 + 可预览/可下载文件摘要；公众版脱敏，内部版含密级/保管期限） |
| InternalDashboardResponse.java | 11.1 返回（recentViews 列表 + myBorrowRequests/currentBorrows/overdueReminders 空结构占位） |

### 2.7 新增 Util（1 个）

| 文件 | 职责 |
|------|------|
| AiTaskNoUtil.java | 调用 `seq_ai_task_no` 序列生成 `AIT-{6位序号}`，与 `ArchiveNoUtil` 模式一致 |

---

## 3 核心业务逻辑

### 3.1 AI 补全异步批处理（9.3-9.5）

**9.3 启动 AI 补全**（`AiTaskService.startIntakeCompletion(batchId)`）：

1. 校验 `ai.enabled = true` 且 `apiKey` 非空（`AiClient.isAvailable()`），否则抛 `EXTERNAL_SERVICE_ERROR`（"AI 功能未启用"）。
2. 查询批次 `intake_batches`，校验批次存在且 `source_type in (transfer, collection)`。
3. 查询该批次下 `intake_items` 中「已接收且未入库」条目（`status in (accepted, pending_archive)` 且 `generated_archive_id is null`）。无条目时抛 `BUSINESS_CONFLICT`（"批次没有可补全的条目"）。
4. 校验该批次不存在 `status = running` 的 `ai_tasks`（`business_type = intake_batch AND business_id = batchId`），否则抛 `BUSINESS_CONFLICT`（"该批次已有运行中的 AI 任务"）。
5. 创建 `ai_tasks` 记录：`task_no = AiTaskNoUtil.generate()`、`task_type = intake_completion`、`business_type = intake_batch`、`business_id = batchId`、`status = running`、`batch_size = AiClient.getBatchSize()`、`total_batches = ceil(条目数 / batchSize)`、`started_at = now()`、`created_by = 当前用户`。
6. 按 `batchSize` 拆分条目 ID，为每批创建 `ai_task_batches`：`batch_no` 从 1 递增、`status = pending`、`target_ids = [本批 itemIds]`。
7. 立即返回 `AiTaskStartResponse`（status=running, totalBatches）。
8. 触发 `@Async("aiTaskExecutor")` 方法 `executeIntakeCompletion(taskId)`，异步执行。

**异步执行**（`AiTaskService.executeIntakeCompletion(taskId)`，`@Async`）：

1. 查询任务的所有 `pending` 批次，按 `batch_no` 顺序逐批处理。
2. 每批：`status = running`、`started_at = now()`、`attempt_count++`。
3. 调用 `AiSuggestionService.buildAndCall(batch)`：
   - 组装系统提示词（AI 交互架构 4.1 场景一），约束 AI 只输出 `title/responsible/formedDate/category/tags`、分类必须从固定五类选、每次最多 50 条、输出 `<JSON>` 包裹。
   - 组装用户消息：注入本批条目的元数据字段（清单条目 ID、档案标题、文件名、形成日期、载体状态、页数、移交单位/部门、经办人、密级、保管期限、是否公开 —— 后三者仅作上下文参考，不进入 AI 可写字段）。
   - `request_context` 字段写入注入的上下文摘要（条目数、字段清单，不含敏感信息）。
   - 调用 `AiClient.callAndExtractJson(systemPrompt, userMessage)`，`raw_response` 保存 AI 原始响应。
4. 白名单校验与写库（`AiSuggestionService.validateAndPersist`）：
   - 解析 JSON：`ruleType` 必须为 `fieldCompletion`，否则记为格式错误。
   - 逐条遍历 `items`：剥离受保护字段（`securityLevel/retentionPeriod/openStatus/allowDigitization/archiveNo/warehouseLocation/destructionStatus` 一律丢弃）；`category` 必须命中固定五类 code/name，否则跳过该字段；`listItemId` 必须在本批 `target_ids` 内，否则丢弃该条；`formedDate` 校验 `yyyy-MM-dd` 格式。
   - 校验通过的字段建议，按 `listItemId` 分组，写入各 `intake_items.ai_suggestion`（`JacksonTypeHandler` 写 Map），只覆盖允许字段，不清空管理员已确认字段。
   - `validated_result` 保存通过校验的 JSON。
5. 批次成功：`status = success`、`completed_at = now()`；失败（AI 调用异常或 JSON 无法解析）：`status = failed`、`error_message` 记录原因、`raw_response` 保留。
6. 全部批次处理完后，`AiTaskService.summarizeTask(taskId)` 汇总：全部 `success` → `completed`；有 `success` 有 `failed` → `partial_completed`；全 `failed` → `failed`；更新 `success_batches`、`failed_batches`、`completed_at`。
7. 异步异常兜底：`@Async` 方法用 `try-catch` 包裹整体逻辑，任何未预期异常都把任务置为 `failed` 并写 `error_message`，避免线程死亡后任务卡在 `running`。

**9.4 查询任务**（`AiTaskService.getTask(taskId)`）：返回 `AiTaskResponse`（任务状态、批次数、成功/失败数、error_message、started_at/completed_at）。校验 `task_type = intake_completion`。

**9.5 重试失败批次**（`AiTaskService.retryFailed(taskId)`）：

1. 校验任务 `status in (partial_completed, failed)` 且存在 `failed` 批次，否则抛 `BUSINESS_CONFLICT`。
2. 将 `failed` 批次重置为 `pending`，`status = running`，回到异步执行流程。
3. 重试只处理 `failed` 批次（`success` 批次不动），每批 `attempt_count++`。
4. 返回任务最新状态。

### 3.2 AI 白名单校验

`AiSuggestionService` 内部实现，核心规则（对齐 AI 交互架构 6.2）：

| 校验项 | 处理 |
|--------|------|
| ruleType != fieldCompletion | 批次记 failed |
| items 非数组或空 | 批次记 failed |
| 受保护字段（securityLevel 等 7 个）出现 | 字段级剥离，不影响整条 |
| category 不在固定五类 | 跳过该字段（不写入 ai_suggestion） |
| listItemId 不在本批 target_ids | 丢弃该条 |
| formedDate 格式非法 | 跳过该字段 |
| tags 不是字符串数组 | 跳过该字段 |

固定五类映射（`categories` 表 code）：`document/technology/accounting/audio_video/personnel`，AI 返回中文名（文书档案等）时按 name 归一化为 categoryId。

### 3.3 公众检索权限过滤（5.2-5.6）

**5.2 公开检索**（`SearchService.publicSearch(query)`）：

1. 读 `system_configs` 中 `public_search.enabled`，为 `false` 时抛 `BUSINESS_CONFLICT`（"公众检索未开放"）。
2. MyBatis-Plus `QueryWrapper<Archive>` 动态拼接：
   - **强制** `security_level = 0`、`open_status = 'open'`、`lifecycle_status = 'normal'`。
   - `keyword` 对 `title`、`archive_no`、`responsible_text` 做 OR `like`；`archiveNo/title/responsibleText` 单独 `like`；`categoryId` 等值；`formedYearStart/End` 对 `formed_year` 范围；`tagIds` 通过 `archive_tags` 子查询 `in`；`sourceType/carrierStatus` 等值；`hasElectronicFile=true` 时加 `exists (select 1 from archive_files where archive_id=archives.id and file_status='normal')`。
3. 分页（复用现有 `PageRequest`/`PageResult`），返回 `ArchiveSummaryResponse` 列表，**脱敏**：不返回密级明细、库房架位、盒号、内部备注。

**5.3 公开详情**：同强制过滤查单条，写 `archive_access_logs(access_type=view_metadata)`，`user_type` 按 `AuthContext.isAuthenticated()` 取 `public`/`anonymous`。

**5.4 公开预览**：校验文件所属档案满足强制过滤 + `file_status = normal` → 复用 `MinioService` 生成预签名 URL → 写日志 `preview`。未登录可预览。

**5.5 公开下载**：`AuthContext.isAuthenticated()` 为 false 时抛 401（`UNAUTHENTICATED`）；同校验 → 预签名 URL → 写日志 `download`。

**5.6 公众 AI 检索**（`SearchService.publicAiQuery(text)`）：

1. 校验 `public_search.enabled`。
2. 注入公开范围标签：`select distinct t.tag_name from tags t join archive_tags at on ... join archives a on ... where a.security_level=0 and a.open_status='open' and a.lifecycle_status='normal'` + 固定五类。
3. 调 `AiClient.callAndExtractJson`（AI 交互架构 4.2 场景二提示词），最多重试 `AiClient.getSearchMaxRetries()` 次（JSON 格式/ruleType 校验失败才重试，AI 不可用直接抛错不重试）。
4. 校验输出：强制覆盖 `securityLevelMax = 0`、`openStatus = open`（不管 AI 返回什么）；`category` 命中五类；日期格式校验。
5. 返回 `AiQueryResponse`（ruleType=query, conditions, rawJson）。**AI 只生成条件，不执行检索。**

### 3.4 内部检索权限过滤（11.1-11.6）

**11.2 内部检索**（`SearchService.internalSearch(query)`）：

1. `QueryWrapper` 强制 `lifecycle_status = 'normal'`。
2. `security_level <= AuthContext.getMaxSecurityLevel()`。
3. 按 `AuthContext.getDataScope()`：
   - `all`：不加组织过滤。
   - `own_org`：`organization_id = AuthContext.getOrganizationId()`。
   - `own_fonds`：`fonds_id in (当前用户全宗范围)`（通过 `fonds.organization_id = 当前组织` 子查询）。
4. **不**硬过滤 `open_status`；其余查询参数同公众检索。
5. 返回 `ArchiveSummaryResponse`，**不**返回具体架位（只标记 `canBorrow` 待借阅模块用）。

**11.3 内部详情**：同内部权限过滤查单条，写 `view_metadata` 日志（`user_type=internal`）。返回当前用户可见元数据 + 可预览文件，含密级/保管期限。

**11.4 内部 AI 检索**：注入用户可见标签（按 `maxSecurityLevel` + `dataScope` + `organizationId/fondsId` + `lifecycle_status=normal` 过滤后的 distinct tags）+ 固定五类；调 AiClient + 重试；校验输出（`category` 命中五类、日期格式、关键词数组）；返回 `AiQueryResponse`。

**11.5/11.6 内部预览/下载**：校验文件所属档案在当前用户权限范围内（同内部检索过滤）+ `file_status=normal` → 预签名 URL → 写 `preview`/`download` 日志。下载可按密级和配置限制（本期按 `security_level <= maxSecurityLevel` 判定，已由档案可见性保证，不额外加配置）。

### 3.5 下载审计（访问日志）

贯穿 5.3/5.4/5.5/11.3/11.5/11.6，统一走 `SearchService.logAccess(archiveId, fileId, accessType)`：

- `user_id`：`AuthContext.isAuthenticated()` ? 当前用户 ID : null。
- `user_type`：公众端 anonymous/public，内部端 internal。
- `access_type`：view_metadata / preview / download。
- `ip_address`：从 `HttpServletRequest.getRemoteAddr()` 取（考虑 `X-Forwarded-For` 代理头）。
- 写入用 MyBatis-Plus；PG `INET` 字段接收 String，若 JDBC 报类型错误则在 Mapper 用 `CAST(? AS inet)`（实施时验证）。

### 3.6 AI 检索 JSON 生成（5.6/11.4）

检索类 AI 为同步调用、单次请求，按 AI 交互架构 2.5 **不留 `ai_tasks` 记录**（`business_type=none` 可选，本期不留）。重试逻辑封装在 `SearchService.callSearchAiWithRetry(systemPrompt, userMessage)`：

- 循环最多 `searchMaxRetries` 次：调用 `AiClient.callAndExtractJson` → 校验 ruleType=query 与必要字段 → 通过则返回；格式/ruleType 错误则重试。
- `BusinessException(EXTERNAL_SERVICE_ERROR)`（AI 不可用/超时/返回内容异常）**不重试**，直接向上抛。
- 全部重试失败抛 `EXTERNAL_SERVICE_ERROR`（"AI 服务异常，请稍后重试或改用普通筛选"）。

### 3.7 dashboard 最近查阅（11.1）

`SearchService.getInternalDashboard()`：

- `recentViews`：`select distinct on (archive_id) ... from archive_access_logs where user_id = ? order by archive_id, accessed_at desc limit 10`，联查 `archives` 取 archiveNo/title。用 `ArchiveAccessLogMapper` XML 或 `@Select` 实现。
- `myBorrowRequests`/`currentBorrows`/`overdueReminders`：返回空列表占位（`Collections.emptyList()`），待 `feat/borrow-liu` 补齐。

---

## 4 关键设计决策

| 决策点 | 选择 | 原因 |
|--------|------|------|
| AI 补全执行模型 | `@Async` 异步 + `ai_tasks`/`ai_task_batches` 记录 | 接口 9.3 返回 running + 9.4 查询 + 9.5 重试本身为异步批处理设计；条目多时同步会超时 |
| 检索类 AI | 同步调用，不留 ai_tasks | 单次请求，AI 交互架构 2.5 允许不留痕；避免检索路径写库开销 |
| Service 拆分 | AiSuggestionService + AiTaskService + SearchService | 按职责拆，与模块说明 M04/M08 对齐；检索权限过滤在 SearchService 内部分公众/内部分支，避免重复 |
| 公众/内部检索 Controller | 独立 PublicSearchController + SearchController | 路径前缀不同（/api/public 免登录 vs /api/internal 需登录），权限模型不同，不复用 admin 的 ArchiveFileController |
| 预签名 URL | 复用 MinioService | 与 admin 端文件预览/下载、暂存文件预览一致 |
| 访问日志 | 独立 ArchiveAccessLogMapper | 与 audit_logs（操作审计）职责不同；archive_access_logs 专记档案内容访问，支撑 dashboard 最近查阅和公众下载记录 |
| task_no 生成 | AiTaskNoUtil + seq_ai_task_no | 与 ArchiveNoUtil、generateBatchNo 模式一致 |
| ArchiveAccessLog 不继承 BaseEntity | 单独定义 id/createdAt/updatedAt | 表无 created_by/updated_by/deleted_at 字段 |
| ip_address INET | String 存储 + 必要时 CAST | 简单，公众匿名场景 IP 可能为 null |
| dashboard 借阅部分 | 空列表占位 | 借阅数据由 feat/borrow-liu 产生，避免跨模块依赖 |

---

## 5 依赖关系

### 5.1 依赖已有模块（均已就绪）

| 模块 | 依赖内容 |
|------|----------|
| AiClient | callAndExtractJson / isAvailable / getSearchMaxRetries / getBatchSize（base-liu） |
| AuthContext | getCurrentUserId / isAuthenticated / getMaxSecurityLevel / getDataScope / getOrganizationId / getUserType |
| ArchiveMapper / ArchiveFileMapper | 查询正式档案和文件 |
| IntakeItemMapper / IntakeBatchMapper | AI 补全读取已接收未入库条目、写 ai_suggestion |
| MinioService | 生成预签名 URL（preview/download） |
| TagMapper / ArchiveTagMapper | 检索 tagIds 过滤、AI 标签上下文注入 |
| AuditService | 写 audit_logs（9.3 启动补全、9.5 重试） |
| SaTokenConfig | `/api/public/**` 已免登录，`/api/internal/**`、`/api/admin/**` 需登录 |
| ErrorCode / R / PageResult / PageRequest | 统一响应与分页 |

### 5.2 不依赖未完成模块

| 模块 | 处理方式 |
|------|----------|
| M09 借阅（feat/borrow-liu） | dashboard 借阅部分返回空占位 |
| M14 统计研判（feat/stats-liu） | archive_analysis 任务类型本次不触发 |
| system_configs 读取 | dashboard/public_search 开关用 JdbcTemplate 或现有配置读取方式直查（若无现成 Service） |

---

## 6 错误处理

遵循已有 `BusinessException` + `ErrorCode` 模式。

| 场景 | HTTP | code | 说明 |
|------|------|------|------|
| AI 未启用/未配置 Key | 502 | EXTERNAL_SERVICE_ERROR | 9.3/5.6/11.4 触发时 |
| AI 调用失败/超时 | 502 | EXTERNAL_SERVICE_ERROR | 检索类不重试到此抛出 |
| 检索类 JSON 重试耗尽 | 502 | EXTERNAL_SERVICE_ERROR | 重试 searchMaxRetries 次仍失败 |
| AI 结果命中保护字段 | — | — | 字段级剥离，不报错（补全）；检索类强制覆盖 |
| 批次无可补全条目 | 409 | BUSINESS_CONFLICT | 9.3 |
| 批次已有运行中 AI 任务 | 409 | BUSINESS_CONFLICT | 9.3 |
| 任务非 partial_completed/failed | 409 | BUSINESS_CONFLICT | 9.5 重试校验 |
| 公众检索未开放 | 409 | BUSINESS_CONFLICT | public_search.enabled=false |
| 公众下载未登录 | 401 | UNAUTHORIZED | 5.5（SaToken 对 /api/public 免登录，Service 层手动判断 isAuthenticated 后抛出） |
| 档案/文件不存在或不可见 | 404 | NOT_FOUND | 详情/预览/下载 |
| 文件 file_status != normal | 409 | BUSINESS_CONFLICT | 预览/下载 |

> ErrorCode 现有枚举已覆盖：`UNAUTHORIZED`(401)、`NOT_FOUND`(404)、`BUSINESS_CONFLICT`(409)、`EXTERNAL_SERVICE_ERROR`(502)、`VALIDATION_FAILED`(422)，无需新增。

---

## 7 测试策略

### 7.1 单元测试

- `AiSuggestionServiceTest`：白名单过滤（命中 7 个受保护字段全部剥离）、category 非法跳过、listItemId 越界丢弃、formedDate 格式校验、tags 非数组跳过；mock `AiClient`。
- `AiTaskServiceTest`：批次拆分（50/100/101 条边界）、状态汇总（completed/partial_completed/failed）、重试只处理 failed 批次、`attempt_count` 递增。
- `SearchServiceTest`：公众强制过滤条件（security_level=0/open=open/lifecycle=normal）、内部按 maxSecurityLevel/dataScope 过滤、tagIds 子查询、hasElectronicFile exists；AI 检索重试次数与降级（mock AiClient 前两次抛异常第三次成功 / 三次全失败）。

### 7.2 集成测试

- 9.3 → 9.4 异步流转：启动任务后轮询 9.4 直到终态，校验 ai_suggestion 写入、批次状态、success/failed 计数。
- 9.5 重试：构造一个 failed 批次，重试后变 success，任务 completed。
- 公众检索权限边界：密级 > 0 / open=closed / lifecycle != normal 的档案不出现在公众检索结果。
- 内部检索权限边界：跨组织/全宗的档案不出现（dataScope=own_org/own_fonds）；超过 maxSecurityLevel 的不出现。
- 下载审计：5.4/5.5/11.5/11.6 调用后 archive_access_logs 有对应 access_type 记录。

### 7.3 AI 降级

- `ai.enabled=false`：9.3 返回 EXTERNAL_SERVICE_ERROR，不创建任务；5.6/11.4 返回错误；检索主流程（5.2/11.2）不受影响。

---

## 8 验收点（对应项目计划 3.2 共同验收点）

- 06-13 至 06-15 共同验收：「清单到正式档案闭环可跑通」。本模块提供 AI 补全（9.3-9.5）让管理员在入库前获得字段建议，与周扬 archive-zhou 的确认入库（9.7）衔接。
- 内部/公众检索可用，预览/下载有访问日志留痕。
- AI 降级不阻塞检索主流程。
