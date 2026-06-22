-- V29: 数据研判演示用「标签明显错配」档案
-- 会计凭证却挂音像/捐赠类标签，供研判 AI 发现 tag_wrong 异常项。
-- 填补 V16 跳过的 ARC-000005 空位（id=5 / archive_no='ARC-000005' 均空缺），
-- 不推进 seq_archive_no（仍=38），不影响演示脚本编号预期（W-A 入库仍产 ARC-000039）。
-- source_batch_id/source_item_id 复用 BAT-000001 的批次与条目（满足 archives 表
-- source_type=transfer 的 CHECK 约束，外键存在）。

INSERT INTO archives (
  id, archive_no, title, responsible_text, formed_date, formed_year, category_id,
  source_type, source_batch_id, source_item_id, source_compilation_id,
  organization_id, fonds_id, carrier_status, retention_period, retention_until,
  security_level, open_status, allow_digitization, lifecycle_status, loan_status,
  condition_status, archived_at, shelved_at, created_by
) VALUES (
  5, 'ARC-000005', '2024 年度第三季度会计凭证（银行存款明细）', '克拉玛依市财政局财务处',
  DATE '2024-09-30', 2024, (SELECT id FROM categories WHERE category_code = 'accounting'),
  'transfer', 1, 1, NULL, 2, 1, 'paper', '30y', DATE '2055-09-30',
  1, 'closed', true, 'normal', 'available', 'normal',
  TIMESTAMPTZ '2026-05-22 16:30:00+08', TIMESTAMPTZ '2026-05-23 09:30:00+08', 2
);

-- 故意挂错标签：会计凭证却挂「影像资料」「社会捐赠」（与会计门类明显冲突）
INSERT INTO archive_tags (archive_id, tag_id) VALUES
  (5, (SELECT id FROM tags WHERE tag_name = '影像资料')),
  (5, (SELECT id FROM tags WHERE tag_name = '社会捐赠'));
