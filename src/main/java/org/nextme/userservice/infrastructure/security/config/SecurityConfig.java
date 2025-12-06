package org.nextme.userservice.infrastructure.security.config;

import org.nextme.userservice.domain.service.NextmeOAuth2UserService;
import org.nextme.userservice.infrastructure.security.jwt.JwtAuthenticationFilter;
import org.nextme.userservice.infrastructure.security.jwt.JwtTokenProvider;
import org.nextme.userservice.infrastructure.security.oauth.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            NextmeOAuth2UserService nextmeOAuth2UserService,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
            JwtTokenProvider jwtTokenProvider   // JwtTokenProvider도 주입 받기
    ) throws Exception {

        // 매 요청마다 돌 JwtAuthenticationFilter 생성 (JwtTokenProvider 사용)
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtTokenProvider);

        http
                .csrf(csrf -> csrf
                        // h2-console 은 CSRF 검사에서 제외
                        .ignoringRequestMatchers("/h2-console/**")
                        .disable()
                )
                .headers(headers -> headers
                        // H2 콘솔이 frame 을 쓰는데, sameOrigin 으로 허용해야 화면이 나옴
                        .frameOptions(frame -> frame.sameOrigin())
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                // JWT 쓰는 구조니 세션은 STATELESS 로 변경
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // H2 콘솔은 개발용이니까 전체 허용
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/", "/health", "/public/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**").permitAll() // OAuth2 로그인 관련 URL은 열어둠
                        .anyRequest().authenticated()
                )
                // OAuth2 로그인 (카카오/구글/네이버 공통)
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(nextmeOAuth2UserService)
                        )
                        // .defaultSuccessUrl("/me", true)
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                // UsernamePasswordAuthenticationFilter 전에 JWT 필터를 끼워 넣기
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
