package com.vidsprout.modules.interaction.model;

import com.vidsprout.modules.user.model.User;
import com.vidsprout.modules.video.model.Video;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interactions_like", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "video_id"}),
    @UniqueConstraint(columnNames = {"user_id", "comment_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Like {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private com.vidsprout.modules.interaction.model.Comment comment;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
