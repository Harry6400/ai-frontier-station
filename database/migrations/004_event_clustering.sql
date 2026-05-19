-- Migration: 004_event_clustering
-- Description: Create tables for event clustering (grouping related content items)

CREATE TABLE IF NOT EXISTS ai_content_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  title VARCHAR(500) NOT NULL COMMENT '事件标题',
  summary TEXT COMMENT '事件摘要',
  description LONGTEXT COMMENT '详细描述',
  event_date VARCHAR(50) COMMENT '事件发生日期',
  tags JSON COMMENT '标签JSON数组',
  content_count INT DEFAULT 0 COMMENT '关联内容数量',
  view_count INT DEFAULT 0 COMMENT '浏览量',
  status VARCHAR(20) DEFAULT 'pending' COMMENT '审核状态: pending/approved/rejected',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_event_title (title(100)),
  KEY idx_event_date (event_date)
) COMMENT='内容事件聚类表';

CREATE TABLE IF NOT EXISTS ai_content_event_link (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  event_id BIGINT NOT NULL COMMENT '事件ID',
  content_id BIGINT NOT NULL COMMENT '内容ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_event_content (event_id, content_id),
  KEY idx_content (content_id),
  CONSTRAINT fk_event_link_event FOREIGN KEY (event_id) REFERENCES ai_content_event (id) ON DELETE CASCADE,
  CONSTRAINT fk_event_link_content FOREIGN KEY (content_id) REFERENCES ai_content (id) ON DELETE CASCADE
) COMMENT='事件内容关联表';
