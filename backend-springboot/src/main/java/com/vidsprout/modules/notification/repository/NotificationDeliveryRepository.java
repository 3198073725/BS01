package com.vidsprout.modules.notification.repository;

import com.vidsprout.modules.notification.model.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {

    List<NotificationDelivery> findByNotificationId(UUID notificationId);

    List<NotificationDelivery> findByStatus(String status);
}
