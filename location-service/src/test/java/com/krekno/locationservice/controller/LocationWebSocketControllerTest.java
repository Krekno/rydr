package com.krekno.locationservice.controller;

import com.krekno.locationservice.dto.DriverLocationPingRequest;
import com.krekno.locationservice.service.LocationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocationWebSocketControllerTest {

    @Mock
    private LocationService locationService;

    @InjectMocks
    private LocationWebSocketController controller;

    @Test
    void pingLocation_delegatesToLocationService() {
        UUID driverId = UUID.randomUUID();
        DriverLocationPingRequest request = DriverLocationPingRequest.builder()
                .driverId(driverId)
                .latitude(41.01)
                .longitude(28.97)
                .bearing(45.0f)
                .speed(30.0)
                .build();

        controller.pingLocation(request);

        verify(locationService).updateDriverLocation(request);
    }
}
