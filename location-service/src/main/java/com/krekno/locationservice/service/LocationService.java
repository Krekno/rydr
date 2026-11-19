package com.krekno.locationservice.service;

import com.krekno.locationservice.config.KafkaProducerConfig;
import com.krekno.locationservice.dto.DriverLocationDetails;
import com.krekno.locationservice.dto.DriverLocationEvent;
import com.krekno.locationservice.dto.DriverLocationPingRequest;
import com.krekno.locationservice.dto.NearbyDriverResponse;
import com.krekno.locationservice.dto.event.DriverStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${location.geo-key:rydr:drivers:geo}")
    private String geoKey;

    @Value("${location.meta-prefix:rydr:drivers:meta:}")
    private String metaPrefix;

    @Value("${location.heartbeat-timeout-seconds:60}")
    private long heartbeatTimeoutSeconds;

    public void updateDriverLocation(DriverLocationPingRequest request) {
        String driverIdStr = request.driverId().toString();
        Instant now = Instant.now();

        Point point = new Point(request.longitude(), request.latitude());
        redisTemplate.opsForGeo().add(geoKey, point, driverIdStr);

        DriverLocationDetails details = new DriverLocationDetails(
                request.driverId(),
                request.latitude(),
                request.longitude(),
                request.bearing() != null ? request.bearing() : 0.0f,
                request.speed() != null ? request.speed() : 0.0,
                request.activeTripId(),
                now);

        String metaKey = metaPrefix + driverIdStr;
        redisTemplate.opsForValue().set(metaKey, details, Duration.ofSeconds(heartbeatTimeoutSeconds));

        DriverLocationEvent event = new DriverLocationEvent(request.driverId(),
                request.latitude(),
                request.longitude(),
                details.bearing(),
                details.speed(),
                request.activeTripId(),
                now);

        kafkaTemplate.send(KafkaProducerConfig.DRIVER_LOCATION_TOPIC, driverIdStr, event);

        messagingTemplate.convertAndSend("/topic/drivers/" + driverIdStr, event);

        if (request.activeTripId() != null) {
            messagingTemplate.convertAndSend("/topic/trips/" + request.activeTripId() + "/driver-location", event);
        }

        log.debug("Updated location for driver: {} (lat: {}, lon: {})", driverIdStr, request.latitude(), request.longitude());
    }

    public List<NearbyDriverResponse> getNearbyDrivers(double latitude, double longitude, double radiusKm, int limit) {
        Point center = new Point(longitude, latitude);
        Distance distance = new Distance(radiusKm, Metrics.KILOMETERS);
        Circle circle = new Circle(center, distance);

        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeCoordinates()
                .includeDistance()
                .sortAscending()
                .limit(limit);

        GeoResults<RedisGeoCommands.GeoLocation<Object>> results = redisTemplate.opsForGeo().radius(geoKey, circle, args);
        if (results == null || results.getContent().isEmpty()) {
            return Collections.emptyList();
        }

        List<NearbyDriverResponse> nearbyDrivers = new ArrayList<>();

        for (GeoResult<RedisGeoCommands.GeoLocation<Object>> result : results) {
            String driverIdStr = result.getContent().getName().toString();
            String metaKey = metaPrefix + driverIdStr;

            DriverLocationDetails details = (DriverLocationDetails) Objects.requireNonNull(redisTemplate.opsForValue().get(metaKey));

            if (details.activeTripId() == null) {
                NearbyDriverResponse nearbyDriverResponse = new NearbyDriverResponse(
                        details.driverId(),
                        details.latitude(),
                        details.longitude(),
                        result.getDistance().getValue(),
                        details.bearing(),
                        details.speed(),
                        details.lastSeen());

                nearbyDrivers.add(nearbyDriverResponse);
            }
        }

        return nearbyDrivers;
    }

    public Optional<DriverLocationDetails> getDriverLocation(UUID driverId) {
        String metaKey = metaPrefix + driverId.toString();
        Object metaObj = redisTemplate.opsForValue().get(metaKey);
        if (metaObj instanceof DriverLocationDetails details) {
            return Optional.of(details);
        }

        List<Point> positions = redisTemplate.opsForGeo().position(geoKey, driverId.toString());
        if (positions != null && !positions.isEmpty() && positions.getFirst() != null) {
            Point p = positions.getFirst();
            return Optional.of(DriverLocationDetails.builder()
                    .driverId(driverId)
                    .latitude(p.getY())
                    .longitude(p.getX())
                    .bearing(0.0f)
                    .speed(0.0)
                    .lastSeen(Instant.now())
                    .build());
        }

        return Optional.empty();
    }

    public void setDriverOffline(UUID driverId) {
        String driverIdStr = driverId.toString();
        redisTemplate.opsForZSet().remove(geoKey, driverIdStr);
        redisTemplate.delete(metaPrefix + driverIdStr);
        log.info("Driver {} is now OFFLINE and removed from location index", driverIdStr);
    }

    @KafkaListener(topics = "driver-status", groupId = "location-service-driver-status-updater")
    public void setDriverActiveTrip(DriverStatusEvent event) {
        DriverLocationDetails details =
                (DriverLocationDetails) Objects.requireNonNull(redisTemplate.opsForValue().get(metaPrefix + event.driverId()));

        DriverLocationDetails updated = new DriverLocationDetails(
                details.driverId(),
                details.latitude(),
                details.longitude(),
                details.bearing(),
                details.speed(),
                event.tripId(),
                details.lastSeen()
        );

        redisTemplate.opsForValue().set(metaPrefix + event.driverId(), updated);
    }
}

