package com.krekno.userservice.service;

import com.krekno.userservice.dto.AuthResponse;
import com.krekno.userservice.entity.RefreshToken;
import com.krekno.userservice.entity.User;
import com.krekno.userservice.entity.enums.Role;
import com.krekno.userservice.repository.RefreshTokenRepository;
import com.krekno.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {
    @Mock UserRepository users;
    @Mock JwtEncoder encoder;
    @Mock RefreshTokenRepository repository;
    private TokenService service;
    private User user;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        service = new TokenService(users, encoder, repository);
        user = User.builder().id(UUID.randomUUID()).role(Role.ROLE_DRIVER).build();
        authentication = new UsernamePasswordAuthenticationToken(user.getId().toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_DRIVER"), new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void accessTokenContainsIdentityAuthoritiesAndFifteenMinuteLifetime() {
        stubEncoder();

        assertEquals("access-token", service.generateToken(authentication));

        ArgumentCaptor<JwtEncoderParameters> parameters = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(encoder).encode(parameters.capture());
        JwtClaimsSet claims = parameters.getValue().getClaims();
        assertEquals(user.getId().toString(), claims.getSubject());
        assertEquals("self", claims.getClaim("iss"));
        assertEquals("ROLE_DRIVER ROLE_USER", claims.getClaim("scope"));
        assertEquals(Duration.ofMinutes(15), Duration.between(Objects.requireNonNull(claims.getIssuedAt()), claims.getExpiresAt()));
    }

    @Test
    void refreshTokenPersistsOnlyHashAndExpiresInSevenDays() {
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        Instant before = Instant.now().plus(Duration.ofDays(7));

        String raw = service.generateRefreshToken(authentication);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertEquals(32, Base64.getUrlDecoder().decode(raw).length);
        assertFalse(raw.contains("="));
        assertEquals(TokenService.hashToken(raw), saved.getValue().getToken());
        assertNotEquals(raw, saved.getValue().getToken());
        assertSame(user, saved.getValue().getUser());
        assertFalse(saved.getValue().getExpiresAt().isBefore(before));
        assertFalse(saved.getValue().getExpiresAt().isAfter(Instant.now().plus(Duration.ofDays(7))));
    }

    @Test
    void generatingRefreshTokenReusesExistingRecordAndRotatesSecret() {
        RefreshToken existing = RefreshToken.builder().id(UUID.randomUUID()).user(user).token("old-hash").build();
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(repository.findByUserId(user.getId())).thenReturn(Optional.of(existing));

        String first = service.generateRefreshToken(authentication);
        String second = service.generateRefreshToken(authentication);

        assertNotEquals(first, second);
        assertEquals(TokenService.hashToken(second), existing.getToken());
        verify(repository, times(2)).save(existing);
    }

    @Test
    void generatingRefreshTokenRejectsMissingUser() {
        assertStatus(HttpStatus.NOT_FOUND, () -> service.generateRefreshToken(authentication));
        verifyNoInteractions(repository, encoder);
    }

    @Test
    void refreshRejectsUnknownTokenWithoutIssuingCredentials() {
        assertStatus(HttpStatus.UNAUTHORIZED, () -> service.refreshAccessToken("unknown"));
        verify(repository).findByToken(TokenService.hashToken("unknown"));
        verify(repository, never()).save(any());
        verifyNoInteractions(encoder, users);
    }

    @Test
    void refreshDeletesExpiredTokenAndRejectsRequest() {
        RefreshToken expired = RefreshToken.builder().user(user).expiresAt(Instant.now().minusSeconds(60)).build();
        when(repository.findByToken(TokenService.hashToken("expired"))).thenReturn(Optional.of(expired));

        assertStatus(HttpStatus.UNAUTHORIZED, () -> service.refreshAccessToken("expired"));

        verify(repository).delete(expired);
        verify(repository, never()).save(any());
        verifyNoInteractions(encoder, users);
    }

    @Test
    void refreshRotatesTokenAndUsesCurrentUserRoleForAccessToken() {
        RefreshToken existing = RefreshToken.builder().user(user).token(TokenService.hashToken("old"))
                .expiresAt(Instant.now().plusSeconds(300)).build();
        when(repository.findByToken(existing.getToken())).thenReturn(Optional.of(existing));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(repository.findByUserId(user.getId())).thenReturn(Optional.of(existing));
        stubEncoder();

        AuthResponse result = service.refreshAccessToken("old");

        assertEquals("access-token", result.accessToken());
        assertNotEquals("old", result.refreshToken());
        assertEquals(TokenService.hashToken(result.refreshToken()), existing.getToken());
        verify(repository).save(existing);
        ArgumentCaptor<JwtEncoderParameters> parameters = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(encoder).encode(parameters.capture());
        assertEquals("ROLE_DRIVER", parameters.getValue().getClaims().getClaim("scope"));
        assertEquals(user.getId().toString(), parameters.getValue().getClaims().getSubject());
    }

    @Test
    void logoutDeletesOnlyAuthenticatedUsersRefreshToken() {
        service.deleteRefreshToken(authentication);
        verify(repository).deleteByUser_Id(user.getId());
    }

    @Test
    void logoutWithoutAuthenticationDoesNothing() {
        service.deleteRefreshToken(null);
        Authentication unnamed = mock(Authentication.class);
        service.deleteRefreshToken(unnamed);
        verifyNoInteractions(repository);
    }

    @Test
    void hashTokenMatchesKnownSha256Vector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", TokenService.hashToken("abc"));
    }

    private void stubEncoder() {
        when(encoder.encode(any())).thenReturn(Jwt.withTokenValue("access-token")
                .header("alg", "RS256").subject(user.getId().toString()).build());
    }

    private static void assertStatus(HttpStatus status, org.junit.jupiter.api.function.Executable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action).getStatusCode());
    }
}
