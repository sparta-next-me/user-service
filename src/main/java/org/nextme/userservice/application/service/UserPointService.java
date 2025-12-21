package org.nextme.userservice.application.service;

import lombok.RequiredArgsConstructor;
import org.nextme.infrastructure.exception.ApplicationException;
import org.nextme.infrastructure.exception.ErrorCode;
import org.nextme.userservice.domain.User;
import org.nextme.userservice.domain.UserId;
import org.nextme.userservice.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserPointService {
    private final UserRepository userRepository;

    public void addPoint(UserId userId, Long amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));

        // 도메인 모델 내에 포인트 적립 로직 구현 권장 (user.addPoint(amount))
        user.addPoint(amount);
    }
}