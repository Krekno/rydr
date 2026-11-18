package com.krekno.tripservice.dto;

public record CreateTripRequest(Double pickupLatitude,
                                Double pickupLongitude,
                                Double destinationLatitude,
                                Double destinationLongitude) {
}
