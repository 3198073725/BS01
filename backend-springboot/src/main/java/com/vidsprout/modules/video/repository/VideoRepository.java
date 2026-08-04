package com.vidsprout.modules.video.repository;

import com.vidsprout.modules.video.model.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID>, JpaSpecificationExecutor<Video> {

    @Query("SELECT v FROM Video v WHERE v.status = 'published' AND v.visibility = 'public' ORDER BY v.publishedAt DESC")
    Page<Video> findPublishedVideos(Pageable pageable);

    @Query("SELECT v FROM Video v WHERE v.status = 'published' AND v.visibility = 'public' AND v.isFeatured = true ORDER BY v.publishedAt DESC")
    Page<Video> findFeaturedVideos(Pageable pageable);

    @Query("SELECT v FROM Video v WHERE v.user.id = :userId ORDER BY v.createdAt DESC")
    Page<Video> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT v FROM Video v WHERE LOWER(v.title) LIKE LOWER(CONCAT('%', :keyword, '%')) AND v.status = 'published' AND v.visibility = 'public' ORDER BY v.publishedAt DESC")
    Page<Video> searchPublishedVideos(@Param("keyword") String keyword, Pageable pageable);

    Optional<Video> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("UPDATE Video v SET v.viewCount = v.viewCount + 1 WHERE v.id = :videoId")
    void incrementViewCount(@Param("videoId") UUID videoId);

    @Modifying
    @Query("UPDATE Video v SET v.likeCount = v.likeCount + :delta WHERE v.id = :videoId")
    void incrementLikeCount(@Param("videoId") UUID videoId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE Video v SET v.commentCount = v.commentCount + :delta WHERE v.id = :videoId")
    void incrementCommentCount(@Param("videoId") UUID videoId, @Param("delta") int delta);

    Page<Video> findByCategoryIdInAndStatus(java.util.List<UUID> categoryIds, String status, Pageable pageable);

    Page<Video> findByVideoTagsTagIdInAndStatus(java.util.List<UUID> tagIds, String status, Pageable pageable);

    Page<Video> findByStatusAndVisibilityOrderByViewCountDescLikeCountDesc(String status, String visibility, Pageable pageable);

    Page<Video> findByCategoryIdAndStatus(UUID categoryId, String status, Pageable pageable);

    Page<Video> findByUserIdInAndStatusAndVisibilityOrderByPublishedAtDesc(
            List<UUID> userIds, String status, String visibility, Pageable pageable);

    @Query("SELECT l.video FROM Like l WHERE l.user.id = :userId ORDER BY l.createdAt DESC")
    Page<Video> findByUserLikesUserId(@Param("userId") UUID userId, Pageable pageable);

    Page<Video> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Video> findByTranscodeErrorIsNotNull(Pageable pageable);

    @Query("SELECT COALESCE(SUM(v.viewCount), 0) FROM Video v")
    long sumViewCount();

    @Query("SELECT COALESCE(SUM(v.likeCount), 0) FROM Video v")
    long sumLikeCount();
}
