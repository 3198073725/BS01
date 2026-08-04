package com.vidsprout.modules.config.repository;

import com.vidsprout.modules.config.model.ConfigNamespace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConfigNamespaceRepository extends JpaRepository<ConfigNamespace, UUID> {

    Optional<ConfigNamespace> findByName(String name);
}
