package com.clinicsystem.service;

import com.clinicsystem.dto.request.LoginRequest;
import com.clinicsystem.dto.request.RegisterRequest;
import com.clinicsystem.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}