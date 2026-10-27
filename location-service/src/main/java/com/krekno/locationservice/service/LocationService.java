package com.krekno.locationservice.service;

import com.krekno.locationservice.config.KafkaProducerConfig;
import com.krekno.locationservice.dto.DriverLocationDetails;
import com.krekno.locationservice.dto.DriverLocationEvent;
import com.krekno.locationservice.dto.DriverLocationPingRequest;
import com.krekno.locationservice.dto.NearbyDriverResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
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

    @Value("${location.geo-key:rydr:drivers:geo}")
    private String geoKey;

    @Value("${location.meta-prefix:rydr:drivers:meta:}")
    private String metaPrefix;

    @Value("${location.heartbeat-timeout-seconds:60}")
    private long heartbeatTimeoutSeconds;

    public void updateDriverLocation(DriverLocationPingRequest request) {
        String driverIdStr = request.driverId().toString();
        Instant now = Instant.now();

        // 1. Update Redis GEO coordinates (Longitude first, then Latitude in Redis)
        Point point = new Point(request.longitude(), request.latitude());
        redisTemplate.opsForGeo().add(geoKey, point, driverIdStr);

        // 2. Store metadata (bearing, speed, trip, lastSeen) with TTL heartbeat
        DriverLocationDetails details = DriverLocationDetails.builder()
                .driverId(request.driverId())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .bearing(request.bearing() != null ? request.bearing() : 0.0f)
                .speed(request.speed() != null ? request.speed() : 0.0)
                .activeTripId(request.activeTripId())
                .lastSeen(now)
                .build();

        String metaKey = metaPrefix + driverIdStr;
        redisTemplate.opsForValue().set(metaKey, details, Duration.ofSeconds(heartbeatTimeoutSeconds));

        // 3. Publish location update to Kafka for downstream consumers
        DriverLocationEvent event = DriverLocationEvent.builder()
                .driverId(request.driverId())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .bearing(details.bearing())
                .speed(details.speed())
                .activeTripId(request.activeTripId())
                .timestamp(now)
                .build();

        kafkaTemplate.send(KafkaProducerConfig.DRIVER_LOCATION_TOPIC, driverIdStr, event);
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

            Object metaObj = redisTemplate.opsForValue().get(metaKey);
            if (metaObj instanceof DriverLocationDetails(
                    UUID driverId, Double latitude1, Double longitude1, Float bearing, Double speed, UUID activeTripId,
                    Instant lastSeen
            )) {
                // If driver is currently on an active trip, exclude from available nearby pool
                if (activeTripId == null) {

                    NearbyDriverResponse nearbyDriverResponse = new NearbyDriverResponse(driverId,
                            latitude1,
                            longitude1,
                            result.getDistance().getValue(),
                            bearing,
                            speed,
                            lastSeen);

                    nearbyDrivers.add(nearbyDriverResponse);
                }
            } else {
                // Heartbeat expired, clean up stale GEO index entry
                redisTemplate.opsForZSet().remove(geoKey, driverIdStr);
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

        // Fallback: check Redis GEO coordinates
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
}

