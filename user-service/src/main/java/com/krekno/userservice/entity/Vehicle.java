package com.krekno.userservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JoinColumn(name = "driver_id", nullable = false)
    @OneToOne(fetch = FetchType.LAZY)
    private Driver driver;

    @Column(name = "make", nullable = false)
    @NotBlank(message = "make can't be empty")
    private String make;

    @Column(name = "model", nullable = false)
    @NotBlank(message = "model can't be empty")
    private String model;

    @Column(name = "year", nullable = false)
    @NotNull(message = "year can't be empty")
    private Integer year;

    @Column(name = "color", nullable = false)
    @NotBlank(message = "color can't be empty")
    private String color;

    @Column(name = "license_plate", nullable = false)
    @NotBlank(message = "license plate can't be empty")
    private String licensePlate;

}
