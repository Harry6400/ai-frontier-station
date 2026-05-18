CREATE TABLE IF NOT EXISTS ai_fetch_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  source VARCHAR(50) NOT NULL COMMENT '来源: arxiv/huggingface/github/rss',
  keyword VARCHAR(200) DEFAULT NULL COMMENT '搜索关键词',
  fetched_count INT DEFAULT 0 COMMENT '抓到条数',
  imported_count INT DEFAULT 0 COMMENT '新入库条数',
  status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAILED',
  error_message TEXT DEFAULT NULL COMMENT '失败原因',
  duration_ms BIGINT DEFAULT 0 COMMENT '采集耗时(毫秒)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_fetch_log_source (source),
  KEY idx_fetch_log_created (created_at)
) COMMENT='采集日志表';
