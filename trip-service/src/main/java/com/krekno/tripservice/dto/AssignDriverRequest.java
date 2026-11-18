package com.krekno.tripservice.dto;

import java.util.UUID;

public record AssignDriverRequest(UUID tripId,
                                  UUID driverId,
                                  Double driverLatitude,
                                  Double driverLongitude) {
}
