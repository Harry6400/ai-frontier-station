package com.harry.aifrontier.util;

import com.harry.aifrontier.entity.ContentCandidate;

/**
 * Shared validation for ContentCandidate before persistence.
 * Ensures minimum data quality for all fetch sources.
 */
public final class CandidateValidator {

    private CandidateValidator() {
    }

    /**
     * Validate and sanitize a candidate before insert.
     *
     * @return true if the candidate passes minimum quality checks
     */
    public static boolean validate(ContentCandidate candidate) {
        if (candidate == null) {
            return false;
        }

        // Title must exist and be at least 5 characters
        if (candidate.getTitle() == null || candidate.getTitle().trim().length() < 5) {
            return false;
        }

        // Clean up blank URL to null
        if (candidate.getUrl() != null && candidate.getUrl().isBlank()) {
            candidate.setUrl(null);
        }

        // If raw_content is too short, fall back to title
        if (candidate.getRawContent() != null && candidate.getRawContent().trim().length() < 20) {
            candidate.setRawContent(candidate.getTitle());
        }

        // Ensure title is trimmed
        candidate.setTitle(candidate.getTitle().trim());

        return true;
    }
}
