package org.nextme.userservice.infrastructure.security.oauth.profile;
import org.nextme.userservice.domain.SocialProvider;
import org.nextme.userservice.infrastructure.security.oauth.SocialUserProfile;
import org.nextme.userservice.infrastructure.security.oauth.SocialUserProfileMapper;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GoogleUserProfileMapper implements SocialUserProfileMapper {

    @Override
    public boolean supports(String registrationId) {
        return "google".equalsIgnoreCase(registrationId);
    }

    @Override
    public SocialUserProfile map(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String providerUserId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String nickname =
                attributes.get("name") != null
                        ? (String) attributes.get("name")
                        : "구글사용자";

        return new SocialUserProfile(
                SocialProvider.GOOGLE,
                providerUserId,
                email,
                nickname,
                attributes
        );
    }
}