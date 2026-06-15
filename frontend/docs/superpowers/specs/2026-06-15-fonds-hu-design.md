# feat/fonds-hu 设计文档：全宗管理（含组织维护弹窗）

**日期**：2026-06-15
**负责人**：胡颖
**分支**：`feat/fonds-hu`
**阶段**：06-16（补充计划 §3.3 前端任务第一行）
**优先级**：P1
**对应设计**：补充计划 §5.2、接口文档 §17.1-17.5、数据库设计 §4.1/§4.2、业务场景 场景十八、`prototype/admin/fonds.html` + 全宗管理-原型设计.md + 全宗管理-验收.md
**共同验收点**：06-18 页面冻结 —— 全宗管理页可完成列表/筛选/新建/编辑/详情/停用闭环，用户管理页「+新增组织」弹窗可用，与已合并页面风格一致。

## 1. 背景

本轮从最新 `develop`（`1c4f24a`）拉 `feat/fonds-hu`。补充计划（§5.2）补做 M06 全宗管理：后端周扬 `feat/fonds-zhou` 负责 `FondsController` / `OrganizationController` CRUD，**后端尚未实现**，本轮沿用 mock 先行（与 feat/settings-hu 一致）。

前置状态（develop 已包含）：

- 胡颖 `feat/settings-hu`：用户管理页已引用只读 `getFonds` / `getOrganizations` + `FondsReference` / `Organization` 类型；`views/admin/fonds/index.vue` 为 6 行占位；`api/fonds.ts` / `api/organizations.ts` 仅有只读 GET。
- 郭一坤 `feat/stats-guo`：统计/研判/编研页，确立 per-subdomain 文件组织模板。
- 后端 `fonds` / `organizations` 表、`Fonds` / `Organization` 实体 + Mapper 已就绪（V1/V12/V13/V16），缺 Controller / Service / DTO。

教训（来自 settings-hu / archive-hu）：每阶段主动执行 `npm run type-check && npm run build`，避免类型错误阻塞 develop 集成。

> 用户已确认（brainstorming 澄清）：本轮两模块（fonds + audit-log）设计 + 计划一起写，依次实现、各自分支 / PR；页面统一验收后再 push / PR。本 spec 仅覆盖全宗管理（含组织弹窗），日志审计见 `2026-06-15-audit-log-hu-design.md`。

## 2. 目标与范围

### 2.1 本轮实现

| 页面/组件 | 路由 | 说明 | 原型 | 角色 |
|-----------|------|------|------|------|
| 全宗管理 | `/admin/fonds` | 重写占位页：指标卡 + 筛选 + 全宗列表 + 右抽屉编辑（全宗号只读 / 名称 / 单位 / 说明）+ 门类分布 + 最近入库 + 停用 / 删除规则 + 审计提示 | `prototype/admin/fonds.html`（430 行）三件套 | back_archivist / sys_admin |
| 用户管理-新增组织弹窗 | `/admin/user-management` 内 | 「所属单位」下拉旁「+新增组织」弹窗（orgName / orgType / contactName / contactPhone → POST → 刷新下拉） | 原型设计 L49 意图 + §17.5 | sys_admin |

### 2.2 不在本轮

- 全宗后端 CRUD（周扬 `feat/fonds-zhou`）。
- 独立组织管理页 / 组织更新 / 删除接口（补充计划 §5.2 明确不做独立页面，并入用户管理）。
- 日志审计页（`feat/audit-log-hu`，单独 spec）。
- 接入真实后端（属联调阶段）。

## 3. 方案选择

### 3.1 文件组织（per-subdomain，沿用约定）

| 方案 | 内容 | 优点 | 缺点 |
|------|------|------|------|
| 扩展现有 per-subdomain 文件（选定） | `types/fonds.ts` / `api/fonds.ts` / `mock/modules/fonds.ts` 扩展写接口；`types/organization.ts` / `api/organizations.ts` / `mock/modules/organizations.ts` 同理 | 与 settings-hu 只读引用无缝衔接，不重复造类型；边界清晰 | — |
| 新建 fonds-crud 子域 | 另起文件 | 隔离 | 与现有只读 fonds 文件割裂，违反 DRY |

**选定**：扩展现有文件。`FondsReference` 保留（用户管理页只读引用），新增更完整的 `FondsItem` 承载页面所需字段。

### 3.2 关键决策（用户确认按推荐）

| 编号 | 决策 | 选定 |
|------|------|------|
| D1 | 全宗详情「门类分布 + 最近入库」数据来源 | 作为 `FondsItem` 可选富字段（`categoryDistribution` / `recentIntake`），mock 全量填充；后端缺省时面板显示空态。视觉忠于原型 + 缺失优雅降级 |
| D2 | 全宗「停用」实现 | 复用 `updateFonds(id, { status: 'disabled' })`（PUT，§17.3 返回详情），不新增独立 disable 端点 |

### 3.3 组件粒度

fonds.html 430 行，Vue 化后单文件预计 450–600 行。**选定**：单 `index.vue` 为主；若实现时超 600 行，将右抽屉编辑表单抽为 `components/FondsEditDrawer.vue`。组织弹窗内联于 user-management（el-dialog，体量小，不单独拆文件）。

## 4. 类型定义（types/fonds.ts、types/organization.ts）

`types/fonds.ts` 在现有 `FondsParams` / `FondsReference` 基础上扩展：

```ts
/** 门类分布条目（富字段，可选） */
export interface FondsCategorySlice { category: string; count: number }
/** 最近入库记录（富字段，可选） */
export interface FondsRecentIntake { date: string; title: string }

/** 全宗完整项（页面主用，对齐 §17.1 返回 + DB §4.2） */
export interface FondsItem {
  id: number
  fondsNo: string
  fondsName: string
  organizationId: number
  organizationName?: string
  description?: string
  archiveCount: number       // 归档档案数（§17.1 明确返回）
  boxCount: number           // 档案盒数（聚合自 archive_boxes）
  status: 'active' | 'disabled'
  // 可选富字段（D1）：后端缺省时为 undefined，面板降级为空态
  categoryDistribution?: FondsCategorySlice[]
  recentIntake?: FondsRecentIntake[]
  createdAt?: string
  updatedAt?: string
  createdBy?: number
  updatedBy?: number
}

/** 新增全宗请求体（§17.2） */
export interface FondsCreate {
  fondsNo: string
  fondsName: string
  organizationId: number
  description?: string
}

/** 更新全宗请求体（§17.3，fondsNo 不可改 → 不含） */
export interface FondsUpdate {
  fondsName?: string
  organizationId?: number
  description?: string
  status?: 'active' | 'disabled'   // 停用 / 启用经此字段
}
```

> `FondsParams` 扩展 `organizationId?` 与 `relation?`（'linked' | 'empty' | 'disabled'，对应原型筛选「关联状态」）。`FondsReference` 保留不变。

`types/organization.ts` 新增：

```ts
/** 新增组织请求体（§17.5） */
export interface OrganizationCreate {
  orgName: string
  orgType: 'archive_org' | 'government' | 'enterprise' | 'public_institution'
  contactName?: string
  contactPhone?: string
}
```

## 5. API 层（api/fonds.ts、api/organizations.ts）

沿用 `USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'` 切换；组件只调 API 函数。

```ts
// api/fonds.ts（在现有 getFonds 基础上新增）
export function createFonds(body: FondsCreate): Promise<FondsItem> { /* mock 或 POST /admin/fonds */ }
export function updateFonds(id: number, body: FondsUpdate): Promise<FondsItem> { /* mock 或 PUT /admin/fonds/{id} */ }
export function removeFonds(id: number): Promise<void> { /* mock 或 DELETE /admin/fonds/{id}（§17 未定义 DELETE，联调确认）；仅无关联数据允许 */ }

// api/organizations.ts（在现有 getOrganizations 基础上新增）
export function createOrganization(body: OrganizationCreate): Promise<Organization> { /* mock 或 POST /admin/organizations */ }
```

- `getFonds` 返回类型由 `PageData<FondsReference>` 改为 `PageData<FondsItem>`（兼容：`FondsItem` 含 `FondsReference` 全部字段；用户管理页只读引用不受影响，只多读了 archiveCount 等字段）。
- 后端就绪后 `.env.development` 置 `VITE_USE_MOCK=false` 即切真请求，组件不改。

## 6. Mock（mock/modules/fonds.ts、organizations.ts）

- `fonds.ts`：现有 4 条扩为含 `archiveCount` / `boxCount` / `categoryDistribution` / `recentIntake` 的 `FondsItem`；新增 `mockCreateFonds(body)`（唯一校验 fondsNo、追加 active 项、返回新项）、`mockUpdateFonds(id, body)`（更新名称 / 单位 / 说明 / status；返回最新项）。
- `organizations.ts`：新增 `mockCreateOrganization(body)`（orgName 唯一校验 §17.6、追加 active 项、返回新项）。
- Mock 内部状态为模块级数组，create/update 即时变更，保证页面刷新可见。

## 7. 页面设计

### 7.1 全宗管理页 `views/admin/fonds/index.vue`（还原 fonds.html）

布局（grid：左主区 `1fr` + 右抽屉 `390px`，≤1120px 单栏）：

1. **顶部工具栏**：标题「全宗管理」+ 副标题；右侧「新建全宗」按钮 + 「组织维护」链接（跳 `/admin/user-management`，对齐原型 L129）。
2. **指标卡（4）**：全宗总数 / 启用全宗 / 归档档案（archiveCount 求和）/ 档案盒（boxCount 求和）。从已加载列表计算。
3. **筛选卡**：关键词（全宗号 / 名称 / 单位）、所属单位（select，来自 `getOrganizations`）、关联状态（有归档数据 / 无关联 / 已停用）；查询 / 重置按钮。
4. **全宗列表**：全宗号 / 全宗名称 / 所属单位 / 归档档案 / 档案盒 / 状态 / 操作（编辑、停用｜删除）。提示「全宗号创建后不可修改」。点击行 → 右抽屉载入详情。
5. **门类分布 + 最近入库**（左主区列表下方 `.grid two`，按原型 L194-203；随选中行联动刷新）：富字段缺失时显示空态「暂无门类数据 / 暂无入库记录」。
6. **右抽屉编辑表单**：当前状态、关联数据（N 件档案 / M 个档案盒）；全宗号（新建可编辑 + 唯一校验，编辑只读）、全宗名称、所属单位（select）、全宗说明（textarea）；操作审计提示（变更写入 audit_logs）；保存 / 取消 / 停用｜删除。
7. **规则提示**：全宗与移交清单不直接关联，入库时才写入正式档案归属。

UI 状态：加载中（骨架/文案）、加载失败（重试）、空数据（暂无全宗）、表单校验失败（ElMessage 提示具体字段）。

### 7.2 用户管理页「+新增组织」弹窗

在 `views/admin/user-management/index.vue` 账号编辑表单「所属单位」select（现 L101-105）旁加 `+新增组织` 按钮：

- 点击 → `el-dialog`（标题「新增组织」）：orgName（必填）、orgType（select：archive_org/government/enterprise/public_institution，必填）、contactName、contactPhone（手机/座席格式校验）。
- 提交 → `createOrganization` → 成功后刷新 `organizations`（复用 `loadAll` 或单独刷新）→ 关闭弹窗 → ElMessage 成功；失败提示唯一性/格式错误。
- 复用页面已有 `organizations` ref 与 `getOrganizations`，新增 `createOrganization` import。

## 8. 校验与业务规则

| 规则 | 处理 |
|------|------|
| 全宗号必填 + 唯一 | 前端校验（与现有 fonds 列表比对，排除自身）；mock create 拒重；后端 §17.2 `uk_fonds_fonds_no` 兜底 |
| 全宗号创建后不可改 | 编辑态 input `readonly` + disabled，仅新建可填 |
| 全宗名称 / 所属单位必填 | 前端校验阻止保存 |
| 有关联数据（archiveCount>0 或 boxCount>0）→ 停用，不可删除 | 操作按钮文案与行为：有关联显示「停用」（updateFonds status=disabled）；无关联显示「删除」（mock 移除） |
| 组织名称唯一 | mock create 拒重；后端 §17.6 兜底 |
| 操作写 audit_logs | UI 文案/notice 说明，mock 不模拟落库（后端职责） |

校验逻辑收敛到 `utils/fondsValidation.ts`（`validateFondsForm`）与组织弹窗内联校验，与 settings-hu 的 `userValidation.ts` 风格一致。

## 9. 测试计划（`.spec.ts`，沿用 settings-hu / storage-guo 约定）

| 文件 | 覆盖 |
|------|------|
| `api/fonds.spec.ts`（扩） | createFonds/updateFonds mock 返回结构与字段；getFonds 返回 FondsItem |
| `api/organizations.spec.ts`（扩） | createOrganization mock 返回结构；唯一校验 |
| `utils/fondsValidation.spec.ts`（新） | 全宗号空 / 重复 / 名称空 / 未选单位的校验结果 |
| `views/admin/fonds/index.spec.ts`（新） | 列表渲染、筛选、新建保存、编辑（全宗号只读）、停用门控、空态 |
| `views/admin/user-management/index.spec.ts`（扩） | 「+新增组织」弹窗打开 / 提交成功刷新下拉 / 校验失败 |

页面 spec 用 `@vue/test-utils` 挂载 + mock api，覆盖核心交互与状态门控，禁止「不可能失败」的断言。

## 10. 风险与降级

| 风险 | 应对 |
|------|------|
| 富字段（门类分布 / 最近入库）后端不提供 | D1：可选字段 + 空态降级，不影响主流程 |
| `getFonds` 返回类型由 Reference 改 Item 影响用户管理页 | Item 是 Reference 超集，编译兼容；用户管理页只读字段不变 |
| 停用复用 PUT 与后端实际语义不符 | 以接口文档 §17.3 为准；联调时与周扬对齐 PUT 请求体（若后端用独立 disable 端点，仅需调 API 函数内部实现，组件不改） |
| 类型/构建错误阻塞 develop | 每阶段 `npm run type-check && npm run build` |


