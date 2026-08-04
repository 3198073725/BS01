package com.vidsprout.modules.notification.repository;

import com.vidsprout.modules.notification.model.WebPushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscription, UUID> {

    Optional<WebPushSubscription> findByEndpoint(String endpoint);

    List<WebPushSubscription> findByUserIdAndIsActiveTrue(UUID userId);

    List<WebPushSubscription> findByIsActiveTrue();
}
