package com.vidsprout.modules.video.repository;

import com.vidsprout.modules.video.model.VideoSubtitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoSubtitleRepository extends JpaRepository<VideoSubtitle, UUID> {

    List<VideoSubtitle> findByVideoId(UUID videoId);

    Optional<VideoSubtitle> findByVideoIdAndLangAndFormat(UUID videoId, String lang, String format);
}
