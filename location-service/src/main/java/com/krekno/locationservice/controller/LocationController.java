package com.krekno.locationservice.controller;

import com.krekno.locationservice.dto.DriverLocationDetails;
import com.krekno.locationservice.dto.DriverLocationPingRequest;
import com.krekno.locationservice.dto.NearbyDriverResponse;
import com.krekno.locationservice.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping("/drivers/ping")
    public ResponseEntity<Void> pingLocation(@RequestBody @Valid DriverLocationPingRequest request, @AuthenticationPrincipal Jwt jwt) {
        if (!request.driverId().toString().equals(jwt.getSubject())) return ResponseEntity.status(403).build();
        locationService.updateDriverLocation(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/drivers/nearby")
    public ResponseEntity<List<NearbyDriverResponse>> getNearbyDrivers(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "5.0") double radiusKm,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(locationService.getNearbyDrivers(lat, lon, radiusKm, limit));
    }

    @GetMapping("/drivers/{driverId}")
    public ResponseEntity<DriverLocationDetails> getDriverLocation(@PathVariable UUID driverId) {
        return locationService.getDriverLocation(driverId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/drivers/{driverId}")
    public ResponseEntity<Void> setDriverOffline(@PathVariable UUID driverId) {
        locationService.setDriverOffline(driverId);
        return ResponseEntity.noContent().build();
    }
}

