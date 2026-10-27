package com.krekno.userservice.dto;

import java.util.UUID;

public record UserDto(UUID id,
                      String firstName,
                      String lastName,
                      String email,
                      String phoneNumber,
                      String role) {

    public UserDto(String firstName, String lastName, String email, String phoneNumber, String role) {
        this(null, firstName, lastName, email, phoneNumber, role);
    }
}
