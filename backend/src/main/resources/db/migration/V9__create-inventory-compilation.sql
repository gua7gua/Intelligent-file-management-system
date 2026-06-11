-- 盘点与编研素材

CREATE TABLE inventory_tasks (
  id BIGSERIAL PRIMARY KEY,
  task_no VARCHAR(50) NOT NULL,
  task_name TEXT NOT NULL,
  room_id BIGINT NOT NULL,
  category_id SMALLINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft','running','completed')),
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  summary TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);

CREATE TABLE inventory_items (
  id BIGSERIAL PRIMARY KEY,
  task_id BIGINT NOT NULL,
  archive_id BIGINT NOT NULL,
  box_id BIGINT,
  expected_location_id BIGINT,
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
  compilation_id BIGINT NOT NULL,
  archive_id BIGINT NOT NULL,
  sort_no INTEGER NOT NULL CHECK (sort_no > 0),
  quote_note TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);
