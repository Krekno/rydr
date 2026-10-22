package com.krekno.userservice.entity;

import com.krekno.userservice.entity.enums.DriverStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @JoinColumn(name = "user_id", nullable = false)
    @OneToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(name = "license_plate", nullable = false)
    @NotBlank(message = "license plate can't be empty")
    private String licensePlate;

    @Column(name = "status", nullable = false)
    private DriverStatus status;

    @Column(name = "rating_average", nullable = false)
    private Float ratingAverage;

    @Column(name = "rating_count", nullable = false)
    private Integer ratingCount;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
