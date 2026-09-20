package com.iitp.financecopilot.dto.auth;

public record AuthResponse(String token, long expiresInSeconds, UserResponse user) {
}
