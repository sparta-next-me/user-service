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
    private final String name;                        // 이름(닉네임 대신)
    private final String slackId;                     // 슬랙 ID (nullable)
    private final Collection<? extends GrantedAuthority> authorities; // 권한
    private final Map<String, Object> attributes;     // 소셜 원본 attributes (필요하면 사용)

    public NextmeUserPrincipal(
            UserId userId,
            String email,
            String name,
            String slackId,
            List<String> roles,
            Map<String, Object> attributes
    ) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.slackId = slackId;
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

    /**
     * 소셜/유저 이름
     */
    public String getName() {
        return name;
    }

    /**
     * 슬랙 ID (없으면 null)
     */
    public String getSlackId() {
        return slackId;
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
}
