# feat/archive-zhou 设计规格

分支：`feat/archive-zhou`，从 `develop` 拉出。
对应模块：M05 入库与档案管理（P0）。
对应接口文档：第 9 章（待入库与上架）、第 10 章（档案管理）。

---

## 1 实现范围

### 1.1 本次实现（16 个接口）

| 接口 | 方法 | 路径 | Controller | Service |
|------|------|------|-----------|---------|
| 9.1 查询待入库批次 | GET | /api/admin/pending-archive/batches | PendingArchiveController | PendingArchiveService |
| 9.2 获取入库批次详情 | GET | /api/admin/pending-archive/batches/{batchId} | PendingArchiveController | PendingArchiveService |
| 9.6 确认条目入库字段 | PUT | /api/admin/pending-archive/items/{itemId}/confirmation | PendingArchiveController | PendingArchiveService |
| 9.7 确认入库 | POST | /api/admin/pending-archive/items/{itemId}/archive | PendingArchiveController | PendingArchiveService |
| 9.8 批次确认上架 | POST | /api/admin/pending-archive/batches/{batchId}/shelve | PendingArchiveController | PendingArchiveService |
| 10.1 管理端查询档案 | GET | /api/admin/archives | ArchiveController | ArchiveService |
| 10.2 获取档案详情 | GET | /api/admin/archives/{archiveId} | ArchiveController | ArchiveService |
| 10.3 编辑非受保护元数据 | PUT | /api/admin/archives/{archiveId} | ArchiveController | ArchiveService |
| 10.4 发起密级调整审批 | POST | /api/admin/archives/{archiveId}/security-adjustments | ArchiveController | ArchiveService |
| 10.5 发起开放调整审批 | POST | /api/admin/archives/{archiveId}/open-adjustments | ArchiveController | ArchiveService |
| 10.6 档案文件预览 | GET | /api/admin/archive-files/{fileId}/preview | ArchiveFileController | ArchiveFileService |
| 10.7 档案文件下载 | GET | /api/admin/archive-files/{fileId}/download | ArchiveFileController | ArchiveFileService |
| 10.8 删除或作废档案文件 | DELETE | /api/admin/archive-files/{fileId} | ArchiveFileController | ArchiveFileService |

### 1.2 AI 补全接口空壳（返回 501）

| 接口 | 方法 | 路径 |
|------|------|------|
| 9.3 启动 AI 补全 | POST | /api/admin/pending-archive/batches/{batchId}/ai-completion |
| 9.4 查询 AI 补全任务 | GET | /api/admin/ai-tasks/{taskId} |
| 9.5 重试失败 AI 批次 | POST | /api/admin/ai-tasks/{taskId}/retry-failed |

### 1.3 本次不实现

- M06 全宗与组织 CRUD（接口文档第 17 章）
- M07 库房管理 CRUD（接口文档第 16 章），本次只做入库时的盒号/架位预占查询
- M10 审批工作台的审批方操作（通过/退回），本次只做审批请求的创建
- AI 补全的完整实现（刘星 feat/search-liu）

---

## 2 文件结构

### 2.1 新增 Entity

| 文件 | 对应表 | 说明 |
|------|--------|------|
| Archive.java | archives | 正式档案主表，继承 BaseEntity |
| ArchiveFile.java | archive_files | 正式电子文件 |
| Tag.java | tags | 标签字典 |
| ArchiveTag.java | archive_tags | 档案-标签关系（复合主键 archive_id + tag_id） |
| ArchiveChangeLog.java | archive_change_logs | 档案字段变更日志 |
| ApprovalRequest.java | approval_requests | 审批单（10.4/10.5 创建用） |
| WarehouseRoom.java | warehouse_rooms | 库房（只读查询，入库时展示可选库房） |
| StorageLocation.java | storage_locations | 架位（只读查询） |
| ArchiveBox.java | archive_boxes | 档案盒（入库预占用 + used_count 更新） |
| ArchiveBoxItem.java | archive_box_items | 盒内档案关系 |

### 2.2 新增 Enum

| 文件 | 值 |
|------|----|
| LifecycleStatus.java | pending_shelf, normal, pending_destruction, destroyed |
| LoanStatus.java | available, on_loan |
| ConditionStatus.java | normal, damaged, repairing, lost, destroyed |
| OpenStatus.java | open, closed |
| FileRole.java | original, scan, compilation_body, signature |
| ApprovalType.java | security_adjust, open_adjust, destruction |
| ApprovalStatus.java | pending, approved, rejected |
| FileStatus.java | pending, normal, failed, deleted |
| UsabilityResult.java | pending, passed, failed |

### 2.3 新增 Mapper

| 文件 | 说明 |
|------|------|
| ArchiveMapper.java | 档案 CRUD |
| ArchiveFileMapper.java | 正式文件 CRUD |
| TagMapper.java | 标签查询 |
| ArchiveTagMapper.java | 档案-标签关系 |
| ArchiveChangeLogMapper.java | 变更日志 |
| ApprovalRequestMapper.java | 审批单 |
| WarehouseRoomMapper.java | 库房只读 |
| StorageLocationMapper.java | 架位只读 |
| ArchiveBoxMapper.java | 档案盒（预占、used_count 更新） |
| ArchiveBoxItemMapper.java | 盒内档案关系 |

### 2.4 新增 Service

| 文件 | 职责 | 预估行数 |
|------|------|----------|
| PendingArchiveService.java | 待入库批次列表、批次详情、确认字段、确认入库、上架 | ~450 |
| ArchiveService.java | 档案列表、详情、元数据编辑、密级/开放调整审批发起 | ~400 |
| ArchiveFileService.java | 文件预览/下载/作废、暂存转正式复制 | ~250 |

### 2.5 新增 Controller

| 文件 | 路径前缀 |
|------|----------|
| PendingArchiveController.java | /api/admin/pending-archive |
| ArchiveController.java | /api/admin/archives |
| ArchiveFileController.java | /api/admin/archive-files |

### 2.6 新增 DTO

Request：

| 文件 | 用途 |
|------|------|
| ItemConfirmationRequest.java | 9.6 确认条目入库字段 |
| ArchiveRequest.java | 9.7 确认入库（fondsId, boxId, locationId, sortNo, pageCount） |
| BatchShelveRequest.java | 9.8 批次上架（note） |
| ArchiveUpdateRequest.java | 10.3 编辑元数据（title, responsibleText, formedDate, categoryId, fondsId, tagNames, changeReason） |
| SecurityAdjustRequest.java | 10.4 密级调整（newSecurityLevel, evidenceArchiveNo, reason） |
| OpenAdjustRequest.java | 10.5 开放调整（newOpenStatus, evidenceArchiveNo, reason） |
| ArchiveFileDeleteRequest.java | 10.8 文件作废（reason） |

Response：

| 文件 | 用途 |
|------|------|
| PendingBatchResponse.java | 9.1 待入库批次列表项 |
| PendingBatchDetailResponse.java | 9.2 批次详情（含条目列表、AI 建议、暂存文件、可用盒号/架位） |
| PendingItemResponse.java | 条目详情 |
| ArchiveResponse.java | 档案列表/详情 |
| ArchiveFileResponse.java | 文件信息 |
| ArchiveResultResponse.java | 9.7 入库结果（archiveId, archiveNo, lifecycleStatus） |

### 2.7 新增 Util

| 文件 | 职责 |
|------|------|
| ArchiveNoUtil.java | 调用 seq_archive_no 序列生成 ARC-{6位序号} |

---

## 3 核心业务逻辑

### 3.1 确认入库（9.7）— 最复杂的事务

事务入口：`PendingArchiveService.confirmArchive(itemId, request)`

步骤：

1. **校验**：查询 intake_item，校验 status = pending_archive、generatedArchiveId 为空。查询 intake_batch 确认来源。
2. **读暂存文件**：查询 staging_files 中 batch_id + item_id 且 match_status = matched 的记录。
3. **生成档号**：调用 `ArchiveNoUtil.generate()`，使用 `seq_archive_no` 序列。
4. **创建 archives 记录**：
   - 字段来源优先级：confirmed 字段 > input 字段 > 条目字段
   - title：confirmedTitle ?? inputTitle
   - responsible_text：confirmedResponsibleText ?? 空
   - formed_date：confirmedFormedDate ?? formedDate
   - formed_year：从 formed_date 提取年份
   - category_id：confirmedCategoryId ?? 空
   - carrier_status：从条目继承
   - source_type/source_batch_id/source_item_id：从批次和条目继承
   - organization_id：从批次继承
   - retention_period：从条目继承
   - security_level/open_status/allow_digitization：从条目继承
   - lifecycle_status：纯电子 → normal，纸质相关 → pending_shelf
   - archived_at：now()
   - 计算 retention_until：10y 加 10 年，30y 加 30 年，permanent 为空
5. **复制暂存文件为正式 archive_files**（补偿口径，见 3.3）
6. **纸质相关档案**：
   - 校验 boxId 和 locationId 非空
   - 查询 archive_box，校验同盒 category_id、fonds_id 一致
   - 创建 archive_box_items 记录
   - 更新 archive_boxes.used_count += 1，若达 capacity 则 status = full
7. **处理标签**：从 confirmedTags 读取标签名数组，查找或创建 tags 记录，写入 archive_tags
8. **回写清单**：`intake_items.generatedArchiveId = archive.id`，`status = archived`
9. **重算批次状态**：调用已有逻辑（所有条目 archived → 批次 archived，部分 → partially_received 等）
10. **审计日志**：写入 audit_logs

### 3.2 档号生成（ArchiveNoUtil）

```java
@Component
public class ArchiveNoUtil {
    private final JdbcTemplate jdbc;
    public String generate() {
        Long seq = jdbc.queryForObject("SELECT nextval('seq_archive_no')", Long.class);
        return String.format("ARC-%06d", seq);
    }
}
```

与现有 generateBatchNo() 模式一致。

### 3.3 暂存转正式文件（ArchiveFileService.stagingToFormal）

入库事务内的同步操作，整体失败时由 `@Transactional` 回滚：

1. 创建 `archive_files` 记录：
   - archive_id = 新档案 ID
   - file_role = original
   - bucket_name = 正式 bucket
   - object_key = `archive-files/{archiveNo}/{archiveFileId}/{fileName}`
   - file_status = normal（事务内直接设为 normal，失败整体回滚）
   - 复制 staging_files 的 sha256、file_size、mime_type、file_ext、original_filename、scan_result
2. 调用 MinioService 复制对象（从暂存路径到正式路径）
3. 更新 staging_files.archived_file_id = archive_files.id，match_status = archived
4. 如果 MinIO 复制失败，抛出异常触发整个入库事务回滚（archives、archive_files、archive_box_items 全部撤销），不需要孤立的 failed 记录或重试机制

### 3.4 上架确认（9.8）

条件：
- 批次所有已接收条目已入库（status = archived）
- 批次包含至少一个纸质相关档案（carrier_status in paper_electronic, paper）
- 批次当前 status = archived

操作：
- 查询该批次关联的所有 lifecycle_status = pending_shelf 的 archives
- 批量更新 lifecycle_status = normal，shelved_at = now()
- 批次 status = shelved，shelved_at = now()
- 写入 audit_logs

不重新选择架位，只确认实物已放入预占架位。

### 3.5 确认条目入库字段（9.6）

- 校验 item.status = accepted
- 更新 confirmedTitle、confirmedResponsibleText、confirmedFormedDate、confirmedCategoryId、confirmedTags
- item.status = pending_archive
- 不得修改 securityLevel、retentionPeriod、openStatus、allowDigitization 等受保护字段
- 受保护字段校验在 Request DTO 层面排除这些字段

### 3.6 编辑非受保护元数据（10.3）

- 允许编辑：title、responsibleText、formedDate、categoryId、fondsId、tagNames
- 禁止编辑：archiveNo、securityLevel、retentionPeriod、openStatus、allowDigitization、lifecycleStatus
- 已销毁档案（lifecycleStatus = destroyed）禁止编辑
- 写入 archive_change_logs：逐字段比较新旧值，只记录实际变更的字段
- 写入 audit_logs
- tagNames 更新：先删除旧 archive_tags，再按新 tagNames 查找/创建 tags 并写入

### 3.7 密级/开放调整审批发起（10.4、10.5）

- 创建 ApprovalRequest 记录
- 校验同一档案同类审批只能有一个 pending
- 凭证档案必须存在、未销毁（lifecycle_status != destroyed），且 organization_id 或 fonds_id 与目标档案匹配
- 提交后目标档案字段不立即变化，等审批通过后由审批模块修改
- 写入 audit_logs

---

## 4 关键设计决策

| 决策点 | 选择 | 原因 |
|--------|------|------|
| 架构 | 3 Service + 3 Controller | 按职责拆分，与接口文档章节对齐 |
| 档号生成 | 独立 ArchiveNoUtil | 与 generateBatchNo 模式一致，唯一生成点 |
| 暂存转正式 | 入库事务内直接复制，失败整体回滚 | 保持事务一致性，避免孤立 failed 记录 |
| 审批发起 | 只创建 ApprovalRequest，不实现审批操作 | 审批操作属于 M10 独立模块 |
| 架位预占 | 入库时写 archive_boxes.location_id | 已有 partial unique index 防并发 |
| archive_tags | 全量替换（先删后插） | 标签数量少，无需 diff |
| AI 补全 | 空壳返回 501 | 等刘星 feat/search-liu |
| 待入库批次查询 | 联查 intake_batches + intake_items | 按 received/partially_received 且有未入库已接收条目过滤 |
| 文件预览/下载 | 复用 MinioService 生成预签名 URL | 与暂存文件预览模式一致 |

---

## 5 依赖关系

### 5.1 依赖已有模块

| 模块 | 依赖内容 |
|------|----------|
| IntakeBatchService | 读取清单批次和条目、重算批次状态 |
| IntakeItemMapper | 查询/更新条目状态和确认字段 |
| StagingFileMapper | 查询已匹配暂存文件 |
| MinioService | 复制暂存对象到正式路径、生成预签名 URL |
| AuditService | 写入审计日志 |
| AuthContext | 当前用户 ID、角色校验 |
| JdbcTemplate | 序列号生成 |

### 5.2 不依赖未完成模块

| 模块 | 处理方式 |
|------|----------|
| M04 AI 补全 | 空壳接口 |
| M06 全宗/组织 | 入库时直接读 fonds/organizations 表，不建 Service |
| M07 库房管理 | 入库时直接读 archive_boxes/storage_locations 表，预占逻辑在 PendingArchiveService 内 |
| M10 审批工作台 | 只创建 ApprovalRequest 记录 |

---

## 6 错误处理

遵循已有的 `BusinessException` + `ErrorCode` 模式。

| 场景 | HTTP | code | 说明 |
|------|------|------|------|
| 条目状态不允许入库 | 409 | BUSINESS_CONFLICT | status 不是 pending_archive |
| 重复入库 | 409 | BUSINESS_CONFLICT | generatedArchiveId 非空 |
| 纯电子档案提供盒号/架位 | 422 | VALIDATION_FAILED | carrierStatus = electronic 时不接受 boxId/locationId |
| 纸质相关档案缺少盒号/架位 | 422 | VALIDATION_FAILED | carrierStatus 为 paper_electronic/paper 时 boxId/locationId 必填 |
| 同盒分类/全宗不一致 | 422 | VALIDATION_FAILED | 入盒时校验 |
| 编辑受保护字段 | 422 | VALIDATION_FAILED | 请求 DTO 不包含受保护字段，服务层二次校验 |
| 已销毁档案禁止编辑 | 409 | BUSINESS_CONFLICT | lifecycleStatus = destroyed |
| 重复审批 | 409 | BUSINESS_CONFLICT | 同一档案同类审批已有 pending |
| 凭证档案不存在 | 422 | VALIDATION_FAILED | evidenceArchiveNo 查不到或已销毁 |
| 凭证档案与目标不匹配 | 422 | VALIDATION_FAILED | organization_id/fonds_id 不一致 |
| AI 补全未实现 | 501 | NOT_IMPLEMENTED | 空壳接口 |
