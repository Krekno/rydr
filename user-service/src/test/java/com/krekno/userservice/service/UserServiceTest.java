package com.krekno.userservice.service;

import com.krekno.userservice.dto.*;
import com.krekno.userservice.entity.User;
import com.krekno.userservice.entity.enums.Role;
import com.krekno.userservice.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository repository;
    @Mock TokenService tokens;
    @Mock AuthenticationManager authenticationManager;
    @Mock PasswordEncoder passwords;
    private UserService service;
    private User user;
    private Authentication authentication;
    private final RegisterDto registration = new RegisterDto("Ada", "Lovelace", "ada@example.com", "password123", "+905551234567");

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        service = new UserService(repository, tokens, authenticationManager, passwords);
        user = User.builder().id(UUID.randomUUID()).firstName("Ada").lastName("Lovelace")
                .email(registration.email()).phoneNumber(registration.phoneNumber())
                .password("old-hash").role(Role.ROLE_USER).build();
        authentication = new UsernamePasswordAuthenticationToken(user.getId().toString(), null);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createUserEncodesPasswordAndAssignsUserRole() {
        when(passwords.encode(registration.password())).thenReturn("encoded-password");

        service.createUser(registration);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(repository).save(saved.capture());
        User result = saved.getValue();
        assertEquals(registration.firstName(), result.getFirstName());
        assertEquals(registration.lastName(), result.getLastName());
        assertEquals(registration.email(), result.getEmail());
        assertEquals(registration.phoneNumber(), result.getPhoneNumber());
        assertEquals("encoded-password", result.getPassword());
        assertEquals(Role.ROLE_USER, result.getRole());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void createUserRejectsDuplicateEmailWithoutSaving() {
        when(repository.existsByEmail(registration.email())).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> service.createUser(registration));

        verify(repository, never()).save(any());
        verifyNoInteractions(passwords);
    }

    @Test
    void createUserRejectsDuplicatePhoneWithoutSaving() {
        when(repository.existsByPhoneNumber(registration.phoneNumber())).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> service.createUser(registration));

        verify(repository, never()).save(any());
        verifyNoInteractions(passwords);
    }

    @Test
    void loginAuthenticatesWithUserIdAndStoresAuthentication() {
        when(repository.findUserByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(tokens.generateRefreshToken(authentication)).thenReturn("refresh");
        when(tokens.generateToken(authentication)).thenReturn("access");

        assertEquals(new AuthResponse("refresh", "access"),
                service.authenticateUser(new LoginDto(user.getEmail(), "password123")));

        ArgumentCaptor<Authentication> submitted = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(submitted.capture());
        assertEquals(user.getId().toString(), submitted.getValue().getName());
        assertEquals("password123", submitted.getValue().getCredentials());
        assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void loginRejectsUnknownEmailWithoutIssuingTokens() {
        when(repository.findUserByEmail(user.getEmail())).thenReturn(Optional.empty());

        assertStatus(HttpStatus.UNAUTHORIZED, () -> service.authenticateUser(new LoginDto(user.getEmail(), "wrong")));

        verifyNoInteractions(authenticationManager, tokens);
    }

    @Test
    void loginRejectsBadPasswordWithoutUpdatingContextOrIssuingTokens() {
        when(repository.findUserByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class,
                () -> service.authenticateUser(new LoginDto(user.getEmail(), "wrong")));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(tokens);
    }

    @Test
    void refreshAndLogoutDelegateToTokenService() {
        AuthResponse response = new AuthResponse("new-refresh", "new-access");
        when(tokens.refreshAccessToken("refresh")).thenReturn(response);

        assertSame(response, service.refreshToken(new RefreshTokenRequest("refresh")));
        service.logout(authentication);

        verify(tokens).deleteRefreshToken(authentication);
    }

    @Test
    void profileAndIdLookupReturnPublicUserFields() {
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        UserDto expected = new UserDto(user.getId(), "Ada", "Lovelace", user.getEmail(), user.getPhoneNumber(), "ROLE_USER");

        assertEquals(expected, service.getUserProfile(authentication));
        assertEquals(expected, service.user(authentication));
        assertEquals(expected, service.getUserById(user.getId()));
    }

    @Test
    void missingUserReturnsNotFoundForReadsAndWrites() {
        when(repository.findById(user.getId())).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> service.getUserProfile(authentication));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.getUserById(user.getId()));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.updateProfile(authentication, new UpdateUserRequest("New", "Name", "123")));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.updateUser(authentication, registration));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.changePassword(authentication, new ChangePasswordRequest("old", "new-password")));
        verify(repository, never()).save(any());
    }

    @Test
    void updateProfileChangesNamesAndPhoneAndPreservesCredentials() {
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));

        UserDto result = service.updateProfile(authentication, new UpdateUserRequest("Grace", "Hopper", "+905559999999"));

        assertEquals("Grace", result.firstName());
        assertEquals("Hopper", result.lastName());
        assertEquals("+905559999999", result.phoneNumber());
        assertEquals(registration.email(), result.email());
        assertEquals("old-hash", user.getPassword());
        verify(repository).save(user);
    }

    @Test
    void updateProfileAllowsUnchangedPhoneWithoutDuplicateCheck() {
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));

        service.updateProfile(authentication, new UpdateUserRequest("Grace", "Hopper", user.getPhoneNumber()));

        verify(repository, never()).existsByPhoneNumber(any());
        verify(repository).save(user);
    }

    @Test
    void updateProfileRejectsTakenPhoneBeforeMutatingUser() {
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        when(repository.existsByPhoneNumber("taken")).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT,
                () -> service.updateProfile(authentication, new UpdateUserRequest("New", "Name", "taken")));

        assertEquals("Ada", user.getFirstName());
        assertEquals(registration.phoneNumber(), user.getPhoneNumber());
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void updateUserPreservesPasswordWhenNoReplacementIsProvided(String password) {
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));

        UserDto result = service.updateUser(authentication, new RegisterDto("Grace", "Hopper", "grace@example.com", password, "123"));

        assertEquals("Grace", result.firstName());
        assertEquals("Hopper", result.lastName());
        assertEquals("grace@example.com", result.email());
        assertEquals("123", result.phoneNumber());
        assertEquals("old-hash", user.getPassword());
        verifyNoInteractions(passwords);
        verify(repository).save(user);
    }

    @Test
    void updateUserEncodesReplacementPassword() {
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwords.encode(registration.password())).thenReturn("new-hash");

        service.updateUser(authentication, registration);

        assertEquals("new-hash", user.getPassword());
        verify(repository).save(user);
    }

    @Test
    void changePasswordChecksCurrentPasswordAndSavesEncodedReplacement() {
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwords.matches("current", "old-hash")).thenReturn(true);
        when(passwords.encode("replacement")).thenReturn("new-hash");

        service.changePassword(authentication, new ChangePasswordRequest("current", "replacement"));

        assertEquals("new-hash", user.getPassword());
        verify(repository).save(user);
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() {
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));

        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.changePassword(authentication, new ChangePasswordRequest("wrong", "replacement")));

        assertEquals("old-hash", user.getPassword());
        verify(passwords, never()).encode(any());
        verify(repository, never()).save(any());
    }

    private static void assertStatus(HttpStatus status, org.junit.jupiter.api.function.Executable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action).getStatusCode());
    }
}
