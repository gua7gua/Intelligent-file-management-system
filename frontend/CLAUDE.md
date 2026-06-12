这是项目的前端部分

## 开发前必读

开始编码前，必须先查阅以下设计文档，确保理解当前任务的业务上下文、页面原型和接口规范：

| 文档 | 路径 | 何时查阅 |
|------|------|----------|
| 项目计划 | [doc/项目计划.md](../doc/项目计划.md) | 了解当前阶段任务、里程碑和交付要求 |
| 模块说明 | [doc/模块说明.md](../doc/模块说明.md) | 各模块的页面清单、前后端分工、模块依赖关系。按模块开发前必读 |
| 业务场景 | [doc/业务场景.md](../doc/业务场景.md) | 理解业务流程、角色权限和操作闭环 |
| 技术架构 | [doc/技术架构.md](../doc/技术架构.md) | 前端技术栈、组件分层和部署方式 |
| 接口文档 | [doc/接口文档.md](../doc/接口文档.md) | REST API 规范，请与后端保持一致 |
| 页面原型 | [doc/prototype/](../doc/prototype/) | 管理后台、公众端、移交端的页面参考 |

生成页面时请参考 doc/prototype 的页面原型，确保布局、交互和字段与原型一致。

> **注意**：模块说明中列出了每个页面的路由规划，但实际路由以 `src/router/` 中的配置为准。两者存在少量命名差异（如 `/admin/users` vs `/admin/user-management`），以实际代码为准。

## 页面开发工作流

每个原型页面对应三件套产物（设计文档 → HTML → 验收文档），开发时按以下步骤逐页完成：

### 步骤

1. **读设计文档**：阅读 `doc/prototype/{portal}/xxx-原型设计.md`，理解业务场景、字段、交互逻辑和异常路径。
2. **圈定接口**：对照 `doc/接口文档.md`，列出该页面需要的 API 端点。在 `src/api/` 下创建接口函数文件，在 `src/mock/modules/` 下创建对应的 mock 数据文件。
3. **搬原型到 Vue**：打开原型 HTML（`doc/prototype/{portal}/xxx.html`），将 `<body>` 内容区搬入 `.vue` 的 `<template>`，页面内 `<style>` 搬入 `<style scoped>`，JS 逻辑转为 `<script setup>` 的响应式状态和事件处理。
4. **验证页面**：确认页面在 mock 数据下能正常渲染和交互。
5. **自查验收**：对照 `doc/prototype/{portal}/xxx-验收.md` 逐项检查。

### 全局样式说明

原型的公共样式（`doc/prototype/common/css/` 下的 `base.css`、`components.css`、`shell.css`）需要作为全局样式引入项目。页面中可直接使用 `.card`、`.toolbar`、`.button`、`.grid`、`.status`、`.metric`、`.table-wrap`、`.tabs`、`.notice` 等类名，无需在组件内重复定义。

原型使用 CSS 自定义属性定义设计令牌（`--primary: #1f6f78` 等），所有页面开发以此为准。`src/styles/variables.scss` 中的 SCSS 变量用于 Layout 组件内部，新增页面不要引用。

### Layout 和路由（已搭好，直接用）

| 门户 | Layout | 说明 |
|------|--------|------|
| 管理后台 | `AdminLayout` | 左侧导航 + 顶栏 + 内容区 |
| 公众端 | `PublicLayout` | 顶部导航 + 内容区 |
| 内部查阅 | `InternalLayout` | 顶部导航 + 内容区 |
| 移交端 | `TransferLayout` | 顶部导航 + 内容区 |

页面组件只写**内容区**，不需要管侧边栏、顶栏和导航。路由和目录已按门户分好，新页面直接在对应 `src/views/{portal}/` 下创建即可。

### API 层和 Mock 约定

#### 整体结构

```
src/api/              # 接口函数（组件只调这里）
src/mock/             # mock 数据
  modules/            # 按业务模块拆分
    fonds.ts
    archive.ts
    ...
  index.ts            # 汇总导出
```

#### Mock 数据格式

Mock 数据**必须与后端实际的响应结构一致**。后端统一响应封装为 `R<T>`（见后端 `common/R.java`）：

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "请求追踪 ID"
}
```

分页响应封装为 `PageResult<T>`（见后端 `common/PageResult.java`）：

```json
{
  "records": [],
  "pageNo": 1,
  "pageSize": 20,
  "total": 100,
  "hasNext": true
}
```

前端 `src/api/request.ts` 的响应拦截器已做了统一解包：成功时直接返回 `res.data`（即 `data` 字段的内容），失败时弹出错误提示。因此：

- **API 函数的返回类型**是 `data` 字段的内容，不是整个 `R<T>`
- **Mock 数据也只模拟 `data` 字段的内容**，不需要包裹 `{ code, message, data }`
- 分页接口的 mock 模拟完整的 `PageData<T>` 结构

实体审计字段遵循后端 `BaseEntity` 的约定：`id`(Long → number)、`createdAt`(OffsetDateTime → ISO 8601 字符串)、`updatedAt`、`createdBy`、`updatedBy`。

```typescript
// src/mock/modules/fonds.ts
import type { PageData } from '@/types/api'

export interface FondsItem {
  id: number
  fondsNo: string
  fondsName: string
  organizationId: number
  organizationName: string
  description: string
  archiveCount: number
  boxCount: number
  status: 'active' | 'disabled'
  createdAt: string
  updatedAt: string
  createdBy: number
  updatedBy: number
}

export const mockFondsDetail: FondsItem = {
  id: 1,
  fondsNo: 'F-001',
  fondsName: '克拉玛依市交通局全宗',
  organizationId: 3,
  organizationName: '克拉玛依市交通局',
  description: '交通局历年档案',
  archiveCount: 128,
  boxCount: 15,
  status: 'active',
  createdAt: '2025-09-12T10:30:00+08:00',
  updatedAt: '2025-11-03T14:20:00+08:00',
  createdBy: 1,
  updatedBy: 1,
}

export const mockFondsList: PageData<FondsItem> = {
  records: [mockFondsDetail, /* 更多记录 */],
  pageNo: 1,
  pageSize: 20,
  total: 56,
  hasNext: true,
}
```

#### API 文件写法

API 函数内部通过环境变量 `VITE_USE_MOCK` 控制返回 mock 还是真实请求。**组件永远只调 API 函数，不直接 import mock 数据**：

```typescript
// src/api/fonds.ts
import request from './request'
import type { PageData, PageParams } from '@/types/api'
import type { FondsItem } from '@/mock/modules/fonds'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

export function getFondsList(params?: PageParams): Promise<PageData<FondsItem>> {
  if (USE_MOCK) {
    return import('@/mock/modules/fonds').then(m => m.mockFondsList)
  }
  return request.get('/admin/fonds', { params })
}
```

```env
# .env.development
VITE_USE_MOCK=true    # 后端就绪后改为 false
```

**切换规则**：后端接口就绪后，在 `.env.development` 中将 `VITE_USE_MOCK` 改为 `false` 即可，所有组件代码无需改动。

### Element Plus

已全局注册，可直接使用。原型中手写的表格、表单、弹窗、消息提示等可按需用 `el-table`、`el-form`、`el-dialog`、`ElMessage` 替代，但**不强制**——原型效果够用的地方直接搬原生 HTML 即可。

## AI 辅助编程约束

本项目的组员使用 AI 辅助编程。为防止 AI 敷衍输出，以下几点必须严格执行：

1. **禁止占位符**：AI 不得输出 `{/* TODO */}`、`// ... 省略`、`// 类似上方组件`、`{/* 此处略 */}` 等占位内容。每个组件、每个交互逻辑都必须写出完整可运行的代码。
2. **禁止伪代码**：AI 不得以伪代码、思路描述、大纲替代实际代码。需要代码的地方必须给出完整的、可直接运行的实现，包括必要的 import、样式和类型定义。
3. **禁止半途而废**：如果一个页面涉及多个组件或多个状态（加载、空数据、错误、正常），AI 必须全部实现，不得只写主流程然后声称「其余状态类似」。
4. **必须处理 UI 状态**：对加载中、空数据、接口报错、权限不足、表单校验失败等状态，必须有对应的 UI 呈现，不得假定数据永远正常返回。
5. **必须验证正确性**：给出的代码必须符合项目的组件库、路由配置和现有代码风格。不得编造不存在的组件名、路径或 API 字段。
6. **如果确实需要省略**：只有在同一个文件中重复超过 3 次的结构化模板（如表格列定义、表单字段配置）才可以省略，且省略处必须写清楚「此处与上方 xxx 完全一致，共 N 个」。

如果 AI 违反以上约束，组员应直接指出并要求其补充完整，不得接受敷衍输出。

开发阶段产生的文档请放在 frontend/docs

提交前请查阅 [doc/commit-convention.md](../doc/commit-convention.md)，确保 commit 信息符合规范。
