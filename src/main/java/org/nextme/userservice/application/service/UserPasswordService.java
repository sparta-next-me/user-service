package org.nextme.userservice.application.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.nextme.infrastructure.exception.ApplicationException;
import org.nextme.userservice.application.error.ErrorCode;
import org.nextme.userservice.domain.User;
import org.nextme.userservice.domain.UserId;
import org.nextme.userservice.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * [소셜 + 비번 미설정] 유저의 "비밀번호 최초 설정"
     * - currentPassword 필요 없음
     * - 이미 설정된 유저가 호출하면 예외
     */
    @Transactional
    public void setInitialPassword(UserId userId, String rawPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> toAppException(ErrorCode.USER_NOT_FOUND));

        if (user.isPasswordInitialized()) {
            throw toAppException(ErrorCode.PASSWORD_ALREADY_INITIALIZED);
        }

        String encoded = passwordEncoder.encode(rawPassword);
        user.initPassword(encoded); // JPA 변경 감지로 update
    }

    /**
     * [비밀번호 설정된] 유저의 "비밀번호 변경"
     * - currentPassword 검증 필수
     */
    @Transactional
    public void changePassword(UserId userId, String currentRawPassword, String newRawPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> toAppException(ErrorCode.USER_NOT_FOUND));

        if (!user.isPasswordInitialized()) {
            throw toAppException(ErrorCode.PASSWORD_NOT_INITIALIZED);
        }

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(currentRawPassword, user.getPassword())) {
            throw toAppException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        String encoded = passwordEncoder.encode(newRawPassword);
        user.changePassword(encoded);
    }

    /**
     * user-service 전용 ErrorCode → 공통 ApplicationException 변환
     */
    private ApplicationException toAppException(ErrorCode errorCode) {
        return new ApplicationException(
                errorCode.getHttpStatus(),   // HttpStatus 는 HttpStatusCode 구현체라 그대로 전달 가능
                errorCode.getCode(),
                errorCode.getDefaultMessage()
        );
    }
}
