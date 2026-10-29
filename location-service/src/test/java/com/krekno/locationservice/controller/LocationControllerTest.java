package com.krekno.locationservice.controller;

import com.krekno.locationservice.dto.DriverLocationDetails;
import com.krekno.locationservice.dto.DriverLocationPingRequest;
import com.krekno.locationservice.dto.NearbyDriverResponse;
import com.krekno.locationservice.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LocationControllerTest {

    private static final String BASE_URL = "/api/locations";

    @Mock
    private LocationService locationService;

    @InjectMocks
    private LocationController locationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(locationController).build();
    }

    // ---------- POST /api/locations/drivers/ping ----------

    @Test
    void ping_withValidFullRequest_returns204AndDelegatesToService() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        String body = """
                {
                  "driverId": "%s",
                  "latitude": 41.0082,
                  "longitude": 28.9784,
                  "bearing": 90.5,
                  "speed": 42.0,
                  "activeTripId": "%s"
                }
                """.formatted(driverId, tripId);

        mockMvc.perform(post(BASE_URL + "/drivers/ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        ArgumentCaptor<DriverLocationPingRequest> captor = ArgumentCaptor.forClass(DriverLocationPingRequest.class);
        verify(locationService).updateDriverLocation(captor.capture());
        DriverLocationPingRequest captured = captor.getValue();
        assertEquals(driverId, captured.driverId());
        assertEquals(41.0082, captured.latitude());
        assertEquals(28.9784, captured.longitude());
        assertEquals(90.5f, captured.bearing());
        assertEquals(42.0, captured.speed());
        assertEquals(tripId, captured.activeTripId());
    }

    @Test
    void ping_withOnlyRequiredFields_returns204() throws Exception {
        UUID driverId = UUID.randomUUID();
        String body = """
                {"driverId": "%s", "latitude": 10.0, "longitude": 20.0}
                """.formatted(driverId);

        mockMvc.perform(post(BASE_URL + "/drivers/ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        ArgumentCaptor<DriverLocationPingRequest> captor = ArgumentCaptor.forClass(DriverLocationPingRequest.class);
        verify(locationService).updateDriverLocation(captor.capture());
        assertEquals(driverId, captor.getValue().driverId());
        assertNull(captor.getValue().bearing());
        assertNull(captor.getValue().speed());
        assertNull(captor.getValue().activeTripId());
    }

    @Test
    void ping_withBoundaryCoordinates_returns204() throws Exception {
        String body = """
                {"driverId": "%s", "latitude": -90.0, "longitude": 180.0}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post(BASE_URL + "/drivers/ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        verify(locationService).updateDriverLocation(any());
    }

    @Test
    void ping_withMissingDriverId_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/drivers/ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\": 10.0, \"longitude\": 20.0}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationService);
    }

    @Test
    void ping_withMissingLatitude_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/drivers/ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\": \"" + UUID.randomUUID() + "\", \"longitude\": 20.0}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationService);
    }

    @Test
    void ping_withMissingLongitude_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/drivers/ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\": \"" + UUID.randomUUID() + "\", \"latitude\": 10.0}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationService);
    }

    @Test
    void ping_withLatitudeAbove90_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/drivers/ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\": \"" + UUID.randomUUID() + "\", \"latitude\": 90.1, \"longitude\": 20.0}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationService);
    }

    @Test
    void ping_withLatitudeBelowMinus90_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/drivers/ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\": \"" + UUID.randomUUID() + "\", \"latitude\": -90.1, \"longitude\": 20.0}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationService);
    }

    @Test
    void ping_withLongitudeAbove180_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/drivers/ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\": \"" + UUID.randomUUID() + "\", \"latitude\": 10.0, \"longitude\": 180.1}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationService);
    }

    @Test
    void ping_withLongitudeBelowMinus180_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/drivers/ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\": \"" + UUID.randomUUID() + "\", \"latitude\": 10.0, \"longitude\": -180.1}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationService);
    }

    @Test
    void ping_withMalformedJson_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/drivers/ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationService);
    }

    @Test
    void ping_withInvalidDriverIdFormat_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/drivers/ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\": \"abc\", \"latitude\": 10.0, \"longitude\": 20.0}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationService);
    }

    @Test
    void ping_withEmptyBody_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/drivers/ping")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationService);
    }

    @Test
    void ping_withUnsupportedMediaType_returns415() throws Exception {
        mockMvc.perform(post(BASE_URL + "/drivers/ping")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(locationService);
    }

    @Test
    void ping_withGet_isTreatedAsInvalidDriverId_returns400() throws Exception {
        // GET /drivers/ping is matched by GET /drivers/{driverId} with an invalid UUID -> 400
        mockMvc.perform(get(BASE_URL + "/drivers/ping"))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET /api/locations/drivers/nearby ----------

    @Test
    void nearby_withAllParams_returns200WithDrivers() throws Exception {
        UUID driverId = UUID.randomUUID();
        NearbyDriverResponse response = new NearbyDriverResponse(
                driverId, 41.0, 29.0, 1.25, 180.0f, 33.0, Instant.parse("2026-01-01T10:00:00Z"));
        when(locationService.getNearbyDrivers(41.0, 29.0, 3.0, 5)).thenReturn(List.of(response));

        mockMvc.perform(get(BASE_URL + "/drivers/nearby")
                        .param("lat", "41.0")
                        .param("lon", "29.0")
                        .param("radiusKm", "3.0")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].driverId").value(driverId.toString()))
                .andExpect(jsonPath("$[0].latitude").value(41.0))
                .andExpect(jsonPath("$[0].longitude").value(29.0))
                .andExpect(jsonPath("$[0].distanceKm").value(1.25))
                .andExpect(jsonPath("$[0].bearing").value(180.0))
                .andExpect(jsonPath("$[0].speed").value(33.0));

        verify(locationService).getNearbyDrivers(41.0, 29.0, 3.0, 5);
    }

    @Test
    void nearby_withoutOptionalParams_usesDefaults() throws Exception {
        when(locationService.getNearbyDrivers(41.0, 29.0, 5.0, 10)).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL + "/drivers/nearby")
                        .param("lat", "41.0")
                        .param("lon", "29.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(locationService).getNearbyDrivers(41.0, 29.0, 5.0, 10);
    }

    @Test
    void nearby_withMultipleDrivers_preservesOrder() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(locationService.getNearbyDrivers(anyDouble(), anyDouble(), anyDouble(), anyInt())).thenReturn(List.of(
                new NearbyDriverResponse(first, 1.0, 2.0, 0.1, 0.0f, 0.0, null),
                new NearbyDriverResponse(second, 1.1, 2.1, 0.9, 0.0f, 0.0, null)));

        mockMvc.perform(get(BASE_URL + "/drivers/nearby")
                        .param("lat", "1.0")
                        .param("lon", "2.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].driverId").value(first.toString()))
                .andExpect(jsonPath("$[1].driverId").value(second.toString()));
    }

    @Test
    void nearby_withMissingLat_returns400() throws Exception {
        mockMvc.perform(get(BASE_URL + "/drivers/nearby").param("lon", "29.0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationService);
    }

    @Test
    void nearby_withMissingLon_returns400() throws Exception {
        mockMvc.perform(get(BASE_URL + "/drivers/nearby").param("lat", "41.0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationService);
    }

    @Test
    void nearby_withNonNumericLat_returns400() throws Exception {
        mockMvc.perform(get(BASE_URL + "/drivers/nearby")
                        .param("lat", "abc")
                        .param("lon", "29.0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationService);
    }

    @Test
    void nearby_withNonNumericLimit_returns400() throws Exception {
        mockMvc.perform(get(BASE_URL + "/drivers/nearby")
                        .param("lat", "41.0")
                        .param("lon", "29.0")
                        .param("limit", "many"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationService);
    }

    @Test
    void nearby_withPost_returns405() throws Exception {
        mockMvc.perform(post(BASE_URL + "/drivers/nearby"))
                .andExpect(status().isMethodNotAllowed());
    }

    // ---------- GET /api/locations/drivers/{driverId} ----------

    @Test
    void getDriverLocation_whenFound_returns200WithDetails() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        DriverLocationDetails details = new DriverLocationDetails(
                driverId, 41.0, 29.0, 270.0f, 55.5, tripId, Instant.parse("2026-01-01T10:00:00Z"));
        when(locationService.getDriverLocation(driverId)).thenReturn(Optional.of(details));

        mockMvc.perform(get(BASE_URL + "/drivers/{driverId}", driverId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.driverId").value(driverId.toString()))
                .andExpect(jsonPath("$.latitude").value(41.0))
                .andExpect(jsonPath("$.longitude").value(29.0))
                .andExpect(jsonPath("$.bearing").value(270.0))
                .andExpect(jsonPath("$.speed").value(55.5))
                .andExpect(jsonPath("$.activeTripId").value(tripId.toString()));

        verify(locationService).getDriverLocation(driverId);
    }

    @Test
    void getDriverLocation_whenNotFound_returns404() throws Exception {
        UUID driverId = UUID.randomUUID();
        when(locationService.getDriverLocation(driverId)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_URL + "/drivers/{driverId}", driverId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDriverLocation_withInvalidUuid_returns400() throws Exception {
        mockMvc.perform(get(BASE_URL + "/drivers/{driverId}", "not-a-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationService);
    }

    // ---------- DELETE /api/locations/drivers/{driverId} ----------

    @Test
    void setDriverOffline_returns204AndDelegatesToService() throws Exception {
        UUID driverId = UUID.randomUUID();

        mockMvc.perform(delete(BASE_URL + "/drivers/{driverId}", driverId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(locationService).setDriverOffline(driverId);
    }

    @Test
    void setDriverOffline_withInvalidUuid_returns400() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/drivers/{driverId}", "not-a-uuid"))
                .andExpect(status().isBadRequest());

        verify(locationService, never()).setDriverOffline(any());
    }

    @Test
    void driverEndpoint_withPut_returns405() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put(BASE_URL + "/drivers/{driverId}", UUID.randomUUID()))
                .andExpect(status().isMethodNotAllowed());
    }
}
