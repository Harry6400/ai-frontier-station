package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harry.aifrontier.dto.request.LoginRequest;
import com.harry.aifrontier.vo.LoginResponse;
import com.harry.aifrontier.entity.AdminUser;
import com.harry.aifrontier.mapper.AdminUserMapper;
import com.harry.aifrontier.security.JwtUtil;
import com.harry.aifrontier.service.AuthService;
import com.harry.aifrontier.vo.AdminUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /** Failed login attempt counter (username -> count) */
    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();

    /** Token blacklist for logout invalidation */
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    @Override
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();

        // Brute force protection: block if too many failed attempts
        Integer attempts = failedAttempts.getOrDefault(username, 0);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            throw new IllegalArgumentException("登录失败次数过多，请5分钟后重试");
        }

        AdminUser user = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>()
                        .eq(AdminUser::getUsername, username)
                        .eq(AdminUser::getIsEnabled, 1)
        );

        if (user == null) {
            failedAttempts.merge(username, 1, Integer::sum);
            throw new IllegalArgumentException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            failedAttempts.merge(username, 1, Integer::sum);
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // Login successful - clear failed attempts
        failedAttempts.remove(username);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        user.setLastLoginAt(LocalDateTime.now());
        adminUserMapper.updateById(user);

        log.info("管理员 {} 登录成功", user.getUsername());

        return new LoginResponse(
                token,
                user.getUsername(),
                user.getDisplayName(),
                user.getRole()
        );
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            blacklistedTokens.add(token);
            log.info("Token 已加入黑名单");
        }
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }

    /** Clear failed login attempts every 5 minutes */
    @Scheduled(fixedRate = 300_000)
    public void clearFailedAttempts() {
        if (!failedAttempts.isEmpty()) {
            log.debug("清除登录失败计数器，共 {} 条记录", failedAttempts.size());
            failedAttempts.clear();
        }
    }

    /** Periodically clean expired tokens from blacklist (every 30 minutes) */
    @Scheduled(fixedRate = 1_800_000)
    public void cleanupBlacklistedTokens() {
        if (!blacklistedTokens.isEmpty()) {
            int before = blacklistedTokens.size();
            blacklistedTokens.removeIf(token -> !jwtUtil.validateToken(token));
            int removed = before - blacklistedTokens.size();
            if (removed > 0) {
                log.debug("清理过期黑名单Token {} 条", removed);
            }
        }
    }

    @Override
    public AdminUserVO getCurrentUser(String username) {
        AdminUser user = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>()
                        .eq(AdminUser::getUsername, username)
                        .eq(AdminUser::getIsEnabled, 1)
        );

        if (user == null) {
            throw new IllegalArgumentException("用户不存在或已被禁用");
        }

        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setDisplayName(user.getDisplayName());
        vo.setRole(user.getRole());
        vo.setLastLoginAt(user.getLastLoginAt());
        return vo;
    }
}
