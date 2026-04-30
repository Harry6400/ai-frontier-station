CREATE DATABASE IF NOT EXISTS ai_frontier_station
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE ai_frontier_station;

CREATE TABLE IF NOT EXISTS ai_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  name VARCHAR(100) NOT NULL COMMENT '分类名称',
  slug VARCHAR(120) NOT NULL COMMENT '分类标识',
  description VARCHAR(255) DEFAULT NULL COMMENT '分类说明',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
  is_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_ai_category_slug (slug)
) COMMENT='内容分类表';

CREATE TABLE IF NOT EXISTS ai_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  name VARCHAR(100) NOT NULL COMMENT '标签名称',
  slug VARCHAR(120) NOT NULL COMMENT '标签标识',
  color VARCHAR(30) DEFAULT NULL COMMENT '标签展示色',
  description VARCHAR(255) DEFAULT NULL COMMENT '标签说明',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_ai_tag_slug (slug)
) COMMENT='内容标签表';

CREATE TABLE IF NOT EXISTS ai_source (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  name VARCHAR(120) NOT NULL COMMENT '来源名称',
  slug VARCHAR(120) NOT NULL COMMENT '来源标识',
  source_type VARCHAR(50) NOT NULL COMMENT '来源类型，如github、paper、official_blog',
  website_url VARCHAR(255) DEFAULT NULL COMMENT '来源官网地址',
  description VARCHAR(255) DEFAULT NULL COMMENT '来源说明',
  is_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_ai_source_slug (slug)
) COMMENT='内容来源表';

CREATE TABLE IF NOT EXISTS ai_content (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  slug VARCHAR(220) NOT NULL COMMENT '内容标识',
  content_type VARCHAR(50) NOT NULL COMMENT '内容类型，如news、paper、project、company_update、practice',
  summary VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  cover_image VARCHAR(255) DEFAULT NULL COMMENT '封面图地址',
  category_id BIGINT NOT NULL COMMENT '所属分类ID',
  source_id BIGINT DEFAULT NULL COMMENT '来源ID',
  source_url VARCHAR(500) DEFAULT NULL COMMENT '原始链接',
  author_name VARCHAR(100) DEFAULT NULL COMMENT '作者或录入人',
  publish_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态',
  featured_level INT NOT NULL DEFAULT 0 COMMENT '精选级别',
  view_count INT NOT NULL DEFAULT 0 COMMENT '浏览量',
  reading_time INT DEFAULT NULL COMMENT '预计阅读分钟数',
  published_at DATETIME DEFAULT NULL COMMENT '发布时间',
  body_markdown LONGTEXT NOT NULL COMMENT '正文Markdown内容',
  extra_json JSON DEFAULT NULL COMMENT '扩展信息JSON',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_ai_content_slug (slug),
  KEY idx_ai_content_type (content_type),
  KEY idx_ai_content_status (publish_status),
  KEY idx_ai_content_category (category_id),
  KEY idx_ai_content_source (source_id),
  KEY idx_ai_content_publish_time (published_at),
  CONSTRAINT fk_ai_content_category FOREIGN KEY (category_id) REFERENCES ai_category (id),
  CONSTRAINT fk_ai_content_source FOREIGN KEY (source_id) REFERENCES ai_source (id)
) COMMENT='核心内容表';

CREATE TABLE IF NOT EXISTS ai_content_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  content_id BIGINT NOT NULL COMMENT '内容ID',
  tag_id BIGINT NOT NULL COMMENT '标签ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_ai_content_tag (content_id, tag_id),
  KEY idx_ai_content_tag_tag_id (tag_id),
  CONSTRAINT fk_ai_content_tag_content FOREIGN KEY (content_id) REFERENCES ai_content (id),
  CONSTRAINT fk_ai_content_tag_tag FOREIGN KEY (tag_id) REFERENCES ai_tag (id)
) COMMENT='内容标签关联表';

CREATE TABLE IF NOT EXISTS ai_content_external_ref (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  content_id BIGINT NOT NULL COMMENT '内容ID',
  ref_type VARCHAR(50) NOT NULL COMMENT '外部引用类型，如github_repo、arxiv_paper、official_post',
  external_id VARCHAR(120) DEFAULT NULL COMMENT '外部系统ID',
  external_url VARCHAR(500) DEFAULT NULL COMMENT '外部系统链接',
  raw_payload_json JSON DEFAULT NULL COMMENT '原始抓取数据快照',
  synced_at DATETIME DEFAULT NULL COMMENT '最近同步时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_ai_ext_ref_content_id (content_id),
  KEY idx_ai_ext_ref_type (ref_type),
  CONSTRAINT fk_ai_ext_ref_content FOREIGN KEY (content_id) REFERENCES ai_content (id)
) COMMENT='外部引用与后续同步预留表';

