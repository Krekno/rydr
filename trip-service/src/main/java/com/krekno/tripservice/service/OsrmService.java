package com.krekno.tripservice.service;

import com.krekno.tripservice.config.OsrmClient;
import com.krekno.tripservice.dto.OsrmResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OsrmService {

    private final OsrmClient osrmClient;

    public OsrmResponse.Route calculateRoute(
            double pickupLat,
            double pickupLon,
            double destinationLat,
            double destinationLon
    ) {

        String coordinates = String.format(
                Locale.US,
                "%f,%f;%f,%f",
                pickupLon,
                pickupLat,
                destinationLon,
                destinationLat
        );

        OsrmResponse response = osrmClient.getRoute(
                coordinates,
                false
        );

        if (response == null ||
                !"Ok".equals(response.code()) ||
                response.routes() == null ||
                response.routes().isEmpty()) {
            throw new IllegalStateException(
                    "Unable to calculate route"
            );
        }

        return response.routes().getFirst();
    }
}