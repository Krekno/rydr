package com.krekno.tripservice.controller;

import com.krekno.tripservice.dto.CreateTripRequest;
import com.krekno.tripservice.entity.Trip;
import com.krekno.tripservice.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/trip")
public class TripController {

    private final TripService tripService;

    @PostMapping()
    public ResponseEntity<Trip> createTrip(
            @RequestBody CreateTripRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(tripService.createTrip(request, jwt));
    }

    @PostMapping("/arrived/{id}")
    public ResponseEntity<Trip> arriveAtPickup(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(tripService.arriveAtPickup(id, jwt));
    }

    @PostMapping("/start/{id}")
    public ResponseEntity<Trip> startTrip(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(tripService.startTrip(id, jwt));
    }

    @PostMapping("/cancel/{id}")
    public ResponseEntity<Trip> cancelTrip(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(tripService.cancelTrip(id, jwt));
    }

    @PostMapping("/complete/{id}")
    public ResponseEntity<Trip> completeTrip(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(tripService.completeTrip(id, jwt));
    }

}
