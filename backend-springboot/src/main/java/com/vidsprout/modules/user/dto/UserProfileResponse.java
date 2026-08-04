package com.vidsprout.modules.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private UUID id;
    private String username;
    private String email;
    private String nickname;
    private String bio;
    private String profilePicture;
    private String profilePictureUrl;
    private String gender;
    private String location;
    private String website;
    private Boolean isVerified;
    private Boolean isEmailVerified;
    private Boolean isCreator;
    private String privacyMode;
    private String adminRole;
    private Integer followersCount;
    private Integer followingCount;
    private Integer videoCount;
    private Integer totalLikesReceived;
    private Long totalViewsReceived;
    private LocalDateTime dateJoined;
    private LocalDateTime lastActive;
}
