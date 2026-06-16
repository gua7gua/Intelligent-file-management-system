# 公众概览扩展 + 公开馆藏统计 — 运行时验证

日期：2026-06-16　作者：周扬（zhou）　模块：公众概览 / 公开统计

## 验证环境

- profile=dev，Tomcat 8080，`/actuator/health`=UP
- 依赖：archive-postgres(5432)、archive-minio(9001/9002)、archive-clamav(3310) 均 running
- 种子账号：zhou.public / 123456 / portal=public（public_user 角色）

## 验证结果

### 1. GET /api/public/stats（免登录）

```
data: {
  "openArchiveCount": 5, "electronicFileCount": 4,
  "collectionCount": 5, "latestOpenCount": 5,
  "categories": [
    {"name":"文书档案","count":2}, {"name":"会计档案","count":2}, {"name":"音像档案","count":1}
  ]
}
```
- 口径 = §5.2 公开检索三条件（security_level=0 AND open_status=open AND lifecycle_status=normal）。
- electronicFileCount=4：archive_files JOIN 公开档案，过滤 deleted_at IS NULL。
- 门类分组正常，category_id 经 categories 表映射为中文名。

### 2. GET /api/public/dashboard（登录 zhou.public）

- `collectionSummary`：{total:2, draft:0, inProgress:1, completed:1}
- `recentCollections`：2 条（BAT-000004 已入库 / BAT-000003 待联系）
- `downloadLogs[0]`：**联表填充成功**
  - `archiveNo=ARC-000004`、`title="2003年老城区改造影像资料"`（清单①）
- `stats`：
  - openArchiveCount/electronicFileCount/collectionCount/latestOpenCount 与 /public/stats **完全一致**（口径交叉验证通过）
  - `myPendingCollections=1`（= collectionSummary.inProgress）
  - `myDownloadCount=1`
- `user`：{realName:"小周", phone:"13800000005", status:"active"}（清单③落点，直接查 users 表）

## 一致性交叉验证

- dashboard.stats.openArchiveCount(5) == /public/stats.openArchiveCount(5) ✓
- myPendingCollections(1) == collectionSummary.inProgress(1) ✓

## 观察（既有行为，非本次改动）

`POST /api/auth/login` 响应的 `user` 未填充 phone/status（AuthService.login 只 set realName/roles/maxSecurityLevel/dataScope），而 `GET /api/auth/me` 填充了 phone/status。前端 `getUserInfoApi` 调的是 `/auth/me`，故实际取得到 phone/status；dashboard 的 user 字段直接查 DB，不受此影响。本次不修改该既有行为。

## 结论

两个端点运行时链路正常，字段与口径符合设计。单测 6 个（PublicStatsServiceTest 4 + PublicDashboardServiceTest 2）+ 全量单测 226 个全绿（排除 ArchiveApplicationTests）。
