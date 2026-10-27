package com.krekno.userservice.controller;

import com.krekno.userservice.dto.AuthResponse;
import com.krekno.userservice.dto.LoginDto;
import com.krekno.userservice.dto.RefreshTokenRequest;
import com.krekno.userservice.dto.RegisterDto;
import com.krekno.userservice.dto.UserDto;
import com.krekno.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterDto registerDto) {
        userService.createUser(registerDto);
        LoginDto loginDto = new LoginDto(registerDto.email(), registerDto.password());
        return ResponseEntity.ok(userService.authenticateUser(loginDto));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginDto loginDto) {
        return ResponseEntity.ok(userService.authenticateUser(loginDto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody @Valid RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(userService.refreshToken(refreshTokenRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        userService.logout(authentication);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/update")
    public ResponseEntity<UserDto> updateUser(Authentication authentication,
                                              @RequestBody @Valid RegisterDto registerDto) {
        return ResponseEntity.ok(userService.updateUser(authentication, registerDto));
    }

}
