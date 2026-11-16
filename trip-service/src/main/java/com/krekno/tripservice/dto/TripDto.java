package com.krekno.tripservice.dto;

public record TripDto(Double pickupLatitude,
                      Double pickupLongitude,
                      Double destinationLatitude,
                      Double destinationLongitude) {
}
