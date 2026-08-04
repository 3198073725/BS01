package com.vidsprout.modules.interaction.repository;

import com.vidsprout.modules.interaction.model.Favorite;
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
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    Optional<Favorite> findByUserIdAndVideoId(UUID userId, UUID videoId);
    boolean existsByUserIdAndVideoId(UUID userId, UUID videoId);

    @Query("SELECT f.video.id FROM Favorite f WHERE f.user.id = :userId AND f.video.id IN :videoIds")
    Set<UUID> findFavoritedVideoIds(@Param("userId") UUID userId, @Param("videoIds") List<UUID> videoIds);

    @Query("SELECT f.video.id FROM Favorite f WHERE f.user.id = :userId ORDER BY f.createdAt DESC")
    List<UUID> findFavoritedVideoIdsByUser(@Param("userId") UUID userId);

    @Query("SELECT f FROM Favorite f JOIN FETCH f.video WHERE f.user.id = :userId AND f.video IS NOT NULL")
    Page<Favorite> findFavoritedVideosByUser(@Param("userId") UUID userId, Pageable pageable);

    List<Favorite> findByUserIdAndVideoIdIn(UUID userId, List<UUID> videoIds);

    long countByVideoId(UUID videoId);
}
