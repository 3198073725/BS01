package com.vidsprout.modules.video.repository;

import com.vidsprout.modules.video.model.PlaylistVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlaylistVideoRepository extends JpaRepository<PlaylistVideo, UUID> {

    List<PlaylistVideo> findByPlaylistIdOrderByPositionAsc(UUID playlistId);

    Optional<PlaylistVideo> findByPlaylistIdAndVideoId(UUID playlistId, UUID videoId);

    long countByPlaylistId(UUID playlistId);
}
