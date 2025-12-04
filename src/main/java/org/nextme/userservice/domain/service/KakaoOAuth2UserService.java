package org.nextme.userservice.domain.service;

import lombok.RequiredArgsConstructor;
import org.nextme.userservice.domain.SocialAccount;
import org.nextme.userservice.domain.SocialProvider;
import org.nextme.userservice.domain.User;
import org.nextme.userservice.domain.UserId;
import org.nextme.userservice.domain.repository.UserRepository;
import org.nextme.userservice.infrastructure.security.NextmeUserPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KakaoOAuth2UserService extends DefaultOAuth2UserService {

    // 우리 DB(p_user, p_social_account)에 접근하기 위한 리포지토리
    private final UserRepository userRepository;

    // 소셜 로그인 유저에게 임시/랜덤 패스워드를 발급할 때 사용 (실제 로그인에는 안 씀)
    private final PasswordEncoder passwordEncoder;

    // 소셜 로그인 사용자의 userName(로그인 ID)을 자동으로 생성해주는 유틸
    private final UserNameGenerator userNameGenerator;

    /**
     * 카카오에서 Access Token으로 사용자 정보를 가져온 뒤,
     * 그 정보를 기반으로 우리 서비스의 User를 조회/생성하는 메서드.
     *
     * 스프링 시큐리티가 OAuth2 로그인 시 이 메서드를 자동으로 호출함.
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        // 1. DefaultOAuth2UserService가 카카오 사용자 정보(/v2/user/me)를 불러와서 OAuth2User로 만들어 줌
        OAuth2User oAuth2User = super.loadUser(userRequest);

        System.out.println(">>> KakaoOAuth2UserService.loadUser 실행됨. attributes = " + oAuth2User.getAttributes());

        // 2. 카카오에서 내려준 raw attributes (JSON)을 Map 형태로 꺼냄
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 3. 카카오 유저 고유 ID (Long 타입) 파싱
        //    응답 JSON의 "id" 필드는 Long 또는 Integer일 수 있으니 Number로 캐스팅 후 longValue 사용
        Long kakaoId = ((Number) attributes.get("id")).longValue();

        // 4. kakao_account 필드 (이메일, 프로필 등) 파싱
        Map<String, Object> kakaoAccount =
                (Map<String, Object>) attributes.get("kakao_account");

        // 5. 이메일 추출 (동의 안 했으면 null일 수 있음)
        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

        // 6. profile 정보 (닉네임 등) 파싱
        Map<String, Object> profile =
                kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;

        // 7. 닉네임 추출, 없으면 기본값 "카카오사용자" 사용
        String nickname = profile != null ? (String) profile.get("nickname") : "카카오사용자";

        // 8. 우리 도메인의 SocialAccount 값 객체 생성
        //    - provider: KAKAO
        //    - providerUserId: kakaoId (문자열로 저장)
        //    - email: 카카오 계정 이메일
        SocialAccount socialAccount = SocialAccount.of(
                SocialProvider.KAKAO,
                String.valueOf(kakaoId),
                email
        );

        // 9. 소셜 계정 정보로 우리 DB에서 User를 찾거나, 없으면 새로 생성
        User user = findOrCreateUserFromKakao(socialAccount, nickname, email);

        System.out.println(">>> 저장된/조회된 User = " + user.getId());
        // 10. 권한 목록 구성
        String roleName = user.getRole().name();

        List<String> roles = List.of(roleName);

        // 11. 우리 서비스 기준 인증 사용자(Principal)로 감싸서 리턴
        return new NextmeUserPrincipal(
                user.getId(),   // UserId
                email,
                nickname,
                roles,
                attributes
        );
    }

    /**
     * 1) 소셜 계정 정보로 이미 가입된 유저가 있으면 그 유저를 반환하고,
     * 2) 없으면 새로 User + SocialAccount를 생성해서 저장 후 반환.
     */
    private User findOrCreateUserFromKakao(SocialAccount socialAccount, String nickname, String email) {
        return userRepository
                .findBySocialAccountsProviderAndSocialAccountsProviderUserId(
                        socialAccount.getProvider(),          // ex) KAKAO
                        socialAccount.getProviderUserId()     // ex) "4622475502"
                )
                // 없으면 신규 유저 생성 로직 실행
                .orElseGet(() -> createNewUserFromKakao(socialAccount, nickname, email));
    }

    /**
     * 카카오 로그인으로 처음 들어온 사용자를 위한 신규 User 생성 로직.
     * - userId: 우리 서비스의 UUID (UserId 값 객체)
     * - nickname: 카카오 닉네임
     * - socialAccount: KAKAO + kakaoId + email
     * - generatedUserName: 우리 서비스용 로그인 ID
     * - randomPassword: 임시 비밀번호 (실제 로그인용 X, 그냥 형식상 저장)
     */
    private User createNewUserFromKakao(SocialAccount socialAccount, String nickname, String email) {
        // 1. "kakao", 닉네임, 이메일 등을 조합해서 서비스용 userName 생성
        String generatedUserName = userNameGenerator.generate("kakao", nickname, email);

        // 2. 임의의 랜덤 패스워드를 생성 후, 인코딩해서 저장
        //    (소셜 로그인 전용 계정이라 실제로 비밀번호로 로그인할 일은 거의 없음)
        String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        // 3. 우리 서비스의 UserId(UUID) 생성
        UserId userId = UserId.newId();

        // 4. 도메인에서 제공하는 팩토리 메서드로 "소셜 계정과 함께 생성되는 유저"를 만든다.
        User user = User.createWithSocial(
                userId,
                nickname,          // 표시용 닉네임
                socialAccount,     // KAKAO + kakaoId + email
                generatedUserName, // 우리 서비스 로그인 ID
                randomPassword     // 인코딩된 랜덤 패스워드
        );

        // 5. JPA를 통해 DB에 저장 후 반환
        return userRepository.save(user);
    }
}
