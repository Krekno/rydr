package com.krekno.userservice.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(@NotBlank String firstName,
                                @NotBlank String lastName,
                                @NotBlank String phoneNumber) {
}
