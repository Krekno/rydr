package com.krekno.locationservice.dto;

import lombok.Builder;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Builder
public record DriverLocationDetails(
        UUID driverId,
        Double latitude,
        Double longitude,
        Float bearing,
        Double speed,
        UUID activeTripId,
        Instant lastSeen
) implements Serializable {}

