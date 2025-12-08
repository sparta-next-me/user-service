package org.nextme.userservice.application.service;

import lombok.RequiredArgsConstructor;
import org.nextme.infrastructure.exception.ApplicationException;
import org.nextme.infrastructure.exception.ErrorCode;
import org.nextme.userservice.application.dto.UserResponse;
import org.nextme.userservice.domain.User;
import org.nextme.userservice.domain.UserId;
import org.nextme.userservice.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유저 조회 관련 읽기 전용 서비스
 */
@Service
@RequiredArgsConstructor
public class UserSearchService {

    private final UserRepository userRepository;

    /**
     * 로그인한 유저의 "내 프로필" 조회
     *
     * @param userId 현재 로그인한 유저의 식별자
     * @return UserMeResponse (비밀번호/소셜계정 제외)
     */
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(UserId userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));

        return UserResponse.from(user);
    }
}