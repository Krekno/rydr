package com.krekno.tripservice.dto.event;

import java.math.BigDecimal;
import java.util.UUID;

public record TripCreatedEvent(UUID eventId,
                               UUID tripId,
                               UUID customerId,
                               BigDecimal fare,
                               Double lat,
                               Double lon
) {}