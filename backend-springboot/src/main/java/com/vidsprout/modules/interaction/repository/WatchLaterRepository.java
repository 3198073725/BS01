package com.vidsprout.modules.interaction.repository;

import com.vidsprout.modules.interaction.model.WatchLater;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchLaterRepository extends JpaRepository<WatchLater, UUID> {
    
    Optional<WatchLater> findByUserIdAndVideoId(UUID userId, UUID videoId);
    
    boolean existsByUserIdAndVideoId(UUID userId, UUID videoId);
    
    Page<WatchLater> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    List<WatchLater> findByUserIdAndVideoIdIn(UUID userId, List<UUID> videoIds);
    
    void deleteByUserIdAndVideoIdIn(UUID userId, List<UUID> videoIds);
}
