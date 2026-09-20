package com.iitp.financecopilot.services;

import com.iitp.financecopilot.common.ApiException;
import com.iitp.financecopilot.domain.User;
import com.iitp.financecopilot.dto.auth.AuthResponse;
import com.iitp.financecopilot.dto.auth.LoginRequest;
import com.iitp.financecopilot.dto.auth.RegisterRequest;
import com.iitp.financecopilot.dto.auth.UserResponse;
import com.iitp.financecopilot.repositories.UserRepository;
import com.iitp.financecopilot.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setCurrency("INR");
        user = userRepository.save(user);
        return toAuth(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return toAuth(user);
    }

    public UserResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return toUser(user);
    }

    private AuthResponse toAuth(User user) {
        return new AuthResponse(jwtService.issue(user.getId(), user.getEmail()), jwtService.expiresInSeconds(), toUser(user));
    }

    private static UserResponse toUser(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getCurrency());
    }
}
