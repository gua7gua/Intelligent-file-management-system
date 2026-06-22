-- 鉴定批次新增「到期窗口天数」字段（D4）：废弃形成年度起/止筛选，改按保管期限到期窗口圈定档案。
-- due_days 记录建批次时填的 N（命中 retention_until <= 今天 + N 天，含已过期未处理）。
-- 保留 formed_year_start/end 旧列以兼容存量批次，新建批次该两列为 NULL。
ALTER TABLE appraisal_batches ADD COLUMN IF NOT EXISTS due_days INTEGER;
