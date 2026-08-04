package com.vidsprout.modules.interaction.model;

import com.vidsprout.modules.user.model.User;
import com.vidsprout.modules.video.model.Video;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interactions_history", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "video_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class History {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;
    
    @Builder.Default
    private Integer watchDuration = 0;
    
    @Builder.Default
    private Double progress = 0.0;
    
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
