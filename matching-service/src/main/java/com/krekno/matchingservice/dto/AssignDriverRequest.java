package com.krekno.matchingservice.dto;

import java.util.UUID;

public record AssignDriverRequest(UUID tripId,
                                  UUID driverId,
                                  Double duration) {
}
