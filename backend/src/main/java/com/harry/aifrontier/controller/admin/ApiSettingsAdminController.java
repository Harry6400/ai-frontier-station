package com.harry.aifrontier.controller.admin;

import com.harry.aifrontier.common.api.ApiResponse;
import com.harry.aifrontier.dto.request.ApiCredentialSaveRequest;
import com.harry.aifrontier.service.ApiCredentialService;
import com.harry.aifrontier.vo.ApiCredentialStatusVO;
import com.harry.aifrontier.vo.ApiSettingsStatusVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/api-settings")
@RequiredArgsConstructor
public class ApiSettingsAdminController {

    private final ApiCredentialService apiCredentialService;

    @GetMapping("/status")
    public ApiResponse<ApiSettingsStatusVO> status() {
        return ApiResponse.success(apiCredentialService.status());
    }

    @PutMapping("/bailian")
    public ApiResponse<ApiCredentialStatusVO> saveBailian(@Valid @RequestBody ApiCredentialSaveRequest request) {
        return ApiResponse.success("百炼 API Key 已启用", apiCredentialService.save(ApiCredentialService.PROVIDER_BAILIAN, request));
    }

    @DeleteMapping("/bailian")
    public ApiResponse<ApiCredentialStatusVO> clearBailian() {
        return ApiResponse.success("百炼 API Key 已清除", apiCredentialService.clear(ApiCredentialService.PROVIDER_BAILIAN));
    }

    @PutMapping("/github")
    public ApiResponse<ApiCredentialStatusVO> saveGitHub(@Valid @RequestBody ApiCredentialSaveRequest request) {
        return ApiResponse.success("GitHub Token 已启用", apiCredentialService.save(ApiCredentialService.PROVIDER_GITHUB, request));
    }

    @DeleteMapping("/github")
    public ApiResponse<ApiCredentialStatusVO> clearGitHub() {
        return ApiResponse.success("GitHub Token 已清除", apiCredentialService.clear(ApiCredentialService.PROVIDER_GITHUB));
    }
}
