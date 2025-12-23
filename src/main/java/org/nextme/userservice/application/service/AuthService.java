package org.nextme.userservice.application.service;

import lombok.RequiredArgsConstructor;
import org.nextme.common.jwt.JwtTokenPair;
import org.nextme.common.jwt.JwtTokenProvider;
import org.nextme.infrastructure.exception.ApplicationException;
import org.nextme.infrastructure.exception.ErrorCode;
import org.nextme.userservice.application.dto.LoginRequest;
import org.nextme.userservice.application.dto.SignupRequest;
import org.nextme.userservice.application.dto.TokenResponse;
import org.nextme.userservice.domain.User;
import org.nextme.userservice.domain.UserId;
import org.nextme.userservice.domain.UserRole;
import org.nextme.userservice.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Security 설정 필요
    private final JwtTokenProvider jwtTokenProvider;

    public void signup(SignupRequest request) {
        // 중복 체크
        if (userRepository.existsByUserName(request.userName())) {
            throw new ApplicationException(ErrorCode.DUPLICATED_USERNAME);
        }

        UserId userId = UserId.of(UUID.randomUUID());
        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.createLocalUser(
                userId,
                request.userName(),
                encodedPassword,
                UserRole.USER,
                request.name(),
                request.slackId()
        );
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUserName(request.userName())
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApplicationException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        // 권한 리스트 생성
        List<String> roles = List.of(user.getRole().name());

        // JwtTokenProvider의 메서드명 generateTokenPair 사용
        JwtTokenPair tokenPair = jwtTokenProvider.generateTokenPair(
                user.getId().getId().toString(),
                user.getName(),
                null, // 현재 User 엔티티에 email 필드가 없다면 null
                user.getSlackId(),
                roles
        );

        // 수정된 TokenResponse 레코드 형식에 맞춰 반환
        return new TokenResponse(
                user.getId().getId().toString(),
                user.getName(),
                null, // email
                user.getSlackId(),
                roles,
                tokenPair.accessToken(),
                tokenPair.refreshToken()
        );
    }
}