package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.harry.aifrontier.entity.ContentExternalRef;
import com.harry.aifrontier.mapper.ContentExternalRefMapper;
import com.harry.aifrontier.service.ApiCredentialService;
import com.harry.aifrontier.service.GitHubSyncService;
import com.harry.aifrontier.vo.GitHubSyncResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubSyncServiceImpl implements GitHubSyncService {

    private final ContentExternalRefMapper contentExternalRefMapper;
    private final ApiCredentialService apiCredentialService;

    @Value("${app.github.token:}")
    private String githubToken;

    @Value("${app.github.base-url:https://api.github.com}")
    private String githubBaseUrl;

    @Override
    public GitHubSyncResultVO syncRepo(Long externalRefId) {
        ContentExternalRef ref = contentExternalRefMapper.selectById(externalRefId);
        if (ref == null) {
            throw new IllegalArgumentException("外部引用记录不存在");
        }
        if (!"github_repo".equals(ref.getRefType())) {
            throw new IllegalArgumentException("该记录不是 GitHub 仓库类型");
        }
        return syncSingleRepo(ref);
    }

    @Override
    public List<GitHubSyncResultVO> syncAllGitHubRepos() {
        List<ContentExternalRef> githubRefs = contentExternalRefMapper.selectList(
                new LambdaQueryWrapper<ContentExternalRef>()
                        .eq(ContentExternalRef::getRefType, "github_repo")
        );

        List<GitHubSyncResultVO> results = new ArrayList<>();
        for (ContentExternalRef ref : githubRefs) {
            try {
                GitHubSyncResultVO result = syncSingleRepo(ref);
                results.add(result);
            } catch (Exception e) {
                log.warn("同步 GitHub 仓库失败: {} - {}", ref.getExternalId(), e.getMessage());
                GitHubSyncResultVO errorResult = new GitHubSyncResultVO();
                errorResult.setExternalRefId(ref.getId());
                errorResult.setRepoFullName(ref.getExternalId());
                errorResult.setSyncStatus("error");
                errorResult.setSyncError(e.getMessage());
                results.add(errorResult);
            }
        }
        return results;
    }

    private GitHubSyncResultVO syncSingleRepo(ContentExternalRef ref) {
        String repoFullName = ref.getExternalId();
        if (repoFullName == null || !repoFullName.contains("/")) {
            throw new IllegalArgumentException("仓库全名格式不正确: " + repoFullName);
        }

        try {
            String[] parts = repoFullName.split("/");
            JsonNode repo = githubClient()
                    .get()
                    .uri("/repos/{owner}/{repo}", parts[0], parts[1])
                    .retrieve()
                    .body(JsonNode.class);

            int stars = repo.path("stargazers_count").asInt(0);
            int forks = repo.path("forks_count").asInt(0);
            int watchers = repo.path("watchers_count").asInt(0);
            String language = repo.path("language").asText("");
            String updatedAtStr = repo.path("updated_at").asText("");

            ref.setStars(stars);
            ref.setForks(forks);
            ref.setWatchers(watchers);
            ref.setLanguage(language.isBlank() ? null : language);
            ref.setLastSyncedAt(LocalDateTime.now());
            ref.setSyncStatus("synced");
            ref.setSyncError(null);
            contentExternalRefMapper.updateById(ref);

            log.info("GitHub 仓库同步成功: {} - {} stars, {} forks", repoFullName, stars, forks);

            GitHubSyncResultVO result = new GitHubSyncResultVO();
            result.setExternalRefId(ref.getId());
            result.setRepoFullName(repoFullName);
            result.setStars(stars);
            result.setForks(forks);
            result.setWatchers(watchers);
            result.setLanguage(language.isBlank() ? null : language);
            result.setSyncStatus("synced");
            result.setSyncedAt(LocalDateTime.now());
            return result;
        } catch (Exception e) {
            ref.setSyncStatus("error");
            ref.setSyncError(e.getMessage());
            contentExternalRefMapper.updateById(ref);
            throw new IllegalArgumentException("GitHub API 调用失败: " + e.getMessage());
        }
    }

    private RestClient githubClient() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(githubBaseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader(HttpHeaders.USER_AGENT, "AI-Frontier-Station");
        String token = apiCredentialService.resolveGitHubToken(githubToken);
        if (token != null && !token.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim());
        }
        return builder.build();
    }
}
