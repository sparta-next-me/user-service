package org.nextme.userservice.application.dto;

// 회원가입 요청
public record SignupRequest(
        String userName,
        String password,
        String name,
        String slackId
) {}