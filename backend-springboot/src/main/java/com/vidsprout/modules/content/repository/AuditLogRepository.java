package com.vidsprout.modules.content.repository;

import com.vidsprout.modules.content.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByActorId(UUID actorId, Pageable pageable);

    Page<AuditLog> findByVerb(String verb, Pageable pageable);

    Page<AuditLog> findByTargetTypeAndTargetId(String targetType, UUID targetId, Pageable pageable);
}
