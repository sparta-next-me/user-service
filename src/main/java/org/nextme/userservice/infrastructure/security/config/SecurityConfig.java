package org.nextme.userservice.infrastructure.security.config;

import org.nextme.common.jwt.JwtTokenProvider;
import org.nextme.common.jwt.TokenBlacklistService;
import org.nextme.common.security.DirectJwtAuthenticationFilter;
import org.nextme.common.security.GatewayUserHeaderAuthenticationFilter;
import org.nextme.userservice.domain.service.NextmeOAuth2UserService;
import org.nextme.userservice.infrastructure.security.oauth.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * msa-common 에 있는 GatewayUserHeaderAuthenticationFilter 를
     * user-service 빈으로 등록.
     */
    @Bean
    public GatewayUserHeaderAuthenticationFilter gatewayUserHeaderAuthenticationFilter() {
        return new GatewayUserHeaderAuthenticationFilter();
    }

    /**
     * Authorization: Bearer 토큰 기반 인증 필터.
     *
     *   여기 전달하는 ignorePathPrefixes(화이트리스트)는
     *    "DirectJwtAuthenticationFilter 가 아예 동작하지 않아야 하는 URL" 들이다.
     *
     *  - 공통 규칙:
     *    1) Authorization 헤더에 **refresh 토큰**이 들어오는 엔드포인트
     *       → DirectJwtAuthenticationFilter 는 type != access 인 토큰을 401 로 막기 때문에
     *         필터를 타면 안 된다.
     *
     *    2) 토큰이 **깨졌거나 만료된 경우에도 컨트롤러에서 조용히 처리하고 싶은 엔드포인트**
     *       → 필터에서 401 을 내버리면 컨트롤러 로직까지 도달하지 못하므로
     *         해당 URL 은 필터를 스킵해야 한다.
     *
     *    3) 그 외, "로그인/토큰 발급 등 인증 이전 단계에서 동작해야 하는 순수 Auth 엔드포인트"
     *       → 컨벤션 차원에서 토큰 필터 범위 밖에 두고 싶다면 같이 넣어준다.
     */
    @Bean
    public DirectJwtAuthenticationFilter directJwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            TokenBlacklistService tokenBlacklistService
    ) {
        return new DirectJwtAuthenticationFilter(
                jwtTokenProvider,
                tokenBlacklistService,
                List.of(
                        "/v1/user/auth/login",
                        "/v1/user/auth/refresh",
                        "/v1/user/auth/logout",
                        "/v3/api-docs/**"
                )
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            GatewayUserHeaderAuthenticationFilter gatewayUserHeaderAuthenticationFilter,
            DirectJwtAuthenticationFilter directJwtAuthenticationFilter,
            NextmeOAuth2UserService nextmeOAuth2UserService,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler
    ) throws Exception {

        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**")
                        .disable()
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/", "/health", "/public/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/api-docs.html"
                        ).permitAll()
                        // Auth API
                        .requestMatchers(
                                "/v1/user/auth/login",
                                "/v1/user/auth/refresh",
                                "/v1/user/auth/logout"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // OAuth2 로그인 (카카오/구글/네이버 공통)
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(nextmeOAuth2UserService)  // <- 메서드 파라미터로 받은 걸 사용
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                // 1순위 : Gateway → X-User-* 헤더 기반 인증 필터 공통 필터 사용
                .addFilterBefore(gatewayUserHeaderAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 2순위 : Gateway 헤더가 없고 Authorization 만 있을 때 JWT 직접 인증
                .addFilterBefore(directJwtAuthenticationFilter, GatewayUserHeaderAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
