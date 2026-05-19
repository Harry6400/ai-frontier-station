-- Migration: 005_add_event_status
-- Description: Add status column to ai_content_event table for approval workflow

ALTER TABLE ai_content_event ADD COLUMN status VARCHAR(20) DEFAULT 'pending' COMMENT '审核状态: pending/approved/rejected' AFTER view_count;
