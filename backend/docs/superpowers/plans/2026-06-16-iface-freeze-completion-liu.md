# 接口冻结收尾实现计划（dashboard / 组织更新 / 删 export / 文档）

- 编写人：刘星（后端）
- 日期：2026-06-16
- 基线分支：`develop` @ `520f40f`
- 背景：接口一致性审计发现 7 处 doc↔代码差异，已与产品确认处置方案，本计划落地全部 7 项，使两侧收敛到 137 个、零差异。

## 0. 目标与验收

完成后：
- 文档 137 / 代码 137，集合 diff 为空。
- 管理概览页（前端 `/admin/overview`）调 `GET /api/admin/dashboard` 返回前端 `DashboardSummary` 完整契约。
- 公众概览调 `GET /api/public/dashboard` 返回 §5.1 契约。
- 组织支持 PUT 停用（status: active/disabled），名称唯一。
- 后端两个 `/export` xlsx 端点删除（统一前端 CSV）。
- 文档 §2.3/§6.1/§17 同步更新。
- `mvn test` 通过；新增 service 单测通过；runtime 登录态 curl 验证关键端点。

## 1. 关键假设（实现细节，非业务抉择；如不符再调整）

1. `archiveSummary.storageUsage`：MinioService 无用量接口 → 用 `SELECT COALESCE(SUM(file_size),0) FROM archive_files` 得已用字节，配额取 system_config 键 `storage.quota.bytes`（缺失默认 500GB），`ratio = used / quota`，`min(1.0,...)`；`storageWarningThreshold` 取 system_config 键 `storage.warning.threshold`（默认 0.85）。
2. `recentAuditLogs.operator`：AuditLog 仅存 actorUserId+actorType → 在 service 内按 actorType join `users`/`public_users` 取 realName（system 类型显示"系统"）。action 取 operationType（detail 作为补充可后续加）。取最近 5 条。
3. `todos` 八项计数的口径（按枚举 status 过滤 selectCount）：
   - pendingTransferReception：intake_batches status=待前台验收
   - pendingArchive：pending_archive 批次有待确认条目
   - borrowApproval / newBorrowRequests：borrow_requests status=待审批
   - approvalPending：approvals status=待审批
   - pendingDestruction：destruction_lists status=待审批/待销毁
   - pendingShelf：archives lifecycleStatus=已入库未上架
   - appraisalDue：appraisal_batches status=进行中
   - （实现时核对各枚举实际取值，统一在 DashboardService 注释里写明映射）
4. `todoEntries`：复刻前端 mock 结构（key/title/description/count/targetRoute/severity），count 取对应 todos 值；保持与前端 `OverviewTodo` 字段一致。
5. 公众 dashboard 的 `recentCollections` 复用 `IntakeBatchService` 的征集列表查询（按当前 public_user 过滤），`downloadLogs` 查 archive_access_logs（accessType=download，按 userId）。

## 2. 分段任务

### 段 A — 删除两个 export 端点（最小改动，先清场）
- `AuditLogController`：删 `export()` 方法 + 相关 import（LogExcelExporter、HttpServletResponse、List、ObjectMapper 若仅此用）。
- `ArchiveAccessLogController`：删 `export()` 方法 + import。
- `AuditLogQueryService`：删 `listForExport()` + `EXPORT_MAX` 常量（仅 listForExport 用）。
- `ArchiveAccessLogQueryService`：同上。
- 删 `util/LogExcelExporter.java`（grep 确认仅这两处引用）。
- 测试：跑 `mvn -q -Dtest='AuditLog*,ArchiveAccessLog*' test` 确认无引用残留编译通过。

### 段 B — 组织更新 PUT（§17.6，支持停用）
- 新增 `dto/request/OrganizationUpdateRequest`：orgName、orgType、contactName、contactPhone、status（active/disabled），带校验。
- `OrganizationService.updateOrganization(Long id, req)`：
  - 存在性 + 软删除检查；
  - 名称唯一（排除自身）；
  - 更新字段（status 允许 active/disabled）；
  - 写审计日志 `update_organization`。
- `OrganizationController.update(@PathVariable Long organizationId, @Valid @RequestBody OrganizationUpdateRequest)` → `PUT /{organizationId}`，角色 sys_admin。
- 测试：`OrganizationServiceTest` 增 update 成功 / 名称冲突 / 不存在 三例。

### 段 C — 公众 dashboard（§5.1 `GET /api/public/dashboard`）
- 新增 `dto/response/PublicDashboardResponse`：collectionSummary{total,active,submitted,...}、recentCollections[]、downloadLogs[]（字段对齐 §5.1 + 前端类型）。
- 新增 `service/PublicDashboardService`：注入 IntakeBatchService（或其 mapper）+ ArchiveAccessLogMapper + users 查询；按 `AuthContext.getCurrentUserId()` 过滤。
- `PublicSearchController`（class `/api/public`）加 `@GetMapping("/dashboard")`。
- 角色：public_user（依赖现有鉴权拦截）。
- 测试：`PublicDashboardServiceTest` 验证按当前用户过滤。

### 段 D — 管理 dashboard（§6.1 `GET /api/admin/dashboard`，最大段）
- 新增 `dto/response/AdminDashboardResponse` + 嵌套 `DashboardTodos`、`ArchiveSummary`、`WarehouseWarning`、`RecentAuditLog`、`OverviewTodo`，字段**逐一对齐前端 `types/dashboard.ts`**。
- 新增 `service/AdminDashboardService`：注入各业务 mapper（intake_batches、borrow_requests、approvals、destruction_lists、archives、appraisal_batches、pending_archive、warehouse、archive_files、users/public_users、system_config）；实现 8 计数 + archiveSummary + warehouseWarnings + recentAuditLogs + todoEntries + summarizedAt。
- 新增 `controller/DashboardController`（无类前缀或 `/api/admin`）：`@GetMapping("/api/admin/dashboard")`。
- 角色：front_archivist/back_archivist/director/sys_admin（§6.1），sys_admin 不返回档案正文（本端点本就不返回正文，天然满足）。
- 测试：`AdminDashboardServiceTest` 用 mock mapper 验证计数聚合逻辑（pendingXxx 各返回给定 count → response.todos 字段正确）。

### 段 E — 文档同步
- §2.3：删除通用 `GET /api/attachments/{id}/download` 接口定义，改为说明「附件下载按业务对象走专用端点：移交清单 `/transfer/batches/{id}/export`、接收回执 `/reception/batches/{id}/receipt`、借阅凭证 `/internal/borrow-requests/{id}/voucher`、销毁照片在销毁清册内、编研正文 `/compilations/{id}/generate`」。
- §6.1：把响应契约补全为前端 `DashboardSummary` 完整结构（todos 8 项、archiveSummary 4 项、warehouseWarnings、recentAuditLogs、todoEntries、summarizedAt）。
- §17：新增 §17.4（重编号）「获取全宗详情 `GET /api/admin/fonds/{fondsId}`」（在全宗列表/新增/更新之间插入；后续节顺延或就近放置，保持不破坏引用）。补 §17.6 组织更新 PUT 说明（请求体含 status：active/disabled，停用不物理删除）。
- （不改编研 `{compilationId}`↔`{id}` 形参名，cosmetic、用户未要求。）

### 段 F — runtime 验证
1. `cd backend && ./mvnw -q -DskipTests=false test` 全量单测通过。
2. 启动 postgres/minio（docker compose），`./mvnw spring-boot:run`。
3. 登录（sys_admin）拿 token，curl：
   - `GET /api/admin/dashboard` → 200 + 结构含 todos/archiveSummary/.../summarizedAt。
   - `GET /api/public/dashboard`（public_user 登录态）→ 200。
   - `PUT /api/admin/organizations/{id}` body `{status:"disabled"}` → 200，再 GET 确认 status。
   - `GET /api/admin/audit-logs/export` → 404（已删）。
4. 重跑 `/tmp/diff_endpoints.py` 确认 doc↔code 集合 diff 为空（含新增路径）。
5. 产物：`backend/docs/superpowers/runtime/2026-06-16-iface-freeze-liu-runtime.md` 记录验证结果。

## 3. 提交与分支

- 从 `develop` 拉新分支 `feat/dashboard-org-iface-liu`，按段提交（commit convention），完成后开 PR 合 `develop`（身份：刘星 Lx123759）。
- 不夹带无关改动。
