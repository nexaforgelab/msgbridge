CREATE TABLE IF NOT EXISTS mb_admin_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  display_name VARCHAR(128) NOT NULL,
  password_hash VARCHAR(512) NOT NULL,
  role VARCHAR(64) NOT NULL,
  status TINYINT DEFAULT 1,
  last_login_at DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mb_admin_user_role_status ON mb_admin_user(role, status);
