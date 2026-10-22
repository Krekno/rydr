package com.krekno.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDto(@NotBlank String firstName,
                          @NotBlank String lastName,
                          @NotBlank String email,
                          @NotBlank @Size(min = 8, max = 72)String password,
                          @NotBlank String phoneNumber) {
}
