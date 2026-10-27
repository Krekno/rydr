package com.krekno.userservice.controller;

import com.krekno.userservice.dto.DriverResponseDto;
import com.krekno.userservice.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DriverService driverService;

    @GetMapping("/drivers")
    public ResponseEntity<List<DriverResponseDto>> getAllDrivers(@RequestParam(required = false) Boolean verified) {
        return ResponseEntity.ok(driverService.getAllDrivers(verified));
    }

    @PatchMapping("/drivers/{id}/verify")
    public ResponseEntity<DriverResponseDto> verifyDriver(@PathVariable UUID id,
                                                          @RequestParam(defaultValue = "true") boolean verified) {
        return ResponseEntity.ok(driverService.verifyDriver(id, verified));
    }
}
