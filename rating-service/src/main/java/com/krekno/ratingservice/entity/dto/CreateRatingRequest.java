package com.krekno.ratingservice.entity.dto;

import java.util.UUID;

public record CreateRatingRequest(
        UUID tripId,
        UUID driverId,
        Integer score,
        String comment
) {
}
