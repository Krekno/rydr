# API reference

This document lists the HTTP and STOMP endpoints implemented in the repository. The gateway listens on `http://localhost:8080` by default. Service URLs and gateway predicates come from each service's configuration; some routes listed below are service-local and may not be reachable through the gateway as currently configured.

## Authentication

Protected routes expect a bearer access token (`Authorization: Bearer <accessToken>`), except where noted. JSON request and response bodies use `application/json`.

## User service

| Method and path | Access | Request | Success response | Description |
|---|---|---|---|---|
| `POST /api/auth/register` | Public | `RegisterDto`: `firstName`, `lastName`, `email`, `password` (8–72 chars), `phoneNumber` | `200 AuthResponse` | Creates an account and returns access and refresh tokens. |
| `POST /api/auth/login` | Public | `LoginDto`: `email`, `password` | `200 AuthResponse` | Authenticates and returns tokens. |
| `POST /api/auth/refresh` | Public | `RefreshTokenRequest`: `refreshToken` | `200 AuthResponse` | Exchanges a refresh token. |
| `POST /api/auth/logout` | Authenticated | — | `204` | Logs out the authenticated user. |
| `PUT /api/auth/update` | Authenticated | `RegisterDto` | `200 UserDto` | Updates user details using the registration-shaped request. |
| `GET /api/users/me` | Authenticated | — | `200 UserDto` | Gets the current user profile. |
| `PUT /api/users/me` | Authenticated | `UpdateUserRequest`: `firstName`, `lastName`, `phoneNumber` | `200 UserDto` | Updates profile details. |
| `PATCH /api/users/me/password` | Authenticated | `ChangePasswordRequest`: `currentPassword`, `newPassword` (8–72 chars) | `204` | Changes the current user's password. |
| `POST /api/drivers/apply` | Authenticated | — | `201 DriverResponseDto` | Applies the current user to become a driver. |
| `GET /api/drivers/me` | Authenticated | — | `200 DriverResponseDto` | Gets the current user's driver profile. |
| `PATCH /api/drivers/me/status` | Authenticated | `UpdateDriverStatusRequest`: `status` | `200 DriverResponseDto` | Changes the current driver's status. |
| `GET /api/drivers/{id}` | As configured by service security | UUID path parameter | `200 DriverResponseDto` | Gets a driver by ID. |
| `POST /api/drivers/me/vehicles` | Authenticated | `CreateVehicleRequest`: `make`, `model`, `year`, `color`, `licensePlate` | `201 VehicleDto` | Adds a vehicle to the current driver. |
| `GET /api/drivers/me/vehicles` | Authenticated | — | `200 VehicleDto[]` | Lists the current driver's vehicles. |
| `DELETE /api/drivers/me/vehicles/{vehicleId}` | Authenticated | UUID path parameter | `204` | Deletes a vehicle belonging to the current driver. |
| `GET /api/admin/drivers` | Admin | Optional `verified` boolean query | `200 DriverResponseDto[]` | Lists drivers, optionally filtering by verification. |
| `PATCH /api/admin/drivers/{id}/verify` | Admin | Optional `verified` boolean query (default `true`) | `200 DriverResponseDto` | Sets driver verification. |

`AuthResponse` contains `accessToken` and `refreshToken`. `UserDto` contains `id`, `firstName`, `lastName`, `email`, `phoneNumber`, `role`. `DriverResponseDto` contains driver/user IDs, contact details, `status`, rating average/count, verification flag, creation time, and vehicles. `VehicleDto` contains `id`, `make`, `model`, `year`, `color`, and `licensePlate`.

### Internal user-service endpoints

These are service-to-service operations and are not included in the gateway's explicit user-service path predicate.

| Method and path | Request | Success response | Description |
|---|---|---|---|
| `GET /api/internal/users/{id}` | UUID path parameter | `200 UserDto` | Gets a user by ID. |
| `GET /api/internal/drivers/{id}` | UUID path parameter | `200 DriverResponseDto` | Gets a driver by ID. |
| `PATCH /api/internal/drivers/{id}/status` | `UpdateDriverStatusRequest`: `status` | `200 DriverResponseDto` | Sets driver status. |
| `POST /api/internal/drivers/{id}/ratings` | `DriverRatingRequest`: integer `Rating` from 1 to 5 | `200 DriverResponseDto` | Updates driver rating. |

## Location service

| Method and path | Access | Request | Success response | Description |
|---|---|---|---|---|
| `POST /api/locations/drivers/ping` | Service security configuration | `DriverLocationPingRequest`: `driverId`, `latitude` (-90..90), `longitude` (-180..180); optional `bearing`, `speed`, `activeTripId` | `204` | Updates a driver's location. |
| `GET /api/locations/drivers/nearby?lat={lat}&lon={lon}&radiusKm={km}&limit={n}` | Service security configuration | `lat` and `lon` required; `radiusKm` defaults to `5.0`; `limit` defaults to `10` | `200 NearbyDriverResponse[]` | Finds nearby drivers. Each result includes driver ID, coordinates, distance, bearing, speed, and last-seen time. |
| `GET /api/locations/drivers/{driverId}` | Service security configuration | UUID path parameter | `200 DriverLocationDetails`; `404` if absent | Gets a driver's latest location, including active trip and last-seen time. |
| `DELETE /api/locations/drivers/{driverId}` | Service security configuration | UUID path parameter | `204` | Sets a driver offline. |

## Trip service

| Method and path | Access | Request | Success response | Description |
|---|---|---|---|---|
| `POST /api/trip` | Authenticated | `CreateTripRequest`: `pickupLatitude`, `pickupLongitude`, `destinationLatitude`, `destinationLongitude` | `200 Trip` | Creates a trip. |
| `POST /api/trip/arrived/{id}` | Authenticated | UUID trip ID | `200 Trip` | Marks arrival at pickup. |
| `POST /api/trip/start/{id}` | Authenticated | UUID trip ID | `200 Trip` | Starts a trip. |
| `POST /api/trip/cancel/{id}` | Authenticated | UUID trip ID | `200 Trip` | Cancels a trip. |
| `POST /api/trip/complete/{id}` | Authenticated | UUID trip ID | `200 Trip` | Completes a trip. |

The trip controller uses `/api/trip` (singular). The gateway's explicit trip route currently matches `/api/trips/**` (plural), so these controller routes may not be reachable through that gateway route without a configuration change.

## Location STOMP/WebSocket

The location service registers a STOMP endpoint at `/ws` with SockJS enabled. Send a STOMP `SEND` frame to `/app/location.ping` with the same JSON shape as `DriverLocationPingRequest` to submit a location update. Location updates are also broadcast by the service; see its messaging configuration for broker destinations. The gateway has a `/ws/**` WebSocket route, but service discovery must expose the location service at the configured route for it to resolve.

## Other configured gateway routes

The gateway declares `/api/matching/**`, `/api/ratings/**`, and `/api/payments/**` routes. No HTTP controller endpoints for those paths are implemented in this repository at present. The gateway also enables discovery-based route lookup and exposes Spring Boot management endpoints according to its configuration; exact paths depend on runtime configuration.

## External OSRM route

The trip service's `OsrmClient` calls the external OSRM service with `GET /route/v1/driving/{coordinates}` and an `overview` query parameter (default `false`). Coordinates and response format follow the OSRM API. This is an outbound dependency, not an application endpoint.
