-- 演示数据
-- 密码 123456 的 BCrypt hash: $2b$10$KWYteJvqKothYhf6Mr56..dcEY6.4Y5qy73DKC/2RIOUwCRkFJSS2

INSERT INTO organizations (id, org_name, org_type, contact_name, contact_phone, status) VALUES
  (1, '克拉玛依市档案馆', 'archive_org', '馆办公室', '0512-66000000', 'active'),
  (2, '克拉玛依市财政局', 'government', '张伟', '13800000003', 'active'),
  (3, '克拉玛依城建集团有限公司', 'enterprise', '王敏', '13800000008', 'active'),
  (4, '克拉玛依市教育科学研究院', 'public_institution', '赵老师', '13800000009', 'active');

INSERT INTO users (
  id, user_type, login_name, employee_no, phone, password_hash, real_name,
  organization_id, department_name, max_security_level, data_scope, status
) VALUES
  (1, 'internal', 'chen.front', 'A001', '13800000001', '$2b$10$KWYteJvqKothYhf6Mr56..dcEY6.4Y5qy73DKC/2RIOUwCRkFJSS2', '小陈', 1, '接收服务部', 2, 'all', 'active'),
  (2, 'internal', 'liu.back', 'A002', '13800000002', '$2b$10$KWYteJvqKothYhf6Mr56..dcEY6.4Y5qy73DKC/2RIOUwCRkFJSS2', '小刘', 1, '档案管理部', 3, 'all', 'active'),
  (3, 'internal', 'zhang.transfer', 'F001', '13800000003', '$2b$10$KWYteJvqKothYhf6Mr56..dcEY6.4Y5qy73DKC/2RIOUwCRkFJSS2', '小张', 2, '财务处', 0, 'own_org', 'active'),
  (4, 'internal', 'li.reader', 'F002', '13800000004', '$2b$10$KWYteJvqKothYhf6Mr56..dcEY6.4Y5qy73DKC/2RIOUwCRkFJSS2', '小李', 2, '预算处', 1, 'own_org', 'active'),
  (5, 'public', 'zhou.public', NULL, '13800000005', '$2b$10$KWYteJvqKothYhf6Mr56..dcEY6.4Y5qy73DKC/2RIOUwCRkFJSS2', '小周', NULL, NULL, 0, 'own_org', 'active'),
  (6, 'internal', 'fang.director', 'L001', '13800000006', '$2b$10$KWYteJvqKothYhf6Mr56..dcEY6.4Y5qy73DKC/2RIOUwCRkFJSS2', '小方', 1, '馆领导', 4, 'all', 'active'),
  (7, 'internal', 'admin', 'S001', '13800000007', '$2b$10$KWYteJvqKothYhf6Mr56..dcEY6.4Y5qy73DKC/2RIOUwCRkFJSS2', '系统管理员', 1, '信息中心', 0, 'all', 'active'),
  (8, 'internal', 'wang.transfer', 'C001', '13800000008', '$2b$10$KWYteJvqKothYhf6Mr56..dcEY6.4Y5qy73DKC/2RIOUwCRkFJSS2', '王敏', 3, '综合办公室', 0, 'own_org', 'active');

INSERT INTO user_roles (user_id, role_id)
SELECT 1, id FROM roles WHERE role_code = 'front_archivist'
UNION ALL SELECT 2, id FROM roles WHERE role_code = 'back_archivist'
UNION ALL SELECT 3, id FROM roles WHERE role_code = 'transfer_user'
UNION ALL SELECT 4, id FROM roles WHERE role_code = 'internal_reader'
UNION ALL SELECT 5, id FROM roles WHERE role_code = 'public_user'
UNION ALL SELECT 6, id FROM roles WHERE role_code = 'director'
UNION ALL SELECT 7, id FROM roles WHERE role_code = 'sys_admin'
UNION ALL SELECT 8, id FROM roles WHERE role_code = 'transfer_user';

INSERT INTO fonds (id, fonds_no, fonds_name, organization_id, description, status) VALUES
  (1, 'F001', '克拉玛依市财政局全宗', 2, '财政预算、决算、会计凭证和财务管理档案。', 'active'),
  (2, 'F002', '克拉玛依城建集团全宗', 3, '城市建设、项目资料和影像档案。', 'active'),
  (3, 'F003', '克拉玛依市档案馆编研全宗', 1, '馆内编研成果及专题材料。', 'active');

INSERT INTO tags (id, tag_name) VALUES
  (1, '财政'),
  (2, '会计凭证'),
  (3, '预算决算'),
  (4, '城建'),
  (5, '规划审批'),
  (6, '社会捐赠'),
  (7, '影像资料'),
  (8, '编研成果'),
  (9, '公开利用'),
  (10, '待鉴定');

INSERT INTO warehouse_rooms (
  id, room_no, room_name, rack_count, layers_per_rack, boxes_per_layer,
  capacity, warning_threshold, status, created_by
) VALUES
  (1, '401', '综合档案库房 A', 2, 2, 3, 12, 0.85, 'active', 2),
  (2, '402', '综合档案库房 B', 2, 2, 2, 8, 0.85, 'active', 2);

INSERT INTO storage_locations (id, room_id, rack_no, layer_no, box_slot_no, location_code, status)
SELECT
  row_number() OVER () AS id,
  r.id,
  rack_no,
  layer_no,
  slot_no,
  r.room_no || '-' || lpad(rack_no::text, 2, '0') || '-' || lpad(layer_no::text, 2, '0') || '-' || lpad(slot_no::text, 2, '0'),
  'active'
FROM warehouse_rooms r
CROSS JOIN LATERAL generate_series(1, r.rack_count) AS rack_no
CROSS JOIN LATERAL generate_series(1, r.layers_per_rack) AS layer_no
CROSS JOIN LATERAL generate_series(1, r.boxes_per_layer) AS slot_no
ORDER BY r.id, rack_no, layer_no, slot_no;

INSERT INTO archive_boxes (
  id, box_no, location_id, category_id, fonds_id, year_label, spine_text,
  capacity, used_count, status, created_by
) VALUES
  (1, 'BOX-000001', 1, (SELECT id FROM categories WHERE category_code = 'accounting'), 1, '2025', '财政局 2025 会计档案 1 盒', 30, 2, 'normal', 2),
  (2, 'BOX-000002', 2, (SELECT id FROM categories WHERE category_code = 'document'), 1, '2025', '财政局 2025 文书档案 1 盒', 30, 1, 'normal', 2),
  (3, 'BOX-000003', 3, (SELECT id FROM categories WHERE category_code = 'document'), 2, '2024', '城建集团 2024 文书档案 1 盒', 30, 1, 'normal', 2);

INSERT INTO intake_batches (
  id, batch_no, source_type, title, status, organization_id, department_name,
  public_user_id, contact_name, contact_phone, archive_year, expected_transfer_date,
  scheduled_receive_at, submitted_at, accepted_by, accepted_at, archived_at,
  shelved_at, reject_reason, agreement_accepted_at, created_by
) VALUES
  (1, 'BAT-000001', 'transfer', '2025 年度财政会计档案移交清单', 'shelved', 2, '财务处', NULL, '小张', '13800000003', 2025, DATE '2026-05-20', NULL, TIMESTAMPTZ '2026-05-10 09:00:00+08', 1, TIMESTAMPTZ '2026-05-20 10:00:00+08', TIMESTAMPTZ '2026-05-22 15:30:00+08', TIMESTAMPTZ '2026-05-23 09:30:00+08', NULL, NULL, 3),
  (2, 'BAT-000002', 'transfer', '2025 年度财政文书档案补交清单', 'partially_received', 2, '办公室', NULL, '小张', '13800000003', 2025, DATE '2026-06-15', NULL, TIMESTAMPTZ '2026-06-01 11:20:00+08', 1, TIMESTAMPTZ '2026-06-08 10:30:00+08', NULL, NULL, NULL, NULL, 3),
  (3, 'BAT-000003', 'collection', '小周家庭照片捐赠意向', 'pending_contact', NULL, NULL, 5, '小周', '13800000005', 1998, NULL, NULL, TIMESTAMPTZ '2026-06-06 14:20:00+08', NULL, NULL, NULL, NULL, NULL, TIMESTAMPTZ '2026-06-06 14:18:00+08', 5),
  (4, 'BAT-000004', 'collection', '城市建设老照片征集清单', 'archived', NULL, NULL, 5, '小周', '13800000005', 2003, NULL, TIMESTAMPTZ '2026-05-12 10:00:00+08', TIMESTAMPTZ '2026-05-01 09:00:00+08', 1, TIMESTAMPTZ '2026-05-12 11:00:00+08', TIMESTAMPTZ '2026-05-15 16:00:00+08', NULL, NULL, TIMESTAMPTZ '2026-05-01 08:58:00+08', 5);

INSERT INTO intake_items (
  id, batch_id, item_no, status, input_title, page_count, retention_period,
  carrier_status, security_level, open_status, allow_digitization,
  electronic_format, expected_filename, formed_date, acceptance_note,
  reject_reason, file_match_status, ai_suggestion, confirmed_title,
  confirmed_responsible_text, confirmed_formed_date, confirmed_category_id,
  confirmed_tags, created_by
) VALUES
  (1, 1, 1, 'archived', '2025 年第一季度会计凭证', 128, '30y', 'paper_electronic', 1, 'closed', true, 'PDF', '2025Q1-accounting-vouchers.pdf', DATE '2025-03-31', '纸质齐全，扫描件匹配通过。', NULL, 'matched', '{"title":"2025 年第一季度会计凭证","responsibleText":"克拉玛依市财政局财务处","categoryCode":"accounting","tags":["财政","会计凭证"]}', '2025 年第一季度会计凭证', '克拉玛依市财政局财务处', DATE '2025-03-31', (SELECT id FROM categories WHERE category_code = 'accounting'), '["财政","会计凭证"]', 3),
  (2, 1, 2, 'archived', '2025 年度预算批复文件', 0, '30y', 'electronic', 0, 'open', false, 'PDF', '2025-budget-approval.pdf', DATE '2025-01-15', '电子文件检查通过。', NULL, 'matched', '{"title":"2025 年度预算批复文件","responsibleText":"克拉玛依市财政局预算处","categoryCode":"accounting","tags":["财政","预算决算"]}', '2025 年度预算批复文件', '克拉玛依市财政局预算处', DATE '2025-01-15', (SELECT id FROM categories WHERE category_code = 'accounting'), '["财政","预算决算","公开利用"]', 3),
  (3, 1, 3, 'archived', '2014 年度临时会计凭证', 90, '10y', 'paper', 0, 'closed', true, NULL, NULL, DATE '2014-12-31', '纸质档案齐全，已入库，进入到期鉴定范围。', NULL, 'none', NULL, '2014 年度临时会计凭证', '克拉玛依市财政局财务处', DATE '2014-12-31', (SELECT id FROM categories WHERE category_code = 'accounting'), '["财政","待鉴定"]', 3),
  (4, 2, 1, 'pending_archive', '2025 年度政府采购合同汇编', 76, '30y', 'paper', 0, 'closed', true, NULL, NULL, DATE '2025-12-20', '纸质通过，待后台补全入库。', NULL, 'none', '{"title":"2025 年度政府采购合同汇编","responsibleText":"克拉玛依市财政局办公室","categoryCode":"document","tags":["财政"]}', '2025 年度政府采购合同汇编', '克拉玛依市财政局办公室', DATE '2025-12-20', (SELECT id FROM categories WHERE category_code = 'document'), '["财政"]', 3),
  (5, 2, 2, 'rejected', '装订破损的会议纪要', 32, '10y', 'paper', 0, 'closed', true, NULL, NULL, DATE '2025-10-11', NULL, '装订破损，页码缺失，退回补正后下次移交。', 'none', NULL, NULL, NULL, NULL, NULL, NULL, 3),
  (6, 3, 1, 'pending_acceptance', '1998 年家庭照片一组', 0, 'permanent', 'electronic', 0, 'open', false, 'JPG', 'family-photos-1998.zip', DATE '1998-07-01', NULL, NULL, 'none', NULL, NULL, NULL, NULL, NULL, NULL, 5),
  (7, 4, 1, 'archived', '2003 年老城区改造影像资料', 0, 'permanent', 'electronic', 0, 'open', false, 'MP4', 'old-city-renewal-2003.mp4', DATE '2003-09-20', '捐赠协议已在线确认，文件安全检查通过。', NULL, 'matched', '{"title":"2003 年老城区改造影像资料","responsibleText":"小周","categoryCode":"audio_video","tags":["社会捐赠","影像资料"]}', '2003 年老城区改造影像资料', '小周', DATE '2003-09-20', (SELECT id FROM categories WHERE category_code = 'audio_video'), '["社会捐赠","影像资料","公开利用"]', 5);

INSERT INTO compilations (
  id, compilation_no, title, compilation_type, date_range_text, keywords,
  summary, content_html, status, created_by
) VALUES
  (1, 'CMP-000001', '克拉玛依财政改革专题编研', '专题汇编', '2014-2025', '财政、预算、会计档案', '围绕财政改革主题形成的编研成果。', '<p>克拉玛依财政改革专题编研示例正文。</p>', 'archived', 2);

INSERT INTO archives (
  id, archive_no, title, responsible_text, formed_date, formed_year, category_id,
  source_type, source_batch_id, source_item_id, source_compilation_id,
  organization_id, fonds_id, carrier_status, retention_period, retention_until,
  security_level, open_status, allow_digitization, lifecycle_status, loan_status,
  condition_status, archived_at, shelved_at, created_by
) VALUES
  (1, 'ARC-000001', '2025 年第一季度会计凭证', '克拉玛依市财政局财务处', DATE '2025-03-31', 2025, (SELECT id FROM categories WHERE category_code = 'accounting'), 'transfer', 1, 1, NULL, 2, 1, 'paper_electronic', '30y', DATE '2056-05-22', 1, 'closed', true, 'normal', 'on_loan', 'normal', TIMESTAMPTZ '2026-05-22 15:30:00+08', TIMESTAMPTZ '2026-05-23 09:30:00+08', 2),
  (2, 'ARC-000002', '2025 年度预算批复文件', '克拉玛依市财政局预算处', DATE '2025-01-15', 2025, (SELECT id FROM categories WHERE category_code = 'accounting'), 'transfer', 1, 2, NULL, 2, 1, 'electronic', '30y', DATE '2056-05-22', 0, 'open', false, 'normal', 'available', 'normal', TIMESTAMPTZ '2026-05-22 15:35:00+08', NULL, 2),
  (3, 'ARC-000003', '2014 年度临时会计凭证', '克拉玛依市财政局财务处', DATE '2014-12-31', 2014, (SELECT id FROM categories WHERE category_code = 'accounting'), 'transfer', 1, 3, NULL, 2, 1, 'paper', '10y', DATE '2036-05-22', 0, 'closed', true, 'pending_destruction', 'available', 'normal', TIMESTAMPTZ '2026-05-22 15:45:00+08', TIMESTAMPTZ '2026-05-23 09:30:00+08', 2),
  (4, 'ARC-000004', '2003 年老城区改造影像资料', '小周', DATE '2003-09-20', 2003, (SELECT id FROM categories WHERE category_code = 'audio_video'), 'collection', 4, 7, NULL, NULL, 2, 'electronic', 'permanent', NULL, 0, 'open', false, 'normal', 'available', 'normal', TIMESTAMPTZ '2026-05-15 16:00:00+08', NULL, 2),
  (5, 'ARC-000005', '克拉玛依财政改革专题编研', '克拉玛依市档案馆编研组', DATE '2026-05-30', 2026, (SELECT id FROM categories WHERE category_code = 'document'), 'compilation', NULL, NULL, 1, 1, 3, 'electronic', 'permanent', NULL, 0, 'open', false, 'normal', 'available', 'normal', TIMESTAMPTZ '2026-05-30 17:00:00+08', NULL, 2);

UPDATE intake_items SET generated_archive_id = 1 WHERE id = 1;
UPDATE intake_items SET generated_archive_id = 2 WHERE id = 2;
UPDATE intake_items SET generated_archive_id = 3 WHERE id = 3;
UPDATE intake_items SET generated_archive_id = 4 WHERE id = 7;
UPDATE compilations SET generated_archive_id = 5 WHERE id = 1;

INSERT INTO archive_files (
  id, archive_id, file_role, bucket_name, object_key, original_filename,
  file_ext, mime_type, file_size, sha256, scan_result, usability_result,
  file_status, created_by
) VALUES
  (1, 1, 'scan', 'archive-files', 'archive-files/ARC-000001/1/2025Q1-accounting-vouchers.pdf', '2025Q1-accounting-vouchers.pdf', 'pdf', 'application/pdf', 2048000, repeat('a', 64), 'safe', 'passed', 'normal', 2),
  (2, 2, 'original', 'archive-files', 'archive-files/ARC-000002/2/2025-budget-approval.pdf', '2025-budget-approval.pdf', 'pdf', 'application/pdf', 1024000, repeat('b', 64), 'safe', 'passed', 'normal', 2),
  (3, 4, 'original', 'archive-files', 'archive-files/ARC-000004/3/old-city-renewal-2003.mp4', 'old-city-renewal-2003.mp4', 'mp4', 'video/mp4', 52428800, repeat('c', 64), 'safe', 'passed', 'normal', 2),
  (4, 5, 'compilation_body', 'archive-files', 'archive-files/ARC-000005/4/suzhou-finance-compilation.pdf', 'suzhou-finance-compilation.pdf', 'pdf', 'application/pdf', 1536000, repeat('d', 64), 'safe', 'passed', 'normal', 2);

UPDATE compilations SET generated_archive_file_id = 4 WHERE id = 1;

INSERT INTO archive_tags (archive_id, tag_id) VALUES
  (1, 1), (1, 2),
  (2, 1), (2, 3), (2, 9),
  (3, 1), (3, 10),
  (4, 6), (4, 7), (4, 9),
  (5, 1), (5, 3), (5, 8), (5, 9);

INSERT INTO archive_box_items (
  id, box_id, archive_id, sort_no, page_count, physical_status, created_by
) VALUES
  (1, 1, 1, 1, 128, 'normal', 2),
  (2, 1, 3, 2, 90, 'normal', 2);

INSERT INTO staging_files (
  id, batch_id, item_id, upload_batch_no, original_filename, file_ext,
  mime_type, file_size, sha256, bucket_name, object_key, match_status,
  scan_result, scan_message, check_summary, uploaded_by, uploaded_at,
  archived_file_id
) VALUES
  (1, 1, 1, 'UP-20260520-001', '2025Q1-accounting-vouchers.pdf', 'pdf', 'application/pdf', 2048000, repeat('a', 64), 'archive-files', 'archive-files/_staging/transfer/1/1/2025Q1-accounting-vouchers.pdf', 'archived', 'safe', 'ClamAV scan passed.', '{"integrity":"passed","usability":"passed","security":"passed","authenticity":"not_configured"}', 1, TIMESTAMPTZ '2026-05-20 10:10:00+08', 1),
  (2, 1, 2, 'UP-20260520-001', '2025-budget-approval.pdf', 'pdf', 'application/pdf', 1024000, repeat('b', 64), 'archive-files', 'archive-files/_staging/transfer/1/2/2025-budget-approval.pdf', 'archived', 'safe', 'ClamAV scan passed.', '{"integrity":"passed","usability":"passed","security":"passed","authenticity":"not_configured"}', 1, TIMESTAMPTZ '2026-05-20 10:11:00+08', 2),
  (3, 2, NULL, 'UP-20260608-001', 'extra-material.pdf', 'pdf', 'application/pdf', 512000, repeat('e', 64), 'archive-files', 'archive-files/_staging/transfer/2/3/extra-material.pdf', 'unmatched', 'safe', 'ClamAV scan passed, but no intake item matched.', '{"integrity":"passed","usability":"passed","security":"passed"}', 1, TIMESTAMPTZ '2026-06-08 10:40:00+08', NULL);

INSERT INTO ai_tasks (
  id, task_no, task_type, business_type, business_id, status, batch_size,
  total_batches, success_batches, failed_batches, started_at, completed_at,
  created_by
) VALUES
  (1, 'AIT-000001', 'intake_completion', 'intake_batch', 1, 'completed', 50, 1, 1, 0, TIMESTAMPTZ '2026-05-21 09:00:00+08', TIMESTAMPTZ '2026-05-21 09:00:08+08', 2),
  (2, 'AIT-000002', 'intake_completion', 'intake_batch', 2, 'partial_completed', 50, 1, 0, 1, TIMESTAMPTZ '2026-06-08 15:00:00+08', TIMESTAMPTZ '2026-06-08 15:00:15+08', 2),
  (3, 'AIT-000003', 'archive_analysis', 'analysis_task', 1, 'completed', 50, 1, 1, 0, TIMESTAMPTZ '2026-06-03 09:00:00+08', TIMESTAMPTZ '2026-06-03 09:00:12+08', 2);

INSERT INTO ai_task_batches (
  id, task_id, batch_no, status, target_ids, request_context, raw_response,
  validated_result, error_message, attempt_count, started_at, completed_at
) VALUES
  (1, 1, 1, 'success', '[1,2,3]', '{"source":"intake_completion","fields":["title","responsibleText","formedDate","category","tags"]}', '<JSON>{"items":[{"id":1},{"id":2},{"id":3}]}</JSON>', '{"items":[{"id":1},{"id":2},{"id":3}]}', NULL, 1, TIMESTAMPTZ '2026-05-21 09:00:00+08', TIMESTAMPTZ '2026-05-21 09:00:08+08'),
  (2, 2, 1, 'failed', '[4]', '{"source":"intake_completion"}', '<JSON>{"items":[{"id":4,"securityLevel":4}]}</JSON>', NULL, 'AI returned protected field securityLevel.', 1, TIMESTAMPTZ '2026-06-08 15:00:00+08', TIMESTAMPTZ '2026-06-08 15:00:15+08'),
  (3, 3, 1, 'success', '[1,2,3,4,5]', '{"source":"archive_analysis","ruleType":"mixed"}', '<JSON>{"items":[{"archiveId":2},{"archiveId":3}]}</JSON>', '{"items":[{"archiveId":2},{"archiveId":3}]}', NULL, 1, TIMESTAMPTZ '2026-06-03 09:00:00+08', TIMESTAMPTZ '2026-06-03 09:00:12+08');

UPDATE intake_batches SET latest_ai_task_id = 1 WHERE id = 1;
UPDATE intake_batches SET latest_ai_task_id = 2 WHERE id = 2;

INSERT INTO business_attachments (
  id, business_type, business_id, attachment_type, bucket_name, object_key,
  original_filename, file_ext, mime_type, file_size, sha256, scan_result,
  scan_message, file_status, uploaded_by
) VALUES
  (1, 'intake_batch', 1, 'transfer_pdf', 'archive-metadata', 'intake/BAT-000001/transfer-list.pdf', 'BAT-000001-移交清单.pdf', 'pdf', 'application/pdf', 280000, repeat('1', 64), 'safe', 'System generated PDF.', 'normal', 3),
  (2, 'intake_batch', 1, 'receipt_pdf', 'archive-metadata', 'intake/BAT-000001/receipt.pdf', 'BAT-000001-接收回执.pdf', 'pdf', 'application/pdf', 260000, repeat('2', 64), 'safe', 'System generated PDF.', 'normal', 1),
  (3, 'compilation', 1, 'report', 'archive-metadata', 'compilation/CMP-000001/draft.pdf', '克拉玛依财政改革专题编研.pdf', 'pdf', 'application/pdf', 1536000, repeat('3', 64), 'safe', 'System generated PDF.', 'normal', 2);

INSERT INTO borrow_requests (
  id, request_no, archive_id, borrower_id, reason, expected_days,
  expected_visit_at, contact_phone, status, approved_by, approved_at,
  reject_reason, voucher_no, voucher_issued_at, checked_out_by,
  checked_out_at, due_at, returned_by, returned_at, return_check_result,
  return_note, created_by
) VALUES
  (1, 'BRW-000001', 1, 4, '财政预算核查需要查阅原件。', 7, TIMESTAMPTZ '2026-06-12 09:30:00+08', '13800000004', 'checked_out', 2, TIMESTAMPTZ '2026-06-09 10:00:00+08', NULL, 'VCH-000001', TIMESTAMPTZ '2026-06-09 10:05:00+08', 1, TIMESTAMPTZ '2026-06-10 09:20:00+08', TIMESTAMPTZ '2026-06-17 09:20:00+08', NULL, NULL, NULL, NULL, 4);

INSERT INTO appraisal_batches (
  id, batch_no, batch_name, category_id, formed_year_start, formed_year_end,
  status, completed_at, created_by
) VALUES
  (1, 'APP-000001', '2026 年到期会计档案鉴定批次', (SELECT id FROM categories WHERE category_code = 'accounting'), 2014, 2014, 'completed', TIMESTAMPTZ '2026-06-02 16:00:00+08', 2);

INSERT INTO appraisal_items (
  id, batch_id, archive_id, appraisal_result, new_retention_period,
  new_retention_until, opinion, appraised_by, appraised_at
) VALUES
  (1, 1, 3, 'destroy', NULL, NULL, '已满十年且无继续保存价值，建议列入销毁清册。', 2, TIMESTAMPTZ '2026-06-02 15:50:00+08');

INSERT INTO destruction_lists (
  id, list_no, list_name, appraisal_batch_id, status, approval_request_id,
  destroyed_at, destroy_method, supervisor_name_1, supervisor_name_2,
  destroy_note, created_by
) VALUES
  (1, 'DES-000001', '2026 年第一批到期会计档案销毁清册', 1, 'pending_destroy', NULL, NULL, NULL, NULL, NULL, NULL, 2);

INSERT INTO approval_requests (
  id, approval_type, target_type, target_id, evidence_archive_id, old_value,
  new_value, reason, status, submitted_by, submitted_at, approved_by,
  approved_at, approval_opinion, created_by
) VALUES
  (1, 'security_adjust', 'archive', 2, 1, '0', '1', '预算批复文件拟调整为内部查阅。', 'pending', 2, TIMESTAMPTZ '2026-06-09 14:00:00+08', NULL, NULL, NULL, 2),
  (2, 'destruction', 'destruction_list', 1, NULL, NULL, 'pending_destroy', '到期会计档案鉴定后提交销毁审批。', 'approved', 2, TIMESTAMPTZ '2026-06-03 09:30:00+08', 6, TIMESTAMPTZ '2026-06-04 11:00:00+08', '同意按制度销毁，保留清册和监销记录。', 2);

UPDATE destruction_lists SET approval_request_id = 2 WHERE id = 1;

INSERT INTO destruction_items (
  id, destruction_list_id, archive_id, archive_no_snapshot, title_snapshot,
  category_snapshot, page_count_snapshot, retention_snapshot,
  security_level_snapshot, appraisal_opinion_snapshot, file_delete_status
) VALUES
  (1, 1, 3, 'ARC-000003', '2014 年度临时会计凭证', '会计档案', 90, '10y', 0, '已满十年且无继续保存价值，建议列入销毁清册。', 'not_started');

INSERT INTO archive_change_logs (
  id, archive_id, field_name, old_value, new_value, change_reason,
  change_source, approval_request_id, changed_by, changed_at
) VALUES
  (1, 3, 'lifecycle_status', 'normal', 'pending_destruction', '鉴定结果为待销毁。', 'appraisal', NULL, 2, TIMESTAMPTZ '2026-06-02 16:00:00+08'),
  (2, 1, 'loan_status', 'available', 'on_loan', '借阅出库。', 'manual_edit', NULL, 1, TIMESTAMPTZ '2026-06-10 09:20:00+08');

INSERT INTO inventory_tasks (
  id, task_no, task_name, room_id, category_id, status, started_at,
  completed_at, summary, created_by
) VALUES
  (1, 'INV-000001', '401 库房会计档案盘点', 1, (SELECT id FROM categories WHERE category_code = 'accounting'), 'running', TIMESTAMPTZ '2026-06-10 08:30:00+08', NULL, NULL, 2);

INSERT INTO inventory_items (
  id, task_id, archive_id, box_id, expected_location_id, actual_location_code,
  check_result, note
) VALUES
  (1, 1, 1, 1, 1, NULL, 'on_loan', '当前档案已借出，归还后复核。'),
  (2, 1, 3, 1, 1, '401-01-01-01', 'normal', '实物与架位一致。');

INSERT INTO compilation_materials (
  id, compilation_id, archive_id, sort_no, quote_note
) VALUES
  (1, 1, 1, 1, '财政改革前后会计凭证样例。'),
  (2, 1, 2, 2, '预算批复依据材料。');

INSERT INTO analysis_tasks (
  id, task_no, task_type, status, rule_snapshot, latest_ai_task_id,
  started_at, completed_at, created_by
) VALUES
  (1, 'ANL-000001', 'mixed', 'completed', '{"rules":["missing_fields","tag_suggestion","category_conflict"]}', 3, TIMESTAMPTZ '2026-06-03 09:00:00+08', TIMESTAMPTZ '2026-06-03 09:05:00+08', 2);

INSERT INTO analysis_items (
  id, task_id, archive_id, issue_type, issue_detail, suggestion, status,
  handled_by, handled_at
) VALUES
  (1, 1, 2, 'tag_suggestion', '{"currentTags":["财政","预算决算"],"reason":"公开利用频次较高"}', '{"addTags":["公开利用"]}', 'adopted', 2, TIMESTAMPTZ '2026-06-03 10:00:00+08'),
  (2, 1, 3, 'missing_field', '{"field":"shelvedAt","reason":"纸质档案状态需复核"}', '{"action":"checkPhysicalLocation"}', 'pending', NULL, NULL);

INSERT INTO backup_tasks (
  id, task_no, backup_scope, status, backup_path, file_size, sha256,
  started_at, finished_at, message, created_by
) VALUES
  (1, 'BAK-000001', 'both', 'success', 'system-files/backups/2026-06-01/full-backup.tar.gz', 73400320, repeat('f', 64), TIMESTAMPTZ '2026-06-01 02:00:00+08', TIMESTAMPTZ '2026-06-01 02:08:00+08', '数据库和文件备份完成。', 7);

INSERT INTO file_check_records (
  id, target_type, target_id, check_type, check_result, expected_hash,
  actual_hash, signature_object_key, signature_algorithm, signature_result,
  signature_checked_at, message, checked_at
) VALUES
  (1, 'staging_file', 1, 'integrity', 'passed', repeat('a', 64), repeat('a', 64), NULL, NULL, NULL, NULL, 'SHA-256 一致。', TIMESTAMPTZ '2026-05-20 10:10:30+08'),
  (2, 'staging_file', 1, 'security', 'passed', NULL, NULL, NULL, NULL, NULL, NULL, 'ClamAV 扫描通过。', TIMESTAMPTZ '2026-05-20 10:10:40+08'),
  (3, 'archive_file', 2, 'authenticity', 'not_configured', NULL, NULL, NULL, NULL, 'not_configured', NULL, '未接入外部数字签名体系。', TIMESTAMPTZ '2026-05-22 15:40:00+08');

INSERT INTO archive_access_logs (
  id, user_id, user_type, archive_id, archive_file_id, access_type,
  ip_address, accessed_at
) VALUES
  (1, 4, 'internal', 2, NULL, 'view_metadata', '10.0.0.20', TIMESTAMPTZ '2026-06-05 10:00:00+08'),
  (2, 4, 'internal', 2, 2, 'preview', '10.0.0.20', TIMESTAMPTZ '2026-06-05 10:01:00+08'),
  (3, 5, 'public', 4, NULL, 'view_metadata', '203.0.113.10', TIMESTAMPTZ '2026-06-06 20:00:00+08'),
  (4, 5, 'public', 4, 3, 'download', '203.0.113.10', TIMESTAMPTZ '2026-06-06 20:03:00+08');

INSERT INTO business_attachments (
  id, business_type, business_id, attachment_type, bucket_name, object_key,
  original_filename, file_ext, mime_type, file_size, sha256, scan_result,
  scan_message, file_status, uploaded_by
) VALUES
  (4, 'borrow_request', 1, 'borrow_voucher_pdf', 'archive-metadata', 'borrow/BRW-000001/voucher.pdf', 'BRW-000001-借阅凭证.pdf', 'pdf', 'application/pdf', 180000, repeat('4', 64), 'safe', 'System generated PDF.', 'normal', 4),
  (5, 'destruction_list', 1, 'report', 'archive-metadata', 'destruction/DES-000001/list.pdf', 'DES-000001-销毁清册.pdf', 'pdf', 'application/pdf', 300000, repeat('5', 64), 'safe', 'System generated PDF.', 'normal', 2);

INSERT INTO audit_logs (
  id, actor_user_id, actor_type, module_name, operation_type, business_type,
  business_id, detail, ip_address, operated_at
) VALUES
  (1, 3, 'internal', 'transfer', 'submit_batch', 'intake_batch', 1, '{"batchNo":"BAT-000001","itemCount":3}', '10.0.0.30', TIMESTAMPTZ '2026-05-10 09:00:00+08'),
  (2, 1, 'internal', 'transfer_reception', 'accept_batch', 'intake_batch', 1, '{"acceptedItems":3,"rejectedItems":0}', '10.0.0.11', TIMESTAMPTZ '2026-05-20 10:00:00+08'),
  (3, 2, 'internal', 'pending_archive', 'archive_items', 'intake_batch', 1, '{"archiveNos":["ARC-000001","ARC-000002","ARC-000003"]}', '10.0.0.12', TIMESTAMPTZ '2026-05-22 15:45:00+08'),
  (4, 4, 'internal', 'borrow', 'apply_borrow', 'borrow_request', 1, '{"archiveNo":"ARC-000001"}', '10.0.0.20', TIMESTAMPTZ '2026-06-09 09:30:00+08'),
  (5, 6, 'internal', 'approval', 'approve_destruction', 'approval_request', 2, '{"listNo":"DES-000001"}', '10.0.0.6', TIMESTAMPTZ '2026-06-04 11:00:00+08'),
  (6, 7, 'internal', 'system_config', 'update_config', 'system_config', 2, '{"configKey":"upload.max_file_size_mb","newValue":"200"}', '10.0.0.7', TIMESTAMPTZ '2026-06-01 09:00:00+08');

SELECT setval(pg_get_serial_sequence('organizations','id'), COALESCE((SELECT MAX(id) FROM organizations), 1), true);
SELECT setval(pg_get_serial_sequence('users','id'), COALESCE((SELECT MAX(id) FROM users), 1), true);
SELECT setval(pg_get_serial_sequence('fonds','id'), COALESCE((SELECT MAX(id) FROM fonds), 1), true);
SELECT setval(pg_get_serial_sequence('tags','id'), COALESCE((SELECT MAX(id) FROM tags), 1), true);
SELECT setval(pg_get_serial_sequence('warehouse_rooms','id'), COALESCE((SELECT MAX(id) FROM warehouse_rooms), 1), true);
SELECT setval(pg_get_serial_sequence('storage_locations','id'), COALESCE((SELECT MAX(id) FROM storage_locations), 1), true);
SELECT setval(pg_get_serial_sequence('archive_boxes','id'), COALESCE((SELECT MAX(id) FROM archive_boxes), 1), true);
SELECT setval(pg_get_serial_sequence('intake_batches','id'), COALESCE((SELECT MAX(id) FROM intake_batches), 1), true);
SELECT setval(pg_get_serial_sequence('intake_items','id'), COALESCE((SELECT MAX(id) FROM intake_items), 1), true);
SELECT setval(pg_get_serial_sequence('compilations','id'), COALESCE((SELECT MAX(id) FROM compilations), 1), true);
SELECT setval(pg_get_serial_sequence('archives','id'), COALESCE((SELECT MAX(id) FROM archives), 1), true);
SELECT setval(pg_get_serial_sequence('archive_files','id'), COALESCE((SELECT MAX(id) FROM archive_files), 1), true);
SELECT setval(pg_get_serial_sequence('staging_files','id'), COALESCE((SELECT MAX(id) FROM staging_files), 1), true);
SELECT setval(pg_get_serial_sequence('business_attachments','id'), COALESCE((SELECT MAX(id) FROM business_attachments), 1), true);
SELECT setval(pg_get_serial_sequence('archive_box_items','id'), COALESCE((SELECT MAX(id) FROM archive_box_items), 1), true);
SELECT setval(pg_get_serial_sequence('borrow_requests','id'), COALESCE((SELECT MAX(id) FROM borrow_requests), 1), true);
SELECT setval(pg_get_serial_sequence('approval_requests','id'), COALESCE((SELECT MAX(id) FROM approval_requests), 1), true);
SELECT setval(pg_get_serial_sequence('archive_change_logs','id'), COALESCE((SELECT MAX(id) FROM archive_change_logs), 1), true);
SELECT setval(pg_get_serial_sequence('appraisal_batches','id'), COALESCE((SELECT MAX(id) FROM appraisal_batches), 1), true);
SELECT setval(pg_get_serial_sequence('appraisal_items','id'), COALESCE((SELECT MAX(id) FROM appraisal_items), 1), true);
SELECT setval(pg_get_serial_sequence('destruction_lists','id'), COALESCE((SELECT MAX(id) FROM destruction_lists), 1), true);
SELECT setval(pg_get_serial_sequence('destruction_items','id'), COALESCE((SELECT MAX(id) FROM destruction_items), 1), true);
SELECT setval(pg_get_serial_sequence('inventory_tasks','id'), COALESCE((SELECT MAX(id) FROM inventory_tasks), 1), true);
SELECT setval(pg_get_serial_sequence('inventory_items','id'), COALESCE((SELECT MAX(id) FROM inventory_items), 1), true);
SELECT setval(pg_get_serial_sequence('compilation_materials','id'), COALESCE((SELECT MAX(id) FROM compilation_materials), 1), true);
SELECT setval(pg_get_serial_sequence('analysis_tasks','id'), COALESCE((SELECT MAX(id) FROM analysis_tasks), 1), true);
SELECT setval(pg_get_serial_sequence('analysis_items','id'), COALESCE((SELECT MAX(id) FROM analysis_items), 1), true);
SELECT setval(pg_get_serial_sequence('ai_tasks','id'), COALESCE((SELECT MAX(id) FROM ai_tasks), 1), true);
SELECT setval(pg_get_serial_sequence('ai_task_batches','id'), COALESCE((SELECT MAX(id) FROM ai_task_batches), 1), true);
SELECT setval(pg_get_serial_sequence('backup_tasks','id'), COALESCE((SELECT MAX(id) FROM backup_tasks), 1), true);
SELECT setval(pg_get_serial_sequence('file_check_records','id'), COALESCE((SELECT MAX(id) FROM file_check_records), 1), true);
SELECT setval(pg_get_serial_sequence('archive_access_logs','id'), COALESCE((SELECT MAX(id) FROM archive_access_logs), 1), true);
SELECT setval(pg_get_serial_sequence('audit_logs','id'), COALESCE((SELECT MAX(id) FROM audit_logs), 1), true);

SELECT setval('seq_archive_no', 5, true);
SELECT setval('seq_batch_no', 4, true);
SELECT setval('seq_borrow_request_no', 1, true);
SELECT setval('seq_destruction_list_no', 1, true);
SELECT setval('seq_borrow_voucher_no', 1, true);
SELECT setval('seq_archive_box_no', 3, true);
SELECT setval('seq_backup_task_no', 1, true);
SELECT setval('seq_analysis_task_no', 1, true);
SELECT setval('seq_ai_task_no', 3, true);
SELECT setval('seq_compilation_no', 1, true);

COMMIT;

