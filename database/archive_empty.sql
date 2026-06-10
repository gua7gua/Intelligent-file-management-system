-- Intelligent Archive Management System
-- Empty PostgreSQL 17 schema with fixed dictionaries and system defaults.
-- Run this script on an empty database, or rerun it to rebuild all application tables.

BEGIN;

CREATE EXTENSION IF NOT EXISTS pg_trgm;

DROP TABLE IF EXISTS
  audit_logs,
  archive_access_logs,
  file_check_records,
  backup_tasks,
  ai_task_batches,
  ai_tasks,
  analysis_items,
  analysis_tasks,
  compilation_materials,
  inventory_items,
  inventory_tasks,
  destruction_items,
  destruction_lists,
  appraisal_items,
  appraisal_batches,
  archive_change_logs,
  approval_requests,
  borrow_requests,
  archive_box_items,
  business_attachments,
  staging_files,
  archive_tags,
  archive_files,
  archives,
  compilations,
  intake_items,
  intake_batches,
  archive_boxes,
  storage_locations,
  warehouse_rooms,
  tags,
  categories,
  user_roles,
  fonds,
  users,
  roles,
  organizations,
  system_configs
CASCADE;

DROP SEQUENCE IF EXISTS
  seq_archive_no,
  seq_batch_no,
  seq_borrow_request_no,
  seq_destruction_list_no,
  seq_borrow_voucher_no,
  seq_archive_box_no,
  seq_backup_task_no,
  seq_analysis_task_no,
  seq_ai_task_no,
  seq_compilation_no
CASCADE;

CREATE SEQUENCE seq_archive_no START WITH 1;
CREATE SEQUENCE seq_batch_no START WITH 1;
CREATE SEQUENCE seq_borrow_request_no START WITH 1;
CREATE SEQUENCE seq_destruction_list_no START WITH 1;
CREATE SEQUENCE seq_borrow_voucher_no START WITH 1;
CREATE SEQUENCE seq_archive_box_no START WITH 1;
CREATE SEQUENCE seq_backup_task_no START WITH 1;
CREATE SEQUENCE seq_analysis_task_no START WITH 1;
CREATE SEQUENCE seq_ai_task_no START WITH 1;
CREATE SEQUENCE seq_compilation_no START WITH 1;

CREATE TABLE organizations (
  id BIGSERIAL PRIMARY KEY,
  org_name TEXT NOT NULL,
  org_type VARCHAR(30) NOT NULL CHECK (org_type IN ('archive_org','government','enterprise','public_institution')),
  contact_name TEXT,
  contact_phone VARCHAR(30),
  status VARCHAR(20) NOT NULL DEFAULT 'active' CHECK (status IN ('active','disabled')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

CREATE TABLE roles (
  id SMALLSERIAL PRIMARY KEY,
  role_code VARCHAR(50) NOT NULL,
  role_name TEXT NOT NULL,
  description TEXT,
  enabled BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  user_type VARCHAR(20) NOT NULL CHECK (user_type IN ('internal','public')),
  login_name VARCHAR(80) NOT NULL,
  employee_no VARCHAR(50),
  phone VARCHAR(30),
  password_hash TEXT NOT NULL,
  real_name TEXT NOT NULL,
  organization_id BIGINT REFERENCES organizations(id),
  department_name TEXT,
  max_security_level SMALLINT NOT NULL DEFAULT 0 CHECK (max_security_level BETWEEN 0 AND 4),
  data_scope VARCHAR(20) NOT NULL DEFAULT 'own_org' CHECK (data_scope IN ('own_org','own_fonds','all')),
  status VARCHAR(20) NOT NULL DEFAULT 'active' CHECK (status IN ('active','disabled')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

CREATE TABLE user_roles (
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role_id SMALLINT NOT NULL REFERENCES roles(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, role_id)
);

CREATE TABLE fonds (
  id BIGSERIAL PRIMARY KEY,
  fonds_no VARCHAR(50) NOT NULL,
  fonds_name TEXT NOT NULL,
  organization_id BIGINT REFERENCES organizations(id),
  description TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'active' CHECK (status IN ('active','disabled')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

CREATE TABLE categories (
  id SMALLSERIAL PRIMARY KEY,
  category_name TEXT NOT NULL,
  category_code VARCHAR(20) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE tags (
  id BIGSERIAL PRIMARY KEY,
  tag_name TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

CREATE TABLE warehouse_rooms (
  id BIGSERIAL PRIMARY KEY,
  room_no VARCHAR(50) NOT NULL,
  room_name TEXT NOT NULL,
  rack_count INTEGER NOT NULL CHECK (rack_count > 0),
  layers_per_rack INTEGER NOT NULL CHECK (layers_per_rack > 0),
  boxes_per_layer INTEGER NOT NULL CHECK (boxes_per_layer > 0),
  capacity INTEGER NOT NULL CHECK (capacity > 0),
  warning_threshold NUMERIC(5,2) NOT NULL DEFAULT 0.85 CHECK (warning_threshold > 0 AND warning_threshold <= 1),
  status VARCHAR(20) NOT NULL DEFAULT 'active' CHECK (status IN ('active','disabled')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

CREATE TABLE storage_locations (
  id BIGSERIAL PRIMARY KEY,
  room_id BIGINT NOT NULL REFERENCES warehouse_rooms(id),
  rack_no INTEGER NOT NULL CHECK (rack_no > 0),
  layer_no INTEGER NOT NULL CHECK (layer_no > 0),
  box_slot_no INTEGER NOT NULL CHECK (box_slot_no > 0),
  location_code VARCHAR(80) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'active' CHECK (status IN ('active','disabled')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

CREATE TABLE archive_boxes (
  id BIGSERIAL PRIMARY KEY,
  box_no VARCHAR(80) NOT NULL,
  location_id BIGINT REFERENCES storage_locations(id),
  category_id SMALLINT REFERENCES categories(id),
  fonds_id BIGINT REFERENCES fonds(id),
  year_label VARCHAR(20),
  spine_text TEXT,
  capacity INTEGER CHECK (capacity IS NULL OR capacity > 0),
  used_count INTEGER NOT NULL DEFAULT 0 CHECK (used_count >= 0),
  status VARCHAR(20) NOT NULL DEFAULT 'normal' CHECK (status IN ('normal','full','moved','destroyed')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

CREATE TABLE intake_batches (
  id BIGSERIAL PRIMARY KEY,
  batch_no VARCHAR(50) NOT NULL,
  source_type VARCHAR(20) NOT NULL CHECK (source_type IN ('transfer','collection')),
  title TEXT NOT NULL,
  status VARCHAR(30) NOT NULL CHECK (status IN ('draft','pending_transfer','pending_contact','pending_receive','received','partially_received','rejected','archived','shelved')),
  organization_id BIGINT REFERENCES organizations(id),
  department_name TEXT,
  public_user_id BIGINT REFERENCES users(id),
  contact_name TEXT NOT NULL,
  contact_phone VARCHAR(30),
  archive_year INTEGER,
  expected_transfer_date DATE,
  scheduled_receive_at TIMESTAMPTZ,
  submitted_at TIMESTAMPTZ,
  accepted_by BIGINT REFERENCES users(id),
  accepted_at TIMESTAMPTZ,
  archived_at TIMESTAMPTZ,
  shelved_at TIMESTAMPTZ,
  latest_ai_task_id BIGINT,
  reject_reason TEXT,
  agreement_accepted_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ,
  CONSTRAINT ck_intake_batch_source_owner CHECK (
    (source_type = 'transfer' AND organization_id IS NOT NULL AND public_user_id IS NULL)
    OR
    (source_type = 'collection' AND public_user_id IS NOT NULL)
  )
);

CREATE TABLE intake_items (
  id BIGSERIAL PRIMARY KEY,
  batch_id BIGINT NOT NULL REFERENCES intake_batches(id) ON DELETE CASCADE,
  item_no INTEGER NOT NULL CHECK (item_no > 0),
  status VARCHAR(30) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft','pending_acceptance','accepted','rejected','pending_archive','archived')),
  input_title TEXT NOT NULL,
  page_count INTEGER CHECK (page_count IS NULL OR page_count >= 0),
  retention_period VARCHAR(20) NOT NULL CHECK (retention_period IN ('10y','30y','permanent')),
  carrier_status VARCHAR(20) NOT NULL CHECK (carrier_status IN ('electronic','paper_electronic','paper')),
  security_level SMALLINT NOT NULL DEFAULT 0 CHECK (security_level BETWEEN 0 AND 4),
  open_status VARCHAR(20) NOT NULL DEFAULT 'open' CHECK (open_status IN ('open','closed')),
  allow_digitization BOOLEAN NOT NULL DEFAULT false,
  electronic_format VARCHAR(50),
  expected_filename TEXT,
  formed_date DATE,
  acceptance_note TEXT,
  reject_reason TEXT,
  file_match_status VARCHAR(30) NOT NULL DEFAULT 'none' CHECK (file_match_status IN ('none','matched','missing','duplicate','failed')),
  ai_suggestion JSONB,
  confirmed_title TEXT,
  confirmed_responsible_text TEXT,
  confirmed_formed_date DATE,
  confirmed_category_id SMALLINT REFERENCES categories(id),
  confirmed_tags JSONB,
  generated_archive_id BIGINT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

CREATE TABLE compilations (
  id BIGSERIAL PRIMARY KEY,
  compilation_no VARCHAR(50) NOT NULL,
  title TEXT NOT NULL,
  compilation_type VARCHAR(50),
  date_range_text TEXT,
  keywords TEXT,
  summary TEXT,
  content_html TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft','generated','archived')),
  generated_file_attachment_id BIGINT,
  generated_archive_file_id BIGINT,
  generated_archive_id BIGINT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT REFERENCES users(id),
  updated_by BIGINT REFERENCES users(id),
  deleted_at TIMESTAMPTZ
);

CREATE TABLE archives (
  id BIGSERIAL PRIMARY KEY,
  archive_no VARCHAR(50) NOT NULL,
  title TEXT NOT NULL,
  responsible_text TEXT,
  formed_date DATE,
  formed_year INTEGER,
  category_id SMALLINT NOT NULL REFERENCES categories(id),
  source_type VARCHAR(20) NOT NULL CHECK (source_type IN ('transfer','collection','compilation')),
  source_batch_id BIGINT REFERENCES intake_batches(id),
  source_item_id BIGINT REFERENCES intake_items(id),
  source_compilation_id BIGINT REFERENCES compilations(id),
  organization_id BIGINT REFERENCES organizations(id),
  fonds_id BIGINT REFERENCES fonds(id),
  carrier_status VARCHAR(20) NOT NULL CHECK (carrier_status IN ('electronic','paper_electronic','paper')),
  retention_period VARCHAR(20) NOT NULL CHECK (retention_period IN ('10y','30y','permanent')),
  retention_until DATE,
  security_level SMALLINT NOT NULL DEFAULT 0 CHECK (security_level BETWEEN 0 AND 4),
  open_status VARCHAR(20) NOT NULL DEFAULT 'open' CHECK (open_status IN ('open','closed')),
  allow_digitization BOOLEAN NOT NULL DEFAULT false,
  lifecycle_status VARCHAR(30) NOT NULL DEFAULT 'pending_shelf' CHECK (lifecycle_status IN ('pending_shelf','normal','pending_destruction','destroyed')),
  loan_status VARCHAR(20) NOT NULL DEFAULT 'available' CHECK (loan_status IN ('available','on_loan')),
  condition_status VARCHAR(20) NOT NULL DEFAULT 'normal' CHECK (condition_status IN ('normal','damaged','repairing','lost','destroyed')),
  archived_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  shelved_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT REFERENCES users(id),
  updated_by BIGINT REFERENCES users(id),
  deleted_at TIMESTAMPTZ,
  CONSTRAINT ck_archives_source_ref CHECK (
    (source_type IN ('transfer','collection') AND source_batch_id IS NOT NULL AND source_item_id IS NOT NULL AND source_compilation_id IS NULL)
    OR
    (source_type = 'compilation' AND source_batch_id IS NULL AND source_item_id IS NULL AND source_compilation_id IS NOT NULL)
  ),
  CONSTRAINT ck_archives_retention_until CHECK (
    (retention_period = 'permanent' AND retention_until IS NULL)
    OR
    (retention_period IN ('10y','30y') AND retention_until IS NOT NULL)
  ),
  CONSTRAINT ck_archives_shelf_status CHECK (
    (carrier_status = 'electronic' AND lifecycle_status <> 'pending_shelf')
    OR
    carrier_status IN ('paper_electronic','paper')
  )
);

CREATE TABLE archive_files (
  id BIGSERIAL PRIMARY KEY,
  archive_id BIGINT NOT NULL REFERENCES archives(id) ON DELETE CASCADE,
  file_role VARCHAR(30) NOT NULL CHECK (file_role IN ('original','scan','compilation_body','signature')),
  bucket_name VARCHAR(100) NOT NULL,
  object_key TEXT NOT NULL,
  original_filename TEXT NOT NULL,
  file_ext VARCHAR(30),
  mime_type VARCHAR(120),
  file_size BIGINT NOT NULL CHECK (file_size >= 0),
  sha256 CHAR(64),
  scan_result VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (scan_result IN ('pending','safe','infected','failed')),
  usability_result VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (usability_result IN ('pending','passed','failed')),
  file_status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (file_status IN ('pending','normal','failed','deleted')),
  deleted_at TIMESTAMPTZ,
  deleted_by BIGINT REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT REFERENCES users(id),
  updated_by BIGINT REFERENCES users(id)
);

CREATE TABLE archive_tags (
  archive_id BIGINT NOT NULL REFERENCES archives(id) ON DELETE CASCADE,
  tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (archive_id, tag_id)
);

CREATE TABLE staging_files (
  id BIGSERIAL PRIMARY KEY,
  batch_id BIGINT NOT NULL REFERENCES intake_batches(id) ON DELETE CASCADE,
  item_id BIGINT REFERENCES intake_items(id) ON DELETE SET NULL,
  upload_batch_no VARCHAR(80) NOT NULL,
  original_filename TEXT NOT NULL,
  file_ext VARCHAR(30),
  mime_type VARCHAR(120),
  file_size BIGINT NOT NULL CHECK (file_size >= 0),
  sha256 CHAR(64),
  bucket_name VARCHAR(100) NOT NULL,
  object_key TEXT NOT NULL,
  match_status VARCHAR(30) NOT NULL CHECK (match_status IN ('unmatched','matched','duplicate','failed_check','archived','deleted')),
  scan_result VARCHAR(20) NOT NULL CHECK (scan_result IN ('pending','safe','infected','failed')),
  scan_message TEXT,
  check_summary JSONB,
  uploaded_by BIGINT REFERENCES users(id),
  uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  archived_file_id BIGINT REFERENCES archive_files(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

CREATE TABLE business_attachments (
  id BIGSERIAL PRIMARY KEY,
  business_type VARCHAR(50) NOT NULL,
  business_id BIGINT NOT NULL,
  attachment_type VARCHAR(50) NOT NULL,
  bucket_name VARCHAR(100) NOT NULL,
  object_key TEXT NOT NULL,
  original_filename TEXT NOT NULL,
  file_ext VARCHAR(30),
  mime_type VARCHAR(120),
  file_size BIGINT NOT NULL CHECK (file_size >= 0),
  sha256 CHAR(64),
  scan_result VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (scan_result IN ('pending','safe','infected','failed')),
  scan_message TEXT,
  file_status VARCHAR(20) NOT NULL DEFAULT 'normal' CHECK (file_status IN ('normal','deleted')),
  uploaded_by BIGINT REFERENCES users(id),
  uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

CREATE TABLE archive_box_items (
  id BIGSERIAL PRIMARY KEY,
  box_id BIGINT NOT NULL REFERENCES archive_boxes(id) ON DELETE CASCADE,
  archive_id BIGINT NOT NULL REFERENCES archives(id) ON DELETE CASCADE,
  sort_no INTEGER NOT NULL CHECK (sort_no > 0),
  page_count INTEGER CHECK (page_count IS NULL OR page_count >= 0),
  physical_status VARCHAR(20) NOT NULL DEFAULT 'normal' CHECK (physical_status IN ('normal','damaged','lost')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

CREATE TABLE borrow_requests (
  id BIGSERIAL PRIMARY KEY,
  request_no VARCHAR(50) NOT NULL,
  archive_id BIGINT NOT NULL REFERENCES archives(id),
  borrower_id BIGINT NOT NULL REFERENCES users(id),
  reason TEXT NOT NULL,
  expected_days INTEGER NOT NULL CHECK (expected_days > 0),
  expected_visit_at TIMESTAMPTZ,
  contact_phone VARCHAR(30),
  status VARCHAR(30) NOT NULL CHECK (status IN ('applied','rejected','approved','voucher_issued','checked_out','returned','abnormal_return')),
  approved_by BIGINT REFERENCES users(id),
  approved_at TIMESTAMPTZ,
  reject_reason TEXT,
  voucher_no VARCHAR(80),
  voucher_issued_at TIMESTAMPTZ,
  checked_out_by BIGINT REFERENCES users(id),
  checked_out_at TIMESTAMPTZ,
  due_at TIMESTAMPTZ,
  returned_by BIGINT REFERENCES users(id),
  returned_at TIMESTAMPTZ,
  return_check_result VARCHAR(30) CHECK (return_check_result IS NULL OR return_check_result IN ('normal','damaged','missing_page','other')),
  return_note TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

CREATE TABLE approval_requests (
  id BIGSERIAL PRIMARY KEY,
  approval_type VARCHAR(30) NOT NULL CHECK (approval_type IN ('security_adjust','open_adjust','destruction')),
  target_type VARCHAR(50) NOT NULL CHECK (target_type IN ('archive','destruction_list')),
  target_id BIGINT NOT NULL,
  evidence_archive_id BIGINT REFERENCES archives(id),
  old_value TEXT,
  new_value TEXT,
  reason TEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','approved','rejected')),
  submitted_by BIGINT REFERENCES users(id),
  submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  approved_by BIGINT REFERENCES users(id),
  approved_at TIMESTAMPTZ,
  approval_opinion TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ,
  CONSTRAINT ck_approval_evidence CHECK (
    (approval_type IN ('security_adjust','open_adjust') AND target_type = 'archive' AND evidence_archive_id IS NOT NULL)
    OR
    (approval_type = 'destruction' AND target_type = 'destruction_list' AND evidence_archive_id IS NULL)
  )
);

CREATE TABLE archive_change_logs (
  id BIGSERIAL PRIMARY KEY,
  archive_id BIGINT NOT NULL REFERENCES archives(id) ON DELETE CASCADE,
  field_name VARCHAR(80) NOT NULL,
  old_value TEXT,
  new_value TEXT,
  change_reason TEXT,
  change_source VARCHAR(30) NOT NULL CHECK (change_source IN ('manual_edit','approval','appraisal','destruction','ai_confirmed')),
  approval_request_id BIGINT REFERENCES approval_requests(id),
  changed_by BIGINT REFERENCES users(id),
  changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE appraisal_batches (
  id BIGSERIAL PRIMARY KEY,
  batch_no VARCHAR(50) NOT NULL,
  batch_name TEXT NOT NULL,
  category_id SMALLINT REFERENCES categories(id),
  formed_year_start INTEGER,
  formed_year_end INTEGER,
  status VARCHAR(20) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft','completed')),
  completed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT REFERENCES users(id),
  updated_by BIGINT REFERENCES users(id),
  deleted_at TIMESTAMPTZ
);

CREATE TABLE appraisal_items (
  id BIGSERIAL PRIMARY KEY,
  batch_id BIGINT NOT NULL REFERENCES appraisal_batches(id) ON DELETE CASCADE,
  archive_id BIGINT NOT NULL REFERENCES archives(id),
  appraisal_result VARCHAR(20) NOT NULL CHECK (appraisal_result IN ('extend','destroy')),
  new_retention_period VARCHAR(20) CHECK (new_retention_period IS NULL OR new_retention_period IN ('10y','30y','permanent')),
  new_retention_until DATE,
  opinion TEXT,
  appraised_by BIGINT REFERENCES users(id),
  appraised_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

CREATE TABLE destruction_lists (
  id BIGSERIAL PRIMARY KEY,
  list_no VARCHAR(50) NOT NULL,
  list_name TEXT NOT NULL,
  appraisal_batch_id BIGINT REFERENCES appraisal_batches(id),
  status VARCHAR(30) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft','pending_approval','pending_destroy','destroyed')),
  approval_request_id BIGINT REFERENCES approval_requests(id),
  destroyed_at TIMESTAMPTZ,
  destroy_method VARCHAR(30) CHECK (destroy_method IS NULL OR destroy_method IN ('shredding','burning','entrusted')),
  supervisor_name_1 TEXT,
  supervisor_name_2 TEXT,
  destroy_note TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT REFERENCES users(id),
  updated_by BIGINT REFERENCES users(id),
  deleted_at TIMESTAMPTZ
);

CREATE TABLE destruction_items (
  id BIGSERIAL PRIMARY KEY,
  destruction_list_id BIGINT NOT NULL REFERENCES destruction_lists(id) ON DELETE CASCADE,
  archive_id BIGINT NOT NULL REFERENCES archives(id),
  archive_no_snapshot VARCHAR(50) NOT NULL,
  title_snapshot TEXT NOT NULL,
  category_snapshot TEXT,
  page_count_snapshot INTEGER,
  retention_snapshot VARCHAR(20),
  security_level_snapshot SMALLINT,
  appraisal_opinion_snapshot TEXT,
  file_delete_status VARCHAR(20) NOT NULL DEFAULT 'not_started' CHECK (file_delete_status IN ('not_started','deleted','failed')),
  file_deleted_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

CREATE TABLE inventory_tasks (
  id BIGSERIAL PRIMARY KEY,
  task_no VARCHAR(50) NOT NULL,
  task_name TEXT NOT NULL,
  room_id BIGINT NOT NULL REFERENCES warehouse_rooms(id),
  category_id SMALLINT NOT NULL REFERENCES categories(id),
  status VARCHAR(20) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft','running','completed')),
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  summary TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT REFERENCES users(id),
  updated_by BIGINT REFERENCES users(id),
  deleted_at TIMESTAMPTZ
);

CREATE TABLE inventory_items (
  id BIGSERIAL PRIMARY KEY,
  task_id BIGINT NOT NULL REFERENCES inventory_tasks(id) ON DELETE CASCADE,
  archive_id BIGINT NOT NULL REFERENCES archives(id),
  box_id BIGINT REFERENCES archive_boxes(id),
  expected_location_id BIGINT REFERENCES storage_locations(id),
  actual_location_code VARCHAR(80),
  check_result VARCHAR(30) NOT NULL CHECK (check_result IN ('normal','missing','misplaced','damaged','on_loan')),
  note TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

CREATE TABLE compilation_materials (
  id BIGSERIAL PRIMARY KEY,
  compilation_id BIGINT NOT NULL REFERENCES compilations(id) ON DELETE CASCADE,
  archive_id BIGINT NOT NULL REFERENCES archives(id),
  sort_no INTEGER NOT NULL CHECK (sort_no > 0),
  quote_note TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

CREATE TABLE analysis_tasks (
  id BIGSERIAL PRIMARY KEY,
  task_no VARCHAR(50) NOT NULL,
  task_type VARCHAR(30) NOT NULL CHECK (task_type IN ('missing_fields','category_conflict','mixed')),
  status VARCHAR(20) NOT NULL CHECK (status IN ('running','partial_completed','completed','failed')),
  rule_snapshot JSONB,
  latest_ai_task_id BIGINT,
  started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  completed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT REFERENCES users(id),
  updated_by BIGINT REFERENCES users(id),
  deleted_at TIMESTAMPTZ
);

CREATE TABLE analysis_items (
  id BIGSERIAL PRIMARY KEY,
  task_id BIGINT NOT NULL REFERENCES analysis_tasks(id) ON DELETE CASCADE,
  archive_id BIGINT NOT NULL REFERENCES archives(id),
  issue_type VARCHAR(50) NOT NULL CHECK (issue_type IN ('missing_field','category_conflict','tag_suggestion')),
  issue_detail JSONB NOT NULL,
  suggestion JSONB,
  status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','adopted','rejected')),
  handled_by BIGINT REFERENCES users(id),
  handled_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

CREATE TABLE ai_tasks (
  id BIGSERIAL PRIMARY KEY,
  task_no VARCHAR(50) NOT NULL,
  task_type VARCHAR(40) NOT NULL CHECK (task_type IN ('internal_search_query','public_search_query','intake_completion','archive_analysis')),
  business_type VARCHAR(50) NOT NULL DEFAULT 'none',
  business_id BIGINT,
  status VARCHAR(30) NOT NULL CHECK (status IN ('running','partial_completed','completed','failed')),
  batch_size INTEGER CHECK (batch_size IS NULL OR batch_size > 0),
  total_batches INTEGER NOT NULL DEFAULT 0 CHECK (total_batches >= 0),
  success_batches INTEGER NOT NULL DEFAULT 0 CHECK (success_batches >= 0),
  failed_batches INTEGER NOT NULL DEFAULT 0 CHECK (failed_batches >= 0),
  started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  completed_at TIMESTAMPTZ,
  error_message TEXT,
  created_by BIGINT REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

CREATE TABLE ai_task_batches (
  id BIGSERIAL PRIMARY KEY,
  task_id BIGINT NOT NULL REFERENCES ai_tasks(id) ON DELETE CASCADE,
  batch_no INTEGER NOT NULL CHECK (batch_no > 0),
  status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','running','success','failed')),
  target_ids JSONB NOT NULL,
  request_context JSONB,
  raw_response TEXT,
  validated_result JSONB,
  error_message TEXT,
  attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

ALTER TABLE intake_batches
  ADD CONSTRAINT fk_intake_batches_latest_ai_task
  FOREIGN KEY (latest_ai_task_id) REFERENCES ai_tasks(id);

ALTER TABLE analysis_tasks
  ADD CONSTRAINT fk_analysis_tasks_latest_ai_task
  FOREIGN KEY (latest_ai_task_id) REFERENCES ai_tasks(id);

CREATE TABLE backup_tasks (
  id BIGSERIAL PRIMARY KEY,
  task_no VARCHAR(50) NOT NULL,
  backup_scope VARCHAR(30) NOT NULL CHECK (backup_scope IN ('database','files','both')),
  status VARCHAR(20) NOT NULL CHECK (status IN ('running','success','failed')),
  backup_path TEXT,
  file_size BIGINT CHECK (file_size IS NULL OR file_size >= 0),
  sha256 CHAR(64),
  started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  finished_at TIMESTAMPTZ,
  message TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT REFERENCES users(id),
  updated_by BIGINT REFERENCES users(id),
  deleted_at TIMESTAMPTZ
);

CREATE TABLE file_check_records (
  id BIGSERIAL PRIMARY KEY,
  target_type VARCHAR(30) NOT NULL CHECK (target_type IN ('staging_file','archive_file')),
  target_id BIGINT NOT NULL,
  check_type VARCHAR(30) NOT NULL CHECK (check_type IN ('integrity','usability','authenticity','security')),
  check_result VARCHAR(20) NOT NULL CHECK (check_result IN ('passed','failed','not_configured')),
  expected_hash CHAR(64),
  actual_hash CHAR(64),
  signature_object_key TEXT,
  signature_algorithm VARCHAR(50),
  signature_result VARCHAR(20) CHECK (signature_result IS NULL OR signature_result IN ('passed','failed','not_configured')),
  signature_checked_at TIMESTAMPTZ,
  message TEXT,
  checked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

CREATE TABLE archive_access_logs (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES users(id),
  user_type VARCHAR(20) NOT NULL CHECK (user_type IN ('internal','public','anonymous')),
  archive_id BIGINT NOT NULL REFERENCES archives(id),
  archive_file_id BIGINT REFERENCES archive_files(id),
  access_type VARCHAR(30) NOT NULL CHECK (access_type IN ('view_metadata','preview','download')),
  ip_address INET,
  accessed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
  id BIGSERIAL PRIMARY KEY,
  actor_user_id BIGINT REFERENCES users(id),
  actor_type VARCHAR(20) NOT NULL CHECK (actor_type IN ('internal','public','system')),
  module_name VARCHAR(50) NOT NULL,
  operation_type VARCHAR(50) NOT NULL,
  business_type VARCHAR(50),
  business_id BIGINT,
  detail JSONB,
  ip_address INET,
  operated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE system_configs (
  id BIGSERIAL PRIMARY KEY,
  config_key VARCHAR(100) NOT NULL,
  config_value TEXT NOT NULL,
  value_type VARCHAR(20) NOT NULL CHECK (value_type IN ('string','number','boolean','json')),
  description TEXT,
  editable BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

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

-- Fixed dictionaries.
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

COMMIT;
