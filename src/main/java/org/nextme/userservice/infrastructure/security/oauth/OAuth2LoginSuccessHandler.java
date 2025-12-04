package org.nextme.userservice.infrastructure.security.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.nextme.userservice.infrastructure.security.NextmeUserPrincipal;
import org.nextme.userservice.infrastructure.security.jwt.JwtTokenPair;
import org.nextme.userservice.infrastructure.security.jwt.JwtTokenProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        // 1. 우리가 만든 Principal로 캐스팅
        NextmeUserPrincipal principal = (NextmeUserPrincipal) authentication.getPrincipal();

        // 2. userId 꺼내기
        var userId = principal.getUserId();

        // 3. ROLE_ 접두어 제거해서 ["USER", "ADVISOR"] 이런 식으로 리스트 만들기
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)          // "ROLE_USER", "ROLE_ADVISOR"
                .map(auth -> auth.startsWith("ROLE_")
                        ? auth.substring("ROLE_".length())    // "USER", "ADVISOR"
                        : auth)
                .toList();

        // 4. JWT 발급
        JwtTokenPair tokenPair = jwtTokenProvider.generateTokenPair(userId, roles);

        // 5. 일단 테스트용: JSON으로 바로 응답

        ObjectMapper objectMapper = new ObjectMapper();
        String rolesJson = objectMapper.writeValueAsString(roles);

        response.setContentType("application/json;charset=UTF-8");
        String body = """
                {
                  "userId": "%s",
                  "roles": %s,
                  "accessToken": "%s",
                  "refreshToken": "%s"
                }
                """.formatted(
                userId.getId().toString(),
                roles.toString(),                    // ["USER", "ADVISOR"] 이런 문자열
                tokenPair.accessToken(),
                tokenPair.refreshToken()
        );

        response.getWriter().write(body);

    }
}
