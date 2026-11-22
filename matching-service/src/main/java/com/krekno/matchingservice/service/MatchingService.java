
package com.krekno.matchingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krekno.matchingservice.client.LocationClient;
import com.krekno.matchingservice.dto.AssignDriverRequest;
import com.krekno.matchingservice.dto.NearbyDriverResponse;
import com.krekno.matchingservice.dto.OsrmResponse;
import com.krekno.matchingservice.dto.event.TripCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

    private final LocationClient locationClient;
    private final OsrmService osrmService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "trip-created", groupId = "driver-matching")
    public void matchDriver(TripCreatedEvent event) {

        List<NearbyDriverResponse> nearbyDrivers =
                locationClient.getNearbyDrivers(
                        event.lat(),
                        event.lon(),
                        5,
                        10
                );

        if (nearbyDrivers == null || nearbyDrivers.isEmpty()) {
            log.warn("No nearby drivers found for trip {}", event.tripId());
            publishUnmatched(event);
            return;
        }

        record Candidate(
                NearbyDriverResponse driver,
                OsrmResponse.Route route
        ) {}

        List<Candidate> candidates = nearbyDrivers.stream()
                .map(driver -> {
                    try {
                        OsrmResponse.Route route = osrmService.calculateRoute(
                                driver.latitude(),
                                driver.longitude(),
                                event.lat(),
                                event.lon()
                        );

                        return route == null ? null : new Candidate(driver, route);

                    } catch (Exception e) {
                        log.warn(
                                "Failed to calculate route for driver {}",
                                driver.driverId(),
                                e
                        );
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        Candidate fastest = candidates.stream()
                .min(Comparator.comparingDouble(
                        candidate -> candidate.route().duration()
                ))
                .orElse(null);

        if (fastest == null) {
            log.warn("No valid routes found for trip {}", event.tripId());
            publishUnmatched(event);
            return;
        }

        AssignDriverRequest request = new AssignDriverRequest(
                event.tripId(),
                fastest.driver().driverId(),
                fastest.route().duration()
        );

        try {
            String payload = objectMapper.writeValueAsString(request);

            kafkaTemplate.send(
                    "driver-match",
                    event.eventId().toString(),
                    payload
            );

            log.info(
                    "Driver {} selected for trip {} with ETA {} seconds",
                    fastest.driver().driverId(),
                    event.tripId(),
                    fastest.route().duration()
            );

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize driver assignment",
                    e
            );
        }
    }

    private void publishUnmatched(TripCreatedEvent event) {
        try {
            kafkaTemplate.send("trip-unmatched", event.tripId().toString(),
                    objectMapper.writeValueAsString(event.tripId()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize unmatched trip", e);
        }
    }
}
