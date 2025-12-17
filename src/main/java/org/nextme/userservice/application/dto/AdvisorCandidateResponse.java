package org.nextme.userservice.application.dto;

import org.nextme.userservice.domain.User;

import java.util.UUID;

/**
 * 어드바이저 승급 후보(신청자) 조회 응답 DTO
 */
public record AdvisorCandidateResponse(
        UUID userId,
        String userName,
        String name,
        String role,
        String advisorStatus
) {

    public static AdvisorCandidateResponse from(User user) {
        return new AdvisorCandidateResponse(
                user.getId().getId(),
                user.getUserName(),
                user.getName(),
                user.getRole().name(),
                user.getAdvisorStatus().name()
        );
    }
}
