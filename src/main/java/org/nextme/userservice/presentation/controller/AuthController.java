package org.nextme.userservice.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.nextme.infrastructure.success.CustomResponse;
import org.nextme.userservice.application.dto.LoginRequest;
import org.nextme.userservice.application.dto.SignupRequest;
import org.nextme.userservice.application.dto.TokenResponse;
import org.nextme.userservice.application.service.AuthService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/user")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/auth/signup")
    public CustomResponse<Void> signup(@RequestBody @Validated SignupRequest request) {
        authService.signup(request);
        return CustomResponse.onSuccess("회원가입에 성공했습니다.", null);
    }

    @PostMapping("/auth/login")
    public CustomResponse<TokenResponse> login(@RequestBody @Validated LoginRequest request) {
        TokenResponse response = authService.login(request);
        return CustomResponse.onSuccess("로그인에 성공했습니다.", response);
    }
}