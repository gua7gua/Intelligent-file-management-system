-- 唯一约束、查询索引和 trigram 索引

-- Unique constraints and partial unique indexes.
CREATE UNIQUE INDEX uk_organizations_org_name ON organizations (org_name);
CREATE UNIQUE INDEX uk_roles_role_code ON roles (role_code);
CREATE UNIQUE INDEX uk_users_login_name ON users (login_name);
CREATE UNIQUE INDEX uk_fonds_fonds_no ON fonds (fonds_no);
CREATE UNIQUE INDEX uk_categories_category_code ON categories (category_code);
CREATE UNIQUE INDEX uk_tags_tag_name ON tags (tag_name);
CREATE UNIQUE INDEX uk_warehouse_rooms_room_no ON warehouse_rooms (room_no);
CREATE UNIQUE INDEX uk_storage_locations_location_code ON storage_locations (location_code);
CREATE UNIQUE INDEX uk_archive_boxes_box_no ON archive_boxes (box_no);
CREATE UNIQUE INDEX uk_intake_batches_batch_no ON intake_batches (batch_no);
CREATE UNIQUE INDEX uk_intake_items_batch_item_no ON intake_items (batch_id, item_no);
CREATE UNIQUE INDEX uk_intake_items_generated_archive
  ON intake_items (generated_archive_id)
  WHERE generated_archive_id IS NOT NULL;
CREATE UNIQUE INDEX uk_compilations_compilation_no ON compilations (compilation_no);
CREATE UNIQUE INDEX uk_archives_archive_no ON archives (archive_no);
CREATE UNIQUE INDEX uk_staging_files_archived_file
  ON staging_files (archived_file_id)
  WHERE archived_file_id IS NOT NULL;
CREATE UNIQUE INDEX uk_archive_boxes_location_active
  ON archive_boxes (location_id)
  WHERE location_id IS NOT NULL AND status IN ('normal','full');
CREATE UNIQUE INDEX uk_archive_box_items_archive ON archive_box_items (archive_id);
CREATE UNIQUE INDEX uk_archive_box_items_box_sort ON archive_box_items (box_id, sort_no);
CREATE UNIQUE INDEX uk_borrow_requests_request_no ON borrow_requests (request_no);
CREATE UNIQUE INDEX uk_borrow_requests_voucher_no
  ON borrow_requests (voucher_no)
  WHERE voucher_no IS NOT NULL;
CREATE UNIQUE INDEX uk_borrow_requests_active_archive
  ON borrow_requests (archive_id)
  WHERE status IN ('applied','approved','voucher_issued','checked_out');
CREATE UNIQUE INDEX uk_approval_requests_pending_target
  ON approval_requests (approval_type, target_type, target_id)
  WHERE status = 'pending';
CREATE UNIQUE INDEX uk_appraisal_batches_batch_no ON appraisal_batches (batch_no);
CREATE UNIQUE INDEX uk_destruction_lists_list_no ON destruction_lists (list_no);
CREATE UNIQUE INDEX uk_inventory_tasks_task_no ON inventory_tasks (task_no);
CREATE UNIQUE INDEX uk_compilation_materials_unique_archive ON compilation_materials (compilation_id, archive_id);
CREATE UNIQUE INDEX uk_analysis_tasks_task_no ON analysis_tasks (task_no);
CREATE UNIQUE INDEX uk_ai_tasks_task_no ON ai_tasks (task_no);
CREATE UNIQUE INDEX uk_ai_task_batches_task_batch ON ai_task_batches (task_id, batch_no);
CREATE UNIQUE INDEX uk_backup_tasks_task_no ON backup_tasks (task_no);
CREATE UNIQUE INDEX uk_system_configs_config_key ON system_configs (config_key);

-- Common query indexes.
CREATE INDEX idx_archives_category_year ON archives (category_id, formed_year);
CREATE INDEX idx_archives_public_filter ON archives (security_level, open_status, lifecycle_status);
CREATE INDEX idx_archives_org_fonds ON archives (organization_id, fonds_id);
CREATE INDEX idx_archives_carrier_loan ON archives (carrier_status, loan_status);
CREATE INDEX idx_archives_retention_until ON archives (retention_until);
CREATE INDEX idx_archive_files_archive_status ON archive_files (archive_id, file_status);
CREATE INDEX idx_intake_batches_type_status ON intake_batches (source_type, status);
CREATE INDEX idx_intake_batches_owner ON intake_batches (organization_id, public_user_id);
CREATE INDEX idx_intake_items_batch_status ON intake_items (batch_id, status);
CREATE INDEX idx_staging_files_batch_filename ON staging_files (batch_id, original_filename);
CREATE INDEX idx_staging_files_sha256 ON staging_files (sha256);
CREATE INDEX idx_business_attachments_business ON business_attachments (business_type, business_id, attachment_type);
CREATE INDEX idx_borrow_requests_borrower_status ON borrow_requests (borrower_id, status);
CREATE INDEX idx_borrow_requests_archive_status ON borrow_requests (archive_id, status);
CREATE INDEX idx_approval_requests_type_status ON approval_requests (approval_type, status);
CREATE INDEX idx_destruction_lists_status ON destruction_lists (status);
CREATE INDEX idx_storage_locations_room_status ON storage_locations (room_id, status);
CREATE INDEX idx_archive_boxes_location ON archive_boxes (location_id);
CREATE INDEX idx_archive_box_items_box_sort ON archive_box_items (box_id, sort_no);
CREATE INDEX idx_archive_tags_tag_archive ON archive_tags (tag_id, archive_id);
CREATE INDEX idx_inventory_items_task_result ON inventory_items (task_id, check_result);
CREATE INDEX idx_analysis_items_task_status ON analysis_items (task_id, status);
CREATE INDEX idx_ai_tasks_type_status ON ai_tasks (task_type, status);
CREATE INDEX idx_ai_tasks_business ON ai_tasks (business_type, business_id);
CREATE INDEX idx_ai_task_batches_task_status ON ai_task_batches (task_id, status);
CREATE INDEX idx_archive_access_logs_user_time ON archive_access_logs (user_id, accessed_at DESC);
CREATE INDEX idx_archive_access_logs_archive_time ON archive_access_logs (archive_id, accessed_at DESC);
CREATE INDEX idx_audit_logs_actor_time ON audit_logs (actor_user_id, operated_at DESC);
CREATE INDEX idx_audit_logs_business_time ON audit_logs (business_type, business_id, operated_at DESC);

-- Trigram indexes for metadata search.
CREATE INDEX gin_archives_title_trgm ON archives USING gin (title gin_trgm_ops);
CREATE INDEX gin_archives_archive_no_trgm ON archives USING gin (archive_no gin_trgm_ops);
CREATE INDEX gin_archives_responsible_trgm ON archives USING gin (responsible_text gin_trgm_ops);
CREATE INDEX gin_archive_files_filename_trgm ON archive_files USING gin (original_filename gin_trgm_ops);
CREATE INDEX gin_intake_items_title_trgm ON intake_items USING gin (input_title gin_trgm_ops);
CREATE INDEX gin_tags_name_trgm ON tags USING gin (tag_name gin_trgm_ops);
