package com.vidsprout.modules.video.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "videos_asset")
@Check(constraints = "kind IN ('thumbnail','sprite','gif','cover','watermark')")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VideoAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(nullable = false, length = 20)
    private String kind;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
