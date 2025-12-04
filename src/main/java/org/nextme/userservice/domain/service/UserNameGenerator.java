package org.nextme.userservice.domain.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserNameGenerator {

    public String generate(String providerPrefix, String nickname, String email) {
        String random = UUID.randomUUID().toString().substring(0, 6);
        return (providerPrefix + "_" + random).toLowerCase();
    }
}