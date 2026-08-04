package com.vidsprout.modules.user.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users_user_statistic",
        uniqueConstraints = @UniqueConstraint(name = "unique_user_date_stat", columnNames = {"user_id", "date"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Builder.Default
    private Integer newFollowers = 0;

    @Builder.Default
    private Integer newFollowing = 0;

    @Builder.Default
    private Integer likesReceived = 0;

    @Builder.Default
    private Integer commentsReceived = 0;

    @Builder.Default
    private Integer sharesReceived = 0;

    @Builder.Default
    private Integer videosUploaded = 0;

    @Builder.Default
    private Long totalViews = 0L;

    @Builder.Default
    private Long watchTime = 0L;

    @Builder.Default
    private Integer loginCount = 0;

    @Builder.Default
    private Integer activeDays = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
