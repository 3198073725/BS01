package com.vidsprout.modules.user.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 150)
    private String username;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 100)
    private String profilePicture;

    @Column(length = 200)
    private String profilePictureF;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 64)
    private String nickname;

    @Column(length = 10)
    @Builder.Default
    private String gender = "private";

    private LocalDate birthDate;

    @Column(length = 100)
    private String location;

    @Column(length = 200)
    private String website;

    @Column(length = 20, unique = true)
    private String phoneNumber;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean isStaff = false;

    @Builder.Default
    private Boolean isVerified = false;

    @Builder.Default
    private Boolean isEmailVerified = false;

    @Builder.Default
    private Boolean isCreator = false;

    @Column(length = 20)
    @Builder.Default
    private String privacyMode = "public";

    @Column(length = 20)
    @Builder.Default
    private String adminRole = "none";

    @Builder.Default
    private Integer followersCount = 0;
    @Builder.Default
    private Integer followingCount = 0;
    @Builder.Default
    private Integer videoCount = 0;
    @Builder.Default
    private Integer totalLikesReceived = 0;
    @Builder.Default
    private Long totalViewsReceived = 0L;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateJoined;

    private LocalDateTime lastActive;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        dateJoined = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
