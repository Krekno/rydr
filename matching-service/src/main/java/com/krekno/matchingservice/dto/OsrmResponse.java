package com.krekno.matchingservice.dto;

import java.util.List;

public record OsrmResponse(
        String code,
        List<Route> routes
) {
    public record Route(
            double distance,
            double duration
    ) {}
}