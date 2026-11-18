package com.krekno.tripservice.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FareCalculator {

    public BigDecimal calculate(
            double distanceMeters,
            double durationSeconds
    ) {
        BigDecimal baseFare = new BigDecimal("40");
        BigDecimal pricePerKm = new BigDecimal("15");
        BigDecimal pricePerMinute = new BigDecimal("2");

        BigDecimal distanceKm =
                BigDecimal.valueOf(distanceMeters / 1000);

        BigDecimal durationMinutes =
                BigDecimal.valueOf(durationSeconds / 60);

        return baseFare
                .add(distanceKm.multiply(pricePerKm))
                .add(durationMinutes.multiply(pricePerMinute))
                .setScale(2, RoundingMode.HALF_UP);
    }
}