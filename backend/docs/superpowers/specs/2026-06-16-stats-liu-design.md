# 设计文档：feat/stats-liu — 盘点、编研、统计研判与保存（M12/M13/M14）

- 负责人：刘星（后端，GitHub: Lx123759）
- 分支：`feat/stats-liu`（从最新 `develop` 拉出）
- 计划周期：2026-06-16 至 2026-06-18（06-18 后端接口冻结里程碑）
- 对应模块：M12 档案盘点（P1）、M13 档案编研（P2）、M14 统计研判与保存（P2）
- 接口契约：[doc/接口文档.md](../../../doc/接口文档.md) 第 18-21 章
- 模块边界：[doc/模块说明.md](../../../doc/模块说明.md) 3.12 / 3.13 / 3.14

## 1. 概述

本分支实现刘星在 06-16 至 06-18 的后端收尾工作，覆盖三个子模块共约 20 个 REST 端点：

- **M12 档案盘点**：按库房 + 门类创建盘点任务、系统生成应盘明细、逐项核对、生成差异统计。
- **M13 档案编研**：选择素材档案、撰写富文本正文、生成 PDF 附件、将成果作为纯电子档案入库。
- **M14 统计研判与保存**：馆藏/分类/趋势统计与报表导出（xlsx+pdf）、数据研判（规则扫描 + AI 建议）、数据库与文件备份、正式文件四性检测。

数据库表与序列在前期迁移中已全部建立（V4/V9/V10/V11 建表，V14 建序列），**本分支不新增数据库迁移**，仅当发现某条高频查询缺少关键索引时才追加 V19。整个工作为纯 Java 应用层新增（实体、Mapper、Service、Controller、DTO、枚举、编号工具）。

## 2. 范围与不变量

### 2.1 必须实现（对齐接口契约）

| 模块 | 端点 | 章节 |
|------|------|------|
| M12 盘点 | 查询/创建/开始/详情/更新明细/完成 | 18.1–18.6 |
| M13 编研 | 查询/创建/详情/更新/生成附件/入库 | 19.1–19.6 |
| M14 统计 | 总览/分类统计/导出报表 | 20.1–20.3 |
| M14 研判 | 查询/创建/详情/处理研判项 | 20.4–20.7 |
| M14 保存 | 查询/创建备份、查询/触发四性检测 | 21.1–21.4 |

### 2.2 关键不变量（来自模块说明与接口契约）

- **档号唯一生成点**仍在 M05 `ArchiveNoUtil`；M13 编研入库时复用 `ArchiveNoUtil` 生成档号，不得另造规则。
- **AI 安全边界**：研判 AI 只写 `analysis_items.suggestion`，绝不直接修改正式档案字段；规则扫描在 AI 不可用时仍可独立产出异常候选。
- **四性检测**：安全性检查（ClamAV）不可降级放行；真实性未接入外部签名体系时记录 `not_configured`。
- **盘点暂停借阅**：借阅可借性校验 `BorrowEligibilityChecker` 已内置「运行中盘点按 room+category 拦截借阅」逻辑，本分支只需保证盘点 `start/complete` 正确切状态，不修改借阅代码。
- **审计留痕**：所有写操作经 `AuditService.log(module, operation, businessType, businessId, detailMap)` 写 `audit_logs`。
- **软删除**：项目未启用 `@TableLogic`，查询一律手写 `isNull("deleted_at")`。

### 2.3 架构约定（沿用既有模式）

- Controller：`@RestController @RequiredArgsConstructor @Tag`，方法级全路径（如 `/api/admin/inventory-tasks`），统一返回 `R<T>`；文件下载返回 `ResponseEntity<byte[]>`。
- Service：`@Service @RequiredArgsConstructor`，业务异常抛 `BusinessException(ErrorCode.X, "中文消息")`，当前用户取 `AuthContext.getCurrentUserId()`。
- 实体：继承 `BaseEntity`（id/createdAt/updatedAt/createdBy/updatedBy），各实体自带 `private OffsetDateTime deletedAt`，MyBatis-Plus `@TableName`/`@EnumValue`。
- 分页：`PageRequest` 入参、`PageResult<T>` 出参。
- 编号：DB 序列 + NoUtil（`jdbcTemplate.queryForObject("SELECT nextval('seq_xxx')")` + `String.format`）。
- 文档落位：本 spec 在 `backend/docs/superpowers/specs/`，实施计划在 `backend/docs/superpowers/plans/`，运行时验证在 `backend/docs/superpowers/runtime/`。

## 3. 新增文件清单

### 3.1 共享基础（编号工具与枚举）

编号工具（均仿 `AppraisalNoUtil`，注入 `JdbcTemplate`，调对应序列）：

| 类 | 序列 | 格式 |
|----|------|------|
| `InventoryTaskNoUtil` | `seq_inventory_task_no` | `INV-{6位序号}` |
| `CompilationNoUtil` | `seq_compilation_no` | `CMP-{6位序号}` |
| `AnalysisTaskNoUtil` | `seq_analysis_task_no` | `ANA-{6位序号}` |
| `BackupTaskNoUtil` | `seq_backup_task_no` | `BAK-{6位序号}` |

枚举（`com.archive.enums`，与 DB CHECK 约束一一对应，`@EnumValue` 序列化）：

- `InventoryTaskStatus`：`draft / running / completed`
- `InventoryCheckResult`：`normal / missing / misplaced / damaged / on_loan`
- `CompilationStatus`：`draft / generated / archived`
- `AnalysisTaskType`：`missing_fields / category_conflict / mixed`
- `AnalysisTaskStatus`：`running / partial_completed / completed / failed`
- `AnalysisIssueType`：`missing_field / category_conflict / tag_suggestion`
- `AnalysisItemStatus`：`pending / adopted / rejected`
- `BackupScope`：`database / files / both`
- `BackupStatus`：`running / success / failed`
- `FileCheckTargetType`：`staging_file / archive_file`
- `FileCheckType`：`integrity / usability / authenticity / security`
- `FileCheckResult`：`passed / failed / not_configured`

### 3.2 各模块文件（Java 包：`com.archive`）

| 层 | M12 盘点 | M13 编研 | M14 统计 | M14 研判 | M14 保存 |
|----|----------|----------|----------|----------|----------|
| Entity | `InventoryTask` `InventoryItem` | `Compilation` `CompilationMaterial` | — | `AnalysisTask` `AnalysisItem` | `BackupTask` `FileCheckRecord` |
| Mapper | `InventoryTaskMapper` `InventoryItemMapper` | `CompilationMapper` `CompilationMaterialMapper` | （复用 `ArchiveMapper` 等只读聚合，`JdbcTemplate` 跑 GROUP BY） | `AnalysisTaskMapper` `AnalysisItemMapper` | `BackupTaskMapper` `FileCheckRecordMapper` |
| Service | `InventoryService` | `CompilationService` | `StatisticsService` | `AnalysisService` | `BackupService` `FileCheckService` |
| Controller | `InventoryController` | `CompilationController` | `StatisticsController` | `AnalysisController` | `BackupController` `FileCheckController` |
| 扩展 | — | — | — | 扩展 `AiTaskService`/`AiTaskAsyncRunner` 增加 `archive_analysis` | — |

DTO（`com.archive.dto.request` / `com.archive.dto.response`）按端点逐一定义，命名沿用 `<Domain><Action>Request` / `<Domain>Response` 模式（详见各模块章节）。

## 4. M12 档案盘点

### 4.1 端点（InventoryController，base `/api/admin/inventory-tasks`，角色 `back_archivist`）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/inventory-tasks` | 18.1 分页查询（status/roomId/categoryId） |
| POST | `/api/admin/inventory-tasks` | 18.2 创建任务 + 生成应盘明细 |
| POST | `/api/admin/inventory-tasks/{taskId}/start` | 18.3 开始 `draft→running` |
| GET | `/api/admin/inventory-tasks/{taskId}` | 18.4 详情（任务+明细+统计） |
| PUT | `/api/admin/inventory-tasks/{taskId}/items/{itemId}` | 18.5 更新明细（限 running） |
| POST | `/api/admin/inventory-tasks/{taskId}/complete` | 18.6 完成 `running→completed` |

### 4.2 应盘明细生成规则（18.2 核心）

- 盘点范围 = `roomId + categoryId`。
- 候选档案 SQL：经 `archive_box_items abi JOIN archive_boxes ab ON ab.id=abi.box_id JOIN storage_locations sl ON sl.id=ab.location_id`，`sl.room_id = roomId` 且 `abi.archive_id` 对应 `archives.category_id = categoryId` 且 `archives.lifecycle_status != 'destroyed'` 且两表 `deleted_at IS NULL`。
- 对每条候选档案写一条 `inventory_items`：
  - `box_id`、`expected_location_id` 取自其盒位与架位；
  - `actual_location_code = null`（待核验时回填）；
  - `check_result`：当前 `loan_status = loaned`（已实际借出）→ `on_loan`（满足「借出中」标记），否则默认 `normal`（满足 NOT NULL 约束，作为待核验初值）；
  - `note = null`。
- 无候选档案时仍创建任务（draft，空明细），但 18.3 start 要求「任务必须有盘点明细」，空任务不可 start。
- 创建即写审计 `AuditService.log("M12","create_inventory_task","inventory_task",taskId,Map)`。

### 4.3 状态机与校验

- `draft → running`（18.3 start）：校验存在明细；回填 `started_at`。
- 明细更新（18.5）：校验任务为 `running`；写入 `actual_location_code`、`check_result`、`note`。
- `running → completed`（18.6 complete）：写入 `summary`、`completed_at`。
- 任务/明细查询统一 `isNull("deleted_at")`。

### 4.4 详情统计（18.4）

返回任务基本信息 + 明细列表 + 按 `check_result` 分组计数（normal/missing/misplaced/damaged/on_loan 各多少，供前端差异报告展示）。

### 4.5 DTO

- 请求：`InventoryTaskQuery`（status/roomId/categoryId + PageRequest 字段）、`InventoryTaskCreateRequest`（taskName/roomId/categoryId，`@Valid` 非空）、`InventoryItemUpdateRequest`（actualLocationCode/checkResult/note）。
- 响应：`InventoryTaskResponse`（列表项）、`InventoryTaskDetailResponse`（任务+明细+统计）、`InventoryItemResponse`、`InventoryTaskStatsResponse`（各结果计数）。

### 4.6 与 M09 借阅的联动验证

`BorrowEligibilityChecker.checkBorrowable` 已实现：解析档案盒位 `room_id`，查询 `inventory_tasks` 中 `status='running'` 且 `room_id+category_id` 命中的任务数，>0 即抛「档案所在架位/门类正在盘点，暂停借阅」。本分支不改借阅代码，但在运行时验证中复测：盘点 start 后，命中范围的档案借阅申请应被 409 拦截；complete 后恢复可借。

## 5. M13 档案编研

### 5.1 端点（CompilationController，base `/api/admin/compilations`，角色 `back_archivist`）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/compilations` | 19.1 分页查询（status/keyword） |
| POST | `/api/admin/compilations` | 19.2 创建草稿（含素材 archiveIds） |
| GET | `/api/admin/compilations/{id}` | 19.3 详情（草稿+素材+附件+入库档案） |
| PUT | `/api/admin/compilations/{id}` | 19.4 更新（限 draft/generated） |
| POST | `/api/admin/compilations/{id}/generate` | 19.5 生成正文附件 `draft→generated` |
| POST | `/api/admin/compilations/{id}/archive` | 19.6 入库 `generated→archived` |

### 5.2 创建/更新（19.2 / 19.4）

- 写 `compilations`（compilation_no 由 `CompilationNoUtil` 生成；title/compilationType/dateRangeText/keywords/summary/contentHtml；status=draft）。
- 素材：`materialArchiveIds` 逐条校验「为当前管理员可见的正式档案（存在、未销毁、`lifecycle_status != destroyed`）」，按 `sortNo` 顺序写 `compilation_materials`（只记引用 `archive_id` + `quote_note`，**不复制正文**）。更新时先删旧素材再写新素材。
- 更新校验：仅 `draft`/`generated` 可改，`archived` 只读（抛 `BUSINESS_CONFLICT`）。

### 5.3 生成正文附件（19.5 generate）

- 校验 status=`draft`。
- `contentHtml` → `PdfGenerator`（openpdf）渲染为 PDF 字节。
- 上传 MinIO（对象键如 `compilations/{compilationId}/body.pdf`），写 `business_attachments`（`business_type=compilation`、`business_id=compilationId`、`attachment_type=report`、bucket/object_key/filename/ext/mime/size/sha256、`scan_result=safe`、`uploaded_by`）。
- 回填 `compilations.generated_file_attachment_id`，`status=draft→generated`。
- 审计 `AuditService.log("M13","generate_compilation","compilation",id,...)`。

### 5.4 入库（19.6 archive，单事务）

- 校验 status=`generated`（已生成正文附件）。
- 生成档号：`ArchiveNoUtil.generate()`（复用 M05 唯一规则）。
- 建 `archives`：`source_type=compilation`、`carrier_status=electronic`（纯电子）、`title=compilation.title`、`category_id`/`fonds_id`/`formed_date`/`retention_period`/`open_status` 取请求、`lifecycle_status=normal`、`loan_status=available`、`condition_status=normal`、`created_by`。
- 复制正文为正式 `archive_files`：`file_role=compilation_body`、上传 MinIO（对象键如 `archives/{archiveId}/compilation-body.pdf`）、回填 bucket/object_key/sha256/size/mime/`scan_result=safe`/`usability_result=passed`/`file_status=normal`。
- 标签：`tagNames` 复用 M05 的 tag 关联写法（tag 不存在则新建，写 `tags` + `archive_tags`）。
- 回填 `compilations.generated_archive_id` / `generated_archive_file_id`，`status=generated→archived`。
- 审计 `AuditService.log("M13","archive_compilation","compilation",id, Map.of("archiveId",archiveId))`。
- 返回生成的正式档案（`ArchiveResponse`）。

### 5.5 DTO

- 请求：`CompilationQuery`（status/keyword + 分页）、`CompilationSaveRequest`（title/compilationType/dateRangeText/keywords/summary/contentHtml/materialArchiveIds，创建与更新共用，`@Valid`）、`CompilationArchiveRequest`（fondsId/categoryId/formedDate/retentionPeriod/openStatus/tagNames）。
- 响应：`CompilationResponse`、`CompilationDetailResponse`（含素材引用列表 + 生成附件 + 入库档案 id）、`CompilationMaterialResponse`。

## 6. M14 数据统计（StatisticsController，base `/api/admin/statistics`，角色 `back_archivist`/`director`）

### 6.1 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/statistics/overview` | 20.1 总览 |
| GET | `/api/admin/statistics/categories` | 20.2 分类统计 |
| GET | `/api/admin/statistics/export` | 20.3 导出（format=xlsx/pdf） |

### 6.2 总览（20.1）

- 过滤参数：`yearStart`/`yearEnd`（按 `archives.formed_year`）、`organizationId`、`fondsId`。
- 实时聚合（无事实表），指标：
  - 馆藏总量 = `count(archives)` 非销毁；
  - 公开数量 = `open_status='open'` 且非销毁；
  - 借阅量 = `count(borrow_requests)`（或已出库数）；
  - 销毁量 = `count(archives where lifecycle_status='destroyed')`；
  - 征集/移交趋势 = `intake_batches` 按月/类型分组计数；
  - 库房占用 = 各 `storage_locations` 已用盒位/总盒位、占用率告警。
- 实现：`StatisticsService` 用 `JdbcTemplate` 跑聚合 SQL（带过滤条件），返回 `StatisticsOverviewResponse`。

### 6.3 分类统计（20.2）

按 门类（category）/年度（formed_year）/来源（source_type）/载体（carrier_status）/密级（security_level）/公开状态（open_status）多维度 GROUP BY 计数。返回 `StatisticsCategoryResponse`（各维度分组数组）。同样用 `JdbcTemplate`。

### 6.4 导出报表（20.3）

- 查询参数同总览 + `format`（`xlsx` 或 `pdf`）。
- 数据源：复用 20.1/20.2 的聚合结果。
- `xlsx`：**新增 Apache POI 依赖**（按项目硬约束，通过 `mvn` / Maven Central 查询当前最新稳定版后写入 `pom.xml`，不写死记忆版本）；构建工作簿（总览表 + 分类表），返回 `ResponseEntity<byte[]>`，Content-Type `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`。
- `pdf`：复用 `PdfGenerator`（openpdf）生成，Content-Type `application/pdf`。
- 文件名 URL 编码（仿 `BorrowController.exportVoucher`）。
- `format` 非法时抛 `BAD_REQUEST`。

### 6.5 DTO

- 请求：`StatisticsOverviewQuery`（yearStart/yearEnd/organizationId/fondsId/format）。
- 响应：`StatisticsOverviewResponse`（各指标 + 趋势 + 库房占用）、`StatisticsCategoryResponse`（各维度分组）。导出无响应体，返回文件流。

## 7. M14 数据研判（AnalysisController，base `/api/admin/analysis-tasks` 与 `/api/admin/analysis-items`，角色 `back_archivist`）

### 7.1 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/analysis-tasks` | 20.4 分页查询（status/taskType） |
| POST | `/api/admin/analysis-tasks` | 20.5 创建研判任务（规则扫描 + 可选 AI） |
| GET | `/api/admin/analysis-tasks/{taskId}` | 20.6 详情（任务+异常项+AI 任务状态） |
| POST | `/api/admin/analysis-items/{itemId}/handle` | 20.7 处理研判项（adopted/rejected） |

### 7.2 创建研判任务（20.5）

- 入参：`taskType`（missing_fields/category_conflict/mixed）+ `rule`（categoryIds/formedYearStart/formedYearEnd/includeAiSuggestion）。
- 写 `analysis_tasks`：task_no（`AnalysisTaskNoUtil`）、task_type、status=`running`（无 AI 分支时直接置 completed，见下）、`rule_snapshot=rule(JSONB)`、`started_at`。
- **规则扫描（始终执行）**：按 rule 圈定档案（categoryIds + formedYear 范围 + 非销毁）。逐档检测：
  - `missing_fields`/`mixed` → 必填字段为空（title/responsible/formedDate 等）→ 写 `analysis_items`(`issue_type=missing_field`、`issue_detail={field,...}`、`status=pending`、`suggestion=null`)；
  - `category_conflict`/`mixed` → 门类与标签/保管期等明显矛盾（如保管期=permanent 但 open_status 与密级冲突等可量化规则）→ `issue_type=category_conflict`。
- **AI 建议（增强项）**：`includeAiSuggestion && aiClient.isAvailable()` 时，创建 `ai_tasks`（`task_type=archive_analysis`、`business_type=analysis_task`、`business_id=analysis_task.id`、默认 50 条/批拆分），交 `AiTaskAsyncRunner.executeArchiveAnalysis(taskId)` 异步执行；分析任务 `latest_ai_task_id` 回填，状态保持 `running`/收敛为 `partial_completed`。AI 批处理结果**只写 `analysis_items.suggestion`**（新增 `issue_type=tag_suggestion` 的建议项或补充既有项 suggestion），不改档案。
- AI 不可用或未启用：规则扫描结束后直接置 `completed`、回填 `completed_at`。
- 审计 `AuditService.log("M14","create_analysis_task","analysis_task",taskId,...)`。

### 7.3 AiTask 扩展（复用既有 AI 任务机制）

- `AiTaskService`：新增 `startArchiveAnalysis(Long analysisTaskId)`——校验 AI 可用、查 analysis 任务圈定的档案 id、按 `aiClient.getBatchSize()` 拆批、写 `ai_tasks`+`ai_task_batches`、触发异步、回填 `analysis_tasks.latest_ai_task_id`。仿 `startIntakeCompletion`。
- `AiTaskAsyncRunner`：新增 `executeArchiveAnalysis(Long aiTaskId)`——逐批调 `AiClient`（注入档案元数据，**不注入正文**，符合安全边界），解析返回的 tag/异常建议，写入对应 `analysis_items.suggestion`（命中既有项则补 suggestion，否则新增 tag_suggestion 项）；批次状态收敛后聚合更新 `ai_tasks` 状态，并据此回写 `analysis_tasks.status`（全部 success→completed，部分→partial_completed，全失败且无规则产物→failed）。
- AI 返回字段白名单复用 M04 约束（只接受标签类建议，剥离受保护字段）。

### 7.4 处理研判项（20.7）

- 入参 `action`（adopted/rejected）+ `note`。
- 校验研判项 `status=pending`；写 `status`、`handled_by`、`handled_at`、`note`。
- **只改研判项状态，不修改正式档案字段**（采纳仅表示「人工已知悉，将在档案详情里手动维护」）。
- 审计 `AuditService.log("M14","handle_analysis_item","analysis_item",itemId,...)`。

### 7.5 详情（20.6）

返回任务（含 rule_snapshot、status、latest_ai_task_id 及关联 AI 任务状态）、异常项分页/列表。`AnalysisItemResponse` 含 issue_type/issue_detail/suggestion/status。

### 7.6 DTO

- 请求：`AnalysisTaskQuery`（status/taskType + 分页）、`AnalysisTaskCreateRequest`（taskType + rule）、`AnalysisRule`（categoryIds/formedYearStart/formedYearEnd/includeAiSuggestion）、`AnalysisItemHandleRequest`（action/note）。
- 响应：`AnalysisTaskResponse`、`AnalysisTaskDetailResponse`、`AnalysisItemResponse`。

## 8. M14 档案保存（备份 + 四性检测）

### 8.1 备份端点（BackupController，base `/api/admin/backup-tasks`，角色 `back_archivist`/`sys_admin`）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/backup-tasks` | 21.1 分页查询（status/backupScope） |
| POST | `/api/admin/backup-tasks` | 21.2 创建（立即执行） |

### 8.2 备份实现（可移植折中方案）

- 配置项：`archive.backup.dir`（默认 `./data/backups`），存放导出产物。
- 创建（21.2）：先校验「同一时刻建议只允许一个 `status=running` 的备份任务」（存在则抛 `BUSINESS_CONFLICT`，软删过滤）。
- 写 `backup_tasks`（task_no=`BackupTaskNoUtil`、backup_scope、status=`running`、started_at），随后立即执行：
  - `database`：用 `JdbcTemplate` 把**核心业务表**（users/roles 除外避免敏感泄漏，至少 archives/archive_files/intake_*/borrow_requests/appraisal_*/destruction_*/inventory_*/compilation*/analysis_*/system_configs 等业务表）导出为 SQL（`COPY ... TO STDOUT` 或 `SELECT` 拼装 `INSERT`），写入 `{dir}/{task_no}-db.sql`。**不依赖宿主机 pg_dump 二进制**，保证可移植与可 runtime 验证。
  - `files`：枚举 MinIO 业务 bucket 对象，复制到备份前缀 `backups/{task_no}/`（用 `MinioService` 的 copy 能力；对象多时写清单 + 按批复制，单次有上限并在 message 注明复制数量）。
  - `both`：两者都做。
- 产物计算 `sha256`、`file_size`、回填 `backup_path`、`status=success`（异常→`failed` + message）、`finished_at`。
- 整个执行不在单一 DB 事务内（导出/复制为重 IO）；任务行先 insert(running) 再执行再 update(终态)。
- 审计 `AuditService.log("M14","create_backup","backup_task",taskId, Map.of("scope",scope,"result",status))`。

### 8.3 四性检测端点（FileCheckController，角色 `back_archivist`）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/file-check-records` | 21.3 分页查询（targetType/targetId/checkType/checkResult） |
| POST | `/api/admin/archive-files/{fileId}/checks` | 21.4 触发正式文件检测 |

（两个端点同属 FileCheckController，跨 `/api/admin/file-check-records` 与 `/api/admin/archive-files` 路径，仿 BorrowController 跨路径写法。）

### 8.4 四性检测实现（21.4）

- 入参 `checkTypes`（integrity/usability/authenticity/security 子集）。
- 载入 `archive_files`（fileId），不存在抛 `NOT_FOUND`。
- 逐类型执行并写 `file_check_records`（`target_type=archive_file`、`target_id=fileId`、`checked_at`）：
  - **integrity**：从 MinIO 取对象重算 SHA-256，与 `archive_files.sha256` 比对 → `passed`/`failed`，记录 expected/actual hash。
  - **usability**：MinIO 对象存在且可读 + MIME 与 `archive_files.mime_type` 一致 → `passed`/`failed`。
  - **authenticity**：未接入外部数字签名体系 → `check_result=not_configured`、`signature_result=not_configured`（`signature_object_key`/`signature_algorithm`/`signature_checked_at` 均留空）。
  - **security**：复用 `ClamAvScanner` 扫描对象（下载临时文件或流式扫描），**不降级放行** → `passed`/`failed`（扫描服务异常视为 failed 并在 message 注明，不静默通过）。
- 返回本次检测记录列表（`FileCheckRecordResponse`）。
- 21.3 列表查询支持按 targetType/targetId/checkType/checkResult 过滤分页。
- 审计 `AuditService.log("M14","trigger_file_check","archive_file",fileId, Map.of("types",checkTypes))`。

### 8.5 DTO

- 请求：`BackupTaskQuery`（status/backupScope + 分页）、`BackupTaskCreateRequest`（backupScope）、`FileCheckRecordQuery`（targetType/targetId/checkType/checkResult + 分页）、`FileCheckTriggerRequest`（checkTypes）。
- 响应：`BackupTaskResponse`、`FileCheckRecordResponse`。

## 9. 测试策略（单元测试 + 运行时测试，缺一不可）

### 9.1 单元/集成测试（`backend/src/test`，仿 `BorrowServiceTest`/`AppraisalNoUtilTest`）

- 编号工具：`InventoryTaskNoUtilTest`/`CompilationNoUtilTest`/`AnalysisTaskNoUtilTest`/`BackupTaskNoUtilTest`（序号单调递增、格式正确）。
- `InventoryServiceTest`：应盘明细生成（room+category 命中、借出标记 on_loan、非销毁过滤）、状态机（空任务不可 start、complete 需 running）、统计计数。
- `AnalysisServiceTest`：规则扫描产出 missing_field/category_conflict 项、AI 不可用时直接 completed、handle 仅改状态不改档案。
- `FileCheckServiceTest`：四性各分支（integrity 哈希不符→failed、authenticity→not_configured、security 扫描异常→failed 不放行）。
- `BackupServiceTest`：database/files/both 各 scope 产物生成、sha256 非空、并发 running 拦截。
- DTO 校验测试（`@Valid` 非空等）。
- 沿用项目既有测试基线（真实 postgres+minio+clamav 已就绪，`@SpringBootTest` 集成测试可直接跑）。

### 9.2 运行时验证（`backend/docs/superpowers/runtime/2026-06-16-stats-liu-runtime.md`，仿 borrow-liu-runtime.md）

以 `back_archivist`（及相关角色）登录态跑完整 HTTP 链路并记录请求/响应/断言：
- 盘点：建任务→start→核对明细（normal/misplaced/on_loan）→complete；并复测「start 后命中范围档案借阅被 409 拦截、complete 后恢复」。
- 编研：建草稿（含素材）→generate（校验生成 business_attachment + PDF）→archive（校验生成 archives.source_type=compilation 与 archive_files.file_role=compilation_body，档号 ARC- 格式）。
- 统计：overview 各指标非空、categories 多维度分组、export 分别下载 xlsx 与 pdf 并校验 Content-Type。
- 研判：create（规则扫描产出异常项；DEEPSEEK_API_KEY 已配置，验证 AI 建议写入 suggestion 且未改档案字段）→handle adopted/rejected。
- 保存：备份 create（database/files/both 三次，校验 backup_path 产物与 sha256）；四性 trigger（integrity/usability/authenticity/security 四类记录、authenticity=not_configured）+ 列表查询。

## 10. 构建顺序与交付门禁

垂直切片、按依赖自底向上，每切片 TDD（先测试后实现）并增量 runtime 自测：

1. 共享基础：4 NoUtil + 枚举（+ NoUtil 单测）。
2. M12 盘点（P1，含借阅联动复测）。
3. M14 保存：备份 + 四性检测。
4. M14 统计：overview/categories/export（新增 Apache POI 依赖，查最新稳定版）。
5. M14 研判：规则扫描 + AiTask 扩展（archive_analysis）。
6. M13 编研：generate + archive 入库。
7. 全量 `mvn test` 通过 + 运行时验证文档成稿。

**交付门禁（用户要求）**：单测 + 运行时测试全部通过后，**等待用户检查**，用户确认后才 `git push` 与发起 PR（PR 标题与 commit 遵循 [doc/commit-convention.md](../../../doc/commit-convention.md)，身份走刘星 git/gh 配置）。

## 11. 风险与假设

- **依赖版本**：Apache POI 版本须通过 Maven Central 查询当前最新稳定版写入，不写死记忆版本（项目硬约束）。
- **备份导出范围**：核心业务表的「白名单」在实现时最终确定，原则是覆盖演示与 runtime 验证所需业务数据，排除 users/roles 等敏感表。
- **AI 研判确定性**：AI 返回非确定，运行时验证聚焦「规则扫描确定性产物 + AI suggestion 被写入且不污染档案字段」，不断言 AI 具体文本。
- **MinIO 复制性能**：files 备份与编研正文上传涉及对象复制，单次有上限并在 message 注明，避免 runtime 超时。
- **无新增迁移假设**：表与序列已存在；若联调中发现某查询缺关键索引导致慢查询，再追加 V19 索引迁移并说明。
