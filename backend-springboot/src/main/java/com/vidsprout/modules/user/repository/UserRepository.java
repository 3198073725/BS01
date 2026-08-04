package com.vidsprout.modules.user.repository;

import com.vidsprout.modules.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT *, similarity(username, CAST(:keyword AS text)) + similarity(COALESCE(nickname,''), CAST(:keyword AS text)) AS siml " +
            "FROM users_user WHERE username % CAST(:keyword AS text) OR COALESCE(nickname,'') % CAST(:keyword AS text) " +
            "ORDER BY siml DESC",
            countQuery = "SELECT COUNT(*) FROM users_user WHERE username % CAST(:keyword AS text) OR COALESCE(nickname,'') % CAST(:keyword AS text)",
            nativeQuery = true)
    Page<User> searchUsersTrigram(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.isActive = true ORDER BY u.dateJoined DESC")
    Page<User> findActiveUsers(Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.followersCount = u.followersCount + :delta WHERE u.id = :userId")
    void incrementFollowersCount(@Param("userId") UUID userId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE User u SET u.followingCount = u.followingCount + :delta WHERE u.id = :userId")
    void incrementFollowingCount(@Param("userId") UUID userId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE User u SET u.videoCount = u.videoCount + :delta WHERE u.id = :userId")
    void incrementVideoCount(@Param("userId") UUID userId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE User u SET u.videoCount = CASE WHEN u.videoCount >= :delta THEN u.videoCount - :delta ELSE 0 END WHERE u.id = :userId")
    void decrementVideoCount(@Param("userId") UUID userId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE User u SET u.followersCount = CASE WHEN u.followersCount >= :delta THEN u.followersCount - :delta ELSE 0 END WHERE u.id = :userId")
    void decrementFollowersCount(@Param("userId") UUID userId, @Param("delta") int delta);
}
