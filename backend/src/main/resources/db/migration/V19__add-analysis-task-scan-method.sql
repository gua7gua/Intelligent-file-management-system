-- B10 方案 A：分离「扫描方式」与「结果类型」
-- task_type 仍保留结果类型语义（missing_fields/category_conflict/mixed），
-- 新增 scan_method 记录用户选择的扫描方式（rule/ai/mixed）。
ALTER TABLE analysis_tasks ADD COLUMN scan_method varchar(30);

ALTER TABLE analysis_tasks ADD CONSTRAINT analysis_tasks_scan_method_check
    CHECK (scan_method IS NULL OR scan_method IN ('rule','ai','mixed'));

-- 回填历史：现有 4 条 task_type=mixed，扫描方式视为综合
UPDATE analysis_tasks SET scan_method = 'mixed' WHERE scan_method IS NULL;
