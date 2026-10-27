package com.krekno.userservice.controller;

import com.krekno.userservice.dto.ChangePasswordRequest;
import com.krekno.userservice.dto.UpdateUserRequest;
import com.krekno.userservice.dto.UserDto;
import com.krekno.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.getUserProfile(authentication));
    }

    @PutMapping("/me")
    public ResponseEntity<UserDto> updateCurrentUser(Authentication authentication,
                                                     @RequestBody @Valid UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateProfile(authentication, request));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(Authentication authentication,
                                               @RequestBody @Valid ChangePasswordRequest request) {
        userService.changePassword(authentication, request);
        return ResponseEntity.noContent().build();
    }
}
