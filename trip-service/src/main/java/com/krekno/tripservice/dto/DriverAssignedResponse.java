package com.krekno.tripservice.dto;

import java.util.UUID;

public record DriverAssignedResponse(UUID driverId,
                                     String driverName) {
}
