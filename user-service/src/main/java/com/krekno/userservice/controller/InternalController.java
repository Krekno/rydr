package com.krekno.userservice.controller;

import com.krekno.userservice.dto.DriverRatingRequest;
import com.krekno.userservice.dto.DriverResponseDto;
import com.krekno.userservice.dto.UpdateDriverStatusRequest;
import com.krekno.userservice.dto.UserDto;
import com.krekno.userservice.service.DriverService;
import com.krekno.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {

    private final UserService userService;
    private final DriverService driverService;

    @GetMapping("/users/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/drivers/{id}")
    public ResponseEntity<DriverResponseDto> getDriverById(@PathVariable UUID id) {
        return ResponseEntity.ok(driverService.getDriverById(id));
    }

    @PatchMapping("/drivers/{id}/status")
    public ResponseEntity<DriverResponseDto> updateDriverStatus(@PathVariable UUID id,
                                                                @RequestBody @Valid UpdateDriverStatusRequest request) {
        return ResponseEntity.ok(driverService.setDriverStatusInternal(id, request.status()));
    }

    @PostMapping("/drivers/{id}/ratings")
    public ResponseEntity<DriverResponseDto> updateDriverRating(@PathVariable UUID id,
                                                                @RequestBody @Valid DriverRatingRequest request) {
        return ResponseEntity.ok(driverService.updateRating(id, request.rating()));
    }
}
