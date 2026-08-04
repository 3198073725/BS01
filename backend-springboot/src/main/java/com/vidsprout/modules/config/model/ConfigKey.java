package com.vidsprout.modules.config.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "configs_key",
        uniqueConstraints = @UniqueConstraint(name = "uq_cfg_key_ns_key", columnNames = {"namespace_id", "\"key\""}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ConfigKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "namespace_id", nullable = false)
    private ConfigNamespace namespace;

    @Column(name = "\"key\"", nullable = false, length = 64)
    private String key;

    @Column(name = "value_type", length = 16)
    @Builder.Default
    private String valueType = "json";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_value")
    private String defaultValue;

    @Column(length = 255)
    private String description;

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
