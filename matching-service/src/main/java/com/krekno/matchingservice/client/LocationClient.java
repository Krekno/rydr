package com.krekno.matchingservice.client;

import com.krekno.matchingservice.dto.NearbyDriverResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "location-client",
        url = "${gateway.url:https://localhost:8080}"
)
public interface LocationClient {

    @GetMapping("/drivers/nearby")
    public List<NearbyDriverResponse> getNearbyDrivers(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "5.0") double radiusKm,
            @RequestParam(defaultValue = "10") int limit
    );

}
