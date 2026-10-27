package com.krekno.locationservice.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record NearbyDriverResponse(
        UUID driverId,
        Double latitude,
        Double longitude,
        Double distanceKm,
        Float bearing,
        Double speed,
        Instant lastSeen
) {}

