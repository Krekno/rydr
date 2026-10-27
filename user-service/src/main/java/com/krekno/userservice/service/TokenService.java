package com.krekno.userservice.service;

import com.krekno.userservice.dto.AuthResponse;
import com.krekno.userservice.entity.RefreshToken;
import com.krekno.userservice.entity.User;
import com.krekno.userservice.repository.RefreshTokenRepository;
import com.krekno.userservice.repository.UserRepository;
import com.krekno.userservice.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final UserRepository userRepository;
    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();

        String scope = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(15, ChronoUnit.MINUTES))
                .subject(authentication.getName())
                .claim("scope", scope)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @Transactional
    public String generateRefreshToken(Authentication authentication) {
        User user = userRepository
                .findById(UUID.fromString(authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        String hashedToken = hashToken(token);

        RefreshToken refreshToken = refreshTokenRepository
                .findByUserId(user.getId())
                .orElseGet(() -> RefreshToken.builder()
                        .user(user)
                        .build());

        refreshToken.setToken(hashedToken);
        refreshToken.setExpiresAt(
                Instant.now().plus(7, ChronoUnit.DAYS)
        );

        refreshTokenRepository.save(refreshToken);

        return token;
    }

    @Transactional
    public AuthResponse refreshAccessToken(String rawRefreshToken) {
        String hashedToken = hashToken(rawRefreshToken);

        RefreshToken refreshToken = refreshTokenRepository.findByToken(hashedToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
        }

        User user = refreshToken.getUser();
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getId().toString(),
                null,
                userDetails.getAuthorities()
        );

        String newAccessToken = generateToken(auth);
        String newRefreshToken = generateRefreshToken(auth);

        return new AuthResponse(newRefreshToken, newAccessToken);
    }

    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public void deleteRefreshToken(Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            refreshTokenRepository.deleteByUser_Id(UUID.fromString(authentication.getName()));
        }
    }
}
