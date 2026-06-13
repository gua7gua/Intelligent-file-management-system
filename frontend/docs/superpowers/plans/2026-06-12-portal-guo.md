# Portal Guo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the 06-11 to 06-14 portal-side frontend work for Guo Yikun: transfer portal, public portal, public registration, and forgot-password, with pages closely restored from the approved HTML prototypes.

**Architecture:** Add typed API/mock modules for transfer and public portal workflows, keep components calling API functions only, and keep state local to each page. Vue pages must restore the corresponding `doc/prototype/` HTML structure and styling first, then add missing API fields, loading/empty/error states, and validation inside the existing prototype sections.

**Tech Stack:** Vue 3 + TypeScript + Vue Router + Pinia + Axios + Element Plus + Vite + vue-tsc + Vitest + Vue Test Utils

**Design Spec:** `frontend/docs/superpowers/specs/2026-06-12-portal-guo-design.md`

---

## File Structure

Create:

- `frontend/src/types/transfer.ts` — transfer dashboard, batch, item, form, and validation types.
- `frontend/src/types/public.ts` — public archive, public home, collection, overview, registration, and reset-password types.
- `frontend/src/utils/fileParser.ts` — pure browser `File` metadata parser; never uploads files.
- `frontend/src/utils/transferValidation.ts` — pure transfer batch validation rules.
- `frontend/src/utils/publicValidation.ts` — pure public registration, forgot-password, and collection validation rules.
- `frontend/src/api/transfer.ts` — transfer portal API functions with `VITE_USE_MOCK`.
- `frontend/src/api/public.ts` — public portal API functions with `VITE_USE_MOCK`.
- `frontend/src/mock/modules/transfer.ts` — transfer dashboard/list/detail mock data and mutable draft helpers.
- `frontend/src/mock/modules/public.ts` — public home/search/collection/overview/auth mock data and helpers.
- `frontend/src/views/public/forgot-password/index.vue` — forgot-password page restored from `doc/prototype/public/forgot-password.html`.
- `frontend/src/test/setup.ts` — Vitest DOM setup.
- `frontend/src/**/*.spec.ts` and `frontend/src/**/*.spec.ts` files listed in tasks below.

Modify:

- `frontend/package.json` — add `test:unit` script and test dependencies.
- `frontend/vite.config.ts` — add Vitest config.
- `frontend/tsconfig.app.json` — include spec and setup files.
- `frontend/src/api/auth.ts` — add public SMS/register/reset-password API functions.
- `frontend/src/mock/index.ts` — export transfer/public mock modules.
- `frontend/src/router/routes/public.ts` — add `/public/forgot-password`.
- Existing placeholder views:
  - `frontend/src/views/transfer/overview/index.vue`
  - `frontend/src/views/transfer/transfer-list/index.vue`
  - `frontend/src/views/public/index/index.vue`
  - `frontend/src/views/public/search/index.vue`
  - `frontend/src/views/public/collection/index.vue`
  - `frontend/src/views/public/overview/index.vue`
  - `frontend/src/views/public/register/index.vue`
- `frontend/src/views/login/index.vue` — verify existing register/forgot links point to public routes; adjust only if needed.

Do not modify:

- Hu Ying's admin acceptance pages except if TypeScript import paths require no-op compatibility checks.
- Internal portal pages; they are explicitly outside this round.

## Prototype Restoration Rule

Every page task must start by reading the exact prototype HTML, prototype design doc, and prototype acceptance doc named in the task. The Vue component should preserve the prototype's content-area structure, CSS class names, page sections, visual hierarchy, action grouping, status labels, and major copy. Add fields from `doc/接口文档.md` only inside the closest existing prototype section. Do not replace the prototype with a new Element Plus layout.

## Task 1: Test Infrastructure

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/vite.config.ts`
- Modify: `frontend/tsconfig.app.json`
- Create: `frontend/src/test/setup.ts`

- [ ] **Step 1: Add the failing test command expectation**

Run:

```bash
cd frontend
npm run test:unit -- --run
```

Expected: FAIL with `Missing script: "test:unit"` or Vitest not found. This confirms the test harness is not yet configured.

- [ ] **Step 2: Install test dependencies**

Run:

```bash
cd frontend
npm install -D vitest @vue/test-utils jsdom
```

Expected: dependencies are added to `package.json` and `package-lock.json`.

- [ ] **Step 3: Update `frontend/package.json` scripts**

Add this script:

```json
"test:unit": "vitest"
```

Keep existing scripts:

```json
"dev": "vite",
"build": "vue-tsc -b && vite build",
"preview": "vite preview"
```

- [ ] **Step 4: Update `frontend/vite.config.ts`**

Replace the file with:

```typescript
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
  },
})
```

- [ ] **Step 5: Create `frontend/src/test/setup.ts`**

```typescript
import { config } from '@vue/test-utils'

config.global.stubs = {
  RouterLink: {
    props: ['to'],
    template: `<a :href="typeof to === 'string' ? to : to.path"><slot /></a>`,
  },
}
```

- [ ] **Step 6: Update `frontend/tsconfig.app.json` include**

Change `include` to:

```json
"include": ["env.d.ts", "src/**/*.ts", "src/**/*.tsx", "src/**/*.vue", "src/**/*.spec.ts"]
```

- [ ] **Step 7: Verify test harness**

Run:

```bash
cd frontend
npm run test:unit -- --run
```

Expected: PASS with no test files or reports no tests found according to Vitest version. If Vitest exits nonzero because no tests exist, proceed after Task 2 adds tests.

- [ ] **Step 8: Commit**

```bash
git add frontend/package.json frontend/package-lock.json frontend/vite.config.ts frontend/tsconfig.app.json frontend/src/test/setup.ts
git commit -m "test(portal-guo): 添加前端单元测试基础"
```

## Task 2: Shared Pure Utilities

**Files:**
- Create: `frontend/src/utils/fileParser.ts`
- Create: `frontend/src/utils/fileParser.spec.ts`
- Create: `frontend/src/utils/transferValidation.ts`
- Create: `frontend/src/utils/transferValidation.spec.ts`
- Create: `frontend/src/utils/publicValidation.ts`
- Create: `frontend/src/utils/publicValidation.spec.ts`

- [ ] **Step 1: Write failing tests for local file parsing**

Create `frontend/src/utils/fileParser.spec.ts`:

```typescript
import { describe, expect, it } from 'vitest'
import { parseLocalFiles } from './fileParser'

describe('parseLocalFiles', () => {
  it('extracts filename, title, extension, and size without uploading file content', () => {
    const files = [
      new File(['demo'], '2025Q1-accounting-vouchers.pdf', { type: 'application/pdf' }),
      new File(['photo'], 'family.photos.1998.JPG', { type: 'image/jpeg' }),
    ]

    const result = parseLocalFiles(files)

    expect(result).toEqual([
      {
        inputTitle: '2025Q1-accounting-vouchers',
        expectedFilename: '2025Q1-accounting-vouchers.pdf',
        electronicFormat: 'PDF',
        localFileSize: 4,
      },
      {
        inputTitle: 'family.photos.1998',
        expectedFilename: 'family.photos.1998.JPG',
        electronicFormat: 'JPG',
        localFileSize: 5,
      },
    ])
  })
})
```

- [ ] **Step 2: Run parser test and verify RED**

Run:

```bash
cd frontend
npm run test:unit -- src/utils/fileParser.spec.ts --run
```

Expected: FAIL because `fileParser.ts` does not exist.

- [ ] **Step 3: Implement `fileParser.ts`**

```typescript
export interface ParsedLocalFile {
  inputTitle: string
  expectedFilename: string
  electronicFormat: string
  localFileSize: number
}

export function parseLocalFiles(files: File[]): ParsedLocalFile[] {
  return files.map((file) => {
    const lastDot = file.name.lastIndexOf('.')
    const title = lastDot > 0 ? file.name.slice(0, lastDot) : file.name
    const extension = lastDot > 0 ? file.name.slice(lastDot + 1).toUpperCase() : ''

    return {
      inputTitle: title,
      expectedFilename: file.name,
      electronicFormat: extension,
      localFileSize: file.size,
    }
  })
}
```

- [ ] **Step 4: Verify parser test GREEN**

Run:

```bash
cd frontend
npm run test:unit -- src/utils/fileParser.spec.ts --run
```

Expected: PASS.

- [ ] **Step 5: Write failing tests for transfer validation**

Create `frontend/src/utils/transferValidation.spec.ts`:

```typescript
import { describe, expect, it } from 'vitest'
import { validateTransferDraft } from './transferValidation'

describe('validateTransferDraft', () => {
  it('reports missing batch fields and item carrier status', () => {
    const errors = validateTransferDraft({
      title: '',
      departmentName: '',
      contactPhone: '',
      archiveYear: undefined,
      expectedTransferDate: '',
      items: [
        {
          inputTitle: '2025 年第一季度会计凭证',
          retentionPeriod: '30y',
          carrierStatus: '',
          securityLevel: 1,
          openStatus: 'closed',
          allowDigitization: true,
        },
      ],
    })

    expect(errors).toContain('请填写清单标题。')
    expect(errors).toContain('请填写移交部门。')
    expect(errors).toContain('请填写联系电话。')
    expect(errors).toContain('请选择档案所属年度。')
    expect(errors).toContain('请选择预计移交日期。')
    expect(errors).toContain('第 1 条请选择载体状态。')
  })

  it('reports conflict when classified item is marked open', () => {
    const errors = validateTransferDraft({
      title: '2025 年度财政会计档案移交清单',
      departmentName: '财务处',
      contactPhone: '13800000003',
      archiveYear: 2025,
      expectedTransferDate: '2026-05-20',
      items: [
        {
          inputTitle: '涉密会议记录',
          retentionPeriod: '30y',
          carrierStatus: 'paper',
          securityLevel: 2,
          openStatus: 'open',
          allowDigitization: false,
        },
      ],
    })

    expect(errors).toEqual(['第 1 条涉密档案不能设置为公开。'])
  })
})
```

- [ ] **Step 6: Run transfer validation test and verify RED**

Run:

```bash
cd frontend
npm run test:unit -- src/utils/transferValidation.spec.ts --run
```

Expected: FAIL because `transferValidation.ts` does not exist.

- [ ] **Step 7: Implement `transferValidation.ts`**

```typescript
interface TransferDraftItem {
  inputTitle: string
  retentionPeriod: string
  carrierStatus: string
  securityLevel: number
  openStatus: string
  allowDigitization: boolean
}

interface TransferDraft {
  title: string
  departmentName: string
  contactPhone: string
  archiveYear?: number
  expectedTransferDate: string
  items: TransferDraftItem[]
}

export function validateTransferDraft(draft: TransferDraft): string[] {
  const errors: string[] = []

  if (!draft.title.trim()) errors.push('请填写清单标题。')
  if (!draft.departmentName.trim()) errors.push('请填写移交部门。')
  if (!draft.contactPhone.trim()) errors.push('请填写联系电话。')
  if (!draft.archiveYear) errors.push('请选择档案所属年度。')
  if (!draft.expectedTransferDate.trim()) errors.push('请选择预计移交日期。')
  if (draft.items.length === 0) errors.push('请至少添加一条清单条目。')

  draft.items.forEach((item, index) => {
    const row = index + 1
    if (!item.inputTitle.trim()) errors.push(`第 ${row} 条请填写档案标题。`)
    if (!item.retentionPeriod) errors.push(`第 ${row} 条请选择保管期限。`)
    if (!item.carrierStatus) errors.push(`第 ${row} 条请选择载体状态。`)
    if (item.securityLevel > 0 && item.openStatus === 'open') {
      errors.push(`第 ${row} 条涉密档案不能设置为公开。`)
    }
  })

  return errors
}
```

- [ ] **Step 8: Verify transfer validation GREEN**

Run:

```bash
cd frontend
npm run test:unit -- src/utils/transferValidation.spec.ts --run
```

Expected: PASS.

- [ ] **Step 9: Write failing tests for public validation**

Create `frontend/src/utils/publicValidation.spec.ts`:

```typescript
import { describe, expect, it } from 'vitest'
import {
  validateCollectionDraft,
  validatePublicRegister,
  validateResetPassword,
} from './publicValidation'

describe('publicValidation', () => {
  it('validates public registration fields', () => {
    expect(
      validatePublicRegister({
        realName: '',
        phone: '123',
        smsCode: '',
        password: 'Public@2026',
        confirmPassword: 'Public@2027',
        accepted: false,
      }),
    ).toEqual([
      '请填写姓名。',
      '请输入有效的 11 位手机号。',
      '请填写短信验证码。',
      '两次密码不一致。',
      '请确认公众账号使用说明。',
    ])
  })

  it('validates reset password fields', () => {
    expect(
      validateResetPassword({
        phone: '13800000005',
        smsCode: '',
        newPassword: 'Public@2026',
        confirmPassword: 'Public@2027',
      }),
    ).toEqual(['请填写短信验证码。', '两次新密码不一致。'])
  })

  it('requires collection agreement and at least one item', () => {
    expect(
      validateCollectionDraft({
        title: '',
        donorName: '',
        donorPhone: 'abc',
        agreementAccepted: false,
        items: [],
      }),
    ).toEqual([
      '请填写清单标题。',
      '请填写捐赠人。',
      '请输入有效的联系电话。',
      '请至少添加一条征集条目。',
      '请勾选在线捐赠协议。',
    ])
  })
})
```

- [ ] **Step 10: Run public validation test and verify RED**

Run:

```bash
cd frontend
npm run test:unit -- src/utils/publicValidation.spec.ts --run
```

Expected: FAIL because `publicValidation.ts` does not exist.

- [ ] **Step 11: Implement `publicValidation.ts`**

```typescript
const phonePattern = /^1\d{10}$/

interface RegisterForm {
  realName: string
  phone: string
  smsCode: string
  password: string
  confirmPassword: string
  accepted: boolean
}

interface ResetPasswordForm {
  phone: string
  smsCode: string
  newPassword: string
  confirmPassword: string
}

interface CollectionDraft {
  title: string
  donorName: string
  donorPhone: string
  agreementAccepted: boolean
  items: unknown[]
}

export function validatePublicRegister(form: RegisterForm): string[] {
  const errors: string[] = []
  if (!form.realName.trim()) errors.push('请填写姓名。')
  if (!phonePattern.test(form.phone)) errors.push('请输入有效的 11 位手机号。')
  if (!form.smsCode.trim()) errors.push('请填写短信验证码。')
  if (form.password !== form.confirmPassword) errors.push('两次密码不一致。')
  if (!form.accepted) errors.push('请确认公众账号使用说明。')
  return errors
}

export function validateResetPassword(form: ResetPasswordForm): string[] {
  const errors: string[] = []
  if (!phonePattern.test(form.phone)) errors.push('请输入有效的公众账号手机号。')
  if (!form.smsCode.trim()) errors.push('请填写短信验证码。')
  if (form.newPassword !== form.confirmPassword) errors.push('两次新密码不一致。')
  return errors
}

export function validateCollectionDraft(form: CollectionDraft): string[] {
  const errors: string[] = []
  if (!form.title.trim()) errors.push('请填写清单标题。')
  if (!form.donorName.trim()) errors.push('请填写捐赠人。')
  if (!phonePattern.test(form.donorPhone)) errors.push('请输入有效的联系电话。')
  if (form.items.length === 0) errors.push('请至少添加一条征集条目。')
  if (!form.agreementAccepted) errors.push('请勾选在线捐赠协议。')
  return errors
}
```

- [ ] **Step 12: Verify all utility tests GREEN**

Run:

```bash
cd frontend
npm run test:unit -- src/utils/fileParser.spec.ts src/utils/transferValidation.spec.ts src/utils/publicValidation.spec.ts --run
```

Expected: PASS.

- [ ] **Step 13: Commit**

```bash
git add frontend/src/utils/fileParser.ts frontend/src/utils/fileParser.spec.ts frontend/src/utils/transferValidation.ts frontend/src/utils/transferValidation.spec.ts frontend/src/utils/publicValidation.ts frontend/src/utils/publicValidation.spec.ts
git commit -m "test(portal-guo): 添加门户表单校验和文件解析"
```

## Task 3: Types, Mock Data, and API Functions

**Files:**
- Create: `frontend/src/types/transfer.ts`
- Create: `frontend/src/types/public.ts`
- Create: `frontend/src/api/transfer.ts`
- Create: `frontend/src/api/public.ts`
- Modify: `frontend/src/api/auth.ts`
- Create: `frontend/src/mock/modules/transfer.ts`
- Create: `frontend/src/mock/modules/public.ts`
- Modify: `frontend/src/mock/index.ts`

- [ ] **Step 1: Write failing API smoke tests**

Create `frontend/src/api/portal-guo.spec.ts`:

```typescript
import { describe, expect, it } from 'vitest'
import { getTransferDashboard } from './transfer'
import {
  generatePublicSearchQuery,
  getPublicHome,
  searchPublicArchives,
} from './public'
import { resetPublicPassword, sendPublicSmsCode } from './auth'

describe('portal-guo api mock mode', () => {
  it('returns transfer dashboard summary from API layer', async () => {
    const dashboard = await getTransferDashboard()
    expect(dashboard.summary.pendingTransfer).toBeGreaterThanOrEqual(1)
    expect(dashboard.recentBatches[0].sourceType).toBe('transfer')
  })

  it('returns public home and public search results', async () => {
    const home = await getPublicHome()
    const results = await searchPublicArchives({ keyword: '老城改造' })

    expect(home.stats.openArchiveCount).toBeGreaterThan(0)
    expect(results.records.every((item) => item.openStatus === 'open')).toBe(true)
  })

  it('generates public AI search query conditions', async () => {
    const query = await generatePublicSearchQuery({ text: '查找 2000 年以后公开的老城改造影像资料' })
    expect(query.ruleType).toBe('publicSearchQuery')
    expect(query.conditions.formedYearStart).toBe(2000)
  })

  it('supports public SMS and reset password mock helpers', async () => {
    await expect(sendPublicSmsCode({ phone: '13800000005', scene: 'forgot_password' })).resolves.toBe(true)
    await expect(
      resetPublicPassword({
        phone: '13800000005',
        smsCode: '135790',
        newPassword: 'Public@2026',
      }),
    ).resolves.toBe(true)
  })
})
```

- [ ] **Step 2: Run API smoke test and verify RED**

Run:

```bash
cd frontend
npm run test:unit -- src/api/portal-guo.spec.ts --run
```

Expected: FAIL because transfer/public API modules and auth helper functions do not exist.

- [ ] **Step 3: Create `frontend/src/types/transfer.ts`**

```typescript
import {
  BatchStatus,
  CarrierStatus,
  ItemStatus,
  OpenStatus,
  RetentionPeriod,
} from './enums'

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
  sourceType: 'transfer'
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

export interface TransferDashboard {
  summary: TransferDashboardSummary
  recentBatches: TransferBatch[]
}

export interface TransferBatchQuery {
  status?: string
  keyword?: string
  archiveYear?: number
  pageNo?: number
  pageSize?: number
}
```

- [ ] **Step 4: Create `frontend/src/types/public.ts`**

```typescript
import type { PageData, PageParams } from './api'
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

export interface PublicArchiveFile {
  id: number
  filename: string
  fileFormat: string
  fileSize: number
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

export interface PublicHomeStats {
  openArchiveCount: number
  electronicFileCount: number
  collectionCount: number
  latestOpenCount: number
}

export interface PublicHomeData {
  stats: PublicHomeStats
  categories: Array<{ name: string; count: number }>
  recentArchives: PublicArchive[]
}

export interface PublicSearchParams extends PageParams {
  keyword?: string
  archiveNo?: string
  title?: string
  categoryId?: number
  formedYearStart?: number
  formedYearEnd?: number
  responsibleText?: string
  tagIds?: string
  sourceType?: string
  carrierStatus?: string
  hasElectronicFile?: boolean
}

export interface PublicAiQueryRequest {
  text: string
}

export interface PublicAiQueryResult {
  ruleType: 'publicSearchQuery'
  conditions: PublicSearchParams
  rawJson: Record<string, unknown>
}

export interface PublicOverviewData {
  user: {
    realName: string
    phone: string
    status: 'active' | 'disabled'
  }
  stats: PublicHomeStats & {
    myPendingCollections: number
    myDownloadCount: number
  }
  collections: PublicCollectionBatch[]
  downloads: Array<{
    id: number
    archiveNo: string
    title: string
    downloadedAt: string
    accessStatus: 'available' | 'permission_changed'
  }>
}

export interface PublicSmsCodeRequest {
  phone: string
  scene: 'register' | 'forgot_password'
}

export interface PublicRegisterRequest {
  phone: string
  smsCode: string
  password: string
  realName: string
}

export interface PublicResetPasswordRequest {
  phone: string
  smsCode: string
  newPassword: string
}

export type PublicArchivePage = PageData<PublicArchive & { openStatus: 'open' }>
```

- [ ] **Step 5: Create `frontend/src/mock/modules/transfer.ts`**

Include at least four batches:

- draft batch `TR-2026-0001`
- pending transfer batch `TR-2026-0002`
- partially received batch `TR-2026-0003` with one rejected item and `rejectReason`
- shelved batch `TR-2026-0004`

Export:

```typescript
export const mockTransferDashboard: TransferDashboard
export const mockTransferBatches: PageData<TransferBatch>
export const mockTransferBatchDetails: Record<number, TransferBatchDetail>
export function mockGetTransferDashboard(): Promise<TransferDashboard>
export function mockGetTransferBatches(params?: TransferBatchQuery): Promise<PageData<TransferBatch>>
export function mockGetTransferBatchDetail(batchId: number): Promise<TransferBatchDetail>
export function mockCreateTransferBatch(data: TransferBatchDetail): Promise<TransferBatchDetail>
export function mockUpdateTransferBatch(batchId: number, data: TransferBatchDetail): Promise<TransferBatchDetail>
export function mockSubmitTransferBatch(batchId: number): Promise<TransferBatchDetail>
export function mockExportTransferBatch(batchId: number): Promise<Blob>
```

Implement filtering by `status`, `keyword`, and `archiveYear` in `mockGetTransferBatches`.

- [ ] **Step 6: Create `frontend/src/mock/modules/public.ts`**

Export:

```typescript
export const mockPublicHome: PublicHomeData
export const mockPublicArchivePage: PublicArchivePage
export const mockPublicArchiveDetails: Record<number, PublicArchiveDetail>
export const mockPublicOverview: PublicOverviewData
export const mockMyCollections: PageData<PublicCollectionBatch>
export function mockGetPublicHome(): Promise<PublicHomeData>
export function mockSearchPublicArchives(params?: PublicSearchParams): Promise<PublicArchivePage>
export function mockGetPublicArchiveDetail(archiveId: number): Promise<PublicArchiveDetail>
export function mockGeneratePublicSearchQuery(data: PublicAiQueryRequest): Promise<PublicAiQueryResult>
export function mockDownloadPublicArchiveFile(fileId: number): Promise<Blob>
export function mockGetPublicOverview(): Promise<PublicOverviewData>
export function mockGetMyCollections(params?: PageParams): Promise<PageData<PublicCollectionBatch>>
export function mockCreateCollectionDraft(data: PublicCollectionBatch): Promise<PublicCollectionBatch>
export function mockUpdateCollectionDraft(batchId: number, data: PublicCollectionBatch): Promise<PublicCollectionBatch>
export function mockSubmitCollectionBatch(batchId: number): Promise<PublicCollectionBatch>
export function mockSendPublicSmsCode(data: PublicSmsCodeRequest): Promise<boolean>
export function mockRegisterPublicUser(data: PublicRegisterRequest): Promise<{ id: number; realName: string; roles: string[] }>
export function mockResetPublicPassword(data: PublicResetPasswordRequest): Promise<boolean>
```

Mock archives must include:

- one previewable/downloadable public archive
- one metadata-only archive
- one paper-only archive
- one collection-sourced public archive

- [ ] **Step 7: Create `frontend/src/api/transfer.ts`**

```typescript
import request from './request'
import type { PageData } from '@/types/api'
import type {
  TransferBatch,
  TransferBatchDetail,
  TransferBatchQuery,
  TransferDashboard,
} from '@/types/transfer'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

export function getTransferDashboard(): Promise<TransferDashboard> {
  if (USE_MOCK) return import('@/mock/modules/transfer').then((m) => m.mockGetTransferDashboard())
  return request.get('/transfer/dashboard')
}

export function getTransferBatches(params?: TransferBatchQuery): Promise<PageData<TransferBatch>> {
  if (USE_MOCK) return import('@/mock/modules/transfer').then((m) => m.mockGetTransferBatches(params))
  return request.get('/transfer/batches', { params })
}

export function getTransferBatchDetail(batchId: number): Promise<TransferBatchDetail> {
  if (USE_MOCK) return import('@/mock/modules/transfer').then((m) => m.mockGetTransferBatchDetail(batchId))
  return request.get(`/transfer/batches/${batchId}`)
}

export function createTransferBatch(data: TransferBatchDetail): Promise<TransferBatchDetail> {
  if (USE_MOCK) return import('@/mock/modules/transfer').then((m) => m.mockCreateTransferBatch(data))
  return request.post('/transfer/batches', data)
}

export function updateTransferBatch(batchId: number, data: TransferBatchDetail): Promise<TransferBatchDetail> {
  if (USE_MOCK) return import('@/mock/modules/transfer').then((m) => m.mockUpdateTransferBatch(batchId, data))
  return request.put(`/transfer/batches/${batchId}`, data)
}

export function submitTransferBatch(batchId: number): Promise<TransferBatchDetail> {
  if (USE_MOCK) return import('@/mock/modules/transfer').then((m) => m.mockSubmitTransferBatch(batchId))
  return request.post(`/transfer/batches/${batchId}/submit`)
}

export function exportTransferBatch(batchId: number): Promise<Blob> {
  if (USE_MOCK) return import('@/mock/modules/transfer').then((m) => m.mockExportTransferBatch(batchId))
  return request.get(`/transfer/batches/${batchId}/export`, { responseType: 'blob' })
}
```

- [ ] **Step 8: Create `frontend/src/api/public.ts`**

Implement API functions listed in design spec section 6.2, using the same `USE_MOCK` pattern and the mock functions created in Step 6. Use these real endpoints:

```typescript
request.get('/public/archives/search', { params })
request.get(`/public/archives/${archiveId}`)
request.post('/public/archives/ai-query', data)
request.get(`/public/archive-files/${fileId}/download`, { responseType: 'blob' })
request.get('/public/overview')
request.get('/public/collections', { params })
request.post('/public/collections', data)
request.put(`/public/collections/${batchId}`, data)
request.post(`/public/collections/${batchId}/submit`, { agreementAccepted: true })
```

For `getPublicHome()`, use mock in mock mode and in real mode temporarily compose:

```typescript
return request.get('/public/archives/search', { params: { pageNo: 1, pageSize: 6 } })
```

then map the result into `PublicHomeData` with zeroed aggregate stats if the backend has not added a dedicated home endpoint.

- [ ] **Step 9: Extend `frontend/src/api/auth.ts`**

Add:

```typescript
import type {
  PublicRegisterRequest,
  PublicResetPasswordRequest,
  PublicSmsCodeRequest,
} from '@/types/public'

export function sendPublicSmsCode(data: PublicSmsCodeRequest): Promise<boolean> {
  if (USE_MOCK) {
    return import('@/mock/modules/public').then((m) => m.mockSendPublicSmsCode(data))
  }
  return request.post('/public/auth/sms-code', data)
}

export function registerPublicUser(data: PublicRegisterRequest): Promise<{ id: number; realName: string; roles: string[] }> {
  if (USE_MOCK) {
    return import('@/mock/modules/public').then((m) => m.mockRegisterPublicUser(data))
  }
  return request.post('/public/auth/register', data)
}

export function resetPublicPassword(data: PublicResetPasswordRequest): Promise<boolean> {
  if (USE_MOCK) {
    return import('@/mock/modules/public').then((m) => m.mockResetPublicPassword(data))
  }
  return request.post('/public/auth/reset-password', data)
}
```

- [ ] **Step 10: Update `frontend/src/mock/index.ts`**

Export transfer and public modules:

```typescript
export * from './modules/transfer'
export * from './modules/public'
```

Keep existing reception/collection exports.

- [ ] **Step 11: Verify API tests GREEN**

Run:

```bash
cd frontend
npm run test:unit -- src/api/portal-guo.spec.ts --run
```

Expected: PASS.

- [ ] **Step 12: Verify type-check**

Run:

```bash
cd frontend
npm run build
```

Expected: PASS through `vue-tsc -b` and Vite build.

- [ ] **Step 13: Commit**

```bash
git add frontend/src/types/transfer.ts frontend/src/types/public.ts frontend/src/api/transfer.ts frontend/src/api/public.ts frontend/src/api/auth.ts frontend/src/mock/modules/transfer.ts frontend/src/mock/modules/public.ts frontend/src/mock/index.ts frontend/src/api/portal-guo.spec.ts
git commit -m "feat(portal-guo): 添加门户类型接口和mock数据"
```

## Task 4: Public Routing and Authentication Pages

**Files:**
- Modify: `frontend/src/router/routes/public.ts`
- Modify: `frontend/src/views/public/register/index.vue`
- Create: `frontend/src/views/public/forgot-password/index.vue`
- Modify: `frontend/src/views/login/index.vue` only if links are wrong
- Test: `frontend/src/views/public/register/index.spec.ts`
- Test: `frontend/src/views/public/forgot-password/index.spec.ts`

- [ ] **Step 1: Read required prototype docs**

Read:

```bash
sed -n '1,220p' doc/prototype/public/公众注册页-原型设计.md
sed -n '1,220p' doc/prototype/public/公众注册页-验收.md
sed -n '1,220p' doc/prototype/public/register.html
sed -n '1,220p' doc/prototype/public/忘记密码页-原型设计.md
sed -n '1,220p' doc/prototype/public/忘记密码页-验收.md
sed -n '1,260p' doc/prototype/public/forgot-password.html
```

Expected: implementation notes preserve prototype layouts and only add Vue state/API calls.

- [ ] **Step 2: Write failing route test**

Create `frontend/src/router/public-routes.spec.ts`:

```typescript
import { describe, expect, it } from 'vitest'
import { publicRoutes } from './routes/public'

describe('publicRoutes', () => {
  it('includes forgot password route', () => {
    expect(publicRoutes.children?.some((route) => route.path === 'forgot-password')).toBe(true)
  })
})
```

- [ ] **Step 3: Run route test and verify RED**

Run:

```bash
cd frontend
npm run test:unit -- src/router/public-routes.spec.ts --run
```

Expected: FAIL because route is missing.

- [ ] **Step 4: Add `/public/forgot-password` route**

Modify `frontend/src/router/routes/public.ts` and add this child route after `register`:

```typescript
{
  path: 'forgot-password',
  component: () => import('@/views/public/forgot-password/index.vue'),
  meta: { title: '忘记密码' },
},
```

- [ ] **Step 5: Verify route test GREEN**

Run:

```bash
cd frontend
npm run test:unit -- src/router/public-routes.spec.ts --run
```

Expected: PASS.

- [ ] **Step 6: Write failing component tests for auth pages**

Create `frontend/src/views/public/forgot-password/index.spec.ts`:

```typescript
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ForgotPassword from './index.vue'

describe('ForgotPassword', () => {
  it('renders public-only reset boundary and form fields', () => {
    const wrapper = mount(ForgotPassword)

    expect(wrapper.text()).toContain('重置公众账号密码')
    expect(wrapper.text()).toContain('内部账号请联系系统管理员重置')
    expect(wrapper.find('#phone').exists()).toBe(true)
    expect(wrapper.find('#code').exists()).toBe(true)
    expect(wrapper.find('#password').exists()).toBe(true)
    expect(wrapper.find('#confirmPassword').exists()).toBe(true)
  })
})
```

Create `frontend/src/views/public/register/index.spec.ts`:

```typescript
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import RegisterPage from './index.vue'

describe('RegisterPage', () => {
  it('renders public registration boundary and required fields', () => {
    const wrapper = mount(RegisterPage)

    expect(wrapper.text()).toContain('公众账号')
    expect(wrapper.text()).toContain('内部账号')
    expect(wrapper.find('#phone').exists()).toBe(true)
    expect(wrapper.find('#code').exists()).toBe(true)
    expect(wrapper.find('#password').exists()).toBe(true)
    expect(wrapper.find('#confirmPassword').exists()).toBe(true)
  })
})
```

- [ ] **Step 7: Run component tests and verify RED**

Run:

```bash
cd frontend
npm run test:unit -- src/views/public/forgot-password/index.spec.ts src/views/public/register/index.spec.ts --run
```

Expected: FAIL because forgot-password component is missing and register page is still placeholder.

- [ ] **Step 8: Implement `public/forgot-password/index.vue`**

Restore `doc/prototype/public/forgot-password.html`:

- Use `<div class="public-shell">` content style only if needed inside `PublicLayout`; do not duplicate global navigation if `PublicLayout` already provides it.
- Preserve `.reset-layout`, `.step-list`, `.card`, `.panel`, `.page-title`, `.notice`, `.hint`, `.button` class usage.
- Convert form state to `<script setup lang="ts">`.
- Use `validateResetPassword()` before calling `resetPublicPassword()`.
- Use `sendPublicSmsCode({ scene: 'forgot_password' })`.
- Display result in a live region.

- [ ] **Step 9: Implement `public/register/index.vue`**

Restore `doc/prototype/public/register.html`:

- Preserve the left capability/boundary section and right registration form.
- Use `validatePublicRegister()` before calling `registerPublicUser()`.
- Use `sendPublicSmsCode({ scene: 'register' })`.
- Keep the public-only boundary text and default `public_user` success feedback.

- [ ] **Step 10: Verify auth page tests GREEN**

Run:

```bash
cd frontend
npm run test:unit -- src/router/public-routes.spec.ts src/views/public/forgot-password/index.spec.ts src/views/public/register/index.spec.ts --run
```

Expected: PASS.

- [ ] **Step 11: Verify login links**

Open `frontend/src/views/login/index.vue` and confirm:

```vue
<router-link to="/public/register" class="button secondary">公众注册</router-link>
<router-link to="/public/forgot-password" class="button ghost">忘记密码</router-link>
```

If either differs, update it.

- [ ] **Step 12: Build**

Run:

```bash
cd frontend
npm run build
```

Expected: PASS.

- [ ] **Step 13: Commit**

```bash
git add frontend/src/router/routes/public.ts frontend/src/router/public-routes.spec.ts frontend/src/views/public/register/index.vue frontend/src/views/public/register/index.spec.ts frontend/src/views/public/forgot-password/index.vue frontend/src/views/public/forgot-password/index.spec.ts frontend/src/views/login/index.vue
git commit -m "feat(portal-guo): 实现公众注册和忘记密码页面"
```

## Task 5: Transfer Portal Pages

**Files:**
- Modify: `frontend/src/views/transfer/overview/index.vue`
- Modify: `frontend/src/views/transfer/transfer-list/index.vue`
- Test: `frontend/src/views/transfer/overview/index.spec.ts`
- Test: `frontend/src/views/transfer/transfer-list/index.spec.ts`

- [ ] **Step 1: Read required prototype docs**

Read:

```bash
sed -n '1,240p' doc/prototype/transfer/移交工作台-原型设计.md
sed -n '1,220p' doc/prototype/transfer/移交工作台-验收.md
sed -n '1,320p' doc/prototype/transfer/overview.html
sed -n '1,240p' doc/prototype/transfer/编制移交清单-原型设计.md
sed -n '1,220p' doc/prototype/transfer/编制移交清单-验收.md
sed -n '1,360p' doc/prototype/transfer/transfer-list.html
```

Expected: note exact prototype sections to preserve in Vue.

- [ ] **Step 2: Write failing component tests**

Create `frontend/src/views/transfer/overview/index.spec.ts`:

```typescript
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TransferOverview from './index.vue'

describe('TransferOverview', () => {
  it('renders dashboard metrics, list, and rejection reason from mock API', async () => {
    const wrapper = mount(TransferOverview)
    await flushPromises()

    expect(wrapper.text()).toContain('移交工作台')
    expect(wrapper.text()).toContain('部分接收')
    expect(wrapper.text()).toContain('回退原因')
    expect(wrapper.text()).toContain('新建清单')
  })
})
```

Create `frontend/src/views/transfer/transfer-list/index.spec.ts`:

```typescript
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TransferList from './index.vue'

describe('TransferList', () => {
  it('renders transfer draft form and local file parsing boundary', () => {
    const wrapper = mount(TransferList)

    expect(wrapper.text()).toContain('编制移交清单')
    expect(wrapper.text()).toContain('只解析')
    expect(wrapper.text()).toContain('不上传文件本体')
    expect(wrapper.text()).toContain('提交清单')
  })
})
```

- [ ] **Step 3: Run tests and verify RED**

Run:

```bash
cd frontend
npm run test:unit -- src/views/transfer/overview/index.spec.ts src/views/transfer/transfer-list/index.spec.ts --run
```

Expected: FAIL because pages are placeholders.

- [ ] **Step 4: Implement `transfer/overview/index.vue`**

Restore `doc/prototype/transfer/overview.html` content area:

- Preserve status metric cards, process strip, filter toolbar, list/table, and detail panel.
- Use `getTransferDashboard()`, `getTransferBatches()`, and `getTransferBatchDetail()`.
- Frontend filters: all, draft, pending_transfer, partially_received, archived, shelved.
- Show loading notice before data arrives.
- Show empty notice when no batch matches filters.
- Show interface failure notice when API call rejects.
- Show rejected item `rejectReason` in detail panel.
- Do not show warehouse location or shelf details.

- [ ] **Step 5: Implement `transfer/transfer-list/index.vue`**

Restore `doc/prototype/transfer/transfer-list.html` content area:

- Preserve base info form, local file parsing drop zone, local file list, editable item table, validation summary, and submit area.
- Use `parseLocalFiles()` for file selection/drop.
- Use `validateTransferDraft()` before save/submit.
- Use `createTransferBatch()`, `updateTransferBatch()`, `submitTransferBatch()`, and `exportTransferBatch()`.
- New file-derived items must have empty `carrierStatus` until manually selected.
- Submitted state makes inputs read-only and shows export feedback.
- Do not upload files or call staging-file APIs.

- [ ] **Step 6: Verify transfer page tests GREEN**

Run:

```bash
cd frontend
npm run test:unit -- src/views/transfer/overview/index.spec.ts src/views/transfer/transfer-list/index.spec.ts --run
```

Expected: PASS.

- [ ] **Step 7: Build**

Run:

```bash
cd frontend
npm run build
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/views/transfer/overview/index.vue frontend/src/views/transfer/overview/index.spec.ts frontend/src/views/transfer/transfer-list/index.vue frontend/src/views/transfer/transfer-list/index.spec.ts
git commit -m "feat(portal-guo): 实现移交工作台和清单编制"
```

## Task 6: Public Home, Search, Collection, and Overview Pages

**Files:**
- Modify: `frontend/src/views/public/index/index.vue`
- Modify: `frontend/src/views/public/search/index.vue`
- Modify: `frontend/src/views/public/collection/index.vue`
- Modify: `frontend/src/views/public/overview/index.vue`
- Test: `frontend/src/views/public/index/index.spec.ts`
- Test: `frontend/src/views/public/search/index.spec.ts`
- Test: `frontend/src/views/public/collection/index.spec.ts`
- Test: `frontend/src/views/public/overview/index.spec.ts`

- [ ] **Step 1: Read required prototype docs**

Read:

```bash
sed -n '1,220p' doc/prototype/public/公众首页-原型设计.md
sed -n '1,220p' doc/prototype/public/公众首页-验收.md
sed -n '1,320p' doc/prototype/public/index.html
sed -n '1,240p' doc/prototype/public/公开档案检索-原型设计.md
sed -n '1,240p' doc/prototype/public/公开档案检索-验收.md
sed -n '1,420p' doc/prototype/public/search.html
sed -n '1,240p' doc/prototype/public/征集清单-原型设计.md
sed -n '1,240p' doc/prototype/public/征集清单-验收.md
sed -n '1,420p' doc/prototype/public/collection.html
sed -n '1,220p' doc/prototype/public/公众概览-原型设计.md
sed -n '1,220p' doc/prototype/public/公众概览-验收.md
sed -n '1,360p' doc/prototype/public/overview.html
```

- [ ] **Step 2: Write failing component tests**

Create `frontend/src/views/public/index/index.spec.ts`:

```typescript
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PublicHome from './index.vue'

describe('PublicHome', () => {
  it('renders public home statistics and main entries', async () => {
    const wrapper = mount(PublicHome)
    await flushPromises()

    expect(wrapper.text()).toContain('公开档案')
    expect(wrapper.text()).toContain('公开检索')
    expect(wrapper.text()).toContain('征集清单')
  })
})
```

Create `frontend/src/views/public/search/index.spec.ts`:

```typescript
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PublicSearch from './index.vue'

describe('PublicSearch', () => {
  it('renders AI query, public search results, and download boundary', async () => {
    const wrapper = mount(PublicSearch)
    await flushPromises()

    expect(wrapper.text()).toContain('公开档案检索')
    expect(wrapper.text()).toContain('AI')
    expect(wrapper.text()).toContain('下载需登录')
  })
})
```

Create `frontend/src/views/public/collection/index.spec.ts`:

```typescript
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import CollectionPage from './index.vue'

describe('CollectionPage', () => {
  it('renders collection draft form and no-upload boundary', () => {
    const wrapper = mount(CollectionPage)

    expect(wrapper.text()).toContain('征集清单')
    expect(wrapper.text()).toContain('不上传文件本体')
    expect(wrapper.text()).toContain('提交捐赠意向')
  })
})
```

Create `frontend/src/views/public/overview/index.spec.ts`:

```typescript
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PublicOverview from './index.vue'

describe('PublicOverview', () => {
  it('renders personal overview, collections, and download records', async () => {
    const wrapper = mount(PublicOverview)
    await flushPromises()

    expect(wrapper.text()).toContain('公众概览')
    expect(wrapper.text()).toContain('征集清单')
    expect(wrapper.text()).toContain('下载记录')
  })
})
```

- [ ] **Step 3: Run tests and verify RED**

Run:

```bash
cd frontend
npm run test:unit -- src/views/public/index/index.spec.ts src/views/public/search/index.spec.ts src/views/public/collection/index.spec.ts src/views/public/overview/index.spec.ts --run
```

Expected: FAIL because pages are placeholders.

- [ ] **Step 4: Implement `public/index/index.vue`**

Restore `doc/prototype/public/index.html` content area:

- Preserve public service first viewport, quick search, public stats, category distribution, recent public batches, collection flow summary, and footer boundary copy.
- Use `getPublicHome()`.
- Quick search navigates to `/public/search?keyword={keyword}`.
- Keep “download requires login” and public-range boundaries visible.

- [ ] **Step 5: Implement `public/search/index.vue`**

Restore `doc/prototype/public/search.html` content area:

- Preserve AI natural language area, JSON result block, structured search form, result list, and detail panel.
- Use `generatePublicSearchQuery()`, `searchPublicArchives()`, `getPublicArchiveDetail()`, and `downloadPublicArchiveFile()`.
- AI generation only fills JSON; it must not search until user confirms.
- Results must not show security level, warehouse location, box number, or internal notes.
- Unauthenticated download shows login guidance.
- Metadata-only and paper-only archives show explicit cannot-download messages.

- [ ] **Step 6: Implement `public/collection/index.vue`**

Restore `doc/prototype/public/collection.html` content area:

- Preserve title/donor/contact form, file-name parsing zone, item table, right-side target attributes, quantity limit, status flow, and submit result.
- Use `parseLocalFiles()` and `validateCollectionDraft()`.
- Use `createCollectionDraft()`, `updateCollectionDraft()`, and `submitCollectionBatch()`.
- Submission requires online donation agreement and then sets page read-only.
- Do not upload file bodies.

- [ ] **Step 7: Implement `public/overview/index.vue`**

Restore `doc/prototype/public/overview.html` content area:

- Preserve welcome header, account status, quick actions, stats cards, collection status list, download records, and personal info/security panel.
- Use `getPublicOverview()`.
- Show collection states: draft, pending_contact, pending_receive, partially_received, received, rejected.
- Historical download “reopen” must display a re-authentication message and not bypass permissions.

- [ ] **Step 8: Verify public page tests GREEN**

Run:

```bash
cd frontend
npm run test:unit -- src/views/public/index/index.spec.ts src/views/public/search/index.spec.ts src/views/public/collection/index.spec.ts src/views/public/overview/index.spec.ts --run
```

Expected: PASS.

- [ ] **Step 9: Build**

Run:

```bash
cd frontend
npm run build
```

Expected: PASS.

- [ ] **Step 10: Commit**

```bash
git add frontend/src/views/public/index/index.vue frontend/src/views/public/index/index.spec.ts frontend/src/views/public/search/index.vue frontend/src/views/public/search/index.spec.ts frontend/src/views/public/collection/index.vue frontend/src/views/public/collection/index.spec.ts frontend/src/views/public/overview/index.vue frontend/src/views/public/overview/index.spec.ts
git commit -m "feat(portal-guo): 实现公众门户主流程页面"
```

## Task 7: Prototype Restoration Review and Final Verification

**Files:**
- Potentially modify any page from Tasks 4 to 6 if restoration gaps are found.

- [ ] **Step 1: Compare each Vue page against prototype acceptance docs**

For each pair, check the acceptance items manually:

| Vue page | Acceptance doc |
|----------|----------------|
| `frontend/src/views/transfer/overview/index.vue` | `doc/prototype/transfer/移交工作台-验收.md` |
| `frontend/src/views/transfer/transfer-list/index.vue` | `doc/prototype/transfer/编制移交清单-验收.md` |
| `frontend/src/views/public/index/index.vue` | `doc/prototype/public/公众首页-验收.md` |
| `frontend/src/views/public/search/index.vue` | `doc/prototype/public/公开档案检索-验收.md` |
| `frontend/src/views/public/collection/index.vue` | `doc/prototype/public/征集清单-验收.md` |
| `frontend/src/views/public/overview/index.vue` | `doc/prototype/public/公众概览-验收.md` |
| `frontend/src/views/public/register/index.vue` | `doc/prototype/public/公众注册页-验收.md` |
| `frontend/src/views/public/forgot-password/index.vue` | `doc/prototype/public/忘记密码页-验收.md` |

Expected: every functional and interaction acceptance item has an implemented equivalent.

- [ ] **Step 2: Run all unit tests**

Run:

```bash
cd frontend
npm run test:unit -- --run
```

Expected: PASS.

- [ ] **Step 3: Run production build**

Run:

```bash
cd frontend
npm run build
```

Expected: PASS.

- [ ] **Step 4: Optional local smoke run**

Run:

```bash
cd frontend
npm run dev -- --host 127.0.0.1
```

Expected: Vite dev server starts. Visit:

- `http://127.0.0.1:3000/transfer/overview`
- `http://127.0.0.1:3000/transfer/transfer-list`
- `http://127.0.0.1:3000/public/index`
- `http://127.0.0.1:3000/public/search`
- `http://127.0.0.1:3000/public/collection`
- `http://127.0.0.1:3000/public/register`
- `http://127.0.0.1:3000/public/forgot-password`

Because `/public/overview` requires auth, log in through `/login` with `public` / any password / 社会公众, then visit `/public/overview`.

- [ ] **Step 5: Final commit if review fixes were needed**

If Step 1 found restoration gaps and files were changed:

```bash
git add frontend/src/views frontend/src/api frontend/src/mock frontend/src/types frontend/src/utils
git commit -m "fix(portal-guo): 补齐门户页面原型还原细节"
```

If no changes were needed, do not create an empty commit.

## Self-Review Checklist

- [ ] Spec coverage: Tasks 1-7 cover test infrastructure, utilities, types, API/mock, auth pages, transfer pages, public pages, prototype restoration review, and final verification.
- [ ] Prototype restoration: Each page task names the exact prototype HTML, design doc, and acceptance doc to read before implementation.
- [ ] Scope control: Internal portal pages and Hu Ying's admin pages are not modified.
- [ ] TDD: Utility, API, routing, and page tasks each include failing tests before implementation.
- [ ] No placeholders: Every task has concrete file paths, commands, expected outcomes, and implementation constraints.
- [ ] Type consistency: `TransferBatch`, `TransferBatchDetail`, `PublicArchive`, `PublicCollectionBatch`, and auth request types match API/mock function signatures.
