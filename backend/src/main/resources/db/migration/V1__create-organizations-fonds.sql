-- 用户组织与全宗

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
