package com.vidsprout.modules.video.repository;

import com.vidsprout.modules.video.model.VideoTranscode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoTranscodeRepository extends JpaRepository<VideoTranscode, UUID> {

    List<VideoTranscode> findByVideoId(UUID videoId);

    Optional<VideoTranscode> findByVideoIdAndProfile(UUID videoId, String profile);

    List<VideoTranscode> findByStatusIn(List<String> statuses);
}
