package com.vidsprout.modules.notification.model;

import com.vidsprout.modules.interaction.model.Notification;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_delivery")
@Check(constraints = "attempt_count >= 0")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "pending";

    @Builder.Default
    private Integer attemptCount = 0;

    private LocalDateTime lastAttemptAt;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
