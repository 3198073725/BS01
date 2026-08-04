package com.vidsprout.modules.content.repository;

import com.vidsprout.modules.content.model.ModerationAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModerationActionRepository extends JpaRepository<ModerationAction, UUID> {

    List<ModerationAction> findByReportId(UUID reportId);

    List<ModerationAction> findByReportIdOrderByCreatedAtDesc(UUID reportId);
}
