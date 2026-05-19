package com.harry.aifrontier.controller.admin;

import com.harry.aifrontier.common.api.ApiResponse;
import com.harry.aifrontier.dto.request.LoginRequest;
import com.harry.aifrontier.vo.LoginResponse;
import com.harry.aifrontier.service.AuthService;
import com.harry.aifrontier.vo.AdminUserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<AdminUserVO> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return ApiResponse.success(authService.getCurrentUser(username));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return ApiResponse.success("登出成功", null);
    }
}
