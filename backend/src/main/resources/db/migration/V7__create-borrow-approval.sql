-- 借阅与审批

CREATE TABLE borrow_requests (
  id BIGSERIAL PRIMARY KEY,
  request_no VARCHAR(50) NOT NULL,
  archive_id BIGINT NOT NULL,
  borrower_id BIGINT NOT NULL,
  reason TEXT NOT NULL,
  expected_days INTEGER NOT NULL CHECK (expected_days > 0),
  expected_visit_at TIMESTAMPTZ,
  contact_phone VARCHAR(30),
  status VARCHAR(30) NOT NULL CHECK (status IN ('applied','rejected','approved','voucher_issued','checked_out','returned','abnormal_return')),
  approved_by BIGINT,
  approved_at TIMESTAMPTZ,
  reject_reason TEXT,
  voucher_no VARCHAR(80),
  voucher_issued_at TIMESTAMPTZ,
  checked_out_by BIGINT,
  checked_out_at TIMESTAMPTZ,
  due_at TIMESTAMPTZ,
  returned_by BIGINT,
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
  evidence_archive_id BIGINT,
  old_value TEXT,
  new_value TEXT,
  reason TEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','approved','rejected')),
  submitted_by BIGINT,
  submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  approved_by BIGINT,
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
  archive_id BIGINT NOT NULL,
  field_name VARCHAR(80) NOT NULL,
  old_value TEXT,
  new_value TEXT,
  change_reason TEXT,
  change_source VARCHAR(30) NOT NULL CHECK (change_source IN ('manual_edit','approval','appraisal','destruction','ai_confirmed')),
  approval_request_id BIGINT,
  changed_by BIGINT,
  changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
