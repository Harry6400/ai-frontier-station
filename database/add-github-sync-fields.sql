USE ai_frontier_station;

ALTER TABLE ai_content_external_ref
ADD COLUMN stars INT DEFAULT NULL COMMENT 'GitHub Star 数',
ADD COLUMN forks INT DEFAULT NULL COMMENT 'GitHub Fork 数',
ADD COLUMN watchers INT DEFAULT NULL COMMENT 'GitHub Watcher 数',
ADD COLUMN language VARCHAR(50) DEFAULT NULL COMMENT 'GitHub 主要语言',
ADD COLUMN last_synced_at DATETIME DEFAULT NULL COMMENT '最近同步时间',
ADD COLUMN sync_status VARCHAR(20) DEFAULT 'pending' COMMENT '同步状态：pending/synced/error',
ADD COLUMN sync_error VARCHAR(255) DEFAULT NULL COMMENT '同步错误信息';
