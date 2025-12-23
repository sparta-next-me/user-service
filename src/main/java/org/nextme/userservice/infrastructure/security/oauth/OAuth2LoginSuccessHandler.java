package org.nextme.userservice.infrastructure.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.nextme.common.jwt.JwtTokenPair;
import org.nextme.common.jwt.JwtTokenProvider;
import org.nextme.userservice.infrastructure.security.NextmeUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider; // msa-common 의 JwtTokenProvider
    private static final String FRONTEND_BASE_URI = "http://34.50.7.8:3001";

    private static final String FRONTEND_REDIRECT_PATH = "/oauth/redirect"; // 프론트엔드에서 토큰을 처리할 경로

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        NextmeUserPrincipal principal = (NextmeUserPrincipal) authentication.getPrincipal();

        // UserId → 문자열(UUID)로 변환
        String userId = principal.getUserId().getId().toString();

        // 추가로 JWT에 넣을 값들
        String name = principal.getName();        // NextmeUserPrincipal.name (우리 서비스 이름)
        String email = principal.getEmail();      // 이메일
        String slackId = principal.getSlackId();  // 슬랙 ID (없으면 null)

        // "ROLE_USER" → "USER"
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(auth -> auth.startsWith("ROLE_")
                        ? auth.substring("ROLE_".length())
                        : auth)
                .toList();

        // JwtTokenProvider에 새로 추가한 메서드 사용
        // public JwtTokenPair generateTokenPair(String userId, String name, String email, String slackId, List<String> roles)
        JwtTokenPair tokenPair = jwtTokenProvider.generateTokenPair(
                userId,
                name,
                email,
                slackId,
                roles
        );

        // 일단 JSON 응답 (나중에 redirect로 바꿔도 됨)
        response.setContentType("application/json;charset=UTF-8");

        String safeSlackId = (slackId != null) ? slackId : "";

        String finalRedirectUrl = UriComponentsBuilder.fromUriString(FRONTEND_BASE_URI + FRONTEND_REDIRECT_PATH)

                .queryParam("accessToken", tokenPair.accessToken())

                .queryParam("refreshToken", tokenPair.refreshToken())

                .queryParam("userId", userId)

                .queryParam("email", email)

                .queryParam("name", name)

                .queryParam("slackId", safeSlackId)

                .queryParam("roles", roles)
                .encode()

                .build()

                .toUriString();

//        String body = """
//                {
//                  "userId": "%s",
//                  "name": "%s",
//                  "email": "%s",
//                  "slackId": "%s",
//                  "roles": %s,
//                  "accessToken": "%s",
//                  "refreshToken": "%s"
//                }
//                """.formatted(
//                userId,
//                name,
//                email,
//                safeSlackId,
//                roles.toString(),
//                tokenPair.accessToken(),
//                tokenPair.refreshToken()
//        );

        //response.getWriter().write(body);
        response.sendRedirect(finalRedirectUrl);
    }
}
