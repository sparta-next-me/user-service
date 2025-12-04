package org.nextme.userservice.domain.repository;

import org.nextme.userservice.domain.SocialProvider;
import org.nextme.userservice.domain.User;
import org.nextme.userservice.domain.UserId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, UserId> {

    Optional<User> findBySocialAccountsProviderAndSocialAccountsProviderUserId(
            SocialProvider provider,
            String providerUserId
    );
}