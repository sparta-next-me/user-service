package org.nextme.userservice.infrastructure.security.config;

import org.nextme.userservice.domain.service.KakaoOAuth2UserService;
import org.nextme.userservice.infrastructure.security.oauth.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            KakaoOAuth2UserService kakaoOAuth2UserService,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler

    ) throws Exception {

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
                .authorizeHttpRequests(auth -> auth
                        // H2 콘솔은 개발용이니까 전체 허용
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/", "/health", "/public/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(kakaoOAuth2UserService)
                        )
                        //.defaultSuccessUrl("/me", true)
                        .successHandler(oAuth2LoginSuccessHandler)
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
