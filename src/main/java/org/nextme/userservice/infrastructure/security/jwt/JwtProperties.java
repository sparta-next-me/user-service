package org.nextme.userservice.infrastructure.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
            String secret,
            long accessTokenValiditySeconds,
            long refreshTokenValiditySeconds
) {
}
