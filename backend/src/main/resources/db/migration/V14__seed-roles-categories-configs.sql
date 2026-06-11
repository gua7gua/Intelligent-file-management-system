-- 固定字典和系统配置种子数据

INSERT INTO categories (category_code, category_name, enabled) VALUES
  ('document', '文书档案', true),
  ('technology', '科技档案', true),
  ('accounting', '会计档案', true),
  ('audio_video', '音像档案', true),
  ('personnel', '人事档案', true);

INSERT INTO roles (role_code, role_name, description, enabled) VALUES
  ('front_archivist', '档案管理员（前台）', '接待移交/捐赠、清点核对、电子文件上传、回执导出、借阅出库归还', true),
  ('back_archivist', '档案管理员（后台）', 'AI 补全、装盒入库上架、库房管理、鉴定销毁、借阅审批、统计研判', true),
  ('transfer_user', '移交单位经办人', '编制移交清单、查看移交状态、导出移交清单', true),
  ('internal_reader', '内部查阅者', '检索档案、在线预览、下载、申请纸质借阅', true),
  ('public_user', '社会公众', '注册登录、公开档案检索、提交征集捐赠意向', true),
  ('director', '馆领导', '密级调整、开放调整和销毁审批', true),
  ('sys_admin', '系统管理员', '用户、角色、组织、全宗、系统配置和日志管理', true);

INSERT INTO system_configs (config_key, config_value, value_type, description, editable) VALUES
  ('upload.allowed_extensions', '["pdf","doc","docx","jpg","jpeg","png","mp4"]', 'json', 'Allowed upload extensions for staging files and business attachments.', true),
  ('upload.max_file_size_mb', '200', 'number', 'Maximum upload file size in MB.', true),
  ('ai.enabled', 'true', 'boolean', 'Global AI feature switch.', true),
  ('ai.batch_size', '50', 'number', 'Default batch size for AI completion and analysis tasks.', true),
  ('ai.search_max_retries', '3', 'number', 'Maximum AI JSON regeneration attempts for search query generation.', true),
  ('public_search.enabled', 'true', 'boolean', 'Public archive search feature switch.', true),
  ('warehouse.usage_warning_threshold', '0.85', 'number', 'Warehouse capacity warning threshold.', true),
  ('borrow.default_days', '7', 'number', 'Default borrow days.', true),
  ('collection.agreement_text', '本人确认自愿向档案馆提交征集线索或捐赠意向，并同意档案馆按规定联系、审核、接收和长期保存相关档案材料。', 'string', 'Online donation agreement text shown before public collection submission.', true),
  ('collection.max_pending_batches_per_user', '5', 'number', 'Maximum pending public collection batches per user.', true),
  ('collection.max_items_per_batch', '200', 'number', 'Maximum items in one public collection batch.', true),
  ('collection.max_pending_or_rejected_batches_per_user', '10', 'number', 'Maximum pending plus rejected public collection batches per user.', true);
