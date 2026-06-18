-- 系统配置 description 汉化（仅改 description，保留 config_value / value_type / editable 不变）
-- 覆盖 V15 种子的全部 13 条英文描述

UPDATE system_configs SET description = '暂存文件与业务附件允许的上传扩展名白名单。'
WHERE config_key = 'upload.allowed_extensions';

UPDATE system_configs SET description = '单个上传文件的最大体积（单位：MB）。'
WHERE config_key = 'upload.max_file_size_mb';

UPDATE system_configs SET description = 'AI 功能总开关。'
WHERE config_key = 'ai.enabled';

UPDATE system_configs SET description = 'AI 补全与数据研判任务的默认批大小。'
WHERE config_key = 'ai.batch_size';

UPDATE system_configs SET description = '检索词转结构化查询时 AI JSON 重生成的最大重试次数。'
WHERE config_key = 'ai.search_max_retries';

UPDATE system_configs SET description = '公众端档案检索功能开关。'
WHERE config_key = 'public_search.enabled';

UPDATE system_configs SET description = '库房容量告警阈值。'
WHERE config_key = 'warehouse.usage_warning_threshold';

UPDATE system_configs SET description = '借阅默认天数。'
WHERE config_key = 'borrow.default_days';

UPDATE system_configs SET description = '公众提交征集捐赠前需勾选确认的线上协议文本。'
WHERE config_key = 'collection.agreement_text';

UPDATE system_configs SET description = '单个用户同时持有的待审核征集批次上限。'
WHERE config_key = 'collection.max_pending_batches_per_user';

UPDATE system_configs SET description = '单个征集批次可包含的条目上限。'
WHERE config_key = 'collection.max_items_per_batch';

UPDATE system_configs SET description = '单个用户同时持有的待审核或已驳回征集批次上限。'
WHERE config_key = 'collection.max_pending_or_rejected_batches_per_user';
