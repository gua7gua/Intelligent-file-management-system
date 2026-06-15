# 日志审计（feat/audit-log-hu）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新建审计日志页与档案访问日志页（筛选 + 游标加载更多 + 客户端 CSV 导出，无删改入口），mock 先行、接口字段对齐接口文档 §23，配齐单测。

**Architecture:** 引入 `CursorData<T>` 游标分页；两页各自 per-subdomain 文件（types/api/mock）；页面单 `index.vue`，按现有 admin 表格页风格；导出走共享 `utils/csvExport`。沿用 `USE_MOCK` 切换、`@vue/test-utils` + memory router 测试。

**Tech Stack:** Vue 3 `<script setup>` + TypeScript + Element Plus + vitest + jsdom。

**对应设计：** `frontend/docs/superpowers/specs/2026-06-15-audit-log-hu-design.md`

**分支：** 从最新 `develop`（`1c4f24a`）拉 `feat/audit-log-hu`，全部提交在此分支，验收前不 push。

---

## 文件结构

| 文件 | 责任 | 动作 |
|------|------|------|
| `src/types/api.d.ts` | 新增 `CursorData<T>` / `CursorParams` | 改 |
| `src/utils/csvExport.ts` | `toCsv` / `downloadCsv` | 新建 |
| `src/utils/csvExport.spec.ts` | CSV 转义测试 | 新建 |
| `src/types/audit-log.ts` | `AuditLog` / `AuditLogQuery` | 新建 |
| `src/api/audit-log.ts` | `getAuditLogs` | 新建 |
| `src/api/audit-log.spec.ts` | 游标分页/筛选测试 | 新建 |
| `src/mock/modules/audit-log.ts` | `mockAuditLogs` | 新建 |
| `src/types/access-log.ts` | `ArchiveAccessLog` / `AccessLogQuery` | 新建 |
| `src/api/access-log.ts` | `getAccessLogs` | 新建 |
| `src/api/access-log.spec.ts` | 游标分页/筛选测试 | 新建 |
| `src/mock/modules/access-log.ts` | `mockAccessLogs` | 新建 |
| `src/views/admin/audit-logs/index.vue` | 审计日志页 | 新建 |
| `src/views/admin/audit-logs/index.spec.ts` | 页面测试 | 新建 |
| `src/views/admin/access-logs/index.vue` | 访问日志页 | 新建 |
| `src/views/admin/access-logs/index.spec.ts` | 页面测试 | 新建 |
| `src/router/routes/admin.ts` | 注册 2 路由 + 2 菜单 | 改 |

**验证命令：** 单测 `npm run test:unit -- <file>`（vitest）；类型 + 构建 `npm run build`。

**测试约定（重要）：** element-plus 仅在 `main.ts` 全局注册，测试环境不自动注入——未注入时 `<el-button>` 不渲染为原生 `<button>`。故两页测试 `mountIt()` 必须显式注入：`mount(Comp, { global: { plugins: [router, ElementPlus] } })`，spec 顶部 `import ElementPlus from 'element-plus'`。

---

## Task 1：共享游标类型 + CSV 导出工具

**Files:**
- Modify: `src/types/api.d.ts`
- Create: `src/utils/csvExport.ts`
- Test: `src/utils/csvExport.spec.ts`

- [ ] **Step 1：写失败测试（`src/utils/csvExport.spec.ts`）**

```ts
import { describe, expect, it } from 'vitest'
import { toCsv } from './csvExport'

describe('toCsv', () => {
  it('builds header and rows from object keys', () => {
    expect(toCsv([{ a: 1, b: 2 }])).toBe('a,b\n1,2')
  })

  it('escapes commas, quotes and newlines', () => {
    const csv = toCsv([{ a: 'x,y', b: 'he said "hi"', c: 'line1\nline2' }], ['a', 'b', 'c'])
    expect(csv).toContain('"x,y"')
    expect(csv).toContain('"he said ""hi"""')
    expect(csv).toContain('"line1\nline2"')
  })

  it('stringifies objects as JSON', () => {
    const csv = toCsv([{ d: { k: 1 } }], ['d'])
    expect(csv).toContain('{"k":1}')
  })

  it('returns empty string for no rows', () => {
    expect(toCsv([])).toBe('')
  })
})
```

- [ ] **Step 2：运行确认失败**

Run: `npm run test:unit -- src/utils/csvExport.spec.ts`
Expected: FAIL（模块不存在）。

- [ ] **Step 3：实现类型（在 `src/types/api.d.ts` 末尾追加）**

```ts

/** 游标分页响应（§23 审计 / 访问日志） */
export interface CursorData<T = any> {
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

- [ ] **Step 4：实现 CSV 工具（`src/utils/csvExport.ts`）**

```ts
function escapeCell(v: unknown): string {
  if (v === null || v === undefined) return ''
  const s = typeof v === 'object' ? JSON.stringify(v) : String(v)
  if (/[",\n\r]/.test(s)) return '"' + s.replace(/"/g, '""') + '"'
  return s
}

/** 将记录数组转为 CSV 字符串（首行表头取自 columns 或首条记录的键） */
export function toCsv(rows: Record<string, unknown>[], columns?: string[]): string {
  if (rows.length === 0) return ''
  const cols = columns ?? Object.keys(rows[0])
  const header = cols.map(escapeCell).join(',')
  const body = rows.map((r) => cols.map((c) => escapeCell(r[c])).join(','))
  return [header, ...body].join('\n')
}

/** 触发浏览器下载 CSV（含 BOM，Excel 中文不乱码） */
export function downloadCsv(filename: string, rows: Record<string, unknown>[], columns?: string[]): void {
  const csv = toCsv(rows, columns)
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
```

- [ ] **Step 5：运行确认通过**

Run: `npm run test:unit -- src/utils/csvExport.spec.ts`
Expected: PASS（4 条）。

- [ ] **Step 6：提交**

```bash
git add src/types/api.d.ts src/utils/csvExport.ts src/utils/csvExport.spec.ts
git commit -m "feat(audit-log-hu): 游标分页类型与 CSV 导出工具"
```

## Task 2：审计日志 types + api + mock

**Files:**
- Create: `src/types/audit-log.ts`、`src/api/audit-log.ts`、`src/api/audit-log.spec.ts`、`src/mock/modules/audit-log.ts`

- [ ] **Step 1：写失败测试（`src/api/audit-log.spec.ts`）**

```ts
import { describe, expect, it } from 'vitest'
import { getAuditLogs } from './audit-log'

describe('audit-log api mock mode', () => {
  it('returns cursor-paginated audit logs', async () => {
    const res = await getAuditLogs({ limit: 5 })
    expect(res.records.length).toBeLessThanOrEqual(5)
    expect(res.records[0]).toHaveProperty('operationType')
    expect(res).toHaveProperty('nextCursor')
    expect(res).toHaveProperty('hasNext')
  })

  it('filters by actorType', async () => {
    const res = await getAuditLogs({ actorType: 'system' })
    expect(res.records.every((l) => l.actorType === 'system')).toBe(true)
  })

  it('loads next page via cursor without overlap', async () => {
    const first = await getAuditLogs({ limit: 5 })
    expect(first.hasNext).toBe(true)
    const next = await getAuditLogs({ limit: 5, cursor: first.nextCursor! })
    expect(next.records.length).toBeGreaterThan(0)
    const ids = new Set(first.records.map((r) => r.id))
    expect(next.records.every((r) => !ids.has(r.id))).toBe(true)
  })
})
```

- [ ] **Step 2：运行确认失败**

Run: `npm run test:unit -- src/api/audit-log.spec.ts`
Expected: FAIL（模块不存在）。

- [ ] **Step 3：实现类型（`src/types/audit-log.ts`）**

```ts
import type { CursorParams } from './api'

/** 操作人类型 */
export type ActorType = 'internal' | 'public' | 'system'

/** 审计日志记录（DB §11.4 + §23.1） */
export interface AuditLog {
  id: number
  actorUserId: number
  actorType: ActorType
  /** 富字段：操作人姓名，便于展示 */
  actorName?: string
  moduleName: string
  operationType: string
  businessType?: string
  businessId?: number
  /** JSONB 详情 */
  detail?: Record<string, unknown>
  ipAddress?: string
  operatedAt: string
}

/** 审计日志查询参数（§23.1） */
export interface AuditLogQuery extends CursorParams {
  actorUserId?: number
  actorType?: ActorType | ''
  moduleName?: string
  operationType?: string
  businessType?: string
  businessId?: number
  startedAt?: string
  endedAt?: string
}
```

- [ ] **Step 4：实现 Mock（`src/mock/modules/audit-log.ts`）**

```ts
import type { AuditLog, AuditLogQuery } from '@/types/audit-log'
import type { CursorData } from '@/types/api'

const auditLogs: AuditLog[] = [
  { id: 1, actorUserId: 1, actorType: 'internal', actorName: '小刘', moduleName: '用户管理', operationType: '禁用账号', businessType: 'user', businessId: 5, detail: { reason: '离职' }, ipAddress: '10.0.0.12', operatedAt: '2026-06-14T09:30:00+08:00' },
  { id: 2, actorUserId: 1, actorType: 'internal', actorName: '小刘', moduleName: '档案管理', operationType: '元数据编辑', businessType: 'archive', businessId: 101, detail: { field: 'title' }, ipAddress: '10.0.0.12', operatedAt: '2026-06-14T10:05:00+08:00' },
  { id: 3, actorUserId: 0, actorType: 'system', moduleName: '系统', operationType: '自动备份', businessType: 'backup', businessId: 7, detail: { scope: 'db' }, ipAddress: '127.0.0.1', operatedAt: '2026-06-14T02:00:00+08:00' },
  { id: 4, actorUserId: 3, actorType: 'internal', actorName: '小张', moduleName: '移交验收', operationType: '接收清单', businessType: 'intake_batch', businessId: 12, detail: {}, ipAddress: '10.0.0.20', operatedAt: '2026-06-13T16:40:00+08:00' },
  { id: 5, actorUserId: 4, actorType: 'public', actorName: '公众用户A', moduleName: '公众查询', operationType: '检索公开档案', businessType: 'archive', businessId: 88, detail: {}, ipAddress: '202.100.1.5', operatedAt: '2026-06-13T11:20:00+08:00' },
  { id: 6, actorUserId: 1, actorType: 'internal', actorName: '小刘', moduleName: '档案鉴定', operationType: '鉴定确认', businessType: 'appraisal', businessId: 3, detail: { result: '销毁' }, ipAddress: '10.0.0.12', operatedAt: '2026-06-12T15:00:00+08:00' },
  { id: 7, actorUserId: 0, actorType: 'system', moduleName: '系统', operationType: '四性检测', businessType: 'file_check', businessId: 9, detail: { result: 'safe' }, ipAddress: '127.0.0.1', operatedAt: '2026-06-12T03:00:00+08:00' },
  { id: 8, actorUserId: 2, actorType: 'internal', actorName: '小李', moduleName: '借阅审批', operationType: '审批通过', businessType: 'borrow_request', businessId: 21, detail: {}, ipAddress: '10.0.0.30', operatedAt: '2026-06-11T14:10:00+08:00' },
  { id: 9, actorUserId: 4, actorType: 'public', actorName: '公众用户B', moduleName: '公众查询', operationType: '下载公开档案', businessType: 'archive', businessId: 90, detail: {}, ipAddress: '202.100.1.9', operatedAt: '2026-06-11T09:45:00+08:00' },
  { id: 10, actorUserId: 1, actorType: 'internal', actorName: '小刘', moduleName: '系统配置', operationType: '更新配置', businessType: 'system_config', businessId: 2, detail: { key: 'ai.enabled' }, ipAddress: '10.0.0.12', operatedAt: '2026-06-10T17:30:00+08:00' },
  { id: 11, actorUserId: 3, actorType: 'internal', actorName: '小张', moduleName: '档案销毁', operationType: '确认销毁', businessType: 'destruction_list', businessId: 5, detail: {}, ipAddress: '10.0.0.20', operatedAt: '2026-06-10T10:00:00+08:00' },
  { id: 12, actorUserId: 0, actorType: 'system', moduleName: '系统', operationType: '登录失败', businessType: 'user', businessId: 9, detail: { reason: '密码错误' }, ipAddress: '203.0.113.7', operatedAt: '2026-06-09T22:15:00+08:00' },
]

export function mockAuditLogs(query?: AuditLogQuery): CursorData<AuditLog> {
  let list = auditLogs
  if (query?.actorType) list = list.filter((l) => l.actorType === query.actorType)
  if (query?.moduleName) list = list.filter((l) => l.moduleName.includes(query.moduleName!.trim()))
  if (query?.operationType) list = list.filter((l) => l.operationType.includes(query.operationType!.trim()))
  if (query?.businessType) list = list.filter((l) => l.businessType === query.businessType)
  if (query?.actorUserId) list = list.filter((l) => l.actorUserId === query.actorUserId)
  if (query?.startedAt) list = list.filter((l) => l.operatedAt >= query.startedAt!)
  if (query?.endedAt) list = list.filter((l) => l.operatedAt <= query.endedAt!)

  const limit = query?.limit ?? 10
  let startIndex = 0
  if (query?.cursor) {
    const idx = list.findIndex((l) => String(l.id) === query.cursor)
    startIndex = idx === -1 ? 0 : idx + 1
  }
  const slice = list.slice(startIndex, startIndex + limit)
  const last = slice[slice.length - 1]
  const hasNext = startIndex + limit < list.length
  return { records: slice.map((l) => ({ ...l })), nextCursor: hasNext && last ? String(last.id) : null, hasNext }
}
```

- [ ] **Step 5：实现 API（`src/api/audit-log.ts`）**

```ts
import request from './request'
import type { CursorData } from '@/types/api'
import type { AuditLog, AuditLogQuery } from '@/types/audit-log'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询审计日志（§23.1，游标分页） */
export function getAuditLogs(query?: AuditLogQuery): Promise<CursorData<AuditLog>> {
  if (USE_MOCK) {
    return import('@/mock/modules/audit-log').then((m) => m.mockAuditLogs(query))
  }
  return request.get('/admin/audit-logs', { params: query })
}
```

- [ ] **Step 6：运行确认通过**

Run: `npm run test:unit -- src/api/audit-log.spec.ts`
Expected: PASS（3 条）。

- [ ] **Step 7：提交**

```bash
git add src/types/audit-log.ts src/api/audit-log.ts src/api/audit-log.spec.ts src/mock/modules/audit-log.ts
git commit -m "feat(audit-log-hu): 审计日志 api/mock/类型与单测"
```

---

## Task 3：访问日志 types + api + mock

**Files:**
- Create: `src/types/access-log.ts`、`src/api/access-log.ts`、`src/api/access-log.spec.ts`、`src/mock/modules/access-log.ts`

- [ ] **Step 1：写失败测试（`src/api/access-log.spec.ts`）**

```ts
import { describe, expect, it } from 'vitest'
import { getAccessLogs } from './access-log'

describe('access-log api mock mode', () => {
  it('returns cursor-paginated access logs', async () => {
    const res = await getAccessLogs({ limit: 5 })
    expect(res.records.length).toBeLessThanOrEqual(5)
    expect(res.records[0]).toHaveProperty('accessType')
    expect(res).toHaveProperty('hasNext')
  })

  it('filters by accessType', async () => {
    const res = await getAccessLogs({ accessType: 'download' })
    expect(res.records.every((l) => l.accessType === 'download')).toBe(true)
  })

  it('loads next page via cursor without overlap', async () => {
    const first = await getAccessLogs({ limit: 5 })
    expect(first.hasNext).toBe(true)
    const next = await getAccessLogs({ limit: 5, cursor: first.nextCursor! })
    expect(next.records.length).toBeGreaterThan(0)
    const ids = new Set(first.records.map((r) => r.id))
    expect(next.records.every((r) => !ids.has(r.id))).toBe(true)
  })
})
```

- [ ] **Step 2：运行确认失败**

Run: `npm run test:unit -- src/api/access-log.spec.ts`
Expected: FAIL（模块不存在）。

- [ ] **Step 3：实现类型（`src/types/access-log.ts`）**

```ts
import type { CursorParams } from './api'

/** 访问者类型 */
export type AccessUserType = 'internal' | 'public' | 'anonymous'

/** 访问类型 */
export type AccessType = 'view_metadata' | 'preview' | 'download'

/** 档案访问日志记录（DB §9.2 + §23.2） */
export interface ArchiveAccessLog {
  id: number
  userId?: number
  userType: AccessUserType
  /** 富字段：用户姓名 */
  userName?: string
  archiveId: number
  /** 富字段：档号，便于展示 */
  archiveNo?: string
  archiveFileId?: number
  accessType: AccessType
  ipAddress?: string
  accessedAt: string
}

/** 访问日志查询参数（§23.2） */
export interface AccessLogQuery extends CursorParams {
  userId?: number
  archiveId?: number
  accessType?: AccessType | ''
  startedAt?: string
  endedAt?: string
}
```

- [ ] **Step 4：实现 Mock（`src/mock/modules/access-log.ts`）**

```ts
import type { AccessLogQuery, ArchiveAccessLog } from '@/types/access-log'
import type { CursorData } from '@/types/api'

const accessLogs: ArchiveAccessLog[] = [
  { id: 1, userId: 2, userType: 'internal', userName: '小李', archiveId: 101, archiveNo: 'ARC-001', accessType: 'view_metadata', ipAddress: '10.0.0.30', accessedAt: '2026-06-14T10:00:00+08:00' },
  { id: 2, userId: 2, userType: 'internal', userName: '小李', archiveId: 101, archiveNo: 'ARC-001', archiveFileId: 201, accessType: 'download', ipAddress: '10.0.0.30', accessedAt: '2026-06-14T10:02:00+08:00' },
  { id: 3, userId: undefined, userType: 'anonymous', archiveId: 88, archiveNo: 'ARC-088', accessType: 'view_metadata', ipAddress: '202.100.1.5', accessedAt: '2026-06-13T11:20:00+08:00' },
  { id: 4, userId: 4, userType: 'public', userName: '公众用户A', archiveId: 88, archiveNo: 'ARC-088', accessType: 'download', ipAddress: '202.100.1.5', accessedAt: '2026-06-13T11:25:00+08:00' },
  { id: 5, userId: 3, userType: 'internal', userName: '小张', archiveId: 105, archiveNo: 'ARC-005', accessType: 'preview', ipAddress: '10.0.0.20', accessedAt: '2026-06-13T09:10:00+08:00' },
  { id: 6, userId: 2, userType: 'internal', userName: '小李', archiveId: 110, archiveNo: 'ARC-010', accessType: 'view_metadata', ipAddress: '10.0.0.30', accessedAt: '2026-06-12T16:30:00+08:00' },
  { id: 7, userId: undefined, userType: 'anonymous', archiveId: 90, archiveNo: 'ARC-090', accessType: 'view_metadata', ipAddress: '202.100.1.9', accessedAt: '2026-06-12T14:00:00+08:00' },
  { id: 8, userId: 4, userType: 'public', userName: '公众用户B', archiveId: 90, archiveNo: 'ARC-090', accessType: 'download', ipAddress: '202.100.1.9', accessedAt: '2026-06-11T09:45:00+08:00' },
  { id: 9, userId: 3, userType: 'internal', userName: '小张', archiveId: 105, archiveNo: 'ARC-005', accessType: 'download', ipAddress: '10.0.0.20', accessedAt: '2026-06-11T08:20:00+08:00' },
  { id: 10, userId: 1, userType: 'internal', userName: '小刘', archiveId: 120, archiveNo: 'ARC-020', accessType: 'view_metadata', ipAddress: '10.0.0.12', accessedAt: '2026-06-10T17:00:00+08:00' },
  { id: 11, userId: undefined, userType: 'anonymous', archiveId: 88, archiveNo: 'ARC-088', accessType: 'preview', ipAddress: '203.0.113.2', accessedAt: '2026-06-10T12:00:00+08:00' },
  { id: 12, userId: 2, userType: 'internal', userName: '小李', archiveId: 110, archiveNo: 'ARC-010', accessType: 'preview', ipAddress: '10.0.0.30', accessedAt: '2026-06-09T15:40:00+08:00' },
]

export function mockAccessLogs(query?: AccessLogQuery): CursorData<ArchiveAccessLog> {
  let list = accessLogs
  if (query?.accessType) list = list.filter((l) => l.accessType === query.accessType)
  if (query?.userId) list = list.filter((l) => l.userId === query.userId)
  if (query?.archiveId) list = list.filter((l) => l.archiveId === query.archiveId)
  if (query?.startedAt) list = list.filter((l) => l.accessedAt >= query.startedAt!)
  if (query?.endedAt) list = list.filter((l) => l.accessedAt <= query.endedAt!)

  const limit = query?.limit ?? 10
  let startIndex = 0
  if (query?.cursor) {
    const idx = list.findIndex((l) => String(l.id) === query.cursor)
    startIndex = idx === -1 ? 0 : idx + 1
  }
  const slice = list.slice(startIndex, startIndex + limit)
  const last = slice[slice.length - 1]
  const hasNext = startIndex + limit < list.length
  return { records: slice.map((l) => ({ ...l })), nextCursor: hasNext && last ? String(last.id) : null, hasNext }
}
```

- [ ] **Step 5：实现 API（`src/api/access-log.ts`）**

```ts
import request from './request'
import type { CursorData } from '@/types/api'
import type { AccessLogQuery, ArchiveAccessLog } from '@/types/access-log'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询档案访问日志（§23.2，游标分页） */
export function getAccessLogs(query?: AccessLogQuery): Promise<CursorData<ArchiveAccessLog>> {
  if (USE_MOCK) {
    return import('@/mock/modules/access-log').then((m) => m.mockAccessLogs(query))
  }
  return request.get('/admin/archive-access-logs', { params: query })
}
```

- [ ] **Step 6：运行确认通过**

Run: `npm run test:unit -- src/api/access-log.spec.ts`
Expected: PASS（3 条）。

- [ ] **Step 7：提交**

```bash
git add src/types/access-log.ts src/api/access-log.ts src/api/access-log.spec.ts src/mock/modules/access-log.ts
git commit -m "feat(audit-log-hu): 访问日志 api/mock/类型与单测"
```

## Task 4：审计日志页

**Files:**
- Create: `src/views/admin/audit-logs/index.vue`、`src/views/admin/audit-logs/index.spec.ts`

- [ ] **Step 1：写失败测试（`src/views/admin/audit-logs/index.spec.ts`）**

```ts
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import AuditLogs from './index.vue'

async function wait() { await new Promise((r) => setTimeout(r, 80)); await flushPromises() }
async function mountIt() {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/admin/audit-logs', component: AuditLogs }] })
  await router.push('/admin/audit-logs')
  await router.isReady()
  return mount(AuditLogs, { global: { plugins: [router] } })
}

describe('AuditLogs', () => {
  it('renders title and audit records', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('审计日志')
    expect(w.text()).toContain('禁用账号')
    expect(w.text()).toContain('自动备份')
  })

  it('shows 导出 button and no 删除/编辑 entry', async () => {
    const w = await mountIt()
    await wait()
    expect(w.findAll('button').some((b) => b.text().includes('导出'))).toBe(true)
    expect(w.findAll('button').some((b) => b.text().includes('删除') || b.text().includes('编辑'))).toBe(false)
  })

  it('appends more records on 加载更多', async () => {
    const w = await mountIt()
    await wait()
    const before = w.findAll('tbody tr').length
    const more = w.findAll('button').find((b) => b.text().includes('加载更多'))
    expect(more).toBeTruthy()
    await more!.trigger('click')
    await wait()
    const after = w.findAll('tbody tr').length
    expect(after).toBeGreaterThan(before)
  })

  it('filters by actorType system', async () => {
    const w = await mountIt()
    await wait()
    await w.find('select[data-testid="actorType"]').findAll('option')[1].setSelected() // system
    await w.findAll('button').find((b) => b.text().includes('查询'))!.trigger('click')
    await wait()
    expect(w.text()).toContain('自动备份')
    expect(w.findAll('tbody tr').length).toBeGreaterThan(0)
  })
})
```

- [ ] **Step 2：运行确认失败**

Run: `npm run test:unit -- src/views/admin/audit-logs/index.spec.ts`
Expected: FAIL（页面不存在）。

- [ ] **Step 3：实现页面（`src/views/admin/audit-logs/index.vue`）**

````vue
<template>
  <div class="audit-logs">
    <section>
      <h1 class="page-title">审计日志</h1>
      <p class="page-subtitle">查询系统操作审计记录，支持按操作人、模块、类型与时间筛选并导出。日志不可删除或修改。</p>
    </section>

    <div class="card panel">
      <h2 class="section-title">筛选条件</h2>
      <div class="form-grid">
        <div class="field">
          <label>操作人类型</label>
          <select v-model="filters.actorType" data-testid="actorType">
            <option value="">全部</option>
            <option value="internal">internal</option>
            <option value="public">public</option>
            <option value="system">system</option>
          </select>
        </div>
        <div class="field"><label>模块</label><input v-model="filters.moduleName" placeholder="如 用户管理" /></div>
        <div class="field"><label>操作类型</label><input v-model="filters.operationType" placeholder="如 禁用账号" /></div>
        <div class="field"><label>业务类型</label><input v-model="filters.businessType" placeholder="如 user" /></div>
        <div class="field"><label>开始时间</label><input type="date" v-model="filters.startedAt" /></div>
        <div class="field"><label>结束时间</label><input type="date" v-model="filters.endedAt" /></div>
      </div>
      <div class="actions" style="margin-top:12px">
        <el-button type="primary" @click="load(true)">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button :disabled="records.length === 0" @click="exportLogs">导出</el-button>
      </div>
    </div>

    <div class="card panel">
      <h2 class="section-title">审计记录</h2>
      <div v-if="loading && records.length === 0" class="detail-empty">加载中...</div>
      <div v-else-if="loadError" class="detail-empty">加载失败：<button class="link" @click="load(true)">重试</button></div>
      <div v-else-if="records.length === 0" class="detail-empty">暂无审计日志</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr><th>操作时间</th><th>操作人</th><th>类型</th><th>模块</th><th>操作类型</th><th>业务类型</th><th>业务 ID</th><th>IP</th><th>详情</th></tr>
          </thead>
          <tbody>
            <tr v-for="l in records" :key="l.id">
              <td class="mono">{{ l.operatedAt }}</td>
              <td>{{ l.actorName || ('用户#' + l.actorUserId) }}</td>
              <td>{{ l.actorType }}</td>
              <td>{{ l.moduleName }}</td>
              <td>{{ l.operationType }}</td>
              <td>{{ l.businessType || '-' }}</td>
              <td>{{ l.businessId ?? '-' }}</td>
              <td class="mono">{{ l.ipAddress || '-' }}</td>
              <td>{{ l.detail ? JSON.stringify(l.detail) : '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="hasNext" class="more-wrap">
        <el-button :loading="loadingMore" @click="loadMore">加载更多</el-button>
      </div>
      <div v-else-if="records.length > 0" class="detail-empty">已加载全部</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuditLogs } from '@/api/audit-log'
import { downloadCsv } from '@/utils/csvExport'
import type { AuditLog, AuditLogQuery, ActorType } from '@/types/audit-log'

const records = ref<AuditLog[]>([])
const nextCursor = ref<string | null>(null)
const hasNext = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const loadError = ref(false)

const filters = reactive({
  actorType: '' as ActorType | '',
  moduleName: '',
  operationType: '',
  businessType: '',
  startedAt: '',
  endedAt: '',
})

function buildQuery(cursor?: string): AuditLogQuery {
  return {
    actorType: filters.actorType || undefined,
    moduleName: filters.moduleName.trim() || undefined,
    operationType: filters.operationType.trim() || undefined,
    businessType: filters.businessType.trim() || undefined,
    startedAt: filters.startedAt ? filters.startedAt + 'T00:00:00+08:00' : undefined,
    endedAt: filters.endedAt ? filters.endedAt + 'T23:59:59+08:00' : undefined,
    cursor,
    limit: 5,
  }
}

async function load(reset = false) {
  if (filters.startedAt && filters.endedAt && filters.startedAt > filters.endedAt) {
    ElMessage.error('开始时间不能晚于结束时间')
    return
  }
  loading.value = true
  loadError.value = false
  try {
    const res = await getAuditLogs(buildQuery())
    records.value = reset ? res.records : res.records
    nextCursor.value = res.nextCursor
    hasNext.value = res.hasNext
  } catch {
    loadError.value = true
    ElMessage.error('审计日志加载失败')
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (!nextCursor.value) return
  loadingMore.value = true
  try {
    const res = await getAuditLogs(buildQuery(nextCursor.value))
    records.value = [...records.value, ...res.records]
    nextCursor.value = res.nextCursor
    hasNext.value = res.hasNext
  } catch {
    ElMessage.error('加载更多失败')
  } finally {
    loadingMore.value = false
  }
}

function reset() {
  filters.actorType = ''
  filters.moduleName = ''
  filters.operationType = ''
  filters.businessType = ''
  filters.startedAt = ''
  filters.endedAt = ''
  load(true)
}

function exportLogs() {
  const rows = records.value.map((l) => ({
    operatedAt: l.operatedAt,
    actor: l.actorName || ('用户#' + l.actorUserId),
    actorType: l.actorType,
    moduleName: l.moduleName,
    operationType: l.operationType,
    businessType: l.businessType ?? '',
    businessId: l.businessId ?? '',
    ipAddress: l.ipAddress ?? '',
    detail: l.detail ? JSON.stringify(l.detail) : '',
  }))
  downloadCsv('审计日志.csv', rows)
  ElMessage.success('已导出当前已加载的审计日志。')
}

onMounted(() => load(true))
</script>

<style scoped>
.audit-logs { padding: 0; }
.page-title { font-size: 20px; font-weight: 700; }
.page-subtitle { color: #909399; margin-top: 6px; }
.card.panel { margin-top: 16px; }
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 10px; }
.form-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.field { display: grid; gap: 4px; }
.field label { font-size: 12px; color: #909399; }
.field input, .field select { height: 34px; padding: 0 8px; border: 1px solid var(--border, #e4e7ed); border-radius: 6px; }
.actions { display: flex; gap: 8px; align-items: center; }
.table-wrap { overflow-x: auto; }
.more-wrap { text-align: center; padding: 12px 0; }
.detail-empty { color: #909399; padding: 16px; text-align: center; }
.link { background: none; border: none; color: var(--primary, #1f6f78); cursor: pointer; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; }
@media (max-width: 900px) { .form-grid { grid-template-columns: 1fr 1fr; } }
</style>
````

- [ ] **Step 4：运行确认通过**

Run: `npm run test:unit -- src/views/admin/audit-logs/index.spec.ts`
Expected: PASS（4 条）。

- [ ] **Step 5：类型 + 构建**

Run: `npm run build`
Expected: 无类型错误，构建成功。

- [ ] **Step 6：提交**

```bash
git add src/views/admin/audit-logs/index.vue src/views/admin/audit-logs/index.spec.ts
git commit -m "feat(audit-log-hu): 审计日志查询页与单测"
```

## Task 5：访问日志页

**Files:**
- Create: `src/views/admin/access-logs/index.vue`、`src/views/admin/access-logs/index.spec.ts`

- [ ] **Step 1：写失败测试（`src/views/admin/access-logs/index.spec.ts`）**

```ts
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import AccessLogs from './index.vue'

async function wait() { await new Promise((r) => setTimeout(r, 80)); await flushPromises() }
async function mountIt() {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/admin/access-logs', component: AccessLogs }] })
  await router.push('/admin/access-logs')
  await router.isReady()
  return mount(AccessLogs, { global: { plugins: [router] } })
}

describe('AccessLogs', () => {
  it('renders title and access records', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('访问日志')
    expect(w.text()).toContain('view_metadata')
    expect(w.text()).toContain('download')
  })

  it('shows 导出 button and no 删除/编辑 entry', async () => {
    const w = await mountIt()
    await wait()
    expect(w.findAll('button').some((b) => b.text().includes('导出'))).toBe(true)
    expect(w.findAll('button').some((b) => b.text().includes('删除') || b.text().includes('编辑'))).toBe(false)
  })

  it('appends more records on 加载更多', async () => {
    const w = await mountIt()
    await wait()
    const before = w.findAll('tbody tr').length
    const more = w.findAll('button').find((b) => b.text().includes('加载更多'))
    expect(more).toBeTruthy()
    await more!.trigger('click')
    await wait()
    expect(w.findAll('tbody tr').length).toBeGreaterThan(before)
  })

  it('filters by accessType download', async () => {
    const w = await mountIt()
    await wait()
    await w.find('select[data-testid="accessType"]').findAll('option')[3].setSelected() // download
    await w.findAll('button').find((b) => b.text().includes('查询'))!.trigger('click')
    await wait()
    expect(w.findAll('tbody tr').length).toBeGreaterThan(0)
    expect(w.text()).toContain('download')
  })
})
```

- [ ] **Step 2：运行确认失败**

Run: `npm run test:unit -- src/views/admin/access-logs/index.spec.ts`
Expected: FAIL（页面不存在）。

- [ ] **Step 3：实现页面（`src/views/admin/access-logs/index.vue`）**

````vue
<template>
  <div class="access-logs">
    <section>
      <h1 class="page-title">访问日志</h1>
      <p class="page-subtitle">查询档案查阅、预览、下载记录，支持按用户、档案、访问类型与时间筛选并导出。日志不可删除或修改。</p>
    </section>

    <div class="card panel">
      <h2 class="section-title">筛选条件</h2>
      <div class="form-grid">
        <div class="field">
          <label>访问类型</label>
          <select v-model="filters.accessType" data-testid="accessType">
            <option value="">全部</option>
            <option value="view_metadata">view_metadata</option>
            <option value="preview">preview</option>
            <option value="download">download</option>
          </select>
        </div>
        <div class="field"><label>用户 ID</label><input v-model.number="filters.userId" type="number" placeholder="留空查全部" /></div>
        <div class="field"><label>档案 ID</label><input v-model.number="filters.archiveId" type="number" placeholder="留空查全部" /></div>
        <div class="field"><label>开始时间</label><input type="date" v-model="filters.startedAt" /></div>
        <div class="field"><label>结束时间</label><input type="date" v-model="filters.endedAt" /></div>
      </div>
      <div class="actions" style="margin-top:12px">
        <el-button type="primary" @click="load(true)">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button :disabled="records.length === 0" @click="exportLogs">导出</el-button>
      </div>
    </div>

    <div class="card panel">
      <h2 class="section-title">访问记录</h2>
      <div v-if="loading && records.length === 0" class="detail-empty">加载中...</div>
      <div v-else-if="loadError" class="detail-empty">加载失败：<button class="link" @click="load(true)">重试</button></div>
      <div v-else-if="records.length === 0" class="detail-empty">暂无访问日志</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr><th>访问时间</th><th>用户</th><th>类型</th><th>档案</th><th>访问类型</th><th>IP</th></tr>
          </thead>
          <tbody>
            <tr v-for="l in records" :key="l.id">
              <td class="mono">{{ l.accessedAt }}</td>
              <td>{{ l.userName || (l.userId ? '用户#' + l.userId : '匿名') }}</td>
              <td>{{ l.userType }}</td>
              <td>{{ l.archiveNo || ('档案#' + l.archiveId) }}</td>
              <td>{{ l.accessType }}</td>
              <td class="mono">{{ l.ipAddress || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="hasNext" class="more-wrap">
        <el-button :loading="loadingMore" @click="loadMore">加载更多</el-button>
      </div>
      <div v-else-if="records.length > 0" class="detail-empty">已加载全部</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAccessLogs } from '@/api/access-log'
import { downloadCsv } from '@/utils/csvExport'
import type { AccessLogQuery, AccessType, ArchiveAccessLog } from '@/types/access-log'

const records = ref<ArchiveAccessLog[]>([])
const nextCursor = ref<string | null>(null)
const hasNext = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const loadError = ref(false)

const filters = reactive({
  accessType: '' as AccessType | '',
  userId: undefined as number | undefined,
  archiveId: undefined as number | undefined,
  startedAt: '',
  endedAt: '',
})

function buildQuery(cursor?: string): AccessLogQuery {
  return {
    accessType: filters.accessType || undefined,
    userId: filters.userId || undefined,
    archiveId: filters.archiveId || undefined,
    startedAt: filters.startedAt ? filters.startedAt + 'T00:00:00+08:00' : undefined,
    endedAt: filters.endedAt ? filters.endedAt + 'T23:59:59+08:00' : undefined,
    cursor,
    limit: 5,
  }
}

async function load(reset = false) {
  if (filters.startedAt && filters.endedAt && filters.startedAt > filters.endedAt) {
    ElMessage.error('开始时间不能晚于结束时间')
    return
  }
  loading.value = true
  loadError.value = false
  try {
    const res = await getAccessLogs(buildQuery())
    records.value = reset ? res.records : res.records
    nextCursor.value = res.nextCursor
    hasNext.value = res.hasNext
  } catch {
    loadError.value = true
    ElMessage.error('访问日志加载失败')
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (!nextCursor.value) return
  loadingMore.value = true
  try {
    const res = await getAccessLogs(buildQuery(nextCursor.value))
    records.value = [...records.value, ...res.records]
    nextCursor.value = res.nextCursor
    hasNext.value = res.hasNext
  } catch {
    ElMessage.error('加载更多失败')
  } finally {
    loadingMore.value = false
  }
}

function reset() {
  filters.accessType = ''
  filters.userId = undefined
  filters.archiveId = undefined
  filters.startedAt = ''
  filters.endedAt = ''
  load(true)
}

function exportLogs() {
  const rows = records.value.map((l) => ({
    accessedAt: l.accessedAt,
    user: l.userName || (l.userId ? '用户#' + l.userId : '匿名'),
    userType: l.userType,
    archive: l.archiveNo || ('档案#' + l.archiveId),
    accessType: l.accessType,
    ipAddress: l.ipAddress ?? '',
  }))
  downloadCsv('访问日志.csv', rows)
  ElMessage.success('已导出当前已加载的访问日志。')
}

onMounted(() => load(true))
</script>

<style scoped>
.access-logs { padding: 0; }
.page-title { font-size: 20px; font-weight: 700; }
.page-subtitle { color: #909399; margin-top: 6px; }
.card.panel { margin-top: 16px; }
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 10px; }
.form-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.field { display: grid; gap: 4px; }
.field label { font-size: 12px; color: #909399; }
.field input, .field select { height: 34px; padding: 0 8px; border: 1px solid var(--border, #e4e7ed); border-radius: 6px; }
.actions { display: flex; gap: 8px; align-items: center; }
.table-wrap { overflow-x: auto; }
.more-wrap { text-align: center; padding: 12px 0; }
.detail-empty { color: #909399; padding: 16px; text-align: center; }
.link { background: none; border: none; color: var(--primary, #1f6f78); cursor: pointer; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; }
@media (max-width: 900px) { .form-grid { grid-template-columns: 1fr 1fr; } }
</style>
````

- [ ] **Step 4：运行确认通过**

Run: `npm run test:unit -- src/views/admin/access-logs/index.spec.ts`
Expected: PASS（4 条）。

- [ ] **Step 5：类型 + 构建**

Run: `npm run build`
Expected: 无类型错误，构建成功。

- [ ] **Step 6：提交**

```bash
git add src/views/admin/access-logs/index.vue src/views/admin/access-logs/index.spec.ts
git commit -m "feat(audit-log-hu): 访问日志查询页与单测"
```

---

## Task 6：注册路由与菜单

**Files:**
- Modify: `src/router/routes/admin.ts`

- [ ] **Step 1：菜单项（在「系统管理」组 `children` 末尾追加 2 项）**

将：
```ts
    children: [
      { path: '/admin/user-management', title: '用户管理', roles: ['sys_admin'] },
      { path: '/admin/system-settings', title: '系统配置', roles: ['sys_admin'] },
    ],
```
改为：
```ts
    children: [
      { path: '/admin/user-management', title: '用户管理', roles: ['sys_admin'] },
      { path: '/admin/system-settings', title: '系统配置', roles: ['sys_admin'] },
      { path: '/admin/audit-logs', title: '审计日志', roles: ['sys_admin'] },
      { path: '/admin/access-logs', title: '访问日志', roles: ['sys_admin'] },
    ],
```

- [ ] **Step 2：路由项（在 `adminRoutes.children` 的 `system-settings` 行后追加 2 行）**

将：
```ts
    { path: 'system-settings', component: () => import('@/views/admin/system-settings/index.vue'), meta: { title: '系统配置' } },
  ],
```
改为：
```ts
    { path: 'system-settings', component: () => import('@/views/admin/system-settings/index.vue'), meta: { title: '系统配置' } },
    { path: 'audit-logs', component: () => import('@/views/admin/audit-logs/index.vue'), meta: { title: '审计日志' } },
    { path: 'access-logs', component: () => import('@/views/admin/access-logs/index.vue'), meta: { title: '访问日志' } },
  ],
```

- [ ] **Step 3：类型 + 构建校验**

Run: `npm run build`
Expected: 无类型错误，构建成功（新页面被路由懒加载引用）。

- [ ] **Step 4：提交**

```bash
git add src/router/routes/admin.ts
git commit -m "feat(audit-log-hu): 注册审计/访问日志路由与菜单"
```

---

## Task 7：整体验证（日志审计分支）

- [ ] **Step 1：全量单测**

Run: `npm run test:unit`
Expected: 全绿（csvExport / audit-log / access-log api 与页面等用例）。

- [ ] **Step 2：类型 + 构建**

Run: `npm run build`
Expected: vue-tsc 无错、vite build 成功。

- [ ] **Step 3：页面自检**

启动 `npm run dev`，以 sys_admin 登录，访问 `/admin/audit-logs` 与 `/admin/access-logs`，逐项核对：
- 筛选（操作人类型/模块/操作类型/业务类型/时间 或 访问类型/用户/档案/时间）、查询/重置；
- 「加载更多」追加无重复、导出下载 CSV；
- 无删除/编辑入口；加载中/失败重试/空态/已加载全部。

> 自检通过后**暂停**，等待用户验收页面；验收通过且用户确认后再 push 与开 PR（不在本计划范围内执行）。

---

## 自检（计划 vs 设计）

- 设计 §4 类型（CursorData / AuditLog / ArchiveAccessLog / Query）→ Task 1/2/3 ✅
- 设计 §5 API（getAuditLogs / getAccessLogs）→ Task 2/3；导出经 csvExport → Task 1 + 页面 ✅
- 设计 §6 mock（游标分页）→ Task 2/3 ✅
- 设计 §7 页面（两页结构、无删改、加载更多、导出、空态）→ Task 4/5 ✅
- 设计 §8 路由/菜单 → Task 6 ✅
- 设计 §9 校验（时间范围、导出范围）→ Task 4/5 内联 ✅
- 设计 §10 测试 → 各 Task 均含 `.spec.ts` ✅
- 无占位符：所有步骤均含完整代码与命令 ✅
- 类型一致：`CursorData<T>` / `AuditLogQuery` / `AccessLogQuery` / `getAuditLogs` / `getAccessLogs` 跨 Task 命名一致 ✅



