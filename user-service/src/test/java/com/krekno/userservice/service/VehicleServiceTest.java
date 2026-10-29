package com.krekno.userservice.service;

import com.krekno.userservice.dto.CreateVehicleRequest;
import com.krekno.userservice.dto.VehicleDto;
import com.krekno.userservice.entity.Driver;
import com.krekno.userservice.entity.Vehicle;
import com.krekno.userservice.repository.DriverRepository;
import com.krekno.userservice.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class VehicleServiceTest {
    @Mock VehicleRepository repository;
    @Mock DriverRepository drivers;
    private VehicleService service;
    private UUID userId;
    private Driver driver;
    private Vehicle vehicle;
    private Authentication authentication;
    private final CreateVehicleRequest request = new CreateVehicleRequest("Toyota", "Corolla", 2024, "White", "34 abc 123");

    @BeforeEach
    void setUp() {
        service = new VehicleService(repository, drivers);
        userId = UUID.randomUUID();
        driver = Driver.builder().id(UUID.randomUUID()).build();
        vehicle = Vehicle.builder().id(UUID.randomUUID()).driver(driver).make("Toyota").model("Corolla")
                .year(2024).color("White").licensePlate("34 ABC 123").build();
        authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null);
    }

    @Test
    void addVehicleNormalizesPlateAndAssociatesAuthenticatedDriver() {
        when(drivers.findByUserId(userId)).thenReturn(Optional.of(driver));
        when(repository.save(any())).thenAnswer(invocation -> {
            Vehicle saved = invocation.getArgument(0);
            saved.setId(vehicle.getId());
            return saved;
        });

        VehicleDto result = service.addVehicle(authentication, request);

        verify(repository).existsByLicensePlate("34 ABC 123");
        ArgumentCaptor<Vehicle> saved = ArgumentCaptor.forClass(Vehicle.class);
        verify(repository).save(saved.capture());
        assertSame(driver, saved.getValue().getDriver());
        assertEquals(expectedVehicle(), result);
    }

    @Test
    void addVehicleRejectsDuplicatePlateWithoutSaving() {
        when(drivers.findByUserId(userId)).thenReturn(Optional.of(driver));
        when(repository.existsByLicensePlate("34 ABC 123")).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> service.addVehicle(authentication, request));

        verify(repository, never()).save(any());
    }

    @Test
    void listVehiclesUsesAuthenticatedDriverAndMapsAllFields() {
        when(drivers.findByUserId(userId)).thenReturn(Optional.of(driver));
        when(repository.findByDriverId(driver.getId())).thenReturn(List.of(vehicle));

        assertEquals(List.of(expectedVehicle()), service.getDriverVehicles(authentication));
    }

    @Test
    void listVehiclesReturnsEmptyListWhenDriverHasNoVehicles() {
        when(drivers.findByUserId(userId)).thenReturn(Optional.of(driver));
        when(repository.findByDriverId(driver.getId())).thenReturn(List.of());

        assertTrue(service.getDriverVehicles(authentication).isEmpty());
    }

    @Test
    void deleteVehicleRequiresOwnership() {
        when(drivers.findByUserId(userId)).thenReturn(Optional.of(driver));
        when(repository.findByIdAndDriverId(vehicle.getId(), driver.getId())).thenReturn(Optional.of(vehicle));

        service.deleteVehicle(authentication, vehicle.getId());

        verify(repository).delete(vehicle);
    }

    @Test
    void deleteMissingOrOtherDriversVehicleReturnsNotFound() {
        when(drivers.findByUserId(userId)).thenReturn(Optional.of(driver));

        assertStatus(HttpStatus.NOT_FOUND, () -> service.deleteVehicle(authentication, vehicle.getId()));

        verify(repository).findByIdAndDriverId(vehicle.getId(), driver.getId());
        verify(repository, never()).delete(any());
    }

    @Test
    void missingDriverReturnsNotFoundBeforeAccessingVehicles() {
        assertStatus(HttpStatus.NOT_FOUND, () -> service.addVehicle(authentication, request));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.getDriverVehicles(authentication));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.deleteVehicle(authentication, vehicle.getId()));
        verifyNoInteractions(repository);
    }

    private VehicleDto expectedVehicle() {
        return new VehicleDto(vehicle.getId(), "Toyota", "Corolla", 2024, "White", "34 ABC 123");
    }

    private static void assertStatus(HttpStatus status, org.junit.jupiter.api.function.Executable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action).getStatusCode());
    }
}
