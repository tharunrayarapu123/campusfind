package com.campusfind.service;

import com.campusfind.dto.request.LoginRequest;
import com.campusfind.dto.request.RegisterRequest;
import com.campusfind.dto.response.AuthResponse;
import com.campusfind.dto.response.UserResponse;
import com.campusfind.entity.Role;
import com.campusfind.entity.User;
import com.campusfind.exception.ApiException;
import com.campusfind.repository.UserRepository;
import com.campusfind.security.CustomUserDetails;
import com.campusfind.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.auth.allowed-email-domain:college.edu}")
    private String allowedEmailDomain;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String requiredDomain = allowedEmailDomain.startsWith("@") ? allowedEmailDomain.toLowerCase() : "@" + allowedEmailDomain.toLowerCase();

        if (!normalizedEmail.endsWith(requiredDomain)) {
            throw new ApiException("Registration failed: Only emails ending with " + requiredDomain + " are permitted", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException("An account with email " + normalizedEmail + " already exists", HttpStatus.BAD_REQUEST);
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .department(request.getDepartment())
                .year(request.getYear())
                .phone(request.getPhone())
                .role(Role.STUDENT)
                .banned(false)
                .karmaPoints(0)
                .build();

        User savedUser = userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String token = jwtService.generateToken(userDetails, savedUser.getId(), savedUser.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .expiresIn(jwtService.getExpirationMs())
                .user(toUserResponse(savedUser))
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        if (user.isBanned()) {
            throw new ApiException("Your account has been banned. Please contact campus administration.", HttpStatus.FORBIDDEN);
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtService.generateToken(userDetails, user.getId(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .expiresIn(jwtService.getExpirationMs())
                .user(toUserResponse(user))
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User profile not found", HttpStatus.NOT_FOUND));
        return toUserResponse(user);
    }

    public static UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .department(user.getDepartment())
                .year(user.getYear())
                .avatarUrl(user.getAvatarUrl())
                .karmaPoints(user.getKarmaPoints())
                .role(user.getRole())
                .banned(user.isBanned())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
