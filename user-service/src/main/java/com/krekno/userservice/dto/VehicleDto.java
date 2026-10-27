package com.krekno.userservice.dto;

import java.util.UUID;

public record VehicleDto(UUID id,
                         String make,
                         String model,
                         Integer year,
                         String color,
                         String licensePlate) {
}
