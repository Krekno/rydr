package com.krekno.userservice.dto;

import com.krekno.userservice.entity.enums.DriverStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DriverResponseDto(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        DriverStatus status,
        Float ratingAverage,
        Integer ratingCount,
        Boolean isVerified,
        Instant createdAt,
        List<VehicleDto> vehicles
) {
}
