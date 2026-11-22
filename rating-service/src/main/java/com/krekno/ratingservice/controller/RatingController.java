
package com.krekno.ratingservice.controller;

import com.krekno.ratingservice.entity.Rating;
import com.krekno.ratingservice.entity.dto.CreateRatingRequest;
import com.krekno.ratingservice.entity.dto.UpdateRatingRequest;
import com.krekno.ratingservice.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<Rating> createRating(
            @Valid @RequestBody CreateRatingRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ratingService.createRating(request, jwt));
    }

    @PutMapping
    public ResponseEntity<Rating> updateRating(
            @Valid @RequestBody UpdateRatingRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                ratingService.updateRating(request, jwt)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRating(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ratingService.deleteRating(id, jwt);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/customer")
    public ResponseEntity<List<Rating>> getMyRatings(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                ratingService.getAllRatingsByCustomerId(jwt)
        );
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<Rating>> getDriverRatings(
            @PathVariable UUID driverId
    ) {
        return ResponseEntity.ok(
                ratingService.getAllRatingsByDriverId(driverId)
        );
    }

    @GetMapping("/driver/{driverId}/average")
    public ResponseEntity<Double> getDriverAverageScore(
            @PathVariable UUID driverId
    ) {
        return ResponseEntity.ok(
                ratingService.getMeanScore(driverId)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Rating> getRatingById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                ratingService.getRatingById(id)
        );
    }
}
