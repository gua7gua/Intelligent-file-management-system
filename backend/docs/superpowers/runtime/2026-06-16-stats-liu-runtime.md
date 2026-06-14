# feat/stats-liu 运行时验证（M12 盘点 / M13 编研 / M14 统计研判保存）

- 验证人：刘星（liu.back，back_archivist）
- 日期：2026-06-15
- 分支：`feat/stats-liu`
- 验证方式：启动后端（dev profile + `ai.enabled=true`，DEEPSEEK_API_KEY 已配置），以登录态对每个端点跑完整 HTTP 链路并记录请求/响应/断言。

## 0. 环境就绪

| 依赖 | 状态 |
|------|------|
| archive-postgres (5432) | Up healthy |
| archive-minio (9001/9002) | Up healthy |
| archive-clamav (3310) | Up healthy（`file.scan.enabled=false`，扫描器对禁用返回 `safe`） |
| DEEPSEEK_API_KEY | 已设置 |
| 后端 | `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments="-Dai.enabled=true"`，Tomcat:8080，`/actuator/health` = UP |

登录：`POST /api/auth/login {loginName:"liu.back",password:"123456",portal:"admin"}` → `code=OK`，roles=`["back_archivist"]`，token 获取成功。`GET /api/auth/me` 返回小刘(id=2, maxSecurityLevel=3, dataScope=all)。

## 1. M12 档案盘点

> 命中范围：room=1（401 库房）、categoryId=3，存在盒位档案（archive 3，盒位 401-01-01-01）。

**18.2 创建盘点任务：**
```
POST /api/admin/inventory-tasks {"taskName":"401库房会计档案盘点(重试)","roomId":1,"categoryId":3}
→ 200 data.taskNo=INV-000002, status=draft
  items=[{archiveId:3, boxId:1, expectedLocationId:1, checkResult:normal}], stats.normal=1
```
应盘明细按 room+category 正确生成，每档一条，`expected_location_id`/`box_id` 取自盒位。

**18.3 / 18.5 / 18.6 状态机 + 明细 + 完成（task 3）：**
```
POST .../3/start        → status: running
PUT  .../3/items/3 {checkResult:misplaced,...} → checkResult: misplaced
POST .../3/complete {"summary":"应盘1件 错位1件"}  → status: completed
GET  .../3  → stats:{normal:0,missing:0,misplaced:1,damaged:0,onLoan:0}
```

**M12↔M09 借阅暂停联动（真实数据复测）：**
- 预置：task 1（INV-000001）处于 `running`（room=1/cat=3）；archive 3 重置为可借（paper/normal/available）。
- `running` 期间：`POST /api/internal/borrow-requests {archiveId:3}`（internal_reader li.reader）
  → **409 `BUSINESS_CONFLICT`「档案所在架位/门类正在盘点，暂停借阅」** ✓
- `POST .../1/complete` 后再借 → **200 `OK`，status=applied，BRW-000002** ✓（恢复可借）

> 借阅暂停逻辑由 `BorrowEligibilityChecker`（M09）实现，读 `inventory_tasks(status=running, room_id, category_id)`；本分支只负责正确设置盘点状态，运行时确认联动生效。

## 2. M13 档案编研

素材：archive 4（ARC-000004，normal）；全宗：fonds 3（编研全宗）。
```
19.2 POST /api/admin/compilations {title:"克拉玛依城建专题编研", materialArchiveIds:[4], contentHtml:"<p>...</p>",...}
    → compilationId=2, status=draft
19.5 POST /api/admin/compilations/2/generate
    → status=generated, generatedFileAttachmentId=7（business_attachment type=compilation/report，PDF 已上 MinIO）
19.6 POST /api/admin/compilations/2/archive {fondsId:3, categoryId:1, formedDate:"2026-05-30", retentionPeriod:"permanent", openStatus:"open", tagNames:["编研成果","公开利用"]}
    → {archiveId:9}
```
DB 校验生成的正式档案：
```
archive 9 | ARC-000007 | source_type=compilation | source_compilation_id=2 | carrier=electronic
         | lifecycle=normal | body_role=compilation_body（archive_files 存在）| compilation.status=archived
```
档号由 `ArchiveNoUtil` 生成（ARC-000007）；正文复制为 `archive_files.file_role=compilation_body`，上传 MinIO 真实对象；标签 upsert 写入。编研成果按纯电子档案入库，状态 `generated→archived`。

## 3. M14 数据统计与导出

**20.1 总览：**
```
GET /api/admin/statistics/overview
→ totals:{totalArchives:5, openArchives:3, borrowCount:2, destroyedCount:0}
  trends:[{2026-06,collection,2},{2026-06,transfer,3}]
  storageUsage:[{401-01-01-01, used:1, capacity:30, rate:3.33%}, ...]
```
**20.2 分类统计：**
```
GET /api/admin/statistics/categories
→ byCategory:[{会计档案,3},{文书档案,1},{音像档案,1}]（另含 byYear/bySource/byCarrier/bySecurity/byOpenStatus）
```
**20.3 导出（两种格式，文件流）：**
```
GET .../export?format=xlsx → Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
                            字节 4303，魔数 "PK"（合法 xlsx/OOXML zip）✓
GET .../export?format=pdf  → Content-Type: application/pdf
                            字节 1925，魔数 "%PDF" ✓
```
统计数据从业务表实时聚合（无事实表），xlsx 经 Apache POI 5.5.1、pdf 经 openpdf 生成。

## 4. M14 数据研判（规则扫描 + AI 建议）

```
20.5 POST /api/admin/analysis-tasks {taskType:mixed, rule:{categoryIds:[1,3], formedYearStart:2010, formedYearEnd:2026, includeAiSuggestion:true}}
    → analysisTaskId=4, latestAiTaskId=8, status=running
（轮询 ai_tasks 8：running → completed）
```
研判项（analysisTaskId=4）：
```
 issue_type      | status  | suggestion
-----------------+---------+--------------------------------------------
 missing_field   | pending | （规则扫描：缺字段）
 tag_suggestion  | pending | {"suggestedTags":["财政改革","专题编研","新疆"]}
 tag_suggestion  | pending | {"suggestedTags":["城市建设","专题编研","新疆"]}
 tag_suggestion  | pending | {"suggestedTags":["年度预算","批复文件","财经管理"]}
 tag_suggestion  | pending | {"suggestedTags":["会计凭证","第一季度","财务档案"]}
 tag_suggestion  | pending | {"suggestedTags":["临时会计凭证","年度财务","历史档案"]}
```
规则扫描产出 1 个 missing_field 项；**真实 DeepSeek 写入 5 个 tag_suggestion**（只写 `analysis_items.suggestion`，**未修改正式档案字段**，符合 AI 安全边界）。
```
20.7 POST /api/admin/analysis-items/6/handle {action:adopted, note:"采纳标签建议"} → status: adopted
     （重复处理 → 409 BUSINESS_CONFLICT「研判项已处理」）
```

> **运行时发现并修复两处缺陷**（见 §6）：研判 AI 异步竞态、AiClient JSON 解析过严。修复前 AI 任务误判 failed（0 批次）；修复后端到端通过。

## 5. M14 档案保存（备份 + 四性检测）

**21.2 备份（三种 scope，立即可移植折中：JDBC 导出 + MinIO 复制 + sha256）：**
```
POST /api/admin/backup-tasks {backupScope:database}
  → BAK-000002, status=success, backupPath=./data/backups/BAK-000002-db.sql, fileSize=53445,
    sha256=3e5e220b...(64位) ✓
POST ... {backupScope:files}
  → status=success, message="文件复制 8 个"，sha256 ✓
POST ... {backupScope:both}
  → status=success, sha256 ✓
```
**21.4 四性检测（两种数据场景对照）：**

种子 archive_file 2（object_key 无真实 MinIO 对象）：
```
integrity    → failed（对象不存在→读取失败）
usability    → failed（对象不可读）
authenticity → not_configured（未接入签名体系）✓
security     → failed（扫描读取失败，不放行）✓
```
编研正文 archive_file 5（generate 上传的真实 PDF 对象）：
```
integrity    → passed（SHA-256 比对一致）✓
usability    → passed（对象可读）✓
authenticity → not_configured ✓
security     → passed（scan.enabled=false → ClamAV 返回 safe）
```
```
21.3 GET /api/admin/file-check-records?targetId=2 → total=5（按 target/type/result 过滤分页正常）
```
四性逻辑完整：真实对象 passed、缺失对象 failed、真实性恒 not_configured、安全性不降级放行。

## 6. 运行时验证发现并修复的缺陷

| # | 现象 | 根因 | 修复 |
|---|------|------|------|
| 1 | 研判 AI 任务创建后 10ms 内 failed、批次恒 pending、error_message 空 | `AnalysisService.create`（@Transactional）内触发 `@Async`，异步线程读不到未提交的 `ai_task_batches` → 0 批次 → `summarizeStatus(0,0)=failed` | `AiTaskService.startArchiveAnalysis` 不再触发异步；新增 `startArchiveAnalysisAsync`，由 `AnalysisService` 在事务 `afterCommit` 回调触发（既有 intake 补全从 controller 调用、无外层事务，不受影响） |
| 2 | DeepSeek 返回有效 JSON（含 suggestedTags）却被判「未包含有效 JSON 块」 | `AiClient.extractJsonBlock` 强制要求 `<JSON>...</JSON>` 包裹；DeepSeek 常省略包裹直接返回裸 JSON | 缺失包裹时回退取首 `{` 到末 `}` 的裸 JSON（增量容错，包裹行为不变；研判与清单补全均受益） |

修复提交：`38b0620 fix(analysis): 修复研判 AI 异步竞态与 JSON 解析容错`。

## 7. 测试结论

- **单元/集成测试**：`./mvnw test` 共 184 个，**183 通过**。新增 10 个测试类（4 NoUtil + InventoryService/BackupService/FileCheckService/StatisticsService/AnalysisService/CompilationService）全绿；改动 `AiClient`/`AiTaskService`/`AiTaskAsyncRunner` 后 AI 相关 28 个既有测试无回归。
- **唯一未通过**：`ArchiveApplicationTests.contextLoads`（裸 `@SpringBootTest`，默认 profile 无 datasource）。**此为 develop 既有问题，与本分支无关**（仅新增 bean，未动 datasource）；加 `-Dspring.profiles.active=dev` + DB 后 `contextLoads` 通过，证明全部新 bean 装配无误。
- **运行时验证**：M12/M13/M14 共 ~20 端点全部以登录态跑通（见 §1-§5），含借阅暂停联动、编研入库、xlsx/pdf 导出、真实 AI 标签建议、备份产物 sha256、四性 passed/failed/not_configured 对照。

## 8. 交付门禁

按用户要求：单元测试 + 运行时测试通过后，**等待用户检查**，确认后再 `git push` 与发起 PR。



