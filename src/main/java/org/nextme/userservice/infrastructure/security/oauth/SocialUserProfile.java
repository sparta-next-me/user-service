package org.nextme.userservice.infrastructure.security.oauth;

import org.nextme.userservice.domain.SocialProvider;

import java.util.Map;

/**
 * 각 소셜 제공자(Kakao, Google, Naver 등)의
 * 서로 다른 응답 JSON 구조를
 * 우리 서비스에서 공통으로 다루기 위한 DTO.
 *
 * - provider        : 어떤 소셜 제공자인지 (KAKAO / GOOGLE / NAVER)
 * - providerUserId  : 소셜에서 제공하는 유저 고유 ID (ex. 카카오 id, 구글 sub, 네이버 id)
 * - email           : 이메일 (없을 수도 있음)
 * - nickname        : 표시용 닉네임
 * - attributes      : 원본 attributes 전체 (디버깅/추가정보 조회용)
 */
public record SocialUserProfile(
        SocialProvider provider,
        String providerUserId,
        String email,
        String nickname,
        Map<String, Object> attributes
) {
}
