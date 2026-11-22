
package com.krekno.ratingservice.service;

import com.krekno.ratingservice.entity.Rating;
import com.krekno.ratingservice.entity.dto.CreateRatingRequest;
import com.krekno.ratingservice.entity.dto.UpdateRatingRequest;
import com.krekno.ratingservice.exception.RatingNotFoundException;
import com.krekno.ratingservice.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RatingService {

    private final RatingRepository ratingRepository;

    @Transactional
    public Rating createRating(CreateRatingRequest request, Jwt jwt) {
        UUID customerId = getUserId(jwt);

        if (ratingRepository.existsByTripId(request.tripId())) {
            throw new IllegalStateException(
                    "A rating already exists for this trip"
            );
        }

        Rating rating = Rating.builder()
                .customerId(customerId)
                .driverId(request.driverId())
                .tripId(request.tripId())
                .score(request.score())
                .comment(request.comment())
                .build();

        return ratingRepository.save(rating);
    }

    @Transactional
    public Rating updateRating(UpdateRatingRequest request, Jwt jwt) {
        UUID customerId = getUserId(jwt);

        Rating rating = ratingRepository.findById(request.ratingId())
                .orElseThrow(() -> new RatingNotFoundException(
                        request.ratingId()
                ));

        verifyOwnership(rating, customerId);

        rating.setScore(request.score());
        rating.setComment(request.comment());

        return ratingRepository.save(rating);
    }

    @Transactional
    public void deleteRating(UUID id, Jwt jwt) {
        UUID customerId = getUserId(jwt);

        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new RatingNotFoundException(id));

        verifyOwnership(rating, customerId);

        ratingRepository.delete(rating);
    }

    public List<Rating> getAllRatingsByCustomerId(Jwt jwt) {
        UUID customerId = getUserId(jwt);

        return ratingRepository.findAllByCustomerId(customerId);
    }

    public List<Rating> getAllRatingsByDriverId(UUID driverId) {
        return ratingRepository.findAllByDriverId(driverId);
    }

    public Rating getRatingById(UUID id) {
        return ratingRepository.findById(id)
                .orElseThrow(() -> new RatingNotFoundException(id));
    }

    public Double getMeanScore(UUID driverId) {
        return ratingRepository.getMeanScore(driverId);
    }

    private void verifyOwnership(Rating rating, UUID customerId) {
        if (!rating.getCustomerId().equals(customerId)) {
            throw new SecurityException(
                    "You are not authorized to modify this rating"
            );
        }
    }

    private UUID getUserId(Jwt jwt) {
        String subject = jwt.getSubject();
        assert subject != null;
        return UUID.fromString(subject);
    }
}
