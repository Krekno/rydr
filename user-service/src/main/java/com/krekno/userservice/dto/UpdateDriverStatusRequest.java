package com.krekno.userservice.dto;

import com.krekno.userservice.entity.enums.DriverStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDriverStatusRequest(@NotNull(message = "Status cannot be null") DriverStatus status) {
}
