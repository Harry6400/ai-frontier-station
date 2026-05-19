-- Migration: 006_clean_dirty_data
-- Description: Clean up orphaned data and stale candidates
-- 
-- 1. Delete non-paper candidates that are still 'pending' (they should have been auto-published)
-- 2. Delete events that have no linked content (orphaned events)
-- 3. Reset orphaned content that has a linked candidate but the candidate was rejected/deleted

-- 1. Remove stale non-paper pending candidates
-- Non-paper candidates (news, product, tools, arena) should be auto-published,
-- so any still-pending ones are stale/dirty data.
DELETE FROM ai_content_candidate
WHERE status = 'pending'
  AND source_type != 'paper';

-- 2. Remove orphaned events (events with no linked content)
DELETE FROM ai_content_event
WHERE id NOT IN (
    SELECT DISTINCT event_id FROM ai_content_event_link
);

-- 3. Reset orphaned content: content that was linked to a candidate but the candidate
-- was rejected or no longer exists, and the content is still in DRAFT status.
-- Set them back to PUBLISHED if they were auto-imported (already have published_at).
UPDATE ai_content c
LEFT JOIN ai_content_candidate cand ON cand.content_id = c.id
SET c.publish_status = 'PUBLISHED'
WHERE c.publish_status = 'DRAFT'
  AND c.published_at IS NOT NULL
  AND (cand.id IS NULL OR cand.status = 'rejected');
