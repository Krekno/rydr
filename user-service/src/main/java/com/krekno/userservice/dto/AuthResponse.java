package com.krekno.userservice.dto;

public record AuthResponse(String refreshToken,
                           String accessToken) {
}
