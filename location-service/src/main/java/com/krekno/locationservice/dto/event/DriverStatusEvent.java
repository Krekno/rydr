package com.krekno.locationservice.dto.event;

import com.krekno.locationservice.dto.enums.DriverStatus;

import java.util.UUID;

public record DriverStatusEvent(UUID eventId,
                                UUID driverId,
                                UUID tripId,
                                DriverStatus status) {
}
