package org.nextme.userservice.infrastructure.security.oauth.profile;
import org.nextme.userservice.domain.SocialProvider;
import org.nextme.userservice.infrastructure.security.oauth.SocialUserProfile;
import org.nextme.userservice.infrastructure.security.oauth.SocialUserProfileMapper;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class KakaoUserProfileMapper implements SocialUserProfileMapper {

    @Override
    public boolean supports(String registrationId) {
        return "kakao".equalsIgnoreCase(registrationId);
    }

    public SocialUserProfile map(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        Long kakaoId = ((Number) attributes.get("id")).longValue();
        Map<String, Object> kakaoAccount =
                (Map<String, Object>) attributes.get("kakaoAccount");
        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

        Map<String, Object> profile =
                kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;
        String nickname = profile != null ? (String) profile.get("nickname") : "카카오사용자";

        return new SocialUserProfile(
                SocialProvider.KAKAO,
                String.valueOf(kakaoId),
                email,
                nickname,
                attributes
        );
    }
}
