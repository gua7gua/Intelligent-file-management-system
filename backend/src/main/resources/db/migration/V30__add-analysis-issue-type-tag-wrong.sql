-- V30: analysis_items.issue_type 增加 tag_wrong（标签错配）类型
-- 用于数据研判 AI 发现「档案标签与门类明显冲突」时写入的异常项类型。
-- 原 CHECK 约束在 V10 内联定义（自动命名 analysis_items_issue_type_check），此处替换。

ALTER TABLE analysis_items DROP CONSTRAINT IF EXISTS analysis_items_issue_type_check;
ALTER TABLE analysis_items ADD CONSTRAINT analysis_items_issue_type_check
  CHECK (issue_type IN ('missing_field','category_conflict','tag_suggestion','tag_wrong'));
