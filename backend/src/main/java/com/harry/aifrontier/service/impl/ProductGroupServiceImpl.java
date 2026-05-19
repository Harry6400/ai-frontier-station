package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harry.aifrontier.entity.ContentCandidate;
import com.harry.aifrontier.mapper.ContentCandidateMapper;
import com.harry.aifrontier.service.ProductGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pipeline C: group product-update candidates by product name.
 * Merges metadata so that related updates are discoverable as a group.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductGroupServiceImpl implements ProductGroupService {

    private final ContentCandidateMapper candidateMapper;

    @Override
    public int autoGroup() {
        List<ContentCandidate> pending = candidateMapper.selectList(
                new LambdaQueryWrapper<ContentCandidate>()
                        .eq(ContentCandidate::getSourceType, "product")
                        .eq(ContentCandidate::getStatus, "pending")
                        .orderByDesc(ContentCandidate::getCreatedAt));

        if (pending.isEmpty()) {
            return 0;
        }

        // Group candidates by extracting a product key from the title
        Map<String, List<ContentCandidate>> groups = pending.stream()
                .collect(Collectors.groupingBy(this::extractProductKey));

        int grouped = 0;
        for (Map.Entry<String, List<ContentCandidate>> entry : groups.entrySet()) {
            String productKey = entry.getKey();
            List<ContentCandidate> members = entry.getValue();

            // If there are multiple candidates for the same product, tag them
            if (members.size() > 1) {
                for (ContentCandidate candidate : members) {
                    try {
                        tagWithGroup(candidate, productKey, members.size());
                        grouped++;
                    } catch (Exception e) {
                        log.warn("Product grouping failed for candidate {}: {}",
                                candidate.getId(), e.getMessage());
                    }
                }
            } else {
                // Single item - still tag it
                tagWithGroup(members.get(0), productKey, 1);
                grouped++;
            }
        }

        if (grouped > 0) {
            log.info("Product auto-group: {} candidates organized into {} product groups",
                    grouped, groups.size());
        }
        return grouped;
    }

    /**
     * Extract a product key from the candidate title.
     * Uses the company/product name as the group identifier.
     * Keeps company prefix (OpenAI, Google, etc.) so events can show "{Company}最新动态".
     */
    private String extractProductKey(ContentCandidate candidate) {
        String title = candidate.getTitle();
        if (title == null || title.isBlank()) {
            return "unknown";
        }
        // Extract company/product name: take first segment before common delimiters
        String key = title.split("[：:|\\-–—]")[0].trim();
        // If the first segment is very long, try to extract the brand name
        if (key.length() > 30) {
            // Take first 2 words as the product/company name
            String[] words = key.split("\\s+");
            if (words.length >= 2) {
                key = words[0] + " " + words[1];
            } else {
                key = key.substring(0, Math.min(30, key.length()));
            }
        }
        return key.isEmpty() ? title.trim() : key;
    }

    /**
     * Tag a candidate with group metadata.
     * Stores product group key and a suggested event title like "{ProductName}最新动态".
     */
    private void tagWithGroup(ContentCandidate candidate, String productKey, int groupSize) {
        String suggestedTitle = productKey + "最新动态";
        String existingMeta = candidate.getMetadataJson();
        StringBuilder meta;
        if (existingMeta != null && !existingMeta.isBlank()) {
            meta = new StringBuilder(existingMeta);
            // Append group info before closing brace if it looks like JSON
            int lastBrace = meta.lastIndexOf("}");
            if (lastBrace > 0) {
                meta.insert(lastBrace, ", \"productGroup\": \"" + escapeJson(productKey)
                        + "\", \"productGroupSize\": " + groupSize
                        + ", \"suggestedEventTitle\": \"" + escapeJson(suggestedTitle) + "\"");
            }
        } else {
            meta = new StringBuilder("{\"productGroup\": \"")
                    .append(escapeJson(productKey))
                    .append("\", \"productGroupSize\": ")
                    .append(groupSize)
                    .append(", \"suggestedEventTitle\": \"")
                    .append(escapeJson(suggestedTitle))
                    .append("\"}");
        }
        candidate.setMetadataJson(meta.toString());
        candidate.setUpdatedAt(LocalDateTime.now());
        candidateMapper.updateById(candidate);
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
