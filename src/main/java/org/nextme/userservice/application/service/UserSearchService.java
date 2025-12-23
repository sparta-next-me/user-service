package org.nextme.userservice.application.service;

import lombok.RequiredArgsConstructor;
import org.nextme.infrastructure.exception.ApplicationException;
import org.nextme.infrastructure.exception.ErrorCode;
import org.nextme.infrastructure.success.CustomResponse;
import org.nextme.userservice.application.dto.UserFeignResponse;
import org.nextme.userservice.application.dto.UserResponse;
import org.nextme.userservice.domain.User;
import org.nextme.userservice.domain.UserId;
import org.nextme.userservice.domain.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

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

    @Transactional(readOnly = true)
    public UserFeignResponse getFeignProfile(UserId userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));

        return UserFeignResponse.from(user);
    }

    /** 전체 유저 조회 (페이징 적용) */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable) // Page<User> 반환
                .map(UserResponse::from);      // 엔티티를 DTO로 변환
    }
}