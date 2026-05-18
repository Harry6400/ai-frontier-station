package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harry.aifrontier.dto.request.LoginRequest;
import com.harry.aifrontier.dto.request.LoginResponse;
import com.harry.aifrontier.entity.AdminUser;
import com.harry.aifrontier.mapper.AdminUserMapper;
import com.harry.aifrontier.security.JwtUtil;
import com.harry.aifrontier.service.AuthService;
import com.harry.aifrontier.vo.AdminUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
        AdminUser user = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>()
                        .eq(AdminUser::getUsername, request.getUsername())
                        .eq(AdminUser::getIsEnabled, 1)
        );

        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

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
