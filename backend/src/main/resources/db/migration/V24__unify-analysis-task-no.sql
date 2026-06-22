-- R3-D3 统一研判任务编号前缀：历史遗留 ANL-000001，新生成均 ANA-YYYYMM-NNNN（AnalysisTaskNoUtil）。
-- 将存量 ANL- 前缀替换为 ANA-，消除编号体系不一致（方江苏 06-19 拍板）。
-- 仅替换前 3 个字符（ANL→ANA），保留后续序号；现有 ANA-202606-0002/0003 不受影响，无唯一冲突。
UPDATE analysis_tasks SET task_no = OVERLAY(task_no PLACING 'ANA' FROM 1 FOR 3)
WHERE task_no LIKE 'ANL-%';
