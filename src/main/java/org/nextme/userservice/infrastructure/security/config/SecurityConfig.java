package org.nextme.userservice.infrastructure.security.config;

import org.nextme.common.jwt.JwtTokenProvider;
import org.nextme.common.security.GatewayUserHeaderAuthenticationFilter;
import org.nextme.userservice.domain.service.NextmeOAuth2UserService;
import org.nextme.userservice.infrastructure.security.DirectJwtAuthenticationFilter;
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
     * Authorization: Bearer 토큰 기반 인증 필터
     */
    @Bean
    public DirectJwtAuthenticationFilter directJwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        return new DirectJwtAuthenticationFilter(jwtTokenProvider);
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
