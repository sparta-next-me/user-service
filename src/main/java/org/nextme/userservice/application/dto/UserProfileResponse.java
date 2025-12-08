package org.nextme.userservice.application.dto;

import org.nextme.userservice.domain.UserProfile;

/**
 * 유저 프로필 조회 응답 DTO
 */
public record UserProfileResponse(
        String mainCategory,
        String intro,
        Integer careerYears,
        boolean active   // 응답은 boolean 그대로 사용 (null이면 false로 취급)
) {

    public static UserProfileResponse from(UserProfile profile) {
        // profile.getActive() 는 Boolean (null 가능)
        boolean isActive = Boolean.TRUE.equals(profile.getActive());

        return new UserProfileResponse(
                profile.getMainCategory(),
                profile.getIntro(),
                profile.getCareerYears(),
                isActive
        );
    }
}
