package org.nextme.userservice.infrastructure.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.nextme.userservice.domain.User;
import org.nextme.userservice.domain.UserId;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component // 스프링 빈으로 등록 (다른 서비스에서 주입해서 사용)
public class JwtTokenProvider {

    // HMAC 서명에 사용할 비밀 키
    private final SecretKey key;

    // application.yml 의 jwt.* 설정을 들고있는 설정 객체
    private final JwtProperties props;

    // 설정 객체를 주입받아서
    // 1) 필드에 저장하고
    // 2) secret 문자열을 바이트 배열로 바꿔서 HMAC 키 생성
    public JwtTokenProvider(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    // ================== 발급 영역 ==================

    /**
     * 유저 + 권한목록을 받아서
     * AccessToken / RefreshToken 두 개를 한 번에 만들어 반환
     */
    public JwtTokenPair generateTokenPair(UserId userId, List<String> roles) {
        String accessToken = generateAccessToken(userId, roles);
        String refreshToken = generateRefreshToken(userId);
        return new JwtTokenPair(accessToken, refreshToken);
    }

    /**
     * AccessToken 발급
     * - subject : 우리 서비스의 UserId(UUID)
     * - roles   : 권한 목록
     * - type    : "access"
     * - exp     : access-token-validity-seconds 만큼 뒤
     */
    public String generateAccessToken(UserId userId, List<String> roles) {
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiry = new Date(now + props.accessTokenValiditySeconds() * 1000L);

        return Jwts.builder()
                .setSubject(userId.getId().toString())  // 여기만 userId 사용
                .claim("roles", roles)
                .claim("type", "access")
                .setIssuedAt(issuedAt)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * RefreshToken 발급
     * - subject : UserId
     * - type    : "refresh"
     * - exp     : refresh-token-validity-seconds 만큼 뒤
     */
    public String generateRefreshToken(UserId userId) {
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiry = new Date(now + props.refreshTokenValiditySeconds() * 1000L);

        return Jwts.builder()
                .setSubject(userId.getId().toString())
                .claim("type", "refresh")
                .setIssuedAt(issuedAt)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ================== 파싱/조회 영역 ==================

    /**
     * 문자열 JWT 를 파싱해서 Claims(페이로드) 꺼내는 메서드
     * - 서명 검증(setSigningKey)까지 같이 수행됨
     * - 서명이 깨졌거나 만료되면 여기서 예외 발생
     */
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)      // 이 키로 서명 검증
                .build()
                .parseClaimsJws(token)   // JWS 파싱 + 검증
                .getBody();              // 페이로드(Claims)만 꺼냄
    }

    /**
     * 토큰에서 우리 서비스의 UserId 값을 꺼내서 UserId 값 객체로 변환
     */
    public UserId getUserId(String token) {
        Claims claims = parseClaims(token);
        UUID uuid = UUID.fromString(claims.getSubject()); // sub 는 문자열이므로 UUID 로 변환
        return UserId.of(uuid);
    }

    /**
     * access / refresh 타입 구분용
     */
    public String getTokenType(String token) {
        return parseClaims(token).get("type", String.class);
    }
}
