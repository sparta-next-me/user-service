package org.nextme.userservice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.SQLRestriction;
import org.nextme.common.jpa.BaseEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * User
 * - 서비스 내 "회원"을 나타내는 Aggregate Root
 * - 소셜 로그인/일반 회원 모두 이 엔티티로 관리
 * - 소셜 계정은 SocialAccount 값 객체 컬렉션(@ElementCollection)으로 관리
 */
@Getter
@Entity
@Table(name = "p_user")
@ToString(exclude = "socialAccounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL") // soft delete (BaseEntity에 deleted_at 있다고 가정)
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

    /**
     * ❗ 일반 회원가입 (ID/PW 직접 입력) 용 팩토리
     *
     * - userName: 사용자가 직접 입력한 로그인 ID
     * - encodedPassword: 암호화된 비밀번호(BCrypt 등)
     * - role: 일반 가입은 대부분 USER, 필요 시 다른 역할로도 생성 가능
     * - slackId: 처음에는 대부분 null
     * - status: 기본 ACTIVE
     * - advisorStatus: 기본 NOT_REQUESTED (아직 어드바이저 신청 전)
     */
    public static User createLocalUser(
            UserId id,
            String userName,
            String encodedPassword,
            UserRole role,
            String name,
            String slackId
    ) {
        return new User(
                id,
                userName,
                encodedPassword,
                role,
                name,
                slackId,
                UserStatus.ACTIVE,
                AdvisorStatus.NOT_REQUESTED
        );
    }

    /**
     * ❗ 소셜 로그인 최초 가입 용 팩토리
     *
     * - generatedUserName: 소셜 로그인 시 자동 생성된 내부 user_name
     *                      (ex. kakao_123abc, google_583920)
     * - randomPassword: 소셜 계정용 랜덤 패스워드(유저는 모름), DB 제약 때문에 입력
     * - role: 무조건 USER
     * - status: 무조건 ACTIVE
     * - advisorStatus: 무조건 NOT_REQUESTED
     * - slackId: 처음엔 null (어드바이저 신청 시 입력)
     */
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
        user.addSocialAccount(socialAccount);   // 첫 소셜 계정 연결
        return user;
    }

    // ==========================
    //  도메인 행위 메서드들
    // ==========================

    /**
     * 소셜 계정 추가 (카카오에 이어 구글 계정도 연결하는 경우 등)
     */
    public void addSocialAccount(SocialAccount socialAccount) {
        this.socialAccounts.add(socialAccount);
    }

    /**
     * 소셜 계정 해제 (연결 끊기)
     */
    public void removeSocialAccount(SocialAccount socialAccount) {
        this.socialAccounts.remove(socialAccount);
    }

    /**
     * 비밀번호 변경 (일반 로그인/비밀번호 설정 기능용)
     */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /**
     * 계정 상태 변경 (관리자 페이지 등에서 사용)
     */
    public void changeStatus(UserStatus status) {
        this.status = status;
    }

    /**
     * 어드바이저 상태 변경 (심사 로직에서 사용)
     */
    public void changeAdvisorStatus(AdvisorStatus advisorStatus) {
        this.advisorStatus = advisorStatus;
    }
}
