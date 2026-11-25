package com.logy.pocketpay.user.dto;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        String role
) {
}
