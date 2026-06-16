# 公众概览扩展与公开馆藏统计 — 接口调整设计

日期：2026-06-16
负责：周扬（后端）
依据：前端「给后端的接口调整清单」+ 前后端真实代码核对 + `doc/接口文档.md` 权威契约

## 1. 背景与诊断

前端清单 3 点经核对（已读后端 `AuthController/AuthService`、`PublicSearchController/PublicDashboardService`、前端 `api/auth.ts`、`api/public.ts`、`views/public/overview/index.vue`、接口文档 §3.3/§5.1/§5.2）：

| 清单项 | 诊断 |
|--------|------|
| ① `/public/dashboard` 的 downloadLogs 缺 archiveNo/title | ✅ 属实。`ArchiveAccessLogResponse` 仅 8 字段，`buildDownloadLogs` 未联表 archives |
| ② 缺公开馆藏统计端点 | ✅ 属实。无任何端点返回 openArchiveCount/electronicFileCount |
| ③ `/api/user/info` 缺 phone/status | ❌ 清单有误。后端无 `/api/user/info`；当前用户端点为 `GET /api/auth/me`，返回的 `LoginResponse.UserInfoResponse` **已含** phone/status/realName/roles/maxSecurityLevel/dataScope。后端无需改，前端仅需补 TS 类型 |

前端代码另有两处端点对不上（清单未提）：
- 公众概览页 `getPublicOverview()` → `GET /api/public/overview`：**后端不存在（404）**，期望 `{user, stats, collections, downloads}`。
- 公众首页 `getPublicHome()`：占位实现（调 `/public/archives/search` 后丢弃结果返回 0）。

判断：前端清单写「端点 `/public/dashboard`」是修正后的意图——概览页统一改调已有 `/public/dashboard`。真正落点是把 dashboard 扩展为能喂饱概览页的聚合端点，并为首页提供公开统计。

## 2. 决策（已与用户确认）

- **端点方案 A**：扩展 `/api/public/dashboard`（补 downloadLogs 档号/题名 + 公开统计 + 用户资料）；新建 `GET /api/public/stats`（首页公开统计，免登录）。
- **electronicFileCount 口径 A**：archive_files 中关联到公开档案的电子文件记录数。
- 统计口径**严格复用 §5.2 publicSearch 三条件**：`security_level=0 AND open_status='open' AND lifecycle_status='normal'`，**不加** deleted_at，使统计数 = 公开检索可搜到的档案数，可互相验证。
- `/public/stats` 与 `/public/archives/search` 一样受 `public_search.enabled` 开关约束（关闭抛 `BUSINESS_CONFLICT`）。
- 自定假设（可推翻）：`latestOpenCount` = 近 30 天开放的公开档案数。

## 3. 端点 1：扩展 `GET /api/public/dashboard`（需登录 public_user）

现有返回 `{collectionSummary, recentCollections, downloadLogs, summarizedAt}`，新增：

- `downloadLogs[*]` 补 `archiveNo`、`title`（取日志后按 archiveId 批量查 archives 建 map 填充，避免 N+1）。
- `stats`：
  ```jsonc
  { "openArchiveCount": 128, "electronicFileCount": 56,
    "collectionCount": 30, "latestOpenCount": 12,
    "myPendingCollections": 2, "myDownloadCount": 15 }
  ```
  - 公开四项（openArchiveCount/electronicFileCount/collectionCount/latestOpenCount）来自 `PublicStatsService`
  - `myPendingCollections` = `collectionSummary.inProgress`
  - `myDownloadCount` = `archive_access_logs` 中 user_id=本人 + access_type=download 计数
- `user`：`{ realName, phone, status:'active'|'disabled' }`，取当前登录公众用户（UserMapper.selectById），status 由 `UserStatus` 枚举名转换。

## 4. 端点 2：新建 `GET /api/public/stats`（免登录）

```jsonc
{ "openArchiveCount": 128, "electronicFileCount": 56,
  "collectionCount": 30, "latestOpenCount": 12,
  "categories": [ {"name":"文书","count":80} ] }
```
前端首页 `recentArchives` 继续用已有 `/public/archives/search`，stats 仅负责统计 + 门类分组。

| 字段 | 来源 |
|------|------|
| openArchiveCount | `archives` 三条件计数（MP selectCount） |
| electronicFileCount | `archive_files af JOIN archives a` 三条件 + `af.deleted_at IS NULL`（ArchiveMapper `@Select`） |
| collectionCount | `intake_batches` deleted_at IS NULL 计数 |
| latestOpenCount | `archives` 三条件 + `updated_at >= now-30d`（MP selectCount） |
| categories | `archives` 三条件 `GROUP BY category_id`，name 由 CategoryMapper 映射（ArchiveMapper `@Select`） |

## 5. 代码改动清单

- `dto/response/ArchiveAccessLogResponse.java`：加 `archiveNo`、`title`
- `dto/response/PublicDashboardResponse.java`：加 `Stats`、`User` 内部类
- 新增 `dto/response/PublicStatsResponse.java`
- `mapper/ArchiveMapper.java`：加 `@Select` 联表计数 + 门类分组
- 新增 `service/PublicStatsService.java`：开关判断 + 公开统计查询
- `service/PublicDashboardService.java`：注入 ArchiveMapper/UserMapper/PublicStatsService；downloadLogs 联表填充；注入 stats/user
- `controller/PublicSearchController.java`：加 `@GetMapping("/stats")`

## 6. 测试

- `PublicStatsServiceTest`（新建）：openArchiveCount/electronicFileCount/collectionCount/latestOpenCount/categories 计数；开关关闭抛 BUSINESS_CONFLICT。
- `PublicDashboardServiceTest`（扩展）：保留原断言，补 downloadLogs 的 archiveNo/title、stats、user。
- 运行时验证：启 dev（profile=dev），种子账号 123456 登录后 HTTP 实测 `/api/public/dashboard` 与 `/api/public/stats`。

## 7. 前端配合（非本次后端范围）

- `getPublicOverview` 改调 `/public/dashboard` 并适配类型（downloads 用 accessedAt，accessStatus 默认 available）。
- `getPublicHome` 改调 `/public/stats`（+ search 取 recentArchives）。
- 前端 `UserInfo` 类型补 `phone/status`（后端 `/auth/me` 早已返回）。
