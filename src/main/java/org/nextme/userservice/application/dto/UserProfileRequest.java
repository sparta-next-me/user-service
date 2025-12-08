package org.nextme.userservice.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 유저 프로필 생성/수정 요청 DTO
 */
public record UserProfileRequest(

        @NotBlank(message = "메인 카테고리는 필수입니다.")
        String mainCategory,

        @NotBlank(message = "소개는 필수입니다.")
        String intro,

        @PositiveOrZero(message = "경력 연차는 0 이상이어야 합니다.")
        Integer careerYears,

        // 활성 여부 (true: 노출, false: 비노출)
        Boolean active
) {
}
