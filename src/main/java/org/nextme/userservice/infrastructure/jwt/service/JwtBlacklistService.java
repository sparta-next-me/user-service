package org.nextme.userservice.infrastructure.jwt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class JwtBlacklistService {

    private static final String PREFIX = "blacklist:jwt:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 토큰을 블랙리스트에 추가
     * @param token  블랙리스트에 넣을 JWT 문자열
     * @param millis 토큰 만료까지 남은 시간(ms)
     */
    public void blacklist(String token, long millis){
        if (millis <= 0){
            return; // 이미 만료됐으면 패스함
        }
        long seconds = millis / 1000L;
        redisTemplate
                .opsForValue()
                .set(PREFIX + token, "1", Duration.ofSeconds(seconds));
    }

    /**
     * 토큰이 블랙리스트에 있는지 여부
     */
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
    }
}
