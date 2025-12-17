package org.nextme.userservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * UserProfile
 * - 유저의 프로필 정보(전문 카테고리, 소개, 경력 연차, 프로필 공개 여부)를 담는 값 객체
 * - User 엔티티에 @Embedded 로 포함된다.
 * - 생성/수정 로직의 "트리거"는 항상 User 엔티티에서만 수행하도록 한다.
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class UserProfile {

    /**
     * 주요 카테고리 (예: "주식", "부동산", "연금" 등)
     */
    @Column(name = "profile_main_category", length = 50)
    private String mainCategory;

    /**
     * 자기 소개 (간단한 소개/한 줄 소개 등)
     */
    @Column(name = "profile_intro", length = 500)
    private String intro;

    /**
     * 경력 연차 (예: 3년, 5년 등)
     */
    @Column(name = "profile_career_years")
    private Integer careerYears;

    /**
     * 프로필 공개 여부
     * - true: 공개 (상담 리스트 등에서 노출 가능)
     * - false: 비공개
     */
    @Column(name = "profile_active")
    private Boolean active;

    // ===== 내부 생성자 (외부에서 직접 new 금지) =====

    private UserProfile(
            String mainCategory,
            String intro,
            Integer careerYears,
            boolean active
    ) {
        this.mainCategory = mainCategory;
        this.intro = intro;
        this.careerYears = careerYears;
        this.active = active;
    }

    /**
     * 새 프로필 생성용 팩토리 메서드
     * - 실제로는 User 엔티티의 createProfile/updateProfile 에서만 호출되게 사용하는 것을 권장
     */
    public static UserProfile of(
            String mainCategory,
            String intro,
            Integer careerYears,
            boolean active
    ) {
        return new UserProfile(mainCategory, intro, careerYears, active);
    }
}
