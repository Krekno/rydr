package com.krekno.userservice.entity;

import com.krekno.userservice.entity.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "first_name", nullable = false)
    @NotBlank(message = "first name can't be empty")
    private String firstName;

    @Column(name = "last_name", nullable = false)
    @NotBlank(message = "surname can't be empty")
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    @NotBlank(message = "email can't be empty")
    private String email;

    @Column(name = "password", nullable = false)
    @NotBlank(message = "password can't be empty")
    private String password;

    @Column(name = "phone_number", nullable = false)
    @NotBlank(message = "phone can't be empty")
    private String phoneNumber;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}
