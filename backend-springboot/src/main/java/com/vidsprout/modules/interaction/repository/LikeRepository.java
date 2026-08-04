package com.vidsprout.modules.interaction.repository;

import com.vidsprout.modules.interaction.model.Like;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface LikeRepository extends JpaRepository<Like, UUID> {
    Optional<Like> findByUserIdAndVideoId(UUID userId, UUID videoId);
    Optional<Like> findByUserIdAndCommentId(UUID userId, UUID commentId);
    boolean existsByUserIdAndVideoId(UUID userId, UUID videoId);
    boolean existsByUserIdAndCommentId(UUID userId, UUID commentId);

    @Query("SELECT l.video.id FROM Like l WHERE l.user.id = :userId AND l.video.id IN :videoIds")
    Set<UUID> findLikedVideoIds(@Param("userId") UUID userId, @Param("videoIds") List<UUID> videoIds);

    @Query("SELECT l.video.id FROM Like l WHERE l.user.id = :userId ORDER BY l.createdAt DESC")
    List<UUID> findLikedVideoIdsByUser(@Param("userId") UUID userId);

    @Query("SELECT l FROM Like l JOIN FETCH l.video WHERE l.user.id = :userId AND l.video IS NOT NULL")
    Page<Like> findLikedVideosByUser(@Param("userId") UUID userId, Pageable pageable);

    List<Like> findByUserIdAndVideoIdIn(UUID userId, List<UUID> videoIds);
}
