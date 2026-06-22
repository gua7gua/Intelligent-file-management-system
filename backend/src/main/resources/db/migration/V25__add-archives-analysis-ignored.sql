-- V25: 档案新增「研判永久跳过」时间戳。
-- 语义：数据研判中「不采纳」某条异常建议时，认为该档案无问题，标记后下次扫描不再纳入候选；
-- 「删除」仅软删当前异常项，不置本字段，下次扫描仍可再次发现。NULL = 未跳过。
ALTER TABLE archives ADD COLUMN IF NOT EXISTS analysis_ignored_at TIMESTAMPTZ;
