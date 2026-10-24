package com.krekno.userservice.repository;

import com.krekno.userservice.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    void deleteByUser_Id(UUID user);

    Optional<RefreshToken> findByUserId(UUID userId);
}
