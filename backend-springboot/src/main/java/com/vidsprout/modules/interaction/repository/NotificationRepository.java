package com.vidsprout.modules.interaction.repository;

import com.vidsprout.modules.interaction.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    
    Page<Notification> findByUserIdAndHidden(UUID userId, Boolean hidden, Pageable pageable);
    
    Page<Notification> findByUserIdAndHiddenAndRead(UUID userId, Boolean hidden, Boolean read, Pageable pageable);
    
    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.actor LEFT JOIN FETCH n.video LEFT JOIN FETCH n.comment " +
            "WHERE n.user.id = :userId AND n.hidden = :hidden AND n.read = :read ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdWithAssociations(@Param("userId") UUID userId, @Param("hidden") Boolean hidden,
                                                     @Param("read") Boolean read, Pageable pageable);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.actor LEFT JOIN FETCH n.video LEFT JOIN FETCH n.comment " +
            "WHERE n.user.id = :userId AND n.hidden = :hidden ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdWithAssociationsAll(@Param("userId") UUID userId, @Param("hidden") Boolean hidden,
                                                        Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    int markAllAsRead(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.id IN :ids AND n.read = false")
    int markByIdsAsRead(@Param("userId") UUID userId, @Param("ids") List<UUID> ids);

    @Modifying
    @Query("UPDATE Notification n SET n.hidden = true, n.read = true WHERE n.user.id = :userId AND n.hidden = false")
    int clearAll(@Param("userId") UUID userId);

    long countByUserIdAndReadAndHidden(UUID userId, Boolean read, Boolean hidden);
    
    void deleteByUserId(UUID userId);
}
