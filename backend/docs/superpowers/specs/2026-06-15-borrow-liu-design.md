# feat/borrow-liu 设计规格

分支：`feat/borrow-liu`，从 `develop` 拉出。
负责人：刘星。
日期：2026-06-15 至 2026-06-16。
对应模块：M09 借阅管理。
对应接口文档：第 11 章（11.7-11.10 内部查阅者借阅申请）、第 12 章（12.1-12.5 借阅审批、出库与归还）。
对应业务场景：场景八（借阅审批）。
依赖模块：M01 认证、M05 入库与档案管理（读 `archives`）、M07 库房管理（审批看盒号架位）、M12 盘点（读 `inventory_tasks` 拦截运行中盘点）。

---

## 1 实现范围

### 1.1 本次实现（9 个接口 + 内部工作台借阅部分补齐）

| 接口 | 方法 | 路径 | 角色 |
|------|------|------|------|
| 11.7 提交借阅申请 | POST | /api/internal/borrow-requests | internal_reader |
| 11.8 查询我的借阅申请 | GET | /api/internal/borrow-requests | internal_reader |
| 11.9 借阅申请详情（本人） | GET | /api/internal/borrow-requests/{requestId} | internal_reader |
| 11.10 导出借阅凭证 | GET | /api/internal/borrow-requests/{requestId}/voucher | internal_reader |
| 12.1 查询借阅申请 | GET | /api/admin/borrow-requests | back_archivist / front_archivist |
| 12.2 获取借阅申请详情 | GET | /api/admin/borrow-requests/{requestId} | back_archivist / front_archivist |
| 12.3 审批借阅申请 | POST | /api/admin/borrow-requests/{requestId}/approve | back_archivist |
| 12.4 核验凭证并确认出库 | POST | /api/admin/borrow-requests/{requestId}/checkout | front_archivist / back_archivist |
| 12.5 确认归还 | POST | /api/admin/borrow-requests/{requestId}/return | front_archivist / back_archivist |
| 11.1 内部工作台借阅部分 | GET | /api/internal/dashboard（补齐） | internal_reader |

### 1.2 本次不实现

- M09 纸质借阅相关页面 → 前端 `feat/storage-guo`（郭一坤）。
- 盘点任务管理（M12）→ `feat/stats-liu`（06-16 至 06-18）。本次只**读取** `inventory_tasks` 表做借阅拦截，不建 `InventoryTaskMapper`，避免与 M12 重复。
- 审批工作台（M10，密级/开放/销毁审批）→ `feat/appraisal-zhou`。借阅审批是 M09 独立流程，不走 `approval_requests` 表。

### 1.3 数据库迁移

本次实现所需表与序列均已在现有迁移脚本中创建，**无需新增 Flyway 迁移脚本**：

| 对象 | 来源 |
|------|------|
| `borrow_requests` 表 | V7__create-borrow-approval.sql |
| `seq_borrow_request_no` 序列 | V14__create-sequences.sql |
| `seq_borrow_voucher_no` 序列 | V14__create-sequences.sql |
| `inventory_tasks` 表（只读拦截） | V9__create-inventory-compilation.sql |
| 种子数据 1 条 checked_out 借阅记录 | V16__seed-demo-data.sql |

---

## 2 文件结构

### 2.1 新增枚举（2 个）

| 文件 | 说明 |
|------|------|
| `enums/BorrowStatus.java` | 借阅申请状态：applied、rejected、approved、voucher_issued、checked_out、returned、abnormal_return。name() 对齐 V7 CHECK 约束与种子数据。带 `displayName`。 |
| `enums/ReturnCheckResult.java` | 归还检查结果：normal、damaged、missing_page、other。对齐 V7 CHECK 约束。带 `displayName`。 |

枚举持久化机制：项目全局配置 `mybatis-plus.configuration.default-enum-type-handler: MybatisEnumTypeHandler`（application.yml）。枚举类内部无 `@EnumValue` 属性时，按 `Enum.name()` 持久化与读取，与现有 `LifecycleStatus`、`LoanStatus`、`ConditionStatus` 完全一致。实体字段以 `@EnumValue` 标注（与 `Archive.lifecycleStatus` 写法一致）。

### 2.2 新增 Entity（1 个）

| 文件 | 对应表 | 说明 |
|------|--------|------|
| `entity/BorrowRequest.java` | borrow_requests | 继承 `BaseEntity`（获 createdAt/updatedAt/createdBy/updatedBy 自动填充） |

字段（与 V7 列一一对应）：

- `String requestNo`（BRW-{6位序号}）
- `Long archiveId`
- `Long borrowerId`
- `String reason`
- `Integer expectedDays`
- `OffsetDateTime expectedVisitAt`
- `String contactPhone`
- `@EnumValue BorrowStatus status`
- `Long approvedBy`
- `OffsetDateTime approvedAt`
- `String rejectReason`
- `String voucherNo`（VCH-{6位序号}）
- `OffsetDateTime voucherIssuedAt`
- `Long checkedOutBy`
- `OffsetDateTime checkedOutAt`
- `OffsetDateTime dueAt`
- `Long returnedBy`
- `OffsetDateTime returnedAt`
- `@EnumValue ReturnCheckResult returnCheckResult`
- `String returnNote`
- `@TableLogic OffsetDateTime deletedAt`（软删除，表有 deleted_at 列，仿 `User`/`Organization`）

### 2.3 新增 Mapper（1 个）

| 文件 | 说明 |
|------|------|
| `mapper/BorrowRequestMapper.java` | 继承 `BaseMapper<BorrowRequest>`。列表关键字（borrowerKeyword/archiveKeyword）通过 Service 层 `QueryWrapper` 或自定义查询拼接，无需 XML。 |

### 2.4 新增 Util（1 个）

| 文件 | 说明 |
|------|------|
| `util/BorrowNoUtil.java` | 仿 `BoxNoUtil`，注入 `JdbcTemplate`。`nextRequestNo()` → `SELECT nextval('seq_borrow_request_no')` 拼接 `BRW-%06d`；`nextVoucherNo()` → `SELECT nextval('seq_borrow_voucher_no')` 拼接 `VCH-%06d`。 |

### 2.5 新增 DTO

请求（`dto/request`）：

| 文件 | 字段 | 校验 |
|------|------|------|
| `BorrowApplyRequest.java` | archiveId、reason、expectedDays、expectedVisitAt、contactPhone | archiveId `@NotNull`；reason `@NotBlank`、`@Size(max=500)`；expectedDays `@Min(1)`、`@Max(365)`；contactPhone `@Pattern` 手机号；expectedVisitAt 可空 |
| `BorrowApproveRequest.java` | approved(boolean)、opinion(String) | approved `@NotNull`；approved=false 时 opinion `@NotBlank` |
| `BorrowCheckoutRequest.java` | voucherNo、dueAt、note | voucherNo `@NotBlank`；dueAt `@NotNull` 且须晚于当前时间（Service 校验）；note `@Size(max=500)` |
| `BorrowReturnRequest.java` | returnCheckResult、returnNote | returnCheckResult `@NotNull`；returnNote `@Size(max=500)` |
| `BorrowRequestQuery.java`（extends `PageRequest`） | status、borrowerKeyword、archiveKeyword、overdue(Boolean)、keyword（我的列表用） | 无强制校验 |

响应（`dto/response`）：

| 文件 | 说明 |
|------|------|
| `BorrowRequestResponse.java` | 列表/详情通用。字段：id、requestNo、status、reason、expectedDays、expectedVisitAt、contactPhone、dueAt、overdue(Boolean，动态计算)、rejectReason、voucherNo、voucherIssuedAt、approvedAt、approvedBy、checkedOutAt、returnedAt、returnCheckResult、returnNote、archive(`ArchiveSummary`：archiveId/archiveNo/title/carrierStatus)、borrower(`BorrowerSummary`：borrowerId/realName/employeeNo/departmentName/organizationName，仅管理端填充)、location(`LocationSummary`：boxNo/locationCode，仅管理端详情填充，内部端为 null) |
| `BorrowSummaryItem.java` | 内部工作台借阅摘要：requestNo、archiveId、archiveNo、title、status、dueAt、appliedAt（createdAt）。用于 `InternalDashboardResponse` 三列表 |

### 2.6 新增/改 Service

| 文件 | 类型 | 说明 |
|------|------|------|
| `service/BorrowService.java` | 新增 | 主服务：apply、listMine、getMine、exportVoucher、adminList、adminGet、approve、checkout、returnBorrow、dashboardMine/dashboardCurrent/dashboardOverdue。角色强制 + 审计写入在此。 |
| `service/BorrowEligibilityChecker.java` | 新增组件 | `checkBorrowable(Long archiveId)`：6 条件校验 + 盘点拦截 SQL。申请/审批复校/出库复校三处复用。注入 `ArchiveMapper`、`BorrowRequestMapper`、`JdbcTemplate`。 |
| `service/SearchService.java` | 改 | `getInternalDashboard` 调 `BorrowService` 填充 myBorrowRequests/currentBorrows/overdueReminders，替换原 `List<Object>` 占位。 |
| `util/PdfGenerator.java` | 改 | 新增 `generateBorrowVoucherPdf(BorrowRequest, Archive, borrower User, Organization)`。 |

### 2.7 新增/改 Controller

| 文件 | 类型 | 说明 |
|------|------|------|
| `controller/BorrowController.java` | 新增 | 无类级 `@RequestMapping`，方法级写全路径。`@Tag(name="借阅管理")`。9 个端点。 |
| `dto/response/InternalDashboardResponse.java` | 改 | `List<Object>` ×3 → `List<BorrowSummaryItem>` ×3。 |

> `SearchController` 本身不改：dashboard 端点（11.1）签名不变，仅其依赖的 `SearchService.getInternalDashboard` 内部接入 `BorrowService`。

---

## 3 状态机

```
                         exportVoucher(首次)
   applied ──approve(true)──> approved ──────────────> voucher_issued
     │                            │                        │
     │                            │      checkout          │
     │                            └─────┐    ┌─────────────┘
     │                                  ▼    ▼
     │                              checked_out
     │                                  │
     │──approve(false)──> rejected      │──return(normal)────> returned（终态）
     │     (终态)                       │──return(非normal)──> abnormal_return（终态）
```

### 3.1 流转规则

| 操作 | 前置状态 | 目标状态 | 必填输入 | 副作用 |
|------|----------|----------|----------|--------|
| apply | （新建） | applied | archiveId/reason/expectedDays | 生成 requestNo；borrowerId=当前用户 |
| approve(true) | applied | approved | opinion（可选） | approvedBy/approvedAt；opinion 入审计 |
| approve(false) | applied | rejected | opinion（必填→rejectReason） | approvedBy/approvedAt；rejectReason |
| exportVoucher | approved | voucher_issued（首次）/不变（重复） | 无 | 首次：生成 voucherNo + voucherIssuedAt；重复：复用原 voucherNo |
| checkout | approved 或 voucher_issued | checked_out | voucherNo(须匹配)/dueAt/note | checkedOutBy/checkedOutAt/dueAt；**事务内** archive.loanStatus=on_loan |
| return(normal) | checked_out | returned | returnCheckResult=normal | returnedBy/returnedAt；archive.loanStatus=available |
| return(非normal) | checked_out | abnormal_return | returnCheckResult∈{damaged,missing_page,other} | returnedBy/returnedAt/returnCheckResult/returnNote；archive.loanStatus=available；archive.conditionStatus=damaged |

非法前置状态 → `BusinessException(BUSINESS_CONFLICT, "当前状态不允许该操作")`。

### 3.2 字段映射约定（V7 无 approve_opinion 列）

- `approved_by`/`approved_at` 在通过与拒绝时**均**填写（复用为「审批处理人/处理时间」）。
- 通过时 opinion 不落 borrow_requests 行（无列），仅写 `audit_logs.detail`。
- 拒绝时 opinion 写入 `reject_reason`，并写审计。

### 3.3 异常归还与实体状态联动

`returnCheckResult` → `archive.conditionStatus` 映射：

| returnCheckResult | conditionStatus |
|-------------------|-----------------|
| normal | 不变（保持 normal） |
| damaged | damaged |
| missing_page | damaged |
| other | damaged |

> 「修复中」「遗失」由后台档案管理员在 M05 档案管理中单独处置，不在借阅归还环节设置。异常归还为借阅记录终态，后续借阅由 `conditionStatus != normal` 拦截。

---

## 4 可借性校验（BorrowEligibilityChecker）

`checkBorrowable(Long archiveId)` 在申请、审批复校（12.3）、出库复校（12.4）三处调用。任一不满足抛 `BusinessException`（附具体中文原因）：

1. 档案存在且未软删（`NOT_FOUND`）
2. `carrierStatus ∈ {paper, paper_electronic}`，纯电子不可借（`VALIDATION_FAILED`：「纯电子档案不可借阅」）
3. `lifecycleStatus == normal`（`BUSINESS_CONFLICT`：「档案当前不可借阅（生命周期状态异常）」）
4. `loanStatus == available`（`BUSINESS_CONFLICT`：「档案已借出中」）
5. `conditionStatus == normal`（`BUSINESS_CONFLICT`：「档案实体状态异常，暂停借阅」）
6. **未命中 running 盘点任务**（`BUSINESS_CONFLICT`：「档案所在架位/门类正在盘点，暂停借阅」）
7. **无未结束借阅申请**（`BUSINESS_CONFLICT`：「该档案存在未结束的借阅申请」）

### 4.1 盘点拦截 SQL

先解析档案所在架位的 room_id 与档案 category_id，再查 running 盘点任务：

```sql
-- 解析 archive 的 room_id（纸质档案应恰有一个盒位）
SELECT sl.room_id
FROM archive_box_items abi
JOIN archive_boxes ab ON ab.id = abi.box_id
JOIN storage_locations sl ON sl.id = ab.location_id
WHERE abi.archive_id = ? AND abi.deleted_at IS NULL
LIMIT 1;
```

```sql
-- 命中 running 盘点（category 取自 archive，room 取自上一步）
SELECT 1 FROM inventory_tasks
WHERE status = 'running' AND room_id = ? AND category_id = ?
LIMIT 1;
```

边界：档案无盒位（未上架）时，lifecycle 校验已先行拦截（非 normal），此处 room_id 为空则跳过盘点校验。两个查询均经 `JdbcTemplate`，无新增 Mapper。

---

## 5 角色强制（Service 层）

`SaTokenConfig` 已对 `/api/**` 做登录拦截（排除 public/auth/dictionaries）。借阅模块在 Service 入口用 `AuthContext` 做角色**显式**二次校验（已确认采用，借阅涉及实体交接）：

| 操作 | 要求角色 | 不符抛出 |
|------|----------|----------|
| apply / listMine / getMine / exportVoucher | `internal_reader` | `BusinessException(FORBIDDEN)` |
| adminList / adminGet | `back_archivist` 或 `front_archivist` | `BusinessException(FORBIDDEN)` |
| approve | `back_archivist` | `BusinessException(FORBIDDEN)` |
| checkout / returnBorrow | `front_archivist` 或 `back_archivist` | `BusinessException(FORBIDDEN)` |

`getMine` 额外校验 `borrowerId == 当前用户`，否则 `FORBIDDEN`（仅本人可见）。

---

## 6 借阅凭证 PDF

`PdfGenerator.generateBorrowVoucherPdf(borrow, archive, borrower, org)`，基于现有 OpenPDF + `STSong-Light` 字体基础设施（与移交清单/回执同源）。布局（A4，符合业务场景八）：

1. **标题**：档案借阅凭证（居中，18pt 加粗）
2. **凭证信息表**：凭证号 voucherNo、申请号 requestNo、状态 status.displayName、审批时间 approvedAt
3. **借阅人信息表**：姓名 realName、工号 employeeNo、联系电话 phone、部门 departmentName、所在单位 org.orgName
4. **档案信息表**：档号 archive.archiveNo、题名 archive.title、载体状态 carrierStatus.displayName、所属全宗/单位
5. **借阅信息表**：预计天数 expectedDays、预计到馆时间 expectedVisitAt
6. **单位意见盖章区**：预留矩形区域 + 提示文字「借阅人所在单位意见（盖章）」
7. **已归还盖章区**：预留矩形区域 + 提示文字「档案馆归还确认（盖章）」
8. **页脚**：本凭证由系统自动生成；首次导出时生成凭证号，重复导出复用原号

导出端点（11.10）返回 `ResponseEntity<byte[]>`，`Content-Type: application/pdf`，`Content-Disposition: inline; filename="VCH-xxxxxx-借阅凭证.pdf"`。

状态变化：`approved → voucher_issued` 仅在**首次导出且 voucherNo 为空**时触发；之后（含 checked_out 后）重复导出复用原 voucherNo，不改状态。

---

## 7 内部工作台整合（11.1）

`InternalDashboardResponse` 字段类型收紧：

```java
private List<RecentView> recentViews;          // search-liu 已实现，不动
private List<BorrowSummaryItem> myBorrowRequests;   // 我的申请（最近 N 条，全部状态）
private List<BorrowSummaryItem> currentBorrows;     // 当前借阅（status=checked_out，按 dueAt 升序）
private List<BorrowSummaryItem> overdueReminders;   // 逾期（status=checked_out AND dueAt<now）
```

`SearchService.getInternalDashboard(userId)` 改为注入 `BorrowService`，调用：
- `BorrowService.dashboardMine(userId, limit)` → myBorrowRequests
- `BorrowService.dashboardCurrent(userId, limit)` → currentBorrows
- `BorrowService.dashboardOverdue(userId, limit)` → overdueReminders

`limit` 取 5（与 recentViews 数量级一致）。

---

## 8 逾期计算

不落库、不批量改状态（业务场景八明确要求）。`overdue = (status == checked_out && dueAt != null && dueAt.isBefore(now()))`，在响应组装时动态计算：

- 列表（11.8 / 12.1）每条返回 `overdue` 字段。
- 管理端列表支持 `overdue=true` 查询参数过滤。
- 工作台 overdueReminders 复用同一判定。

---

## 9 审计日志

每个状态流转调 `auditService.log("M09", operationType, "borrow_request", id, detail)`：

| 操作 | operationType | detail |
|------|---------------|--------|
| 申请 | apply | {archiveId, requestNo} |
| 审批通过 | approve | {opinion} |
| 审批拒绝 | reject | {rejectReason} |
| 导出凭证 | issue_voucher | {voucherNo, firstIssue} |
| 出库 | checkout | {voucherNo, dueAt} |
| 归还 | return | {returnCheckResult, abnormal} |

审计写入失败不影响主事务（`AuditService` 已 catch + log.error）。

---

## 10 事务边界

- `apply`：单事务（insert borrow_requests）。
- `approve`：单事务（update borrow_requests）。
- `checkout`：**单事务**内 update borrow_requests + update archives.loanStatus；先 `SELECT ... FOR UPDATE`（MyBatis-Plus `selectById` + 版本/状态条件 update 模拟行锁，或 `SELECT FOR UPDATE` 原生 SQL）锁定借阅记录与档案记录，避免并发出库。
- `returnBorrow`：**单事务**内 update borrow_requests + update archives.loanStatus（+ conditionStatus）。
- `exportVoucher`：单事务（首次 update voucherNo/voucherIssuedAt/status）。

---

## 11 错误码使用

| 场景 | ErrorCode |
|------|-----------|
| 档案/申请不存在 | NOT_FOUND |
| 角色/本人校验不通过 | FORBIDDEN |
| 状态流转非法、重复申请、凭证号不匹配、盘点命中、已借出、实体异常 | BUSINESS_CONFLICT |
| 纯电子不可借、参数越界 | VALIDATION_FAILED |

统一经 `BusinessException` 抛出，由 `GlobalExceptionHandler` 转 `R<>`。

---

## 12 测试策略（TDD，先红后绿）

| 测试类 | 覆盖要点 |
|--------|----------|
| `BorrowEligibilityCheckerTest` | 6 条件逐一正反例：载体（纯电子拒绝）、生命周期（非 normal 拒绝）、借阅状态（on_loan 拒绝）、实体状态（damaged 拒绝）、盘点命中（running 拒绝/无则通过）、无盒位边界跳过盘点 |
| `BorrowNoUtilTest` | BRW-%06d、VCH-%06d 格式与序号推进（nextval 递增） |
| `BorrowServiceTest` | 申请全链路、7 状态合法/非法流转、拒绝 opinion 必填、凭证首次生成 vs 重复复用、出库 voucherNo 不匹配拒绝、归还 normal/abnormal 联动 archive 状态、逾期动态计算、角色强制（非 internal_reader 申请抛 FORBIDDEN）、未结束申请拦截、getMine 仅本人、审计写入调用验证 |
| `PdfGeneratorTest`（借阅凭证） | 生成非空 byte[]、含凭证号/标题/双盖章区文字 |

### 12.1 Runtime 全链路验证（必做）

按 runtime-verification 完整性要求，真实起后端服务（Postgres/MinIO 经 docker-compose，ClamAV/外部 API 按需），覆盖登录态 + 完整链路：

1. login(internal_reader, 用户4) → POST 申请借阅 archive 2（paper）→ 201 applied
2. login(back_archivist, 用户2) → POST approve {approved:true} → approved；校验 DB approved_by/approved_at
3. login(internal_reader) → GET voucher（首次）→ 200 PDF，Content-Type 正确，DB status=voucher_issued、voucherNo=VCH-xxxxxx
4. login(internal_reader) → GET voucher（重复）→ 复用同 voucherNo，status 不变
5. login(back_archivist) → POST checkout {voucherNo,dueAt} → checked_out；校验 archives.loan_status=on_loan
6. 同人非法流转：approved 状态再 approve → 409；checked_out 再 checkout → 409
7. login(front_archivist/back_archivist) → POST return {normal} → returned；校验 archives.loan_status=available
8. 异常归还分支（独立构造）：return {damaged} → abnormal_return，archives.condition_status=damaged
9. 盘点拦截：构造 archive 在 running 盘点范围 → 申请 409
10. 角色强制：internal_reader 调 admin 列表 → 403

命令：`./mvnw test`（单测）+ 手动 curl/HTTP 脚本（runtime）。验证结果据实记录，不模拟通过。

---

## 13 模块边界

- **不生成档号**（M05 专属），只读 archive.archiveNo。
- **不修改 archive.loanStatus/conditionStatus 之外的字段**，出库/归还只动这两个。
- **不建 InventoryTaskMapper**，盘点只读 SQL，留给 M12。
- **不走 approval_requests 表**，借阅审批独立。
- 借阅状态变更权唯一归属 M09（模块说明 4.2）。

---

## 14 文件清单总览

新增（15）：
- `enums/BorrowStatus.java`
- `enums/ReturnCheckResult.java`
- `entity/BorrowRequest.java`
- `mapper/BorrowRequestMapper.java`
- `util/BorrowNoUtil.java`
- `dto/request/BorrowApplyRequest.java`
- `dto/request/BorrowApproveRequest.java`
- `dto/request/BorrowCheckoutRequest.java`
- `dto/request/BorrowReturnRequest.java`
- `dto/request/BorrowRequestQuery.java`
- `dto/response/BorrowRequestResponse.java`
- `dto/response/BorrowSummaryItem.java`
- `service/BorrowService.java`
- `service/BorrowEligibilityChecker.java`
- `controller/BorrowController.java`

改动（3）：
- `dto/response/InternalDashboardResponse.java`（占位类型收紧）
- `service/SearchService.java`（dashboard 接入 BorrowService）
- `util/PdfGenerator.java`（新增借阅凭证生成方法）

测试（3）：
- `test/.../BorrowEligibilityCheckerTest.java`
- `test/.../BorrowServiceTest.java`
- `test/.../BorrowNoUtilTest.java`（+ PdfGenerator 借阅凭证用例并入既有或新增）
