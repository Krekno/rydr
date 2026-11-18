package com.krekno.tripservice.dto.event;

import java.util.UUID;

public record DriverAssignedEvent(UUID eventId,
                                  UUID driverId) {
}
