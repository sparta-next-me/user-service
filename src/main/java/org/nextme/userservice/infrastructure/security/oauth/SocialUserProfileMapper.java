package org.nextme.userservice.infrastructure.security.oauth;

import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * 소셜 Provider 별로 응답 JSON 구조가 다르므로
 * 각 Provider 마다 "attributes → SocialUserProfile" 변환을 담당하는 전략 인터페이스.
 *
 * 구현체 예:
 *  - KakaoUserProfileMapper
 *  - GoogleUserProfileMapper
 *  - NaverUserProfileMapper
 */
public interface SocialUserProfileMapper {

    /**
     * 이 Mapper 가 처리할 수 있는 registrationId인지 여부
     * - application.yml 의
     *   spring.security.oauth2.client.registration.{registrationId}
     *   의 {registrationId} 값과 매칭
     *
     *   예) kakao, google, naver
     */
    boolean supports(String registrationId);

    /**
     * OAuth2UserRequest, OAuth2User → SocialUserProfile 변환 메서드
     *
     * @param userRequest  OAuth2UserRequest (등록 정보, 클라이언트 정보 등)
     * @param oAuth2User   Spring 이 만들어 준 OAuth2User (attributes 포함)
     * @return             우리 서비스 공통 SocialUserProfile
     */
    SocialUserProfile map(OAuth2UserRequest userRequest, OAuth2User oAuth2User);
}
