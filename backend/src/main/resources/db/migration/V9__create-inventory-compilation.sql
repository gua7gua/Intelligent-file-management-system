-- 盘点与编研素材

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
