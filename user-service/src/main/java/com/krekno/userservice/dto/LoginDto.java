package com.krekno.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginDto(@NotBlank String email,
                       @NotBlank @Size(min = 8, max = 72)String password) {
}
