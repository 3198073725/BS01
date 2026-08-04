package com.vidsprout.modules.config.repository;

import com.vidsprout.modules.config.model.ConfigEntry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConfigEntryRepository extends JpaRepository<ConfigEntry, UUID> {

    Optional<ConfigEntry> findByKeyIdAndContentTypeIdAndObjectId(UUID keyId, Long contentTypeId, String objectId);

    List<ConfigEntry> findByKeyId(UUID keyId);

    List<ConfigEntry> findByKeyIdAndIsActiveTrue(UUID keyId);

    List<ConfigEntry> findByContentTypeIdIsNullAndObjectIdIsNullAndIsActiveTrue();

    @EntityGraph(attributePaths = "key")
    List<ConfigEntry> findByKeyIdInAndContentTypeIdIsNullAndObjectIdIsNullAndIsActiveTrue(List<UUID> keyIds);
}
