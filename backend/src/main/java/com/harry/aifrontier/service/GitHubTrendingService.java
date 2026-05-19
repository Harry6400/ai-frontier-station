package com.harry.aifrontier.service;

public interface GitHubTrendingService {
    int fetchTrending(String language, String since);
}
