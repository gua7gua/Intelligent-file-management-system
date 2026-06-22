-- 修复 V27 副作用：V27__seed-second-box-per-category 直接硬编码插入了 BOX-000007~010，
-- 但未推进 seq_archive_box_no。BoxNoUtil.generate() 通过 nextval('seq_archive_box_no') 取号，
-- 序列滞留在 7，建新盒时生成 BOX-000007 与已存在盒冲突（unique uk_archive_boxes_box_no → 500）。
-- 这里把序列推进到当前最大盒号，后续建盒续编 BOX-000011。
SELECT setval('seq_archive_box_no',
  (SELECT COALESCE(max(CAST(substr(box_no, 5) AS bigint)), 0) FROM archive_boxes));
