-- 每个档案分类保证至少 2 个盒，便于「档案管理 → 换盒」演示（W-I-3 / M-库房-4）。
-- V16 现状：文书 2 盒，科技/会计/音像/人事各 1 盒；本迁移给后 4 类各补 1 个备用盒。
-- 架位 7~10（401-02-01-01/02/03、401-02-02-01）在 V16 中为空闲，可直接放置。
INSERT INTO archive_boxes (box_no, location_id, category_id, fonds_id, year_label, spine_text, capacity, used_count, status, created_by)
VALUES
('BOX-000007', 7, (SELECT id FROM categories WHERE category_code = 'accounting'),  1, '2014-2025', '财政局会计档案 2 盒（换盒演示备用）', 30, 0, 'normal', 2),
('BOX-000008', 8, (SELECT id FROM categories WHERE category_code = 'technology'),  2, '2019-2023', '城建集团科技档案 2 盒（换盒演示备用）', 30, 0, 'normal', 2),
('BOX-000009', 9, (SELECT id FROM categories WHERE category_code = 'audio_video'), 2, '2003-2021', '城建历史影像档案 2 盒（换盒演示备用）', 30, 0, 'normal', 2),
('BOX-000010',10, (SELECT id FROM categories WHERE category_code = 'personnel'),  1, '2018-2023', '档案馆人事档案 2 盒（换盒演示备用）', 30, 0, 'normal', 2);
