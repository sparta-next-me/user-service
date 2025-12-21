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
    ),

    /**
     * 회원가입 시 중복된 아이디(userName)일 경우
     */
    DUPLICATE_USERNAME(
            HttpStatus.CONFLICT,
            "DUPLICATE_USERNAME",
                    "이미 사용 중인 아이디입니다."
    ),

    /**
     * 로그인 시 비밀번호가 틀린 경우
     */
    INVALID_PASSWORD(
            HttpStatus.UNAUTHORIZED,
            "INVALID_PASSWORD",
            "비밀번호가 일치하지 않습니다."
    ),

    /**
     * 계정 상태가 ACTIVE가 아닐 때 (정지된 계정 등)
     */
    USER_STATUS_NOT_ACTIVE(
            HttpStatus.FORBIDDEN,
            "USER_STATUS_NOT_ACTIVE",
                    "비활성화되었거나 정지된 계정입니다."
    ),

    /**
     * 토큰 관련 (필요시 추가)
     */
    INVALID_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "INVALID_TOKEN",
                    "유효하지 않은 토큰입니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String defaultMessage;
}
