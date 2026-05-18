package com.harry.aifrontier.service;

import com.harry.aifrontier.dto.request.LoginRequest;
import com.harry.aifrontier.dto.request.LoginResponse;
import com.harry.aifrontier.vo.AdminUserVO;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    AdminUserVO getCurrentUser(String username);
}
