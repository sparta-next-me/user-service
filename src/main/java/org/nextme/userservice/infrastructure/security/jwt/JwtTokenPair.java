package org.nextme.userservice.infrastructure.security.jwt;

public record JwtTokenPair(
        String accessToken,
        String refreshToken
) {
}
