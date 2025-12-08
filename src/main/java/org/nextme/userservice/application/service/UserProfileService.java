package org.nextme.userservice.application.service;

import lombok.RequiredArgsConstructor;
import org.nextme.infrastructure.exception.ApplicationException;
import org.nextme.userservice.application.dto.UpdateMyInfoRequest;
import org.nextme.userservice.application.dto.UserProfileRequest;
import org.nextme.userservice.application.dto.UserProfileResponse;
import org.nextme.userservice.application.error.ErrorCode;
import org.nextme.userservice.domain.User;
import org.nextme.userservice.domain.UserId;
import org.nextme.userservice.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "내 프로필" 생성/조회/수정/비활성화 유스케이스 서비스
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;

    private User getUserOrThrow(UserId userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    ErrorCode e = ErrorCode.USER_NOT_FOUND;
                    return new ApplicationException(
                            e.getHttpStatus(),
                            e.getCode(),
                            e.getDefaultMessage()
                    );
                });
    }

    /**
     * 내 프로필 조회
     * - 프로필이 없으면 PROFILE_NOT_FOUND 에러
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(UserId userId) {
        User user = getUserOrThrow(userId);

        if (user.getProfile() == null) {
            ErrorCode e = ErrorCode.PROFILE_NOT_FOUND;
            throw new ApplicationException(
                    e.getHttpStatus(),
                    e.getCode(),
                    e.getDefaultMessage()
            );
        }

        return UserProfileResponse.from(user.getProfile());
    }

    /**
     * 프로필 최초 생성
     * - 이미 있으면 PROFILE_ALREADY_EXISTS 에러 (도메인에서 처리)
     */
    @Transactional
    public void createMyProfile(UserId userId, UserProfileRequest request) {
        User user = getUserOrThrow(userId);

        // 도메인 메서드 사용 (안에서 PROFILE_ALREADY_EXISTS 던짐)
        user.createProfile(
                request.mainCategory(),
                request.intro(),
                request.careerYears(),
                request.active() != null && request.active()
        );
        // JPA 변경 감지로 자동 update
    }

    /**
     * 프로필 수정
     * - 없으면 PROFILE_NOT_FOUND 에러 (도메인에서 처리)
     */
    @Transactional
    public void updateMyProfile(UserId userId, UserProfileRequest request) {
        User user = getUserOrThrow(userId);

        user.updateProfile(
                request.mainCategory(),
                request.intro(),
                request.careerYears(),
                request.active() != null && request.active()
        );
    }

    /**
     * 프로필 비활성화 (삭제 대신 비노출 처리)
     * - 없으면 PROFILE_NOT_FOUND 에러
     */
    @Transactional
    public void deactivateMyProfile(UserId userId) {
        User user = getUserOrThrow(userId);
        user.deactivateProfile();
    }

    /**
     * 로그인한 유저의 이름 / 슬랙 ID 수정
     */
    @Transactional
    public void updateMyInfo(UserId userId, UpdateMyInfoRequest request) {
        User user = getUserOrThrow(userId);
        user.updateBasicInfo(
                request.name(),
                request.slackId()
        );
    }
}
