package com.krekno.tripservice.config;

import com.krekno.tripservice.dto.OsrmResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "osrm-client",
        url = "${osrm.url}"
)
public interface OsrmClient {
    @GetMapping("/route/v1/driving/{coordinates}")
    OsrmResponse getRoute(
            @PathVariable String coordinates,
            @RequestParam(defaultValue = "false") boolean overview
    );
}