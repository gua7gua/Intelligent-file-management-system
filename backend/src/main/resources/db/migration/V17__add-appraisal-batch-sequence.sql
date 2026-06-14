-- 鉴定批次号序列（编号规范 APP-{自增序号}，V14 缺失，本迁移补齐）
CREATE SEQUENCE IF NOT EXISTS seq_appraisal_batch_no START WITH 1;

-- 对齐已有演示/种子数据（如 APP-000001），避免 uk_appraisal_batches_batch_no 唯一索引冲突
SELECT setval('seq_appraisal_batch_no',
  GREATEST(1, COALESCE(
    (SELECT MAX(CAST(SUBSTRING(batch_no FROM '[0-9]+$') AS bigint)) FROM appraisal_batches),
    0)));
