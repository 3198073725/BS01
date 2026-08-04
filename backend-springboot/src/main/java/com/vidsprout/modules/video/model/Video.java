package com.vidsprout.modules.video.model;

import com.vidsprout.modules.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "videos_video")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String videoFile;

    @Column(length = 100)
    private String thumbnail;

    @Column(length = 200)
    private String videoFileF;

    @Column(length = 200)
    private String thumbnailF;

    @Builder.Default
    private Integer duration = 0;
    @Builder.Default
    private Integer width = 0;
    @Builder.Default
    private Integer height = 0;
    @Builder.Default
    private Long fileSize = 0L;

    @Builder.Default
    private Boolean allowComments = true;
    @Builder.Default
    private Boolean allowDownload = false;

    @Column(length = 150)
    private String lowMp4;
    @Column(columnDefinition = "TEXT")
    private String transcodeError;

    @Column(length = 20)
    @Builder.Default
    private String visibility = "public";

    @Column(length = 20)
    @Builder.Default
    private String status = "draft";

    @Column(length = 20)
    @Builder.Default
    private String uploadStatus = "pending";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private com.vidsprout.modules.content.model.Category category;

    @OneToMany(mappedBy = "video", fetch = FetchType.LAZY)
    private Set<VideoTag> videoTags;

    @Builder.Default
    private Long viewCount = 0L;
    @Builder.Default
    private Long likeCount = 0L;
    @Builder.Default
    private Long commentCount = 0L;

    @Builder.Default
    private Boolean isFeatured = false;

    private LocalDateTime publishedAt;
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
