package com.krekno.tripservice.dto.event;

import java.util.UUID;

public record TripCompletedEvent(UUID id,
                                 UUID tripId) {
}
