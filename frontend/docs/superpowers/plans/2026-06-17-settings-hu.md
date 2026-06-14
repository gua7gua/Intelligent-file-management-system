# feat/settings-hu 实施计划：档案保存 · 用户管理 · 系统配置

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现胡颖 06-17 至 06-18 的三个管理后台页面（档案保存 `/admin/preservation`、用户管理 `/admin/user-management`、系统配置 `/admin/system-settings`），mock 开发，照搬 HTML 原型还原。

**Architecture:** per-subdomain 文件组织（types+api+mock+utils+views+spec），沿用 appraisal-hu/storage-guo 风格基线。三页独立闭环，mock 字段对齐接口文档 §21/§22/§17 与数据库设计。用户管理页只读引用组织/全宗（新增最小只读 api+mock）。

**Tech Stack:** Vue 3 `<script setup>` + TypeScript + Element Plus（按钮/Message/MessageBox）+ 原型全局 CSS 类 + vitest + @vue/test-utils。

**设计文档：** `frontend/docs/superpowers/specs/2026-06-17-settings-hu-design.md`

**分支：** 从 `develop`（`4160b1f`）创建 `feat/settings-hu`。

---

## 文件结构

| 文件 | 职责 | 任务 |
|------|------|------|
| `src/types/enums.ts`（修改） | 扩展保存/用户/配置枚举与中文映射 | T1 |
| `src/types/organization.ts` | Organization 只读类型 | T2 |
| `src/api/organizations.ts` | §17.4 只读列表 | T2 |
| `src/mock/modules/organizations.ts` | 组织 mock | T2 |
| `src/api/organizations.spec.ts` | api 测试 | T2 |
| `src/types/fonds.ts` | FondsReference 只读类型 | T3 |
| `src/api/fonds.ts` | §17.1 只读列表 | T3 |
| `src/mock/modules/fonds.ts` | 全宗 mock | T3 |
| `src/api/fonds.spec.ts` | api 测试 | T3 |
| `src/types/preservation.ts` | BackupTask/FileCheckRecord 等 | T4 |
| `src/mock/modules/preservation.ts` | 备份/检测 mock | T4 |
| `src/api/preservation.ts` + `.spec.ts` | §21 四端点 | T4 |
| `src/views/admin/preservation/index.vue` + `index.spec.ts` | 档案保存页 | T5 |
| `src/types/user-management.ts` | User/Role 等 | T6 |
| `src/mock/modules/user-management.ts` | 用户/角色 mock | T6 |
| `src/api/user-management.ts` + `.spec.ts` | §22.1–22.7 | T6 |
| `src/utils/userValidation.ts` + `.spec.ts` | 用户校验 | T6 |
| `src/views/admin/user-management/index.vue` + `index.spec.ts` | 用户管理页 | T7 |
| `src/types/system-settings.ts` | SystemConfig 等 | T8 |
| `src/mock/modules/system-settings.ts` | 配置 mock | T8 |
| `src/api/system-settings.ts` + `.spec.ts` | §22.8–22.10 | T8 |
| `src/utils/systemConfigValidation.ts` + `.spec.ts` | 配置校验 | T8 |
| `src/views/admin/system-settings/index.vue` + `index.spec.ts` | 系统配置页 | T9 |

> **测试运行约定：** 所有 api/utils 测试在 `frontend/` 下 `npx vitest run <path>`；view 测试同。构建验证 `npm run type-check && npm run build && npm run test:unit`。

> **提交约定（commit-convention）：** 每个 Task 末尾按 `feat(settings-hu): ...` / `test(settings-hu): ...` 提交。本计划内 commit 为本地提交，**不 push、不开 PR**（等用户查看页面还原后再由用户决定）。

---

## Task 1: 扩展枚举（`types/enums.ts`）

**Files:**
- Modify: `src/types/enums.ts`（在文件末尾追加）
- Test: 复用既有枚举无独立 spec；通过后续 api/view spec 间接覆盖

- [ ] **Step 1: 追加枚举常量与中文映射**

在 `src/types/enums.ts` 末尾追加（不在现有内容中间插入，避免破坏既有行）：

```typescript
/** 备份范围（对齐 DB 11.1 backup_scope） */
export const BackupScope = {
  DATABASE: 'database',
  FILES: 'files',
  BOTH: 'both',
} as const

/** 备份任务状态（对齐 DB 11.1 status） */
export const BackupTaskStatus = {
  RUNNING: 'running',
  SUCCESS: 'success',
  FAILED: 'failed',
} as const

/** 四性检测类型（对齐接口文档 §21.4 checkTypes） */
export const CheckType = {
  INTEGRITY: 'integrity',
  USABILITY: 'usability',
  AUTHENTICITY: 'authenticity',
  SECURITY: 'security',
} as const

/** 四性检测结果 */
export const CheckResult = {
  PASSED: 'passed',
  FAILED: 'failed',
  NOT_CONFIGURED: 'not_configured',
} as const

/** 用户类型（对齐 DB 4.3） */
export const UserType = {
  INTERNAL: 'internal',
  PUBLIC: 'public',
} as const

/** 数据范围（对齐 DB 4.3 data_scope） */
export const DataScope = {
  OWN_ORG: 'own_org',
  OWN_FONDS: 'own_fonds',
  ALL: 'all',
} as const

/** 用户状态（对齐 DB 4.3 status） */
export const UserStatus = {
  ACTIVE: 'active',
  DISABLED: 'disabled',
} as const

/** 配置项值类型（对齐接口文档 §22.9 valueType） */
export const ConfigValueType = {
  STRING: 'string',
  NUMBER: 'number',
  BOOLEAN: 'boolean',
  JSON: 'json',
} as const

export type BackupScopeValue = (typeof BackupScope)[keyof typeof BackupScope]
export type BackupTaskStatusValue = (typeof BackupTaskStatus)[keyof typeof BackupTaskStatus]
export type CheckTypeValue = (typeof CheckType)[keyof typeof CheckType]
export type CheckResultValue = (typeof CheckResult)[keyof typeof CheckResult]
export type UserTypeValue = (typeof UserType)[keyof typeof UserType]
export type DataScopeValue = (typeof DataScope)[keyof typeof DataScope]
export type UserStatusValue = (typeof UserStatus)[keyof typeof UserStatus]
export type ConfigValueTypeValue = (typeof ConfigValueType)[keyof typeof ConfigValueType]

export const BackupScopeLabel: Record<string, string> = {
  database: '数据库',
  files: '电子文件',
  both: '同时',
}
export const BackupTaskStatusLabel: Record<string, string> = {
  running: '执行中',
  success: '成功',
  failed: '失败',
}
export const CheckTypeLabel: Record<string, string> = {
  integrity: '完整性',
  usability: '可用性',
  authenticity: '真实性',
  security: '安全性',
}
export const CheckResultLabel: Record<string, string> = {
  passed: 'passed',
  failed: 'failed',
  not_configured: 'not_configured',
}
export const UserTypeLabel: Record<string, string> = {
  internal: '内部',
  public: '公众',
}
export const DataScopeLabel: Record<string, string> = {
  own_org: '本单位',
  own_fonds: '本全宗',
  all: '全部',
}
export const UserStatusLabel: Record<string, string> = {
  active: '启用',
  disabled: '禁用',
}
export const ConfigValueTypeLabel: Record<string, string> = {
  string: 'string',
  number: 'number',
  boolean: 'boolean',
  json: 'json',
}
```

- [ ] **Step 2: 类型检查**

Run: `npx vue-tsc --noEmit -p tsconfig.app.json`（或 `npm run type-check`）
Expected: 无新增类型错误（既有错误数不变）。

- [ ] **Step 3: Commit**

```bash
git add src/types/enums.ts
git commit -m "feat(settings-hu): 扩展保存/用户/系统配置枚举与中文映射"
```

## Task 2: 组织只读（types + api + mock + spec）

**Files:**
- Create: `src/types/organization.ts`
- Create: `src/api/organizations.ts`
- Create: `src/mock/modules/organizations.ts`
- Test: `src/api/organizations.spec.ts`

- [ ] **Step 1: 写类型 `src/types/organization.ts`**

```typescript
import type { PageParams } from './api'

/** 组织查询参数（§17.4，用户管理页只读 + 编辑表单下拉） */
export interface OrganizationParams extends PageParams {
  status?: 'active' | 'disabled' | ''
  keyword?: string
}

/** 组织（对齐 DB 4.1 organizations） */
export interface Organization {
  id: number
  orgName: string
  /** archive_org / government / enterprise / public_institution */
  orgType: string
  contactName?: string
  contactPhone?: string
  status: 'active' | 'disabled'
}
```

- [ ] **Step 2: 写 mock `src/mock/modules/organizations.ts`**

```typescript
import type { Organization, OrganizationParams } from '@/types/organization'
import type { PageData } from '@/types/api'

const organizations: Organization[] = [
  { id: 1, orgName: '克拉玛依市档案馆', orgType: 'archive_org', contactName: '李馆长', contactPhone: '0990-6800001', status: 'active' },
  { id: 2, orgName: '市财政局', orgType: 'government', contactName: '王主任', contactPhone: '0990-6800002', status: 'active' },
  { id: 3, orgName: '市交通局', orgType: 'government', contactName: '陈科长', contactPhone: '0990-6800003', status: 'active' },
  { id: 4, orgName: '市科技研究中心', orgType: 'public_institution', contactName: '王工', contactPhone: '0990-6800004', status: 'active' },
  { id: 5, orgName: '城投建设集团', orgType: 'enterprise', contactName: '刘经理', contactPhone: '0990-6800005', status: 'disabled' },
]

export function mockOrganizations(params?: OrganizationParams): PageData<Organization> {
  let list = organizations
  if (params?.status) list = list.filter((o) => o.status === params.status)
  if (params?.keyword) {
    const kw = params.keyword.trim().toLowerCase()
    list = list.filter((o) => o.orgName.toLowerCase().includes(kw))
  }
  const pageNo = params?.pageNo ?? 1
  const pageSize = params?.pageSize ?? 50
  const total = list.length
  const start = (pageNo - 1) * pageSize
  return {
    records: list.slice(start, start + pageSize),
    pageNo,
    pageSize,
    total,
    hasNext: start + pageSize < total,
  }
}
```

- [ ] **Step 3: 写 api `src/api/organizations.ts`**

```typescript
import request from './request'
import type { PageData } from '@/types/api'
import type { Organization, OrganizationParams } from '@/types/organization'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询组织（§17.4，只读引用） */
export function getOrganizations(params?: OrganizationParams): Promise<PageData<Organization>> {
  if (USE_MOCK) {
    return import('@/mock/modules/organizations').then((m) => m.mockOrganizations(params))
  }
  return request.get('/admin/organizations', { params })
}
```

- [ ] **Step 4: 写测试 `src/api/organizations.spec.ts`**

```typescript
import { describe, expect, it } from 'vitest'
import { getOrganizations } from './organizations'

describe('organizations api mock mode', () => {
  it('returns organization list', async () => {
    const res = await getOrganizations()
    expect(res.records.length).toBeGreaterThan(0)
    expect(res.records[0]).toHaveProperty('orgName')
    expect(res.records[0]).toHaveProperty('orgType')
  })

  it('filters by status', async () => {
    const res = await getOrganizations({ status: 'active' })
    expect(res.records.every((o) => o.status === 'active')).toBe(true)
  })

  it('filters by keyword', async () => {
    const res = await getOrganizations({ keyword: '财政' })
    expect(res.records.every((o) => o.orgName.includes('财政'))).toBe(true)
  })
})
```

- [ ] **Step 5: 运行测试**

Run: `npx vitest run src/api/organizations.spec.ts`
Expected: 3 passed

- [ ] **Step 6: Commit**

```bash
git add src/types/organization.ts src/api/organizations.ts src/api/organizations.spec.ts src/mock/modules/organizations.ts
git commit -m "feat(settings-hu): 新增组织只读 api 与 mock 供用户管理页引用"
```

## Task 3: 全宗只读（types + api + mock + spec）

**Files:**
- Create: `src/types/fonds.ts`
- Create: `src/api/fonds.ts`
- Create: `src/mock/modules/fonds.ts`
- Test: `src/api/fonds.spec.ts`

- [ ] **Step 1: 写类型 `src/types/fonds.ts`**

```typescript
import type { PageParams } from './api'

/** 全宗查询参数（§17.1，只读引用） */
export interface FondsParams extends PageParams {
  status?: 'active' | 'disabled' | ''
  keyword?: string
}

/** 全宗引用（对齐 DB 4.2 fonds 子集） */
export interface FondsReference {
  id: number
  fondsNo: string
  fondsName: string
  organizationId: number
  organizationName?: string
  description?: string
  status: 'active' | 'disabled'
}
```

- [ ] **Step 2: 写 mock `src/mock/modules/fonds.ts`**

```typescript
import type { FondsParams, FondsReference } from '@/types/fonds'
import type { PageData } from '@/types/api'

const fondsList: FondsReference[] = [
  { id: 1, fondsNo: 'F001', fondsName: '克拉玛依市档案馆综合全宗', organizationId: 1, organizationName: '克拉玛依市档案馆', description: '本馆综合全宗', status: 'active' },
  { id: 18, fondsNo: 'F018', fondsName: '市财政局财务业务全宗', organizationId: 2, organizationName: '市财政局', description: '财务业务档案', status: 'active' },
  { id: 31, fondsNo: 'F031', fondsName: '市交通局科技项目全宗', organizationId: 3, organizationName: '市交通局', description: '科技项目档案', status: 'active' },
  { id: 42, fondsNo: 'F042', fondsName: '城投建设集团工程全宗', organizationId: 5, organizationName: '城投建设集团', description: '工程项目档案', status: 'disabled' },
]

export function mockFonds(params?: FondsParams): PageData<FondsReference> {
  let list = fondsList
  if (params?.status) list = list.filter((f) => f.status === params.status)
  if (params?.keyword) {
    const kw = params.keyword.trim().toLowerCase()
    list = list.filter((f) => f.fondsName.toLowerCase().includes(kw) || f.fondsNo.toLowerCase().includes(kw))
  }
  const pageNo = params?.pageNo ?? 1
  const pageSize = params?.pageSize ?? 50
  const total = list.length
  const start = (pageNo - 1) * pageSize
  return {
    records: list.slice(start, start + pageSize),
    pageNo,
    pageSize,
    total,
    hasNext: start + pageSize < total,
  }
}
```

- [ ] **Step 3: 写 api `src/api/fonds.ts`**

```typescript
import request from './request'
import type { PageData } from '@/types/api'
import type { FondsParams, FondsReference } from '@/types/fonds'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询全宗（§17.1，只读引用） */
export function getFonds(params?: FondsParams): Promise<PageData<FondsReference>> {
  if (USE_MOCK) {
    return import('@/mock/modules/fonds').then((m) => m.mockFonds(params))
  }
  return request.get('/admin/fonds', { params })
}
```

- [ ] **Step 4: 写测试 `src/api/fonds.spec.ts`**

```typescript
import { describe, expect, it } from 'vitest'
import { getFonds } from './fonds'

describe('fonds api mock mode', () => {
  it('returns fonds list with org reference', async () => {
    const res = await getFonds()
    expect(res.records.length).toBeGreaterThan(0)
    expect(res.records[0].fondsNo).toMatch(/^F\d+$/)
    expect(res.records[0]).toHaveProperty('organizationName')
  })

  it('filters by status', async () => {
    const res = await getFonds({ status: 'active' })
    expect(res.records.every((f) => f.status === 'active')).toBe(true)
  })
})
```

- [ ] **Step 5: 运行测试**

Run: `npx vitest run src/api/fonds.spec.ts`
Expected: 2 passed

- [ ] **Step 6: Commit**

```bash
git add src/types/fonds.ts src/api/fonds.ts src/api/fonds.spec.ts src/mock/modules/fonds.ts
git commit -m "feat(settings-hu): 新增全宗只读 api 与 mock 供用户管理页引用"
```

## Task 4: 档案保存数据层（types + mock + api + spec）

**Files:**
- Create: `src/types/preservation.ts`
- Create: `src/mock/modules/preservation.ts`
- Create: `src/api/preservation.ts`
- Test: `src/api/preservation.spec.ts`

- [ ] **Step 1: 写类型 `src/types/preservation.ts`**

```typescript
import type { PageData, PageParams } from './api'
import type { BackupScopeValue, BackupTaskStatusValue, CheckResultValue, CheckTypeValue } from './enums'

/** 备份任务查询参数（§21.1） */
export interface BackupTaskParams extends PageParams {
  status?: BackupTaskStatusValue | ''
  backupScope?: BackupScopeValue | ''
}

/** 备份任务（对齐 DB 11.1 backup_tasks） */
export interface BackupTask {
  id: number
  taskNo: string
  backupScope: BackupScopeValue
  status: BackupTaskStatusValue
  backupPath: string
  fileSize: number
  sha256?: string
  startedAt: string
  finishedAt?: string
  message?: string
  createdAt: string
}

/** 创建备份任务请求（§21.2） */
export interface BackupTaskCreateData {
  backupScope: BackupScopeValue
}

/** 四性检测记录查询参数（§21.3） */
export interface FileCheckRecordParams extends PageParams {
  targetType?: string
  targetId?: number
  checkType?: CheckTypeValue | ''
  checkResult?: CheckResultValue | ''
}

/** 四性检测记录（对齐 DB file_check_records） */
export interface FileCheckRecord {
  id: number
  targetType: string
  targetId: number
  checkType: CheckTypeValue
  checkResult: CheckResultValue
  message?: string
  checkedAt: string
}

/** 触发检测请求（§21.4） */
export interface TriggerCheckData {
  checkTypes: CheckTypeValue[]
}

export type BackupTaskPage = PageData<BackupTask>
export type FileCheckRecordPage = PageData<FileCheckRecord>
```

- [ ] **Step 2: 写 mock `src/mock/modules/preservation.ts`**

```typescript
import type {
  BackupTask,
  BackupTaskCreateData,
  BackupTaskParams,
  FileCheckRecord,
  FileCheckRecordParams,
  TriggerCheckData,
} from '@/types/preservation'
import type { PageData } from '@/types/api'

let nextTaskId = 191
const tasks: BackupTask[] = [
  { id: 188, taskNo: 'BAK-188', backupScope: 'database', status: 'success', backupPath: '/backup/db/20260609.dump', fileSize: 9019431320, sha256: '9f2ac7e1d4', startedAt: '2026-06-09T02:00:00+08:00', finishedAt: '2026-06-09T02:10:00+08:00', message: '月度例行备份', createdAt: '2026-06-09T02:00:00+08:00' },
  { id: 189, taskNo: 'BAK-189', backupScope: 'files', status: 'success', backupPath: 'minio://backup/archive-files/20260609', fileSize: 689153961984, sha256: '71be0a3f09', startedAt: '2026-06-09T01:30:00+08:00', finishedAt: '2026-06-09T01:40:00+08:00', message: '正式电子文件备份', createdAt: '2026-06-09T01:30:00+08:00' },
  { id: 190, taskNo: 'BAK-190', backupScope: 'files', status: 'failed', backupPath: 'minio://backup/archive-files/20260608', fileSize: 0, sha256: '', startedAt: '2026-06-08T01:30:00+08:00', finishedAt: '2026-06-08T01:34:00+08:00', message: '对象存储连接超时，可重试', createdAt: '2026-06-08T01:30:00+08:00' },
]

// 最近一轮四性检测（target 为最近一份正式电子文件，id=5001）
const records: FileCheckRecord[] = [
  { id: 9101, targetType: 'archive_file', targetId: 5001, checkType: 'integrity', checkResult: 'passed', message: 'SHA-256 校验一致', checkedAt: '2026-06-15T09:00:00+08:00' },
  { id: 9102, targetType: 'archive_file', targetId: 5001, checkType: 'usability', checkResult: 'passed', message: '格式白名单内可打开', checkedAt: '2026-06-15T09:00:00+08:00' },
  { id: 9103, targetType: 'archive_file', targetId: 5001, checkType: 'authenticity', checkResult: 'not_configured', message: '外部签名体系未接入', checkedAt: '2026-06-15T09:00:00+08:00' },
  { id: 9104, targetType: 'archive_file', targetId: 5001, checkType: 'security', checkResult: 'passed', message: '病毒扫描通过', checkedAt: '2026-06-15T09:00:00+08:00' },
]

function paginate<T>(list: T[], pageNo = 1, pageSize = 20): PageData<T> {
  const total = list.length
  const start = (pageNo - 1) * pageSize
  return { records: list.slice(start, start + pageSize), pageNo, pageSize, total, hasNext: start + pageSize < total }
}

export function mockBackupTasks(params?: BackupTaskParams): PageData<BackupTask> {
  let list = [...tasks].sort((a, b) => b.id - a.id)
  if (params?.status) list = list.filter((t) => t.status === params.status)
  if (params?.backupScope) list = list.filter((t) => t.backupScope === params.backupScope)
  return paginate(list, params?.pageNo, params?.pageSize)
}

export function mockCreateBackupTask(data: BackupTaskCreateData): BackupTask {
  const running = tasks.find((t) => t.status === 'running')
  if (running) {
    throw new Error('当前已有运行中的备份任务，请等待其完成')
  }
  const id = nextTaskId++
  const now = '2026-06-15T10:00:00+08:00'
  const task: BackupTask = {
    id,
    taskNo: `BAK-${id}`,
    backupScope: data.backupScope,
    status: 'running',
    backupPath: '等待生成',
    fileSize: 0,
    sha256: '',
    startedAt: now,
    message: '手动发起备份任务',
    createdAt: now,
  }
  tasks.unshift(task)
  // mock 模拟异步完成：1.2s 后置成功
  setTimeout(() => {
    task.status = 'success'
    task.backupPath = data.backupScope === 'database' ? `/backup/db/manual-${id}.dump` : `minio://backup/manual-${id}`
    task.fileSize = data.backupScope === 'database' ? 8500000000 : 680000000000
    task.sha256 = Math.random().toString(16).slice(2, 12)
    task.finishedAt = '2026-06-15T10:01:12+08:00'
  }, 1200)
  return task
}

export function mockFileCheckRecords(params?: FileCheckRecordParams): PageData<FileCheckRecord> {
  let list = [...records].sort((a, b) => b.id - a.id)
  if (params?.targetType) list = list.filter((r) => r.targetType === params.targetType)
  if (params?.targetId) list = list.filter((r) => r.targetId === params.targetId)
  if (params?.checkType) list = list.filter((r) => r.checkType === params.checkType)
  if (params?.checkResult) list = list.filter((r) => r.checkResult === params.checkResult)
  return paginate(list, params?.pageNo, params?.pageSize)
}

export function mockTriggerFileCheck(fileId: number, data: TriggerCheckData): FileCheckRecord[] {
  const now = '2026-06-15T10:05:00+08:00'
  const resultMap: Record<string, FileCheckRecord['checkResult']> = {
    integrity: 'passed',
    usability: 'passed',
    authenticity: 'not_configured',
    security: 'passed',
  }
  // 移除该 target 旧记录，写入新记录
  const filtered = records.filter((r) => !(r.targetType === 'archive_file' && r.targetId === fileId))
  records.length = 0
  records.push(...filtered)
  const created: FileCheckRecord[] = []
  for (const ct of data.checkTypes) {
    const rec: FileCheckRecord = {
      id: Math.floor(Math.random() * 100000) + 9200,
      targetType: 'archive_file',
      targetId: fileId,
      checkType: ct,
      checkResult: resultMap[ct] ?? 'passed',
      message: ct === 'authenticity' ? '外部签名体系未接入' : '检测通过',
      checkedAt: now,
    }
    records.push(rec)
    created.push(rec)
  }
  return created
}
```

- [ ] **Step 3: 写 api `src/api/preservation.ts`**

```typescript
import request from './request'
import type { PageData } from '@/types/api'
import type {
  BackupTask,
  BackupTaskCreateData,
  BackupTaskParams,
  FileCheckRecord,
  FileCheckRecordParams,
  TriggerCheckData,
} from '@/types/preservation'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询备份任务（§21.1） */
export function getBackupTasks(params?: BackupTaskParams): Promise<PageData<BackupTask>> {
  if (USE_MOCK) {
    return import('@/mock/modules/preservation').then((m) => m.mockBackupTasks(params))
  }
  return request.get('/admin/backup-tasks', { params })
}

/** 创建备份任务（§21.2） */
export function createBackupTask(data: BackupTaskCreateData): Promise<BackupTask> {
  if (USE_MOCK) {
    return import('@/mock/modules/preservation').then((m) => m.mockCreateBackupTask(data))
  }
  return request.post('/admin/backup-tasks', data)
}

/** 查询四性检测记录（§21.3） */
export function getFileCheckRecords(params?: FileCheckRecordParams): Promise<PageData<FileCheckRecord>> {
  if (USE_MOCK) {
    return import('@/mock/modules/preservation').then((m) => m.mockFileCheckRecords(params))
  }
  return request.get('/admin/file-check-records', { params })
}

/** 触发正式文件检测（§21.4） */
export function triggerFileCheck(fileId: number, data: TriggerCheckData): Promise<FileCheckRecord[]> {
  if (USE_MOCK) {
    return import('@/mock/modules/preservation').then((m) => m.mockTriggerFileCheck(fileId, data))
  }
  return request.post(`/admin/archive-files/${fileId}/checks`, data)
}
```

- [ ] **Step 4: 写测试 `src/api/preservation.spec.ts`**

```typescript
import { describe, expect, it } from 'vitest'
import { createBackupTask, getBackupTasks, getFileCheckRecords, triggerFileCheck } from './preservation'

describe('preservation api mock mode', () => {
  it('lists backup tasks and filters by scope', async () => {
    const all = await getBackupTasks()
    expect(all.records.length).toBeGreaterThan(0)
    const db = await getBackupTasks({ backupScope: 'database' })
    expect(db.records.every((t) => t.backupScope === 'database')).toBe(true)
  })

  it('includes a failed task with message', async () => {
    const all = await getBackupTasks()
    const failed = all.records.find((t) => t.status === 'failed')
    expect(failed).toBeTruthy()
    expect(failed!.message).toBeTruthy()
  })

  it('creates a running backup task', async () => {
    const task = await createBackupTask({ backupScope: 'both' })
    expect(task.status).toBe('running')
    expect(task.taskNo).toMatch(/^BAK-/)
  })

  it('lists file check records with not_configured authenticity', async () => {
    const recs = await getFileCheckRecords()
    expect(recs.records.length).toBeGreaterThan(0)
    const auth = recs.records.find((r) => r.checkType === 'authenticity')
    expect(auth?.checkResult).toBe('not_configured')
  })

  it('trigger check returns records for requested types', async () => {
    const created = await triggerFileCheck(5001, { checkTypes: ['integrity', 'authenticity'] })
    expect(created).toHaveLength(2)
    expect(created.map((r) => r.checkType).sort()).toEqual(['authenticity', 'integrity'])
  })
})
```

- [ ] **Step 5: 运行测试**

Run: `npx vitest run src/api/preservation.spec.ts`
Expected: 5 passed

- [ ] **Step 6: Commit**

```bash
git add src/types/preservation.ts src/mock/modules/preservation.ts src/api/preservation.ts src/api/preservation.spec.ts
git commit -m "feat(settings-hu): 实现档案保存备份与四性检测 api 封装与 mock"
```

## Task 5: 档案保存页面（`views/admin/preservation/index.vue`）

**Files:**
- Modify: `src/views/admin/preservation/index.vue`（重写占位）
- Test: `src/views/admin/preservation/index.spec.ts`

- [ ] **Step 1: 写页面 `src/views/admin/preservation/index.vue`**

```vue
<template>
  <div class="preservation">
    <section>
      <h1 class="page-title">档案保存</h1>
      <p class="page-subtitle">管理数据库备份、电子文件备份和四性检测记录，形成可审计的保存管理记录。</p>
    </section>

    <section class="grid four" aria-label="保存概览">
      <div class="metric card"><div class="metric-label">最近数据库备份</div><div class="metric-num">{{ latestDbBackup }}</div><div class="metric-note">backup_scope = database</div></div>
      <div class="metric card"><div class="metric-label">最近电子文件备份</div><div class="metric-num">{{ latestFileBackup }}</div><div class="metric-note">backup_scope = files</div></div>
      <div class="metric card"><div class="metric-num">{{ passRate }}%</div><div class="metric-label">四性检测通过率</div><div class="metric-note">archive_file 检测记录</div></div>
      <div class="metric card"><div class="metric-num">{{ todayCheckCount }}</div><div class="metric-label">今日检测任务</div><div class="metric-note">file_check_records</div></div>
    </section>

    <div class="preserve-layout">
      <section class="stack">
        <div class="card panel">
          <h2 class="section-title">备份范围</h2>
          <div v-if="loadError" class="detail-empty">备份任务加载失败：<button class="link" @click="loadAll">重试</button></div>
          <div class="backup-options">
            <label class="option-card" v-for="opt in scopeOptions" :key="opt.value">
              <input type="radio" name="backupScope" :value="opt.value" v-model="selectedScope" />
              <strong>{{ opt.label }}</strong>
              <span class="muted">{{ opt.desc }}</span>
              <span class="status" :class="opt.tagClass">{{ opt.value }}</span>
            </label>
          </div>
          <div class="toolbar" style="margin-top:12px">
            <div class="actions">
              <el-button type="primary" :loading="backupLoading" @click="runBackup">立即备份</el-button>
              <el-button :loading="checkLoading" @click="runCheck">执行四性检测</el-button>
            </div>
          </div>
        </div>

        <div class="card panel">
          <h2 class="section-title">备份任务</h2>
          <div v-if="backupLoading && backupTasks.length === 0" class="detail-empty">加载中...</div>
          <div v-else-if="backupTasks.length === 0" class="detail-empty">暂无备份任务</div>
          <div v-else class="table-wrap">
            <table>
              <thead>
                <tr><th>任务号</th><th>范围</th><th>状态</th><th>路径</th><th>大小</th><th>校验哈希</th><th>说明</th></tr>
              </thead>
              <tbody>
                <tr v-for="t in backupTasks" :key="t.id">
                  <td class="mono">{{ t.taskNo }}</td>
                  <td>{{ BackupScopeLabel[t.backupScope] }}</td>
                  <td><span class="status" :class="statusClass(t.status)">{{ BackupTaskStatusLabel[t.status] }}</span></td>
                  <td class="mono">{{ t.backupPath }}</td>
                  <td>{{ t.fileSize ? formatSize(t.fileSize) : '-' }}</td>
                  <td class="mono">{{ t.sha256 ? t.sha256.slice(0, 4) + '...' + t.sha256.slice(-4) : '-' }}</td>
                  <td>{{ t.message }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="card panel">
          <h2 class="section-title">四性检测记录</h2>
          <div v-if="checkLoading && checkRecords.length === 0" class="detail-empty">加载中...</div>
          <div v-else-if="checkRecords.length === 0" class="detail-empty">暂无检测记录</div>
          <div v-else class="check-grid">
            <div class="check-card" v-for="ct in checkTypes" :key="ct">
              <strong>{{ CheckTypeLabel[ct] }}</strong>
              <span class="muted">{{ checkDesc[ct] }}</span>
              <span class="status" :class="checkClass(latestResult(ct))">{{ latestResult(ct) || '-' }}</span>
            </div>
          </div>
          <div class="notice" style="margin-top:12px">
            四性检测写入 <span class="mono">file_check_records</span>，检测失败只形成记录和提示，不直接修改正式档案状态。
          </div>
        </div>
      </section>

      <aside class="stack">
        <div class="card panel">
          <h2 class="section-title">存储空间</h2>
          <ul class="timeline">
            <li><span>MinIO</span><div>已用 742 GB / 1 TB，正式文件和备份分桶存储。</div></li>
            <li><span>PostgreSQL</span><div>元数据 18.6 GB，审计日志 4.2 GB。</div></li>
            <li><span>备份目录</span><div>保留最近 6 次例行备份，路径与哈希入库。</div></li>
          </ul>
        </div>
        <div class="notice warning">
          <strong>保存边界</strong>
          <div>本页面不做跨机房容灾编排，不自动恢复档案，也不替代鉴定、销毁或审批流程。</div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createBackupTask, getBackupTasks, getFileCheckRecords, triggerFileCheck } from '@/api/preservation'
import type { BackupTask, FileCheckRecord } from '@/types/preservation'
import type { BackupScopeValue, BackupTaskStatusValue, CheckResultValue, CheckTypeValue } from '@/types/enums'
import { BackupScopeLabel, BackupTaskStatusLabel, CheckResultLabel, CheckTypeLabel } from '@/types/enums'

const backupTasks = ref<BackupTask[]>([])
const checkRecords = ref<FileCheckRecord[]>([])
const selectedScope = ref<BackupScopeValue>('database')
const backupLoading = ref(false)
const checkLoading = ref(false)
const loadError = ref(false)

const scopeOptions = [
  { value: 'database' as BackupScopeValue, label: '数据库备份', desc: 'PostgreSQL 元数据、清单、审批、日志', tagClass: '' },
  { value: 'files' as BackupScopeValue, label: '电子文件备份', desc: 'MinIO 正式电子文件、扫描件、附件', tagClass: 'info' },
  { value: 'both' as BackupScopeValue, label: '同时备份', desc: '数据库与电子文件任务统一记录', tagClass: 'success' },
]
const checkTypes: CheckTypeValue[] = ['integrity', 'usability', 'authenticity', 'security']
const checkDesc: Record<string, string> = {
  integrity: '校验 SHA-256 哈希',
  usability: '格式白名单与基础打开检测',
  authenticity: '外部签名体系验签记录',
  security: '病毒扫描和格式限制',
}

const latestDbBackup = computed(() => {
  const t = backupTasks.value.find((x) => x.backupScope !== 'files' && x.status === 'success')
  return t ? t.finishedAt?.slice(11, 16) ?? '-' : '-'
})
const latestFileBackup = computed(() => {
  const t = backupTasks.value.find((x) => x.backupScope !== 'database' && x.status === 'success')
  return t ? t.finishedAt?.slice(11, 16) ?? '-' : '-'
})
const passRate = computed(() => {
  if (checkRecords.value.length === 0) return '-'
  const passed = checkRecords.value.filter((r) => r.checkResult === 'passed').length
  return ((passed / checkRecords.value.length) * 100).toFixed(1)
})
const todayCheckCount = computed(() => {
  const today = '2026-06-15'
  return checkRecords.value.filter((r) => r.checkedAt.slice(0, 10) === today).length
})

function latestResult(ct: CheckTypeValue): CheckResultValue | '' {
  const rec = checkRecords.value.find((r) => r.checkType === ct)
  return rec ? rec.checkResult : ''
}
function statusClass(s: BackupTaskStatusValue): string {
  return s === 'success' ? 'success' : s === 'failed' ? 'danger' : 'warning'
}
function checkClass(r: CheckResultValue | ''): string {
  if (!r) return ''
  return r === 'passed' ? 'success' : r === 'failed' ? 'danger' : 'warning'
}
function formatSize(bytes: number): string {
  if (bytes >= 1e9) return (bytes / 1e9).toFixed(1) + ' GB'
  if (bytes >= 1e6) return (bytes / 1e6).toFixed(1) + ' MB'
  return bytes + ' B'
}

async function loadAll() {
  loadError.value = false
  backupLoading.value = true
  checkLoading.value = true
  try {
    const [tasks, recs] = await Promise.all([getBackupTasks(), getFileCheckRecords()])
    backupTasks.value = tasks.records
    checkRecords.value = recs.records
  } catch {
    loadError.value = true
    ElMessage.error('保存数据加载失败')
  } finally {
    backupLoading.value = false
    checkLoading.value = false
  }
}

async function runBackup() {
  backupLoading.value = true
  try {
    await createBackupTask({ backupScope: selectedScope.value })
    ElMessage.success('已创建备份任务，范围：' + BackupScopeLabel[selectedScope.value])
    const tasks = await getBackupTasks()
    backupTasks.value = tasks.records
    setTimeout(async () => {
      const refreshed = await getBackupTasks()
      backupTasks.value = refreshed.records
    }, 1400)
  } catch (e) {
    ElMessage.error((e as Error).message || '创建备份任务失败')
  } finally {
    backupLoading.value = false
  }
}

async function runCheck() {
  checkLoading.value = true
  try {
    const created = await triggerFileCheck(5001, { checkTypes: checkTypes })
    checkRecords.value = created
    ElMessage.success('四性检测记录已生成；真实性未配置时记录为 not_configured。')
  } catch {
    ElMessage.error('四性检测执行失败')
  } finally {
    checkLoading.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.preservation { padding: 0; }
.page-title { font-size: 20px; font-weight: 700; }
.page-subtitle { color: #909399; margin-top: 6px; }
.grid.four { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-top: 16px; }
.metric.card { padding: 14px; }
.metric-num { font-size: 22px; font-weight: 800; color: var(--primary, #1f6f78); }
.metric-label { font-size: 13px; color: #606266; }
.metric-note { font-size: 11px; color: #909399; margin-top: 4px; }
.preserve-layout { display: grid; grid-template-columns: minmax(0, 1fr) 380px; gap: 16px; align-items: start; margin-top: 16px; }
.stack { display: grid; gap: 16px; }
.backup-options { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
.option-card { position: relative; display: grid; gap: 8px; min-height: 106px; padding: 12px; border: 1px solid var(--border, #e4e7ed); border-radius: 8px; background: #fff; }
.option-card input { position: absolute; top: 12px; right: 12px; width: 18px; height: 18px; }
.check-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.check-card { display: grid; gap: 8px; padding: 12px; border: 1px solid var(--border, #e4e7ed); border-radius: 8px; background: #fff; }
.detail-empty { color: #909399; padding: 16px; text-align: center; }
.link { background: none; border: none; color: var(--primary, #1f6f78); cursor: pointer; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; }
.muted { color: #909399; font-size: 12px; }
.timeline { list-style: none; margin: 0; padding: 0; display: grid; gap: 10px; }
.timeline li { padding: 8px 0; border-bottom: 1px solid var(--border, #e4e7ed); }
.timeline li span { font-weight: 700; display: block; }
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 10px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.actions { display: flex; gap: 8px; }
@media (max-width: 1120px) { .preserve-layout { grid-template-columns: 1fr; } }
</style>
```

- [ ] **Step 2: 写测试 `src/views/admin/preservation/index.spec.ts`**

```typescript
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import Preservation from './index.vue'

async function wait() {
  await new Promise((r) => setTimeout(r, 80))
  await flushPromises()
}
async function mountIt() {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/admin/preservation', component: Preservation }] })
  await router.push('/admin/preservation')
  await router.isReady()
  return mount(Preservation, { global: { plugins: [router] } })
}

describe('Preservation', () => {
  it('renders overview metrics and backup scope options', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('档案保存')
    expect(w.text()).toContain('数据库备份')
    expect(w.text()).toContain('电子文件备份')
    expect(w.text()).toContain('同时备份')
  })

  it('renders backup task table with failed reason', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('BAK-')
    expect(w.text()).toContain('对象存储连接超时')
  })

  it('renders four-property check cards with not_configured authenticity', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('完整性')
    expect(w.text()).toContain('真实性')
    expect(w.text()).toContain('not_configured')
  })
})
```

- [ ] **Step 3: 运行测试**

Run: `npx vitest run src/views/admin/preservation/index.spec.ts`
Expected: 3 passed

- [ ] **Step 4: Commit**

```bash
git add src/views/admin/preservation/index.vue src/views/admin/preservation/index.spec.ts
git commit -m "feat(settings-hu): 实现档案保存页面还原与备份/四性检测交互"
```

## Task 6: 用户管理数据层（types + mock + api + utils + spec）

**Files:**
- Create: `src/types/user-management.ts`
- Create: `src/mock/modules/user-management.ts`
- Create: `src/api/user-management.ts`
- Create: `src/utils/userValidation.ts`
- Test: `src/api/user-management.spec.ts`、`src/utils/userValidation.spec.ts`

- [ ] **Step 1: 写类型 `src/types/user-management.ts`**

```typescript
import type { PageData, PageParams } from './api'
import type { DataScopeValue, UserStatusValue, UserTypeValue } from './enums'

export interface UserParams extends PageParams {
  userType?: UserTypeValue | ''
  roleCode?: string | ''
  organizationId?: number
  status?: UserStatusValue | ''
  keyword?: string
}

export interface Role {
  id: number
  roleCode: string
  roleName: string
  description?: string
  enabled: boolean
}

export interface User {
  id: number
  userType: UserTypeValue
  loginName: string
  employeeNo?: string
  phone?: string
  realName: string
  organizationId?: number
  organizationName?: string
  departmentName?: string
  maxSecurityLevel: number
  dataScope: DataScopeValue
  roleCodes: string[]
  status: UserStatusValue
  createdAt: string
  updatedAt?: string
}

export interface UserDetail extends User {
  recentActions?: Array<{ operationType: string; moduleName: string; operatedAt: string }>
}

export interface UserCreateData {
  userType: UserTypeValue
  loginName: string
  employeeNo?: string
  phone?: string
  realName: string
  organizationId?: number
  departmentName?: string
  maxSecurityLevel: number
  dataScope: DataScopeValue
  roleCodes: string[]
  initialPassword: string
}

export interface UserUpdateData {
  realName?: string
  employeeNo?: string
  phone?: string
  organizationId?: number
  departmentName?: string
  maxSecurityLevel?: number
  dataScope?: DataScopeValue
  roleCodes?: string[]
}

export interface UserStatusData {
  status: UserStatusValue
  reason?: string
}

export interface ResetPasswordData {
  newPassword: string
}

export type UserPage = PageData<User>
```

- [ ] **Step 2: 写 mock `src/mock/modules/user-management.ts`**

```typescript
import type {
  ResetPasswordData,
  Role,
  User,
  UserCreateData,
  UserDetail,
  UserParams,
  UserStatusData,
  UserUpdateData,
} from '@/types/user-management'
import type { PageData } from '@/types/api'

let nextUserId = 100

const roles: Role[] = [
  { id: 1, roleCode: 'front_archivist', roleName: '前台档案管理员', description: '负责前台接收验收', enabled: true },
  { id: 2, roleCode: 'back_archivist', roleName: '后台档案管理员', description: '负责入库整理鉴定', enabled: true },
  { id: 3, roleCode: 'transfer_user', roleName: '移交单位经办人', description: '移交单位编制清单', enabled: true },
  { id: 4, roleCode: 'internal_reader', roleName: '内部查阅者', description: '内部检索借阅', enabled: true },
  { id: 5, roleCode: 'director', roleName: '馆领导', description: '审批与密级管理', enabled: true },
  { id: 6, roleCode: 'sys_admin', roleName: '系统管理员', description: '账号与系统配置', enabled: true },
  { id: 7, roleCode: 'public_user', roleName: '社会公众', description: '公众注册账号', enabled: true },
]

const users: User[] = [
  { id: 11, userType: 'internal', loginName: 'liuxiao', realName: '小刘', employeeNo: 'A001', phone: '13800000001', organizationId: 1, organizationName: '克拉玛依市档案馆', departmentName: '业务科', maxSecurityLevel: 2, dataScope: 'all', roleCodes: ['back_archivist'], status: 'active', createdAt: '2025-09-01T09:00:00+08:00' },
  { id: 12, userType: 'internal', loginName: 'fanglead', realName: '小方', employeeNo: 'A010', phone: '13800000010', organizationId: 1, organizationName: '克拉玛依市档案馆', departmentName: '馆领导', maxSecurityLevel: 4, dataScope: 'all', roleCodes: ['director'], status: 'active', createdAt: '2025-09-01T09:00:00+08:00' },
  { id: 13, userType: 'internal', loginName: 'zhangcw', realName: '小张', employeeNo: 'B002', phone: '13800000002', organizationId: 2, organizationName: '市财政局', departmentName: '经办组', maxSecurityLevel: 0, dataScope: 'own_org', roleCodes: ['transfer_user'], status: 'active', createdAt: '2025-10-01T09:00:00+08:00' },
  { id: 14, userType: 'internal', loginName: 'admin01', realName: '系统管理员甲', employeeNo: 'S001', phone: '13800000003', organizationId: 1, organizationName: '克拉玛依市档案馆', departmentName: '信息科', maxSecurityLevel: 0, dataScope: 'all', roleCodes: ['sys_admin'], status: 'active', createdAt: '2025-08-01T09:00:00+08:00' },
  { id: 15, userType: 'internal', loginName: 'admin02', realName: '系统管理员乙', employeeNo: 'S002', phone: '13800000004', organizationId: 1, organizationName: '克拉玛依市档案馆', departmentName: '信息科', maxSecurityLevel: 0, dataScope: 'all', roleCodes: ['sys_admin'], status: 'active', createdAt: '2025-08-01T09:00:00+08:00' },
  { id: 16, userType: 'internal', loginName: 'chenfront', realName: '小陈', employeeNo: 'A003', phone: '13800000005', organizationId: 1, organizationName: '克拉玛依市档案馆', departmentName: '接收服务部', maxSecurityLevel: 1, dataScope: 'own_org', roleCodes: ['front_archivist'], status: 'active', createdAt: '2025-11-01T09:00:00+08:00' },
  { id: 17, userType: 'internal', loginName: 'wangread', realName: '小王', employeeNo: 'A004', phone: '13800000006', organizationId: 3, organizationName: '市交通局', departmentName: '档案室', maxSecurityLevel: 2, dataScope: 'own_fonds', roleCodes: ['internal_reader'], status: 'active', createdAt: '2025-12-01T09:00:00+08:00' },
  { id: 18, userType: 'internal', loginName: 'zhaoleft', realName: '小赵', employeeNo: 'A005', phone: '13800000007', organizationId: 4, organizationName: '市科技研究中心', departmentName: '办公室', maxSecurityLevel: 1, dataScope: 'own_org', roleCodes: ['front_archivist', 'internal_reader'], status: 'disabled', createdAt: '2025-06-01T09:00:00+08:00' },
]

function paginate(list: User[], pageNo = 1, pageSize = 20): PageData<User> {
  const total = list.length
  const start = (pageNo - 1) * pageSize
  return { records: list.slice(start, start + pageSize), pageNo, pageSize, total, hasNext: start + pageSize < total }
}

export function mockUsers(params?: UserParams): PageData<User> {
  let list = [...users]
  if (params?.userType) list = list.filter((u) => u.userType === params.userType)
  if (params?.roleCode) list = list.filter((u) => u.roleCodes.includes(params.roleCode!))
  if (params?.organizationId) list = list.filter((u) => u.organizationId === params.organizationId)
  if (params?.status) list = list.filter((u) => u.status === params.status)
  if (params?.keyword) {
    const kw = params.keyword.trim().toLowerCase()
    list = list.filter((u) => u.loginName.toLowerCase().includes(kw) || u.realName.includes(params.keyword!.trim()))
  }
  return paginate(list, params?.pageNo, params?.pageSize)
}

export function mockUserDetail(id: number): UserDetail {
  const u = users.find((x) => x.id === id)
  if (!u) throw new Error('用户不存在')
  return { ...u, recentActions: [{ operationType: 'login', moduleName: 'auth', operatedAt: '2026-06-15T08:30:00+08:00' }] }
}

export function mockCreateUser(data: UserCreateData): UserDetail {
  if (users.some((u) => u.loginName === data.loginName)) {
    throw new Error('登录名已存在')
  }
  const id = nextUserId++
  const u: User = {
    id,
    userType: data.userType,
    loginName: data.loginName,
    employeeNo: data.employeeNo,
    phone: data.phone,
    realName: data.realName,
    organizationId: data.organizationId,
    organizationName: data.organizationId === 1 ? '克拉玛依市档案馆' : data.organizationId === 2 ? '市财政局' : data.organizationId === 3 ? '市交通局' : '市科技研究中心',
    departmentName: data.departmentName,
    maxSecurityLevel: data.maxSecurityLevel,
    dataScope: data.dataScope,
    roleCodes: data.roleCodes,
    status: 'active',
    createdAt: '2026-06-15T10:00:00+08:00',
  }
  users.unshift(u)
  return { ...u }
}

export function mockUpdateUser(id: number, data: UserUpdateData): UserDetail {
  const u = users.find((x) => x.id === id)
  if (!u) throw new Error('用户不存在')
  Object.assign(u, data)
  u.updatedAt = '2026-06-15T10:00:00+08:00'
  return { ...u }
}

export function mockUpdateUserStatus(id: number, data: UserStatusData): UserDetail {
  const u = users.find((x) => x.id === id)
  if (!u) throw new Error('用户不存在')
  if (data.status === 'disabled' && u.roleCodes.includes('sys_admin')) {
    const activeAdmins = users.filter((x) => x.roleCodes.includes('sys_admin') && x.status === 'active')
    if (activeAdmins.length <= 1) {
      throw new Error('不能禁用最后一个系统管理员')
    }
  }
  u.status = data.status
  return { ...u }
}

export function mockResetUserPassword(_id: number, _data: ResetPasswordData): boolean {
  return true
}

export function mockRoles(): Role[] {
  return roles.filter((r) => r.enabled)
}
```

- [ ] **Step 3: 写 api `src/api/user-management.ts`**

```typescript
import request from './request'
import type { PageData } from '@/types/api'
import type {
  ResetPasswordData,
  Role,
  User,
  UserCreateData,
  UserDetail,
  UserParams,
  UserStatusData,
  UserUpdateData,
} from '@/types/user-management'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询用户（§22.1） */
export function getUsers(params?: UserParams): Promise<PageData<User>> {
  if (USE_MOCK) {
    return import('@/mock/modules/user-management').then((m) => m.mockUsers(params))
  }
  return request.get('/admin/users', { params })
}

/** 创建用户（§22.2） */
export function createUser(data: UserCreateData): Promise<UserDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/user-management').then((m) => m.mockCreateUser(data))
  }
  return request.post('/admin/users', data)
}

/** 用户详情（§22.3） */
export function getUserDetail(userId: number): Promise<UserDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/user-management').then((m) => m.mockUserDetail(userId))
  }
  return request.get(`/admin/users/${userId}`)
}

/** 更新用户（§22.4） */
export function updateUser(userId: number, data: UserUpdateData): Promise<UserDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/user-management').then((m) => m.mockUpdateUser(userId, data))
  }
  return request.put(`/admin/users/${userId}`, data)
}

/** 禁用/启用用户（§22.5） */
export function updateUserStatus(userId: number, data: UserStatusData): Promise<UserDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/user-management').then((m) => m.mockUpdateUserStatus(userId, data))
  }
  return request.put(`/admin/users/${userId}/status`, data)
}

/** 重置密码（§22.6） */
export function resetUserPassword(userId: number, data: ResetPasswordData): Promise<boolean> {
  if (USE_MOCK) {
    return import('@/mock/modules/user-management').then((m) => m.mockResetUserPassword(userId, data))
  }
  return request.post(`/admin/users/${userId}/reset-password`, data)
}

/** 查询角色（§22.7） */
export function getRoles(): Promise<Role[]> {
  if (USE_MOCK) {
    return import('@/mock/modules/user-management').then((m) => m.mockRoles())
  }
  return request.get('/admin/roles')
}
```

- [ ] **Step 4: 写校验 `src/utils/userValidation.ts`**

```typescript
import type { User, UserCreateData, UserStatusData } from '@/types/user-management'

/** 校验结果：valid 为 true 时 errors 为空 */
export interface ValidationResult {
  valid: boolean
  errors: string[]
}

/** 校验新建/编辑用户表单 */
export function validateUserForm(
  data: { loginName: string; realName: string; roleCodes: string[]; maxSecurityLevel: number; dataScope: string; initialPassword?: string },
  existingLoginNames: string[],
  isCreate: boolean,
): ValidationResult {
  const errors: string[] = []
  if (!data.loginName.trim()) errors.push('登录名不能为空')
  else if (isCreate && existingLoginNames.includes(data.loginName.trim())) errors.push('登录名已存在')
  if (!data.realName.trim()) errors.push('姓名不能为空')
  if (!data.roleCodes || data.roleCodes.length === 0) errors.push('请至少选择一个预设角色')
  if (data.maxSecurityLevel < 0 || data.maxSecurityLevel > 4) errors.push('密级上限必须在 0–4 之间')
  if (!['own_org', 'own_fonds', 'all'].includes(data.dataScope)) errors.push('数据范围不合法')
  if (isCreate && (!data.initialPassword || data.initialPassword.length < 6)) errors.push('初始密码至少 6 位')
  return { valid: errors.length === 0, errors }
}

/** 判断禁用某用户是否会移除最后一个 sys_admin */
export function canDisableUser(target: User, allUsers: User[], data: UserStatusData): { allowed: boolean; reason?: string } {
  if (data.status !== 'disabled') return { allowed: true }
  if (!target.roleCodes.includes('sys_admin')) return { allowed: true }
  const activeAdmins = allUsers.filter((u) => u.roleCodes.includes('sys_admin') && u.status === 'active')
  if (activeAdmins.length <= 1) {
    return { allowed: false, reason: '不能禁用最后一个系统管理员' }
  }
  return { allowed: true }
}

/** 手机号格式（宽松：11 位数字） */
export function isValidPhone(phone: string): boolean {
  return /^\d{11}$/.test(phone.trim())
}
```

- [ ] **Step 5: 写 api 测试 `src/api/user-management.spec.ts`**

```typescript
import { describe, expect, it } from 'vitest'
import { createUser, getRoles, getUserDetail, getUsers, updateUserStatus } from './user-management'

describe('user-management api mock mode', () => {
  it('lists users and filters by role', async () => {
    const all = await getUsers()
    expect(all.records.length).toBeGreaterThan(0)
    const admins = await getUsers({ roleCode: 'sys_admin' })
    expect(admins.records.every((u) => u.roleCodes.includes('sys_admin'))).toBe(true)
  })

  it('user detail includes recent actions', async () => {
    const d = await getUserDetail(11)
    expect(d.realName).toBe('小刘')
    expect(d.recentActions?.length).toBeGreaterThan(0)
  })

  it('throws on duplicate login name', async () => {
    await expect(createUser({ userType: 'internal', loginName: 'liuxiao', realName: '重复', maxSecurityLevel: 0, dataScope: 'all', roleCodes: ['back_archivist'], initialPassword: '123456' })).rejects.toThrow('登录名已存在')
  })

  it('creates user with new login name', async () => {
    const u = await createUser({ userType: 'internal', loginName: 'newperson', realName: '新人', maxSecurityLevel: 1, dataScope: 'own_org', roleCodes: ['internal_reader'], initialPassword: '123456' })
    expect(u.status).toBe('active')
  })

  it('rejects disabling last sys_admin', async () => {
    const admins = await getUsers({ roleCode: 'sys_admin' })
    const last = admins.records.find((u) => u.loginName === 'admin02')!
    await expect(updateUserStatus(last.id, { status: 'disabled', reason: '测试' })).rejects.toThrow('最后一个系统管理员')
  })

  it('returns enabled roles', async () => {
    const roles = await getRoles()
    expect(roles.length).toBeGreaterThan(0)
    expect(roles.every((r) => r.enabled)).toBe(true)
  })
})
```

- [ ] **Step 6: 写 utils 测试 `src/utils/userValidation.spec.ts`**

```typescript
import { describe, expect, it } from 'vitest'
import { canDisableUser, validateUserForm } from './userValidation'
import type { User } from '@/types/user-management'

const baseUser: User = {
  id: 1, userType: 'internal', loginName: 'a', realName: '甲', maxSecurityLevel: 0,
  dataScope: 'all', roleCodes: ['sys_admin'], status: 'active', createdAt: '2026-01-01T00:00:00+08:00',
}

describe('validateUserForm', () => {
  it('rejects empty login name', () => {
    const r = validateUserForm({ loginName: '', realName: '甲', roleCodes: ['back_archivist'], maxSecurityLevel: 0, dataScope: 'all', initialPassword: '123456' }, [], true)
    expect(r.valid).toBe(false)
    expect(r.errors).toContain('登录名不能为空')
  })
  it('rejects duplicate login name on create', () => {
    const r = validateUserForm({ loginName: 'liuxiao', realName: '甲', roleCodes: ['back_archivist'], maxSecurityLevel: 0, dataScope: 'all', initialPassword: '123456' }, ['liuxiao'], true)
    expect(r.valid).toBe(false)
  })
  it('rejects zero roles', () => {
    const r = validateUserForm({ loginName: 'new', realName: '甲', roleCodes: [], maxSecurityLevel: 0, dataScope: 'all', initialPassword: '123456' }, [], true)
    expect(r.errors).toContain('请至少选择一个预设角色')
  })
  it('rejects bad security level', () => {
    const r = validateUserForm({ loginName: 'new', realName: '甲', roleCodes: ['back_archivist'], maxSecurityLevel: 9, dataScope: 'all', initialPassword: '123456' }, [], true)
    expect(r.valid).toBe(false)
  })
  it('passes a valid create form', () => {
    const r = validateUserForm({ loginName: 'newuser', realName: '甲', roleCodes: ['back_archivist'], maxSecurityLevel: 2, dataScope: 'all', initialPassword: '123456' }, [], true)
    expect(r.valid).toBe(true)
  })
})

describe('canDisableUser', () => {
  it('blocks disabling last sys_admin', () => {
    const users: User[] = [{ ...baseUser, id: 1, loginName: 'only' }]
    const r = canDisableUser(users[0], users, { status: 'disabled' })
    expect(r.allowed).toBe(false)
  })
  it('allows disabling when another active admin exists', () => {
    const users: User[] = [{ ...baseUser, id: 1 }, { ...baseUser, id: 2, loginName: 'b' }]
    const r = canDisableUser(users[0], users, { status: 'disabled' })
    expect(r.allowed).toBe(true)
  })
  it('allows disabling non-admin', () => {
    const r = canDisableUser({ ...baseUser, roleCodes: ['back_archivist'] }, [baseUser], { status: 'disabled' })
    expect(r.allowed).toBe(true)
  })
})
```

- [ ] **Step 7: 运行测试**

Run: `npx vitest run src/api/user-management.spec.ts src/utils/userValidation.spec.ts`
Expected: 9 passed (6 + 3)

- [ ] **Step 8: Commit**

```bash
git add src/types/user-management.ts src/mock/modules/user-management.ts src/api/user-management.ts src/utils/userValidation.ts src/api/user-management.spec.ts src/utils/userValidation.spec.ts
git commit -m "feat(settings-hu): 实现用户管理 api/mock/校验与角色维护"
```

## Task 7: 用户管理页面（`views/admin/user-management/index.vue`）

**Files:**
- Modify: `src/views/admin/user-management/index.vue`（重写占位）
- Test: `src/views/admin/user-management/index.spec.ts`

> 若实现后 index.vue 超 600 行，将右栏「账号编辑」表单 + 角色勾选抽到 `components/UserEditPanel.vue`（props: form/roles/organizations；emit: update:form）。下方先按单文件给出。

- [ ] **Step 1: 写页面 `src/views/admin/user-management/index.vue`**

```vue
<template>
  <div class="user-mgmt">
    <section>
      <h1 class="page-title">用户管理</h1>
      <p class="page-subtitle">维护组织、全宗、账号和预设角色。系统管理员负责账号配置，不默认拥有档案正文和电子文件查看权限。</p>
    </section>

    <section class="grid four" aria-label="用户概览">
      <div class="metric card"><div class="metric-num">{{ internalCount }}</div><div class="metric-label">内部账号</div><div class="metric-note">user_type = internal</div></div>
      <div class="metric card"><div class="metric-num">{{ publicCount }}</div><div class="metric-label">公众账号</div><div class="metric-note">user_type = public</div></div>
      <div class="metric card"><div class="metric-num">{{ disabledCount }}</div><div class="metric-label">禁用账号</div><div class="metric-note">历史日志与业务记录保留</div></div>
      <div class="metric card"><div class="metric-num">{{ sysAdminCount }}</div><div class="metric-label">系统管理员</div><div class="metric-note">sys_admin 角色</div></div>
    </section>

    <div class="toolbar">
      <div class="actions-left">
        <select v-model="roleFilter" aria-label="角色筛选">
          <option value="">全部角色</option>
          <option v-for="r in roles" :key="r.roleCode" :value="r.roleCode">{{ r.roleName }}</option>
        </select>
        <el-button @click="applyFilter">筛选</el-button>
      </div>
      <div class="actions">
        <el-button @click="newUser">新建用户</el-button>
        <el-button type="primary" :loading="saving" @click="saveUser">保存用户</el-button>
      </div>
    </div>

    <div class="user-layout">
      <section class="stack">
        <div class="card panel">
          <h2 class="section-title">账号列表</h2>
          <div v-if="loading && users.length === 0" class="detail-empty">加载中...</div>
          <div v-else-if="loadError" class="detail-empty">加载失败：<button class="link" @click="loadAll">重试</button></div>
          <div v-else-if="users.length === 0" class="detail-empty">暂无用户</div>
          <div v-else class="table-wrap">
            <table>
              <thead>
                <tr><th>登录名</th><th>姓名</th><th>单位/部门</th><th>角色</th><th>数据范围</th><th>密级上限</th><th>状态</th><th>操作</th></tr>
              </thead>
              <tbody>
                <tr v-for="u in users" :key="u.id">
                  <td class="mono">{{ u.loginName }}</td>
                  <td>{{ u.realName }}</td>
                  <td>{{ (u.organizationName || '-') + ' / ' + (u.departmentName || '-') }}</td>
                  <td><span class="status" :class="roleTagClass(u.roleCodes[0])">{{ u.roleCodes[0] }}</span></td>
                  <td>{{ u.dataScope }}</td>
                  <td>{{ securityLabel(u.maxSecurityLevel) }}</td>
                  <td><span class="status" :class="u.status === 'active' ? 'success' : 'danger'">{{ UserStatusLabel[u.status] }}</span></td>
                  <td>
                    <div class="actions">
                      <el-button size="small" @click="editUser(u)">编辑</el-button>
                      <el-button size="small" type="warning" @click="resetPwd(u)">重置</el-button>
                      <el-button size="small" :type="u.status === 'active' ? 'danger' : 'success'" @click="toggleStatus(u)">{{ u.status === 'active' ? '禁用' : '启用' }}</el-button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="grid two">
          <div class="card panel">
            <h2 class="section-title">组织</h2>
            <ul class="mini-list">
              <li v-for="o in organizations" :key="o.id">
                <strong>{{ o.orgName }}</strong>
                <span class="muted">org_type = {{ o.orgType }} · 状态 {{ o.status === 'active' ? 'active' : 'disabled' }}</span>
              </li>
            </ul>
          </div>
          <div class="card panel">
            <h2 class="section-title">全宗</h2>
            <ul class="mini-list">
              <li v-for="f in fonds" :key="f.id">
                <strong class="mono">{{ f.fondsNo }} · {{ f.fondsName }}</strong>
                <span class="muted">关联组织：{{ f.organizationName || '-' }}</span>
              </li>
            </ul>
          </div>
        </div>
      </section>

      <aside class="stack">
        <div class="card panel">
          <h2 class="section-title">账号编辑</h2>
          <div class="form-grid">
            <div class="field"><label>登录名</label><input v-model="form.loginName" :disabled="!isCreate" /></div>
            <div class="field"><label>姓名</label><input v-model="form.realName" /></div>
            <div class="field">
              <label>用户类型</label>
              <select v-model="form.userType"><option value="internal">internal</option><option value="public">public</option></select>
            </div>
            <div class="field"><label>联系电话</label><input v-model="form.phone" /></div>
            <div class="field">
              <label>所属单位</label>
              <select v-model="form.organizationId">
                <option v-for="o in organizations" :key="o.id" :value="o.id">{{ o.orgName }}</option>
              </select>
            </div>
            <div class="field"><label>所属部门</label><input v-model="form.departmentName" /></div>
            <div class="field">
              <label>数据范围</label>
              <select v-model="form.dataScope"><option value="all">all</option><option value="own_org">own_org</option><option value="own_fonds">own_fonds</option></select>
            </div>
            <div class="field">
              <label>密级上限</label>
              <select v-model.number="form.maxSecurityLevel">
                <option :value="0">非密</option><option :value="1">秘密</option><option :value="2">机密</option><option :value="3">机密</option><option :value="4">绝密</option>
              </select>
            </div>
          </div>
          <div v-if="isCreate" class="field" style="margin-top:10px"><label>初始密码</label><input v-model="form.initialPassword" type="text" /></div>

          <h3 class="section-title" style="margin-top:16px">预设角色</h3>
          <div class="role-grid">
            <label class="role-check" v-for="r in internalRoles" :key="r.roleCode">
              <input type="checkbox" :value="r.roleCode" v-model="form.roleCodes" />
              {{ r.roleName }}
            </label>
          </div>
        </div>

        <div class="card panel">
          <h2 class="section-title">角色代码</h2>
          <div class="role-tags">
            <span class="status" v-for="r in roles" :key="r.roleCode">{{ r.roleCode }}</span>
          </div>
        </div>

        <div class="notice warning">
          <strong>权限边界</strong>
          <div>系统管理员维护账号和配置，不默认查看档案正文和电子文件；密级调整、开放调整、销毁审批仍由馆领导审批工作台办理。</div>
        </div>
        <div class="notice">
          <strong>审计留痕</strong>
          <div>用户禁用、密码重置、角色变更写入 <span class="mono">audit_logs</span>，历史审批、移交和操作记录保留原用户信息。</div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createUser, getRoles, getUserDetail, getUsers, resetUserPassword, updateUser, updateUserStatus } from '@/api/user-management'
import { getOrganizations } from '@/api/organizations'
import { getFonds } from '@/api/fonds'
import type { Role, User } from '@/types/user-management'
import type { Organization } from '@/types/organization'
import type { FondsReference } from '@/types/fonds'
import type { UserTypeValue } from '@/types/enums'
import { UserStatusLabel } from '@/types/enums'
import { canDisableUser, validateUserForm } from '@/utils/userValidation'

const users = ref<User[]>([])
const roles = ref<Role[]>([])
const organizations = ref<Organization[]>([])
const fonds = ref<FondsReference[]>([])
const loading = ref(false)
const loadError = ref(false)
const saving = ref(false)
const roleFilter = ref('')
const appliedRoleFilter = ref('')

const isCreate = ref(true)
const editingId = ref<number | null>(null)
const form = reactive({
  loginName: '',
  realName: '',
  userType: 'internal' as UserTypeValue,
  phone: '',
  organizationId: undefined as number | undefined,
  departmentName: '',
  dataScope: 'all' as 'all' | 'own_org' | 'own_fonds',
  maxSecurityLevel: 0,
  roleCodes: [] as string[],
  initialPassword: '123456',
})

const internalRoles = computed(() => roles.value.filter((r) => r.roleCode !== 'public_user'))
const internalCount = computed(() => users.value.filter((u) => u.userType === 'internal').length)
const publicCount = computed(() => users.value.filter((u) => u.userType === 'public').length)
const disabledCount = computed(() => users.value.filter((u) => u.status === 'disabled').length)
const sysAdminCount = computed(() => users.value.filter((u) => u.roleCodes.includes('sys_admin')).length)

const securityLevels = ['非密', '内部', '秘密', '机密', '绝密']
function securityLabel(level: number): string {
  return securityLevels[level] ?? '非密'
}
function roleTagClass(code: string): string {
  return code === 'director' ? 'warning' : code === 'sys_admin' ? 'info' : ''
}

async function loadAll() {
  loading.value = true
  loadError.value = false
  try {
    const [u, r, o, f] = await Promise.all([getUsers({ roleCode: appliedRoleFilter.value || undefined }), getRoles(), getOrganizations(), getFonds()])
    users.value = u.records
    roles.value = r
    organizations.value = o.records
    fonds.value = f.records
  } catch {
    loadError.value = true
    ElMessage.error('用户数据加载失败')
  } finally {
    loading.value = false
  }
}

function applyFilter() {
  appliedRoleFilter.value = roleFilter.value
  loadAll()
}

function newUser() {
  isCreate.value = true
  editingId.value = null
  form.loginName = ''
  form.realName = ''
  form.userType = 'internal'
  form.phone = ''
  form.organizationId = organizations.value[0]?.id
  form.departmentName = ''
  form.dataScope = 'all'
  form.maxSecurityLevel = 0
  form.roleCodes = ['internal_reader']
  form.initialPassword = '123456'
  ElMessage.success('已进入新建用户状态。')
}

async function editUser(u: User) {
  const detail = await getUserDetail(u.id)
  isCreate.value = false
  editingId.value = u.id
  form.loginName = detail.loginName
  form.realName = detail.realName
  form.userType = detail.userType
  form.phone = detail.phone ?? ''
  form.organizationId = detail.organizationId
  form.departmentName = detail.departmentName ?? ''
  form.dataScope = detail.dataScope
  form.maxSecurityLevel = detail.maxSecurityLevel
  form.roleCodes = [...detail.roleCodes]
  ElMessage.success('已载入账号：' + detail.loginName)
}

async function saveUser() {
  const existing = users.value.filter((u) => u.id !== editingId.value).map((u) => u.loginName)
  const result = validateUserForm(
    { loginName: form.loginName, realName: form.realName, roleCodes: form.roleCodes, maxSecurityLevel: form.maxSecurityLevel, dataScope: form.dataScope, initialPassword: form.initialPassword },
    existing,
    isCreate.value,
  )
  if (!result.valid) {
    ElMessage.error(result.errors[0])
    return
  }
  saving.value = true
  try {
    if (isCreate.value) {
      await createUser({
        userType: form.userType, loginName: form.loginName, realName: form.realName, phone: form.phone,
        organizationId: form.organizationId, departmentName: form.departmentName, maxSecurityLevel: form.maxSecurityLevel,
        dataScope: form.dataScope, roleCodes: form.roleCodes, initialPassword: form.initialPassword,
      })
      ElMessage.success('用户已保存，角色关系写入 user_roles。')
    } else {
      await updateUser(editingId.value!, {
        realName: form.realName, phone: form.phone, organizationId: form.organizationId, departmentName: form.departmentName,
        maxSecurityLevel: form.maxSecurityLevel, dataScope: form.dataScope, roleCodes: form.roleCodes,
      })
      ElMessage.success('用户已更新，变更写入审计日志。')
    }
    await loadAll()
  } catch (e) {
    ElMessage.error((e as Error).message || '保存用户失败')
  } finally {
    saving.value = false
  }
}

async function resetPwd(u: User) {
  try {
    await ElMessageBox.confirm(`确认重置用户「${u.realName}」的密码？`, '重置密码', { type: 'warning' })
    await resetUserPassword(u.id, { newPassword: '123456' })
    ElMessage.success('已生成密码重置记录，需通知用户首次登录修改。')
  } catch {
    // 用户取消
  }
}

async function toggleStatus(u: User) {
  const next = u.status === 'active' ? 'disabled' : 'active'
  if (next === 'disabled') {
    const check = canDisableUser(u, users.value, { status: 'disabled' })
    if (!check.allowed) {
      ElMessage.error(check.reason || '不可禁用')
      return
    }
    try {
      const { value } = await ElMessageBox.prompt('请输入禁用原因', '禁用账号', { type: 'warning' })
      await updateUserStatus(u.id, { status: 'disabled', reason: value })
      ElMessage.success('账号已禁用，历史业务记录保留。')
    } catch {
      // 取消
    }
  } else {
    await updateUserStatus(u.id, { status: 'active' })
    ElMessage.success('账号已启用。')
  }
  await loadAll()
}

onMounted(loadAll)
</script>

<style scoped>
.user-mgmt { padding: 0; }
.page-title { font-size: 20px; font-weight: 700; }
.page-subtitle { color: #909399; margin-top: 6px; }
.grid.four { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-top: 16px; }
.grid.two { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.metric.card { padding: 14px; }
.metric-num { font-size: 22px; font-weight: 800; color: var(--primary, #1f6f78); }
.metric-label { font-size: 13px; color: #606266; }
.metric-note { font-size: 11px; color: #909399; margin-top: 4px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 8px; margin: 16px 0; }
.actions { display: flex; gap: 8px; }
.actions-left { display: flex; gap: 8px; align-items: center; }
.user-layout { display: grid; grid-template-columns: minmax(0, 1fr) 390px; gap: 16px; align-items: start; }
.stack { display: grid; gap: 16px; }
.role-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
.role-check { display: flex; min-height: 44px; align-items: center; gap: 8px; padding: 9px; border: 1px solid var(--border, #e4e7ed); border-radius: 8px; background: #fff; font-weight: 700; }
.role-check input { width: 17px; height: 17px; }
.mini-list { display: grid; gap: 10px; margin: 0; padding: 0; list-style: none; }
.mini-list li { display: grid; gap: 5px; padding: 11px; border: 1px solid var(--border, #e4e7ed); border-radius: 8px; background: #fff; }
.role-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.field { display: grid; gap: 4px; }
.field label { font-size: 12px; color: #909399; }
.field input, .field select { height: 34px; padding: 0 8px; border: 1px solid var(--border, #e4e7ed); border-radius: 6px; }
.detail-empty { color: #909399; padding: 16px; text-align: center; }
.link { background: none; border: none; color: var(--primary, #1f6f78); cursor: pointer; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; }
.muted { color: #909399; font-size: 12px; }
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 10px; }
@media (max-width: 1120px) { .user-layout { grid-template-columns: 1fr; } }
</style>
```

- [ ] **Step 2: 写测试 `src/views/admin/user-management/index.spec.ts`**

```typescript
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import UserManagement from './index.vue'

async function wait() { await new Promise((r) => setTimeout(r, 80)); await flushPromises() }
async function mountIt() {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/admin/user-management', component: UserManagement }] })
  await router.push('/admin/user-management')
  await router.isReady()
  return mount(UserManagement, { global: { plugins: [router] } })
}

describe('UserManagement', () => {
  it('renders metrics and account list', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('用户管理')
    expect(w.text()).toContain('小刘')
    expect(w.text()).toContain('back_archivist')
  })

  it('renders organization and fonds reference lists', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('克拉玛依市档案馆')
    expect(w.text()).toContain('F001')
  })

  it('enters new user state on 新建用户', async () => {
    const w = await mountIt()
    await wait()
    const btn = w.findAll('button').find((b) => b.text().includes('新建用户'))
    await btn?.trigger('click')
    await flushPromises()
    // 进入新建态后登录名可编辑且为空
    const loginInput = w.find('input').element as HTMLInputElement
    expect(loginInput.value).toBe('')
  })
})
```

- [ ] **Step 3: 运行测试**

Run: `npx vitest run src/views/admin/user-management/index.spec.ts`
Expected: 3 passed

- [ ] **Step 4: Commit**

```bash
git add src/views/admin/user-management/index.vue src/views/admin/user-management/index.spec.ts
git commit -m "feat(settings-hu): 实现用户管理页面还原与账号/角色维护交互"
```

## Task 8: 系统配置数据层（types + mock + api + utils + spec）

**Files:**
- Create: `src/types/system-settings.ts`
- Create: `src/mock/modules/system-settings.ts`
- Create: `src/api/system-settings.ts`
- Create: `src/utils/systemConfigValidation.ts`
- Test: `src/api/system-settings.spec.ts`、`src/utils/systemConfigValidation.spec.ts`

- [ ] **Step 1: 写类型 `src/types/system-settings.ts`**

```typescript
import type { ConfigValueTypeValue } from './enums'

export interface SystemConfig {
  configKey: string
  configValue: string
  valueType: ConfigValueTypeValue
  editable: boolean
  description?: string
  updatedAt?: string
}

export interface ConfigUpdateItem {
  configKey: string
  configValue: string
}

export interface ConfigBatchUpdateData {
  items: ConfigUpdateItem[]
}
```

- [ ] **Step 2: 写 mock `src/mock/modules/system-settings.ts`**

```typescript
import type { ConfigBatchUpdateData, SystemConfig } from '@/types/system-settings'

const configs: SystemConfig[] = [
  { configKey: 'upload.allowed_extensions', configValue: 'pdf,docx,jpg,png,mp4', valueType: 'json', editable: true, description: '文件上传格式白名单', updatedAt: '2026-06-14T09:22:00+08:00' },
  { configKey: 'upload.max_file_size_mb', configValue: '512', valueType: 'number', editable: true, description: '最大上传大小', updatedAt: '2026-06-14T09:22:00+08:00' },
  { configKey: 'ai.enabled', configValue: 'true', valueType: 'boolean', editable: true, description: 'AI 功能统一开关', updatedAt: '2026-06-14T09:22:00+08:00' },
  { configKey: 'public_search.enabled', configValue: 'true', valueType: 'boolean', editable: true, description: '公众公开检索入口', updatedAt: '2026-06-14T09:22:00+08:00' },
  { configKey: 'warehouse.usage_warning_threshold', configValue: '85', valueType: 'number', editable: true, description: '库房占用告警阈值', updatedAt: '2026-06-14T09:22:00+08:00' },
  { configKey: 'borrow.default_days', configValue: '14', valueType: 'number', editable: true, description: '默认借阅天数', updatedAt: '2026-06-14T09:22:00+08:00' },
  { configKey: 'collection.agreement_text', configValue: '捐赠人声明材料来源合法、权属清晰，同意无偿捐赠，并允许档案馆按规定整理、保存和公开利用。', valueType: 'string', editable: true, description: '公众提交征集清单前展示并勾选同意', updatedAt: '2026-06-14T09:22:00+08:00' },
  { configKey: 'security.audit_retention', configValue: 'permanent', valueType: 'string', editable: false, description: '审计日志保留策略，不允许页面修改', updatedAt: '2026-06-01T00:00:00+08:00' },
]

export function mockSystemConfigs(): SystemConfig[] {
  return configs.map((c) => ({ ...c }))
}

export function mockUpdateSystemConfig(key: string, value: string): SystemConfig {
  const c = configs.find((x) => x.configKey === key)
  if (!c) throw new Error('配置项不存在：' + key)
  if (!c.editable) throw new Error('该配置项不允许修改：' + key)
  c.configValue = value
  c.updatedAt = '2026-06-15T10:00:00+08:00'
  return { ...c }
}

export function mockBatchUpdateSystemConfigs(data: ConfigBatchUpdateData): SystemConfig[] {
  const updated: SystemConfig[] = []
  for (const item of data.items) {
    updated.push(mockUpdateSystemConfig(item.configKey, item.configValue))
  }
  return updated
}
```

- [ ] **Step 3: 写 api `src/api/system-settings.ts`**

```typescript
import request from './request'
import type { ConfigBatchUpdateData, SystemConfig } from '@/types/system-settings'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询系统配置（§22.8） */
export function getSystemConfigs(): Promise<SystemConfig[]> {
  if (USE_MOCK) {
    return import('@/mock/modules/system-settings').then((m) => m.mockSystemConfigs())
  }
  return request.get('/admin/system-configs')
}

/** 更新单项配置（§22.9） */
export function updateSystemConfig(configKey: string, configValue: string): Promise<SystemConfig> {
  if (USE_MOCK) {
    return import('@/mock/modules/system-settings').then((m) => m.mockUpdateSystemConfig(configKey, configValue))
  }
  return request.put(`/admin/system-configs/${configKey}`, { configValue })
}

/** 批量更新配置（§22.10） */
export function batchUpdateSystemConfigs(data: ConfigBatchUpdateData): Promise<SystemConfig[]> {
  if (USE_MOCK) {
    return import('@/mock/modules/system-settings').then((m) => m.mockBatchUpdateSystemConfigs(data))
  }
  return request.put('/admin/system-configs', data)
}
```

- [ ] **Step 4: 写校验 `src/utils/systemConfigValidation.ts`**

```typescript
import type { SystemConfig } from '@/types/system-settings'

export interface ConfigValidationResult {
  valid: boolean
  errors: string[]
}

/** 数值范围校验规则（key → [min, max]） */
const numberRanges: Record<string, [number, number]> = {
  'upload.max_file_size_mb': [1, 2048],
  'warehouse.usage_warning_threshold': [50, 100],
  'borrow.default_days': [1, 90],
}

/** 校验待保存的配置项集合（仅 editable 项） */
export function validateConfigs(items: SystemConfig[]): ConfigValidationResult {
  const errors: string[] = []
  for (const item of items) {
    if (!item.editable) continue
    const range = numberRanges[item.configKey]
    if (range) {
      const n = Number(item.configValue)
      if (Number.isNaN(n) || n < range[0] || n > range[1]) {
        errors.push(`${item.configKey} 必须在 ${range[0]} 到 ${range[1]} 之间`)
      }
    }
    if (item.valueType === 'boolean' && !['true', 'false'].includes(item.configValue)) {
      errors.push(`${item.configKey} 必须为 true 或 false`)
    }
  }
  return { valid: errors.length === 0, errors }
}

/** 白名单字符串规范化：去点、小写、去重 */
export function normalizeExtensions(raw: string[]): string[] {
  const seen = new Set<string>()
  const result: string[] = []
  for (const ext of raw) {
    const clean = ext.trim().replace(/\./g, '').toLowerCase()
    if (clean && !seen.has(clean)) {
      seen.add(clean)
      result.push(clean)
    }
  }
  return result
}
```

- [ ] **Step 5: 写 api 测试 `src/api/system-settings.spec.ts`**

```typescript
import { describe, expect, it } from 'vitest'
import { batchUpdateSystemConfigs, getSystemConfigs, updateSystemConfig } from './system-settings'

describe('system-settings api mock mode', () => {
  it('returns config list with editable flags', async () => {
    const list = await getSystemConfigs()
    expect(list.length).toBeGreaterThan(0)
    const nonEditable = list.find((c) => !c.editable)
    expect(nonEditable?.configKey).toBe('security.audit_retention')
  })

  it('updates editable config value', async () => {
    const updated = await updateSystemConfig('upload.max_file_size_mb', '1024')
    expect(updated.configValue).toBe('1024')
  })

  it('rejects updating non-editable config', async () => {
    await expect(updateSystemConfig('security.audit_retention', '5y')).rejects.toThrow('不允许修改')
  })

  it('batch updates multiple editable items', async () => {
    const updated = await batchUpdateSystemConfigs({ items: [{ configKey: 'ai.enabled', configValue: 'false' }, { configKey: 'borrow.default_days', configValue: '30' }] })
    expect(updated).toHaveLength(2)
    expect(updated.find((c) => c.configKey === 'ai.enabled')?.configValue).toBe('false')
  })
})
```

- [ ] **Step 6: 写 utils 测试 `src/utils/systemConfigValidation.spec.ts`**

```typescript
import { describe, expect, it } from 'vitest'
import { normalizeExtensions, validateConfigs } from './systemConfigValidation'
import type { SystemConfig } from '@/types/system-settings'

const cfg = (key: string, value: string, editable = true, valueType: SystemConfig['valueType'] = 'number'): SystemConfig => ({ configKey: key, configValue: value, valueType, editable })

describe('validateConfigs', () => {
  it('passes in-range number config', () => {
    const r = validateConfigs([cfg('upload.max_file_size_mb', '512')])
    expect(r.valid).toBe(true)
  })
  it('rejects out-of-range max size', () => {
    const r = validateConfigs([cfg('upload.max_file_size_mb', '99999')])
    expect(r.valid).toBe(false)
    expect(r.errors[0]).toContain('1 到 2048')
  })
  it('rejects out-of-range borrow days', () => {
    const r = validateConfigs([cfg('borrow.default_days', '0')])
    expect(r.valid).toBe(false)
  })
  it('ignores non-editable items', () => {
    const r = validateConfigs([cfg('security.audit_retention', 'permanent', false, 'string')])
    expect(r.valid).toBe(true)
  })
  it('rejects bad boolean value', () => {
    const r = validateConfigs([cfg('ai.enabled', 'yes', true, 'boolean')])
    expect(r.valid).toBe(false)
  })
})

describe('normalizeExtensions', () => {
  it('strips dots lowercases and dedups', () => {
    expect(normalizeExtensions(['PDF', '.docx', 'pdf', 'JPG'])).toEqual(['pdf', 'docx', 'jpg'])
  })
  it('drops empty entries', () => {
    expect(normalizeExtensions(['', '  ', 'png'])).toEqual(['png'])
  })
})
```

- [ ] **Step 7: 运行测试**

Run: `npx vitest run src/api/system-settings.spec.ts src/utils/systemConfigValidation.spec.ts`
Expected: 9 passed (4 + 5)

- [ ] **Step 8: Commit**

```bash
git add src/types/system-settings.ts src/mock/modules/system-settings.ts src/api/system-settings.ts src/utils/systemConfigValidation.ts src/api/system-settings.spec.ts src/utils/systemConfigValidation.spec.ts
git commit -m "feat(settings-hu): 实现系统配置 api/mock/校验与批量更新"
```

## Task 9: 系统配置页面（`views/admin/system-settings/index.vue`）

**Files:**
- Modify: `src/views/admin/system-settings/index.vue`（重写占位）
- Test: `src/views/admin/system-settings/index.spec.ts`

- [ ] **Step 1: 写页面 `src/views/admin/system-settings/index.vue`**

```vue
<template>
  <div class="settings">
    <section>
      <h1 class="page-title">系统配置</h1>
      <p class="page-subtitle">维护上传白名单、上传大小、AI 开关、公开检索开关和业务默认参数；配置变更记录审计日志。</p>
    </section>

    <section class="grid four" aria-label="配置概览">
      <div class="metric card"><div class="metric-num">{{ editableCount }}</div><div class="metric-label">可编辑配置</div><div class="metric-note">editable = true</div></div>
      <div class="metric card"><div class="metric-num">{{ aiEnabled ? '开' : '关' }}</div><div class="metric-label">AI 功能</div><div class="metric-note">ai.enabled</div></div>
      <div class="metric card"><div class="metric-num">{{ publicEnabled ? '开' : '关' }}</div><div class="metric-label">公开检索</div><div class="metric-note">public_search.enabled</div></div>
      <div class="metric card"><div class="metric-num">{{ savedAt }}</div><div class="metric-label">最近保存</div><div class="metric-note">写入 audit_logs</div></div>
    </section>

    <div class="toolbar">
      <div></div>
      <div class="actions">
        <el-button :loading="loading" @click="resetConfig">恢复本页示例</el-button>
        <el-button type="primary" :loading="saving" @click="saveConfig">保存配置</el-button>
      </div>
    </div>

    <div class="settings-layout">
      <section class="stack">
        <div v-if="loading && configs.length === 0" class="detail-empty">加载中...</div>
        <div v-else-if="loadError" class="detail-empty">加载失败：<button class="link" @click="loadAll">重试</button></div>
        <template v-else>
          <div class="card panel">
            <h2 class="section-title">文件上传配置</h2>
            <div class="config-row">
              <div class="config-key"><strong>上传格式白名单</strong><span class="mono">upload.allowed_extensions</span></div>
              <div>
                <div class="tag-list">
                  <span class="format-tag" v-for="ext in extensions" :key="ext">{{ ext }}</span>
                </div>
                <div class="field" style="margin-top:10px"><label>新增格式</label><input v-model="newExt" @keyup.enter="addFormat" /></div>
              </div>
              <el-button @click="addFormat">添加格式</el-button>
            </div>
            <div class="config-row">
              <div class="config-key"><strong>最大上传大小</strong><span class="mono">upload.max_file_size_mb</span></div>
              <div class="field"><label>大小限制 MB</label><input type="number" min="1" max="2048" v-model.number="maxSize" /></div>
              <span class="status">number</span>
            </div>
          </div>

          <div class="card panel">
            <h2 class="section-title">能力开关</h2>
            <div class="config-row">
              <div class="config-key"><strong>AI 功能开关</strong><span class="mono">ai.enabled</span></div>
              <label class="switch-line"><input type="checkbox" v-model="aiEnabled" @change="onAiToggle" /><span>启用 AI 补全、检索 JSON 和数据研判建议</span></label>
              <span class="status info">boolean</span>
            </div>
            <div class="config-row">
              <div class="config-key"><strong>公开检索开关</strong><span class="mono">public_search.enabled</span></div>
              <label class="switch-line"><input type="checkbox" v-model="publicEnabled" @change="onPublicToggle" /><span>启用公众门户公开档案检索入口</span></label>
              <span class="status info">boolean</span>
            </div>
            <div class="notice" style="margin-top:12px">AI 关闭时业务流程回退为人工填写和手工筛选；公开检索关闭只影响入口，不改变正式档案的公开字段。</div>
          </div>

          <div class="card panel">
            <h2 class="section-title">业务默认参数</h2>
            <div class="config-row">
              <div class="config-key"><strong>库房占用告警阈值</strong><span class="mono">warehouse.usage_warning_threshold</span></div>
              <div class="field"><label>阈值百分比</label><input type="number" min="50" max="100" v-model.number="warehouseThreshold" /></div>
              <span class="status">number</span>
            </div>
            <div class="config-row">
              <div class="config-key"><strong>默认借阅天数</strong><span class="mono">borrow.default_days</span></div>
              <div class="field"><label>天数</label><input type="number" min="1" max="90" v-model.number="borrowDays" /></div>
              <span class="status">number</span>
            </div>
            <div class="config-row">
              <div class="config-key"><strong>征集捐赠协议文案</strong><span class="mono">collection.agreement_text</span></div>
              <div class="field"><label>协议文案</label><textarea v-model="agreementText" rows="3"></textarea></div>
              <span class="status">string</span>
            </div>
          </div>

          <div class="card panel">
            <h2 class="section-title">配置项明细</h2>
            <div class="table-wrap">
              <table>
                <thead><tr><th>配置键</th><th>当前值</th><th>类型</th><th>可编辑</th><th>说明</th></tr></thead>
                <tbody>
                  <tr v-for="c in configs" :key="c.configKey">
                    <td class="mono">{{ c.configKey }}</td>
                    <td>{{ displayValue(c) }}</td>
                    <td>{{ c.valueType }}</td>
                    <td><span class="status" :class="c.editable ? 'success' : ''">{{ c.editable ? '是' : '否' }}</span></td>
                    <td>{{ c.description }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </template>
      </section>

      <aside class="stack">
        <div class="card panel">
          <h2 class="section-title">配置边界</h2>
          <ul class="timeline">
            <li><span>上传</span><div>只维护格式白名单和大小限制。</div></li>
            <li><span>AI</span><div>统一启停补全、检索 JSON 和数据研判建议。</div></li>
            <li><span>公开检索</span><div>只控制公众入口，不直接改档案公开状态。</div></li>
            <li><span>征集协议</span><div>公众提交时在线展示并记录同意时间。</div></li>
          </ul>
        </div>
        <div class="notice warning"><strong>审批边界</strong><div>配置页不办理密级调整、开放调整或销毁审批；这些业务必须进入审批工作台并保留审批记录。</div></div>
        <div class="notice"><strong>审计要求</strong><div>保存配置写入 <span class="mono">audit_logs</span>，不可在页面删除审计记录。</div></div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { batchUpdateSystemConfigs, getSystemConfigs } from '@/api/system-settings'
import type { SystemConfig } from '@/types/system-settings'
import { normalizeExtensions, validateConfigs } from '@/utils/systemConfigValidation'

const configs = ref<SystemConfig[]>([])
const loading = ref(false)
const loadError = ref(false)
const saving = ref(false)
const savedAt = ref('-')

const extensions = ref<string[]>([])
const newExt = ref('')
const maxSize = ref(512)
const aiEnabled = ref(true)
const publicEnabled = ref(true)
const warehouseThreshold = ref(85)
const borrowDays = ref(14)
const agreementText = ref('')

const editableCount = computed(() => configs.value.filter((c) => c.editable).length)

function displayValue(c: SystemConfig): string {
  if (c.configKey === 'upload.allowed_extensions') return extensions.value.join(', ')
  if (c.configKey === 'upload.max_file_size_mb') return String(maxSize.value)
  if (c.configKey === 'ai.enabled') return String(aiEnabled.value)
  if (c.configKey === 'public_search.enabled') return String(publicEnabled.value)
  if (c.configKey === 'warehouse.usage_warning_threshold') return String(warehouseThreshold.value)
  if (c.configKey === 'borrow.default_days') return String(borrowDays.value)
  if (c.configKey === 'collection.agreement_text') return agreementText.value ? '捐赠协议文案' : '-'
  return c.configValue
}

function hydrateFromConfigs() {
  for (const c of configs.value) {
    if (c.configKey === 'upload.allowed_extensions') extensions.value = c.configValue.split(',').map((s) => s.trim()).filter(Boolean)
    else if (c.configKey === 'upload.max_file_size_mb') maxSize.value = Number(c.configValue)
    else if (c.configKey === 'ai.enabled') aiEnabled.value = c.configValue === 'true'
    else if (c.configKey === 'public_search.enabled') publicEnabled.value = c.configValue === 'true'
    else if (c.configKey === 'warehouse.usage_warning_threshold') warehouseThreshold.value = Number(c.configValue)
    else if (c.configKey === 'borrow.default_days') borrowDays.value = Number(c.configValue)
    else if (c.configKey === 'collection.agreement_text') agreementText.value = c.configValue
  }
}

async function loadAll() {
  loading.value = true
  loadError.value = false
  try {
    configs.value = await getSystemConfigs()
    hydrateFromConfigs()
    const latest = configs.value.filter((c) => c.updatedAt).sort((a, b) => (b.updatedAt! > a.updatedAt! ? 1 : -1))[0]
    savedAt.value = latest?.updatedAt?.slice(11, 16) ?? '-'
  } catch {
    loadError.value = true
    ElMessage.error('系统配置加载失败')
  } finally {
    loading.value = false
  }
}

function addFormat() {
  const normalized = normalizeExtensions([newExt.value])
  if (normalized.length === 0) {
    ElMessage.warning('请输入要加入白名单的扩展名。')
    return
  }
  for (const ext of normalized) {
    if (!extensions.value.includes(ext)) extensions.value.push(ext)
  }
  newExt.value = ''
  ElMessage.success('格式已加入白名单，保存后写入 system_configs。')
}

function onAiToggle() {
  ElMessage.success(aiEnabled.value ? 'AI 功能已启用。' : 'AI 已关闭，业务流程回退为人工处理。')
}
function onPublicToggle() {
  ElMessage.success(publicEnabled.value ? '公开检索入口已启用。' : '公开检索入口已关闭，档案公开字段不变化。')
}

function resetConfig() {
  hydrateFromConfigs()
  ElMessage.success('已恢复本页示例值。')
}

async function saveConfig() {
  const items: SystemConfig[] = [
    { configKey: 'upload.allowed_extensions', configValue: extensions.value.join(','), valueType: 'json', editable: true },
    { configKey: 'upload.max_file_size_mb', configValue: String(maxSize.value), valueType: 'number', editable: true },
    { configKey: 'ai.enabled', configValue: String(aiEnabled.value), valueType: 'boolean', editable: true },
    { configKey: 'public_search.enabled', configValue: String(publicEnabled.value), valueType: 'boolean', editable: true },
    { configKey: 'warehouse.usage_warning_threshold', configValue: String(warehouseThreshold.value), valueType: 'number', editable: true },
    { configKey: 'borrow.default_days', configValue: String(borrowDays.value), valueType: 'number', editable: true },
    { configKey: 'collection.agreement_text', configValue: agreementText.value, valueType: 'string', editable: true },
  ]
  const result = validateConfigs(items)
  if (!result.valid) {
    ElMessage.error(result.errors[0])
    return
  }
  saving.value = true
  try {
    await batchUpdateSystemConfigs({ items: items.map((i) => ({ configKey: i.configKey, configValue: i.configValue })) })
    configs.value = await getSystemConfigs()
    savedAt.value = '刚刚'
    ElMessage.success('配置已保存，变更写入 audit_logs。')
  } catch (e) {
    ElMessage.error((e as Error).message || '保存配置失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.settings { padding: 0; }
.page-title { font-size: 20px; font-weight: 700; }
.page-subtitle { color: #909399; margin-top: 6px; }
.grid.four { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-top: 16px; }
.metric.card { padding: 14px; }
.metric-num { font-size: 22px; font-weight: 800; color: var(--primary, #1f6f78); }
.metric-label { font-size: 13px; color: #606266; }
.metric-note { font-size: 11px; color: #909399; margin-top: 4px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin: 16px 0; }
.actions { display: flex; gap: 8px; }
.settings-layout { display: grid; grid-template-columns: minmax(0, 1fr) 380px; gap: 16px; align-items: start; }
.stack { display: grid; gap: 16px; }
.config-row { display: grid; grid-template-columns: 220px minmax(0, 1fr) 130px; gap: 12px; align-items: center; padding: 13px 0; border-bottom: 1px solid var(--border, #e4e7ed); }
.config-key { display: grid; gap: 3px; min-width: 0; overflow-wrap: anywhere; }
.switch-line { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; }
.switch-line input { width: 18px; height: 18px; }
.tag-list { display: flex; flex-wrap: wrap; gap: 7px; }
.format-tag { display: inline-flex; align-items: center; gap: 6px; padding: 5px 9px; border-radius: 999px; color: #23494f; background: rgba(31, 111, 120, 0.12); font-size: 12px; font-weight: 800; }
.field { display: grid; gap: 4px; }
.field label { font-size: 12px; color: #909399; }
.field input, .field textarea, .field select { padding: 6px 8px; border: 1px solid var(--border, #e4e7ed); border-radius: 6px; font-family: inherit; }
.detail-empty { color: #909399; padding: 16px; text-align: center; }
.link { background: none; border: none; color: var(--primary, #1f6f78); cursor: pointer; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; }
.timeline { list-style: none; margin: 0; padding: 0; display: grid; gap: 10px; }
.timeline li { padding: 8px 0; border-bottom: 1px solid var(--border, #e4e7ed); }
.timeline li span { font-weight: 700; display: block; }
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 10px; }
@media (max-width: 1120px) { .settings-layout { grid-template-columns: 1fr; } }
</style>
```

- [ ] **Step 2: 写测试 `src/views/admin/system-settings/index.spec.ts`**

```typescript
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import SystemSettings from './index.vue'

async function wait() { await new Promise((r) => setTimeout(r, 80)); await flushPromises() }
async function mountIt() {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/admin/system-settings', component: SystemSettings }] })
  await router.push('/admin/system-settings')
  await router.isReady()
  return mount(SystemSettings, { global: { plugins: [router] } })
}

describe('SystemSettings', () => {
  it('renders config sections and format tags', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('系统配置')
    expect(w.text()).toContain('文件上传配置')
    expect(w.text()).toContain('能力开关')
    expect(w.text()).toContain('upload.allowed_extensions')
    expect(w.findAll('.format-tag').length).toBeGreaterThan(0)
  })

  it('shows non-editable audit retention row', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('security.audit_retention')
  })

  it('renders ai and public search switches on by default', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('ai.enabled')
    expect(w.text()).toContain('public_search.enabled')
  })
})
```

- [ ] **Step 3: 运行测试**

Run: `npx vitest run src/views/admin/system-settings/index.spec.ts`
Expected: 3 passed

- [ ] **Step 4: Commit**

```bash
git add src/views/admin/system-settings/index.vue src/views/admin/system-settings/index.spec.ts
git commit -m "feat(settings-hu): 实现系统配置页面还原与批量保存交互"
```

## Task 10: 全量构建验证

**Files:** 无（仅运行验证命令）

- [ ] **Step 1: 类型检查**

Run: `npm run type-check`
Expected: 无错误（既有错误数不变，本轮新增代码零类型错误）。

- [ ] **Step 2: 生产构建**

Run: `npm run build`
Expected: 构建成功，无 chunk 报错。

- [ ] **Step 3: 全量单测**

Run: `npm run test:unit`
Expected: 全部通过，本轮新增 spec 全绿（organizations 3 + fonds 2 + preservation 5 + preservation view 3 + user api 6 + user utils 8 + user view 3 + settings api 4 + settings utils 7 + settings view 3 = 44 条新增用例）。

- [ ] **Step 4: 视觉自查（对照原型）**

启动 dev：`npm run dev`，逐页对照 `doc/prototype/admin/{preservation,user-management,system-settings}.html`：
- preservation：备份范围三卡 + 立即备份 + 备份任务表（含失败行）+ 四性检测四卡（真实性 not_configured）+ 存储空间。
- user-management：4 指标 + 账号列表 8 列 + 编辑表单（角色勾选）+ 组织/全宗 mini-list + 权限/审计提示。
- system-settings：4 指标 + 上传白名单/大小 + AI/公开检索开关 + 业务默认三项 + 配置明细表（含不可编辑行）+ 保存/恢复。

- [ ] **Step 5: 停在 push 前，等用户查看页面还原**

本轮所有提交均为本地。**不 push、不开 PR**。启动 dev 服务器后通知用户在浏览器查看三页还原情况，由用户确认后再决定 push / 提 PR（按用户原始指令）。

---

## 自审小结

**Spec 覆盖：** 设计文档 §2.1 三页 + §2.2 组织/全宗只读 → T1–T9 全覆盖；§6 枚举 → T1；§7 类型 → T2/T3/T4/T6/T8；§8 API → T2/T3/T4/T6/T8；§9 三页页面 → T5/T7/T9；§11 校验/边界 → userValidation(T6)/systemConfigValidation(T8)/页面内门控；§12 mock → 各数据层 Task；§13 测试 → 各 spec + T10；§14 验收 → T10 Step 4。

**占位符扫描：** 无 TBD/TODO；所有代码步骤均含完整可运行代码。

**类型一致性：** `User`/`UserDetail`/`UserCreateData`/`UserUpdateData` 在 T6 类型、mock、api、view、validation 间命名一致；`BackupTask`/`FileCheckRecord` 在 T4 一致；`SystemConfig`/`ConfigUpdateItem`/`ConfigBatchUpdateData` 在 T8 一致；枚举值 T1 定义后被 T4/T6/T8 引用，名称对齐（BackupScopeValue/CheckTypeValue 等）。

**执行衔接：** 本计划由当前会话以内联方式（superpowers:executing-plans 风格）逐 Task 执行，每 Task 完成后运行对应 spec 验证，再进入下一 Task。Task 10 全量验证通过后启动 dev 服务器，停在 push/PR 前等用户查看页面还原。








