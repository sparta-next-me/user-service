package org.nextme.userservice.application.dto;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {}