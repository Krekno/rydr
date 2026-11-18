package com.krekno.locationservice.controller;

import com.krekno.locationservice.dto.DriverLocationPingRequest;
import com.krekno.locationservice.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LocationWebSocketController {

    private final LocationService locationService;

    @MessageMapping("/location.ping")
    public void pingLocation(@Payload @Valid DriverLocationPingRequest request) {
        log.debug("Received WebSocket location ping from driver: {}", request.driverId());
        locationService.updateDriverLocation(request);
    }
}
