package com.vidsprout.modules.video.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "videos_subtitle",
        uniqueConstraints = @UniqueConstraint(name = "uq_subtitle_video_lang_format", columnNames = {"video_id", "lang", "format"}))
@Check(constraints = "status IN ('pending','processing','ready','failed')")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VideoSubtitle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(nullable = false, length = 16)
    private String lang;

    @Column(nullable = false, length = 16)
    private String format;

    @Column(columnDefinition = "TEXT")
    private String textContent;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ready";

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
