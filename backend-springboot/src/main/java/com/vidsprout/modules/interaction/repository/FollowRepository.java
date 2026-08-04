package com.vidsprout.modules.interaction.repository;

import com.vidsprout.modules.interaction.model.Follow;
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
public interface FollowRepository extends JpaRepository<Follow, UUID> {
    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
    Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
    Page<Follow> findByFollowingIdOrderByCreatedAtDesc(UUID followingId, Pageable pageable);
    Page<Follow> findByFollowerIdOrderByCreatedAtDesc(UUID followerId, Pageable pageable);

    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :followerId")
    List<UUID> findFollowedIdsByFollowerId(@Param("followerId") UUID followerId);
}
