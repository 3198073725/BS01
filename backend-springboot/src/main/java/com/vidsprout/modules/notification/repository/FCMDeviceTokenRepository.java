package com.vidsprout.modules.notification.repository;

import com.vidsprout.modules.notification.model.FCMDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FCMDeviceTokenRepository extends JpaRepository<FCMDeviceToken, UUID> {

    Optional<FCMDeviceToken> findByToken(String token);

    List<FCMDeviceToken> findByUserIdAndIsActiveTrue(UUID userId);

    List<FCMDeviceToken> findByIsActiveTrue();
}
