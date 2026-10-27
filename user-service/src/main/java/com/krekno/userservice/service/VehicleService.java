package com.krekno.userservice.service;

import com.krekno.userservice.dto.CreateVehicleRequest;
import com.krekno.userservice.dto.VehicleDto;
import com.krekno.userservice.entity.Driver;
import com.krekno.userservice.entity.Vehicle;
import com.krekno.userservice.repository.DriverRepository;
import com.krekno.userservice.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    @Transactional
    public VehicleDto addVehicle(Authentication authentication, CreateVehicleRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver profile not found"));

        if (vehicleRepository.existsByLicensePlate(request.licensePlate().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vehicle with this license plate already exists");
        }

        Vehicle vehicle = Vehicle.builder()
                .driver(driver)
                .make(request.make())
                .model(request.model())
                .year(request.year())
                .color(request.color())
                .licensePlate(request.licensePlate().toUpperCase())
                .build();

        vehicle = vehicleRepository.save(vehicle);
        return mapToDto(vehicle);
    }

    @Transactional(readOnly = true)
    public List<VehicleDto> getDriverVehicles(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver profile not found"));

        return vehicleRepository.findByDriverId(driver.getId())
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public void deleteVehicle(Authentication authentication, UUID vehicleId) {
        UUID userId = UUID.fromString(authentication.getName());
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver profile not found"));

        Vehicle vehicle = vehicleRepository.findByIdAndDriverId(vehicleId, driver.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found for this driver"));

        vehicleRepository.delete(vehicle);
    }

    private VehicleDto mapToDto(Vehicle vehicle) {
        return new VehicleDto(
                vehicle.getId(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getColor(),
                vehicle.getLicensePlate()
        );
    }
}
