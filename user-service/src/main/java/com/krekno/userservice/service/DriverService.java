package com.krekno.userservice.service;

import com.krekno.userservice.dto.DriverResponseDto;
import com.krekno.userservice.dto.VehicleDto;
import com.krekno.userservice.entity.Driver;
import com.krekno.userservice.entity.User;
import com.krekno.userservice.entity.enums.DriverStatus;
import com.krekno.userservice.entity.enums.Role;
import com.krekno.userservice.repository.DriverRepository;
import com.krekno.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;

    @Transactional
    public DriverResponseDto applyAsDriver(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (driverRepository.existsByUserId(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Driver application already exists");
        }

        user.setRole(Role.ROLE_DRIVER);
        userRepository.save(user);

        Driver driver = Driver.builder()
                .user(user)
                .status(DriverStatus.OFFLINE)
                .isVerified(false)
                .ratingAverage(5.0f)
                .ratingCount(0)
                .vehicles(Collections.emptyList())
                .build();

        driver = driverRepository.save(driver);
        return mapToDto(driver);
    }

    @Transactional(readOnly = true)
    public DriverResponseDto getMyDriverProfile(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver profile not found"));
        return mapToDto(driver);
    }

    @Transactional
    public DriverResponseDto updateStatus(Authentication authentication, DriverStatus newStatus) {
        UUID userId = UUID.fromString(authentication.getName());
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver profile not found"));

        if (newStatus != DriverStatus.OFFLINE && Boolean.FALSE.equals(driver.getIsVerified())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver must be verified to go online");
        }

        driver.setStatus(newStatus);
        driverRepository.save(driver);
        return mapToDto(driver);
    }

    @Transactional(readOnly = true)
    public DriverResponseDto getDriverById(UUID driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
        return mapToDto(driver);
    }

    @Transactional
    public DriverResponseDto updateRating(UUID driverId, int newRating) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));

        float currentAvg = driver.getRatingAverage() != null ? driver.getRatingAverage() : 5.0f;
        int currentCount = driver.getRatingCount() != null ? driver.getRatingCount() : 0;

        float newAvg = ((currentAvg * currentCount) + newRating) / (currentCount + 1);
        driver.setRatingAverage(newAvg);
        driver.setRatingCount(currentCount + 1);
        driverRepository.save(driver);

        return mapToDto(driver);
    }

    @Transactional
    public DriverResponseDto setDriverStatusInternal(UUID driverId, DriverStatus newStatus) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
        driver.setStatus(newStatus);
        driverRepository.save(driver);
        return mapToDto(driver);
    }

    @Transactional(readOnly = true)
    public List<DriverResponseDto> getAllDrivers(Boolean verified) {
        List<Driver> drivers = (verified != null)
                ? driverRepository.findByIsVerified(verified)
                : driverRepository.findAll();

        return drivers.stream().map(this::mapToDto).toList();
    }

    @Transactional
    public DriverResponseDto verifyDriver(UUID driverId, boolean verified) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
        driver.setIsVerified(verified);
        driverRepository.save(driver);
        return mapToDto(driver);
    }

    public DriverResponseDto mapToDto(Driver driver) {
        List<VehicleDto> vehicles = (driver.getVehicles() != null)
                ? driver.getVehicles().stream()
                .map(v -> new VehicleDto(v.getId(), v.getMake(), v.getModel(), v.getYear(), v.getColor(), v.getLicensePlate()))
                .toList()
                : Collections.emptyList();

        User user = driver.getUser();
        return new DriverResponseDto(
                driver.getId(),
                user != null ? user.getId() : null,
                user != null ? user.getFirstName() : null,
                user != null ? user.getLastName() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getPhoneNumber() : null,
                driver.getStatus(),
                driver.getRatingAverage(),
                driver.getRatingCount(),
                driver.getIsVerified(),
                driver.getCreatedAt(),
                vehicles
        );
    }
}
