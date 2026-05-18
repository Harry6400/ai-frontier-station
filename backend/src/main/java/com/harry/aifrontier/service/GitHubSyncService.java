package com.harry.aifrontier.service;

import com.harry.aifrontier.vo.GitHubSyncResultVO;

import java.util.List;

public interface GitHubSyncService {

    GitHubSyncResultVO syncRepo(Long externalRefId);

    List<GitHubSyncResultVO> syncAllGitHubRepos();
}
