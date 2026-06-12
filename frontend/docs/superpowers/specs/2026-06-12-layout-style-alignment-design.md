# Layout 样式对齐原型设计系统

日期：2026-06-12
负责人：胡颖
状态：设计已确认

## 背景

管理后台 Layout 框架使用 Element Plus 默认配色（深色侧边栏 `#1d1e2c`、蓝色主色 `#409eff`），与原型设计系统不一致。原型使用白色侧边栏、`#1f6f78` 青色主色、自定义导航链接。需要将 Layout 对齐原型视觉基准，使导航栏和页面内容风格统一。

## 涉及文件

### 需要新增的文件

| 文件 | 说明 |
|------|------|
| `src/styles/prototype/base.css` | 从 `doc/prototype/common/css/base.css` 复制 |
| `src/styles/prototype/components.css` | 从 `doc/prototype/common/css/components.css` 复制 |
| `src/styles/prototype/shell.css` | 从 `doc/prototype/common/css/shell.css` 复制 |

### 需要修改的文件

| 文件 | 修改内容 |
|------|----------|
| `src/main.ts` | 引入三个原型 CSS 文件 |
| `src/layouts/AdminLayout.vue` | 模板和样式对齐原型 app-shell 结构 |
| `src/layouts/PublicLayout.vue` | 模板和样式对齐原型 public-shell 结构 |
| `src/layouts/InternalLayout.vue` | 模板和样式对齐原型 app-shell 结构 |
| `src/layouts/TransferLayout.vue` | 模板和样式对齐原型 app-shell 结构 |
| `src/styles/variables.scss` | 删除与原型 CSS 变量冲突的 SCSS 变量 |
| `src/styles/index.scss` | 删除旧的 Layout 样式定义 |

### 不修改的文件

- `src/router/` — 路由配置不变
- `src/stores/` — 状态管理不变
- `src/components/PortalSwitcher/` — 组件逻辑不变
- `src/views/` — 页面内容不变

## 设计方案

### 1. 原型 CSS 放置位置

将 `doc/prototype/common/css/` 下的三个文件复制到 `src/styles/prototype/`：

```
src/styles/prototype/
  base.css        — CSS 自定义属性（--primary: #1f6f78 等）、重置、排版
  components.css  — .card、.button、.grid、.status 等组件类
  shell.css       — .app-shell、.sidebar、.topbar、.content、.public-shell、.public-nav 布局
```

在 `main.ts` 中从 `@/styles/prototype/` 引入，不跨目录引用 doc。

### 2. AdminLayout.vue 改造

原型管理后台结构（摘自 `doc/prototype/admin/appraisal.html`）：

```html
<div class="app-shell">
  <aside class="sidebar">
    <div class="brand"><strong>智能档案管理系统</strong><span>管理后台</span></div>
    <nav class="nav-section">
      <div class="nav-title">工作台</div>
      <a class="nav-link" href="overview.html">管理概览 <span>总览</span></a>
    </nav>
    <!-- 更多 nav-section -->
  </aside>
  <main class="main">
    <header class="topbar">
      <div><strong>页面标题</strong><span class="muted"> 副标题</span></div>
      <div class="role">角色信息</div>
    </header>
    <section class="content">
      <!-- 页面内容 -->
    </section>
  </main>
</div>
```

改造要点：
- 外层 `div` 使用 `class="app-shell"`，利用 shell.css 的 grid 布局
- 侧边栏 `<aside class="sidebar">`，白色背景，无 Element Plus el-menu
- 品牌区用 `.brand` 类名
- 导航按原型分组（工作台、接收入库、库房与利用、处置与审批、支撑管理），使用 `.nav-section` > `.nav-title` + `.nav-link`
- `router-link` 替代 `<a>`，`active` 类根据当前路由自动绑定
- 顶栏 `<header class="topbar">`，粘性定位 + 毛玻璃背景
- 内容区 `<section class="content">`
- 保留角色切换（`PortalSwitcher`）和用户下拉菜单
- 保留侧边栏折叠功能，用 CSS 类 `.sidebar-collapsed` 控制

侧边栏导航分组（来自原型 admin 页面）：

| 分组标题 | 导航项 |
|----------|--------|
| 工作台 | 管理概览 |
| 接收入库 | 移交验收、征集管理与接收、待入库与上架、档案管理、全宗管理 |
| 库房与利用 | 库房管理、档案盘点、借阅审批 |
| 处置与审批 | 档案鉴定、档案销毁、审批工作台 |
| 支撑管理 | 档案编研、数据统计、数据研判、档案保存、用户管理、系统配置 |

导航项需根据用户角色过滤（复用现有 `filterMenu` 逻辑），角色无权限的分组整体隐藏。

### 3. PublicLayout.vue 改造

原型公众门户结构（摘自 `doc/prototype/public/index.html`）：

```html
<div class="public-shell">
  <nav class="public-nav">
    <a href="index.html" class="active">智能档案馆</a>
    <div class="links">
      <a href="search.html">公开检索</a>
      <a href="collection.html">征集清单</a>
      <a href="overview.html">公众概览</a>
      <a>登录</a>
      <a>注册</a>
    </div>
  </nav>
  <main class="public-content">
    <!-- 页面内容 -->
  </main>
</div>
```

改造要点：
- 外层 `div` 使用 `class="public-shell"`
- 导航 `<nav class="public-nav">`，粘性顶部 + 毛玻璃背景
- 左侧品牌链接 "智能档案馆"，右侧 `.links` 放导航项
- 已登录显示：公开检索、征集清单、公众概览 + PortalSwitcher + 用户下拉
- 未登录显示：公开检索、征集清单、公众概览 + 登录、注册按钮
- 内容区 `<main class="public-content">`

### 4. InternalLayout.vue 改造

原型内部门户结构（摘自 `doc/prototype/internal/overview.html`）：

```html
<div class="app-shell">
  <aside class="sidebar">
    <div class="brand">
      <strong>智能档案管理系统</strong><span>内部查阅者门户</span>
    </div>
    <nav class="nav-section">
      <div class="nav-title">个人工作台</div>
      <a class="nav-link" href="overview.html">工作台概览</a>
      <a class="nav-link" href="search.html">档案检索利用</a>
      <div class="nav-title">利用记录</div>
      <a class="nav-link" href="overview.html#borrow">我的借阅申请</a>
      <a class="nav-link" href="overview.html#downloads">我的下载记录</a>
    </nav>
  </aside>
  <div class="main">
    <header class="topbar">...</header>
    <main class="content">...</main>
  </div>
</div>
```

改造要点：
- 复用 `.app-shell` 结构，和 AdminLayout 类似但导航项不同
- 品牌副标题 "内部查阅者门户"
- 顶栏显示用户名 + 角色状态标签
- 保留 PortalSwitcher 和用户下拉

### 5. TransferLayout.vue 改造

原型移交门户结构（摘自 `doc/prototype/transfer/overview.html`）：

```html
<div class="app-shell">
  <aside class="sidebar">
    <div class="brand">
      <strong>智能档案管理系统</strong><span>移交单位门户</span>
    </div>
    <nav class="nav-section">
      <div class="nav-title">移交业务</div>
      <a class="nav-link" href="overview.html">移交工作台</a>
      <a class="nav-link" href="transfer-list.html">编制移交清单</a>
      <div class="nav-title">状态说明</div>
      <a class="nav-link">清单导出打印</a>
      <a class="nav-link">回退条目处理</a>
    </nav>
  </aside>
  <div class="main">
    <header class="topbar">...</header>
    <main class="content">...</main>
  </div>
</div>
```

改造要点：
- 复用 `.app-shell` 结构
- 品牌副标题 "移交单位门户"
- 顶栏显示用户单位信息
- 保留 PortalSwitcher 和用户下拉

### 6. variables.scss 清理

删除与原型 CSS 变量冲突的变量：

```scss
// 删除以下变量（原型 base.css 的 CSS 自定义属性已覆盖）
$sidebar-bg: #1d1e2c;         // 原型 sidebar 是白色，border-right 分隔
$sidebar-text: #c0c4cc;       // 原型 nav-link 默认色 #364554
$sidebar-active-text: #ffffff;
$sidebar-active-bg: #409eff;  // 原型 active 用 --primary-soft 背景 + --primary-strong 文字
$header-bg: #ffffff;          // 原型 topbar 用 rgba(255,255,255,0.94) + backdrop-filter
$header-border: #e4e7ed;      // 原型用 var(--border) = #d7e0e7
$content-bg: #f5f7fa;         // 原型用 var(--bg) = #f6f8fb
$content-padding: 20px;       // 原型 .content padding: 26px
$primary-color: #409eff;      // 原型 --primary: #1f6f78
$success-color: #67c23a;      // 原型 --success: #2f855a
$warning-color: #e6a23c;      // 原型 --warning: #b7791f
$danger-color: #f56c6c;       // 原型 --danger: #c2413a
$info-color: #909399;         // 原型 --info: #2b6cb0
```

可能保留的变量（Layout 内部尺寸用）：
```scss
$sidebar-width: 248px;              // 原型 .app-shell grid-template-columns 也是 248px
$sidebar-collapsed-width: 64px;    // 折叠宽度，原型没有但功能需要
```

### 7. index.scss 清理

删除旧 Layout 样式定义，由 shell.css 的类名替代：
- `.admin-layout`、`.admin-sidebar`、`.admin-sidebar-logo`、`.admin-sidebar-menu`
- `.admin-main`、`.admin-header`、`.admin-header-left`、`.admin-header-right`
- `.admin-content`、`.collapse-btn`、`.user-info`
- `.public-layout`
- 相关的 Element Plus el-menu 覆盖样式

保留：
- 滚动条美化样式
- `@use './variables' as *`（如果 variables.scss 还有内容）
- ProTable 相关样式（非 Layout 部分）

## 风险和注意事项

1. **Element Plus el-menu 移除**：AdminLayout 不再使用 el-menu 的 router 模式，改用自定义导航。路由跳转由 router-link 处理。
2. **侧边栏折叠**：原型没有折叠功能，但管理后台需要。用自定义 CSS（`.sidebar-collapsed` 类）实现，不影响原型的展开态样式。
3. **角色过滤**：导航项的角色过滤逻辑保留，只是从 el-menu 结构迁移到自定义 nav 结构。
4. **$header-height**：原型 topbar 用 `min-height: 64px`，不是固定高度。删除 `$header-height: 50px`。
