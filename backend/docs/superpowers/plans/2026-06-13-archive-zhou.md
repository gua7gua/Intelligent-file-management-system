# feat/archive-zhou 实施计划

分支：`feat/archive-zhou`
设计规格：`backend/docs/superpowers/specs/2026-06-13-archive-zhou-design.md`

---

## Step 1: 新增枚举类

创建入库与档案管理所需的所有枚举类。这些枚举不依赖其他新代码，可以独立编写。

| 文件 | 枚举值 |
|------|--------|
| `enums/LifecycleStatus.java` | `pending_shelf`, `normal`, `pending_destruction`, `destroyed` |
| `enums/LoanStatus.java` | `available`, `on_loan` |
| `enums/ConditionStatus.java` | `normal`, `damaged`, `repairing`, `lost`, `destroyed` |
| `enums/OpenStatus.java` | `open`, `closed` |
| `enums/FileRole.java` | `original`, `scan`, `compilation_body`, `signature` |
| `enums/ApprovalType.java` | `security_adjust`, `open_adjust`, `destruction` |
| `enums/ApprovalStatus.java` | `pending`, `approved`, `rejected` |
| `enums/FileStatus.java` | `pending`, `normal`, `failed`, `deleted` |
| `enums/UsabilityResult.java` | `pending`, `passed`, `failed` |

编写方式：参照已有的 `enums/BatchStatus.java`、`enums/CarrierStatus.java` 等枚举类格式，使用 `@EnumValue` 注解 + `@Getter`。

验证：`mvn compile` 通过。

---

## Step 2: 新增实体类

创建所有数据库实体，对应 archives、archive_files、tags、archive_tags、archive_change_logs、approval_requests、warehouse_rooms、storage_locations、archive_boxes、archive_box_items 表。

| 文件 | 对应表 | 继承 | 特殊字段 |
|------|--------|------|----------|
| `entity/Archive.java` | archives | BaseEntity | 枚举字段用 `@EnumValue`，不使用软删除（无 deletedAt） |
| `entity/ArchiveFile.java` | archive_files | BaseEntity | deletedAt + deletedBy 字段 |
| `entity/Tag.java` | tags | BaseEntity | tagName 唯一 |
| `entity/ArchiveTag.java` | archive_tags | 无（复合主键） | 不继承 BaseEntity，只有 archiveId + tagId + createdAt |
| `entity/ArchiveChangeLog.java` | archive_change_logs | 无 | 不继承 BaseEntity，有自己的 changedBy + changedAt |
| `entity/ApprovalRequest.java` | approval_requests | BaseEntity | 不使用软删除 |
| `entity/WarehouseRoom.java` | warehouse_rooms | BaseEntity | |
| `entity/StorageLocation.java` | storage_locations | BaseEntity | |
| `entity/ArchiveBox.java` | archive_boxes | BaseEntity | |
| `entity/ArchiveBoxItem.java` | archive_box_items | BaseEntity | |

编写方式：参照已有的 `entity/IntakeItem.java`、`entity/IntakeBatch.java` 格式。字段类型和约束严格对齐 `doc/数据库设计.md` 第 5~8 章的表定义。ArchiveTag 使用 `@TableId(type = IdType.INPUT)` 处理复合主键。

验证：`mvn compile` 通过。

---

## Step 3: 新增 Mapper 接口

创建所有 Mapper 接口。参照已有的 `mapper/IntakeBatchMapper.java` 格式（继承 `BaseMapper<T>` + `@Mapper` 注解）。

| 文件 | 泛型 | 说明 |
|------|------|------|
| `mapper/ArchiveMapper.java` | Archive | 档案 CRUD |
| `mapper/ArchiveFileMapper.java` | ArchiveFile | 正式文件 CRUD |
| `mapper/TagMapper.java` | Tag | 标签查询 |
| `mapper/ArchiveTagMapper.java` | ArchiveTag | 档案-标签关系（复合主键） |
| `mapper/ArchiveChangeLogMapper.java` | ArchiveChangeLog | 变更日志 |
| `mapper/ApprovalRequestMapper.java` | ApprovalRequest | 审批单 |
| `mapper/WarehouseRoomMapper.java` | WarehouseRoom | 库房只读 |
| `mapper/StorageLocationMapper.java` | StorageLocation | 架位只读 |
| `mapper/ArchiveBoxMapper.java` | ArchiveBox | 档案盒（预占、used_count 更新） |
| `mapper/ArchiveBoxItemMapper.java` | ArchiveBoxItem | 盒内档案关系 |

注意事项：
- ArchiveTagMapper 需要自定义 insert/delete，因为 ArchiveTag 是复合主键实体，不能用 BaseMapper 的默认 insert（依赖单列主键）。
- 其他 Mapper 与现有格式完全一致，不需要 XML。

验证：`mvn compile` 通过。

---

## Step 4: 新增 DTO

创建所有请求和响应 DTO。参照已有的 `dto/request/TransferBatchCreateRequest.java`、`dto/response/IntakeBatchResponse.java` 格式，使用 Lombok `@Data`。

### Request DTO

| 文件 | 字段 | 校验注解 |
|------|------|----------|
| `dto/request/ItemConfirmationRequest.java` | confirmedTitle(String), confirmedResponsibleText(String), confirmedFormedDate(LocalDate), confirmedCategoryId(Integer), confirmedTags(List\<String\>) | 无必填（确认阶段允许部分字段为空） |
| `dto/request/ArchiveRequest.java` | fondsId(Long), boxId(Long, 可空), locationId(Long, 可空), sortNo(Integer, 可空), pageCount(Integer, 可空) | 纸质相关时 boxId/locationId 必填，服务层校验 |
| `dto/request/BatchShelveRequest.java` | note(String) | 可空 |
| `dto/request/ArchiveUpdateRequest.java` | title(String), responsibleText(String), formedDate(LocalDate), categoryId(Integer), fondsId(Long), tagNames(List\<String\>), changeReason(String) | title 必填 |
| `dto/request/SecurityAdjustRequest.java` | newSecurityLevel(Integer), evidenceArchiveNo(String), reason(String) | 全部必填 |
| `dto/request/OpenAdjustRequest.java` | newOpenStatus(String), evidenceArchiveNo(String), reason(String) | 全部必填 |
| `dto/request/ArchiveFileDeleteRequest.java` | reason(String) | 必填 |

### Response DTO

| 文件 | 字段 |
|------|------|
| `dto/response/PendingBatchResponse.java` | id, batchNo, sourceType, title, status, organizationName, contactName, acceptedAt, itemCount, archivedCount, pendingArchiveCount, latestAiTaskStatus |
| `dto/response/PendingBatchDetailResponse.java` | 继承 PendingBatchResponse 字段 + items(List\<PendingItemResponse\>), availableBoxes(摘要), availableLocations(摘要) |
| `dto/response/PendingItemResponse.java` | id, itemNo, inputTitle, status, carrierStatus, pageCount, securityLevel, retentionPeriod, fileMatchStatus, aiSuggestion, confirmedTitle, confirmedResponsibleText, confirmedFormedDate, confirmedCategoryId, confirmedTags, stagingFiles(摘要), generatedArchiveId |
| `dto/response/ArchiveResponse.java` | id, archiveNo, title, responsibleText, formedDate, formedYear, categoryId, categoryName, sourceType, organizationId, organizationName, fondsId, fondsName, carrierStatus, retentionPeriod, retentionUntil, securityLevel, openStatus, allowDigitization, lifecycleStatus, loanStatus, conditionStatus, archivedAt, shelvedAt, tagNames, boxNo, locationCode |
| `dto/response/ArchiveFileResponse.java` | id, archiveId, fileRole, originalFilename, fileExt, mimeType, fileSize, scanResult, usabilityResult, fileStatus |
| `dto/response/ArchiveResultResponse.java` | archiveId(Long), archiveNo(String), lifecycleStatus(String) |

验证：`mvn compile` 通过。

---

## Step 5: ArchiveNoUtil + ArchiveFileService

### 5.1 ArchiveNoUtil

位置：`util/ArchiveNoUtil.java`

```java
@Component
@RequiredArgsConstructor
public class ArchiveNoUtil {
    private final JdbcTemplate jdbcTemplate;
    public String generate() {
        Long seq = jdbcTemplate.queryForObject("SELECT nextval('seq_archive_no')", Long.class);
        return String.format("ARC-%06d", seq);
    }
}
```

### 5.2 ArchiveFileService

位置：`service/ArchiveFileService.java`

方法列表：

| 方法 | 签名 | 说明 |
|------|------|------|
| stagingToFormal | `void stagingToFormal(Archive archive, List<StagingFile> stagingFiles)` | 将暂存文件复制为正式文件，在入库事务内调用 |
| findById | `ArchiveFile findById(Long fileId)` | 查询文件，不存在抛 404 |
| getPreviewUrl | `String getPreviewUrl(Long fileId)` | 生成预签名预览 URL |
| getDownloadUrl | `String getDownloadUrl(Long fileId)` | 生成预签名下载 URL |
| deleteFile | `void deleteFile(Long fileId, String reason)` | 作废文件（fileStatus = deleted），写审计日志 |

stagingToFormal 内部逻辑：
1. 遍历每个 match_status = matched 的 stagingFile
2. 创建 archive_files 记录（file_status = normal，object_key = `archive-files/{archiveNo}/{archiveFileId}/{fileName}`）
3. 调用 `minioService.copyObject(stagingBucket, stagingKey, formalBucket, formalKey)` 复制 MinIO 对象
4. 更新 staging_files.archived_file_id + match_status = archived

依赖注入：ArchiveFileMapper, MinioService, AuditService, JdbcTemplate（用于获取 archiveFileId）

验证：`mvn compile` 通过。

---

## Step 6: PendingArchiveService（核心入库逻辑）

位置：`service/PendingArchiveService.java`

依赖注入：IntakeItemMapper, IntakeBatchMapper, ArchiveMapper, ArchiveFileService, ArchiveNoUtil, ArchiveBoxMapper, ArchiveBoxItemMapper, TagMapper, ArchiveTagMapper, StorageLocationMapper, AuditService, JdbcTemplate

### 6.1 查询待入库批次（9.1）

```java
public PageResult<PendingBatchResponse> listPendingBatches(String sourceType, String aiStatus, String keyword, int pageNo, int pageSize)
```

逻辑：
1. 查询 intake_batches where status in ('received', 'partially_received')
2. 按 sourceType/keyword 过滤
3. 对每个批次，统计 intake_items 中 status = 'accepted' 或 'pending_archive' 的数量作为待入库数
4. 关联查询 latest_ai_task_id 对应的 ai_tasks.status 作为 aiStatus
5. 按 acceptedAt DESC 排序分页

### 6.2 获取入库批次详情（9.2）

```java
public PendingBatchDetailResponse getBatchDetail(Long batchId)
```

逻辑：
1. 查询批次 + 校验权限
2. 查询所有 status in ('accepted', 'pending_archive', 'archived') 的条目
3. 每个条目关联 staging_files（match_status = matched）
4. 查询可用的 archive_boxes（status in ('normal', 'full')，排除 full）
5. 查询可用的 storage_locations（status = 'active'，未被 archive_boxes 占用）
6. 组装返回

### 6.3 确认条目入库字段（9.6）

```java
@Transactional
public PendingItemResponse confirmItemFields(Long itemId, ItemConfirmationRequest req)
```

逻辑：
1. 查询 item，校验 status = accepted
2. 更新 confirmedTitle、confirmedResponsibleText、confirmedFormedDate、confirmedCategoryId、confirmedTags
3. status → pending_archive
4. 写审计日志

### 6.4 确认入库（9.7）— 核心事务

```java
@Transactional
public ArchiveResultResponse confirmArchive(Long itemId, ArchiveRequest req)
```

逻辑（严格按设计规格 3.1 节）：
1. 校验 item.status = pending_archive，generatedArchiveId 为空
2. 查询 item + batch
3. 查询 staging_files (batch_id + item_id, match_status = matched)
4. 生成档号 archiveNo = archiveNoUtil.generate()
5. 构建 Archive 实体：
   - 字段来源：confirmed > input > 条目
   - lifecycleStatus：纯电子 → normal，纸质相关 → pending_shelf
   - 计算 retention_until
6. archiveMapper.insert(archive)
7. archiveFileService.stagingToFormal(archive, stagingFiles)
8. 如果纸质相关（carrierStatus in paper_electronic, paper）：
   a. 校验 req.boxId 和 req.locationId 非空
   b. 查询 archiveBox，校验 categoryId/fondsId 一致
   c. 创建 archiveBoxItem
   d. archiveBox.usedCount += 1，检查是否 full
9. 处理标签：从 confirmedTags 查找/创建 tag → 写入 archive_tags
10. 回写 item.generatedArchiveId = archive.id，item.status = archived
11. 重算批次状态（查批次下所有条目状态，更新批次 status）
12. 写审计日志

### 6.5 批次确认上架（9.8）

```java
@Transactional
public void shelveBatch(Long batchId, BatchShelveRequest req)
```

逻辑：
1. 校验批次 status = archived
2. 校验批次下存在纸质相关档案
3. 查询该批次关联的所有 lifecycle_status = pending_shelf 的 archives
4. 批量更新 lifecycle_status = normal, shelved_at = now()
5. 批次 status = shelved, shelved_at = now()
6. 写审计日志

验证：`mvn compile` 通过。

---

## Step 7: ArchiveService（档案 CRUD + 元数据编辑 + 审批发起）

位置：`service/ArchiveService.java`

依赖注入：ArchiveMapper, ArchiveFileMapper, TagMapper, ArchiveTagMapper, ArchiveChangeLogMapper, ApprovalRequestMapper, IntakeItemMapper, IntakeBatchMapper, AuditService

### 7.1 管理端查询档案（10.1）

```java
public PageResult<ArchiveResponse> listArchives(String keyword, String archiveNo, Integer categoryId, Integer formedYearStart, Integer formedYearEnd, Long organizationId, Long fondsId, Integer securityLevel, String openStatus, String carrierStatus, String lifecycleStatus, String loanStatus, String conditionStatus, int pageNo, int pageSize)
```

逻辑：
1. 构建 QueryWrapper<Archive>，按各筛选条件追加 where
2. keyword 模糊匹配 title、archive_no、responsible_text
3. 按 archived_at DESC 排序分页
4. 转换为 ArchiveResponse（关联查询 categoryName、organizationName、fondsName、tagNames、boxNo、locationCode）

### 7.2 获取档案详情（10.2）

```java
public ArchiveResponse getArchiveDetail(Long archiveId)
```

逻辑：
1. 查询 archive，不存在抛 404
2. 关联查询 archive_files、archive_tags + tags、archive_change_logs
3. 查询来源清单（source_batch_id → intake_batch，source_item_id → intake_item）
4. 查询盒号架位（archive_box_items → archive_box → storage_location）
5. 组装完整 ArchiveResponse

### 7.3 编辑非受保护元数据（10.3）

```java
@Transactional
public ArchiveResponse updateArchive(Long archiveId, ArchiveUpdateRequest req)
```

逻辑：
1. 查询 archive，校验 lifecycleStatus != destroyed
2. 逐字段比较新旧值（title、responsibleText、formedDate、categoryId、fondsId）
3. 只记录实际变更的字段到 archive_change_logs（changeSource = manual_edit）
4. 处理标签：删除旧 archive_tags，按新 tagNames 查找/创建 tags 并写入
5. 更新 archive 记录
6. 写审计日志

受保护字段列表（禁止通过此接口修改）：archiveNo、securityLevel、retentionPeriod、openStatus、allowDigitization、lifecycleStatus、loanStatus、conditionStatus。DTO 层面不包含这些字段。

### 7.4 发起密级调整审批（10.4）

```java
@Transactional
public ApprovalRequest createSecurityAdjustment(Long archiveId, SecurityAdjustRequest req)
```

逻辑：
1. 查询目标 archive，校验存在
2. 校验新密级 != 当前密级
3. 查询 evidenceArchiveNo 对应的凭证档案，校验：存在、未销毁、organization_id 或 fonds_id 与目标匹配
4. 校验同一 archive 无 pending 的 security_adjust 审批（查 approval_requests）
5. 创建 ApprovalRequest（approvalType = security_adjust, targetType = archive, targetId = archiveId, oldValue = 当前密级, newValue = 新密级）
6. 写审计日志

### 7.5 发起开放调整审批（10.5）

```java
@Transactional
public ApprovalRequest createOpenAdjustment(Long archiveId, OpenAdjustRequest req)
```

逻辑：同密级调整，但 approvalType = open_adjust，oldValue/newValue 为 openStatus。

验证：`mvn compile` 通过。

---

## Step 8: Controller 层

创建 3 个 Controller。参照已有的 `controller/ReceptionController.java`、`controller/StagingFileController.java` 格式：`@RestController` + `@RequestMapping` + `@RequiredArgsConstructor`，角色校验使用 `@SaCheckRole("back_archivist")`。

### 8.1 PendingArchiveController

路径前缀：`/api/admin/pending-archive`

| 方法 | HTTP | 路径 | 对应 Service 方法 |
|------|------|------|-------------------|
| listBatches | GET | /batches | pendingArchiveService.listPendingBatches() |
| getBatchDetail | GET | /batches/{batchId} | pendingArchiveService.getBatchDetail() |
| startAiCompletion | POST | /batches/{batchId}/ai-completion | 返回 501 NOT_IMPLEMENTED（空壳） |
| confirmItem | PUT | /items/{itemId}/confirmation | pendingArchiveService.confirmItemFields() |
| confirmArchive | POST | /items/{itemId}/archive | pendingArchiveService.confirmArchive() |
| shelveBatch | POST | /batches/{batchId}/shelve | pendingArchiveService.shelveBatch() |

### 8.2 ArchiveController

路径前缀：`/api/admin/archives`

| 方法 | HTTP | 路径 | 对应 Service 方法 |
|------|------|------|-------------------|
| listArchives | GET | / | archiveService.listArchives() |
| getDetail | GET | /{archiveId} | archiveService.getArchiveDetail() |
| updateArchive | PUT | /{archiveId} | archiveService.updateArchive() |
| securityAdjust | POST | /{archiveId}/security-adjustments | archiveService.createSecurityAdjustment() |
| openAdjust | POST | /{archiveId}/open-adjustments | archiveService.createOpenAdjustment() |

### 8.3 ArchiveFileController

路径前缀：`/api/admin/archive-files`

| 方法 | HTTP | 路径 | 对应 Service 方法 |
|------|------|------|-------------------|
| preview | GET | /{fileId}/preview | archiveFileService.getPreviewUrl() → 302 重定向 |
| download | GET | /{fileId}/download | archiveFileService.getDownloadUrl() → 302 重定向 |
| deleteFile | DELETE | /{fileId} | archiveFileService.deleteFile() |

### 8.4 AI 空壳 Controller（待刘星实现）

在 PendingArchiveController 中额外添加两个空壳方法：

| 方法 | HTTP | 路径 | 响应 |
|------|------|------|------|
| getAiTask | GET | /api/admin/ai-tasks/{taskId} | 返回 R.fail("NOT_IMPLEMENTED", "AI 补全功能待实现") |
| retryAiTask | POST | /api/admin/ai-tasks/{taskId}/retry-failed | 同上 |

也可以单独创建一个 AiTaskController 空壳，视代码组织偏好。这里选择在 ArchiveController 同目录下创建空的 `AiTaskController.java`。

验证：`mvn compile` 通过。

---

## Step 9: 编译验证 + 提交

### 9.1 编译验证

```bash
cd backend && mvn compile -q
```

确保零编译错误。如果出现类型不匹配、缺少 import、字段名与数据库列名不一致等问题，立即修复。

### 9.2 数据库连通性验证（可选）

如果本地 PostgreSQL 运行中：

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"
```

启动后检查：
- Swagger UI 可访问（/swagger-ui.html）
- 新接口出现在 Swagger 文档中

### 9.3 提交

按 commit 规范（`doc/commit-convention.md`），每个 Step 单独提交：

```bash
git add . && git commit -m "feat(archive): 实现入库与档案管理模块"
git push origin feat/archive-zhou
```

如果按 Step 分别提交更清晰，可以拆为：
- `feat(archive): 添加档案模块枚举和实体类`（Step 1+2）
- `feat(archive): 添加档案模块 Mapper 和 DTO`（Step 3+4）
- `feat(archive): 实现档号生成和正式文件服务`（Step 5）
- `feat(archive): 实现待入库确认和上架服务`（Step 6）
- `feat(archive): 实现档案管理和审批发起服务`（Step 7）
- `feat(archive): 添加入库和档案管理 Controller`（Step 8）

---

## 执行顺序总结

```
Step 1 (枚举) → Step 2 (实体) → Step 3 (Mapper) → Step 4 (DTO)
    → Step 5 (ArchiveNoUtil + ArchiveFileService)
    → Step 6 (PendingArchiveService — 核心)
    → Step 7 (ArchiveService)
    → Step 8 (Controller)
    → Step 9 (编译验证 + 提交)
```

每步完成后运行 `mvn compile` 确认无编译错误。Step 6 是最复杂的事务逻辑，需要仔细处理状态校验和并发控制。
