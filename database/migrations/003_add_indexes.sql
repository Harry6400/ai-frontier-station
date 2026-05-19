-- Migration 003: Add performance indexes
-- Run after schema is created

USE ai_frontier_station;

-- Index on content title for search
CREATE INDEX idx_content_title ON ai_content(title(100));

-- Index on external ref URL for lookups
CREATE INDEX idx_ext_ref_url ON ai_content_external_ref(external_url(200));

-- Composite index for content type + status filtering
CREATE INDEX idx_content_type_status ON ai_content(content_type, publish_status);
