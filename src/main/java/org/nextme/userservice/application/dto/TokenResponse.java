package org.nextme.userservice.application.dto;
import java.util.List;

// RefreshToken을 이용한 AccessToken 재발급을 위한 dto
public record TokenResponse(
        String userId,
        String name,
        String email,
        String slackId,
        List<String> roles,
        String accessToken,
        String refreshToken
) {
}