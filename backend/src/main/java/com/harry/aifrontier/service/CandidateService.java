package com.harry.aifrontier.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.harry.aifrontier.entity.ContentCandidate;

import java.util.Map;

public interface CandidateService {

    /**
     * List candidates with optional filters
     */
    Page<ContentCandidate> listCandidates(String sourceType, String status, Long pageNum, Long pageSize);

    /**
     * Get single candidate by ID
     */
    ContentCandidate getCandidate(Long id);

    /**
     * Update candidate fields (aiSummary, aiBody, metadataJson, title)
     */
    ContentCandidate updateCandidate(Long id, Map<String, String> updates);

    /**
     * Approve candidate and import into Content table.
     * Returns the new Content ID.
     */
    Long approveCandidate(Long id, Map<String, String> overrides);

    /**
     * Reject a candidate
     */
    void rejectCandidate(Long id);

    /**
     * AI-process a candidate: send to AI API and generate structured content
     */
    ContentCandidate aiProcess(Long id, String customPrompt);

    /**
     * Trigger manual fetch for a source type.
     * Returns number of new candidates fetched.
     */
    int fetchFromSource(String sourceType);
    String translateContent(Long contentId);
    int batchTranslateEnglish();

    /**
     * Get the saved custom AI prompt
     */
    String getCustomPrompt();

    /**
     * Save a custom AI prompt
     */
    void saveCustomPrompt(String prompt);
}
