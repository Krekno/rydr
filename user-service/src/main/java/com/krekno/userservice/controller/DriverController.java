package com.krekno.userservice.controller;

import com.krekno.userservice.dto.*;
import com.krekno.userservice.service.DriverService;
import com.krekno.userservice.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;
    private final VehicleService vehicleService;

    @PostMapping("/apply")
    public ResponseEntity<DriverResponseDto> applyAsDriver(Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(driverService.applyAsDriver(authentication));
    }

    @GetMapping("/me")
    public ResponseEntity<DriverResponseDto> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(driverService.getMyDriverProfile(authentication));
    }

    @PatchMapping("/me/status")
    public ResponseEntity<DriverResponseDto> updateStatus(Authentication authentication,
                                                         @RequestBody @Valid UpdateDriverStatusRequest request) {
        return ResponseEntity.ok(driverService.updateStatus(authentication, request.status()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverResponseDto> getDriverById(@PathVariable UUID id) {
        return ResponseEntity.ok(driverService.getDriverById(id));
    }

    @PostMapping("/me/vehicles")
    public ResponseEntity<VehicleDto> addVehicle(Authentication authentication,
                                                 @RequestBody @Valid CreateVehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.addVehicle(authentication, request));
    }

    @GetMapping("/me/vehicles")
    public ResponseEntity<List<VehicleDto>> getVehicles(Authentication authentication) {
        return ResponseEntity.ok(vehicleService.getDriverVehicles(authentication));
    }

    @DeleteMapping("/me/vehicles/{vehicleId}")
    public ResponseEntity<Void> deleteVehicle(Authentication authentication,
                                              @PathVariable UUID vehicleId) {
        vehicleService.deleteVehicle(authentication, vehicleId);
        return ResponseEntity.noContent().build();
    }
}
