package com.vidsprout.modules.interaction.repository;

import com.vidsprout.modules.interaction.model.History;
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
public interface HistoryRepository extends JpaRepository<History, UUID> {
    
    Optional<History> findByUserIdAndVideoId(UUID userId, UUID videoId);
    
    Page<History> findByUserId(UUID userId, Pageable pageable);
    
    @Query("SELECT h FROM History h JOIN FETCH h.video WHERE h.user.id = :userId ORDER BY h.createdAt DESC")
    Page<History> findByUserIdWithVideo(@Param("userId") UUID userId, Pageable pageable);
    
    List<History> findByUserIdAndVideoIdIn(UUID userId, List<UUID> videoIds);
    
    void deleteByUserIdAndVideoIdIn(UUID userId, List<UUID> videoIds);
}
