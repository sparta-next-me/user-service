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

        // 1. 카카오 ID 추출
        Long kakaoId = ((Number) attributes.get("id")).longValue();

        // 2. kakao_account 추출 (오타 수정: kakaoAccount -> kakao_account)
        Map<String, Object> kakaoAccount =
                (Map<String, Object>) attributes.get("kakao_account");

        String email = null;
        String nickname = "카카오사용자";

        if (kakaoAccount != null) {
            // 이메일 추출
            email = (String) kakaoAccount.get("email");

            // 3. profile 추출
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            if (profile != null) {
                nickname = (String) profile.get("nickname");
            }
        }

        return new SocialUserProfile(
                SocialProvider.KAKAO,
                String.valueOf(kakaoId),
                email,
                nickname,
                attributes
        );
    }
}