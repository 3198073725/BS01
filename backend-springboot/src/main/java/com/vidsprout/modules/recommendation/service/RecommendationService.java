package com.vidsprout.modules.recommendation.service;

import com.vidsprout.modules.video.model.Video;
import com.vidsprout.modules.video.model.VideoTag;
import com.vidsprout.modules.video.repository.VideoRepository;
import com.vidsprout.modules.video.repository.VideoTagRepository;
import com.vidsprout.modules.user.model.User;
import com.vidsprout.modules.interaction.repository.LikeRepository;
import com.vidsprout.modules.interaction.repository.FollowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommendationService {

    private final VideoRepository videoRepository;
    private final VideoTagRepository videoTagRepository;
    private final LikeRepository likeRepository;
    private final FollowRepository followRepository;

    public RecommendationService(VideoRepository videoRepository, VideoTagRepository videoTagRepository,
                                 LikeRepository likeRepository, FollowRepository followRepository) {
        this.videoRepository = videoRepository;
        this.videoTagRepository = videoTagRepository;
        this.likeRepository = likeRepository;
        this.followRepository = followRepository;
    }

    public List<Video> getPersonalizedRecommendations(User user, int page, int limit) {
        if (user == null) {
            return getTrendingVideos(page, limit);
        }

        List<Video> recommendations = new ArrayList<>();

        Set<UUID> likedVideoIds = new HashSet<>(likeRepository.findLikedVideoIdsByUser(user.getId()));
        if (!likedVideoIds.isEmpty()) {
            Set<UUID> categoryIds = new HashSet<>();
            Set<UUID> tagIds = new HashSet<>();

            for (UUID vid : likedVideoIds) {
                Video v = videoRepository.findById(vid).orElse(null);
                if (v != null) {
                    if (v.getCategory() != null) {
                        categoryIds.add(v.getCategory().getId());
                    }
                    List<VideoTag> vts = videoTagRepository.findByVideoId(vid);
                    vts.forEach(vt -> {
                        if (vt.getTag() != null) {
                            tagIds.add(vt.getTag().getId());
                        }
                    });
                }
            }

            if (!categoryIds.isEmpty()) {
                List<Video> byCategory = videoRepository.findByCategoryIdInAndStatus(
                        new ArrayList<>(categoryIds), "published",
                        PageRequest.of(Math.max(0, page - 1), limit / 2, Sort.by(Sort.Direction.DESC, "viewCount"))
                ).getContent();
                recommendations.addAll(byCategory);
            }

            if (!tagIds.isEmpty()) {
                List<Video> byTags = videoRepository.findByVideoTagsTagIdInAndStatus(
                        new ArrayList<>(tagIds), "published",
                        PageRequest.of(Math.max(0, page - 1), limit / 2, Sort.by(Sort.Direction.DESC, "publishedAt"))
                ).getContent();
                recommendations.addAll(byTags);
            }
        }

        if (recommendations.size() < limit) {
            List<Video> trending = getTrendingVideos(page, limit - recommendations.size());
            for (Video v : trending) {
                if (recommendations.stream().noneMatch(r -> r.getId().equals(v.getId()))) {
                    recommendations.add(v);
                }
            }
        }

        return recommendations.stream()
                .filter(v -> !v.getUser().getId().equals(user.getId()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Video> getTrendingVideos(int page, int limit) {
        return videoRepository.findByStatusAndVisibilityOrderByViewCountDescLikeCountDesc(
                "published", "public",
                PageRequest.of(Math.max(0, page - 1), limit)
        ).getContent();
    }

    public List<Video> getRelatedVideos(UUID videoId, int limit) {
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null) {
            return getTrendingVideos(1, limit);
        }

        Set<UUID> tagIds = new HashSet<>();
        List<VideoTag> vts = videoTagRepository.findByVideoId(videoId);
        vts.forEach(vt -> {
            if (vt.getTag() != null) {
                tagIds.add(vt.getTag().getId());
            }
        });

        if (!tagIds.isEmpty()) {
            List<Video> byTags = videoRepository.findByVideoTagsTagIdInAndStatus(
                    new ArrayList<>(tagIds), "published",
                    PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "publishedAt"))
            ).getContent();

            return byTags.stream()
                    .filter(v -> !v.getId().equals(videoId))
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        if (video.getCategory() != null) {
            List<Video> byCategory = videoRepository.findByCategoryIdAndStatus(
                    video.getCategory().getId(), "published",
                    PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "publishedAt"))
            ).getContent();

            return byCategory.stream()
                    .filter(v -> !v.getId().equals(videoId))
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        return getTrendingVideos(1, limit);
    }

    public List<Video> getVideosLikedByUser(UUID userId) {
        return videoRepository.findByUserIdOrderByCreatedAtDesc(userId,
                PageRequest.of(0, 50)
        ).getContent();
    }

    public List<Video> getFollowingFeed(User user, int page, int limit) {
        if (user == null) {
            return getTrendingVideos(page, limit);
        }
        List<UUID> followedIds = followRepository.findFollowedIdsByFollowerId(user.getId());
        if (followedIds.isEmpty()) {
            return List.of();
        }
        return videoRepository.findByUserIdInAndStatusAndVisibilityOrderByPublishedAtDesc(
                followedIds, "published", "public", PageRequest.of(Math.max(0, page - 1), limit)).getContent();
    }
}
