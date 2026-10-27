package com.krekno.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateVehicleRequest(@NotBlank(message = "Make cannot be empty") String make,
                                  @NotBlank(message = "Model cannot be empty") String model,
                                  @NotNull(message = "Year cannot be empty") Integer year,
                                  @NotBlank(message = "Color cannot be empty") String color,
                                  @NotBlank(message = "License plate cannot be empty") String licensePlate) {
}
