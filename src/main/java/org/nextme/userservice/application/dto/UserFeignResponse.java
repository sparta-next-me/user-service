package org.nextme.userservice.application.dto;

import org.nextme.userservice.domain.User;

/**
 * 로그인한 유저 "페인" 조회 응답 DTO
 * - 민감 정보(비밀번호, 소셜 계정 목록)는 포함하지 않음
 */

public record UserFeignResponse (
    String name,
    String role,
    String slackId
    ){
    public static UserFeignResponse from(User user) {
        return new UserFeignResponse(
                user.getName(),
                user.getRole().name(),
                user.getSlackId()
        );
    }
}

