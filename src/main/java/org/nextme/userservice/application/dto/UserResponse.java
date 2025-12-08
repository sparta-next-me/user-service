package org.nextme.userservice.application.dto;

import org.nextme.userservice.domain.*;

import java.util.UUID;

/**
 * 로그인한 유저 "본인 정보" 조회 응답 DTO
 * - 민감 정보(비밀번호, 소셜 계정 목록)는 포함하지 않음
 */
public record UserResponse(
        UUID userId,
        String userName,
        String name,
        String role,
        String slackId,
        String status,
        String advisorStatus
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId().getId(),
                user.getUserName(),
                user.getName(),
                user.getRole().name(),
                user.getSlackId(),
                user.getStatus().name(),
                user.getAdvisorStatus().name()
        );
    }
}
