# feat/appraisal-zhou 运行时验证记录

- 日期：2026-06-15
- 环境：dev profile + Docker（postgres:5432 / minio:9001 / clamav:3310）
- 应用：`SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run`（8080），登录 `admin/123456` portal `admin`，token 经 `Authorization: Bearer <token>` 传递

## 发现并修复的缺陷（单测无法覆盖，运行时暴露）

1. **`appraisal_items.appraisal_result NOT NULL` 与契约冲突**
   - 现象：创建鉴定批次（14.2，命中到期档案）→ INTERNAL_ERROR，DB 报 `null value in column "appraisal_result" violates not-null constraint`。
   - 根因：V8 建表将 `appraisal_result` 设为 `NOT NULL CHECK(...)`；但接口 14.2 创建批次即生成命中明细（结果为空），14.4 才填 extend/destroy。
   - 修复：V18 迁移 `ALTER TABLE appraisal_items ALTER COLUMN appraisal_result DROP NOT NULL`（CHECK 保留，NULL 表示未鉴定）。

2. **`DestructionList.supervisorName1/2` 列映射错误**
   - 现象：销毁清册详情/确认销毁 → INTERNAL_ERROR，DB 报 `column "supervisor_name1" does not exist`（实际列 `supervisor_name_1`）。
   - 根因：MyBatis-Plus 驼峰转下划线将 `supervisorName1` 映射为 `supervisor_name1`，与 DB 列 `supervisor_name_1`（数字前有下划线）不符。
   - 修复：实体加 `@TableField("supervisor_name_1")` / `@TableField("supervisor_name_2")`。

## 闭环端到端验证（修复后）

| 步骤 | 接口 | 结果 |
|------|------|------|
| 创建鉴定批次 | POST /api/admin/appraisal-batches | APP-000005，命中档案 1、2 |
| 保存明细 | PUT .../items | 档1=destroy、档2=extend 30y |
| 完成鉴定 | POST .../complete | 批次 completed；档2 保管期 10y→30y、到期 2055-12-31、写 change_log；档1 pending_destruction；生成 DES-000002 |
| 清册详情 | GET .../destruction-lists/2 | 含明细+审批摘要+照片 |
| 上传现场照片 | POST .../photos | ClamAV 扫描通过→MinIO 上传→business_attachments 落库（sha256 计算） |
| 提交审批 | POST .../submit-approval | 清册 pending_approval，创建 destruction 审批单 |
| 审批通过 | POST .../approvals/{id}/approve | approval approved，清册 pending_destroy |
| 确认销毁 | POST .../destroy | 清册 destroyed；档1 lifecycle/condition=destroyed；电子文件 file_status=deleted；审计落库 |

辅助端点：角色查询（7 项）、系统配置（脱敏/单更合法/非法 number 校验/批量）、审批列表、用户禁用最后 sys_admin 拦截——均符合预期。

## 序列

V17 `seq_appraisal_batch_no` setval 生效：演示数据 `APP-000001` 之后首个真实批次为 `APP-000002`，无唯一索引冲突。

## 未覆盖

- 密级/开放调整审批（10.4/10.5 发起端由 M05 创建）的运行时生效，仅单测覆盖（演示数据无 pending 的 security/open 审批单）。
- 默认 profile 下 `ArchiveApplicationTests` 因无数据源配置失败（仅 dev profile 可跑），属预存环境依赖。
