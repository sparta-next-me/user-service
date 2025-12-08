package org.nextme.userservice.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.nextme.common.jwt.JwtTokenProvider;
import org.nextme.common.security.UserPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 *   Gateway 없이 user-service에 직접 요청할 때를 위한 JWT 필터
 *
 * - 이미 SecurityContext에 인증이 있으면 아무 것도 안 함 (Gateway가 인증해줬다는 뜻)
 * - Authorization: Bearer 토큰이 없으면 패스
 * - 토큰이 있으면:
 *      - 유효성 검사 + type=access 확인
 *      - userId, roles 꺼내서 UserPrincipal 생성
 *      - SecurityContext 에 Authentication 세팅
 *
 * ⇒ 이렇게 해두면:
 *   - Gateway 경유: JwtGatewayFilter → X-User-* 헤더 → GatewayUserHeaderAuthenticationFilter 가 인증
 *   - 직접 호출: Authorization: Bearer xxx → DirectJwtAuthenticationFilter 가 인증
 */
@RequiredArgsConstructor
public class DirectJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // 인증 흐름 전용 엔드포인트는 이 필터 완전 제외
        return uri.startsWith("/v1/user/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. 이미 인증이 있으면 건드리지 않음 (Gateway 경유 케이스 등)
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Authorization 헤더에서 Bearer 토큰 추출
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(bearer) || !bearer.startsWith("Bearer ")) {
            // 토큰 없으면 그냥 다음 필터로
            filterChain.doFilter(request, response);
            return;
        }

        String token = bearer.substring(7);

        // 3. 토큰 유효성 검사
        if (!jwtTokenProvider.validateToken(token)) {
            // 여기서 바로 401 줄 수도 있는데, 일단은 그냥 통과시키고
            // 최종적으로 @PreAuthorize 에서 막히게 할 수도 있음
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 4. access 토큰인지 확인
        String tokenType = jwtTokenProvider.getTokenType(token);
        if (!"access".equals(tokenType)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 5. userId, roles 추출
        String userId = jwtTokenProvider.getUserId(token);
        List<String> roles = jwtTokenProvider.getRoles(token);
        String name    = jwtTokenProvider.getName(token);
        String email   = jwtTokenProvider.getEmail(token);
        String slackId = jwtTokenProvider.getSlackId(token);

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .toList();

        UserPrincipal principal = new UserPrincipal(
                userId,         // userId
                userId,         // username (로그인 ID; 필요하면 토큰에 userName도 넣어서 써도 됨)
                name,           // name
                email,          // email
                slackId,        // slackId
                null,           // password (JWT 기반이라 비밀번호는 안 씀)
                authorities
        );


        Authentication authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 6. 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
}
