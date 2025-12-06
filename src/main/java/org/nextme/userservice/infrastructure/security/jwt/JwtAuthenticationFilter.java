package org.nextme.userservice.infrastructure.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.nextme.userservice.domain.UserId;
import org.nextme.userservice.infrastructure.security.NextmeUserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JWT 인증 필터
 *
 * - 모든 요청마다 1번씩 실행되는 필터(OncePerRequestFilter)
 * - Authorization 헤더의 Bearer 토큰을 꺼내서 검증하고
 * - 유효하면 SecurityContextHolder에 Authentication(=NextmeUserPrincipal)을 세팅
 *
 * 흐름:
 *   1) Authorization: Bearer <accessToken> 헤더 확인
 *   2) 없으면 → 그냥 다음 필터로 넘기고 종료 (익명 사용자로 취급)
 *   3) 있으면 → JwtTokenProvider로 검증 및 파싱
 *   4) 토큰에서 userId, roles 추출
 *   5) NextmeUserPrincipal 생성
 *   6) UsernamePasswordAuthenticationToken으로 감싸서 SecurityContext에 저장
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 이미 다른 필터(예: OAuth2 로그인)에서 인증 객체를 만들어놨다면 건드리지 않고 패스
        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuth != null && existingAuth.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Authorization 헤더에서 Bearer 토큰 꺼내기
        String authHeader = request.getHeader("Authorization");

        // 헤더가 없거나, 형식이 "Bearer "로 시작하지 않으면 → 이 필터는 아무것도 안 하고 넘긴다.
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // "Bearer " 이후가 실제 토큰 문자열

        try {
            // 2. 토큰 타입 검증 (access 토큰만 허용, refresh 토큰은 거부)
            String tokenType = jwtTokenProvider.getTokenType(token);
            if (!"access".equals(tokenType)) {
                // refresh 토큰인데 API 호출에 쓰려고 하면 401 응답
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"invalid_token_type\",\"message\":\"access token required\"}");
                return;
            }

            // 3. 토큰에서 UserId 추출
            UserId userId = jwtTokenProvider.getUserId(token);

            // 4. 토큰에서 roles 클레임 추출 (["USER", "ADVISOR"] 같은 형태)
            //    제네릭 타입 정보가 지워져서 List<?>로 한 번 받은 후 String으로 변환
            var claims = jwtTokenProvider.parseClaims(token);
            Object rawRoles = claims.get("roles");

            List<String> roles;
            if (rawRoles instanceof List<?> list) {
                roles = list.stream()
                        .map(Object::toString) // 안전하게 문자열로 변환
                        .collect(Collectors.toList());
            } else {
                // roles 클레임이 없거나 예상과 다르면 기본 USER 하나만 넣는다
                roles = Collections.singletonList("USER");
            }

            // 5. 우리 서비스 기준 Principal(NextmeUserPrincipal) 생성
            //    - JWT 안에는 email, nickname을 아직 안 넣었으므로 null/placeholder 사용
            NextmeUserPrincipal principal = new NextmeUserPrincipal(
                    userId,
                    null,               // email: 나중에 토큰에 넣고 싶으면 넣어서 채우면 됨
                    null,               // nickname
                    roles,
                    Map.of()            // attributes: JWT 기반 요청에서는 특별히 쓸 원본 데이터 없음
            );

            // 6. 스프링 시큐리티용 Authentication 객체로 감싼 후 SecurityContext에 저장
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,                           // 자격 증명(비밀번호)은 JWT 인증에서는 필요 없음
                    principal.getAuthorities()      // ROLE_USER, ROLE_ADVISOR ... 권한 목록
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            // JWT 파싱/검증 실패 시 → 401 반환 또는 그냥 인증 없이 통과시키는 전략 중 선택 가능
            // 여기서는 401로 바로 응답하도록 처리 (테스트하기 편함)

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"invalid_token\",\"message\":\"JWT validation failed\"}");
            return;
        }

        // 7. 나머지 필터/컨트롤러로 진행
        filterChain.doFilter(request, response);
    }
}