package com.krekno.ratingservice.entity.dto;

import java.util.UUID;

public record UpdateRatingRequest(
        UUID ratingId,
        Integer score,
        String comment
) {
}
