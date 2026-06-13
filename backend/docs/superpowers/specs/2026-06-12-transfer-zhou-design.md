---
name: transfer-zhou-design
description: 周扬 06-11 至 06-13 后端任务设计：移交/征集清单、前台验收、回退、回执
---

# 周扬 06-11 至 06-13 后端任务设计

## 任务范围

周扬负责移交/征集清单的纯元数据 CRUD、前台验收回退、回执导出。
文件上传/暂存匹配（MinIO、ClamAV）留给刘星的 `feat/file-liu`。

## 对应接口

| 章节 | 路径前缀 | 角色 | 说明 |
|------|----------|------|------|
| 4 | `/api/transfer/batches` | `transfer_user` | 移交清单 CRUD + 提交 |
| 5.7-5.11 | `/api/public/collections` | `public_user` | 征集清单 CRUD + 提交 |
| 7 | `/api/admin/reception` | `front_archivist` | 前台验收、回退、回执 |
| 8 | `/api/admin/collections` | `back_archivist` | 征集联系、拒绝 |

暂不实现：7.3 暂存文件上传、7.4 手工匹配、7.7 回执 PDF 导出（仅建骨架）。

## 代码组织

### 枚举

| 枚举 | 值 |
|------|----|
| `SourceType` | `transfer`, `collection` |
| `BatchStatus` | `draft`, `pending_transfer`, `pending_contact`, `pending_receive`, `received`, `partially_received`, `rejected`, `archived`, `shelved` |
| `ItemStatus` | `draft`, `pending_acceptance`, `accepted`, `rejected`, `pending_archive`, `archived` |
| `CarrierStatus` | `electronic`, `paper_electronic`, `paper` |
| `RetentionPeriod` | `_10y`, `_30y`, `permanent` |
| `FileMatchStatus` | `none`, `matched`, `missing`, `duplicate`, `failed` |

### Entity

- `IntakeBatch` — 对应 `intake_batches`，枚举字段用 `@EnumValue`
- `IntakeItem` — 对应 `intake_items`

### Mapper

- `IntakeBatchMapper extends BaseMapper<IntakeBatch>`
- `IntakeItemMapper extends BaseMapper<IntakeItem>`

### DTO

请求：
- `TransferBatchCreateRequest` — 创建移交清单（含 items）
- `TransferItemRequest` — 单条条目（增/改共用）
- `CollectionBatchCreateRequest` — 创建征集清单
- `CollectionSubmitRequest` — 提交征集清单（含 agreementAccepted）
- `ItemAcceptanceRequest` — 条目验收结果（accepted/rejected + reason）
- `BatchCompleteRequest` — 完成批次验收
- `CollectionScheduleRequest` — 征集约定到馆
- `CollectionRejectRequest` — 征集拒绝

响应：
- `IntakeBatchResponse` — 批次详情（含 items 列表、状态文本）
- `IntakeItemResponse` — 条目详情
- `TransferDashboardResponse` — 移交工作台统计
- `BatchPageQuery` — 清单分页查询参数

### Service

`IntakeBatchService`：
- 移交清单 CRUD：create, getById, update, delete, list
- 移交清单条目：addItem, updateItem, deleteItem
- 提交清单：submit（状态流转 draft → pending_transfer）
- 征集清单 CRUD：createCollection, getCollection, updateCollection
- 提交征集：submitCollection（draft → pending_contact）
- 前台验收：listPending, getReceptionDetail
- 条目验收：acceptItem / rejectItem
- 完成验收：completeAcceptance（触发批次状态重算）
- 回退处理：回退时标记 rejectReason
- 征集管理：scheduleReceive, rejectCollection
- 回执导出骨架：exportReceipt（暂返回 JSON）
- 批次状态重算：recalculateBatchStatus

### Controller

| Controller | 路径 | Swagger Tag |
|------------|------|-------------|
| `TransferController` | `/api/transfer` | 移交单位门户 |
| `PublicCollectionController` | `/api/public/collections` | 公众征集 |
| `ReceptionController` | `/api/admin/reception` | 前台移交验收 |
| `CollectionManageController` | `/api/admin/collections` | 征集管理 |

## 关键实现策略

1. **批次状态重算**：`recalculateBatchStatus()` — 条目全部 archived → 批次 archived；有 accepted 未入库 → received/partially_received；全部 rejected → 批次 rejected
2. **清单号生成**：`SELECT nextval('seq_batch_no')`，格式 `BAT-{000001}`
3. **回执导出**：暂返回 JSON，PDF 实现依赖模板引擎
4. **权限**：`AuthContext` 获取角色和组织 ID，Service 层校验
5. **审计**：预留 audit_logs 写入点，本期不实现完整审计

## 不在本期范围

- 暂存文件上传/手工匹配（刘星 feat/file-liu）
- AI 补全（刘星 feat/search-liu）
- PDF 导出具体实现
- 移交工作台统计（Section 4.1）暂返回空结构
- 征集公众概览（Section 5.1）暂返回空结构
