package com.vidsprout.modules.video.repository;

import com.vidsprout.modules.video.model.VideoAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoAssetRepository extends JpaRepository<VideoAsset, UUID> {

    List<VideoAsset> findByVideoId(UUID videoId);

    Optional<VideoAsset> findByVideoIdAndKind(UUID videoId, String kind);
}
