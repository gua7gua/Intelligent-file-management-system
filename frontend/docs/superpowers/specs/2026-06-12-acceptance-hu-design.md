# feat/acceptance-hu 设计文档 — 移交验收与征集管理页面

> 胡颖 06-11 至 06-14 前端任务
> 分支：`feat/acceptance-hu`
> 原型：`doc/prototype/admin/transfer-reception.html`、`doc/prototype/admin/collection.html`
> 接口文档：第 7 章（前台验收）、第 8 章（征集管理）

## 1. 范围

实现管理后台两个页面，覆盖前台管理员从清单调取、纸质验收、电子文件暂存匹配、异常回退到回执导出的完整流程，以及征集批次的联系/拒绝/到馆验收。

| 页面 | 路由 | 原型 |
|------|------|------|
| 移交验收与电子文件上传 | `/admin/transfer-reception` | `transfer-reception.html` |
| 征集管理与接收 | `/admin/collection` | `collection.html` |

路由已在 `src/router/routes/admin.ts` 中配好，无需改动。

## 2. 实现方式

**方案：原型直译 + Element Plus 按需补充。**

- 将原型 HTML 搬入 Vue SFC，使用已全局引入的原型 CSS 类（`.card`, `.toolbar`, `.status`, `.metric`, `.drop-zone`, `.match-card`, `.receipt-preview` 等）
- Element Plus 仅在确实更好的地方使用：`ElMessage`（消息提示）、`el-upload`（文件上传拖拽区）、`el-dialog`（确认弹窗）
- 不用 Element Plus 替代原型效果够用的原生 HTML

## 3. 文件结构

```
src/api/
  reception.ts          # 前台验收 API（7.x 接口，7 个函数）
  collection.ts         # 征集管理 API（8.x 接口，3 个函数）

src/mock/modules/
  reception.ts          # 验收 mock 数据（2 个移交批次）
  collection.ts         # 征集 mock 数据（1 个征集批次）

src/types/
  reception.ts          # 类型定义（ReceptionBatch, ReceptionItem, StagingFile, MatchSummary）
  collection.ts         # 征集类型（CollectionBatch，扩展 ReceptionBatch）

src/views/admin/
  transfer-reception/
    index.vue           # 移交验收主页面（单文件，约 400-500 行）
  collection/
    index.vue           # 征集管理主页面（单文件，约 400-500 行）
```

不拆子组件：两个页面各自内聚，无跨页面复用区块。如果单文件超出 600 行再提取。

## 4. API 层

### 4.1 reception.ts

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `getReceptionBatches(params)` | GET | `/admin/reception/batches` | 查询待验收批次（sourceType/status/keyword + 分页） |
| `getReceptionBatchDetail(batchId)` | GET | `/admin/reception/batches/{batchId}` | 批次详情 + 条目 + 暂存文件 + 匹配摘要 |
| `uploadStagingFiles(batchId, files)` | POST | `/admin/reception/batches/{batchId}/staging-files` | multipart 上传，返回匹配结果 |
| `manualMatchFile(fileId, data)` | PUT | `/admin/reception/staging-files/{fileId}/match` | 人工匹配暂存文件到条目 |
| `updateItemAcceptance(itemId, data)` | PUT | `/admin/reception/items/{itemId}/acceptance` | 条目验收（accepted/rejected） |
| `completeBatchAcceptance(batchId, data)` | POST | `/admin/reception/batches/{batchId}/complete` | 完成批次验收 |
| `exportReceipt(batchId)` | GET | `/admin/reception/batches/{batchId}/receipt` | 下载回执 PDF（blob） |

### 4.2 collection.ts

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `getCollections(params)` | GET | `/admin/collections` | 查询征集批次 |
| `scheduleCollection(batchId, data)` | POST | `/admin/collections/{batchId}/schedule` | 约定到馆时间 |
| `rejectCollection(batchId, data)` | POST | `/admin/collections/{batchId}/reject` | 拒绝征集 |

征集的到馆验收复用 reception.ts 的函数，传 `sourceType=collection`。

### 4.3 Mock 切换

遵循已有模式：`VITE_USE_MOCK` 环境变量控制。API 函数内部判断，组件不直接 import mock 数据。

## 5. TypeScript 类型

### 5.1 reception.ts 类型

从后端 `IntakeBatchResponse` / `IntakeItemResponse` 映射：

```typescript
interface ReceptionBatch {
  id: number
  batchNo: string
  title: string
  sourceType: 'transfer' | 'collection' | 'compilation'
  status: string
  statusText: string
  organizationId: number
  organizationName: string
  departmentName: string
  contactPerson: string
  contactPhone: string
  expectedTransferDate: string
  submittedAt: string
  itemCount: number
  signatureStatus: string
  acceptanceNote?: string
  acceptedBy?: number
  acceptedAt?: string
  createdAt: string
  updatedAt: string
}

interface ReceptionItem {
  id: number
  batchId: number
  seqNo: number
  title: string
  carrierType: string
  expectedFilename: string
  paperCheckStatus: 'pending' | 'passed' | 'failed'
  fileMatchStatus: 'none' | 'matched' | 'unmatched' | 'duplicate' | 'missing' | 'failed'
  result: 'pending' | 'accepted' | 'rejected'
  acceptanceNote: string
  rejectReason: string
  createdAt: string
  updatedAt: string
}

interface StagingFile {
  id: number
  batchId: number
  originalFilename: string
  fileSize: number
  sha256: string
  scanResult: 'safe' | 'unsafe' | 'error'
  matchStatus: 'staging' | 'matched' | 'unmatched' | 'duplicate' | 'deleted'
  matchedItemId?: number
  uploadBatchNo: string
  createdAt: string
}

interface MatchSummary {
  matched: number
  unmatched: number
  duplicate: number
  failed: number
  missingItems: number[]
}

interface UploadResult {
  uploadBatchNo: string
  files: StagingFile[]
  matchSummary: MatchSummary
}
```

### 5.2 collection.ts 类型

```typescript
interface CollectionBatch extends ReceptionBatch {
  sourceType: 'collection'
  donorName: string
  donorPhone: string
  agreementAcceptedAt?: string
  contactNote?: string
  scheduledReceiveAt?: string
  rejectReason?: string
}
```

## 6. 页面设计

### 6.1 移交验收（transfer-reception/index.vue）

**布局**（对应原型 `reception-layout`）：

```
┌──────────────────────────────────────────────────────┐
│ 页面标题 + 副标题                                      │
├──────────────────────────────────────────────────────┤
│ 概览指标（4 格）：今日到馆清单 / 待验收条目 / 匹配异常 / 待导出回执 │
├──────────┬───────────────────────────────────────────┤
│          │ 清单详情卡片（批次状态/签字清单/预计移交）     │
│ 待验收   │ + 权限提示 notice                            │
│ 清单列表 ├───────────────────────────────────────────┤
│          │ U盘文件上传与匹配（collapsible 可折叠）       │
│ 360px    │  - 拖拽上传区                                │
│ 独立滚动 │  - 匹配结果卡片网格                           │
│          ├───────────────────────────────────────────┤
│          │ 条目验收表格（min-width 980px，独立滚动）     │
│          │  + 批量纸质通过 / 确认接收 按钮               │
│          ├───────────────────────────────────────────┤
│          │ 接收回执预览 + 导出按钮                       │
└──────────┴───────────────────────────────────────────┘
```

**顶部筛选**：待移交 / 今日到馆 / 存在异常 三个 tab，前端过滤不重新请求。

**数据流**：

```
getReceptionBatches(sourceType='transfer')
  → 左侧批次列表
  → 点击批次 → getReceptionBatchDetail(batchId)
    → 填充详情卡片、条目表格、回执预览

uploadStagingFiles(batchId, files)
  → 返回匹配结果 → 渲染匹配卡片
  → 异常文件 → manualMatchFile 手工匹配

条目逐条操作 → updateItemAcceptance(itemId, result)
全部处理完 → completeBatchAcceptance(batchId)
  → 批次状态变为 received/partially_received
  → 启用 exportReceipt 下载回执
```

**状态管理**（ref/reactive，不引入 store）：

- `batchList` — 批次列表
- `activeBatch` — 当前选中批次详情
- `activeFilter` — 筛选标签
- `searchKeyword` — 搜索关键字
- `loading` / `uploading` — 加载/上传状态

**交互规则**：

1. 批次切换 → 右侧全部刷新
2. 筛选/搜索 → 前端过滤 batchList
3. 文件上传 → el-upload 拖拽模式，上传后渲染匹配卡片
4. 批量纸质通过 → carrier=纯纸质 || matchStatus=matched 的自动标记接收
5. 确认接收校验 → pending 条目阻止；回退条目必须填写原因
6. 回执导出 → pending 清零后启用，blob 下载

**UI 状态**：加载占位、空数据提示、上传进度、ElMessage 错误提示。

### 6.2 征集管理（collection/index.vue）

**布局**（对应原型）：

```
┌──────────────────────────────────────────────────────┐
│ 页面标题 + 副标题                                      │
├──────────────────────────────────────────────────────┤
│ 概览指标：待联系 / 待接收 / 今日到馆 / 协议异常          │
├──────────┬───────────────────────────────────────────┤
│          │ 详情区（按状态 + 角色条件渲染）               │
│ 征集批次 │                                           │
│ 列表     │ pending_contact → 后台：联系/约定到馆/拒绝   │
│          │ pending_receive → 前台：上传匹配+条目验收+回执│
│          │ received/partially_received → 只读+回执     │
│          │ rejected → 只读 + 拒绝理由                  │
└──────────┴───────────────────────────────────────────┘
```

**角色条件渲染**（通过 `usePermission` composable）：

| 状态 | `back_archivist` | `front_archivist` |
|------|-----------------|-------------------|
| `pending_contact` | 约定到馆 / 拒绝 | 列表不可见 |
| `pending_receive` | 只读 | 验收流程（复用 reception API） |
| `received` | 只读 + 回执 | 只读 + 回执 |
| `rejected` | 只读 | 列表不可见 |

**征集验收**复用 reception.ts 的 `getReceptionBatchDetail`, `uploadStagingFiles`, `updateItemAcceptance`, `completeBatchAcceptance`, `exportReceipt`，额外校验 `agreementAcceptedAt`。

## 7. Mock 数据

覆盖以下场景：

**移交批次 A**（YJ-2026-0008）：4 个条目
- 条目 01：纸质+电子，filename=2025-01-voucher.pdf，待验收
- 条目 02：纸质+电子，filename=2025-02-voucher.pdf，待验收
- 条目 03：纸质+电子，filename=2025-03-voucher.pdf，文件缺失
- 条目 04：纯纸质，无电子文件，待验收

**移交批次 B**（YJ-2026-0007）：2 个条目
- 条目 01：纯电子，salary-2025-h1.xlsx，纸质已通过
- 条目 02：纯电子，salary-2025-h2.xlsx，纸质已通过

**征集批次**（ZJ-2026-0003）：3 个条目
- 状态 pending_contact，包含 donorName/donorPhone
- 条目含 expectedFilename 和 agreementAcceptedAt

## 8. 代码复用策略

- **API 层复用**：征集验收直接调用 reception.ts 函数
- **类型复用**：共用 ReceptionBatch, ReceptionItem, StagingFile
- **CSS 类复用**：原型全局样式已引入，直接使用 .card/.toolbar/.status/.metric/.drop-zone/.match-card/.receipt-preview 等
- **模板不抽象**：collection 页面内直接写验收区块 HTML，与 transfer-reception 结构一致但不抽取子组件

## 9. 非目标（不在本期范围）

- 不实现待入库与上架页面（06-13 至 06-15 的 feat/archive-hu）
- 不实现 AI 补全、正式档号、盒号、架位（后台入库职责）
- 不改动 Layout 组件或全局样式
- 不改动路由配置
- 不做后端联调（mock 阶段）
