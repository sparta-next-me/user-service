package org.nextme.userservice.application.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * user-service 전용 에러 코드 정의
 *
 * - httpStatus: HTTP 응답 status
 * - code      : 비즈니스 에러 코드 (로그/응답 공통 사용)
 * - defaultMessage: 기본 메시지
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "USER_NOT_FOUND",
            "User not found."
    ),

    PASSWORD_ALREADY_INITIALIZED(
            HttpStatus.BAD_REQUEST,
            "PASSWORD_ALREADY_INITIALIZED",
            "Password already initialized."
    ),

    PASSWORD_NOT_INITIALIZED(
            HttpStatus.BAD_REQUEST,
            "PASSWORD_NOT_INITIALIZED",
            "Password not initialized."
    ),

    INVALID_CURRENT_PASSWORD(
            HttpStatus.BAD_REQUEST,
            "INVALID_CURRENT_PASSWORD",
            "Current password does not match."
    ),

    // ==== 프로필 관련 에러 ====

    PROFILE_ALREADY_EXISTS(
            HttpStatus.BAD_REQUEST,
            "PROFILE_ALREADY_EXISTS",
            "Profile already exists."
    ),

    PROFILE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "PROFILE_NOT_FOUND",
            "Profile not found."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String defaultMessage;
}
