package com.krekno.locationservice.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record DriverLocationEvent(
        UUID driverId,
        Double latitude,
        Double longitude,
        Float bearing,
        Double speed,
        UUID activeTripId,
        Instant timestamp
) {}

