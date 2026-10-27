package com.krekno.userservice.service;

import com.krekno.userservice.dto.*;
import com.krekno.userservice.entity.User;
import com.krekno.userservice.entity.enums.Role;
import com.krekno.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void createUser(RegisterDto registerDto) {
        Instant now = Instant.now();

        if (userRepository.existsByEmail(registerDto.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        if (userRepository.existsByPhoneNumber(registerDto.phoneNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already exists");
        }

        User newUser = User.builder()
                .firstName(registerDto.firstName())
                .lastName(registerDto.lastName())
                .email(registerDto.email())
                .password(passwordEncoder.encode(registerDto.password()))
                .phoneNumber(registerDto.phoneNumber())
                .role(Role.ROLE_USER)
                .createdAt(now)
                .build();

        userRepository.save(newUser);
    }

    public AuthResponse authenticateUser(LoginDto loginDto) {
        User user = userRepository.findUserByEmail(loginDto.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getId().toString(),
                        loginDto.password()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return new AuthResponse(
                tokenService.generateRefreshToken(authentication),
                tokenService.generateToken(authentication)
        );
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        return tokenService.refreshAccessToken(request.refreshToken());
    }

    public void logout(Authentication authentication) {
        tokenService.deleteRefreshToken(authentication);
    }

    public UserDto getUserProfile(Authentication authentication) {
        User user = userRepository.findById(UUID.fromString(authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapToDto(user);
    }

    public UserDto user(Authentication authentication) {
        return getUserProfile(authentication);
    }

    @Transactional
    public UserDto updateProfile(Authentication authentication, UpdateUserRequest request) {
        User user = userRepository.findById(UUID.fromString(authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!user.getPhoneNumber().equals(request.phoneNumber()) &&
                userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already taken");
        }

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhoneNumber(request.phoneNumber());
        userRepository.save(user);

        return mapToDto(user);
    }

    @Transactional
    public UserDto updateUser(Authentication authentication, RegisterDto registerDto) {
        User user = userRepository.findById(UUID.fromString(authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setFirstName(registerDto.firstName());
        user.setLastName(registerDto.lastName());
        user.setEmail(registerDto.email());
        user.setPhoneNumber(registerDto.phoneNumber());
        if (registerDto.password() != null && !registerDto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(registerDto.password()));
        }
        userRepository.save(user);
        return mapToDto(user);
    }

    @Transactional
    public void changePassword(Authentication authentication, ChangePasswordRequest request) {
        User user = userRepository.findById(UUID.fromString(authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password does not match");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    public UserDto getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapToDto(user);
    }

    private UserDto mapToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole().name()
        );
    }
}
