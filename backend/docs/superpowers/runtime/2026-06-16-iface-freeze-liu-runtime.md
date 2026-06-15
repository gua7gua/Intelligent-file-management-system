# 接口冻结收尾 — 运行时验证（dashboard / 组织更新 / 删 export）

- 验证人：刘星
- 日期：2026-06-16
- 分支：`feat/dashboard-org-iface-liu`（基于 `develop` @ `520f40f`）
- 验证方式：dev profile 启动后端，以登录态对每个新增/变更端点跑完整 HTTP 链路，记录请求/响应。

## 0. 环境就绪

| 依赖 | 状态 |
|------|------|
| archive-postgres (5432) | Up healthy（session 启动前已在运行，未改动） |
| archive-minio (9001/9002) | Up healthy |
| archive-clamav (3310) | Up healthy |
| 后端 | `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`，Tomcat:8080，`/actuator/health` = UP |

> 全量 Spring 上下文启动成功（含新增 DashboardController / AdminDashboardService / PublicDashboardService 等 bean），证明新 bean 装配无误。
> 注：`ArchiveApplicationTests.contextLoads` 单测在默认 profile 下因无数据源报 `Failed to determine a suitable driver class`——属项目既有测试配置缺口（数据源仅在 dev profile），与本次改动无关；dev profile 启动成功即等价证明上下文可加载。

## 1. 单测

`./mvnw test` → 222 个用例，221 通过，1 个为 `ArchiveApplicationTests.contextLoads`（上述环境原因）。本次新增 12 个用例全部通过：
- OrganizationServiceTest（8，含 4 个 update 用例）
- AdminDashboardServiceTest（3）
- PublicDashboardServiceTest（1）
- 两个日志 service 测试移除 export 用例后仍通过。

## 2. GET /api/admin/dashboard（§6.1，新增）

登录 `admin`(sys_admin) / `liu.back`(back_archivist) 均可访问（角色校验通过）；非管理角色应 403（未逐个测，controller 已显式校验 4 角色）。

```
GET /api/admin/dashboard  (Bearer liu.back)
→ 200 code=OK
  todos: {pendingTransferReception:0, pendingArchive:1, borrowApproval:1, approvalPending:1,
          pendingDestruction:1, pendingShelf:0, appraisalDue:0, newBorrowRequests:1}
  archiveSummary: {totalArchives:7, monthAdded:7, storageUsage:0.000106, storageWarningThreshold:0.85}
  warehouseWarnings: []  (无库房超阈值)
  recentAuditLogs: 5 条
  todoEntries: 4 条
  summarizedAt: 2026-06-16T02:14:32+08:00
```

断言：6 个顶层 key 齐全，与前端 `DashboardSummary` 契约一致；计数取真实 DB；storageUsage = sum(file_size)/500GB 计算正确。

## 3. GET /api/public/dashboard（§5.1，新增）

鉴权守卫：未登录访问 → HTTP 401（`/api/public/**` 虽免登录，但本端点显式校验 isAuthenticated）。

```
登录 zhou.public(public_user, id=5) portal=public → code=OK
GET /api/public/dashboard
→ 200 code=OK
  collectionSummary: {total:2, draft:0, inProgress:1, completed:1}
  recentCollections: 2 条（BAT-000003 待联系、BAT-000004 已入库）
  downloadLogs: 1 条
  summarizedAt: 2026-06-16T02:18:23+08:00
```

断言：按当前公众用户本人过滤；collectionSummary 与种子数据（小周 2 条征集：1 进行中 + 1 已入库）吻合。

## 4. PUT /api/admin/organizations/{id}（§17.7 更新/停用，新增）

```
登录 liu.back(back_archivist) → PUT /api/admin/organizations/1 {status:disabled}
→ 403 FORBIDDEN "仅系统管理员可更新组织"  ✓ 权限正确

登录 admin(sys_admin) → PUT /api/admin/organizations/1 {status:disabled}
→ 200 code=OK, status=disabled
GET 列表确认 org1.status=disabled  ✓
PUT {status:active} → 恢复 active（避免污染种子数据）
```

断言：部分更新生效；sys_admin 鉴权正确；停用不物理删除。

## 5. 已删除端点（统一前端 CSV）

```
GET /api/admin/audit-logs/export        → 无 handler（NoResourceFoundException）
GET /api/admin/archive-access-logs/export → 无 handler（NoResourceFoundException）
```

后端日志确认 `NoResourceFoundException: No static resource .../export`，即端点确已移除。（返回码 500 而非 404，是 GlobalExceptionHandler 对未知 URL 的既有映射，非本次引入；list 端点仍正常 200。）

## 6. 文档↔代码一致性

`/tmp/diff_endpoints.py` 重跑：CODE 137 / DOC 137，集合 diff 为空（doc_only=0, code_only=0）。7 处差异全部收敛。

## 7. 文档同步

- §2.3：删除通用附件下载端点，改为「按业务专用端点下载」说明表。
- §6.1：响应契约补全为前端 DashboardSummary 完整结构（todos 8 项 + 字段映射表）。
- §17：新增 §17.2 全宗详情；原 17.2-17.6 顺延为 17.3-17.7；§17.7 更新组织补充请求体（含 status:active/disabled，停用非物理删除）。
