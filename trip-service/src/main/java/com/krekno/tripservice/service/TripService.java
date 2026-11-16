package com.krekno.tripservice.service;

import com.krekno.tripservice.dto.TripDto;
import com.krekno.tripservice.entity.Trip;
import com.krekno.tripservice.entity.enums.TripStatus;
import com.krekno.tripservice.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final FareCalculator fareCalculator;



    public Trip newTrip(TripDto tripDto, Jwt jwt) {
        Trip newTrip = Trip.builder()
                .customerId(UUID.fromString(Objects.requireNonNull(jwt.getSubject())))
                .status(TripStatus.REQUESTED)
                .pickupLatitude(tripDto.pickupLatitude())
                .pickupLongitude(tripDto.pickupLongitude())
                .destinationLatitude(tripDto.destinationLatitude())
                .destinationLongitude(tripDto.destinationLongitude())
                .fare()
                .build();

        return tripRepository.save(newTrip);
    }

}
