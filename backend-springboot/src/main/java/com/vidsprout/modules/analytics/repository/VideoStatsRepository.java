package com.vidsprout.modules.analytics.repository;

import com.vidsprout.modules.analytics.model.VideoStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VideoStatsRepository extends JpaRepository<VideoStats, UUID> {
}
