package com.krekno.tripservice.dto.event;

import com.krekno.tripservice.entity.enums.DriverStatus;

import java.util.UUID;

public record DriverStatusEvent(UUID eventId,
                                UUID driverId,
                                UUID tripId,
                                DriverStatus status) {
}
