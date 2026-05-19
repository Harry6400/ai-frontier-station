package com.harry.aifrontier.service;

public interface AutoPublishService {
    void publishCandidate(Long candidateId);
    int publishPending(String sourceType);
}
