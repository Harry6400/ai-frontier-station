package com.harry.aifrontier.service.impl;

import com.harry.aifrontier.dto.request.ApiCredentialSaveRequest;
import com.harry.aifrontier.service.ApiCredentialService;
import com.harry.aifrontier.vo.ApiCredentialStatusVO;
import com.harry.aifrontier.vo.ApiSettingsStatusVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ApiCredentialServiceImpl implements ApiCredentialService {

    private final Map<String, RuntimeCredential> credentials = new ConcurrentHashMap<>();

    @Value("${app.bailian.api-key:}")
    private String bailianFallbackKey;

    @Value("${app.github.token:}")
    private String githubFallbackToken;

    @Value("${app.mimo.api-key:}")
    private String mimoFallbackKey;

    @Override
    public ApiSettingsStatusVO status() {
        ApiSettingsStatusVO vo = new ApiSettingsStatusVO();
        vo.setBailian(toStatus(PROVIDER_BAILIAN));
        vo.setGithub(toStatus(PROVIDER_GITHUB));
        vo.setMimo(toStatus(PROVIDER_MIMO));
        return vo;
    }

    @Override
    public ApiCredentialStatusVO save(String provider, ApiCredentialSaveRequest request) {
        validateProvider(provider);
        credentials.put(provider, new RuntimeCredential(
                request.getApiKey().trim(),
                blankToNull(request.getRemark()),
                LocalDateTime.now()
        ));
        return toStatus(provider);
    }

    @Override
    public ApiCredentialStatusVO clear(String provider) {
        validateProvider(provider);
        credentials.remove(provider);
        return toStatus(provider);
    }

    @Override
    public String resolveBailianApiKey(String fallback) {
        return resolve(PROVIDER_BAILIAN, fallback);
    }

    @Override
    public String resolveGitHubToken(String fallback) {
        return resolve(PROVIDER_GITHUB, fallback);
    }

    @Override
    public String resolveMimoApiKey(String fallback) {
        return resolve(PROVIDER_MIMO, fallback);
    }

    private String resolve(String provider, String fallback) {
        RuntimeCredential credential = credentials.get(provider);
        if (credential != null && credential.apiKey() != null && !credential.apiKey().isBlank()) {
            return credential.apiKey();
        }
        return fallback == null ? "" : fallback.trim();
    }

    private ApiCredentialStatusVO toStatus(String provider) {
        RuntimeCredential credential = credentials.get(provider);
        String fallback;
        if (PROVIDER_BAILIAN.equals(provider)) {
            fallback = bailianFallbackKey;
        } else if (PROVIDER_MIMO.equals(provider)) {
            fallback = mimoFallbackKey;
        } else {
            fallback = githubFallbackToken;
        }
        boolean hasRuntime = credential != null && credential.apiKey() != null && !credential.apiKey().isBlank();
        boolean hasFallback = !hasRuntime && fallback != null && !fallback.isBlank();
        ApiCredentialStatusVO vo = new ApiCredentialStatusVO();
        vo.setProvider(provider);
        vo.setEnabled(hasRuntime || hasFallback);
        vo.setMaskedKey(hasRuntime ? mask(credential.apiKey()) : hasFallback ? mask(fallback) : "");
        vo.setSource(hasRuntime ? "runtime" : hasFallback ? "environment" : "none");
        vo.setRemark(credential == null ? "" : credential.remark());
        vo.setUpdatedAt(credential == null ? null : credential.updatedAt());
        return vo;
    }

    private void validateProvider(String provider) {
        if (!PROVIDER_BAILIAN.equals(provider) && !PROVIDER_GITHUB.equals(provider) && !PROVIDER_MIMO.equals(provider)) {
            throw new IllegalArgumentException("不支持的 API 配置类型");
        }
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "****" + trimmed.substring(trimmed.length() - 4);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record RuntimeCredential(String apiKey, String remark, LocalDateTime updatedAt) {
    }
}
