
package com.krekno.tripservice.service;

import com.krekno.tripservice.dto.*;
import com.krekno.tripservice.dto.event.DriverStatusEvent;
import com.krekno.tripservice.dto.event.TripCompletedEvent;
import com.krekno.tripservice.dto.event.TripCreatedEvent;
import com.krekno.tripservice.entity.Trip;
import com.krekno.tripservice.entity.enums.DriverStatus;
import com.krekno.tripservice.entity.enums.TripStatus;
import com.krekno.tripservice.repository.TripRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final FareCalculator fareCalculator;
    private final OsrmService osrmService;
    private final OutboxService outboxService;

    @Transactional
    public Trip createTrip(CreateTripRequest request, Jwt jwt) {

        OsrmResponse.Route route = osrmService.calculateRoute(
                request.pickupLatitude(),
                request.pickupLongitude(),
                request.destinationLatitude(),
                request.destinationLongitude()
        );

        BigDecimal fare = fareCalculator.calculate(
                route.distance(),
                route.duration()
        );

        Trip trip = Trip.builder()
                .customerId(getUserId(jwt))
                .driverId(null)
                .pickupLatitude(request.pickupLatitude())
                .pickupLongitude(request.pickupLongitude())
                .destinationLatitude(request.destinationLatitude())
                .destinationLongitude(request.destinationLongitude())
                .fare(fare)
                .status(TripStatus.REQUESTED)
                .build();

        trip = tripRepository.save(trip);

        TripCreatedEvent event = new TripCreatedEvent(
                UUID.randomUUID(),
                trip.getId(),
                trip.getCustomerId(),
                trip.getFare(),
                trip.getPickupLatitude(),
                trip.getPickupLongitude()
        );

        outboxService.saveEvent(
                trip.getId(),
                "TripCreated",
                "trip-created",
                event
        );

        return trip;
    }

    @KafkaListener(topics = "driver-match", groupId = "DriverAssignment")
    @Transactional
    public void assignDriver(AssignDriverRequest request) {

        if (request.driverId() == null || request.tripId() == null) {
            throw new IllegalArgumentException("Invalid driver assignment");
        }

        Trip trip = tripRepository.findById(request.tripId())
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        if (trip.getStatus() != TripStatus.REQUESTED) {
            return;
        }

        trip.setDriverId(request.driverId());
        trip.setStatus(TripStatus.DRIVER_ASSIGNED);

        tripRepository.save(trip);

        publishDriverStatus(
                trip,
                DriverStatus.ON_TRIP,
                "DriverAssigned"
        );
    }

    @KafkaListener(topics = "trip-unmatched", groupId = "trip-service-unmatched")
    @Transactional
    public void markTripUnmatched(String tripId) {
        tripRepository.findById(UUID.fromString(tripId)).ifPresent(trip -> {
            if (trip.getStatus() == TripStatus.REQUESTED) {
                trip.setStatus(TripStatus.NO_DRIVER_AVAILABLE);
            }
        });
    }

    @Transactional
    public Trip arriveAtPickup(UUID tripId, Jwt jwt) {

        UUID driverId = getUserId(jwt);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        if (trip.getStatus() != TripStatus.DRIVER_ASSIGNED) {
            throw new IllegalStateException("Trip is not awaiting driver arrival");
        }

        if (!driverId.equals(trip.getDriverId())) {
            throw new SecurityException("Only the assigned driver can complete the trip");
        }

        trip.setStatus(TripStatus.DRIVER_ARRIVED);

        return trip;
    }

    @Transactional
    public Trip startTrip(UUID tripId, Jwt jwt) {

        UUID driverId = getUserId(jwt);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        if (trip.getStatus() != TripStatus.DRIVER_ARRIVED) {
            throw new IllegalStateException("Driver has not arrived");
        }

        if (!driverId.equals(trip.getDriverId())) {
            throw new SecurityException("Only the assigned driver can complete the trip");
        }

        trip.setStatus(TripStatus.IN_PROGRESS);

        return trip;
    }

    @Transactional
    public Trip cancelTrip(UUID tripId, Jwt jwt) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        UUID userId = getUserId(jwt);

        String scope = jwt.getClaimAsString("scope");
        boolean customer = userId.equals(trip.getCustomerId()) && hasRole(scope, "ROLE_USER");
        boolean driver = userId.equals(trip.getDriverId()) && hasRole(scope, "ROLE_DRIVER");
        boolean authorized = customer || driver;

        if (!authorized) {
            throw new SecurityException("Not authorized");
        }

        if (trip.getStatus() != TripStatus.REQUESTED &&
                trip.getStatus() != TripStatus.DRIVER_ASSIGNED &&
                trip.getStatus() != TripStatus.DRIVER_ARRIVED &&
                trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new IllegalStateException("Trip cannot be cancelled");
        }

        trip.setStatus(TripStatus.CANCELLED);

        if (trip.getDriverId() != null) {
            publishDriverStatus(
                    trip,
                    DriverStatus.AVAILABLE,
                    "DriverStatusChanged"
            );
        }

        return trip;
    }

    private boolean hasRole(String scope, String role) {
        return scope != null && Arrays.asList(scope.split("\\s+")).contains(role);
    }

    @Transactional
    public Trip completeTrip(UUID tripId, Jwt jwt) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        UUID driverId = getUserId(jwt);

        if (!driverId.equals(trip.getDriverId())) {
            throw new SecurityException("Only the assigned driver can complete the trip");
        }

        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new IllegalStateException("Trip is not in progress");
        }

        trip.setStatus(TripStatus.COMPLETED);

        publishDriverStatus(
                trip,
                DriverStatus.AVAILABLE,
                "DriverStatusChanged"
        );

        outboxService.saveEvent(
                trip.getId(),
                "TripCompleted",
                "trip-completed",
                new TripCompletedEvent(
                        UUID.randomUUID(),
                        trip.getId()
                )
        );

        return trip;
    }

    private UUID getUserId(Jwt jwt) {
        return UUID.fromString(
                Objects.requireNonNull(jwt.getSubject())
        );
    }

    private void publishDriverStatus(
            Trip trip,
            DriverStatus status,
            String eventType
    ) {

        DriverStatusEvent event = new DriverStatusEvent(
                UUID.randomUUID(),
                trip.getDriverId(),
                trip.getId(),
                status
        );

        outboxService.saveEvent(
                trip.getId(),
                eventType,
                "driver-status",
                event
        );
    }
}
