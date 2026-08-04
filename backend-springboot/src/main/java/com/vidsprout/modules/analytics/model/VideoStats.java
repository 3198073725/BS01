package com.vidsprout.modules.analytics.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Immutable
@Table(name = "mv_video_stats")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VideoStats {

    @Id
    @Column(name = "video_id")
    private UUID videoId;

    @Builder.Default
    private Long viewCount = 0L;

    @Builder.Default
    private Long likeCount = 0L;

    @Builder.Default
    private Long commentCount = 0L;

    @Builder.Default
    private Long uniqueLikes = 0L;

    @Builder.Default
    private Long uniqueComments = 0L;

    @Builder.Default
    private Double avgCompletionRate = 0.0;
}
