# 智能档案管理系统 — Playwright 前端 E2E 测试

真实集成 E2E：连真前端（3000）+ 真后端（8080），以真实用户视角驱动浏览器，覆盖 4 门户页面级冒烟、异常路径。业务闭环（阶段二）待补。

## 前置

1. 前后端在跑：
   - 前端 `cd frontend && npm run dev`（http://localhost:3000）
   - 后端 `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`（8080）
2. DB 任意基线即可（**幂等设计，不重置 DB**）。
3. 浏览器：复用系统 `google-chrome-stable`（`channel:'chrome'`），无需下载浏览器二进制。

## 运行

```bash
cd doc/e2e-tests
npm install                 # 首次
npm test                    # 全量（68 用例，约 30-40s）
npx playwright test tests/00-login       # 只跑登录
npx playwright test tests/smoke-public   # 只跑公众冒烟
npx playwright test --headed             # 有头模式看执行
npm run report              # 打开 HTML 报告
```

- `globalSetup` 会先探活前后端 + 为 8 个角色 API 登录生成 `storageState`（`.auth/*.json`，已 gitignore）。任一角色登录失败即中止并报清晰原因。
- 失败自动截图 + trace（`on-first-retry`），产物在 `test-results/`、报告在 `playwright-report/`。

## 目录

```
support/        账号/鉴权/夹具/断言/页面对象/通用冒烟
tests/
  00-login/        登录与鉴权
  smoke-public/    公众门户（首页/检索/概览/征集）
  smoke-internal/  内部门户（工作台/检索/借阅）
  smoke-transfer/   移交门户（工作台/编制清单）
  smoke-admin/     管理后台（按角色：back/front/director/sysadmin）
  error-paths/     异常路径（检索权限/登录鉴权边界）
测试用例/        人类可读用例文档，与 spec 一一对应
```

## 用例文档

见 [测试用例/00-总览与约定.md](测试用例/00-总览与约定.md)（账号/密码/夹具/幂等/定责/方法论）及各门户文档。

## 已知问题（测试发现）

- 访问日志「访问时间」列为 ISO 未格式化（A-SYS-5 暂跳过 ISO 检查），见 [测试用例/50-管理后台.md](测试用例/50-管理后台.md)。
