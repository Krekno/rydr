package com.krekno.userservice.repository;

import com.krekno.userservice.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverRepository extends JpaRepository<Driver, UUID> {
    Optional<Driver> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    List<Driver> findByIsVerified(Boolean isVerified);
}
