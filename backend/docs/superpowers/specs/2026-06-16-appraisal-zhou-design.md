# feat/appraisal-zhou 设计规格

- 分支：`feat/appraisal-zhou`（从最新 `develop` 拉出）
- 对应模块：M10 档案鉴定、M11 档案销毁、M08 审批工作台、M14 用户/角色/系统配置
- 接口规范：[doc/接口文档.md](../../../doc/接口文档.md) 第 13、14、15、22 章
- 数据库：[doc/数据库设计.md](../../../doc/数据库设计.md) 第 8 章（审批/鉴定/销毁）、第 11.3 节（system_configs）、第 6.4 节（business_attachments）、第 2~3 章（用户/角色/组织）
- 业务场景：[doc/业务场景.md](../../../doc/业务场景.md) 鉴定销毁闭环、安全开放闭环、用户与系统配置

## 1 实现范围

### 1.1 本次实现（24 个接口 + 1 个序列迁移）

| # | 方法 | 路径 | 角色 | 说明 |
|---|------|------|------|------|
| 13.1 | GET | `/api/admin/approvals` | director | 分页查询审批单，按 approvalType/status/keyword 过滤 |
| 13.2 | GET | `/api/admin/approvals/{approvalId}` | director | 审批详情，含目标对象、凭证档案、调整前后值、清册快照 |
| 13.3 | POST | `/api/admin/approvals/{approvalId}/approve` | director | 审批通过，按类型生效（密级/开放/销毁） |
| 13.4 | POST | `/api/admin/approvals/{approvalId}/reject` | director | 审批退回，退回意见必填 |
| 14.1 | GET | `/api/admin/appraisal-batches` | back_archivist | 分页查询鉴定批次 |
| 14.2 | POST | `/api/admin/appraisal-batches` | back_archivist | 创建鉴定批次，命中到期未销毁档案 |
| 14.3 | GET | `/api/admin/appraisal-batches/{batchId}` | back_archivist | 批次详情（命中档案 + 鉴定明细） |
| 14.4 | PUT | `/api/admin/appraisal-batches/{batchId}/items` | back_archivist | 保存鉴定明细，仅 draft 可改 |
| 14.5 | POST | `/api/admin/appraisal-batches/{batchId}/complete` | back_archivist | 完成鉴定，extend 更新期限/destroy 生成销毁清册 |
| 15.1 | GET | `/api/admin/destruction-lists` | back_archivist、director | 分页查询销毁清册 |
| 15.2 | GET | `/api/admin/destruction-lists/{listId}` | back_archivist、director | 清册详情（明细快照 + 审批单 + 照片附件） |
| 15.3 | POST | `/api/admin/destruction-lists/{listId}/submit-approval` | back_archivist | 提交销毁审批，创建 destruction 审批单 |
| 15.4 | POST | `/api/admin/destruction-lists/{listId}/photos` | back_archivist | 上传销毁现场照片（ClamAV+MinIO+SHA-256） |
| 15.5 | POST | `/api/admin/destruction-lists/{listId}/destroy` | back_archivist | 确认销毁，档案置 destroyed、解除盒关系 |
| 22.1 | GET | `/api/admin/users` | sys_admin | 分页查询用户（角色/单位/数据范围/密级上限） |
| 22.2 | POST | `/api/admin/users` | sys_admin | 创建用户（登录名唯一、角色校验） |
| 22.3 | GET | `/api/admin/users/{userId}` | sys_admin | 用户详情（含最近操作摘要） |
| 22.4 | PUT | `/api/admin/users/{userId}` | sys_admin | 更新用户（不含密码） |
| 22.5 | PUT | `/api/admin/users/{userId}/status` | sys_admin | 启用/禁用，不可禁用最后一个 sys_admin |
| 22.6 | POST | `/api/admin/users/{userId}/reset-password` | sys_admin | 重置内部用户密码 |
| 22.7 | GET | `/api/admin/roles` | sys_admin | 预设角色列表 |
| 22.8 | GET | `/api/admin/system-configs` | sys_admin | 配置项列表，敏感密钥不返回 |
| 22.9 | PUT | `/api/admin/system-configs/{configKey}` | sys_admin | 更新单项配置（editable+valueType 校验） |
| 22.10 | PUT | `/api/admin/system-configs` | sys_admin | 批量更新配置 |

迁移：`V17__add-appraisal-batch-sequence.sql` 新增 `seq_appraisal_batch_no` 序列（编号规范要求 `APP-{自增序号}`，V14 缺该序列；`START WITH` 取值避开演示数据 `APP-000001`，与现有序列风格一致）。

### 1.2 本次不实现

- **§10.4/10.5 密级/开放调整审批的「发起」端**：已由 M05 `ArchiveService`（archive-zhou）实现，创建 `approval_requests` 记录。本任务只实现审批工作台的「受理」端（13.1-13.4）与销毁审批的「发起」（15.3），不重写发起端。
- **逐按钮自定义授权**：文档明确本期不做，角色为预设。
- **编研/统计/研判/保存/盘点**（§18-21、§17 全宗管理）：属 `feat/stats-liu`，不在本任务。
- **审计日志查询接口**（§23.1/23.2）：表与 `AuditService` 已存在，查询端点归属待定，本期仅在用户详情（22.3）内部聚合「最近操作」，不暴露独立审计查询接口。

### 1.3 鉴定→销毁闭环契约（本任务打通的关键闭环）

```
14.2 创建鉴定批次  → 命中到期未销毁档案，生成 appraisal_items（appraisal_result 待填）
14.4 保存鉴定明细  → 填写 extend/destroy 结论（仅 draft）
14.5 完成鉴定      → extend: 更新档案保管期限+写 change_log
                     destroy: 档案 lifecycle=pending_destruction，生成 destruction_list(draft)+明细快照
15.3 提交销毁审批  → list draft→pending_approval，创建 approval_requests(destruction)
13.3 审批通过      → approval pending→approved；destruction: list pending_approval→pending_destroy
                     （security/open_adjust: 生效档案字段+change_log）
15.5 确认销毁      → list pending_destroy→destroyed；档案 lifecycle/condition=destroyed；
                     电子文件按策略删除；纸质解除盒关系、盒数 -n
```

`approval_requests` 实体/Mapper 已存在（archive-zhou），本任务新增审批受理 Service/Controller 与销毁审批发起逻辑。

## 2 文件结构

### 2.1 新增 Entity

| 文件 | 表 | 说明 |
|------|----|------|
| `entity/AppraisalBatch.java` | appraisal_batches | 鉴定批次（继承 BaseEntity） |
| `entity/AppraisalItem.java` | appraisal_items | 鉴定明细 |
| `entity/DestructionList.java` | destruction_lists | 销毁清册 |
| `entity/DestructionItem.java` | destruction_items | 销毁明细（含档号/题名等快照） |
| `entity/SystemConfig.java` | system_configs | 系统配置项 |
| `entity/BusinessAttachment.java` | business_attachments | 业务附件（销毁照片复用） |

> `ApprovalRequest`、`User`、`Role`、`UserRole`、`Organization` 已存在，直接复用。

### 2.2 新增 Mapper（均继承 `BaseMapper`，聚合查询用 `JdbcTemplate`）

`AppraisalBatchMapper`、`AppraisalItemMapper`、`DestructionListMapper`、`DestructionItemMapper`、`SystemConfigMapper`、`BusinessAttachmentMapper`。`ApprovalRequestMapper` 已存在，复用。

### 2.3 新增 Enum

| 文件 | 值 |
|------|----|
| `enums/AppraisalBatchStatus.java` | `draft`、`completed` |
| `enums/AppraisalResult.java` | `extend`、`destroy` |
| `enums/DestructionListStatus.java` | `draft`、`pending_approval`、`pending_destroy`、`destroyed` |
| `enums/DestroyMethod.java` | `shredding`、`burning`、`entrusted` |
| `enums/ConfigValueType.java` | `string`、`number`、`boolean`、`json` |

> `ApprovalType`(security_adjust/open_adjust/destruction)、`ApprovalStatus`(pending/approved/rejected)、`LifecycleStatus`、`SecurityLevel`、`OpenStatus`、`RetentionPeriod`、`UserStatus`、`RoleCode`、`UserType` 均已存在，复用。

### 2.4 新增 Util

| 文件 | 说明 |
|------|------|
| `util/AppraisalNoUtil.java` | 仿 `ArchiveNoUtil`，用 `seq_appraisal_batch_no` 生成 `APP-{6位序号}` |
| `util/DestructionNoUtil.java` | 用 `seq_destruction_list_no` 生成 `DES-{6位序号}` |

### 2.5 新增 / 改造 Service

| 文件 | 说明 |
|------|------|
| `service/AppraisalService.java` | 鉴定批次创建/查询/明细保存/完成（生成销毁清册） |
| `service/DestructionService.java` | 销毁清册查询/详情/提交审批/上传照片/确认销毁 |
| `service/ApprovalService.java` | 审批查询/详情/通过/退回；按类型编排生效（密级/开放/销毁） |
| `service/SystemConfigService.java` | 配置查询（脱敏）/单更/批量更（editable+valueType 校验） |
| `service/RoleService.java` | 预设角色列表 |
| `service/UserService.java`（改造） | 迁移到 `/api/admin/users`，补全 22.1 过滤/22.3 详情/22.5 启用禁用 |

> 密码哈希、角色绑定、登录名唯一校验复用现有 `UserService` 已有逻辑，按 §22 字段扩展。

### 2.6 新增 / 改造 Controller

| 文件 | 基路径 | 端点数 |
|------|--------|--------|
| `controller/AppraisalController.java` | `/api/admin/appraisal-batches` | 5 |
| `controller/DestructionController.java` | `/api/admin/destruction-lists` | 5 |
| `controller/ApprovalController.java` | `/api/admin/approvals` | 4 |
| `controller/SystemConfigController.java` | `/api/admin/system-configs` | 3 |
| `controller/RoleController.java` | `/api/admin/roles` | 1 |
| `controller/UserController.java`（改造） | `/api/users` → `/api/admin/users` | 扩展至 6 |

### 2.7 新增 DTO（请求 / 响应）

请求（`dto/request/`）：
- `AppraisalBatchCreateRequest`：batchName、categoryId、formedYearStart、formedYearEnd
- `AppraisalItemSaveRequest`：items[]{archiveId、appraisalResult、newRetentionPeriod、opinion}
- `DestructionSubmitRequest`：reason
- `DestructionDestroyRequest`：destroyMethod、supervisorName1、supervisorName2、destroyNote
- `ApprovalOpinionRequest`：opinion
- `SystemConfigUpdateRequest`：configValue
- `SystemConfigBatchUpdateRequest`：items[]{configKey、configValue}
- `UserStatusRequest`：status、reason
- `UserCreateRequest`（扩展）、`UserUpdateRequest`（扩展）：补 userType/organizationId/departmentName/maxSecurityLevel/dataScope/roleCodes 等

响应（`dto/response/`）：
- `AppraisalBatchResponse` / `AppraisalBatchDetailResponse`（含命中档案 + 明细）
- `DestructionListResponse` / `DestructionListDetailResponse`（含明细快照 + 审批 + 照片）
- `ApprovalResponse` / `ApprovalDetailResponse`（含目标摘要 + 调整前后值）
- `SystemConfigResponse`
- `RoleResponse`
- `UserInfoResponse`（扩展：角色/单位/数据范围/密级上限 + 最近操作摘要）

## 3 核心业务逻辑

### 3.1 创建鉴定批次（14.2）— 命中到期档案

1. 校验 `formedYearStart <= formedYearEnd`（均非空时），否则 `VALIDATION_FAILED`。
2. `AppraisalNoUtil.generate()` 生成 `batch_no`。
3. `@Transactional`：insert `appraisal_batches`（status=draft）。
4. 命中查询：`archives` 满足 `retention_until IS NOT NULL AND retention_until < 当前日期 AND lifecycle_status NOT IN ('destroyed') AND (category_id = ? 可选) AND (formed_year BETWEEN start AND end 可选)`，用 `JdbcTemplate` 聚合查询。
5. 为每条命中档案 insert `appraisal_items`（archive_id、appraisal_result 待填、appraised_by 留空）。
6. 返回批次 + 命中档案明细。

### 3.2 保存鉴定明细（14.4）

1. 校验批次 `status=draft`，否则 `BUSINESS_CONFLICT`。
2. 校验 items 非空，每条 `appraisalResult ∈ {extend,destroy}`；`extend` 必须带 `newRetentionPeriod`。
3. 校验 archiveId 均属于该批次命中集合，否则 `VALIDATION_FAILED`。
4. 批量 upsert `appraisal_items`（appraisal_result、new_retention_period、new_retention_until（按保管期限推算）、opinion、appraised_by=当前用户、appraised_at=now）。

### 3.3 完成鉴定（14.5）— 闭环核心，事务

1. 校验批次 `status=draft`；所有命中明细均已填 `appraisal_result`，否则 `VALIDATION_FAILED`。
2. `@Transactional`：
   - **extend** 明细：更新 `archives.retention_period`、按期限重算 `retention_until`；写 `archive_change_logs`（field=retention_period，old/new）。
   - **destroy** 明细：`archives.lifecycle_status=pending_destruction`。
   - 若存在 destroy 明细：生成一条 `destruction_lists`（list_no=`DES-`、list_name=批次名、appraisal_batch_id、status=draft）；为每条 destroy 明细生成 `destruction_items` 快照（archive_no/title/category/page_count/retention/security_level/appraisal_opinion 快照、file_delete_status=not_started）。
   - 批次 `status=draft→completed`、`completed_at=now`。
3. 审计 `auditService.log("M10","complete_appraisal","appraisal_batch",batchId,…)`。

### 3.4 提交销毁审批（15.3）

1. 校验清册 `status=draft`、明细非空、明细档案均为 `pending_destruction`，否则 `BUSINESS_CONFLICT`/`VALIDATION_FAILED`。
2. `@Transactional`：清册 `status=draft→pending_approval`；创建 `approval_requests`（approval_type=destruction、target_type=destruction_list、target_id=listId、reason、submitted_by/at、status=pending）；回写 `destruction_lists.approval_request_id`。
3. 审计。

### 3.5 审批通过（13.3）— 跨模块生效，事务（Approach A）

1. 校验审批单 `status=pending`，否则 `BUSINESS_CONFLICT`；校验目标对象当前状态仍允许生效（档案未销毁 / 清册仍 pending_approval），否则 `BUSINESS_CONFLICT`。
2. `@Transactional`：
   - 审批单 `pending→approved`，写 `approved_by/approved_at/approval_opinion`。
   - 按 `approval_type` 生效：
     - `security_adjust`：`archives.security_level=parse(newValue)`；写 `archive_change_logs`（field=security_level）。
     - `open_adjust`：`archives.open_status=parse(newValue)`；写 `archive_change_logs`（field=open_status）。
     - `destruction`：`destruction_lists.status=pending_destroy`。
   - 审计 `auditService.log("M08","approve","approval_request",approvalId,…)`。
3. 凭证档案校验（密级/开放）由发起端（M05）在创建审批单时完成；本端不再重复校验来源关系，仅校验目标当前可生效。

### 3.6 审批退回（13.4）

1. 校验 `opinion` 非空（`@NotBlank`），否则 `VALIDATION_FAILED`。
2. 校验审批单 `status=pending`。
3. `@Transactional`：审批单 `pending→rejected`；`destruction` 类型清册 `pending_approval→draft`（可重新编辑/再提交）；密级/开放目标字段不变。
4. 审计。

### 3.7 上传销毁现场照片（15.4）— 复用上传扫描链路

1. 校验清册存在。
2. 对每个文件：校验扩展名/MIME/大小（取 `system_configs` 的 `upload.allowed_extensions`、`upload.max_file_size_mb`）；算 SHA-256；ClamAV 扫描（不可用/超时/失败/命中病毒 → 拒绝并仅写 audit，不创建附件，复用 `StagingFileService` 同款策略）。
3. 上传 MinIO（对象 key 带 `destruction/{listId}/` 前缀）；insert `business_attachments`（business_type=destruction_list、business_id=listId、attachment_type=destruction_photo、sha256、size、mime、object_key）。
4. 返回附件列表。失败文件不阻断整体（已成功项保留），但任一文件命中病毒则该文件拒绝；具体策略对齐 `StagingFileService`。

### 3.8 确认销毁（15.5）— 闭环终点，事务

1. 校验清册 `status=pending_destroy`、已审批通过；`destroyMethod`/`supervisorName1`/`supervisorName2` 必填，否则 `VALIDATION_FAILED`。
2. `@Transactional`：
   - 清册 `pending_destroy→destroyed`，写 `destroyed_at/destroy_method/supervisor_name_1/2/destroy_note`。
   - 每条明细档案：`archives.lifecycle_status=destroyed`、`condition_status=destroyed`。
   - 电子文件按策略：`archive_files` 标记删除或物理删除对象（本期策略：置 `archive_files.status=deleted` 并记录，对象保留可追溯，`destruction_items.file_delete_status=deleted/file_deleted_at=now`）；具体对齐项目保存策略。
   - 纸质：解除 `archive_box_items` 关系，对应 `archive_boxes.used_count -= n`（盒内清空则盒可释放，本期不主动改盒 status）。
   - 销毁清册/明细/审批/审计/档案元数据永久保留（不删除）。
3. 审计 `auditService.log("M11","destroy","destruction_list",listId,…)`。

### 3.9 用户管理（22.1-22.6）

- 22.1 查询：`QueryWrapper` 拼 userType/roleCode（join user_roles）/organizationId/status/keyword（login_name/real_name/employee_no 模糊），分页；每条回填角色码列表、组织名、dataScope、maxSecurityLevel。
- 22.2 创建：复用现有 `createUser`，补字段；校验 loginName 唯一、roleCodes 存在且启用、organizationId 存在；密码哈希复用现有编码器；写 user + user_roles；审计。
- 22.3 详情：用户 + 角色 + 组织 + 最近操作（查 `audit_logs` where actor_user_id=? limit N）。
- 22.4 更新：除密码外字段 + roleCodes；角色/状态变更写审计。
- 22.5 启用/禁用：禁用时校验「非最后一个启用的 sys_admin」（查 `user_roles`+`users.status`）；写审计含 reason。
- 22.6 重置密码：哈希新密码；审计。

### 3.10 系统配置（22.8-22.10）

- 22.8 查询：返回全部 `system_configs`，**敏感密钥脱敏**（config_key 含 `key`/`secret`/`password`/`token` 的项不返回 value 或返回 `***`），value/valueType/editable/description 正常返回。
- 22.9 单更：校验 `editable=true`（否则 `BUSINESS_CONFLICT`）；按 `valueType` 校验 value（number 可解析、boolean∈{true,false}、json 可解析、string 任意）；更新 + 审计。
- 22.10 批更：逐项同 22.9 校验，整体事务，任一失败回滚。

## 4 关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 审批生效编排 | Approach A：`ApprovalService` 单事务内直接窄变更 Archive/DestructionList + 写 change_log + audit | 与 Borrow/Warehouse 跨表事务一致；原子、可测、无循环依赖；事件驱动过度设计（YAGNI） |
| 鉴定生成销毁清册 | 14.5 完成鉴定时，若有 destroy 明细则自动生成一条 destruction_list(draft) | 文档 14.5 明确；清册与批次 1:1（appraisal_batch_id） |
| 销毁明细快照 | 完成鉴定时即写 destruction_items 全字段快照 | 文档 8.5：不依赖档案主表后续字段 |
| 电子文件销毁策略 | 置 archive_files.status=deleted + 记录，对象保留 | 永久保留要求；物理删除不可逆，本期保守标记 |
| 用户管理路径 | 迁移 `/api/users`→`/api/admin/users` 并扩展 | 接口文档权威约定；前端零调用方，无破坏；同步更新鉴权配置/测试引用 |
| 敏感配置脱敏 | key 含 key/secret/password/token 不返回 value | 文档 22.8「不返回敏感密钥」 |
| 最后 sys_admin 保护 | 禁用时查 user_roles+status 计数 | 文档 22.5 硬约束 |
| 聚合查询 | JdbcTemplate | 与 ArchiveNoUtil/WarehouseService 同款，不引入 XML Mapper |
| 编号序列 | 新增 V17 `seq_appraisal_batch_no`；销毁复用 V14 `seq_destruction_list_no` | 编号规范要求 APP-/DES- |

## 5 依赖关系

### 5.1 依赖已有（develop 已具备）

- 实体/Mapper：`ApprovalRequest(+Mapper)`、`User/UserMapper`、`Role/RoleMapper`、`UserRole/UserRoleMapper`、`Organization/OrganizationMapper`、`Archive(+Mapper)`、`ArchiveFile(+Mapper)`、`ArchiveBox(+Mapper)`、`ArchiveBoxItem(+Mapper)`、`AuditLog(+Mapper)`、`ArchiveChangeLog(+Mapper)`。
- 序列：`seq_destruction_list_no`（V14）；通用：`R`、`PageResult`、`PageRequest`、`ErrorCode`、`BusinessException`、`AuthContext`、`AuditService`、`BaseEntity`。
- 上传链路：`MinioService`、ClamAV 客户端（`StagingFileService` 同款）、SHA-256 工具。
- 鉴权：SaToken，路由鉴权与 `WarehouseController`/`PendingArchiveController` 一致。

### 5.2 实现时需对齐现有代码（低风险读取后复用）

- 现有 `UserService.createUser/updateUser/resetPassword` 的密码哈希、角色绑定、登录名唯一校验逻辑（扩展而非重写）。
- 现有 `ArchiveService` 密级/开放调整审批**发起**逻辑（10.4/10.5），确保审批单字段（target_type/evidence_archive_id/old_value/new_value）与 13.x 受理端一致。
- `StagingFileService` 的 ClamAV/MinIO/SHA-256 上传与拒绝策略，销毁照片复用。
- `/api/users` 在 `SaTokenConfig`/测试中的引用，迁移后同步更新。

### 5.3 不依赖未完成模块

- 不依赖 M12 盘点、M13 编研、M15 统计研判、M16 保存、M17 全宗管理的对外接口。

## 6 错误处理

| 场景 | ErrorCode |
|------|-----------|
| 鉴定批次/清册/审批单/用户/配置不存在 | NOT_FOUND |
| 批次非 draft 仍保存明细/完成 | BUSINESS_CONFLICT |
| 明细未全部填写结论即完成 | VALIDATION_FAILED |
| 清册非 draft 提交审批、明细档案非 pending_destruction | BUSINESS_CONFLICT |
| 审批单非 pending 仍审批/退回；目标已不可生效 | BUSINESS_CONFLICT |
| 退回意见为空 | VALIDATION_FAILED |
| 清册非 pending_destroy 确认销毁；监销人/方式缺失 | BUSINESS_CONFLICT / VALIDATION_FAILED |
| 销毁照片格式/大小/MIME 非法；ClamAV 拒绝 | UNSUPPORTED_MEDIA_TYPE / PAYLOAD_TOO_LARGE / VALIDATION_FAILED |
| 登录名重复；角色不存在/禁用 | BUSINESS_CONFLICT / VALIDATION_FAILED |
| 禁用最后一个 sys_admin | BUSINESS_CONFLICT |
| 配置项不可编辑（editable=false） | BUSINESS_CONFLICT |
| 配置值 valueType 校验失败（非数字/非布尔/非 JSON） | VALIDATION_FAILED |

所有写操作同事务完成；闭环关键步骤（完成鉴定/提交审批/审批通过/确认销毁）失败整体回滚。

## 7 测试方案（严格 TDD）

风格仿 `WarehouseServiceTest`/`BorrowServiceTest`：JUnit5 + Mockito + AssertJ，mock 全部 Mapper + JdbcTemplate + Util + AuditService + MinioService，纯单测。

- `AppraisalServiceTest`：创建命中查询正确；保存明细状态/字段校验；完成鉴定 extend 更新期限+写 change_log、destroy 置 pending_destruction+生成清册快照；状态冲突抛错。
- `DestructionServiceTest`：提交审批创建 approval_request 并回写；确认销毁状态机+档案置 destroyed+盒关系解除+used_count 递减；照片上传 ClamAV 拒绝路径。
- `ApprovalServiceTest`：通过按类型生效（security/open/destruction 三分支）+ 写 change_log + 审计；退回销毁清册回 draft；目标不可生效抛错。
- `SystemConfigServiceTest`：敏感脱敏；editable=false 拒绝；valueType 校验（number/boolean/json 各正反例）；批量整体事务。
- `UserServiceTest`（扩展）：查询过滤；禁用最后 sys_admin 抛错；创建 loginName 重复抛错；详情含最近操作。
- `AppraisalNoUtilTest`/`DestructionNoUtilTest`：序列格式断言。

每个端点核心分支先写测试（红）→ 实现（绿）→ 重构。验证 = `mvn test` 全绿 + `mvn compile`；若本机 Postgres/MinIO/ClamAV 可用，追加 runtime 冒烟（Swagger 触发闭环），记录到 `backend/docs/superpowers/runtime/`。
