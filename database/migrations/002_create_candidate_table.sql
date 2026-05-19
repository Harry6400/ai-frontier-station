-- Migration 002: Create content candidate table
-- Run: mysql -uroot -p ai_frontier_station < database/migrations/002_create_candidate_table.sql
-- Date: 2026-05-19

CREATE TABLE IF NOT EXISTS ai_content_candidate (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  slug VARCHAR(220) NOT NULL COMMENT '内容标识',
  content_type VARCHAR(50) NOT NULL COMMENT '内容类型',
  summary VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  cover_image VARCHAR(255) DEFAULT NULL COMMENT '封面图地址',
  source_url VARCHAR(500) DEFAULT NULL COMMENT '原始链接',
  source_id BIGINT DEFAULT NULL COMMENT '来源ID',
  raw_content LONGTEXT DEFAULT NULL COMMENT '原始内容',
  ai_generated_title VARCHAR(200) DEFAULT NULL COMMENT 'AI生成标题',
  ai_generated_summary TEXT DEFAULT NULL COMMENT 'AI生成摘要',
  status VARCHAR(30) NOT NULL DEFAULT 'pending' COMMENT '审核状态: pending/approved/rejected',
  reviewer_note VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  trend_score DECIMAL(10,2) DEFAULT 0 COMMENT '趋势评分',
  star_growth7d INT DEFAULT 0 COMMENT '7天Star增长数',
  fork_count INT DEFAULT 0 COMMENT 'Fork数',
  raw_payload_json JSON DEFAULT NULL COMMENT '原始抓取数据快照',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_ai_candidate_slug (slug),
  KEY idx_ai_candidate_status (status),
  KEY idx_ai_candidate_source (source_id),
  CONSTRAINT fk_ai_candidate_source FOREIGN KEY (source_id) REFERENCES ai_source (id) ON DELETE SET NULL
) COMMENT='内容候选表（审核队列）';
