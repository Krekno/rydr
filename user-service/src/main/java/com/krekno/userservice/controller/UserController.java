package com.krekno.userservice.controller;

import com.krekno.userservice.dto.LoginDto;
import com.krekno.userservice.dto.RegisterDto;
import com.krekno.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterDto registerDto) {
        userService.createUser(registerDto);
        LoginDto loginDto = new LoginDto(registerDto.email(), registerDto.password());
        return ResponseEntity.ok(userService.AuthenticateUser(loginDto));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginDto loginDto) {
        return ResponseEntity.ok(userService.AuthenticateUser(loginDto));
    }

}
