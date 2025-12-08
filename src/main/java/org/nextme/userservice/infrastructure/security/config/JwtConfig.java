package org.nextme.userservice.infrastructure.security.config;

import lombok.RequiredArgsConstructor;
import org.nextme.common.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-validity-seconds}")
    private long accessTokenValiditySeconds;

    @Value("${jwt.refresh-token-validity-seconds}")
    private long refreshTokenValiditySeconds;

    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider(
                secret,
                accessTokenValiditySeconds,
                refreshTokenValiditySeconds
        );
    }
}