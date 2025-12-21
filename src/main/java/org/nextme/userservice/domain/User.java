package org.nextme.userservice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.nextme.common.jpa.BaseEntity;
import org.nextme.infrastructure.exception.ApplicationException;
import org.nextme.userservice.application.error.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.Set;

/**
 * User
 * - 서비스 내 "회원"을 나타내는 Aggregate Root
 * - 소셜 로그인/일반 회원 모두 이 엔티티로 관리
 * - 소셜 계정은 SocialAccount 값 객체 컬렉션(@ElementCollection)으로 관리
 * - 프로필(UserProfile)은 @Embedded 값 객체로 관리
 */
@Getter
@Entity
@Table(name = "p_user")
@ToString(exclude = {"socialAccounts", "profile"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    // UUID 기반 식별자 (EmbeddedId)
    @EmbeddedId
    private UserId id;

    /**
     * 서비스 내부 로그인 ID (닉네임/핸들 용도)
     * - 카카오/구글 ID 그대로 쓰지 않음
     * - 소셜 회원은 최초 로그인 시 자동 생성 (ex. kakao_123abc)
     */
    @Column(name = "user_name", nullable = false, length = 25, unique = true)
    private String userName;

    /**
     * 비밀번호 (BCrypt 등으로 해시된 값)
     * - 소셜 로그인 전용 계정도 DB 제약 때문에 랜덤 패스워드를 넣어둘 수 있음
     * - 실제 로그인은 OAuth2로만 허용하고, 나중에 사용자가 원하면 비밀번호 설정 가능
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /**
     * 회원 역할
     * - 기본: USER
     * - 관리자/어드바이저 등 확장 가능
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    /**
     * 실제 이름 (소셜 프로필 이름 등)
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * 슬랙 ID
     * - 어드바이저로 활동하는 경우에만 필요
     * - 기본은 null
     */
    @Column(name = "slack_id", length = 100)
    private String slackId;

    /**
     * 계정 상태
     * - ACTIVE / INACTIVE / BLOCKED / DELETED
     * - 소셜/일반에 상관 없이 기본값은 ACTIVE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    /**
     * 어드바이저 승급 상태
     * - NOT_REQUESTED: 아직 신청하지 않음(기본)
     * - PENDING: 심사 대기
     * - APPROVED: 승인
     * - REJECTED: 거절
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "advisor_status", nullable = false, length = 20)
    private AdvisorStatus advisorStatus;

    /**
     * 비밀번호가 사용자가 직접 설정한 상태인지 여부
     * - 로컬 회원가입: true
     * - 소셜 최초 가입: false (랜덤 패스워드만 들어가 있고, 사용자는 모름)
     * - 사용자가 "비밀번호 최초 설정" 하면 true로 변경
     */
    @Column(name = "password_initialized", nullable = false)
    private boolean passwordInitialized;

    /**
     * 이메일 주소
     * - 일반 회원가입 및 소셜 로그인 공통 사용
     */
    @Column(name = "email", length = 100)
    private String email;

    /**
     * 누적 포인트
     * - 기본값 0
     */
    @Column(name = "point", nullable = false)
    private Long point = 0L;

    // ===== 소셜 계정 컬렉션 =====

    /**
     * 소셜 계정 목록
     * - 한 유저가 여러 소셜 계정을 연결할 수 있음 (카카오 + 구글 등)
     * - SocialAccount는 값 객체(@Embeddable)이고, 별도 테이블(social_account)에 저장됨
     */
    @ElementCollection
    @CollectionTable(
            name = "social_account",
            joinColumns = @JoinColumn(name = "user_id")
    )
    private Set<SocialAccount> socialAccounts = new HashSet<>();

    // ===== 프로필 (임베디드 값 객체) =====

    /**
     * 유저의 프로필 정보
     * - null 일 수 있음 (아직 프로필을 작성하지 않은 경우)
     */
    @Embedded
    private UserProfile profile;

    // ===== 생성자 (외부에서 new 불가, 팩토리 메서드로만 생성) =====

    private User(
            UserId id,
            String userName,
            String password,
            UserRole role,
            String name,
            String slackId,
            UserStatus status,
            AdvisorStatus advisorStatus
    ) {
        this.id = id;
        this.userName = userName;
        this.password = password;
        this.role = role;
        this.name = name;
        this.slackId = slackId;
        this.status = status;
        this.advisorStatus = advisorStatus;
    }

    // ==========================
    //  팩토리 메서드들
    // ==========================

    public static User createLocalUser(
            UserId id,
            String userName,
            String encodedPassword,
            UserRole role,
            String name,
            String slackId
    ) {
        User user = new User(
                id,
                userName,
                encodedPassword,
                role,
                name,
                slackId,
                UserStatus.ACTIVE,
                AdvisorStatus.NOT_REQUESTED
        );
        user.passwordInitialized = true;   // 로컬 회원가입은 비밀번호가 이미 설정됨
        return user;
    }

    public static User createWithSocial(
            UserId id,
            String name,
            SocialAccount socialAccount,
            String generatedUserName,
            String randomPassword
    ) {
        User user = new User(
                id,
                generatedUserName,
                randomPassword,
                UserRole.USER,
                name,
                null,                   // slackId
                UserStatus.ACTIVE,
                AdvisorStatus.NOT_REQUESTED
        );

        user.passwordInitialized = false;       // 소셜 회원은 최초엔 비번 미설정 상태
        user.addSocialAccount(socialAccount);   // 첫 소셜 계정 연결

        return user;
    }

    // ==========================
    //  도메인 행위 메서드들
    // ==========================

    public void addSocialAccount(SocialAccount socialAccount) {
        this.socialAccounts.add(socialAccount);
    }

    public void removeSocialAccount(SocialAccount socialAccount) {
        this.socialAccounts.remove(socialAccount);
    }

    public boolean isPasswordInitialized() {
        return passwordInitialized;
    }

    /**
     * 비밀번호 최초 설정
     * - 이미 passwordInitialized == true 인 상태에서 호출되면
     *   PASSWORD_ALREADY_INITIALIZED 에러 발생
     */
    public void initPassword(String encodedPassword) {
        if (this.passwordInitialized) {
            ErrorCode e = ErrorCode.PASSWORD_ALREADY_INITIALIZED;
            throw new ApplicationException(
                    e.getHttpStatus(),
                    e.getCode(),
                    e.getDefaultMessage()
            );
        }
        this.password = encodedPassword;
        this.passwordInitialized = true;
    }

    /** 비밀번호 변경 (일반 로그인/비밀번호 설정 기능용) */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /** 계정 상태 변경 (관리자 페이지 등에서 사용) */
    public void changeStatus(UserStatus status) {
        this.status = status;
    }

    /** 회원 역할 변경 (권한 승급/변경 시 사용) */
    public void changeRole(UserRole role) {
        this.role = role;
    }

    /** 기본 정보 수정 (이름 + 슬랙 ID) */
    public void updateBasicInfo(String name, String slackId) {
        this.name = name;
        this.slackId = slackId;
    }

    /** 어드바이저 상태 변경 (심사 로직에서 사용) */
    public void changeAdvisorStatus(AdvisorStatus advisorStatus) {
        this.advisorStatus = advisorStatus;
    }

    // ==========================
    //  프로필(UserProfile) 관련 도메인 메서드
    // ==========================

    /**
     * 프로필 최초 생성
     * - 이미 profile 이 존재하면 PROFILE_ALREADY_EXISTS 에러
     */
    public void createProfile(
            String mainCategory,
            String intro,
            Integer careerYears,
            boolean isActive
    ) {
        if (this.profile != null) {
            ErrorCode e = ErrorCode.PROFILE_ALREADY_EXISTS;
            throw new ApplicationException(
                    e.getHttpStatus(),
                    e.getCode(),
                    e.getDefaultMessage()
            );
        }
        this.profile = UserProfile.of(mainCategory, intro, careerYears, isActive);
    }

    /**
     * 프로필 수정
     * - profile 이 null 이면 PROFILE_NOT_FOUND 에러
     */
    public void updateProfile(
            String mainCategory,
            String intro,
            Integer careerYears,
            boolean isActive
    ) {
        if (this.profile == null) {
            ErrorCode e = ErrorCode.PROFILE_NOT_FOUND;
            throw new ApplicationException(
                    e.getHttpStatus(),
                    e.getCode(),
                    e.getDefaultMessage()
            );
        }
        this.profile = UserProfile.of(mainCategory, intro, careerYears, isActive);
    }

    /**
     * 프로필 비활성화
     * - profile 이 null 이면 PROFILE_NOT_FOUND 에러
     */
    public void deactivateProfile() {
        if (this.profile == null) {
            ErrorCode e = ErrorCode.PROFILE_NOT_FOUND;
            throw new ApplicationException(
                    e.getHttpStatus(),
                    e.getCode(),
                    e.getDefaultMessage()
            );
        }
        this.profile = UserProfile.of(
                this.profile.getMainCategory(),
                this.profile.getIntro(),
                this.profile.getCareerYears(),
                false      // active → false
        );
    }

    /**
     * 포인트 적립 로직
     * - 비즈니스 규칙: 적립 금액은 0보다 커야 함
     */
    public void addPoint(Long amount) {
        if (amount == null || amount <= 0) {
            // 필요 시 전용 에러 코드 사용 (예: INVALID_POINT_AMOUNT)
            throw new ApplicationException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_POINT_AMOUNT",
                    "적립할 포인트는 0보다 커야 합니다."
            );
        }
        this.point += amount;
    }
}
