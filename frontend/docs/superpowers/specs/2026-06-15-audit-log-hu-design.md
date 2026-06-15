# feat/audit-log-hu 设计文档：审计日志 + 档案访问日志

**日期**：2026-06-15
**负责人**：胡颖
**分支**：`feat/audit-log-hu`
**阶段**：06-17（补充计划 §3.3 前端任务第二行）
**优先级**：P2
**对应设计**：补充计划 §5.3、接口文档 §23.1/§23.2、数据库设计 §9.2（archive_access_logs）/§11.4（audit_logs）、业务场景 场景十四、模块说明 M15（L675-681）
**共同验收点**：06-18 页面冻结 —— 审计日志页、访问日志页可查询 / 筛选 / 导出，无删除入口，与已合并页面风格一致。
**特别说明**：日志审计**无原型 HTML**，按现有 admin 表格页（user-management / system-settings）风格设计。

## 1. 背景

本轮从最新 `develop`（`1c4f24a`）拉 `feat/audit-log-hu`。补充计划（§5.3）补做 M15 日志审计查询页：后端周扬 `feat/audit-log-zhou` 负责审计 / 访问日志查询 Controller，**后端尚未实现**，本轮沿用 mock 先行（与 feat/settings-hu 一致）。

前置状态（develop 已包含）：

- `AuditService`（只写）、`AuditLog` / `ArchiveAccessLog` 实体 + Mapper 后端已就绪，缺查询 Controller。
- 前端仅概览页摘要展示审计计数，**无独立查询 / 导出页、无路由**。
- §23 接口用**游标分页**（`cursor` / `limit` → `records` / `nextCursor` / `hasNext`），与项目现有 `PageData`（pageNo / pageSize）不同 —— 本轮引入 `CursorData<T>`。

教训（来自 settings-hu / archive-hu）：每阶段主动执行 `npm run type-check && npm run build`，避免类型错误阻塞 develop 集成。

> 用户已确认（brainstorming 澄清）：本轮两模块（fonds + audit-log）设计 + 计划一起写，依次实现、各自分支 / PR；页面统一验收后再 push / PR。本 spec 仅覆盖日志审计，全宗管理见 `2026-06-15-fonds-hu-design.md`。

## 2. 目标与范围

### 2.1 本轮实现

| 页面 | 路由 | 说明 | 角色 |
|------|------|------|------|
| 审计日志 | `/admin/audit-logs` | 新建：筛选栏 + 审计日志表 + 游标「加载更多」+ 导出；无删除 / 编辑入口 | sys_admin |
| 访问日志 | `/admin/access-logs` | 新建：筛选栏 + 访问日志表 + 游标「加载更多」+ 导出；无删除 / 编辑入口 | back_archivist / sys_admin |

### 2.2 不在本轮

- 日志后端查询 Controller（周扬 `feat/audit-log-zhou`）。
- 审计 / 访问日志的删除、修改（M15 L681 明确不可删改）。
- 系统管理员档案正文查看权限授予（§23.1 校验说明，非前端职责）。
- 接入真实后端（属联调阶段）。

## 3. 方案选择

### 3.1 文件组织（per-subdomain，新建子域）

| 方案 | 内容 | 优点 | 缺点 |
|------|------|------|------|
| 拆 `audit-log` / `access-log` 两子域（选定） | 各自 `types` + `api` + `mock` + `views` | 两套端点 / 两类记录语义独立，可独立测试；符合 per-subdomain 约定 | 文件数较多 |
| 合并为单 `log` 文件 | 共用一文件 | 文件少 | 审计 / 访问字段、筛选维度不同，语义混杂 |

**选定**：拆两子域。共享的游标响应结构 `CursorData<T>` 放 `types/api.d.ts`（D5）。

### 3.2 关键决策（用户确认按推荐）

| 编号 | 决策 | 选定 |
|------|------|------|
| D3 | 游标分页交互 | 「加载更多」按钮 + 游标累计（贴合 §23 cursor 语义，实现简单）；不模拟上/下页跳页 |
| D4 | 日志「导出」 | 客户端导出「当前已加载 + 筛选」记录为 CSV（§23 无导出端点；满足「查询与导出」） |
| D5 | `CursorData<T>` 类型位置 | 放 `types/api.d.ts`（共享游标响应结构） |

### 3.3 组件粒度

两页结构扁平（筛选栏 + 表格 + 加载更多 + 导出），单文件预计 300–420 行。**选定**：单 `index.vue` 为主；导出逻辑抽共享 `utils/csvExport.ts`（两页复用）。

## 4. 类型定义

`types/api.d.ts` 新增共享游标响应：

```ts
/** 游标分页响应（§23 审计 / 访问日志） */
export interface CursorData<T> {
  records: T[]
  nextCursor: string | null
  hasNext: boolean
}
/** 游标分页请求基类 */
export interface CursorParams {
  cursor?: string
  limit?: number
}
```

`types/audit-log.ts`（新）：

```ts
import type { CursorParams } from './api'

/** 审计日志记录（DB §11.4 + §23.1） */
export interface AuditLog {
  id: number
  actorUserId: number
  actorType: 'internal' | 'public' | 'system'
  actorName?: string          // 富字段：操作人姓名，便于展示
  moduleName: string
  operationType: string
  businessType?: string
  businessId?: number
  detail?: Record<string, unknown>   // JSONB
  ipAddress?: string
  operatedAt: string          // ISO 8601
}

/** 审计日志查询参数（§23.1） */
export interface AuditLogQuery extends CursorParams {
  actorUserId?: number
  actorType?: 'internal' | 'public' | 'system' | ''
  moduleName?: string
  operationType?: string
  businessType?: string
  businessId?: number
  startedAt?: string
  endedAt?: string
}
```

`types/access-log.ts`（新）：

```ts
import type { CursorParams } from './api'

/** 档案访问日志记录（DB §9.2 + §23.2） */
export interface ArchiveAccessLog {
  id: number
  userId?: number              // 未登录公众可空
  userType: 'internal' | 'public' | 'anonymous'
  userName?: string            // 富字段
  archiveId: number
  archiveNo?: string           // 富字段：档号，便于展示
  archiveFileId?: number
  accessType: 'view_metadata' | 'preview' | 'download'
  ipAddress?: string
  accessedAt: string
}

/** 访问日志查询参数（§23.2） */
export interface AccessLogQuery extends CursorParams {
  userId?: number
  archiveId?: number
  accessType?: 'view_metadata' | 'preview' | 'download' | ''
  startedAt?: string
  endedAt?: string
}
```

## 5. API 层（api/audit-log.ts、api/access-log.ts）

```ts
// api/audit-log.ts
export function getAuditLogs(query?: AuditLogQuery): Promise<CursorData<AuditLog>> { /* mock 或 GET /admin/audit-logs */ }

// api/access-log.ts
export function getAccessLogs(query?: AccessLogQuery): Promise<CursorData<ArchiveAccessLog>> { /* mock 或 GET /admin/archive-access-logs */ }
```

- `getAuditLogs` / `getAccessLogs` 返回 `CursorData<T>`，组件据 `nextCursor` 累计「加载更多」。
- **导出**（D4）：页面直接调 `utils/csvExport.ts` 的 `downloadCsv(filename, rows, columns)`，基于组件已加载的累计记录生成 CSV；不单独定义 api 导出函数（联调阶段若后端提供导出端点，再在 api 层补 `exportXxx`）。
- 后端就绪后置 `VITE_USE_MOCK=false` 切真请求；导出在联调阶段可与后端导出端点对齐（预留 `exportXxx` 内部切换）。

## 6. Mock（mock/modules/audit-log.ts、access-log.ts，新）

- 各建一组真实样例记录（8–12 条），覆盖 internal/public/system、view_metadata/preview/download 等枚举，时间分布跨多日。
- `mockAuditLogs(query)` / `mockAccessLogs(query)`：按筛选条件过滤 → 按 cursor（用记录 id 字符串）切片返回 `{ records, nextCursor, hasNext }`，模拟游标分页。
- 导出函数复用组件已加载的累计记录（与当前筛选条件一致），调 `utils/csvExport` 生成 CSV（与 D4 一致）。

## 7. 页面设计（按现有 admin 表格页风格，无原型）

两页统一结构：标题 + 副标题 → 工具栏（筛选条件 + 导出按钮）→ 表格区 → 底部「加载更多」（hasNext 时显示）。

### 7.1 审计日志页 `views/admin/audit-logs/index.vue`

- **筛选栏**：操作人（actorUserId / 文本）、操作人类型（select：internal/public/system）、模块（moduleName）、操作类型（operationType）、业务类型（businessType）、时间范围（el-date-picker startedAt/endedAt）；查询 / 重置。
- **表格列**：操作时间 / 操作人（actorName，无则 actorUserId）/ 类型 / 模块 / 操作类型 / 业务类型 / 业务 ID / IP / 详情（detail 折叠或 tooltip）。
- **加载更多**：`hasNext` 为真时显示，点击用 `nextCursor` 追加；loading / 失败重试 / 空态。
- **导出**：导出当前筛选全量为 CSV（D4）。

### 7.2 访问日志页 `views/admin/access-logs/index.vue`

- **筛选栏**：用户（userId / 文本）、用户类型、档案（archiveId / 文本）、访问类型（select：view_metadata/preview/download）、时间范围；查询 / 重置。
- **表格列**：访问时间 / 用户（userName，无则 userId）/ 类型 / 档案（archiveNo，无则 archiveId）/ 访问类型 / IP。
- **加载更多 / 导出 / 状态**：同 7.1。

两页均**无删除 / 编辑入口**（M15 L681 不可删改），通过不渲染操作列实现。

UI 状态：加载中、加载失败（重试）、空数据（暂无日志）、加载更多中、无更多。

## 8. 路由与菜单（router/routes/admin.ts）

`adminMenuConfig`「系统管理」组（sys_admin）追加两项：

```ts
{ path: '/admin/audit-logs', title: '审计日志', roles: ['sys_admin'] },
{ path: '/admin/access-logs', title: '访问日志', roles: ['sys_admin'] },
```

`adminRoutes.children` 追加两条懒加载路由：

```ts
{ path: 'audit-logs', component: () => import('@/views/admin/audit-logs/index.vue'), meta: { title: '审计日志' } },
{ path: 'access-logs', component: () => import('@/views/admin/access-logs/index.vue'), meta: { title: '访问日志' } },
```

> 访问日志角色含 back_archivist（§23.2）；菜单可见性以 sys_admin 为主（与 user-management/system-settings 同组），back_archivist 经路由直接访问不受菜单限制。若需 back_archivist 菜单可见，联调阶段再评估。

## 9. 校验与业务规则

| 规则 | 处理 |
|------|------|
| 日志不可删改 | 两页不渲染删除 / 编辑入口 |
| 游标分页 | 组件维护 `nextCursor` 与累计 records，「加载更多」带 cursor 请求；切换筛选重置累计 |
| 时间范围校验 | startedAt ≤ endedAt，否则 ElMessage 提示 |
| 导出范围 | 基于当前筛选条件，导出组件已加载的累计记录为 CSV（D4） |

共享工具：`utils/csvExport.ts`（`downloadCsv(filename: string, rows: Record<string, unknown>[], columns?: string[])`，转义逗号 / 换行 / 引号，触发浏览器下载）。

## 10. 测试计划（`.spec.ts`）

| 文件 | 覆盖 |
|------|------|
| `api/audit-log.spec.ts`（新） | getAuditLogs 游标返回结构、筛选过滤、hasNext 边界 |
| `api/access-log.spec.ts`（新） | getAccessLogs 游标返回结构、筛选过滤 |
| `utils/csvExport.spec.ts`（新） | 字段转义（逗号 / 引号 / 换行）、表头生成、空数据 |
| `views/admin/audit-logs/index.spec.ts`（新） | 列表渲染、筛选重置累计、加载更多追加、导出触发、无删除入口 |
| `views/admin/access-logs/index.spec.ts`（新） | 同上 |

## 11. 风险与降级

| 风险 | 应对 |
|------|------|
| 游标分页与现有 PageData 习惯不同 | D3「加载更多」最简实现；不引入跳页，降低复杂度 |
| 导出无后端端点 | D4 客户端导出筛选全量；联调阶段如后端提供导出端点，仅改 `exportXxx` 内部 |
| 本模块 P2、开发窗口紧 | 补充计划 §6 允许降级为「仅后端端点，页面延后」；本轮优先保证两页可查询 + 导出 |
| 时间/资源不足 | 两页结构高度相似，复用 `csvExport` + 相近模板，缩短实现 |
| 类型/构建错误阻塞 develop | 每阶段 `npm run type-check && npm run build` |


