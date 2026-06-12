# feat/portal-guo 设计文档：移交门户与公众门户

**日期**：2026-06-12
**负责人**：郭一坤
**分支**：`feat/portal-guo`
**阶段**：06-11 至 06-14
**验收标准**：移交侧与公众侧主流程可操作

## 1. 背景

本轮从最新 `develop` 开始，`develop` 已包含以下前置工作：

- 胡颖 `feat/base-hu`：前端脚手架、四类 Layout、路由存根和全局原型样式。
- 郭一坤 `feat/base-guo`：登录、角色命名、API request 层、mock 切换、字典 API。
- 周扬 `feat/transfer-zhou`：移交/征集清单、前台验收、回退、回执后端接口。
- 刘星 `feat/file-liu`：电子文件暂存、哈希、ClamAV、MinIO 匹配后端接口。
- 胡颖 `feat/acceptance-hu`：管理后台 `/admin/transfer-reception`、`/admin/collection` 页面，以及验收/征集 API、mock、类型。

本轮聚焦郭一坤 06-11 至 06-14 前端任务。用户确认采用“门户侧完整交付，内部检索暂缓”的方案，加入项目计划中遗漏的公众忘记密码页面。

## 2. 目标与范围

### 2.1 本轮实现

| 门户 | 路由 | 页面 | 原型 |
|------|------|------|------|
| 移交门户 | `/transfer/overview` | 移交工作台 | `doc/prototype/transfer/overview.html` |
| 移交门户 | `/transfer/transfer-list` | 编制移交清单 | `doc/prototype/transfer/transfer-list.html` |
| 公众门户 | `/public/index` | 公众首页 | `doc/prototype/public/index.html` |
| 公众门户 | `/public/search` | 公开档案检索 | `doc/prototype/public/search.html` |
| 公众门户 | `/public/collection` | 征集清单 | `doc/prototype/public/collection.html` |
| 公众门户 | `/public/overview` | 公众概览 | `doc/prototype/public/overview.html` |
| 公众门户 | `/public/register` | 公众注册 | `doc/prototype/public/register.html` |
| 公众门户 | `/public/forgot-password` | 忘记密码 | `doc/prototype/public/forgot-password.html` |

登录页保留并验证“公众注册 / 忘记密码”入口。

### 2.2 不在本轮

- `/internal/overview`
- `/internal/search`
- 内部预览、下载、借阅申请
- 管理后台入库、库房、统计、盘点等页面
- 胡颖已完成的后台验收和征集管理页面改造

内部查阅与借阅申请留给下一轮 `feat/search-guo`。

## 3. 方案选择

### 3.1 备选方案

| 方案 | 内容 | 优点 | 风险 |
|------|------|------|------|
| 门户侧完整交付 | 完成移交门户、公众门户、注册、忘记密码 | 严格贴合 06-11 至 06-14 任务，和后台验收/征集形成闭环 | 内部检索需下一轮完成 |
| 压缩门户并顺带内部检索壳子 | 门户主流程 + 内部检索静态入口 | 提前覆盖 06-13 后任务 | 范围变宽，门户交互容易不完整 |
| 只做核心 API 和表单 | API、mock、核心提交/查询页面 | 快速 | 不符合逐页按原型完成的前端流程，演示效果弱 |

### 3.2 选定方案

采用“门户侧完整交付”。本轮页面必须按 `frontend/CLAUDE.md` 的逐页流程实现：阅读原型设计与验收文档，圈定接口，搬原型到 Vue，接入 API/mock，验证页面交互。

## 4. 架构

沿用当前前端架构：

- 页面组件只写内容区，布局继续由 `TransferLayout` 和 `PublicLayout` 承担。
- API 函数统一放在 `src/api/`，组件只调用 API 函数，不直接 import mock。
- mock 数据放在 `src/mock/modules/`，通过 `VITE_USE_MOCK` 控制 mock/真实请求切换。
- 业务类型放在 `src/types/`，状态值优先复用 `src/types/enums.ts`。
- 页面内部使用 Vue `ref`/`reactive` 管理局部状态，不引入新的全局 store。
- 原型公共样式已经全局引入，页面复用 `.card`、`.toolbar`、`.button`、`.grid`、`.status`、`.metric`、`.table-wrap`、`.notice` 等类。

## 5. 文件结构

```
frontend/src/api/
  transfer.ts              # 移交门户接口
  public.ts                # 公众门户、公开检索、公众认证辅助接口
  auth.ts                  # 扩展公众注册、验证码、忘记密码接口

frontend/src/mock/modules/
  transfer.ts              # 移交清单、工作台 mock
  public.ts                # 公众首页、检索、概览、注册、忘记密码 mock

frontend/src/types/
  transfer.ts              # 移交批次、条目、工作台、表单类型
  public.ts                # 公开档案、公众概览、征集、认证辅助类型

frontend/src/views/
  transfer/overview/index.vue
  transfer/transfer-list/index.vue
  public/index/index.vue
  public/search/index.vue
  public/collection/index.vue
  public/overview/index.vue
  public/register/index.vue
  public/forgot-password/index.vue

frontend/src/router/routes/public.ts
```

`/public/forgot-password` 需要新增路由。其余页面路由已经存在。

## 6. API 设计

### 6.1 移交门户 API

`src/api/transfer.ts`

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `getTransferDashboard()` | GET | `/transfer/dashboard` | 工作台统计和最近清单 |
| `getTransferBatches(params)` | GET | `/transfer/batches` | 查询移交清单列表 |
| `getTransferBatchDetail(batchId)` | GET | `/transfer/batches/{batchId}` | 获取清单详情和条目 |
| `createTransferBatch(data)` | POST | `/transfer/batches` | 创建移交清单草稿 |
| `updateTransferBatch(batchId, data)` | PUT | `/transfer/batches/{batchId}` | 更新移交清单草稿 |
| `submitTransferBatch(batchId)` | POST | `/transfer/batches/{batchId}/submit` | 提交清单，进入待移交 |
| `exportTransferBatch(batchId)` | GET | `/transfer/batches/{batchId}/export` | 导出移交清单 PDF |

### 6.2 公众门户 API

`src/api/public.ts`

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `getPublicHome()` | 前端聚合 | mock 下返回公众首页数据；真实接口阶段由公开检索和统计接口组合 | 公众首页统计和最近公开 |
| `searchPublicArchives(params)` | GET | `/public/archives/search` | 公开档案检索 |
| `getPublicArchiveDetail(archiveId)` | GET | `/public/archives/{archiveId}` | 公开档案详情 |
| `generatePublicSearchQuery(data)` | POST | `/public/archives/ai-query` | AI 自然语言转公开检索 JSON |
| `downloadPublicArchiveFile(fileId)` | GET | `/public/archive-files/{fileId}/download` | 登录公众下载公开文件 |
| `getPublicOverview()` | GET | `/public/overview` | 公众个人概览 |
| `getMyCollections(params)` | GET | `/public/collections` | 本人征集清单列表 |
| `createCollectionDraft(data)` | POST | `/public/collections` | 创建征集清单草稿 |
| `updateCollectionDraft(batchId, data)` | PUT | `/public/collections/{batchId}` | 更新征集草稿 |
| `submitCollectionBatch(batchId)` | POST | `/public/collections/{batchId}/submit` | 提交征集清单 |

### 6.3 公众认证辅助 API

扩展 `src/api/auth.ts`：

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `sendPublicSmsCode(data)` | POST | `/public/auth/sms-code` | 发送验证码 |
| `registerPublicUser(data)` | POST | `/public/auth/register` | 公众注册 |
| `resetPublicPassword(data)` | POST | `/public/auth/reset-password` | 公众找回密码 |

这些函数仍遵循 `VITE_USE_MOCK` 切换。真实请求使用 `request`，mock 下动态 import `@/mock/modules/public`。

## 7. 类型设计

### 7.1 移交类型

`src/types/transfer.ts`

```typescript
import type { BatchStatus, CarrierStatus, ItemStatus, OpenStatus, RetentionPeriod } from './enums'

export type BatchStatusValue = (typeof BatchStatus)[keyof typeof BatchStatus]
export type ItemStatusValue = (typeof ItemStatus)[keyof typeof ItemStatus]
export type CarrierStatusValue = (typeof CarrierStatus)[keyof typeof CarrierStatus]
export type RetentionPeriodValue = (typeof RetentionPeriod)[keyof typeof RetentionPeriod]
export type OpenStatusValue = (typeof OpenStatus)[keyof typeof OpenStatus]

export interface TransferDashboardSummary {
  draft: number
  pendingTransfer: number
  partiallyReceived: number
  received: number
  archived: number
  shelved: number
  rejected: number
}

export interface TransferBatch {
  id: number
  batchNo: string
  title: string
  status: BatchStatusValue
  statusText: string
  organizationName: string
  departmentName: string
  contactPerson: string
  contactPhone: string
  archiveYear: number
  expectedTransferDate: string
  submittedAt?: string
  receivedAt?: string
  archivedAt?: string
  shelvedAt?: string
  itemCount: number
  acceptedCount: number
  rejectedCount: number
  receiptAttachmentId?: number
}

export interface TransferItem {
  id: number
  batchId: number
  seqNo: number
  inputTitle: string
  pageCount?: number
  retentionPeriod: RetentionPeriodValue
  carrierStatus: CarrierStatusValue | ''
  securityLevel: number
  openStatus: OpenStatusValue
  allowDigitization: boolean
  electronicFormat?: string
  expectedFilename?: string
  formedDate?: string
  status: ItemStatusValue
  rejectReason?: string
  localFileSize?: number
}

export interface TransferBatchDetail extends TransferBatch {
  items: TransferItem[]
}
```

### 7.2 公众类型

`src/types/public.ts`

```typescript
import type {
  BatchStatusValue,
  CarrierStatusValue,
  ItemStatusValue,
} from './transfer'

export interface PublicArchive {
  id: number
  archiveNo: string
  title: string
  responsible: string
  category: string
  formedYear: number
  carrierStatus: CarrierStatusValue
  sourceType: 'transfer' | 'collection' | 'compilation'
  tags: string[]
  hasElectronicFile: boolean
  canPreview: boolean
  canDownload: boolean
}

export interface PublicArchiveDetail extends PublicArchive {
  formedDate: string
  retentionPeriod: '10y' | '30y' | 'permanent'
  openStatus: 'open'
  summary: string
  files: PublicArchiveFile[]
}

export interface PublicArchiveFile {
  id: number
  filename: string
  fileFormat: string
  fileSize: number
  canPreview: boolean
  canDownload: boolean
}

export interface PublicCollectionBatch {
  id: number
  batchNo: string
  title: string
  donorName: string
  donorPhone: string
  donationNote: string
  status: BatchStatusValue
  statusText: string
  submittedAt?: string
  scheduledReceiveAt?: string
  itemCount: number
  agreementAcceptedAt?: string
  rejectReason?: string
  items: PublicCollectionItem[]
}

export interface PublicCollectionItem {
  id: number
  seqNo: number
  inputTitle: string
  pageCount?: number
  carrierStatus: CarrierStatusValue | ''
  electronicFormat?: string
  expectedFilename?: string
  formedDate?: string
  status: ItemStatusValue
  localFileSize?: number
}
```

## 8. 页面设计

### 8.1 移交工作台

目标：经办人查看本单位清单状态、回退原因、接收回执、入库和上架进度。

布局：

- 顶部标题和“新建清单”入口。
- 状态统计卡片：草稿、待移交、部分接收、已上架等。
- 流程进度说明：编制清单、前台验收、后台入库、确认上架。
- 左侧/上方筛选：全部、草稿、待移交、部分接收、已入库、已上架。
- 清单列表：清单号、标题、年度、预计移交日期、条目数、状态、操作。
- 详情区：选中清单的批次元数据、条目状态、回退原因、回执下载、下一步提示。

行为：

- 首次加载调用 `getTransferDashboard()` 和 `getTransferBatches()`。
- 筛选和关键字搜索在前端过滤当前列表。
- 点击清单行调用 `getTransferBatchDetail()`。
- 草稿显示“继续编辑”，跳转 `/transfer/transfer-list?batchId={id}`。
- 提交后的清单只读，只允许导出清单和查看状态。
- 部分接收清单突出显示回退原因和“新建补交清单”入口。

### 8.2 编制移交清单

目标：经办人填写清单级信息和条目级信息，支持本地解析电子文件名，提交后进入待移交。

布局：

- 清单基础信息表单：清单标题、移交单位、部门、经办人、联系电话、年度、预计移交日期。
- 本地文件解析区：拖拽或选择文件，只解析文件名、扩展名和大小。
- 本地文件列表：展示已解析但未上传的文件。
- 条目表格：可编辑标题、页数、保管期限、载体状态、密级、公开、数字化、文件名、形成日期。
- 校验摘要：展示缺失字段、涉密公开冲突、载体与文件名冲突。
- 底部操作：保存草稿、校验清单、提交清单、导出打印。

行为：

- 文件拖拽只读取 `File.name`、扩展名和 `File.size`，不调用上传接口。
- 拖拽生成的条目 `carrierStatus` 为空，必须人工选择。
- 新增空白条目用于纯纸质档案。
- 保存草稿调用 `createTransferBatch()` 或 `updateTransferBatch()`。
- 提交前校验清单级必填、至少一个条目、条目必填和受保护字段规则。
- 提交成功后调用 `submitTransferBatch()`，表单切为只读，展示导出按钮。

### 8.3 公众首页

目标：提供公开馆藏概况、公开检索入口、公众登录注册入口和征集清单入口。

布局：

- 首屏服务标题和公开检索主入口。
- 公开馆藏统计卡片。
- 分类分布、最近公开批次、征集流程摘要。
- 服务边界提示：只展示非密、公开、正常且未销毁档案聚合信息。

行为：

- 调用 `getPublicHome()`。
- 快速检索跳转 `/public/search?keyword={keyword}`。
- 征集入口跳转 `/public/collection`。
- 登录和注册入口跳转 `/login`、`/public/register`。

### 8.4 公开档案检索

目标：公众通过自然语言或结构化条件检索公开档案，查看详情，登录后下载。

布局：

- 公开范围提示。
- AI 自然语言输入区。
- JSON 结果区。
- 结构化条件表单：关键词、档号、题名、门类、年度、责任者、标签、来源、载体状态、电子文件状态、排序。
- 结果列表。
- 详情区：公开元数据、预览/下载操作、登录状态提示。

行为：

- `generatePublicSearchQuery()` 只生成查询条件 JSON，不执行检索。
- “填充表单”将 JSON 字段写入结构化表单。
- “确认检索”调用 `searchPublicArchives()`。
- 未登录点击下载提示跳转登录。
- 已登录公众点击下载调用 `downloadPublicArchiveFile()` 或 mock 下载反馈。
- 结果不展示密级明细、架位、盒号和内部备注。

### 8.5 征集清单

目标：公众填写捐赠意向和征集条目，提交后进入后台联系判断流程。

布局：

- 清单级表单：清单标题、捐赠人、联系电话、捐赠说明。
- 文件名解析区：只解析文件名、扩展名和大小。
- 条目表格：标题、页数、载体状态、格式、文件名、形成日期。
- 右侧辅助：目标属性、数量限制、状态流转、提交结果。
- 底部操作：保存草稿、提交捐赠意向、返回公众概览。

行为：

- 文件拖拽不上传文件本体。
- 征集目标属性固定展示：永久、无密级、公开；正式公开必须等待管理员接收和入库。
- 提交前校验清单标题、捐赠人、联系电话、至少一个条目、在线捐赠协议勾选。
- 保存草稿调用 `createCollectionDraft()` 或 `updateCollectionDraft()`。
- 提交调用 `submitCollectionBatch()`，状态变为待联系，页面只读。

### 8.6 公众概览

目标：公众登录后查看个人信息、公开馆藏概况、本人征集清单状态和本人下载记录。

布局：

- 欢迎信息、账号状态、快速操作。
- 统计卡：公开档案总量、最近公开、我的征集待处理、我的下载次数。
- 左侧：征集清单状态和下载记录。
- 右侧：个人信息与安全设置。

行为：

- 路由已要求登录。
- 调用 `getPublicOverview()`。
- 联系电话编辑在页面内模拟或调用后端扩展接口前先做本地反馈。
- 征集清单按状态展示：草稿可继续编辑，待联系/待接收可查看，已拒绝/部分接收/已接收只读。
- 下载记录重新查看必须提示重新鉴权。

### 8.7 公众注册

目标：社会公众自注册账号，注册成功后进入公众概览或返回登录。

布局：

- 左侧公众账号能力和边界说明。
- 右侧注册表单：姓名、手机号、验证码、密码、确认密码、确认说明。

行为：

- 发送验证码调用 `sendPublicSmsCode({ phone, scene: 'register' })`。
- 提交前校验手机号、验证码、密码一致性和确认勾选。
- 注册调用 `registerPublicUser()`。
- 成功后提示默认角色 `public_user`，提供登录/公众概览入口。
- 内部账号不通过此页创建。

### 8.8 忘记密码

目标：社会公众通过手机号和短信验证码重置密码。

布局：

- 左侧步骤说明：校验手机号、验证短信码、重新登录。
- 右侧表单：公众账号手机号、验证码、新密码、确认新密码。
- 底部提示：内部账号由系统管理员重置。

行为：

- 发送验证码调用 `sendPublicSmsCode({ phone, scene: 'forgot_password' })`。
- 提交前校验手机号、验证码、新密码一致性。
- 提交调用 `resetPublicPassword()`。
- 成功后提示返回统一登录页。
- 登录页“忘记密码”入口指向 `/public/forgot-password`。

## 9. 错误处理与边界

- 每个页面必须处理加载中、空数据、接口失败、表单校验失败。
- 组件捕获 API 异常后展示页面级 `notice` 或 Element Plus 消息，避免静默失败。
- 移交和征集侧文件拖拽只解析本地文件元数据，不上传文件本体，不创建暂存文件。
- 载体状态不能由文件拖拽自动判断，必须由经办人或公众手动选择。
- 移交清单和征集清单提交后进入只读，不允许继续编辑。
- 公开检索不展示密级、架位、盒号和内部备注。
- 公开检索下载未登录时引导登录，登录公众下载时重新鉴权。
- 公众注册和忘记密码只服务公众账号，内部账号由系统管理员维护。
- 本轮不修改胡颖后台页面；如需共享语义，只通过类型和 mock 数据保持一致。

## 10. Mock 数据

### 10.1 移交 mock

`mockTransferDashboard` 覆盖：

- 草稿清单
- 待移交清单
- 部分接收清单，包含回退原因
- 已入库/已上架清单，包含公开给移交单位的进度摘要

`mockTransferBatchDetail` 覆盖：

- 纯电子条目
- 纸质+电子条目
- 纯纸质条目
- 已回退条目

### 10.2 公众 mock

`mockPublicHome` 覆盖公开统计、分类分布、最近公开批次。

`mockPublicArchives` 覆盖：

- 可预览可下载档案
- 仅元数据档案
- 纯纸质档案
- 征集来源公开档案

`mockPublicOverview` 覆盖：

- 当前公众用户
- 草稿、待联系、待接收、已拒绝、部分接收、已接收征集清单
- 下载记录和权限变化记录

`mockPublicAuth` 覆盖验证码发送、注册成功、重置密码成功。

## 11. 测试与验证

### 11.1 单元测试

优先覆盖：

- 移交清单状态筛选和关键字过滤。
- 移交清单提交前校验。
- 文件名解析生成条目但不上传。
- 公众检索 AI JSON 填充。
- 公开检索未登录下载提示。
- 征集提交后只读。
- 公众注册和忘记密码表单校验。

### 11.2 组件交互测试

关键页面行为：

- `/transfer/overview` 点击清单切换详情。
- `/transfer/transfer-list` 拖拽文件、校验、保存草稿、提交。
- `/public/search` 生成 JSON、填充表单、检索、切换详情。
- `/public/collection` 新增/删除条目、提交后只读。
- `/public/register` 验证码和注册成功反馈。
- `/public/forgot-password` 验证码和重置成功反馈。

### 11.3 构建验证

执行：

```bash
npm run type-check
npm run build
```

如项目测试脚本已经可用，补充执行：

```bash
npm run test:unit
```

## 12. 验收口径

- 所有本轮路由可访问，无“待实现”占位。
- 页面字段、交互和状态与对应原型设计/验收文档一致。
- 移交清单可保存草稿、提交、只读、导出反馈、查看回退原因。
- 公众注册、忘记密码、公众首页、公开检索、征集清单、公众概览形成可演示闭环。
- API 函数全部支持 mock/真实切换。
- mock 数据结构对齐接口文档的 `data` 字段内容，不包裹统一响应。
- TypeScript 检查和生产构建通过。
