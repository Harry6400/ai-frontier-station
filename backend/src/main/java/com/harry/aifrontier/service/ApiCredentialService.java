package com.harry.aifrontier.service;

import com.harry.aifrontier.dto.request.ApiCredentialSaveRequest;
import com.harry.aifrontier.vo.ApiCredentialStatusVO;
import com.harry.aifrontier.vo.ApiSettingsStatusVO;

public interface ApiCredentialService {

    String PROVIDER_BAILIAN = "bailian";

    String PROVIDER_GITHUB = "github";

    String PROVIDER_MIMO = "mimo";

    ApiSettingsStatusVO status();

    ApiCredentialStatusVO save(String provider, ApiCredentialSaveRequest request);

    ApiCredentialStatusVO clear(String provider);

    String resolveBailianApiKey(String fallback);

    String resolveGitHubToken(String fallback);

    String resolveMimoApiKey(String fallback);
}
