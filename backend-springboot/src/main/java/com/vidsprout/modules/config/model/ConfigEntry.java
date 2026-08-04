package com.vidsprout.modules.config.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "configs_entry",
        uniqueConstraints = @UniqueConstraint(name = "uq_cfg_entry_scope", columnNames = {"key_id", "content_type_id", "object_id"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ConfigEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "key_id", nullable = false)
    private ConfigKey key;

    @Column(name = "content_type_id")
    private Long contentTypeId;

    @Column(name = "object_id", length = 64)
    private String objectId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"value\"")
    private String value;

    @Builder.Default
    private Boolean isActive = true;

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
