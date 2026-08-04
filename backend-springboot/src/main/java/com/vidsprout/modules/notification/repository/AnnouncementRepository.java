package com.vidsprout.modules.notification.repository;

import com.vidsprout.modules.notification.model.SystemAnnouncement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<SystemAnnouncement, UUID> {

    Page<SystemAnnouncement> findByIsActiveTrueOrderByPinnedDescPublishedAtDescCreatedAtDesc(Pageable pageable);

    Page<SystemAnnouncement> findAllByOrderByPinnedDescPublishedAtDescCreatedAtDesc(Pageable pageable);

    long countByIsActiveTrue();

    @Query("SELECT a FROM SystemAnnouncement a WHERE a.isActive = true AND NOT EXISTS (" +
            "SELECT r FROM SystemAnnouncementRead r WHERE r.announcement = a AND r.user.id = :userId) " +
            "ORDER BY a.pinned DESC, a.publishedAt DESC, a.createdAt DESC")
    List<SystemAnnouncement> findLatestUnread(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM SystemAnnouncement a WHERE a.isActive = true AND NOT EXISTS (" +
            "SELECT r FROM SystemAnnouncementRead r WHERE r.announcement = a AND r.user.id = :userId)")
    long countUnread(@Param("userId") UUID userId);
}
