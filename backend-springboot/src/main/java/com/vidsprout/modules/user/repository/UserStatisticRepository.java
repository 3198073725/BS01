package com.vidsprout.modules.user.repository;

import com.vidsprout.modules.user.model.UserStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserStatisticRepository extends JpaRepository<UserStatistic, UUID> {

    Optional<UserStatistic> findByUserIdAndDate(UUID userId, LocalDate date);

    boolean existsByUserIdAndDate(UUID userId, LocalDate date);
}
