package com.krekno.tripservice.repository;

import com.krekno.tripservice.entity.Trip;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface TripRepository extends JpaRepository<Trip, UUID> {
    Optional<Trip> findById(UUID id);
}
