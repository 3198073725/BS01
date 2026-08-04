package com.vidsprout.modules.video.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "playlist_videos",
        uniqueConstraints = @UniqueConstraint(name = "uq_plv_playlist_video", columnNames = {"playlist_id", "video_id"}))
@Check(constraints = "position >= 0")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PlaylistVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Builder.Default
    private Integer position = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
