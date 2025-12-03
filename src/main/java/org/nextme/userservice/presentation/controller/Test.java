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

}
