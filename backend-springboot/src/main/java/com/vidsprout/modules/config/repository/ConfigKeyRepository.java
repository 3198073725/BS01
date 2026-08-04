package com.vidsprout.modules.config.repository;

import com.vidsprout.modules.config.model.ConfigKey;
import com.vidsprout.modules.config.model.ConfigNamespace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConfigKeyRepository extends JpaRepository<ConfigKey, UUID> {

    Optional<ConfigKey> findByNamespaceIdAndKey(UUID namespaceId, String key);

    List<ConfigKey> findByNamespace(ConfigNamespace namespace);
}
