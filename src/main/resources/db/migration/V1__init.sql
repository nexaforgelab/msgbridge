CREATE TABLE IF NOT EXISTS mb_app (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  app_id VARCHAR(64) NOT NULL UNIQUE,
  app_name VARCHAR(128) NOT NULL,
  app_secret_encrypted VARCHAR(1024) NOT NULL,
  sign_type VARCHAR(32) DEFAULT 'HMAC_SHA256',
  ip_whitelist TEXT,
  rate_limit_per_min INT DEFAULT 600,
  status TINYINT DEFAULT 1,
  owner_name VARCHAR(64),
  owner_contact VARCHAR(128),
  remark VARCHAR(512),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS mb_channel (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  channel_code VARCHAR(64) NOT NULL UNIQUE,
  channel_name VARCHAR(128) NOT NULL,
  channel_type VARCHAR(64) NOT NULL,
  config_json LONGTEXT NOT NULL,
  secret_json_encrypted LONGTEXT,
  status TINYINT DEFAULT 1,
  remark VARCHAR(512),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mb_channel_type_status ON mb_channel(channel_type, status);

CREATE TABLE IF NOT EXISTS mb_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_code VARCHAR(64) NOT NULL,
  template_name VARCHAR(128) NOT NULL,
  scene_code VARCHAR(64) NOT NULL,
  channel_type VARCHAR(64) NOT NULL,
  msg_type VARCHAR(32) NOT NULL,
  content_template LONGTEXT NOT NULL,
  variables_json LONGTEXT,
  version INT DEFAULT 1,
  status TINYINT DEFAULT 1,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_template_scene_channel_version (scene_code, channel_type, version)
);

CREATE INDEX IF NOT EXISTS idx_mb_template_lookup ON mb_template(scene_code, channel_type, status, version);

CREATE TABLE IF NOT EXISTS mb_route_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_code VARCHAR(64) NOT NULL UNIQUE,
  rule_name VARCHAR(128) NOT NULL,
  scene_code VARCHAR(64) NOT NULL,
  route_key VARCHAR(128),
  condition_expr LONGTEXT,
  target_channels_json LONGTEXT NOT NULL,
  priority INT DEFAULT 100,
  status TINYINT DEFAULT 1,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mb_route_lookup ON mb_route_rule(scene_code, route_key, status, priority);

CREATE TABLE IF NOT EXISTS mb_message_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  message_id VARCHAR(64) NOT NULL UNIQUE,
  request_id VARCHAR(128) NOT NULL,
  app_id VARCHAR(64) NOT NULL,
  scene_code VARCHAR(64) NOT NULL,
  route_key VARCHAR(128),
  priority VARCHAR(32) DEFAULT 'NORMAL',
  message_json LONGTEXT NOT NULL,
  status VARCHAR(32) NOT NULL,
  retry_count INT DEFAULT 0,
  max_retry_count INT DEFAULT 3,
  next_retry_at DATETIME(6),
  error_code VARCHAR(64),
  error_message LONGTEXT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_app_request (app_id, request_id)
);

CREATE INDEX IF NOT EXISTS idx_mb_task_poll ON mb_message_task(status, next_retry_at, created_at);
CREATE INDEX IF NOT EXISTS idx_mb_task_scene ON mb_message_task(scene_code, created_at);

CREATE TABLE IF NOT EXISTS mb_send_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  message_id VARCHAR(64) NOT NULL,
  channel_id BIGINT NOT NULL,
  channel_type VARCHAR(64),
  platform_request LONGTEXT,
  platform_response LONGTEXT,
  platform_code VARCHAR(64),
  platform_message VARCHAR(512),
  status VARCHAR(32) NOT NULL,
  retryable TINYINT DEFAULT 0,
  cost_ms INT,
  sent_at DATETIME(6),
  created_at DATETIME(6) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mb_send_log_message ON mb_send_log(message_id, created_at);
CREATE INDEX IF NOT EXISTS idx_mb_send_log_channel ON mb_send_log(channel_id, created_at);

CREATE TABLE IF NOT EXISTS mb_token_cache (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  token_key VARCHAR(128) NOT NULL UNIQUE,
  token_value_encrypted LONGTEXT NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS mb_audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  actor VARCHAR(128) NOT NULL,
  action VARCHAR(128) NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_id VARCHAR(128),
  detail_json LONGTEXT,
  created_at DATETIME(6) NOT NULL
);
