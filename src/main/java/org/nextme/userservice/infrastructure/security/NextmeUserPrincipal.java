package org.nextme.userservice.infrastructure.security;

import org.nextme.userservice.domain.UserId;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class NextmeUserPrincipal implements OAuth2User {

    private final UserId userId;                      // 우리 서비스 UserId(UUID)
    private final String email;                       // 유저 이메일 (nullable 가능)
    private final String nickname;                    // 닉네임
    private final Collection<? extends GrantedAuthority> authorities; // 권한
    private final Map<String, Object> attributes;     // 카카오 원본 attributes (필요하면 사용)

    public NextmeUserPrincipal(
            UserId userId,
            String email,
            String nickname,
            List<String> roles,
            Map<String, Object> attributes
    ) {
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
        this.attributes = attributes;
        this.authorities = roles.stream()
                // "USER" -> "ROLE_USER" 이런 식으로 prefix 붙여줌
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    public UserId getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    // === OAuth2User 인터페이스 구현 ===

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // Security 에서 "이 유저 이름"으로 쓸 값
    // 여기서는 UserId 문자열로 사용
    @Override
    public String getName() {
        return userId.toString();
    }
}
