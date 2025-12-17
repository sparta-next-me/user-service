package org.nextme.userservice.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextme.common.jwt.JwtTokenPair;
import org.nextme.common.jwt.JwtTokenProvider;
import org.nextme.common.jwt.TokenBlacklistService;
import org.nextme.infrastructure.exception.ApplicationException;
import org.nextme.infrastructure.exception.ErrorCode;
import org.nextme.userservice.application.dto.TokenResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    // ==========================
    //  공통 에러 생성
    // ==========================
    private ApplicationException invalidRefreshTokenException() {
        ErrorCode e = ErrorCode.INVALID_REFRESH_TOKEN;
        return new ApplicationException(
                e.getHttpStatus(),
                e.getCode(),
                e.getDefaultMessage()
        );
    }

    // ==========================
    //  1) 리프레시 토큰으로 재발급
    // ==========================
    /**
     * 리프레시 토큰을 사용한 토큰 재발급
     *
     * - 요청 헤더:
     *   Authorization: Bearer {refreshToken}
     *
     * - 조건:
     *   - JWT 유효해야 함
     *   - type == "refresh" 이어야 함
     *   - 블랙리스트에 있으면 안 됨
     *
     * - 응답:
     *   - 새 accessToken + 새 refreshToken
     *   - userId, name, email, slackId, roles 포함
     */
    public TokenResponse refreshToken(String authorizationHeader) {
        log.debug("[refreshToken] Authorization header = {}", authorizationHeader);

        if(!StringUtils.hasText(authorizationHeader) ||
            !authorizationHeader.startsWith("Bearer")){
            log.info("[refreshToken] Authorization header invalid");
            throw invalidRefreshTokenException();
        }

        String refreshToken = authorizationHeader.substring(7);
        log.info("[refreshToken] refreshToken = {}", refreshToken);

        // 1) 토큰 유효성 검사
        boolean valid = jwtTokenProvider.validateToken(refreshToken);
        log.info("[refreshToken] valid = {}", valid);
        if(!valid){
            throw invalidRefreshTokenException();
        }

        // 2) type == "refresh" 인지 확인
        String type = jwtTokenProvider.getTokenType(refreshToken);
        log.info("[refreshToken] type = {}", type);
        if(!"refresh".equals(type)){
            throw invalidRefreshTokenException();
        }

        // 3) 블랙 리스트 여부 확인
        boolean blacklisted = tokenBlacklistService.isBlacklisted(refreshToken);
        log.info("[refreshToken] blacklisted = {}", blacklisted);
        if(blacklisted){
            throw invalidRefreshTokenException();
        }

        // 4) 클래임에서 데이터 꺼내기
        String userId = jwtTokenProvider.getUserId(refreshToken);
        String name = jwtTokenProvider.getName(refreshToken);
        String email = jwtTokenProvider.getEmail(refreshToken);
        String slackId = jwtTokenProvider.getSlackId(refreshToken);
        List<String> roles = jwtTokenProvider.getRoles(refreshToken);

        log.info("[refreshToken] claims userId={}, name={}, email={}, roles={}",
                userId, name, email, roles);

        // 4-1) 기존 refresh 토큰 블랙리스트에 등록
        long remainingMs = jwtTokenProvider.getRemainingValidityMillis(refreshToken);
        tokenBlacklistService.blacklist(refreshToken, remainingMs);

        // 5) 새 토큰 쌍 발급
        JwtTokenPair newPair = jwtTokenProvider.generateTokenPair(
                userId,
                name,
                email,
                slackId,
                roles
        );

        return new TokenResponse(
                userId,
                name,
                email,
                slackId,
                roles,
                newPair.accessToken(),
                newPair.refreshToken()
        );
    }

    /**
     * 로그아웃
     *
     * - 요청 헤더
     *   Authorization: Bearer {accessToken}
     *   X-Refresh-Token: {refreshToken} (선택, 있으면 같이 블랙리스트 처리)
     *
     * - 동작
     *   1) accessToken 유효하면 남은 TTL 만큼 블랙리스트에 등록
     *   2) refreshToken 이 있으면, 유효 + type=refresh 인 경우 블랙리스트에 등록
     *   3) 토큰이 없거나 이미 만료/이상해도 그냥 조용히 리턴 (idempotent)
     */
    public void logout(String authorizationHeader, String refreshTokenHeader) {
        handleAccessToken(authorizationHeader);
        handleRefreshToken(refreshTokenHeader);
    }

    private void handleAccessToken(String authoziationHeader){
        if (!StringUtils.hasText(authoziationHeader) ||
            !authoziationHeader.startsWith("Bearer ")) {
            log.info("[logout] No Authorization Bearer access token found.");
            return;
        }

        String accessToken = authoziationHeader.substring(7);

        boolean valid = jwtTokenProvider.validateToken(accessToken);
        if (!valid){
            log.info("[logout] Authorization access token invalid or expired.");
            return;
        }

        String type = jwtTokenProvider.getTokenType(accessToken);
        if (!"access".equals(type)){
            log.info("[logout] Authorization token is not access type. type={}", type);
            return;
        }

        long remainingMs = jwtTokenProvider.getRemainingValidityMillis(accessToken);
        log.info("[logout] blacklist accessToken. remainingMs={}", remainingMs);
        tokenBlacklistService.blacklist(accessToken, remainingMs);
    }

    private void handleRefreshToken(String refreshTokenHeader) {
        if (!StringUtils.hasText(refreshTokenHeader)) {
            log.info("[logout] No X-Refresh-Token header provided.");
            return;
        }

        String refreshToken = refreshTokenHeader;
        if (refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }

        boolean valid = jwtTokenProvider.validateToken(refreshToken);
        if (!valid) {
            log.info("[logout] X-Refresh-Token invalid or expired.");
            return;
        }

        String type = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(type)) {
            log.info("[logout] X-Refresh-Token is not refresh type. type={}", type);
            return;
        }

        long remainingMs = jwtTokenProvider.getRemainingValidityMillis(refreshToken);
        log.info("[logout] blacklist refreshToken. remainingMs={}", remainingMs);
        tokenBlacklistService.blacklist(refreshToken, remainingMs);
    }

}
