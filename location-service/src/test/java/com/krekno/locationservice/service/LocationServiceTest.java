package com.krekno.locationservice.service;

import com.krekno.locationservice.config.KafkaProducerConfig;
import com.krekno.locationservice.dto.DriverLocationDetails;
import com.krekno.locationservice.dto.DriverLocationEvent;
import com.krekno.locationservice.dto.DriverLocationPingRequest;
import com.krekno.locationservice.dto.NearbyDriverResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    private static final String GEO_KEY = "test:geo";
    private static final String META_PREFIX = "test:meta:";
    private static final long TIMEOUT_SECONDS = 45;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private GeoOperations<String, Object> geoOps;
    @Mock
    private ValueOperations<String, Object> valueOps;
    @Mock
    private ZSetOperations<String, Object> zSetOps;

    private LocationService service;

    @BeforeEach
    void setUp() {
        service = new LocationService(redisTemplate, kafkaTemplate);
        ReflectionTestUtils.setField(service, "geoKey", GEO_KEY);
        ReflectionTestUtils.setField(service, "metaPrefix", META_PREFIX);
        ReflectionTestUtils.setField(service, "heartbeatTimeoutSeconds", TIMEOUT_SECONDS);

        lenient().when(redisTemplate.opsForGeo()).thenReturn(geoOps);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
    }

    private static DriverLocationDetails details(UUID id, UUID tripId) {
        return new DriverLocationDetails(id, 41.0, 29.0, 90.0f, 30.0, tripId, Instant.parse("2026-01-01T10:00:00Z"));
    }

    private static GeoResult<RedisGeoCommands.GeoLocation<Object>> geoResult(UUID id, double distanceKm) {
        return new GeoResult<>(
                new RedisGeoCommands.GeoLocation<>(id.toString(), new Point(29.0, 41.0)),
                new Distance(distanceKm, Metrics.KILOMETERS));
    }

    // ---------- updateDriverLocation ----------

    @Test
    void updateDriverLocation_storesGeoMetaAndPublishesEvent() {
        UUID driverId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        DriverLocationPingRequest request = DriverLocationPingRequest.builder()
                .driverId(driverId)
                .latitude(41.0)
                .longitude(29.0)
                .bearing(120.0f)
                .speed(55.0)
                .activeTripId(tripId)
                .build();

        service.updateDriverLocation(request);

        ArgumentCaptor<Point> pointCaptor = ArgumentCaptor.forClass(Point.class);
        verify(geoOps).add(eq(GEO_KEY), pointCaptor.capture(), eq(driverId.toString()));
        assertEquals(29.0, pointCaptor.getValue().getX());
        assertEquals(41.0, pointCaptor.getValue().getY());

        ArgumentCaptor<Object> metaCaptor = ArgumentCaptor.forClass(Object.class);
        verify(valueOps).set(eq(META_PREFIX + driverId), metaCaptor.capture(), eq(Duration.ofSeconds(TIMEOUT_SECONDS)));
        DriverLocationDetails stored = (DriverLocationDetails) metaCaptor.getValue();
        assertEquals(driverId, stored.driverId());
        assertEquals(41.0, stored.latitude());
        assertEquals(29.0, stored.longitude());
        assertEquals(120.0f, stored.bearing());
        assertEquals(55.0, stored.speed());
        assertEquals(tripId, stored.activeTripId());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(KafkaProducerConfig.DRIVER_LOCATION_TOPIC), eq(driverId.toString()), eventCaptor.capture());
        DriverLocationEvent event = (DriverLocationEvent) eventCaptor.getValue();
        assertEquals(driverId, event.driverId());
        assertEquals(41.0, event.latitude());
        assertEquals(29.0, event.longitude());
        assertEquals(120.0f, event.bearing());
        assertEquals(55.0, event.speed());
        assertEquals(tripId, event.activeTripId());
        assertEquals(stored.lastSeen(), event.timestamp());
    }

    @Test
    void updateDriverLocation_defaultsBearingAndSpeedToZeroWhenNull() {
        UUID driverId = UUID.randomUUID();
        DriverLocationPingRequest request = DriverLocationPingRequest.builder()
                .driverId(driverId)
                .latitude(10.0)
                .longitude(20.0)
                .build();

        service.updateDriverLocation(request);

        ArgumentCaptor<Object> metaCaptor = ArgumentCaptor.forClass(Object.class);
        verify(valueOps).set(eq(META_PREFIX + driverId), metaCaptor.capture(), any(Duration.class));
        DriverLocationDetails stored = (DriverLocationDetails) metaCaptor.getValue();
        assertEquals(0.0f, stored.bearing());
        assertEquals(0.0, stored.speed());
        assertNull(stored.activeTripId());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(KafkaProducerConfig.DRIVER_LOCATION_TOPIC), eq(driverId.toString()), eventCaptor.capture());
        DriverLocationEvent event = (DriverLocationEvent) eventCaptor.getValue();
        assertEquals(0.0f, event.bearing());
        assertEquals(0.0, event.speed());
    }

    // ---------- getNearbyDrivers ----------

    @Test
    void getNearbyDrivers_buildsRadiusQueryFromParameters() {
        when(geoOps.radius(eq(GEO_KEY), any(Circle.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(null);

        service.getNearbyDrivers(41.0, 29.0, 3.0, 7);

        ArgumentCaptor<Circle> circleCaptor = ArgumentCaptor.forClass(Circle.class);
        ArgumentCaptor<RedisGeoCommands.GeoRadiusCommandArgs> argsCaptor =
                ArgumentCaptor.forClass(RedisGeoCommands.GeoRadiusCommandArgs.class);
        verify(geoOps).radius(eq(GEO_KEY), circleCaptor.capture(), argsCaptor.capture());

        Circle circle = circleCaptor.getValue();
        assertEquals(29.0, circle.getCenter().getX());
        assertEquals(41.0, circle.getCenter().getY());
        assertEquals(3.0, circle.getRadius().getValue());
        assertEquals(Metrics.KILOMETERS.getMultiplier(), circle.getRadius().getMetric().getMultiplier());

        RedisGeoCommands.GeoRadiusCommandArgs args = argsCaptor.getValue();
        assertEquals(7L, args.getLimit());
        assertTrue(args.getFlags().contains(RedisGeoCommands.GeoRadiusCommandArgs.Flag.WITHCOORD));
        assertTrue(args.getFlags().contains(RedisGeoCommands.GeoRadiusCommandArgs.Flag.WITHDIST));
        assertEquals("ASC", String.valueOf(args.getSortDirection()));
    }

    @Test
    void getNearbyDrivers_whenResultsNull_returnsEmptyList() {
        when(geoOps.radius(eq(GEO_KEY), any(Circle.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(null);

        assertTrue(service.getNearbyDrivers(1.0, 2.0, 5.0, 10).isEmpty());
    }

    @Test
    void getNearbyDrivers_whenResultsEmpty_returnsEmptyList() {
        when(geoOps.radius(eq(GEO_KEY), any(Circle.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(new GeoResults<>(List.of()));

        assertTrue(service.getNearbyDrivers(1.0, 2.0, 5.0, 10).isEmpty());
    }

    @Test
    void getNearbyDrivers_returnsOnlyIdleDrivers_andMapsFields() {
        UUID idle = UUID.randomUUID();
        UUID busy = UUID.randomUUID();
        when(geoOps.radius(eq(GEO_KEY), any(Circle.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(new GeoResults<>(List.of(geoResult(idle, 0.5), geoResult(busy, 1.5))));
        when(valueOps.get(META_PREFIX + idle)).thenReturn(details(idle, null));
        when(valueOps.get(META_PREFIX + busy)).thenReturn(details(busy, UUID.randomUUID()));

        List<NearbyDriverResponse> result = service.getNearbyDrivers(41.0, 29.0, 5.0, 10);

        assertEquals(1, result.size());
        NearbyDriverResponse response = result.getFirst();
        assertEquals(idle, response.driverId());
        assertEquals(41.0, response.latitude());
        assertEquals(29.0, response.longitude());
        assertEquals(0.5, response.distanceKm());
        assertEquals(90.0f, response.bearing());
        assertEquals(30.0, response.speed());
        assertEquals(Instant.parse("2026-01-01T10:00:00Z"), response.lastSeen());
        verify(zSetOps, never()).remove(any(), any());
    }

    @Test
    void getNearbyDrivers_preservesResultOrder() {
        UUID near = UUID.randomUUID();
        UUID far = UUID.randomUUID();
        when(geoOps.radius(eq(GEO_KEY), any(Circle.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(new GeoResults<>(List.of(geoResult(near, 0.2), geoResult(far, 2.0))));
        when(valueOps.get(META_PREFIX + near)).thenReturn(details(near, null));
        when(valueOps.get(META_PREFIX + far)).thenReturn(details(far, null));

        List<NearbyDriverResponse> result = service.getNearbyDrivers(41.0, 29.0, 5.0, 10);

        assertEquals(2, result.size());
        assertEquals(near, result.get(0).driverId());
        assertEquals(far, result.get(1).driverId());
    }

    @Test
    void getNearbyDrivers_removesStaleDriversWithoutMetadata() {
        UUID stale = UUID.randomUUID();
        when(geoOps.radius(eq(GEO_KEY), any(Circle.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(new GeoResults<>(List.of(geoResult(stale, 0.5))));
        when(valueOps.get(META_PREFIX + stale)).thenReturn(null);

        List<NearbyDriverResponse> result = service.getNearbyDrivers(41.0, 29.0, 5.0, 10);

        assertTrue(result.isEmpty());
        verify(zSetOps).remove(GEO_KEY, stale.toString());
    }

    // ---------- getDriverLocation ----------

    @Test
    void getDriverLocation_whenMetadataExists_returnsIt() {
        UUID driverId = UUID.randomUUID();
        DriverLocationDetails stored = details(driverId, UUID.randomUUID());
        when(valueOps.get(META_PREFIX + driverId)).thenReturn(stored);

        Optional<DriverLocationDetails> result = service.getDriverLocation(driverId);

        assertTrue(result.isPresent());
        assertEquals(stored, result.get());
        verify(geoOps, never()).position(any(), any(Object[].class));
    }

    @Test
    void getDriverLocation_whenMetadataMissing_fallsBackToGeoPosition() {
        UUID driverId = UUID.randomUUID();
        when(valueOps.get(META_PREFIX + driverId)).thenReturn(null);
        when(geoOps.position(GEO_KEY, driverId.toString())).thenReturn(List.of(new Point(29.5, 41.5)));

        Optional<DriverLocationDetails> result = service.getDriverLocation(driverId);

        assertTrue(result.isPresent());
        DriverLocationDetails fallback = result.get();
        assertEquals(driverId, fallback.driverId());
        assertEquals(41.5, fallback.latitude());
        assertEquals(29.5, fallback.longitude());
        assertEquals(0.0f, fallback.bearing());
        assertEquals(0.0, fallback.speed());
        assertNull(fallback.activeTripId());
        assertNotNull(fallback.lastSeen());
    }

    @Test
    void getDriverLocation_whenMetadataIsWrongType_fallsBackToGeoPosition() {
        UUID driverId = UUID.randomUUID();
        when(valueOps.get(META_PREFIX + driverId)).thenReturn("garbage");
        when(geoOps.position(GEO_KEY, driverId.toString())).thenReturn(List.of(new Point(1.0, 2.0)));

        Optional<DriverLocationDetails> result = service.getDriverLocation(driverId);

        assertTrue(result.isPresent());
        assertEquals(2.0, result.get().latitude());
        assertEquals(1.0, result.get().longitude());
    }

    @Test
    void getDriverLocation_whenNothingStored_returnsEmpty() {
        UUID driverId = UUID.randomUUID();
        when(valueOps.get(META_PREFIX + driverId)).thenReturn(null);
        when(geoOps.position(GEO_KEY, driverId.toString())).thenReturn(null);

        assertTrue(service.getDriverLocation(driverId).isEmpty());
    }

    @Test
    void getDriverLocation_whenPositionListEmpty_returnsEmpty() {
        UUID driverId = UUID.randomUUID();
        when(valueOps.get(META_PREFIX + driverId)).thenReturn(null);
        when(geoOps.position(GEO_KEY, driverId.toString())).thenReturn(List.of());

        assertTrue(service.getDriverLocation(driverId).isEmpty());
    }

    @Test
    void getDriverLocation_whenPositionIsNullEntry_returnsEmpty() {
        UUID driverId = UUID.randomUUID();
        when(valueOps.get(META_PREFIX + driverId)).thenReturn(null);
        when(geoOps.position(GEO_KEY, driverId.toString())).thenReturn(java.util.Collections.singletonList(null));

        assertTrue(service.getDriverLocation(driverId).isEmpty());
    }

    // ---------- setDriverOffline ----------

    @Test
    void setDriverOffline_removesGeoEntryAndMetadata() {
        UUID driverId = UUID.randomUUID();

        service.setDriverOffline(driverId);

        verify(zSetOps).remove(GEO_KEY, driverId.toString());
        verify(redisTemplate).delete(META_PREFIX + driverId);
    }
}
