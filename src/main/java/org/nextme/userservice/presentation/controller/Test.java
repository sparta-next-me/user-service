package org.nextme.userservice.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class Test {
    @GetMapping("/test")
    public String test() {
        return "user-service OK";
    }

    @GetMapping("/me")
    public Object me(@org.springframework.security.core.annotation.AuthenticationPrincipal
                     org.springframework.security.oauth2.core.user.OAuth2User principal) {
        return principal;
    }
}
