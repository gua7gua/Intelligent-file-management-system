-- 系统管理表

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
  created_by BIGINT,
  updated_by BIGINT,
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
  user_id BIGINT,
  user_type VARCHAR(20) NOT NULL CHECK (user_type IN ('internal','public','anonymous')),
  archive_id BIGINT NOT NULL,
  archive_file_id BIGINT,
  access_type VARCHAR(30) NOT NULL CHECK (access_type IN ('view_metadata','preview','download')),
  ip_address INET,
  accessed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
  id BIGSERIAL PRIMARY KEY,
  actor_user_id BIGINT,
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
