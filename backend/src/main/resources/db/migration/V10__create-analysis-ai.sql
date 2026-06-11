-- 研判与 AI 任务

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

-- 外键：ai_tasks 被引用
ALTER TABLE intake_batches
  ADD CONSTRAINT fk_intake_batches_latest_ai_task
  FOREIGN KEY (latest_ai_task_id) REFERENCES ai_tasks(id);

ALTER TABLE analysis_tasks
  ADD CONSTRAINT fk_analysis_tasks_latest_ai_task
  FOREIGN KEY (latest_ai_task_id) REFERENCES ai_tasks(id);

