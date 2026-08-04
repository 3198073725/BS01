package com.vidsprout.modules.video.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "videos_transcode",
        uniqueConstraints = @UniqueConstraint(name = "uq_transcode_video_profile", columnNames = {"video_id", "profile"}))
@Check(constraints = "segment_duration > 0")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VideoTranscode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(nullable = false, length = 50)
    private String profile;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "pending";

    private Integer width;

    private Integer height;

    private Integer bitrate;

    @Column(length = 50)
    private String codec;

    @Builder.Default
    private Integer segmentDuration = 6;

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
