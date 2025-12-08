package org.nextme.userservice.domain.repository;

import org.nextme.userservice.domain.AdvisorStatus;
import org.nextme.userservice.domain.SocialProvider;
import org.nextme.userservice.domain.User;
import org.nextme.userservice.domain.UserId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, UserId> {

    // 1) 특정 소셜(provider + providerUserId)로 유저 찾기
    Optional<User> findBySocialAccountsProviderAndSocialAccountsProviderUserId(
            SocialProvider provider,
            String providerUserId
    );

    // 어드바이저 신청(PENDING) 상태인 유저 목록 조회
    List<User> findByAdvisorStatus(AdvisorStatus advisorStatus);
}