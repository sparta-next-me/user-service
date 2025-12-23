package org.nextme.userservice.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextme.common.security.UserPrincipal;
import org.nextme.infrastructure.exception.ApplicationException;
import org.nextme.infrastructure.exception.ErrorCode;
import org.nextme.infrastructure.success.CustomResponse;
import org.nextme.userservice.application.dto.*;
import org.nextme.userservice.application.service.*;
import org.nextme.userservice.domain.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * User 관련 REST API 컨트롤러
 *
 * - /v1/user/me/password/initial : 소셜 + 비번 미설정 유저의 비밀번호 최초 설정
 * - /v1/user/me/password        : 비밀번호 변경
 * - /v1/user/me                 : 내 기본 정보 조회
 * - /v1/user/me/profile         : 내 프로필 CRUD
 *
 * 인증 흐름
 * 1) 클라이언트 → Gateway 로 Authorization: Bearer {accessToken} 전송
 * 2) Gateway(JwtGatewayFilter) 에서 토큰 검증 후 X-User-Id, X-User-Roles 헤더 추가
 * 3) user-service 의 GatewayUserHeaderAuthenticationFilter 가 헤더를 읽어
 *    SecurityContext 에 UserPrincipal 세팅
 * 4) 컨트롤러에서 @AuthenticationPrincipal UserPrincipal principal 로 바로 사용
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/user")
public class UserController {

    private final UserPasswordService userPasswordService;
    private final UserSearchService userSearchService;
    private final UserProfileService userProfileService;
    private final AdvisorApplicationService advisorApplicationService;
    private final UserPointService userPointService;
    private final AuthTokenService authTokenService;

    /** Gateway 가 넣어준 userId(String) → 도메인 UserId 변환 공통 메서드 */
    private UserId toUserId(UserPrincipal principal) {
        return UserId.of(UUID.fromString(principal.userId()));
    }

    /** 공통 에러 처리 */
    private ApplicationException invalidRefreshTokenException() {
        ErrorCode e = ErrorCode.INVALID_REFRESH_TOKEN;
        return new ApplicationException(
                e.getHttpStatus(),
                e.getCode(),
                e.getDefaultMessage()
        );
    }


    // ==========================
    //  1) 비밀번호 최초 설정
    // ==========================

    /**
     * [소셜 + 비번 미설정] → 비밀번호 최초 설정
     *
     * 조건
     * - ROLE_USER 권한 필요
     * - 현재 로그인한 유저가 소셜 로그인으로만 가입되어 있고,
     *   아직 passwordInitialized == false 인 상태에서만 정상 동작
     *
     * 요청 예시 (POST /v1/user/me/password/initial)
     * {
     *   "newPassword": "Abcd1234!"
     * }
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/me/password/initial")
    public CustomResponse<Void> setInitialPassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody InitialPasswordRequest request
    ) {
        UserId userId = toUserId(principal);

        userPasswordService.setInitialPassword(
                userId,
                request.newPassword()
        );

        return CustomResponse.onSuccess("비밀번호가 초기 설정되었습니다.", null);
    }

    // ==========================
    //  2) 비밀번호 변경
    // ==========================

    /**
     * 비밀번호 변경
     *
     * 조건
     * - ROLE_USER 권한 필요
     * - 비밀번호가 이미 초기 설정된 유저만 가능 (passwordInitialized == true)
     * - currentPassword 가 기존 비밀번호와 일치해야 함
     *
     * 요청 예시 (PATCH /v1/user/me/password)
     * {
     *   "currentPassword": "Old1234!",
     *   "newPassword": "New1234!"
     * }
     */
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/me/password")
    public CustomResponse<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ChangePasswordRequest request
    ) {
        UserId userId = toUserId(principal);

        userPasswordService.changePassword(
                userId,
                request.currentPassword(),
                request.newPassword()
        );

        return CustomResponse.onSuccess("비밀번호가 변경되었습니다.", null);
    }

    // ==========================
    //  3) 내 기본 정보 조회
    // ==========================

    /**
     * 로그인한 유저 본인의 기본 정보 조회
     *
     * 요청: GET /v1/user/me
     * 응답: CustomResponse<UserResponse>
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/me")
    public CustomResponse<UserResponse> getMe(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserId userId = toUserId(principal);

        UserResponse response = userSearchService.getMyProfile(userId);

        return CustomResponse.onSuccess("내 정보 조회에 성공했습니다.", response);
    }

    /**
     * 로그인한 유저 닉네임 조회 (페인 전용)
     *
     * 요청: GET /v1/user/feing/profile
     * 응답: CustomResponse<UserFeignResponse>
     */
    @GetMapping("/feign/profile")
    public CustomResponse<UserFeignResponse> getFeignMe(
            @RequestParam("userId") String userIdStr // 쿼리 파라미터로 ID를 받음
    ) {
        UserId userId = UserId.of(UUID.fromString(userIdStr));
        UserFeignResponse response = userSearchService.getFeignProfile(userId);
        return CustomResponse.onSuccess("내 정보 조회에 성공했습니다.", response);
    }

    /**
     * 로그인한 유저 본인의 기본 정보 수정
     *
     * 요청: Fatch /v1/user/me
     */
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/me")
    public CustomResponse<Void> updateMyInfo(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Validated UpdateMyInfoRequest request
    ){
        UserId userId = toUserId(principal);
        userProfileService.updateMyInfo(userId, request);
        return CustomResponse.onSuccess("내 정보가 수정되었습니다.", null);
    }


    // ==========================
    //  4) 내 프로필 CRUD
    // ==========================

    /**
     * 내 프로필 조회
     * - GET /v1/user/me/profile
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/me/profile")
    public CustomResponse<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserId userId = toUserId(principal);
        UserProfileResponse response = userProfileService.getMyProfile(userId);
        return CustomResponse.onSuccess("프로필 조회에 성공했습니다.", response);
    }

    /**
     * 프로필 최초 생성
     * - POST /v1/user/me/profile
     * - 이미 프로필이 있으면 PROFILE_ALREADY_EXISTS 에러
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/me/profile")
    public CustomResponse<Void> createMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Validated UserProfileRequest request
    ) {
        UserId userId = toUserId(principal);
        userProfileService.createMyProfile(userId, request);
        return CustomResponse.onSuccess("프로필이 생성되었습니다.", null);
    }

    /**
     * 프로필 수정
     * - PATCH /v1/user/me/profile
     * - 존재하지 않으면 PROFILE_NOT_FOUND 에러
     */
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/me/profile")
    public CustomResponse<Void> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Validated UserProfileRequest request
    ) {
        UserId userId = toUserId(principal);
        userProfileService.updateMyProfile(userId, request);
        return CustomResponse.onSuccess("프로필이 수정되었습니다.", null);
    }

    /**
     * 프로필 비활성화 (삭제 대신 노출 OFF)
     * - DELETE /v1/user/me/profile
     */
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/me/profile")
    public CustomResponse<Void> deactivateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserId userId = toUserId(principal);
        userProfileService.deactivateMyProfile(userId);
        return CustomResponse.onSuccess("프로필이 비활성화되었습니다.", null);
    }

    // ==========================
    //  어드바이저 신청
    // ==========================

    /**
     * 어드바이저 신청
     *
     * - POST /v1/user/me/advisor/apply
     *
     * 상태별 동작:
     * - NOT_REQUESTED → PENDING 으로 변경, "어드바이저 신청이 접수되었습니다."
     * - PENDING       → "이미 신청이 완료되었습니다."
     * - APPROVED      → "신청이 승인되었습니다."
     * - REJECTED      → "신청이 거절되었습니다."
     *
     * USER_NOT_FOUND 인 경우:
     * - AdvisorApplicationService 내부에서 ApplicationException(ErrorCode.USER_NOT_FOUND) 발생
     * - 공통 예외 핸들러에서 처리
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/me/advisor/apply")
    public CustomResponse<Void> applyAdvisor(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserId userId = UserId.of(UUID.fromString(principal.userId()));

        String message = advisorApplicationService.applyForAdvisor(userId);

        return CustomResponse.onSuccess(message, null);
    }

    /**
     * 어드바이저 신청(PENDING) 상태인 유저 목록 조회
     *
     * - GET /v1/admin/advisors/pending
     * - ROLE: MASTER, MANAGER
     */
    @PreAuthorize("hasAnyRole('MASTER', 'MANAGER')")
    @GetMapping("/advisor/pending")
    public CustomResponse<List<AdvisorCandidateResponse>> getPendingAdvisors() {
        List<AdvisorCandidateResponse> response =
                advisorApplicationService.getPendingAdvisors();

        return CustomResponse.onSuccess("어드바이저 신청 대기 목록 조회에 성공했습니다.", response);
    }

    /**
     * 특정 유저를 어드바이저로 승인(승급)
     *
     * - POST /v1/admin/advisors/{userId}/approve
     * - path 의 userId 는 UUID 문자열
     * - ROLE: MASTER, MANAGER
     */
    @PreAuthorize("hasAnyRole('MASTER', 'MANAGER')")
    @PostMapping("/advisor/{userId}/approve")
    public CustomResponse<Void> approveAdvisor(
            @PathVariable("userId") String userIdString
    ) {
        UserId userId = UserId.of(UUID.fromString(userIdString));

        String message = advisorApplicationService.approveAdvisor(userId);

        return CustomResponse.onSuccess(message, null);
    }

    // ==========================
    //  토큰 관련 ( 토큰 블랙리스트 처리(로그 아웃), 엑세스 토큰 재발급)
    // ==========================

    /**
     * 리프레시 토큰을 사용한 토큰 재발급
     *
     * - 요청 헤더:
     *   Authorization: Bearer {refreshToken}
     *
     * - 조건:
     *   - JWT 유효해야 함
     *   - type == "refresh" 이어야 함
     *   - 블랙리스트에 있으면 안 됨
     *
     * - 응답:
     *   - 새 accessToken + 새 refreshToken
     *   - userId, name, email, slackId, roles 포함
     */
    @PostMapping("/auth/refresh")
    public CustomResponse<TokenResponse> refreshToken(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        TokenResponse body = authTokenService.refreshToken(authorization);
        return CustomResponse.onSuccess("토큰이 재발급되었습니다.", body);
    }

    /**
     * 로그아웃
     *
     * - 요청 헤더
     *   Authorization: Bearer {accessToken}
     *   X-Refresh-Token: {refreshToken} (선택)
     */
    @PostMapping("/auth/logout")
    public CustomResponse<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshTokenHeader
    ) {
        authTokenService.logout(authorization, refreshTokenHeader);
        return CustomResponse.onSuccess("로그아웃 되었습니다.", null);
    }

    /** 전체 유저 조회 (관리자 전용, 페이징 적용) */
    @GetMapping("/admin/users")
    public CustomResponse<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return CustomResponse.onSuccess("전체 유저 조회 성공", userSearchService.getAllUsers(pageable));
    }

    /** 포인트 적립 */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/me/points")
    public CustomResponse<Void> addPoint(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody AddPointRequest request
    ) {
        UserId userId = toUserId(principal);
        userPointService.addPoint(userId, request.amount());
        return CustomResponse.onSuccess("포인트가 적립되었습니다.", null);
    }

}
