package com.vidsprout.modules.video.repository;

import com.vidsprout.modules.video.model.Playlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {

    Page<Playlist> findByUserId(UUID userId, Pageable pageable);

    Page<Playlist> findByUserIdAndVisibility(UUID userId, String visibility, Pageable pageable);
}
