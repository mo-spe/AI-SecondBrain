package com.secondbrain.service;

import com.secondbrain.dto.LoginDTO;
import com.secondbrain.dto.LoginResponseDTO;
import com.secondbrain.dto.RegisterDTO;

public interface AuthService {
    void register(RegisterDTO registerDTO);
    LoginResponseDTO login(LoginDTO loginDTO);
}
