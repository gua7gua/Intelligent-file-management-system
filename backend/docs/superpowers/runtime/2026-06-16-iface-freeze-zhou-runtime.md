# 接口冻结前 — 运行时体检（全量 HTTP 链路 + 写接口 E2E）

- 验证人：周扬（后端）
- 日期：2026-06-16
- 基线分支：`develop` @ `b91fcfa`
- 验证方式：dev profile 启动后端（带 `--ai.enabled=true`），各门户登录态对每个端点跑真实 HTTP 链路；写接口做完整 happy-path E2E（创建→状态流转→清理）。
- 目的：接口冻结二次检测，确认运行时层面可冻结。

## 0. 环境

postgres(5432)/minio(9001/9002)/clamav(3310) 均 Up healthy；后端 Tomcat:8080 `/actuator/health`=UP；`DEEPSEEK_API_KEY` 在环境中。

## 1. 范围与深度

- 覆盖 137 端点，排除短信 3 个（sms-code/register/reset-password），**实测 134 个**。
- **写接口全部做了 E2E happy-path**（早期"仅错误链路"的版本已补齐）：对每个写接口用正确 body + 合法前置状态驱动，验证 200 落库，再用临时数据避免污染 seed。

| 模块 | E2E 验证的写接口（happy-path） |
|------|-------------------------------|
| 移交 | create / item.add-upd-del / batch.upd / batch.del / submit |
| 接收 | staging.upload(multipart) / staging.match / item.acceptance / batch.complete / receipt |
| 待归档 | item.confirm / item.archive(建档案) / batch.shelve / ai-completion(真实AI) |
| 鉴定 | create / items.save / complete |
| 库房 | room.upd / box.create / box.move / loc.status |
| 盘点 | create / start / item.upd / complete |
| 编研 | create / generate(PDF) / archive(建档案) |
| 分析 | create / item.handle |
| 备份 | create(database scope 实跑) |
| 文件检测 | trigger(integrity/usability) |
| 档案调整 | security-adjustments / open-adjustments |
| 公众征集 | create / upd / submit / admin.schedule / admin.reject |
| 借阅 | create / approve / checkout / return / voucher |
| 审批 | approve / reject |
| 销毁 | submit-approval / photos / destroy(完整链) |

> 全量单测：`mvn test -Dtest='!ArchiveApplicationTests'` → **221 通过 0 失败**。

## 2. 发现并修复的 Bug（共 3 个，均经 E2E 暴露）

### Bug 1：非法枚举值导致 500（系统性）
`GlobalExceptionHandler` 未捕获 `IllegalArgumentException`；全仓 ~10 处 `Enum.valueOf(用户输入)` 无校验，任一非法枚举值 → 500。
**修复**：新增 `@ExceptionHandler(IllegalArgumentException.class) → 400`（GlobalExceptionHandler.java，+13 行）。一处修复覆盖全仓。验证：错误枚举值→400，正确值→200。

### Bug 2：鉴定"延期永久"未清 retention_until → 约束违反 500
`AppraisalService.completeBatch` 延期为 `permanent` 时设 `retention_until=null`，但 MyBatis-Plus `updateById` 默认跳过 null 字段，DB 残留旧值 → 违反 `ck_archives_retention_until`（permanent 须 NULL）→ 500。生产中到期 10y/30y 档案延期为永久必触发。
**修复**：extend 分支改用 `UpdateWrapper.set(...)` 显式写 retention_period/retention_until（AppraisalService.java:292-309），同步更新 AppraisalServiceTest。验证：complete→OK，archive 落库 permanent/retention_until=NULL。

### Bug 3：审批 approve 写变更日志 change_source 非法 → 约束违反 500
`ApprovalService.applySecurityAdjust/applyOpenAdjust` 把 `change_source` 写成 `"security_adjust"`/`"open_adjust"`（approval_type），但 `archive_change_logs_change_source_check` 仅允许 `manual_edit/approval/appraisal/destruction/ai_confirmed` → 500。审批通过密级/公开调整必触发。
**修复**：两处 change_source 改为 `"approval"`（ApprovalService.java:173,182）。验证：approve→OK，archive 密级正确生效。

三个修复均**单测零回归**（221/221）。

## 3. 测试方法问题（非 Bug）

- 首轮 2 个 500（reception/destruction 上传）= 我用 JSON 调 multipart 端点；改 multipart 后干净 404。
- 写接口首轮大量 400 = DTO 严格字段校验（`Unrecognized field`），正确 body 后 200。
- `retentionPeriod` 合法 API 值是枚举常量名 `_10y`/`_30y`/`permanent`（注意数字前缀下划线），非 `10y`/`30y`——非 bug，但前端需注意。
- checkout 的 voucherNo 在 approve 时自动生成，checkout 须传**同一个**（非 bug）。
- 借阅需纸质载体档案；seed 无 security≤1 的纸质档案供 internal_reader 借，E2E 用 smoke 纸质档案完成。

## 4. AI 通路（已实测，纠正初版误判）

`DEEPSEEK_API_KEY` 在环境中；AI 不可用是因 `ai.enabled` 在 application.yml 硬编码 false、dev 未覆盖、无 `${AI_ENABLED}` 开关。以 `--ai.enabled=true` 启动后，真正调 AI 的 3 个端点 happy-path 全通过：
- 公众/内部 `archives/ai-query` → 真实 DeepSeek，200，~2s。
- `pending-archive ai-completion` → 对 seed batch 2 触发，ai_task 异步 completed，raw_response 捕获真实 AI 返回、ai_suggestion 落库（验证后已还原 seed）。

`compilations/{id}/generate` **不是 AI 端点**（把 contentHtml 渲染成 PDF 的文档生成），已实测 200。

## 5. 鉴权与角色（确认正确）
sys_admin 仅系统域（用户/角色/配置/组织/日志/备份），业务模块需 back_archivist/front_archivist/director——**设计如此**，各端点对正确角色 200、错误角色 403。

## 6. 数据清理状态（✓ 已完成，用户授权后执行）
E2E 测试产生的 smoke 数据已**全部清理**：DB 按依赖序事务删除（archives 12/13、批次 BAT-000010/13/14、鉴定批次 7-11、盒 5、盘点 5/6、编研 5、分析 6、借阅 6、销毁清册 3、备份 5、审批 5/6/7、staging 14、附件 9/10、file_check 12/13、change_logs 等）+ MinIO 5 个对象 + 备份文件 BAK-000005-db.sql + box2 used_count 还原为 seed 值 1。全库 smoke 残留 = 0。
seed 完好：archives 1-5（+历史会话 8/9）、intake_batches 1-5、approval_requests 1/2 等均保留；历史会话遗留（archive 8/9、BAT-000005）非本次产生，未动。

## 7. 冻结结论
- 134 接口路由/鉴权/错误处理链路全部正常；**写接口已全部 E2E happy-path 验证**。
- 发现并修复 3 个真 bug（均生产必触发），单测 221/221 零回归。
- 待办：①授权后清理 smoke DB 数据；②冻结前确认环境 `ai.enabled` 已开。
- **运行时层面，接口可冻结。**
