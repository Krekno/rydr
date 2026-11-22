
package com.krekno.ratingservice.repository;

import com.krekno.ratingservice.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RatingRepository extends JpaRepository<Rating, UUID> {

    List<Rating> findAllByCustomerId(UUID customerId);

    List<Rating> findAllByDriverId(UUID driverId);

    boolean existsByTripId(UUID tripId);

    @Query("""
            SELECT AVG(r.score)
            FROM Rating r
            WHERE r.driverId = :driverId
            """)
    Double getMeanScore(@Param("driverId") UUID driverId);
}
