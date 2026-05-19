-- Migration 001: Add TrendRadar fields
-- Run: mysql -uroot -p ai_frontier_station < database/migrations/001_add_trend_fields.sql
-- Date: 2026-05-19

-- Add trend fields to ai_content
ALTER TABLE ai_content ADD COLUMN IF NOT EXISTS trend_score DECIMAL(10,2) DEFAULT 0;
ALTER TABLE ai_content ADD COLUMN IF NOT EXISTS star_growth7d INT DEFAULT 0;
ALTER TABLE ai_content ADD COLUMN IF NOT EXISTS fork_count INT DEFAULT 0;
ALTER TABLE ai_content ADD COLUMN IF NOT EXISTS ai_tags JSON;
ALTER TABLE ai_content ADD COLUMN IF NOT EXISTS ai_direction VARCHAR(50);
ALTER TABLE ai_content ADD COLUMN IF NOT EXISTS ai_difficulty VARCHAR(20);
ALTER TABLE ai_content ADD COLUMN IF NOT EXISTS ai_audience TEXT;
ALTER TABLE ai_content ADD COLUMN IF NOT EXISTS ai_learning_path TEXT;

-- Add trend fields to ai_content_candidate
ALTER TABLE ai_content_candidate ADD COLUMN IF NOT EXISTS trend_score DECIMAL(10,2) DEFAULT 0;
ALTER TABLE ai_content_candidate ADD COLUMN IF NOT EXISTS star_growth7d INT DEFAULT 0;
ALTER TABLE ai_content_candidate ADD COLUMN IF NOT EXISTS fork_count INT DEFAULT 0;
