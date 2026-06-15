# 模块一运行时验证报告：全宗管理 + 组织维护（feat/fonds-zhou）

- 验证人：周扬
- 验证日期：2026-06-15
- 分支：`feat/fonds-zhou`
- 环境：PostgreSQL 17（archive-postgres:5432）+ 后端 dev profile（8080）

## 验证范围

§17.1–17.6 后端端点：全宗查询/新增/更新/详情 + 组织查询/新增。含唯一校验、归档数量、业务关联保护、审计日志、权限。

## 启动

- `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` 启动成功（~2s），Flyway 迁移完成，上下文加载通过（证明新增 FondsService/OrganizationService/Controller bean 正常注入）。
- 单测：`FondsServiceTest` 8 通过、`OrganizationServiceTest` 4 通过。
- 全量 `./mvnw test`：196 项，195 通过；唯一失败为 `ArchiveApplicationTests.contextLoads`（默认 profile 无 datasource，**既有环境依赖问题，非本模块引入**）。

## 验证结果（真实 HTTP 请求）

登录：`admin`(sys_admin) portal=admin，token 获取成功，roles=["sys_admin"]。

| # | 场景 | 期望 | 实际 | 结论 |
|---|------|------|------|------|
| 1 | GET `/api/admin/fonds` 列表 | 返回种子 F001/F002/F003 + 归档数 | total=3，F001 archiveCount=4、F002=1、F003=2 | ✅ |
| 2 | POST 新增 F999（org 1） | 200 + 详情 | code=OK，id=4，fondsNo=F999 | ✅ |
| 3 | POST 重复 F999 | 409 全宗号已存在 | BUSINESS_CONFLICT 全宗号已存在 | ✅ |
| 4 | POST organizationId=999 不存在 | 409 所属组织不存在 | BUSINESS_CONFLICT 所属组织不存在 | ✅ |
| 5 | GET F999 详情 | archiveCount/boxCount=0 + orgName | archiveCount=0、boxCount=0、orgName=克拉玛依市档案馆 | ✅ |
| 6 | PUT F001（archiveCount=4 有关联）改 fondsName+停用 | 仅 status 生效，fondsName 不变 | fondsName 保持原名、status=disabled | ✅ 业务关联保护 |
| 7 | PUT F999（无关联）改 fondsName | 全字段更新 | fondsName→验证全宗-改名 | ✅ |
| 8 | GET `/api/admin/organizations` | 4 个种子组织 | total=4（档案馆/财政局/城建/教科院） | ✅ |
| 9 | POST 新增组织「验证组织」 | 200 + orgType | code=OK，id=5，orgType=government | ✅ |
| 10 | POST 组织重名 | 409 组织名称已存在 | BUSINESS_CONFLICT 组织名称已存在 | ✅ |
| 11 | back_archivist(liu.back) GET fonds | 200 允许 | HTTP 200 | ✅ |
| 12 | back_archivist POST 组织 | 403 仅系统管理员 | FORBIDDEN 仅系统管理员可新增组织 | ✅ |
| 13 | 未登录 GET fonds | 401 | UNAUTHORIZED | ✅ |

## 审计日志（audit_logs，module=M06）

验证后查 DB，确认写入：
- `create_fonds` / fonds / 4 / `{"fondsNo":"F999","fondsName":"验证全宗"}`
- `update_fonds` / fonds / 1 / `{"onlyStatus":true,"assoc":6,"status":"disabled"}`（关联 6=归档4+档案盒2，仅状态）
- `update_fonds` / fonds / 1 / `{"onlyStatus":true,"assoc":6,"status":"active"}`
- `update_fonds` / fonds / 4 / `{}`（无关联全字段更新）
- `create_organization` / organization / 5 / `{"orgName":"验证组织","orgType":"government"}`

## 数据清理

验证产生的 F999、验证组织已删除；F001 状态恢复 active。

## 结论

模块一运行时验证全部通过，端点行为符合 §17 与设计预期。基本完成。

## 已知无关问题（非本模块）

- `ArchiveApplicationTests.contextLoads`：默认 profile 无 datasource 导致，dev profile 下正常。
- 公众门户 `POST /api/auth/login`（portal=public）返回 INTERNAL_ERROR：AuthService 公众门户登录既有问题，与本模块（fonds/org）无关；模块二忘记密码走 `/api/public/auth/**`，不依赖该登录路径。
