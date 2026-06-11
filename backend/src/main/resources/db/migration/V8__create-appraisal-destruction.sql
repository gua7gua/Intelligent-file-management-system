-- 鉴定与销毁

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
