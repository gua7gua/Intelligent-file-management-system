-- 鉴定明细 appraisal_result 允许为空
-- 理由：接口 14.2 创建鉴定批次时即生成命中档案明细（appraisal_result 暂为空），
-- 14.4 保存鉴定明细时才填写 extend/destroy 结论。V8 建表将该列设为 NOT NULL，
-- 与「先命中、后鉴定」的流程契约冲突，导致批次创建插入空结果明细时违反约束。
-- CHECK 约束保留：非空值仍必须为 extend/destroy；NULL 表示尚未鉴定。
ALTER TABLE appraisal_items ALTER COLUMN appraisal_result DROP NOT NULL;
