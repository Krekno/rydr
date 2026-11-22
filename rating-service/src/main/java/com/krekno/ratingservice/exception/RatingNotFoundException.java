package com.krekno.ratingservice.exception;


import java.util.UUID;

public class RatingNotFoundException extends RuntimeException {

    public RatingNotFoundException(UUID ratingId) {
        super("Rating not found: " + ratingId);
    }
}
