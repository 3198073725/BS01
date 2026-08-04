package com.vidsprout.modules.content.repository;

import com.vidsprout.modules.content.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByNameContainingIgnoreCase(String name);

    Optional<Category> findByNameIgnoreCase(String name);
}
