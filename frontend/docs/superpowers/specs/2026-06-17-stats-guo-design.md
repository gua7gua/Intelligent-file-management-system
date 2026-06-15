# feat/stats-guo 设计文档：数据统计 · 数据研判 · 档案编研 · 管理概览（工作台补齐）

**日期**：2026-06-17
**负责人**：郭一坤
**分支**：`feat/stats-guo`
**阶段**：06-17 至 06-18
**优先级**：P2（统计/研判/编研基础可用，复杂边界可降级）+ P1（管理概览工作台补齐）
**共同验收点**：06-18 完成前端页面冻结 —— 统计、研判、编研、工作台补齐页面可进入、可操作、可还原原型

## 1. 背景

本轮从最新 `develop`（`7d264f8`，含胡颖 `feat/settings-hu` PR#25）开始，`develop` 已包含以下前置工作：

- 郭一坤 `feat/base-guo`（PR#9）、`feat/portal-guo`（PR#13）、`feat/search-guo`（PR#17）、`feat/storage-guo`（PR#21）：登录、四门户布局、API request 层、mock 切换、移交/公众/内部门户页面、内部检索与借阅申请、库房盘点借阅审批/出库/归还。确立了「搬原型 `<body>` → `<template>`、原型 `<style>` → `<style scoped>`、原型 JS → `<script setup>`、API 函数 USE_MOCK 分支、mock 模块、per-subdomain types、api.spec + index.spec 双测试」的落地基线。
- 胡颖 `feat/base-hu`（PR#1/6/7）、`feat/acceptance-hu`（PR#10）、`feat/archive-hu`（PR#15）、`feat/appraisal-hu`（PR#20）、`feat/settings-hu`（PR#25）：管理后台基础布局、验收、入库、档案管理、上架、鉴定、销毁、审批工作台、保存管理、用户管理、系统配置页面，确立了管理后台「顶部指标卡 + 左列表 + 右详情 + 状态 tab + 原生表格/徽章 + Element Plus 用于弹窗/Message/MessageBox」的风格基线，并补齐了公共样式与设计令牌。
- 周扬 `feat/base-zhou/transfer-zhou/archive-zhou/storage-zhou/appraisal-zhou`（PR#2/8/16/19/24）：后端脚手架、移交验收、入库、库房、鉴定销毁、用户管理、系统配置。
- 刘星 `feat/base-liu/file-liu/search-liu/borrow-liu`（PR#3/12/18/22）：OpenAPI、AI 客户端、电子文件暂存、检索下载审计、借阅审批出库归还。

本轮聚焦郭一坤 06-17 至 06-18 前端任务（`feat/stats-guo`）。后端状态：

- **数据统计与研判（§20）后端缺失**（计划属刘星 `feat/stats-liu`，未合并）。本轮基于 `doc/接口文档.md` 第 20 章 + `doc/数据库设计.md`（`analysis_tasks`/`analysis_items` 等）+ 原型 HTML 用 mock 开发。
- **档案编研（§19）后端缺失**（属刘星 `feat/stats-liu`，未合并）。基于第 19 章 + DB（`compilations`/`compilation_materials`）+ 原型 HTML 用 mock 开发。
- **管理概览（§6.1）后端缺失**（dashboard 聚合接口未实现）。基于第 6.1 章 + 原型 HTML 用 mock 开发。

> 接口文档第 20 章统计/研判响应字段为散文描述（非完整 JSON），类型与 mock 字段据「原型设计文档（DB 表引用）+ 原型 HTML 数据结构 + 接口文档散文」推导，**mock 即事实契约**，后端联调时切 `VITE_USE_MOCK=false` 由后端对齐。
>
> **教训（来自 archive-hu，沿用 storage-guo）**：archive-hu 曾有类型错误阻塞 develop 构建。本轮每个阶段主动执行 `npm run type-check && npm run build`，杜绝再次阻塞集成。

## 2. 目标与范围

### 2.1 本轮实现

| 页面/组件 | 路由 | 说明 | 原型 | 角色 |
|-----------|------|------|------|------|
| 管理概览（补齐） | `/admin/overview` | 重写 32 行占位页：8 张指标卡 + 待办提醒列表 + 最近操作日志 + 角色权限提示 + 存储告警语义 | `doc/prototype/admin/overview.html` | front_archivist / back_archivist / director / sys_admin |
| 数据统计 | `/admin/statistics` | 重写占位页：统计口径提示 + 时间筛选 + 核心指标卡 + 纯 CSS 图表（年度进馆/门类分布/载体分布）+ 业务明细表 + 数据源状态 + 导出 | `doc/prototype/admin/statistics.html` | back_archivist / director |
| 数据研判 | `/admin/data-analysis` | 重写占位页：扫描控制 + 任务状态 + 异常建议列表 + 条目详情抽屉（采纳/不采纳）+ AI JSON 展示 + 采纳队列 + 白名单边界 + AI 不可用降级 | `doc/prototype/admin/data-analysis.html` | back_archivist |
| 档案编研 | `/admin/compilation` | 重写占位页：状态指标卡 + 成果列表 + 新建/编辑区（含正文编辑）+ 素材引用区 + 入库确认区 + 状态流转 draft→generated→archived | `doc/prototype/admin/compilation.html` | back_archivist |
| 研判-条目详情抽屉子组件 | `views/admin/data-analysis/components/AnalysisItemDrawer.vue` | 研判项详情 + 采纳/不采纳 + AI 候选 JSON | data-analysis.html 条目详情区 | — |
| 编研-编辑抽屉子组件 | `views/admin/compilation/components/CompilationEditorDrawer.vue` | 新建/编辑成果表单 + 正文编辑 + 素材引用 + 生成正文/入库 | compilation.html 新建/编辑区 | — |

路由 `routes/admin.ts` 已配置四页（`overview` 工作台、`compilation` 编研与保存组、`statistics`/`data-analysis` 统计与分析组），本轮**不改路由**。`overview` 当前为 32 行占位，`statistics`/`data-analysis`/`compilation` 为 6 行 stub，本轮全部重写。

### 2.2 不在本轮

- 后端统计/研判/编研/dashboard 接口实现（刘星 `feat/stats-liu`）。
- 统计报表真实文件流导出（接口文档 §20.3 返回文件流；mock 下用 ElMessage 模拟反馈 + 前端不生成真实 xlsx/pdf）。
- 编研正文富文本格式化工具栏（加粗/列表等），本期正文用原生 `.editor-surface`（contenteditable div）保留 HTML，工具栏留作后续。
- 编研正文 PDF/HTML 真实生成落库到 `business_attachments`（§19.5 副作用），mock 下仅状态流转 + 反馈。
- 研判 AI 真实调用（白名单过滤/JSON 提取在后端），前端 mock 模拟扫描执行中→完成 + 候选建议。
- 工作台指标卡的独立看板聚合接口（§6.1 dashboard 接口后端缺失，mock 提供聚合数据）。
- 保存管理（§21，胡颖 `feat/settings-hu` 已实现 preservation 页）。

## 3. 方案选择

### 3.1 文件组织（per-subdomain，沿用项目约定）

| 方案 | 内容 | 优点 | 缺点 |
|------|------|------|------|
| 拆分四个子域文件（选定） | `types`+`api`+`mock` 各分 `dashboard`/`statistics`/`data-analysis`/`compilation` 四个文件 | 边界清晰，符合 `warehouse`/`inventory`/`borrow-approval`/`appraisal` 既有 per-subdomain 约定；四组 API 端点（§6/19/20）天然分离 | 四套文件 |
| 合并为单一 stats 文件 | 四页共用一个文件 | 文件少 | 语义混杂，与项目惯例不一致 |

**选定**：拆分四个子域文件。其中概览页接口为 dashboard 聚合，单列 `dashboard` 子域；统计/研判虽同属 §20 但端点不同（statistics vs analysis-tasks），分列 `statistics`、`data-analysis`；编研单列 `compilation`。

### 3.2 图表实现（无图表库）

| 方案 | 内容 | 优点 | 缺点 |
|------|------|------|------|
| 纯 CSS 柱状条/分布条（选定） | 沿用原型 `statistics.html` 的 CSS 柱状条（`.bar-chart`/`.bar`/`.dist-bar`）和分布条 | 零新依赖，与原型视觉基准一致，符合「搬原型」原则 | 无交互式图表（tooltip/缩放） |
| 引入 ECharts | 统计/研判用 ECharts 渲染 | 图表更丰富 | 新增大依赖、偏离原型视觉基准、违反简单原则 |

**选定**：纯 CSS 图表，忠实还原原型。原型本就用 CSS 模拟柱状图/分布图，项目无图表库依赖。

### 3.3 编研正文编辑（无富文本库）

| 方案 | 内容 | 优点 | 缺点 |
|------|------|------|------|
| 原生 contenteditable div（选定） | 沿用原型 `.editor-surface`（contenteditable），存储 `contentHtml` | 零新依赖，与原型一致，可保留 HTML 结构 | 无格式工具栏（本期可接受，原型亦无） |
| 引入 wangEditor/tinymce | 富文本编辑器 | 格式化能力强 | 新增依赖、偏离原型、超范围 |
| 纯 textarea | 纯文本输入 | 最简 | 丢失原型 HTML 正文展示效果 |

**选定**：原生 contenteditable div，保留 `contentHtml`（含 h3/p 等），与原型展示一致。

### 3.4 overview 组件拆分

`overview.html` 仅 76 行，结构简单（指标卡网格 + 待办列表 + 日志列表），单文件内联即可，不抽子组件。`data-analysis`/`compilation` 原型较大（435/442 行），抽条目详情/编辑抽屉子组件避免主文件破 600 行（`frontend/CLAUDE.md` 8.1）。`statistics` 原型 363 行，单文件内联。

### 3.5 原型还原原则（沿用 `feat/storage-guo`）

- 页面布局、视觉层级、网格比例、卡片组织、列表/详情关系、状态标签、主要文案优先保持原型原貌。
- 搬入原型 `<body>` 内容区结构和页面内样式，原型 JavaScript 转为 `<script setup>` 响应式状态和事件。
- 原型 CSS 类名、设计令牌、公共组件类（`.card`/`.metric`/`.grid`/`.status`/`.notice`/`.tabs`/`.bar-chart`/`.bar`/`.dist`/`.todo-list`/`.log-row`/`.editor-surface` 等）优先保留；Element Plus 用于弹窗/抽屉/Message/MessageBox/日期选择/表单校验。
- 接口文档和数据库设计用于补齐字段、API、mock、加载/空/错误状态和权限边界，**不推翻原型布局**。
- 展示层保留原型业务语言，数据字段和请求参数按接口文档/数据库设计命名。

## 4. 架构

沿用当前前端架构：

- 页面组件只写内容区，布局由 `AdminLayout` 承担。
- API 函数统一放 `src/api/`，组件只调 API 函数，不直接 import mock。
- mock 数据放 `src/mock/modules/`，通过 `VITE_USE_MOCK` 控制 mock/真实切换。
- 业务类型放 `src/types/`，枚举值复用并扩展 `types/enums.ts`。
- 页面内用 Vue `ref`/`reactive` 管理局部状态，不引入新全局 store。
- 原型公共样式已全局引入，页面复用公共类。
- `request.ts` 响应拦截器已对 `R<T>` 解包，API 函数返回类型为 `data` 字段内容；mock 只模拟 `data` 内容，分页接口模拟完整 `PageData<T>`。
- 审计字段遵循 `BaseEntity` 约定（`id` number、`createdAt`/`updatedAt` ISO 8601 字符串）。

## 5. 文件结构

```
frontend/src/types/
  enums.ts                       # 扩展：编研/研判任务与建议状态、研判任务类型、问题类型枚举与中文映射
  dashboard.ts                   # 管理概览聚合统计类型（§6.1）
  statistics.ts                  # 统计总览/分类统计类型（§20.1-20.3）
  data-analysis.ts               # 研判任务/研判项/创建/处理类型（§20.4-20.7）
  compilation.ts                 # 编研成果/素材/创建/更新/入库类型（§19.1-19.6）

frontend/src/api/
  dashboard.ts                   # §6.1 GET /api/admin/dashboard
  statistics.ts                  # §20.1-20.3 统计总览/分类/导出
  data-analysis.ts               # §20.4-20.7 研判任务查询/创建/详情 + 研判项处理
  compilation.ts                 # §19.1-19.6 编研成果 CRUD + 生成正文 + 入库

frontend/src/mock/modules/
  dashboard.ts                   # 概览聚合 mock 数据
  statistics.ts                  # 统计总览/分类 mock 数据
  data-analysis.ts               # 研判任务/研判项 mock 数据
  compilation.ts                 # 编研成果/素材 mock 数据

frontend/src/views/admin/
  overview/index.vue                                  # 管理概览（重写补齐）
  statistics/index.vue                                # 数据统计（重写）
  data-analysis/index.vue                             # 数据研判（重写）
  data-analysis/components/AnalysisItemDrawer.vue     # 研判项详情抽屉
  compilation/index.vue                               # 档案编研（重写）
  compilation/components/CompilationEditorDrawer.vue  # 编研新建/编辑抽屉
```

测试文件（`.spec.ts`）与各 `api/`、`views/` 源文件同目录，对齐既有约定。

## 6. 枚举扩展（`types/enums.ts`）

复用既有 `SourceType`（含 `compilation`）、`CarrierStatus`、`RetentionPeriod`、`SecurityLevel`、`OpenStatus`、`ArchiveLifecycleStatus`、`BorrowStatus`、`DestructionListStatus`。新增编研/研判相关枚举：

```typescript
/** 编研成果状态（对齐 DB compilations.status） */
export const CompilationStatus = {
  DRAFT: 'draft',
  GENERATED: 'generated',
  ARCHIVED: 'archived',
} as const

/** 研判任务状态（对齐 DB analysis_tasks.status） */
export const AnalysisTaskStatus = {
  RUNNING: 'running',
  COMPLETED: 'completed',
  FAILED: 'failed',
} as const

/** 研判任务类型（§20.5 taskType） */
export const AnalysisTaskType = {
  RULE: 'rule',       // 仅规则扫描
  AI: 'ai',           // 仅 AI 建议
  MIXED: 'mixed',     // 规则 + AI
} as const

/** 研判建议处理状态（对齐 DB analysis_items.status） */
export const AnalysisItemStatus = {
  PENDING: 'pending',
  ADOPTED: 'adopted',
  REJECTED: 'rejected',
} as const

/** 研判问题类型（候选异常分类） */
export const AnalysisProblemType = {
  MISSING_FIELD: 'missing_field',       // 元数据缺失
  CATEGORY_CONFLICT: 'category_conflict', // 分类冲突
  TAG_SUGGESTION: 'tag_suggestion',     // 标签建议
  DATE_ABNORMAL: 'date_abnormal',       // 形成日期异常
  DUPLICATE: 'duplicate',               // 疑似重复
} as const

/** 研判建议处理动作（§20.7 action） */
export const AnalysisHandleAction = {
  ADOPTED: 'adopted',
  REJECTED: 'rejected',
} as const

// 中文映射
export const CompilationStatusLabel: Record<string, string> = {
  draft: '草稿',
  generated: '已生成',
  archived: '已入库',
}
export const AnalysisTaskStatusLabel: Record<string, string> = {
  running: '执行中',
  completed: '已完成',
  failed: '失败',
}
export const AnalysisTaskTypeLabel: Record<string, string> = {
  rule: '规则扫描',
  ai: 'AI 建议',
  mixed: '规则 + AI',
}
export const AnalysisItemStatusLabel: Record<string, string> = {
  pending: '待处理',
  adopted: '已采纳',
  rejected: '已不采纳',
}
export const AnalysisProblemTypeLabel: Record<string, string> = {
  missing_field: '缺失字段',
  category_conflict: '分类冲突',
  tag_suggestion: '标签建议',
  date_abnormal: '日期异常',
  duplicate: '疑似重复',
}
```

派生值类型沿用既有 `enums.ts` 模式（`export type XxxValue = (typeof Xxx)[keyof typeof Xxx]`）：`CompilationStatusValue`、`AnalysisTaskStatusValue`、`AnalysisTaskTypeValue`、`AnalysisItemStatusValue`、`AnalysisProblemTypeValue`、`AnalysisHandleActionValue`。

## 7. 类型设计

字段名严格对齐 `doc/接口文档.md`（§6.1/19/20）、`doc/数据库设计.md`（`compilations`/`compilation_materials`/`analysis_tasks`/`analysis_items`/`archives`/`audit_logs`）、原型 HTML 数据结构。接口文档第 20 章响应为散文，字段据原型 + DB 推导。

### 7.1 `types/dashboard.ts`（§6.1）

```typescript
/** 概览待办计数（§6.1 todos） */
export interface DashboardTodos {
  /** 待验收移交清单 */
  pendingTransferReception: number
  /** 待入库条目 */
  pendingArchive: number
  /** 待审批借阅 */
  borrowApproval: number
  /** 待审批事项（开放/密级/销毁） */
  approvalPending: number
  /** 待销毁清册 */
  pendingDestruction: number
  /** 待上架纸质档案 */
  pendingShelf: number
  /** 到期鉴定提醒 */
  appraisalDue: number
  /** 新借阅申请（与 borrowApproval 口径区分：本月新增） */
  newBorrowRequests: number
}

/** 馆藏汇总（§6.1 archiveSummary，原型 8 卡取数） */
export interface ArchiveSummary {
  /** 馆藏总量 */
  totalArchives: number
  /** 本月新增 */
  monthAdded: number
  /** 存储使用率 0~1 */
  storageUsage: number
  /** 存储告警阈值 0~1，默认 0.85 */
  storageWarningThreshold: number
}

/** 库房告警项（§6.1 warehouseWarnings） */
export interface WarehouseWarning {
  roomNo: string
  roomName: string
  occupancyRate: number
  warningThreshold: number
}

/** 最近操作日志行（§6.1 recentAuditLogs，原型 log-row） */
export interface RecentAuditLog {
  id: number
  operator: string
  module: string
  action: string
  operatedAt: string
}

/** 待办提醒项（原型 todo-list，跳转入口 + 筛选意图） */
export interface OverviewTodo {
  key: string
  title: string
  description: string
  count: number
  /** 跳转目标路由 */
  targetRoute: string
  /** 跳转携带的 query 筛选 */
  targetQuery?: Record<string, string>
  /** 语义色：info/warning/danger */
  severity: 'info' | 'warning' | 'danger'
}

/** 概览聚合响应（§6.1 GET /api/admin/dashboard） */
export interface DashboardSummary {
  todos: DashboardTodos
  archiveSummary: ArchiveSummary
  warehouseWarnings: WarehouseWarning[]
  recentAuditLogs: RecentAuditLog[]
  /** 待办跳转入口（前端据 todos + 角色派生） */
  todoEntries: OverviewTodo[]
  /** 最近汇总时间 */
  summarizedAt: string
}
```

### 7.2 `types/statistics.ts`（§20.1-20.3）

```typescript
/** 统计查询参数（§20.1/20.3 yearStart/yearEnd/organizationId/fondsId） */
export interface StatisticsParams {
  yearStart?: number
  yearEnd?: number
  organizationId?: number
  fondsId?: number
}

/** 统计指标卡项 */
export interface StatisticsMetric {
  key: string
  label: string
  value: number
  unit?: string
  /** 来源表口径说明 */
  source: string
  /** 跳转路由 + 筛选 */
  targetRoute?: string
  targetQuery?: Record<string, string>
}

/** 年度进馆柱状项（原型 bar-chart） */
export interface YearlyIntakeBar {
  year: number
  count: number
}

/** 分布项（门类/载体/密级/公开等通用） */
export interface DistributionItem {
  label: string
  value: number
  /** 占比 0~1 */
  ratio: number
}

/** 业务明细行（移交/征集/借阅/销毁/保存检测） */
export interface BusinessBreakdownRow {
  key: string
  domain: string
  total: number
  /** 子状态计数 */
  details: { label: string; count: number }[]
  /** 来源表 */
  sourceTable: string
}

/** 数据源状态项 */
export interface DataSourceStatus {
  table: string
  label: string
  healthy: boolean
  lastSyncedAt: string
}

/** 统计总览响应（§20.1） */
export interface StatisticsOverview {
  /** 核心指标卡 */
  metrics: StatisticsMetric[]
  /** 年度进馆趋势 */
  yearlyIntake: YearlyIntakeBar[]
  /** 档案门类分布 */
  categoryDistribution: DistributionItem[]
  /** 纸电载体分布 */
  carrierDistribution: DistributionItem[]
  /** 业务明细 */
  businessBreakdown: BusinessBreakdownRow[]
  /** 数据源状态 */
  dataSources: DataSourceStatus[]
  /** 最近汇总时间 */
  summarizedAt: string
}

/** 分类统计响应（§20.2，按门类/年度/来源/载体/密级/公开状态分组） */
export interface StatisticsCategories {
  byCategory: DistributionItem[]
  byYear: YearlyIntakeBar[]
  bySourceType: DistributionItem[]
  byCarrier: DistributionItem[]
  bySecurityLevel: DistributionItem[]
  byOpenStatus: DistributionItem[]
}

/** 导出结果（§20.3，mock 用反馈） */
export interface StatisticsExportResult {
  format: 'xlsx' | 'pdf'
  /** mock 下不生成真实文件，仅反馈 */
  message: string
}
```

### 7.3 `types/data-analysis.ts`（§20.4-20.7）

```typescript
import type { PageData, PageParams } from './api'
import type {
  AnalysisHandleActionValue, AnalysisItemStatusValue,
  AnalysisProblemTypeValue, AnalysisTaskStatusValue, AnalysisTaskTypeValue,
} from './enums'

/** 研判扫描规则（§20.5 rule） */
export interface AnalysisRule {
  categoryIds?: number[]
  formedYearStart?: number
  formedYearEnd?: number
  includeAiSuggestion?: boolean
}

/** 创建研判任务请求（§20.5） */
export interface AnalysisTaskCreateData {
  taskType: AnalysisTaskTypeValue
  rule: AnalysisRule
}

/** 研判任务查询参数（§20.4） */
export interface AnalysisTaskParams extends PageParams {
  status?: AnalysisTaskStatusValue | ''
  taskType?: AnalysisTaskTypeValue | ''
}

/** AI 候选建议 JSON（ruleType=dataAnalysis，原型 AI JSON 区） */
export interface AnalysisSuggestionJson {
  ruleType: 'dataAnalysis'
  archiveId: number
  archiveNo: string
  /** 候选建议字段（经白名单过滤后） */
  candidates: {
    field: string
    currentValue?: string
    suggestedValue: string
    confidence: number
  }[]
}

/** 研判建议项（DB analysis_items + 展示字段） */
export interface AnalysisItem {
  id: number
  taskId: number
  archiveId: number
  archiveNo: string
  title: string
  problemType: AnalysisProblemTypeValue
  /** 问题描述 */
  problemDesc: string
  /** 建议动作 */
  suggestedAction: string
  status: AnalysisItemStatusValue
  /** AI 候选 JSON（命中受保护字段已剥离） */
  suggestion?: AnalysisSuggestionJson
  handleNote?: string
  handledAt?: string
  handledBy?: string
}

/** 研判任务摘要（列表行） */
export interface AnalysisTask {
  id: number
  taskNo: string
  taskType: AnalysisTaskTypeValue
  status: AnalysisTaskStatusValue
  /** 扫描范围描述 */
  scopeText: string
  /** 扫描档案数 */
  scannedCount: number
  /** 异常建议数 */
  abnormalCount: number
  /** 已采纳数 */
  adoptedCount: number
  /** 进度 0~1 */
  progress: number
  startedAt?: string
  completedAt?: string
  createdAt: string
}

/** 研判任务详情（§20.6） */
export interface AnalysisTaskDetail extends AnalysisTask {
  rule: AnalysisRule
  items: AnalysisItem[]
}

/** 处理研判项请求（§20.7） */
export interface AnalysisItemHandleData {
  action: AnalysisHandleActionValue
  note?: string
}

export type AnalysisTaskPage = PageData<AnalysisTask>
```

### 7.4 `types/compilation.ts`（§19.1-19.6）

```typescript
import type { PageData, PageParams } from './api'
import type { CompilationStatusValue } from './enums'

/** 编研成果查询参数（§19.1） */
export interface CompilationParams extends PageParams {
  status?: CompilationStatusValue | ''
  keyword?: string
}

/** 编研素材引用项（DB compilation_materials，只引用档号） */
export interface CompilationMaterial {
  id: number
  compilationId: number
  archiveId: number
  archiveNo: string
  title: string
  /** 引用说明 */
  referenceNote?: string
}

/** 编研正文附件（§19.5 生成） */
export interface CompilationAttachment {
  id: number
  fileName: string
  fileUrl: string
  attachmentType: 'report'
  generatedAt: string
}

/** 创建/更新编研草稿请求（§19.2/19.4） */
export interface CompilationWriteData {
  title: string
  compilationType: string
  dateRangeText?: string
  keywords?: string
  summary?: string
  contentHtml?: string
  materialArchiveIds: number[]
}

/** 编研成果（DB compilations + 展示字段） */
export interface Compilation {
  id: number
  compilationNo: string
  title: string
  compilationType: string
  dateRangeText?: string
  keywords?: string
  summary?: string
  contentHtml?: string
  status: CompilationStatusValue
  materialCount: number
  /** 正文附件（generated/archived 时存在） */
  attachment?: CompilationAttachment
  /** 入库后正式档号（archived 时存在） */
  archiveNo?: string
  /** 入库档案 ID */
  archiveId?: number
  createdBy: string
  createdAt: string
  updatedAt: string
}

/** 编研成果详情（§19.3） */
export interface CompilationDetail extends Compilation {
  materials: CompilationMaterial[]
}

/** 素材档案筛选参数（管理端档案查询复用，简化） */
export interface MaterialSearchParams {
  keyword?: string
  categoryId?: number
  year?: number
  tag?: string
}

/** 素材档案候选（引用选择，只读摘要） */
export interface MaterialCandidate {
  id: number
  archiveNo: string
  title: string
  categoryName: string
  formedYear: number
  tags: string[]
}

/** 编研入库请求（§19.6） */
export interface CompilationArchiveData {
  fondsId: number
  categoryId: number
  formedDate: string
  retentionPeriod: string
  openStatus: string
  tagNames?: string[]
}

/** 编研状态指标卡（原型状态卡） */
export interface CompilationStatusMetrics {
  draftCount: number
  generatedCount: number
  archivedCount: number
  monthAdded: number
}

export type CompilationPage = PageData<Compilation>
export type MaterialCandidatePage = PageData<MaterialCandidate>
```

## 8. API 设计

所有函数内部通过 `USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'` 控制 mock/真实请求。真实端点严格对齐接口文档（§6.1/19/20）。

### 8.1 `api/dashboard.ts`（§6.1）

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `getDashboardSummary()` | GET | `/admin/dashboard` | 概览聚合（待办计数 + 馆藏汇总 + 库房告警 + 最近日志 + 待办入口 + 汇总时间） |

> 顶部指标卡与待办入口由 dashboard 聚合返回；前端据当前角色渲染入口可见性（角色切换为前端示例，提示实际由后端权限过滤）。

### 8.2 `api/statistics.ts`（§20.1-20.3）

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `getStatisticsOverview(params?)` | GET | `/admin/statistics/overview` | 核心指标 + 年度进馆 + 门类/载体分布 + 业务明细 + 数据源状态 |
| `getStatisticsCategories()` | GET | `/admin/statistics/categories` | 按门类/年度/来源/载体/密级/公开状态分组 |
| `exportStatistics(params, format)` | GET | `/admin/statistics/export` | 导出 xlsx/pdf；mock 用 ElMessage 反馈 |

### 8.3 `api/data-analysis.ts`（§20.4-20.7）

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `getAnalysisTasks(params)` | GET | `/admin/analysis-tasks` | 研判任务分页（status/taskType 过滤） |
| `createAnalysisTask(data)` | POST | `/admin/analysis-tasks` | 创建任务（taskType + rule），返回任务（初始 running/扫描中） |
| `getAnalysisTaskDetail(taskId)` | GET | `/admin/analysis-tasks/{taskId}` | 任务 + 异常项 + AI 状态 |
| `handleAnalysisItem(itemId, data)` | POST | `/admin/analysis-items/{itemId}/handle` | 采纳/不采纳（action + note），仅改研判项状态 |

> 「开始扫描」语义：前端调用 `createAnalysisTask` 创建任务（running），mock 下模拟扫描进度→completed 并生成候选建议；详情接口返回 items + suggestion。AI 不可用降级：taskType=rule 仅规则扫描建议。

### 8.4 `api/compilation.ts`（§19.1-19.6）

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `getCompilations(params)` | GET | `/admin/compilations` | 编研成果分页（status/keyword 过滤） |
| `createCompilation(data)` | POST | `/admin/compilations` | 创建草稿（status=draft） |
| `getCompilationDetail(id)` | GET | `/admin/compilations/{id}` | 草稿 + 素材引用 + 附件 + 入库档案 |
| `updateCompilation(id, data)` | PUT | `/admin/compilations/{id}` | 更新草稿（仅 draft/generated 可编辑） |
| `generateCompilationBody(id)` | POST | `/admin/compilations/{id}/generate` | 生成正文附件（draft→generated） |
| `archiveCompilation(id, data)` | POST | `/admin/compilations/{id}/archive` | 纯电子入库（generated→archived，生成正式档号） |
| `searchMaterials(params)` | GET | `/admin/archives` | 素材档案候选（复用档案管理查询，简化字段） |

> 素材候选复用档案管理查询端点（§10.1 `/admin/archives`），前端取 `MaterialCandidate` 摘要字段；不新增独立端点。

## 9. 页面设计

> **顶部指标卡取数**：dashboard（§6.1）提供聚合数据；statistics（§20）独立查询。概览页指标卡与待办入口由 `getDashboardSummary()` 一次返回；统计页指标卡由 `getStatisticsOverview()` 返回。

### 9.1 管理概览（补齐） `/admin/overview`

**目标**：作为后台入口页，按角色展示统计指标卡、待办提醒、风险告警和最近操作日志，只做提醒与跳转，不直接执行审批/销毁/批量修改。

**布局**（还原 `overview.html`）：

- `toolbar`：`page-title`（管理概览）+ `page-subtitle`（今日工作入口）+ 角色切换下拉（前端示例：后台管理员/前台管理员/馆领导/系统管理员）。
- `notice`（`roleNotice`）：角色权限提示 ——「当前示例角色：xx。实际入口和统计由后端按角色、密级和数据范围过滤。」切换角色时更新提示文字。
- `grid four`：8 张 `metric` 指标卡，每张带跳转目标 + 来源说明 + query 筛选意图：
  1. 馆藏总量 → `archive-management`（note：正式档案 archives）
  2. 本月新增 → `archive-management?month=current`（note：入库完成记录）
  3. 待入库条目 → `pending-archive`（note：已接收/部分接收）
  4. 待审批 → `approval`（note：借阅、开放、密级、销毁）
  5. 待销毁清册 → `destruction`（note：馆领导已批准）
  6. 到期提醒 → `appraisal`（note：进入鉴定范围）
  7. 存储使用率 → `preservation`（note：超过 85% 告警线，达阈值告警色 `danger`）
  8. 新借阅申请 → `borrow-approval`（note：待后台审核）
- `grid two`：
  - 左 `card panel`「待办提醒」`todo-list`：每条 `todo`（标题 + 说明 + 计数徽章 severity）可跳转。待验收移交清单（warning 12 批）、待 AI 补全与入库（info 37 条）、待上架纸质档案（warning 11 件）、存储空间告警（danger 86%）。
  - 右 `card panel`「最近操作日志」`log-row` 列表：操作人/模块/操作/时间。

**行为**：

- 首次加载 `getDashboardSummary()` → 填充指标卡（`archiveSummary.totalArchives`/`monthAdded`/`storageUsage`、`todos.*` 计数）、待办入口（`todoEntries`）、库房告警（`warehouseWarnings`）、最近日志（`recentAuditLogs`）。
- 指标卡点击 → `router.push({ path: targetRoute, query: targetQuery })` 保留筛选意图。
- 待办项点击 → 同上跳转。
- 角色切换 → 仅更新 `notice` 文字（前端示例），不改实际数据（提示由后端权限过滤）。
- 存储使用率 ≥ 阈值（默认 0.85）→ 指标卡与待办项用 `danger`/`warning` 语义色。
- 三态：聚合加载中（骨架）、聚合为空、聚合加载失败 + 重试。

### 9.2 数据统计 `/admin/statistics`

**目标**：只读展示馆藏、移交、征集、借阅、销毁、保存等指标（来自正式业务表），支持时间范围筛选、卡片带条件跳转和导出，不修改任何业务数据。

**布局**（还原 `statistics.html`）：

- `toolbar`：`page-title`（数据统计）+ `page-subtitle`（口径提示）+ 时间范围筛选（年度起止下拉 `yearStart`/`yearEnd` + 组织/全宗可选）+「刷新」按钮。
- `notice`：统计口径提示 —— 馆藏来自 `archives`，移交/征集来自 `intake_batches`，借阅来自 `borrow_requests`，销毁来自 `destruction_lists`，保存来自 `backup_tasks`/`file_check_records`。
- `grid four`：4 张可点击 `metric` 卡（馆藏总量→`archive-management?lifecycle_status=normal`、本月新增→`archive-management?archived_at=this_month`、待入库/待上架→`pending-archive?status=pending_archive`、待审批销毁清册→`destruction?status=pending_approval`），每张带来源说明。
- `grid two` 图表区（纯 CSS）：
  - `card panel`「年度进馆趋势」：`bar-chart` 柱状条（按 `yearlyIntake` 渲染，柱高按最大值比例）。
  - `card panel`「档案门类分布」：`dist` 分布条（`categoryDistribution`，按 `ratio` 宽度 + 数值）。
  - `card panel`「纸电载体分布」：`dist` 分布条（`carrierDistribution`，electronic/paper_electronic/paper 三类带 status 色）。
- `card panel`「业务汇总」`table-wrap` 表格：行 = 移交/征集/借阅/销毁/保存检测（`businessBreakdown`），列 = 业务域/总数/状态明细/来源表。
- `card panel`「数据源状态」列表：`dataSources`（正式档案主表/移交征集批次/借阅申请/销毁清册/备份检测，healthy 状态 + lastSyncedAt）。
- `card panel`「领导关注指标」：衍生指标（馆藏总量、本月新增、销毁审批统计、趋势摘要）。
- `notice warning`：只读边界 —— 统计页不修改正式档案，不执行审批或销毁。
- 导出「导出统计表」按钮 → `exportStatistics(params, format)` → ElMessage 反馈（mock 不生成真实文件）。

**行为**：

- 首次加载 `getStatisticsOverview(params)` + `getStatisticsCategories()` → 渲染指标卡/图表/明细/数据源/分类分布。
- 切换时间范围（yearStart/yearEnd）+ 刷新 → 重新 `getStatisticsOverview(params)`，提示「来源表不变，按时间范围更新」。
- 指标卡点击 → `router.push` 带筛选意图。
- 导出 → `exportStatistics` → toast「已生成 xx 口径统计表（模拟）」。
- 数据源异常（`healthy=false`）→ 对应项 `warning`/`danger` 色 + 提示。
- 三态：加载中、无数据、加载失败 + 重试。

### 9.3 数据研判 `/admin/data-analysis`

**目标**：扫描正式档案元数据，生成缺失字段/分类冲突/标签建议等异常候选（规则扫描 + AI 建议），采纳后进入待处理清单，**不在本页直接改库**；AI 受白名单约束，受保护字段不可直接覆盖。

**布局**（还原 `data-analysis.html`）：

- `toolbar`：`page-title`（数据研判）+ `page-subtitle`（生成异常建议，不在本页直接改库）。
- `notice warning`：AI 安全边界 —— AI 只写 `analysis_items.suggestion`，不直接改档案；受保护字段（密级/保管期限/公开状态/档号/架位/销毁状态）禁止直接覆盖。
- `card panel`「扫描控制」：扫描范围（分类多选 `categoryIds`）、形成年度起止、任务类型（rule/ai/mixed）、每批数量、AI 建议开关（`includeAiSuggestion`）+「开始扫描」按钮。
- `card panel`「任务状态」：任务号、状态徽章（`taskStatus`：未开始/执行中/已完成）、扫描数、异常建议数、已采纳数、进度条。
- `card panel`「异常建议列表」`table-wrap`：列 = 档号/题名/问题类型（`problemType` 徽章）/问题描述/建议动作/处理状态（`data-role=state`：待处理/已采纳/已不采纳）。行可点击 → 载入条目详情。
- `card panel`「条目详情」（`AnalysisItemDrawer` 抽屉，承载 §20.7）：选中项的档号/题名/问题类型/描述/建议动作 + AI 候选字段 + 「采纳」「不采纳」按钮 + 处理备注 textarea。
- `card panel`「AI JSON 建议」：`<pre>` 展示选中项 `suggestion`（`ruleType=dataAnalysis` 结构化候选）。
- `card panel`「字段边界」：可建议字段 success 徽章（title/responsible/formedDate/category/tags/summary）+ 禁止字段 danger 徽章（密级/保管期限/公开状态/档号/架位）。
- `card panel`「待处理队列」：已采纳项列表 + 「生成待补充清单」按钮（无采纳项时提示先采纳）。
- `notice`：后续需在档案管理页面人工处理并记录变更日志。
- `toast`：操作反馈。

**行为**：

- 首次加载 `getAnalysisTasks(params)` → 任务列表（支持 status/taskType 过滤 + 分页）；默认选中最近任务 → `getAnalysisTaskDetail(id)` 加载异常项。
- 「开始扫描」`createAnalysisTask({ taskType, rule })` → 新任务 running，mock 下模拟扫描进度（`progress` 0→1）→ completed 并生成候选 `items`（含 `suggestion`）；刷新任务状态与列表。
- 选中建议项 → 详情区/AI JSON 展示该项。
- 「采纳」`handleAnalysisItem(itemId, { action: 'adopted', note })` → 项状态 pending→adopted，加入待处理队列；「不采纳」`{ action: 'rejected', note }` → pending→rejected。仅改研判项状态，不动正式档案。
- 「生成待补充清单」：无 adopted 项时提示「请先采纳建议」；有则输出后续处理反馈（跳转档案管理或导出清单提示）。
- AI 不可用降级：taskType=rule 或 AI 开关关闭时，仅规则扫描建议（无 `suggestion` JSON 或标注重格扫描）。
- 三态：任务列表加载中/空/失败；明细加载中/空/失败；操作 toast 反馈。

### 9.4 档案编研 `/admin/compilation`

**目标**：编研成果以 draft→generated→archived 三阶段流转；素材只保存引用关系（不复制正文、不改素材档案）；入库后生成 `sourceType=compilation` 的纯电子正式档案与正文文件。

**布局**（还原 `compilation.html`）：

- `toolbar`：`page-title`（档案编研）+ `page-subtitle`（三阶段流转 + 素材只引用）+「新建编研成果」按钮 → 打开 `CompilationEditorDrawer`。
- `grid four`：4 张状态指标卡（草稿 `draftCount`、已生成 `generatedCount`、已入库 `archivedCount`、本月新增 `monthAdded`）。
- `card panel`「编研成果列表」：`status-line`（`compilations`/`compilation_materials`/`archive_files.compilation_body` 口径徽章）+ `table-wrap` 表格（编号/题名/类型/状态徽章/素材数/正文文件状态/入库档号）+ 行操作（查看/编辑/继续）。支持 status/keyword 筛选 + 分页。
- `card panel`「成果编辑」（`CompilationEditorDrawer` 抽屉承载新建/编辑）：表单 —— 标题、类型（专题汇编/大事记/组织史等下拉）、时间范围 `dateRangeText`、关键词、摘要 `textarea`、正文 `.editor-surface`（contenteditable div，存 `contentHtml`）。
- `notice`：素材只记录引用关系，不复制正文，不改变素材档案；入库后来源为 compilation，载体为纯电子。
- `card panel`「素材引用」：筛选（题名/分类/年度/标签）+ `searchMaterials(params)` 候选列表 + 「添加素材」勾选（写入 `materialArchiveIds`，只引用档号）+ 已引用素材列表（可移除）。
- `card panel`「正文文件与入库确认」：正文文件状态（`bodyFileCheck`：未生成/已生成）+ 纯电子徽章 + 「保存草稿」「生成正文文件」「确认纯电子入库」按钮 + 入库元数据（全宗/分类/形成日期/保管期限/公开状态/标签）。

**状态流转与校验**：

- 新建 → `createCompilation(data)`（status=draft）。
- 保存草稿 → `updateCompilation(id, data)`（draft/generated 可编辑，archived 只读）。
- 生成正文文件 → `generateCompilationBody(id)`（draft→generated，生成附件 `attachmentType=report`）。
- 确认纯电子入库 → `archiveCompilation(id, { fondsId, categoryId, formedDate, retentionPeriod, openStatus, tagNames })`（generated→archived，生成正式档号 + 正式档案 + 正文文件记录）。
- **入库前校验**：必须有标题、摘要、≥1 素材引用、已生成正文文件；缺项高亮 + 阻断入库（提示「先生成正文文件」/「请添加至少一个素材」等）。
- 素材无权限或已销毁 → 禁止引用并提示原因。

**行为**：

- 首次加载 `getCompilations(params)` + 状态指标 → 列表 + 4 卡（指标由列表派生或 mock 聚合）。
- 选中成果 → `getCompilationDetail(id)` → 抽屉载入草稿/素材/附件/入库信息。
- 状态门控：draft 可编辑/生成正文；generated 可编辑/生成正文/入库；archived 只读展示档号与正文。
- 三态：列表加载中/空/失败；详情加载中/失败；操作 toast 反馈；敏感操作（生成正文/入库）二次确认。

## 10. 跨页联动

- **概览 → 业务页**：概览指标卡与待办入口跳转目标（archive-management/pending-archive/approval/destruction/appraisal/preservation/borrow-approval）保留 query 筛选意图，对应页支持接收 query 预筛选（若目标页暂未支持，跳转仍生效，筛选为后续增强）。
- **统计 → 业务页**：统计指标卡带条件跳转（如 `archive-management?lifecycle_status=normal`），与概览跳转口径一致。
- **研判 → 档案管理**：研判采纳后「生成待补充清单」指向档案管理页面人工处理（跳转 + 提示），不在研判页改库。
- **编研 → 档案管理/检索**：编研素材引用来源为正式档案（`searchMaterials` 复用 `/admin/archives`）；入库后生成 `sourceType=compilation` 正式档案，可在档案管理页查看。
- 四页各自 mock 数据自闭环（不依赖跳转才能工作），联动仅提升链路连续性。

## 11. 错误处理与边界

- 每页/组件必须处理加载中、空数据、接口失败、表单校验失败（`frontend/CLAUDE.md` AI 约束）。
- 捕获 API 异常后展示页面级 `notice` 或 Element Plus 消息，避免静默失败。
- **概览**：聚合接口失败 → 整页失败态 + 重试；角色切换仅前端示例，不改后端数据；存储告警按阈值渲染语义色；概览无危险操作按钮。
- **统计**：只读边界（不修改业务数据）；时间范围 yearStart ≤ yearEnd 校验；导出 mock 反馈；数据源异常高亮。
- **研判**：AI 建议不直接改库；采纳/不采纳仅改研判项状态；受保护字段白名单边界提示；AI 不可用降级为规则扫描；已销毁档案只展示风险提示不提供处理入口。
- **编研**：素材只引用不复制/不改素材档案；入库前校验（标题/摘要/≥1 素材/正文文件）；仅 draft/generated 可编辑；素材无权限或已销毁禁止引用；纯电子入库。
- 不可逆/敏感操作（研判生成待补充清单、编研生成正文、编研入库）二次确认（`ElMessageBox.confirm`）。
- mock 下文件导出/正文生成用前端模拟，不落库。
- 枚举值与状态标签复用 `enums.ts`，四页状态展示一致。
- 本轮不改胡颖已合并页面与已合并的 search/storage 页面；共享语义（SourceType/CarrierStatus 等）只通过 `types/enums.ts` 保持一致。

## 12. Mock 数据

`src/mock/modules/`，结构对齐接口 `data` 字段内容（不包 `R<T>`），分页模拟完整 `PageData<T>`。字段严格对齐接口文档 + DB 设计 + 原型 HTML 数据。

### 12.1 `mock/modules/dashboard.ts`

- `mockDashboardSummary()`：聚合数据 —— `todos`（8 类计数，与原型示意数值接近）、`archiveSummary`（totalArchives/monthAdded/storageUsage=0.86 触发告警）、`warehouseWarnings`（≥1 库房达阈值）、`recentAuditLogs`（≥5 条，操作人/模块/操作/时间）、`todoEntries`（4 条跳转入口 + severity + query）、`summarizedAt`。

### 12.2 `mock/modules/statistics.ts`

- `mockStatisticsOverview(params?)`：`metrics`（4 卡 + 来源 + 跳转）、`yearlyIntake`（≥6 年柱状）、`categoryDistribution`（≥5 门类）、`carrierDistribution`（electronic/paper_electronic/paper 三类）、`businessBreakdown`（移交/征集/借阅/销毁/保存检测 5 行 + 状态明细 + 来源表）、`dataSources`（5 源 + healthy + lastSyncedAt，1 源延迟汇总）、`summarizedAt`。支持 yearStart/yearEnd/organizationId/fondsId 过滤（mock 下按范围裁剪数值）。
- `mockStatisticsCategories()`：按门类/年度/来源/载体/密级/公开状态 6 维分组。
- `mockExportStatistics(params, format)`：返回 `{ format, message }`，不生成真实文件。

### 12.3 `mock/modules/data-analysis.ts`

- `mockAnalysisTasks(params)`：≥4 任务覆盖 running/completed/failed；含 taskNo/taskType/scopeText/scannedCount/abnormalCount/adoptedCount/progress；支持 status/taskType 过滤 + 分页。
- `mockCreateAnalysisTask(data)`：生成 `taskNo`（`FX-yyyyMM-xxx`），初始 running，模拟扫描进度→completed，按 rule 生成候选 `items`（覆盖 missing_field/category_conflict/tag_suggestion/date_abnormal/duplicate 五类问题，部分带 `suggestion` JSON）。
- `mockAnalysisTaskDetail(id)`：任务 + `items[]`（含 problemType/problemDesc/suggestedAction/status/suggestion/handleNote）。
- `mockHandleAnalysisItem(itemId, data)`：pending→adopted（action=adopted）/pending→rejected（action=rejected），写 handleNote/handledAt/handledBy；非 pending 状态不可重复处理（演示校验）。

### 12.4 `mock/modules/compilation.ts`

- `mockCompilations(params)`：≥5 成果覆盖 draft/generated/archived；含 compilationNo/title/type/status/materialCount/attachment/archiveNo；支持 status/keyword 过滤 + 分页。
- `mockCompilationDetail(id)`：成果 + `materials[]`（archiveNo/title/referenceNote）+ attachment + archiveNo（archived 时）。
- `mockCreateCompilation(data)`：生成 `compilationNo`（`BY-yyyyMM-xxx`），status=draft，materialCount=materialArchiveIds.length。
- `mockUpdateCompilation(id, data)`：仅 draft/generated 可改（archived 抛错），更新字段，同步 materialCount。
- `mockGenerateCompilationBody(id)`：draft→generated，生成 attachment（`attachmentType=report`，fileName/fileUrl/generatedAt）。
- `mockArchiveCompilation(id, data)`：generated→archived 校验（须已生成正文 + ≥1 素材 + 标题/摘要），生成正式档号（`AJ-compile-xxxxx`）+ archiveId。
- `mockSearchMaterials(params)`：≥8 候选正式档案（archiveNo/title/categoryName/formedYear/tags），支持 keyword/categoryId/year/tag 过滤 + 分页。

mock 数据需保证口径一致：编研素材候选与档案管理 mock 的 archiveNo/分类一致；研判 archiveNo 与档案管理口径一致；统计来源表与各业务页一致。

## 13. 测试与验证

### 13.1 单元测试

- `api/dashboard.spec.ts`、`api/statistics.spec.ts`、`api/data-analysis.spec.ts`、`api/compilation.spec.ts`：mock 分支返回结构与类型正确性、过滤逻辑、分页结构、状态流转。
- 研判：创建任务生成候选 items；处理仅 pending 可改状态；任务状态过滤。
- 编研：入库前校验（缺正文/缺素材/缺标题阻断）；状态门控（archived 只读）；生成正文 draft→generated；入库 generated→archived 生成档号。

### 13.2 组件交互测试

- `/admin/overview`：指标卡跳转带 query；待办入口跳转；角色切换更新提示；存储告警语义色。
- `/admin/statistics`：时间范围切换刷新；卡片跳转；导出反馈；纯 CSS 图表按数据渲染；数据源异常高亮。
- `/admin/data-analysis`：扫描控制创建任务；任务状态过滤；建议列表选中载入详情；采纳/不采纳状态流转；生成待补充清单门控；字段边界展示。
- `/admin/compilation`：状态指标卡；新建/编辑抽屉；素材添加/移除；状态门控（draft/generated/archived）；入库前校验阻断；生成正文/入库状态流转。

### 13.3 构建验证

```bash
npm run build      # vue-tsc -b 类型检查 + vite 生产构建
npm run test:unit  # vitest 单元测试
```

`npm run build` 内含 `vue-tsc -b` 类型检查；类型检查、生产构建、单元测试均需通过（杜绝 archive-hu 式类型错误阻塞集成）。

## 14. 验收口径

- `/admin/overview`、`/admin/statistics`、`/admin/data-analysis`、`/admin/compilation` 无「待实现」占位。
- 页面布局、视觉层级、文案、交互结构、状态标签还原对应 HTML 原型；字段补充不改变原型组织方式。
- 概览：8 指标卡（带跳转+筛选意图）+ 待办提醒列表（可跳转）+ 最近操作日志 + 角色权限提示 + 存储告警语义 + 无危险操作。
- 统计：统计口径提示 + 时间筛选 + 4 指标卡 + 年度进馆/门类/载体分布（纯 CSS 图表）+ 业务汇总表 + 数据源状态 + 只读边界 + 导出反馈。
- 研判：扫描控制 + 任务状态 + 异常建议列表 + 条目详情（采纳/不采纳）+ AI JSON + 字段边界 + 待处理队列 + 生成待补充清单门控 + AI 降级。
- 编研：4 状态卡 + 成果列表 + 新建/编辑抽屉（含正文编辑）+ 素材引用 + 入库确认 + 状态流转 draft→generated→archived + 入库前校验。
- 跨页联动：概览/统计跳转带筛选；研判采纳→档案管理；编研素材→正式档案。
- 四页覆盖加载中、空数据、接口错误三态；敏感操作二次确认。
- API 函数全部支持 mock/真实切换；mock 字段对齐接口文档 + DB 设计；mock 联动一致。
- TypeScript 检查（含于 `npm run build` 的 vue-tsc）、生产构建、单元测试通过。
