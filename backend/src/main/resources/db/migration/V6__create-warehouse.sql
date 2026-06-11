-- 库房与架位

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
  room_id BIGINT NOT NULL,
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
  location_id BIGINT,
  category_id SMALLINT,
  fonds_id BIGINT,
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

CREATE TABLE archive_box_items (
  id BIGSERIAL PRIMARY KEY,
  box_id BIGINT NOT NULL,
  archive_id BIGINT NOT NULL,
  sort_no INTEGER NOT NULL CHECK (sort_no > 0),
  page_count INTEGER CHECK (page_count IS NULL OR page_count >= 0),
  physical_status VARCHAR(20) NOT NULL DEFAULT 'normal' CHECK (physical_status IN ('normal','damaged','lost')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by BIGINT,
  updated_by BIGINT,
  deleted_at TIMESTAMPTZ
);
