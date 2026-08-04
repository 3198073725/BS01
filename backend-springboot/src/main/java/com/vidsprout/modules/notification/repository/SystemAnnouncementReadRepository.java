package com.vidsprout.modules.notification.repository;

import com.vidsprout.modules.notification.model.SystemAnnouncementRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemAnnouncementReadRepository extends JpaRepository<SystemAnnouncementRead, UUID> {

    Optional<SystemAnnouncementRead> findByAnnouncementIdAndUserId(UUID announcementId, UUID userId);

    boolean existsByAnnouncementIdAndUserId(UUID announcementId, UUID userId);

    List<SystemAnnouncementRead> findByUserIdAndAnnouncementIdIn(UUID userId, List<UUID> announcementIds);

    long countByUserIdAndAnnouncement_IsActiveTrue(UUID userId);
}
