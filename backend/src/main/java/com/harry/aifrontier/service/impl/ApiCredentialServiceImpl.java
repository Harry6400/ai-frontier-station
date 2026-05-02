package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harry.aifrontier.dto.request.ApiCredentialSaveRequest;
import com.harry.aifrontier.entity.ApiCredential;
import com.harry.aifrontier.mapper.ApiCredentialMapper;
import com.harry.aifrontier.service.ApiCredentialService;
import com.harry.aifrontier.util.CryptoUtil;
import com.harry.aifrontier.vo.ApiCredentialStatusVO;
import com.harry.aifrontier.vo.ApiSettingsStatusVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiCredentialServiceImpl implements ApiCredentialService {

    private final ApiCredentialMapper apiCredentialMapper;

    private final Map<String, RuntimeCredential> credentials = new ConcurrentHashMap<>();

    @Value("${app.crypto.master-key:ai-frontier-station-default-key-2024}")
    private String masterKey;

    @Value("${app.bailian.api-key:}")
    private String bailianFallbackKey;

    @Value("${app.github.token:}")
    private String githubFallbackToken;

    @Value("${app.mimo.api-key:}")
    private String mimoFallbackKey;

    @PostConstruct
    public void init() {
        log.info("正在从数据库加载已保存的 API 密钥...");
        try {
            for (String provider : new String[]{PROVIDER_BAILIAN, PROVIDER_GITHUB, PROVIDER_MIMO}) {
                loadFromDatabase(provider);
            }
            log.info("API 密钥加载完成");
        } catch (Exception e) {
            log.warn("加载 API 密钥失败，可能是数据库表尚未创建: {}", e.getMessage());
        }
    }

    private void loadFromDatabase(String provider) {
        ApiCredential record = apiCredentialMapper.selectOne(
                new LambdaQueryWrapper<ApiCredential>()
                        .eq(ApiCredential::getProvider, provider)
                        .eq(ApiCredential::getIsEnabled, 1)
        );
        if (record != null) {
            try {
                String decryptedKey = CryptoUtil.decrypt(record.getEncryptedKey(), record.getIv(), masterKey);
                credentials.put(provider, new RuntimeCredential(
                        decryptedKey,
                        record.getRemark(),
                        record.getUpdatedAt()
                ));
                log.info("已从数据库加载 {} 密钥", provider);
            } catch (Exception e) {
                log.warn("解密 {} 密钥失败: {}", provider, e.getMessage());
            }
        }
    }

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
        String apiKey = request.getApiKey().trim();

        CryptoUtil.EncryptedResult encrypted = CryptoUtil.encrypt(apiKey, masterKey);
        String suffix = apiKey.length() > 4 ? apiKey.substring(apiKey.length() - 4) : apiKey;

        ApiCredential existing = apiCredentialMapper.selectOne(
                new LambdaQueryWrapper<ApiCredential>().eq(ApiCredential::getProvider, provider)
        );

        if (existing != null) {
            existing.setEncryptedKey(encrypted.encrypted());
            existing.setIv(encrypted.iv());
            existing.setKeySuffix(suffix);
            existing.setRemark(blankToNull(request.getRemark()));
            existing.setIsEnabled(1);
            existing.setUpdatedAt(LocalDateTime.now());
            apiCredentialMapper.updateById(existing);
        } else {
            ApiCredential record = new ApiCredential();
            record.setProvider(provider);
            record.setEncryptedKey(encrypted.encrypted());
            record.setIv(encrypted.iv());
            record.setKeySuffix(suffix);
            record.setRemark(blankToNull(request.getRemark()));
            record.setIsEnabled(1);
            apiCredentialMapper.insert(record);
        }

        credentials.put(provider, new RuntimeCredential(
                apiKey,
                blankToNull(request.getRemark()),
                LocalDateTime.now()
        ));

        log.info("已加密保存 {} 密钥到数据库", provider);
        return toStatus(provider);
    }

    @Override
    public ApiCredentialStatusVO clear(String provider) {
        validateProvider(provider);
        credentials.remove(provider);

        ApiCredential existing = apiCredentialMapper.selectOne(
                new LambdaQueryWrapper<ApiCredential>().eq(ApiCredential::getProvider, provider)
        );
        if (existing != null) {
            apiCredentialMapper.deleteById(existing.getId());
            log.info("已从数据库删除 {} 密钥", provider);
        }

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
        vo.setMaskedKey(hasRuntime ? CryptoUtil.mask(credential.apiKey()) : hasFallback ? CryptoUtil.mask(fallback) : "");
        vo.setSource(hasRuntime ? "database" : hasFallback ? "environment" : "none");
        vo.setRemark(credential == null ? "" : credential.remark());
        vo.setUpdatedAt(credential == null ? null : credential.updatedAt());
        return vo;
    }

    private void validateProvider(String provider) {
        if (!PROVIDER_BAILIAN.equals(provider) && !PROVIDER_GITHUB.equals(provider) && !PROVIDER_MIMO.equals(provider)) {
            throw new IllegalArgumentException("不支持的 API 配置类型");
        }
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
