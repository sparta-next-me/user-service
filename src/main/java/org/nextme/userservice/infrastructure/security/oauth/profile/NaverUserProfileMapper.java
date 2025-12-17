package org.nextme.userservice.infrastructure.security.oauth.profile;

import org.nextme.userservice.domain.SocialProvider;
import org.nextme.userservice.infrastructure.security.oauth.SocialUserProfile;
import org.nextme.userservice.infrastructure.security.oauth.SocialUserProfileMapper;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NaverUserProfileMapper implements SocialUserProfileMapper {

    @Override
    public boolean supports(String registrationId) {
        return "naver".equalsIgnoreCase(registrationId);
    }

    @Override
    public SocialUserProfile map(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        Map<String, Object> response =
                (Map<String, Object>) attributes.get("response");

        String providerUserId = (String) response.get("id");
        String email = (String) response.get("email");
        String nickname =
                response.get("nickname") != null
                        ? (String) response.get("nickname")
                        : "네이버사용자";

        return new SocialUserProfile(
                SocialProvider.NAVER,
                providerUserId,
                email,
                nickname,
                attributes
        );
    }
}