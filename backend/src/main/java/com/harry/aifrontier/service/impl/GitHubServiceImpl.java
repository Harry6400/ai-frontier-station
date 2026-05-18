package com.harry.aifrontier.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.harry.aifrontier.common.api.PageResult;
import com.harry.aifrontier.service.ApiCredentialService;
import com.harry.aifrontier.service.GitHubService;
import com.harry.aifrontier.vo.GitHubRepoCandidateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GitHubServiceImpl implements GitHubService {

    private final ApiCredentialService apiCredentialService;

    @Value("${app.github.token:}")
    private String githubToken;

    @Value("${app.github.base-url:https://api.github.com}")
    private String githubBaseUrl;

    @Override
    public GitHubRepoCandidateVO findRepo(String fullName) {
        String normalized = normalizeFullName(fullName);
        try {
            JsonNode repo = githubClient()
                    .get()
                    .uri("/repos/{owner}/{repo}", normalized.split("/")[0], normalized.split("/")[1])
                    .retrieve()
                    .body(JsonNode.class);
            GitHubRepoCandidateVO vo = toCandidate(repo);
            vo.setReadmeSummary(fetchReadmeSummary(normalized));
            return vo;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new IllegalArgumentException("GitHub 仓库不存在，请检查 owner/repo 是否正确");
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new IllegalArgumentException("GitHub Token 无效，请在 API 设置页重新配置");
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new IllegalArgumentException("GitHub API 访问受限，可能已触发限流，请稍后重试或配置 Token");
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("GitHub 服务暂时无法访问，请检查网络后重试");
        }
    }

    @Override
    public PageResult<GitHubRepoCandidateVO> searchRepos(String keyword, String sort, Long pageNum, Long pageSize) {
        String q = keyword == null || keyword.isBlank() ? "ai llm" : keyword.trim();
        String safeSort = resolveSort(sort);
        long current = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long size = pageSize == null || pageSize < 1 ? 10L : Math.min(pageSize, 20L);
        try {
            JsonNode result = githubClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/repositories")
                            .queryParam("q", q)
                            .queryParam("sort", safeSort)
                            .queryParam("order", "desc")
                            .queryParam("page", current)
                            .queryParam("per_page", size)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            List<GitHubRepoCandidateVO> records = new ArrayList<>();
            result.path("items").forEach(item -> records.add(toCandidate(item)));
            return PageResult.of(result.path("total_count").asLong(records.size()), current, size, records);
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new IllegalArgumentException("GitHub Token 无效，请在 API 设置页重新配置");
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new IllegalArgumentException("GitHub 搜索访问受限，可能已触发限流，请稍后重试或配置 Token");
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("GitHub 搜索暂时无法访问，请检查网络后重试");
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

    private GitHubRepoCandidateVO toCandidate(JsonNode node) {
        GitHubRepoCandidateVO vo = new GitHubRepoCandidateVO();
        vo.setRepoFullName(text(node, "full_name"));
        vo.setRepoUrl(text(node, "html_url"));
        vo.setDescription(text(node, "description"));
        vo.setStars(node.path("stargazers_count").asInt(0));
        vo.setForks(node.path("forks_count").asInt(0));
        vo.setWatchers(node.path("watchers_count").asInt(0));
        vo.setLanguage(text(node, "language"));
        vo.setHomepage(text(node, "homepage"));
        vo.setDefaultBranch(text(node, "default_branch"));
        vo.setUpdatedAt(parseDateTime(text(node, "updated_at")));
        List<String> topics = new ArrayList<>();
        node.path("topics").forEach(item -> {
            if (!item.asText().isBlank()) {
                topics.add(item.asText());
            }
        });
        vo.setTopics(topics);
        return vo;
    }

    private String fetchReadmeSummary(String fullName) {
        try {
            JsonNode readme = githubClient()
                    .get()
                    .uri("/repos/{owner}/{repo}/readme", fullName.split("/")[0], fullName.split("/")[1])
                    .retrieve()
                    .body(JsonNode.class);
            String encoded = readme.path("content").asText("").replaceAll("\\s+", "");
            if (encoded.isBlank()) {
                return "";
            }
            String markdown = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            return markdown.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .filter(line -> !line.startsWith("[!") && !line.startsWith("<"))
                    .limit(8)
                    .reduce((left, right) -> left + "\n" + right)
                    .orElse("");
        } catch (Exception ex) {
            return "";
        }
    }

    private String normalizeFullName(String fullName) {
        if (fullName == null || fullName.isBlank() || !fullName.trim().contains("/")) {
            throw new IllegalArgumentException("请输入合法 GitHub 仓库全名，例如 openai/openai-cookbook");
        }
        String normalized = fullName.trim();
        if (normalized.split("/").length != 2) {
            throw new IllegalArgumentException("请输入合法 GitHub 仓库全名，例如 openai/openai-cookbook");
        }
        return normalized;
    }

    private String resolveSort(String sort) {
        if ("updated".equals(sort) || "forks".equals(sort) || "stars".equals(sort)) {
            return sort;
        }
        return "stars";
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(value).toLocalDateTime();
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText("");
        return value.isBlank() ? "" : value.trim();
    }
}
