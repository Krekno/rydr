package com.krekno.matchingservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krekno.matchingservice.client.LocationClient;
import com.krekno.matchingservice.dto.AssignDriverRequest;
import com.krekno.matchingservice.dto.NearbyDriverResponse;
import com.krekno.matchingservice.dto.OsrmResponse;
import com.krekno.matchingservice.dto.event.TripCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock LocationClient locationClient;
    @Mock OsrmService osrmService;
    @Mock KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MatchingService service;

    @BeforeEach
    void setUp() {
        service = new MatchingService(locationClient, osrmService, kafkaTemplate, objectMapper);
        lenient().when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    private TripCreatedEvent event() {
        return new TripCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.TEN, 41.0, 29.0);
    }

    private NearbyDriverResponse driver(UUID id, double lat) {
        return new NearbyDriverResponse(id, lat, 29.0, 1.0, 0.0f, 0.0, Instant.now());
    }

    @Test
    void matchDriver_selectsFastestRouteAndPublishesAssignment() throws Exception {
        TripCreatedEvent event = event();
        UUID slowerDriverId = UUID.randomUUID();
        UUID fasterDriverId = UUID.randomUUID();
        NearbyDriverResponse slower = driver(slowerDriverId, 40.0);
        NearbyDriverResponse faster = driver(fasterDriverId, 41.1);
        when(locationClient.getNearbyDrivers(41.0, 29.0, 5.0, 10)).thenReturn(List.of(slower, faster));
        when(osrmService.calculateRoute(40.0, 29.0, 41.0, 29.0))
                .thenReturn(new OsrmResponse.Route(3000, 300));
        when(osrmService.calculateRoute(41.1, 29.0, 41.0, 29.0))
                .thenReturn(new OsrmResponse.Route(1000, 100));

        service.matchDriver(event);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("driver-match"), eq(event.eventId().toString()), payload.capture());
        AssignDriverRequest assignment = objectMapper.readValue(payload.getValue(), AssignDriverRequest.class);
        assertEquals(event.tripId(), assignment.tripId());
        assertEquals(fasterDriverId, assignment.driverId());
        assertEquals(100.0, assignment.duration());
        verify(kafkaTemplate, never()).send(eq("trip-unmatched"), anyString(), anyString());
    }

    @Test
    void matchDriver_whenNoNearbyDriversPublishesUnmatchedTrip() throws Exception {
        TripCreatedEvent event = event();
        when(locationClient.getNearbyDrivers(41.0, 29.0, 5.0, 10)).thenReturn(List.of());

        service.matchDriver(event);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("trip-unmatched"), eq(event.tripId().toString()), payload.capture());
        assertEquals(event.tripId(), objectMapper.readValue(payload.getValue(), UUID.class));
        verifyNoInteractions(osrmService);
    }

    @Test
    void matchDriver_whenLocationServiceReturnsNullPublishesUnmatchedTrip() {
        TripCreatedEvent event = event();
        when(locationClient.getNearbyDrivers(41.0, 29.0, 5.0, 10)).thenReturn(null);

        service.matchDriver(event);

        verify(kafkaTemplate).send(eq("trip-unmatched"), eq(event.tripId().toString()), anyString());
        verifyNoInteractions(osrmService);
    }

    @Test
    void matchDriver_whenAllRoutesFailPublishesUnmatchedTrip() {
        TripCreatedEvent event = event();
        NearbyDriverResponse driver = driver(UUID.randomUUID(), 41.1);
        when(locationClient.getNearbyDrivers(41.0, 29.0, 5.0, 10)).thenReturn(List.of(driver));
        when(osrmService.calculateRoute(41.1, 29.0, 41.0, 29.0)).thenThrow(new IllegalStateException("OSRM unavailable"));

        service.matchDriver(event);

        verify(kafkaTemplate).send(eq("trip-unmatched"), eq(event.tripId().toString()), anyString());
        verify(kafkaTemplate, never()).send(eq("driver-match"), anyString(), anyString());
    }
}
