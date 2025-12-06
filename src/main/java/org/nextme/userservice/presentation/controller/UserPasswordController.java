package org.nextme.userservice.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.nextme.userservice.application.dto.ChangePasswordRequest;
import org.nextme.userservice.application.dto.InitialPasswordRequest;
import org.nextme.userservice.application.service.UserPasswordService;
import org.nextme.userservice.infrastructure.security.NextmeUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/users")
public class UserPasswordController {

    private final UserPasswordService userPasswordService;

    /**
     * [소셜 + 비번 미설정] → 비번 최초 설정
     * - Authorization 필요
     * - currentPassword 없음
     */
    @PostMapping("/me/password/initial")
    public ResponseEntity<Void> setInitialPassword(
            @AuthenticationPrincipal NextmeUserPrincipal principal,
            @RequestBody InitialPasswordRequest request
    ) {
        userPasswordService.setInitialPassword(
                principal.getUserId(),
                request.newPassword()
        );

        return ResponseEntity.ok().build();
    }

    /**
     * 비밀번호 변경
     * - Authorization 필요
     * - currentPassword + newPassword 둘 다 필요
     */
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal NextmeUserPrincipal principal,
            @RequestBody ChangePasswordRequest request
    ) {
        userPasswordService.changePassword(
                principal.getUserId(),
                request.currentPassword(),
                request.newPassword()
        );

        return ResponseEntity.ok().build();
    }
}
