package org.nextme.userservice.domain.service;

import lombok.RequiredArgsConstructor;
import org.nextme.userservice.domain.SocialAccount;
import org.nextme.userservice.domain.User;
import org.nextme.userservice.domain.UserId;
import org.nextme.userservice.domain.repository.UserRepository;
import org.nextme.userservice.infrastructure.security.NextmeUserPrincipal;
import org.nextme.userservice.infrastructure.security.oauth.SocialUserProfile;
import org.nextme.userservice.infrastructure.security.oauth.SocialUserProfileMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * NextmeOAuth2UserService
 *
 * - Kakao / Google / Naver 등 "여러 소셜 로그인"을 공통 처리하는 OAuth2UserService
 * - Spring Security 의 DefaultOAuth2UserService 를 확장해서,
 *   1) Spring 이 Provider에서 가져온 OAuth2User를 받고
 *   2) Provider별 SocialUserProfileMapper로 공통 SocialUserProfile 로 변환하고
 *   3) 우리 도메인 User + SocialAccount 를 조회/생성하고
 *   4) NextmeUserPrincipal 로 감싸서 반환한다.
 *
 *   요 서비스는 "소셜 로그인 완료 시, 우리 서비스 기준 인증 사용자 만들기" 책임만 가진다.
 *    (JWT 발급은 SuccessHandler에서 함)
 */
@Service
@RequiredArgsConstructor
public class NextmeOAuth2UserService extends DefaultOAuth2UserService {

    // 우리 DB(p_user, p_social_account)에 접근하기 위한 리포지토리
    private final UserRepository userRepository;

    // 소셜 로그인 유저에게 임시/랜덤 패스워드를 발급할 때 사용 (실제 로그인에는 안 씀)
    private final PasswordEncoder passwordEncoder;

    // 소셜 로그인 사용자의 userName(로그인 ID)을 자동으로 생성해주는 유틸
    private final UserNameGenerator userNameGenerator;

    // Kakao / Google / Naver ... Provider별 Profile 매퍼 목록
    private final List<SocialUserProfileMapper> profileMappers;

    /**
     * Spring Security 가 OAuth2 로그인 과정에서
     * Access Token 으로 유저 정보를 가져온 뒤,
     * 최종적으로 호출하는 메서드.
     *
     * - userRequest: clientRegistration, accessToken 등 정보
     * - return: 인증된 사용자 정보 (OAuth2User 구현체)
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        // 1. DefaultOAuth2UserService 가 실제로 Provider(/userinfo, /me 등)에 요청해서 OAuth2User 를 만들어 줌
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest
                .getClientRegistration()
                .getRegistrationId(); // kakao / google / naver

        System.out.println(">>> NextmeOAuth2UserService.loadUser 실행됨. registrationId = "
                + registrationId + ", attributes = " + oAuth2User.getAttributes());

        // 2. registrationId 에 맞는 SocialUserProfileMapper 찾기
        SocialUserProfileMapper mapper = profileMappers.stream()
                .filter(m -> m.supports(registrationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원하지 않는 소셜 로그인 타입: " + registrationId
                ));

        // 3. Provider별 JSON 구조 → 우리 공통 SocialUserProfile 로 변환
        SocialUserProfile profile = mapper.map(userRequest, oAuth2User);

        // 4. 도메인 SocialAccount 값 객체 구성
        SocialAccount socialAccount = SocialAccount.of(
                profile.provider(),        // KAKAO / GOOGLE / NAVER
                profile.providerUserId(),  // ex) 카카오 id, 구글 sub, 네이버 id
                profile.email()            // 이메일
        );

        // 5. 소셜 계정으로 우리 DB에서 User 조회 or 새로 생성
        User user = findOrCreateUserFromSocial(
                socialAccount,
                profile.nickname(),
                profile.email()
        );

        System.out.println(">>> 저장된/조회된 User = " + user.getId());

        // 6. 권한 목록 구성 (우리 도메인의 UserRole 을 기반으로)
        String roleName = user.getRole().name(); // ex) USER, ADVISOR, ADMIN ...
        List<String> roles = List.of(roleName);

        // 7. 우리 서비스 기준 인증 사용자(Principal)로 감싸서 반환
        //    - 이후 SecurityContext 에 Authentication 으로 저장됨
        //    - onAuthenticationSuccess 에서 Authentication.getPrincipal() 로 꺼낼 수 있음
        return new NextmeUserPrincipal(
                user.getId(),           // UserId (값 객체)
                profile.email(),        // 이메일
                profile.nickname(),     // 닉네임
                roles,                  // ["USER"], ["ADVISOR"] ...
                profile.attributes()    // raw attributes (필요 시 참고)
        );
    }

    /**
     * 1) 소셜 계정 정보로 이미 가입된 유저가 있으면 그 유저를 반환하고,
     * 2) 없으면 새로 User + SocialAccount 를 생성해서 저장 후 반환.
     */
    private User findOrCreateUserFromSocial(
            SocialAccount socialAccount,
            String nickname,
            String email
    ) {
        return userRepository
                .findBySocialAccountsProviderAndSocialAccountsProviderUserId(
                        socialAccount.getProvider(),        // ex) KAKAO
                        socialAccount.getProviderUserId()   // ex) "4622475502"
                )
                // 없으면 신규 유저 생성 로직 실행
                .orElseGet(() -> createNewUserFromSocial(socialAccount, nickname, email));
    }

    /**
     * 소셜 로그인으로 "처음" 들어온 사용자를 위한 신규 User 생성 로직.
     *
     * - userId: 우리 서비스의 UUID (UserId 값 객체)
     * - nickname: 소셜 프로필 닉네임
     * - socialAccount: KAKAO/GOOGLE/NAVER + providerUserId + email
     * - generatedUserName: 우리 서비스용 user_name (로그인 핸들)
     * - randomPassword: 임시 비밀번호 (DB 제약 때문에 넣는 값, 실제로는 사용하지 않을 수 있음)
     */
    private User createNewUserFromSocial(
            SocialAccount socialAccount,
            String nickname,
            String email
    ) {
        // 1. "provider명 + 닉네임 + 이메일" 등을 조합해서 서비스용 userName 생성
        String generatedUserName = userNameGenerator.generate(
                socialAccount.getProvider().name().toLowerCase(), // kakao / google / naver
                nickname,
                email
        );

        // 2. 임의의 랜덤 패스워드를 생성 후, 인코딩해서 저장
        String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        // 3. 우리 서비스의 UserId(UUID) 생성
        UserId userId = UserId.newId();

        // 4. 도메인에서 제공하는 팩토리 메서드로 "소셜 계정과 함께 생성되는 유저" 만들기
        User user = User.createWithSocial(
                userId,
                nickname,          // 표시용 이름
                socialAccount,     // 소셜 계정 (provider + providerUserId + email)
                generatedUserName, // 우리 서비스 로그인 ID (user_name)
                randomPassword     // 인코딩된 랜덤 패스워드
        );

        // 5. JPA를 통해 DB에 저장 후 반환
        return userRepository.save(user);
    }
}
