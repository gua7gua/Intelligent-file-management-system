# feat/archive-hu 设计文档 — 待入库与上架、档案管理页面

日期：2026-06-13
分支：`feat/archive-hu`
负责人：胡颖
阶段：06-13 至 06-15

---

## 1. 范围

实现胡颖在 06-13 至 06-15 期间的两个管理后台页面：

| 页面 | 路由 | 原型 | 接口章节 |
|------|------|------|----------|
| 待入库与上架 | `/admin/pending-archive` | `prototype/admin/pending-archive.html` | §9 |
| 档案管理 | `/admin/archive-management` | `prototype/admin/archive-management.html` | §10 |

对应项目计划甘特图阶段 fe3（06-12 至 06-15），共同验收点为"入库与利用页面可联动"。

## 2. 产物清单

| 产物 | 路径 | 说明 |
|------|------|------|
| 类型定义 | `src/types/archive.ts` | 待入库批次、条目、AI 任务、正式档案、审批申请等类型 |
| Mock 数据 | `src/mock/modules/archive.ts` | 对齐接口文档 §9 §10 响应结构的 mock 数据 |
| API 函数 | `src/api/archive.ts` | 待入库和档案管理全部接口函数，含 VITE_USE_MOCK 切换 |
| 待入库页面 | `src/views/admin/pending-archive/index.vue` | 三栏布局：批次→条目→详情表单 |
| 档案管理页面 | `src/views/admin/archive-management/index.vue` | 三栏布局：分类树→筛选列表→详情抽屉 |
| Mock 注册 | `src/mock/index.ts` | 注册 archive mock 模块 |

## 3. 页面设计

### 3.1 待入库与上架

**布局**：三栏 grid（280px | minmax(0,1fr) | 420px），响应式 ≤1200px 变为单栏。

**顶部工具栏**：
- AI 状态标签（未开始 / 处理中 / 已完成 / 失败）
- "AI 整批补全"按钮

**左栏 — 待处理批次列表**：
- 调用 `GET /api/admin/pending-archive/batches`，支持 sourceType / aiStatus / keyword 筛选
- 卡片展示：标题、批次号、接收/回退数量、AI 状态
- 选中高亮联动中栏和右栏

**中栏 — 已接收条目列表**：
- 选中批次后调用 `GET /api/admin/pending-archive/batches/{batchId}` 获取条目
- 条目卡片：题名、载体类型标签、状态徽章（已接收/AI已建议/待入库/已入库未上架/正常）
- 点击条目联动右栏

**右栏 — 条目详情表单**：
- 受保护字段展示条（黄色背景）：密级、保管期限、开放、允许数字化、档号、文件匹配状态
- 可编辑字段：正式题名、责任者、形成日期（date input）、分类（下拉五门类）、标签（逗号分隔）
- 纸质相关档案（`carrier !== 'electronic'`）额外显示：盒号、盒脊信息、架位
- 纯电子档案显示提示"纯电子档案无需盒号和架位"
- 操作按钮：
  - "确认入库"：校验必填字段后调用 `POST .../items/{itemId}/archive`
  - "确认上架"：仅纸质已入库未上架条目可见，调用 `POST .../batches/{batchId}/shelve`

**AI 补全流程**：
1. 点击"AI 整批补全" → `POST .../batches/{batchId}/ai-completion` → 返回 aiTaskId
2. 轮询 `GET /api/admin/ai-tasks/{taskId}` 直到 status !== 'running'
3. AI 完成后自动填充 accepted 状态条目的候选字段，条目状态变为 suggested
4. 失败时显示"AI 不可用，可手工填写"提示，条目保持 accepted 状态仍可手工编辑
5. 管理员逐条核查后保存确认：`PUT .../items/{itemId}/confirmation`，状态变为 pending_archive

**状态校验规则**：
- 入库前置条件：条目状态为 accepted/suggested/confirmed，且无 archiveNo
- 必填字段：正式题名、责任者、形成日期、分类
- 纸质档案额外必填：盒号、盒脊、架位
- 纯电子入库后 lifecycleStatus = normal；纸质入库后 = pending_shelf
- 上架仅对 pending_shelf 状态的纸质档案可用

**UI 状态处理**：
- 加载中：各栏显示 skeleton 或 "加载中..."
- 空数据：批次列表为空显示"暂无待入库批次"；条目列表为空显示"选择左侧批次查看条目"
- 未选中：右栏显示"← 选择左侧条目查看和编辑详情"
- 操作反馈：所有操作通过 ElMessage 反馈结果

### 3.2 档案管理

**布局**：三栏 grid（220px | minmax(0,1fr) | 380px），响应式 ≤1220px 变为单栏。

**左栏 — 固定分类树**：
- 五大门类按钮：文书档案、科技档案、会计档案、音像档案、人事档案 + "全部门类"
- 选中高亮，点击设置筛选条件的 categoryId
- 底部提示文字："五大门类来自 categories 初始化字典，本页不提供增删。"

**中栏上部 — 筛选表单**：
- 字段：关键词、年度（number）、密级（下拉）、开放状态（下拉）、保管期限（下拉）、载体状态（下拉）
- 查询按钮：调用 `GET /api/admin/archives` 带筛选参数
- 重置按钮：清空所有筛选条件

**中栏下部 — 档案列表**：
- 表格列：档号、题名、分类、密级、开放、载体、状态（状态徽章）
- 分页
- 点击行设置右栏详情

**右栏 — 详情抽屉**：
- 固定信息：档号、生命周期状态、架位（纯电子显示"纯电子无架位"）
- **可编辑元数据区**：
  - 题名（input）、责任者（input）、形成日期（date）、分类（下拉五门类）、标签（input）、摘要（textarea）
  - "保存元数据"按钮 → `PUT /api/admin/archives/{archiveId}`
  - "预览"按钮 → 提示"预览需复用档案查询鉴权"
- **受保护字段区**（黄色警告 + 只读）：
  - 当前密级、当前开放状态（disabled input）
  - 说明："密级、保管期限、公开状态、允许数字化、档号、架位和销毁状态不可普通编辑。解密不等于自动公开。"
- **发起审批区**：
  - 凭证档号（input，必填）
  - 调整后密级（下拉）、调整后开放状态（下拉）
  - 申请理由（textarea，必填）
  - "密级调整"按钮 → `POST .../archives/{archiveId}/security-adjustments`
  - "开放调整"按钮 → `POST .../archives/{archiveId}/open-adjustments`
  - "审批工作台"链接 → 跳转 `/admin/approval`
  - 校验：凭证档号和申请理由缺一不可

**UI 状态处理**：
- 加载中：列表和详情各自独立 loading 状态
- 空列表："暂无匹配的正式档案"
- 未选中详情："← 点击档案行查看详情"
- 操作反馈：ElMessage 提示保存成功、审批单已生成等

## 4. 类型设计

```typescript
// src/types/archive.ts

/** 待入库批次摘要 */
export interface PendingBatch {
  id: number
  batchNo: string
  title: string
  sourceType: 'transfer' | 'collection'
  status: string
  itemCount: number
  acceptedCount: number
  returnedCount: number
  aiStatus: 'not_started' | 'running' | 'partial_completed' | 'completed' | 'failed'
  createdAt: string
  updatedAt: string
}

/** 待入库批次详情（含条目） */
export interface PendingBatchDetail extends PendingBatch {
  items: PendingItem[]
}

/** 待入库条目 */
export interface PendingItem {
  id: number
  batchId: number
  seqNo: number
  inputTitle: string
  expectedFilename: string
  carrierStatus: 'electronic' | 'paper_electronic' | 'paper'
  itemStatus: 'accepted' | 'suggested' | 'confirmed' | 'pending_archive' | 'archived'
  matchStatus: string
  // 受保护字段
  securityLevel: number
  retentionPeriod: string
  openStatus: 'open' | 'closed'
  allowDigitization: boolean
  // AI 候选字段
  suggestedTitle?: string
  suggestedResponsible?: string
  suggestedFormedDate?: string
  suggestedCategoryId?: number
  suggestedTags?: string[]
  // 确认字段
  confirmedTitle?: string
  confirmedResponsible?: string
  confirmedFormedDate?: string
  confirmedCategoryId?: number
  confirmedTags?: string[]
  // 入库相关
  archiveId?: number
  archiveNo?: string
  lifecycleStatus?: string
  boxNo?: string
  spine?: string
  locationCode?: string
  createdAt: string
  updatedAt: string
}

/** AI 补全任务 */
export interface AiTask {
  id: number
  taskNo: string
  status: 'running' | 'partial_completed' | 'completed' | 'failed'
  batchSize: number
  totalBatches: number
  completedCount: number
  failedCount: number
  errorMessage?: string
}

/** 确认入库字段请求 */
export interface ConfirmItemData {
  confirmedTitle: string
  confirmedResponsibleText: string
  confirmedFormedDate: string
  confirmedCategoryId: number
  confirmedTags: string[]
}

/** 入库请求 */
export interface ArchiveItemData {
  fondsId: number
  boxId?: number
  locationId?: number
  sortNo?: number
  pageCount?: number
}

/** 上架请求 */
export interface ShelveBatchData {
  note?: string
}

/** 正式档案摘要 */
export interface ArchiveRecord {
  id: number
  archiveNo: string
  title: string
  categoryId: number
  categoryName: string
  securityLevel: number
  openStatus: 'open' | 'closed'
  carrierStatus: string
  lifecycleStatus: string
  responsibleText: string
  formedDate: string
  tags: string[]
  fondsId: number
  fondsName?: string
  locationCode?: string
  boxNo?: string
  createdAt: string
  updatedAt: string
}

/** 档案详情 */
export interface ArchiveDetail extends ArchiveRecord {
  summary?: string
  allowDigitization: boolean
  retentionPeriod: string
  loanStatus: string
  conditionStatus: string
  files: ArchiveFile[]
  changeLogs: ArchiveChangeLog[]
  pendingApprovals: PendingApproval[]
}

/** 档案文件 */
export interface ArchiveFile {
  id: number
  originalFilename: string
  fileSize: number
  fileStatus: string
  mimeType: string
  createdAt: string
}

/** 变更日志 */
export interface ArchiveChangeLog {
  id: number
  field: string
  oldValue: string
  newValue: string
  changeSource: string
  changedAt: string
  changedBy: string
}

/** 待审批记录 */
export interface PendingApproval {
  id: number
  type: 'security_adjustment' | 'open_adjustment'
  status: 'pending' | 'approved' | 'rejected'
  newSecurityLevel?: number
  newOpenStatus?: string
  reason: string
  createdAt: string
}

/** 档案编辑请求 */
export interface ArchiveEditData {
  title: string
  responsibleText: string
  formedDate: string
  categoryId: number
  fondsId?: number
  tagNames: string[]
  changeReason?: string
}

/** 密级调整请求 */
export interface SecurityAdjustData {
  newSecurityLevel: number
  evidenceArchiveNo: string
  reason: string
}

/** 开放调整请求 */
export interface OpenAdjustData {
  newOpenStatus: 'open' | 'closed'
  evidenceArchiveNo: string
  reason: string
}

/** 档案查询参数 */
export interface ArchiveQueryParams {
  keyword?: string
  archiveNo?: string
  categoryId?: number
  formedYearStart?: number
  formedYearEnd?: number
  organizationId?: number
  fondsId?: number
  securityLevel?: number
  openStatus?: string
  carrierStatus?: string
  lifecycleStatus?: string
  loanStatus?: string
  conditionStatus?: string
  pageNo?: number
  pageSize?: number
}

/** 待入库批次查询参数 */
export interface PendingBatchParams {
  sourceType?: 'transfer' | 'collection'
  aiStatus?: string
  keyword?: string
  pageNo?: number
  pageSize?: number
}
```

## 5. API 函数设计

```typescript
// src/api/archive.ts

// 待入库相关
getPendingBatches(params?: PendingBatchParams & PageParams): Promise<PageData<PendingBatch>>
getPendingBatchDetail(batchId: number): Promise<PendingBatchDetail>
startAiCompletion(batchId: number): Promise<AiTask>
getAiTask(taskId: number): Promise<AiTask>
retryAiTask(taskId: number): Promise<AiTask>
confirmItem(itemId: number, data: ConfirmItemData): Promise<PendingItem>
archiveItem(itemId: number, data: ArchiveItemData): Promise<{ archiveId: number; archiveNo: string; lifecycleStatus: string }>
shelveBatch(batchId: number, data?: ShelveBatchData): Promise<PendingBatchDetail>

// 档案管理相关
getArchives(params?: ArchiveQueryParams & PageParams): Promise<PageData<ArchiveRecord>>
getArchiveDetail(archiveId: number): Promise<ArchiveDetail>
updateArchive(archiveId: number, data: ArchiveEditData): Promise<ArchiveDetail>
submitSecurityAdjust(archiveId: number, data: SecurityAdjustData): Promise<unknown>
submitOpenAdjust(archiveId: number, data: OpenAdjustData): Promise<unknown>
previewArchiveFile(fileId: number): Promise<string>
downloadArchiveFile(fileId: number): Promise<Blob>
```

## 6. Mock 数据策略

- Mock 数据对齐接口文档 §9 §10 的响应结构
- 仅模拟 `data` 字段内容（响应拦截器已解包 `R<T>`）
- 分页接口模拟完整 `PageData<T>` 结构
- 待入库 mock 包含 1-2 个批次，每批次 3-4 个条目，覆盖不同载体和状态
- 档案管理 mock 包含 3-5 条正式档案，覆盖不同门类/密级/载体/状态
- AI 补全 mock 模拟 1.2 秒延迟后返回成功，填充候选字段
- 重复已有 enums.ts 的标签映射，不在 mock 中重新定义

## 7. 复用与约定

- 复用 `enums.ts` 中已有的状态枚举和标签映射（`ArchiveStatusLabel`、`CarrierStatusLabel`、`SecurityLevelLabel`、`OpenStatusLabel` 等）
- 复用 `api.d.ts` 中的 `PageData`、`PageParams` 类型
- API 层沿用 `USE_MOCK` 模式，风格与 `reception.ts` 一致
- 页面使用全局 CSS 变量和公共类名（`.card`、`.toolbar`、`.button`、`.status` 等），不引入新全局样式
- Element Plus 全局注册，可按需使用 ElMessage 等组件
- 路由已搭好，无需修改路由配置

## 8. 风险与边界

- 后端接口尚在开发中，全部走 mock，后续联调时切换 `VITE_USE_MOCK=false`
- AI 补全为异步任务，前端用 setTimeout 模拟 polling，实际需后端 AI 服务就绪
- 盒号/架位选择器当前简化为文本输入，联调后可改为下拉选择可用架位
- 档案管理的分类树为固定五门类按钮，不请求分类接口（原型设计如此）
- 密级/开放调整审批只提交审批单，审批通过后的字段更新由后端处理
