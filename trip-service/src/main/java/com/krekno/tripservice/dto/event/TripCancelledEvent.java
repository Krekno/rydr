package com.krekno.tripservice.dto.event;

import java.util.UUID;

public record TripCancelledEvent(UUID id,
                                 UUID tripId) {
}
