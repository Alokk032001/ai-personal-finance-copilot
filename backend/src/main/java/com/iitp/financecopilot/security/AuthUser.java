package com.iitp.financecopilot.security;

import java.util.UUID;

/**
 * JWT principal. userId on bills is always this UUID as a string — never taken from JSON.
 */
public record AuthUser(UUID id, String email) {
}
