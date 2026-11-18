package com.krekno.userservice.dto.event;

import com.krekno.userservice.entity.enums.DriverStatus;

import java.util.UUID;

public record DriverStatusEvent(UUID eventId,
                                UUID driverId,
                                UUID tripId,
                                DriverStatus status) {
}
