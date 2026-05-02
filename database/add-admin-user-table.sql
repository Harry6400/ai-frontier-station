USE ai_frontier_station;

CREATE TABLE IF NOT EXISTS ai_admin_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密后的密码',
  display_name VARCHAR(100) DEFAULT NULL COMMENT '显示名称',
  role VARCHAR(50) NOT NULL DEFAULT 'admin' COMMENT '角色：admin',
  is_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='管理员用户表';

-- 默认管理员账号：admin / admin123
INSERT INTO ai_admin_user (username, password_hash, display_name, role, is_enabled)
VALUES ('admin', '$2y$10$.vdOnVZcNImd5eccQwby.O25KEplZbt99cjb7LG5koNYNZBtolPAO', '系统管理员', 'admin', 1)
ON DUPLICATE KEY UPDATE username = username;
