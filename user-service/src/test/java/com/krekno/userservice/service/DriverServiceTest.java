package com.krekno.userservice.service;

import com.krekno.userservice.dto.DriverResponseDto;
import com.krekno.userservice.entity.Driver;
import com.krekno.userservice.entity.User;
import com.krekno.userservice.entity.enums.DriverStatus;
import com.krekno.userservice.entity.enums.Role;
import com.krekno.userservice.repository.DriverRepository;
import com.krekno.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {
    @Mock DriverRepository repository;
    @Mock UserRepository users;
    private DriverService service;
    private User user;
    private Driver driver;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        service = new DriverService(repository, users);
        user = User.builder().id(UUID.randomUUID()).firstName("Ada").lastName("Lovelace")
                .email("ada@example.com").phoneNumber("123").role(Role.ROLE_USER).build();
        driver = Driver.builder().id(UUID.randomUUID()).user(user).status(DriverStatus.OFFLINE)
                .isVerified(false).ratingAverage(4.0f).ratingCount(2).build();
        authentication = new UsernamePasswordAuthenticationToken(user.getId().toString(), null);
    }

    @Test
    void applicationPromotesUserAndCreatesUnverifiedOfflineDriver() {
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DriverResponseDto result = service.applyAsDriver(authentication);

        assertEquals(Role.ROLE_DRIVER, user.getRole());
        verify(users).save(user);
        ArgumentCaptor<Driver> saved = ArgumentCaptor.forClass(Driver.class);
        verify(repository).save(saved.capture());
        assertSame(user, saved.getValue().getUser());
        assertEquals(user.getId(), result.userId());
        assertEquals(DriverStatus.OFFLINE, result.status());
        assertFalse(result.isVerified());
        assertEquals(5.0f, result.ratingAverage());
        assertEquals(0, result.ratingCount());
        assertTrue(result.vehicles().isEmpty());
    }

    @Test
    void duplicateApplicationDoesNotPromoteOrSaveUser() {
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(repository.existsByUserId(user.getId())).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> service.applyAsDriver(authentication));

        assertEquals(Role.ROLE_USER, user.getRole());
        verify(users, never()).save(any());
        verify(repository, never()).save(any());
    }

    @Test
    void applicationRejectsMissingUser() {
        assertStatus(HttpStatus.NOT_FOUND, () -> service.applyAsDriver(authentication));
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @EnumSource(value = DriverStatus.class, names = "OFFLINE", mode = EnumSource.Mode.EXCLUDE)
    void unverifiedDriverCannotEnterActiveStatus(DriverStatus status) {
        when(repository.findByUserId(user.getId())).thenReturn(Optional.of(driver));

        assertStatus(HttpStatus.FORBIDDEN, () -> service.updateStatus(authentication, status));

        assertEquals(DriverStatus.OFFLINE, driver.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void unverifiedDriverCanGoOffline() {
        driver.setStatus(DriverStatus.AVAILABLE);
        when(repository.findByUserId(user.getId())).thenReturn(Optional.of(driver));

        assertEquals(DriverStatus.OFFLINE, service.updateStatus(authentication, DriverStatus.OFFLINE).status());
        verify(repository).save(driver);
    }

    @ParameterizedTest
    @EnumSource(DriverStatus.class)
    void verifiedDriverCanChangeStatus(DriverStatus status) {
        driver.setIsVerified(true);
        when(repository.findByUserId(user.getId())).thenReturn(Optional.of(driver));

        assertEquals(status, service.updateStatus(authentication, status).status());
        verify(repository).save(driver);
    }

    @Test
    void ratingUsesWeightedAverageAndIncrementsCount() {
        when(repository.findById(driver.getId())).thenReturn(Optional.of(driver));

        DriverResponseDto result = service.updateRating(driver.getId(), 1);

        assertEquals(3.0f, result.ratingAverage());
        assertEquals(3, result.ratingCount());
        verify(repository).save(driver);
    }

    @Test
    void firstRatingHandlesMissingHistoricalValues() {
        driver.setRatingAverage(null);
        driver.setRatingCount(null);
        when(repository.findById(driver.getId())).thenReturn(Optional.of(driver));

        DriverResponseDto result = service.updateRating(driver.getId(), 2);

        assertEquals(2.0f, result.ratingAverage());
        assertEquals(1, result.ratingCount());
    }

    @Test
    void internalStatusUpdatePersistsRequestedStatus() {
        when(repository.findById(driver.getId())).thenReturn(Optional.of(driver));

        assertEquals(DriverStatus.ON_TRIP, service.setDriverStatusInternal(driver.getId(), DriverStatus.ON_TRIP).status());
        verify(repository).save(driver);
    }

    @Test
    void verificationCanBeGrantedAndRevoked() {
        when(repository.findById(driver.getId())).thenReturn(Optional.of(driver));

        assertTrue(service.verifyDriver(driver.getId(), true).isVerified());
        assertFalse(service.verifyDriver(driver.getId(), false).isVerified());
        verify(repository, times(2)).save(driver);
    }

    @Test
    void listingSupportsAllVerifiedAndUnverifiedDrivers() {
        when(repository.findAll()).thenReturn(List.of(driver));
        when(repository.findByIsVerified(true)).thenReturn(List.of());
        when(repository.findByIsVerified(false)).thenReturn(List.of(driver));

        assertEquals(driver.getId(), service.getAllDrivers(null).getFirst().id());
        assertTrue(service.getAllDrivers(true).isEmpty());
        assertEquals(driver.getId(), service.getAllDrivers(false).getFirst().id());
    }

    @Test
    void profileLookupMapsUserInformation() {
        when(repository.findByUserId(user.getId())).thenReturn(Optional.of(driver));
        when(repository.findById(driver.getId())).thenReturn(Optional.of(driver));

        DriverResponseDto result = service.getMyDriverProfile(authentication);

        assertEquals(service.getDriverById(driver.getId()), result);
        assertEquals("Ada", result.firstName());
        assertEquals("Lovelace", result.lastName());
        assertEquals(user.getEmail(), result.email());
        assertEquals(user.getPhoneNumber(), result.phoneNumber());
    }

    @Test
    void missingDriverReturnsNotFoundForReadsAndWrites() {
        assertStatus(HttpStatus.NOT_FOUND, () -> service.getMyDriverProfile(authentication));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.getDriverById(driver.getId()));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.updateStatus(authentication, DriverStatus.AVAILABLE));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.updateRating(driver.getId(), 5));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.verifyDriver(driver.getId(), true));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.setDriverStatusInternal(driver.getId(), DriverStatus.OFFLINE));
        verify(repository, never()).save(any());
    }

    private static void assertStatus(HttpStatus status, org.junit.jupiter.api.function.Executable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action).getStatusCode());
    }
}
