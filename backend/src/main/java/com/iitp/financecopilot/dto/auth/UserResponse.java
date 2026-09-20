package com.iitp.financecopilot.dto.auth;

import java.util.UUID;

public record UserResponse(UUID id, String email, String fullName, String currency) {
}
