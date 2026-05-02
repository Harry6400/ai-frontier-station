USE ai_frontier_station;

CREATE TABLE IF NOT EXISTS ai_api_credential (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  provider VARCHAR(50) NOT NULL COMMENT 'Provider 标识，如 bailian、github、mimo',
  encrypted_key TEXT NOT NULL COMMENT 'AES 加密后的密钥',
  iv VARCHAR(64) NOT NULL COMMENT 'AES-GCM 初始化向量（Base64）',
  key_suffix VARCHAR(10) NOT NULL COMMENT '密钥明文后4位，用于前端展示',
  remark VARCHAR(255) DEFAULT NULL COMMENT '用户备注',
  is_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_ai_api_credential_provider (provider)
) COMMENT='API 密钥加密存储表';
