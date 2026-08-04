package com.vidsprout.modules.video.repository;

import com.vidsprout.modules.video.model.VideoTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoTagRepository extends JpaRepository<VideoTag, UUID> {
    
    List<VideoTag> findByVideoId(UUID videoId);
    
    @Query("SELECT vt FROM VideoTag vt JOIN FETCH vt.tag WHERE vt.video.id = :videoId")
    List<VideoTag> findByVideoIdWithTags(@Param("videoId") UUID videoId);
    
    void deleteByVideoId(UUID videoId);

    void deleteByVideoIdAndTagId(UUID videoId, UUID tagId);
    
    @Query("SELECT vt.tag.id FROM VideoTag vt WHERE vt.video.id = :videoId")
    List<UUID> findTagIdsByVideoId(@Param("videoId") UUID videoId);

    List<VideoTag> findByTagId(UUID tagId);

    long countByTagId(UUID tagId);
}
