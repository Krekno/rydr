package com.krekno.userservice.service;

import com.krekno.userservice.dto.LoginDto;
import com.krekno.userservice.dto.RegisterDto;
import com.krekno.userservice.entity.User;
import com.krekno.userservice.entity.enums.Role;
import com.krekno.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    public void createUser(RegisterDto registerDto) {
        Instant now = Instant.now();

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

    public String AuthenticateUser(LoginDto LoginDto) {
        User user = userRepository.findUserByEmail(LoginDto.email()).orElseThrow();

        Authentication authentication =
                authenticationManager
                        .authenticate(new UsernamePasswordAuthenticationToken(
                                user.getId().toString(),
                                LoginDto.password()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        authentication.getPrincipal();

        return authService.generateToken(authentication);
    }

}
