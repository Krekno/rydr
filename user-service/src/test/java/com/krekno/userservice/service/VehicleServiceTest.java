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
    @Mock VehicleRepository vehicleRepository;
    @Mock DriverRepository driverRepository;
    private VehicleService vehicleService;
    private UUID userId;
    private Driver driver;
    private Vehicle vehicle;
    private Authentication authentication;
    private final CreateVehicleRequest request = new CreateVehicleRequest(
            "Toyota",
            "Corolla",
            2024,
            "White",
            "34 abc 123"
    );

    @BeforeEach
    void setUp() {
        vehicleService = new VehicleService(vehicleRepository, driverRepository);
        userId = UUID.randomUUID();
        driver = Driver.builder().id(UUID.randomUUID()).build();
        vehicle = Vehicle.builder().id(UUID.randomUUID()).driver(driver).make("Toyota").model("Corolla")
                .year(2024).color("White").licensePlate("34 ABC 123").build();
        authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null);
    }

    @Test
    void addVehicleNormalizesPlateAndAssociatesAuthenticatedDriver() {
        when(driverRepository.findByUserId(userId)).thenReturn(Optional.of(driver));
        when(vehicleRepository.save(any())).thenAnswer(invocation -> {
            Vehicle saved = invocation.getArgument(0);
            saved.setId(vehicle.getId());
            return saved;
        });

        VehicleDto result = vehicleService.addVehicle(authentication, request);

        verify(vehicleRepository).existsByLicensePlate("34 ABC 123");
        ArgumentCaptor<Vehicle> saved = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(saved.capture());
        assertSame(driver, saved.getValue().getDriver());
        assertEquals(expectedVehicle(), result);
    }

    @Test
    void addVehicleRejectsDuplicatePlateWithoutSaving() {
        when(driverRepository.findByUserId(userId)).thenReturn(Optional.of(driver));
        when(vehicleRepository.existsByLicensePlate("34 ABC 123")).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> vehicleService.addVehicle(authentication, request));

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void listVehiclesUsesAuthenticatedDriverAndMapsAllFields() {
        when(driverRepository.findByUserId(userId)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByDriverId(driver.getId())).thenReturn(List.of(vehicle));

        assertEquals(List.of(expectedVehicle()), vehicleService.getDriverVehicles(authentication));
    }

    @Test
    void listVehiclesReturnsEmptyListWhenDriverHasNoVehicles() {
        when(driverRepository.findByUserId(userId)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByDriverId(driver.getId())).thenReturn(List.of());

        assertTrue(vehicleService.getDriverVehicles(authentication).isEmpty());
    }

    @Test
    void deleteVehicleRequiresOwnership() {
        when(driverRepository.findByUserId(userId)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdAndDriverId(vehicle.getId(), driver.getId())).thenReturn(Optional.of(vehicle));

        vehicleService.deleteVehicle(authentication, vehicle.getId());

        verify(vehicleRepository).delete(vehicle);
    }

    @Test
    void deleteMissingOrOtherDriversVehicleReturnsNotFound() {
        when(driverRepository.findByUserId(userId)).thenReturn(Optional.of(driver));

        assertStatus(HttpStatus.NOT_FOUND, () -> vehicleService.deleteVehicle(authentication, vehicle.getId()));

        verify(vehicleRepository).findByIdAndDriverId(vehicle.getId(), driver.getId());
        verify(vehicleRepository, never()).delete(any());
    }

    @Test
    void missingDriverReturnsNotFoundBeforeAccessingVehicles() {
        assertStatus(HttpStatus.NOT_FOUND, () -> vehicleService.addVehicle(authentication, request));
        assertStatus(HttpStatus.NOT_FOUND, () -> vehicleService.getDriverVehicles(authentication));
        assertStatus(HttpStatus.NOT_FOUND, () -> vehicleService.deleteVehicle(authentication, vehicle.getId()));
        verifyNoInteractions(vehicleRepository);
    }

    private VehicleDto expectedVehicle() {
        return new VehicleDto(vehicle.getId(), "Toyota", "Corolla", 2024, "White", "34 ABC 123");
    }

    private static void assertStatus(HttpStatus status, org.junit.jupiter.api.function.Executable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action).getStatusCode());
    }
}
