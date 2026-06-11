-- 角色与用户

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
