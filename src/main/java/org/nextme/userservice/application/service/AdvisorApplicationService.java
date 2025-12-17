package org.nextme.userservice.application.service;

import lombok.RequiredArgsConstructor;
import org.nextme.infrastructure.exception.ApplicationException;
import org.nextme.infrastructure.exception.ErrorCode;
import org.nextme.userservice.application.dto.AdvisorCandidateResponse;
import org.nextme.userservice.domain.AdvisorStatus;
import org.nextme.userservice.domain.User;
import org.nextme.userservice.domain.UserId;
import org.nextme.userservice.domain.UserRole;
import org.nextme.userservice.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 어드바이저 신청/승급 관련 유스케이스 서비스
 *
 * - 일반 유저: 어드바이저 신청 (NOT_REQUESTED → PENDING)
 * - 관리자/매니저: 신청 목록 조회, 승인(승급)
 */
@Service
@RequiredArgsConstructor
public class AdvisorApplicationService {

    private final UserRepository userRepository;

    // ==========================
    //  1) 일반 유저: 어드바이저 신청
    // ==========================

    /**
     * 어드바이저 신청 처리
     *
     * - NOT_REQUESTED → PENDING 으로 변경 후 "어드바이저 신청이 접수되었습니다."
     * - PENDING       → 상태 그대로, "이미 신청이 완료되었습니다."
     * - APPROVED      → 상태 그대로, "신청이 승인되었습니다."
     * - REJECTED      → 상태 그대로, "신청이 거절되었습니다."
     */
    @Transactional
    public String applyForAdvisor(UserId userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));

        AdvisorStatus status = user.getAdvisorStatus();

        return switch (status) {
            case NOT_REQUESTED -> {
                user.changeAdvisorStatus(AdvisorStatus.PENDING);
                yield "어드바이저 신청이 접수되었습니다.";
            }
            case PENDING -> "이미 신청이 완료되었습니다.";
            case APPROVED -> "신청이 승인되었습니다.";
            case REJECTED -> "신청이 거절되었습니다.";
        };
    }

    // ==========================
    //  2) 관리자/매니저: PENDING 목록 조회
    // ==========================

    /**
     * 어드바이저 신청(PENDING) 상태인 유저 목록 조회
     *
     * - MASTER / MANAGER 용
     */
    @Transactional(readOnly = true)
    public List<AdvisorCandidateResponse> getPendingAdvisors() {
        List<User> pendingUsers =
                userRepository.findByAdvisorStatus(AdvisorStatus.PENDING);

        return pendingUsers.stream()
                .map(AdvisorCandidateResponse::from)
                .toList();
    }

    // ==========================
    //  3) 관리자/매니저: 특정 유저 어드바이저 승인(승급)
    // ==========================

    /**
     * 특정 유저를 어드바이저로 승인(승급)
     *
     * - advisorStatus: PENDING → APPROVED
     * - role: (기존 값) → ADVISOR 로 변경
     *
     * 상태별 동작:
     * - PENDING 이 아니어도 USER_NOT_FOUND 외에 에러는 던지지 않고,
     *   이미 APPROVED 인 경우엔 메시지만 다르게 내려도 됨.
     *
     * @return 처리 결과 메시지
     */
    @Transactional
    public String approveAdvisor(UserId targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));

        AdvisorStatus current = user.getAdvisorStatus();

        if (current == AdvisorStatus.APPROVED) {
            // 이미 승인된 경우
            return "이미 어드바이저로 승인된 유저입니다.";
        }

        // 승인 처리
        user.changeAdvisorStatus(AdvisorStatus.APPROVED);
        user.changeRole(UserRole.ADVISOR);

        return "해당 유저가 어드바이저로 승급되었습니다.";
    }
}
