package org.nextme.userservice.application.dto;

// 로그인 요청
public record LoginRequest(
        String userName,
        String password
) {}
