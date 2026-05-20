-- 007: Arena tables + category seed

-- 模型排行榜表
CREATE TABLE IF NOT EXISTS ai_arena_model (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_name VARCHAR(200) NOT NULL COMMENT '模型名称',
    provider VARCHAR(100) DEFAULT NULL COMMENT '提供商 (OpenAI, Google, Meta ...)',
    current_rank INT DEFAULT NULL COMMENT '当前排名',
    current_elo DECIMAL(8,2) DEFAULT NULL COMMENT '当前 Elo 分数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_model_name (model_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Arena 模型排行榜';

-- 模型历史趋势表
CREATE TABLE IF NOT EXISTS ai_arena_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_id BIGINT NOT NULL COMMENT '关联 ai_arena_model.id',
    elo_score DECIMAL(8,2) DEFAULT NULL COMMENT 'Elo 分数',
    rank_position INT DEFAULT NULL COMMENT '排名',
    recorded_at DATETIME NOT NULL COMMENT '记录时间',
    INDEX idx_model_id (model_id),
    INDEX idx_recorded_at (recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Arena 模型历史趋势';

-- 分析报告表
CREATE TABLE IF NOT EXISTS ai_arena_analysis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content TEXT COMMENT '分析报告内容 (Markdown)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Arena 分析报告';

-- 新增 Arena 分类
INSERT IGNORE INTO ai_category (name, slug, description, sort_order)
VALUES ('模型评测', 'arena', 'AI模型评测排行', 4);
