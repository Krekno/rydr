package com.krekno.tripservice.service;

import com.krekno.tripservice.dto.*;
import com.krekno.tripservice.dto.event.DriverStatusEvent;
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

        BigDecimal fare = fareCalculator.calculate(route.distance(), route.duration());

        Trip trip = Trip.builder()

                .customerId(UUID.fromString(Objects.requireNonNull(jwt.getSubject())))
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
                trip.getFare()
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
        Trip trip = tripRepository.findById(request.tripId()).orElseThrow();

        if (trip.getStatus() != TripStatus.REQUESTED) {
            return;
        }

        if (trip.getDriverId() != null) {
            return;
        }

        trip.setDriverId(request.driverId());
        trip.setStatus(TripStatus.IN_PROGRESS);

        tripRepository.save(trip);

        DriverStatusEvent event = new DriverStatusEvent(UUID.randomUUID(), request.driverId(), request.tripId(), DriverStatus.ON_TRIP);

        outboxService.saveEvent(
                trip.getId(),
                "DriverAssigned",
                "driver-status",
                event
        );
    }
}
