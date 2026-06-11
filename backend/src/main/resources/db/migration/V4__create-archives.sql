-- 编研成果与正式档案

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
