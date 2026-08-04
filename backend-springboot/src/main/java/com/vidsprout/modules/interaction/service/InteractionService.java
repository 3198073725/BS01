package com.vidsprout.modules.interaction.service;

import com.vidsprout.common.exception.BusinessException;
import com.vidsprout.common.exception.ResourceNotFoundException;
import com.vidsprout.modules.content.model.Report;
import com.vidsprout.modules.content.repository.ReportRepository;
import com.vidsprout.modules.interaction.dto.CommentRequest;
import com.vidsprout.modules.interaction.dto.CommentResponse;
import com.vidsprout.modules.interaction.model.*;
import com.vidsprout.modules.interaction.repository.*;
import com.vidsprout.modules.user.model.User;
import com.vidsprout.modules.user.repository.UserRepository;
import com.vidsprout.modules.user.service.AuthService;
import com.vidsprout.modules.video.model.Video;
import com.vidsprout.modules.video.repository.VideoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class InteractionService {

    private final LikeRepository likeRepository;
    private final FavoriteRepository favoriteRepository;
    private final CommentRepository commentRepository;
    private final FollowRepository followRepository;
    private final WatchLaterRepository watchLaterRepository;
    private final HistoryRepository historyRepository;
    private final NotificationRepository notificationRepository;
    private final ReportRepository reportRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    public InteractionService(LikeRepository likeRepository,
                              FavoriteRepository favoriteRepository,
                              CommentRepository commentRepository,
                              FollowRepository followRepository,
                              WatchLaterRepository watchLaterRepository,
                              HistoryRepository historyRepository,
                              NotificationRepository notificationRepository,
                              ReportRepository reportRepository,
                              VideoRepository videoRepository,
                              UserRepository userRepository,
                              AuthService authService) {
        this.likeRepository = likeRepository;
        this.favoriteRepository = favoriteRepository;
        this.commentRepository = commentRepository;
        this.followRepository = followRepository;
        this.watchLaterRepository = watchLaterRepository;
        this.historyRepository = historyRepository;
        this.notificationRepository = notificationRepository;
        this.reportRepository = reportRepository;
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Transactional
    public Map<String, Object> follow(UUID targetUserId) {
        User user = authService.getCurrentUserEntity();
        if (user.getId().equals(targetUserId)) throw new BusinessException("不能关注自己");
        userRepository.findById(targetUserId).orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(user.getId(), targetUserId);
        if (isFollowing) return Map.of("ok", true, "message", "已关注");

        followRepository.save(Follow.builder().follower(user).following(
                userRepository.findById(targetUserId).orElseThrow()).build());
        return Map.of("ok", true, "message", "关注成功");
    }

    @Transactional
    public Map<String, Object> unfollow(UUID targetUserId) {
        User user = authService.getCurrentUserEntity();
        Follow follow = followRepository.findByFollowerIdAndFollowingId(user.getId(), targetUserId)
                .orElseThrow(() -> new BusinessException("未关注该用户"));
        followRepository.delete(follow);
        return Map.of("ok", true, "message", "已取消关注");
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFollowers(UUID userId, int page, int size) {
        UUID targetId = resolveTargetUserId(userId);
        return followRepository.findByFollowingIdOrderByCreatedAtDesc(targetId, org.springframework.data.domain.PageRequest.of(Math.max(0, page - 1), size))
                .map(f -> userInfoMap(f.getFollower())).getContent();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFollowing(UUID userId, int page, int size) {
        UUID targetId = resolveTargetUserId(userId);
        return followRepository.findByFollowerIdOrderByCreatedAtDesc(targetId, org.springframework.data.domain.PageRequest.of(Math.max(0, page - 1), size))
                .map(f -> userInfoMap(f.getFollowing())).getContent();
    }

    private UUID resolveTargetUserId(UUID userId) {
        if (userId != null) return userId;
        return authService.getCurrentUserEntity().getId();
    }

    public Map<String, Object> getRelationship(UUID targetUserId) {
        User user = authService.getCurrentUserEntity();
        boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(user.getId(), targetUserId);
        boolean isFollowed = followRepository.existsByFollowerIdAndFollowingId(targetUserId, user.getId());
        return Map.of("user_id", targetUserId.toString(), "following", isFollowing, "followed_by", isFollowed, "mutual", isFollowing && isFollowed);
    }

    @Transactional
    public Map<String, Object> toggleLike(UUID videoId) {
        User user = authService.getCurrentUserEntity();
        Optional<Like> existing = likeRepository.findByUserIdAndVideoId(user.getId(), videoId);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            videoRepository.incrementLikeCount(videoId, -1);
            return Map.of("ok", true, "liked", false);
        }
        videoRepository.findById(videoId).orElseThrow(() -> new ResourceNotFoundException("视频不存在"));
        Like like = Like.builder().user(user).build();
        like.setVideo(videoRepository.findById(videoId).orElseThrow());
        likeRepository.save(like);
        videoRepository.incrementLikeCount(videoId, 1);
        return Map.of("ok", true, "liked", true);
    }

    @Transactional
    public Map<String, Object> toggleFavorite(UUID videoId) {
        User user = authService.getCurrentUserEntity();
        Optional<Favorite> existing = favoriteRepository.findByUserIdAndVideoId(user.getId(), videoId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return Map.of("ok", true, "favorited", false);
        }
        Video video = videoRepository.findById(videoId).orElseThrow(() -> new ResourceNotFoundException("视频不存在"));
        favoriteRepository.save(Favorite.builder().user(user).video(video).build());
        return Map.of("ok", true, "favorited", true);
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getLikes(UUID userId, int page, int size) {
        UUID targetId = resolveTargetUserId(userId);
        return likeRepository.findLikedVideosByUser(targetId, org.springframework.data.domain.PageRequest.of(Math.max(0, page - 1), size))
                .map(l -> videoCardMap(l.getVideo(), Map.of("liked_at", l.getCreatedAt())));
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getFavorites(UUID userId, int page, int size) {
        UUID targetId = resolveTargetUserId(userId);
        return favoriteRepository.findFavoritedVideosByUser(targetId, org.springframework.data.domain.PageRequest.of(Math.max(0, page - 1), size))
                .map(f -> videoCardMap(f.getVideo(), Map.of("saved_at", f.getCreatedAt())));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(UUID videoId, int page, int size) {
        return commentRepository.findByVideoIdAndParentIsNullOrderByCreatedAtDesc(videoId, org.springframework.data.domain.PageRequest.of(Math.max(0, page - 1), size))
                .map(this::toCommentResponse).getContent();
    }

    @Transactional
    public CommentResponse createComment(CommentRequest request) {
        User user = authService.getCurrentUserEntity();
        Video video = videoRepository.findById(request.getVideoId())
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在"));

        Comment.CommentBuilder builder = Comment.builder()
                .content(request.getContent()).user(user).video(video);

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("父评论不存在"));
            builder.parent(parent);
            parent.setReplyCount(parent.getReplyCount() + 1);
            commentRepository.save(parent);
        }

        Comment comment = commentRepository.save(builder.build());
        videoRepository.incrementCommentCount(video.getId(), 1);
        return toCommentResponse(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getReplies(UUID parentId, int page, int size) {
        return commentRepository.findByParentIdOrderByCreatedAtAsc(parentId, org.springframework.data.domain.PageRequest.of(Math.max(0, page - 1), size))
                .map(this::toCommentResponse).getContent();
    }

    @Transactional
    public void deleteComment(UUID commentId) {
        User user = authService.getCurrentUserEntity();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("评论不存在"));
        if (!comment.getUser().getId().equals(user.getId())
                && !Set.of("admin", "super_admin", "moderator").contains(user.getAdminRole())) {
            throw new BusinessException("无权删除此评论");
        }
        comment.setIsDeleted(true);
        comment.setContent("[评论已删除]");
        commentRepository.save(comment);
    }

    @Transactional
    public Map<String, Object> toggleCommentLike(UUID commentId) {
        User user = authService.getCurrentUserEntity();
        Optional<Like> existing = likeRepository.findByUserIdAndCommentId(user.getId(), commentId);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            return Map.of("ok", true, "liked", false);
        }
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("评论不存在"));
        Like like = Like.builder().user(user).comment(comment).build();
        likeRepository.save(like);
        comment.setLikeCount(comment.getLikeCount() + 1);
        commentRepository.save(comment);
        return Map.of("ok", true, "liked", true);
    }

    public Map<String, Object> createReport(Map<String, Object> body) {
        User user = authService.getCurrentUserEntity();
        String targetType = body.get("target_type") != null ? body.get("target_type").toString() : null;
        UUID targetId = body.get("target_id") != null ? parseUuid(body.get("target_id")) : null;
        if (targetType == null || targetId == null) {
            throw new BusinessException("缺少举报目标类型或ID");
        }
        String reason = body.get("reason_code") != null ? body.get("reason_code").toString() : "other";
        String description = body.get("description") != null ? body.get("description").toString() : "";
        if (!Set.of("video", "comment", "user").contains(targetType)) {
            throw new BusinessException("非法的举报类型");
        }
        reportRepository.save(Report.builder()
                .reporter(user)
                .targetType(targetType)
                .targetId(targetId)
                .reason(reason)
                .description(description)
                .build());
        return Map.of("ok", true, "message", "举报已提交");
    }

    private UUID parseUuid(Object obj) {
        if (obj == null) return null;
        if (obj instanceof UUID) return (UUID) obj;
        try { return UUID.fromString(obj.toString()); }
        catch (IllegalArgumentException e) { return null; }
    }

    public Set<UUID> getLikedVideoIds(UUID userId, List<UUID> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) return Collections.emptySet();
        return likeRepository.findLikedVideoIds(userId, videoIds);
    }

    public Set<UUID> getFavoritedVideoIds(UUID userId, List<UUID> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) return Collections.emptySet();
        return favoriteRepository.findFavoritedVideoIds(userId, videoIds);
    }

    @Transactional
    public Map<String, Object> toggleWatchLater(UUID videoId) {
        User user = authService.getCurrentUserEntity();
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在"));
        Optional<WatchLater> existing = watchLaterRepository.findByUserIdAndVideoId(user.getId(), videoId);
        if (existing.isPresent()) {
            watchLaterRepository.delete(existing.get());
            return Map.of("ok", true, "saved", false);
        }
        watchLaterRepository.save(WatchLater.builder().user(user).video(video).build());
        return Map.of("ok", true, "saved", true);
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getWatchLaterList(UUID userId, int page, int size) {
        UUID targetId = resolveTargetUserId(userId);
        return watchLaterRepository.findByUserIdOrderByCreatedAtDesc(targetId, org.springframework.data.domain.PageRequest.of(Math.max(0, page - 1), size))
                .map(wl -> videoCardMap(wl.getVideo(), Map.of("saved_at", wl.getCreatedAt())));
    }

    @Transactional
    public Map<String, Object> recordHistory(UUID videoId, Integer watchDuration, Double progress) {
        User user = authService.getCurrentUserEntity();
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在"));
        History history = historyRepository.findByUserIdAndVideoId(user.getId(), videoId).orElse(null);
        if (history == null) {
            history = History.builder().user(user).video(video).watchDuration(watchDuration != null ? watchDuration : 0)
                    .progress(progress != null ? progress : 0.0).build();
            historyRepository.save(history);
        } else {
            history.setWatchDuration(watchDuration != null ? watchDuration : history.getWatchDuration());
            history.setProgress(progress != null ? progress : history.getProgress());
            historyRepository.save(history);
        }
        return Map.of("ok", true, "progress", history.getProgress(), "watch_duration", history.getWatchDuration());
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getHistoryList(UUID userId, int page, int size) {
        UUID targetId = resolveTargetUserId(userId);
        return historyRepository.findByUserIdWithVideo(targetId, org.springframework.data.domain.PageRequest.of(Math.max(0, page - 1), size))
                .map(h -> videoCardMap(h.getVideo(), Map.of(
                        "watched_at", h.getCreatedAt(),
                        "progress", h.getProgress(),
                        "watch_duration", h.getWatchDuration())));
    }

    @Transactional
    public Map<String, Object> bulkRemoveHistory(List<UUID> videoIds) {
        User user = authService.getCurrentUserEntity();
        historyRepository.deleteByUserIdAndVideoIdIn(user.getId(), videoIds);
        return Map.of("ok", true, "removed", videoIds.size());
    }

    @Transactional
    public Map<String, Object> bulkRemoveLikes(List<UUID> videoIds) {
        User user = authService.getCurrentUserEntity();
        List<Like> likes = likeRepository.findByUserIdAndVideoIdIn(user.getId(), videoIds);
        likes.forEach(like -> videoRepository.incrementLikeCount(like.getVideo().getId(), -1));
        likeRepository.deleteAll(likes);
        return Map.of("ok", true, "removed", likes.size());
    }

    @Transactional
    public Map<String, Object> bulkRemoveFavorites(List<UUID> videoIds) {
        User user = authService.getCurrentUserEntity();
        List<Favorite> favorites = favoriteRepository.findByUserIdAndVideoIdIn(user.getId(), videoIds);
        favoriteRepository.deleteAll(favorites);
        return Map.of("ok", true, "removed", favorites.size());
    }

    @Transactional
    public Map<String, Object> bulkRemoveWatchLater(List<UUID> videoIds) {
        User user = authService.getCurrentUserEntity();
        List<WatchLater> watchLater = watchLaterRepository.findByUserIdAndVideoIdIn(user.getId(), videoIds);
        watchLaterRepository.deleteAll(watchLater);
        return Map.of("ok", true, "removed", watchLater.size());
    }

    @Transactional(readOnly = true)
    public List<? extends Map<String, Object>> getNotifications(int page, int size, Boolean unread) {
        User user = authService.getCurrentUserEntity();
        Page<Notification> notifications;
        var pr = PageRequest.of(Math.max(0, page - 1), size);
        if (Boolean.TRUE.equals(unread)) {
            notifications = notificationRepository.findByUserIdWithAssociations(user.getId(), false, false, pr);
        } else {
            notifications = notificationRepository.findByUserIdWithAssociationsAll(user.getId(), false, pr);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Notification n : notifications) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", n.getId());
            data.put("type", n.getVerb());
            data.put("verb", n.getVerb());
            data.put("read", n.getRead());
            data.put("created_at", n.getCreatedAt());
            data.put("actor", actorPayload(n.getActor()));
            data.put("video", videoPayload(n.getVideo()));
            data.put("comment", commentPayload(n.getComment()));
            result.add(data);
        }
        return result;
    }

    private Map<String, Object> actorPayload(User actor) {
        if (actor == null) return null;
        Map<String, Object> m = new HashMap<>();
        m.put("id", actor.getId());
        m.put("username", actor.getUsername() != null ? actor.getUsername() : "");
        m.put("nickname", actor.getNickname() != null ? actor.getNickname() : "");
        m.put("display_name", actor.getNickname() != null ? actor.getNickname() : actor.getUsername());
        m.put("avatar_url", actor.getProfilePictureF());
        return m;
    }

    private Map<String, Object> videoPayload(Video video) {
        if (video == null) return null;
        Map<String, Object> m = new HashMap<>();
        m.put("id", video.getId());
        m.put("title", video.getTitle() != null ? video.getTitle() : "");
        m.put("thumbnail_url", video.getThumbnailF());
        return m;
    }

    private Map<String, Object> commentPayload(Comment comment) {
        if (comment == null) return null;
        Map<String, Object> m = new HashMap<>();
        m.put("id", comment.getId());
        m.put("content", comment.getContent() != null ? comment.getContent() : "");
        return m;
    }

    @Transactional
    public Map<String, Object> markAllNotificationsRead() {
        User user = authService.getCurrentUserEntity();
        int count = notificationRepository.markAllAsRead(user.getId());
        return Map.of("updated", count);
    }

    @Transactional
    public Map<String, Object> markNotificationsRead(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of("updated", 0);
        }
        User user = authService.getCurrentUserEntity();
        int count = notificationRepository.markByIdsAsRead(user.getId(), ids);
        return Map.of("updated", count);
    }

    @Transactional
    public Map<String, Object> clearAllNotifications() {
        User user = authService.getCurrentUserEntity();
        int count = notificationRepository.clearAll(user.getId());
        return Map.of("updated", count);
    }

    public Map<String, Object> getUnreadNotificationCount() {
        User user = authService.getCurrentUserEntity();
        long count = notificationRepository.countByUserIdAndReadAndHidden(user.getId(), false, false);
        return Map.of("unread", count);
    }

    private CommentResponse toCommentResponse(Comment comment) {
        return toCommentResponse(comment, false);
    }

    private CommentResponse toCommentResponse(Comment comment, boolean withReplies) {
        User viewer = authService.getCurrentUserEntityOrNull();
        boolean liked = viewer != null && likeRepository.existsByUserIdAndCommentId(viewer.getId(), comment.getId());
        CommentResponse.CommentResponseBuilder builder = CommentResponse.builder()
                .id(comment.getId()).content(comment.getContent())
                .user(commentUserPayload(comment.getUser()))
                .video(comment.getVideo() != null ? comment.getVideo().getId() : null)
                .parent(comment.getParent() != null ? comment.getParent().getId() : null)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .repliesCount(comment.getReplyCount() != null ? comment.getReplyCount() : 0)
                .isLiked(liked)
                .likeCount(comment.getLikeCount() != null ? comment.getLikeCount() : 0);
        if (withReplies && comment.getReplies() != null) {
            builder.replies(comment.getReplies().stream()
                    .map(r -> toCommentResponse(r, false))
                    .collect(Collectors.toList()));
        }
        return builder.build();
    }

    private Map<String, Object> commentUserPayload(User u) {
        if (u == null) return null;
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername() != null ? u.getUsername() : "");
        m.put("display_name", u.getNickname() != null ? u.getNickname() : u.getUsername());
        m.put("avatar_url", u.getProfilePictureF());
        return m;
    }

    private Map<String, Object> userInfoMap(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("nickname", u.getNickname() != null ? u.getNickname() : u.getUsername());
        m.put("display_name", u.getNickname() != null ? u.getNickname() : u.getUsername());
        m.put("bio", u.getBio() != null ? u.getBio() : "");
        m.put("profile_picture", u.getProfilePictureF() != null ? u.getProfilePictureF() : "");
        m.put("profilePicture", u.getProfilePictureF() != null ? u.getProfilePictureF() : "");
        m.put("avatar_url", u.getProfilePictureF() != null ? u.getProfilePictureF() : "");
        m.put("is_following", Boolean.FALSE);
        m.put("is_mutual", Boolean.FALSE);
        m.put("followers_count", u.getFollowersCount() != null ? u.getFollowersCount() : 0);
        m.put("following_count", u.getFollowingCount() != null ? u.getFollowingCount() : 0);
        m.put("video_count", u.getVideoCount() != null ? u.getVideoCount() : 0);
        return m;
    }

    private Map<String, Object> videoCardMap(Video v, Map<String, Object> extra) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", v.getId());
        m.put("video_id", v.getId());
        m.put("title", v.getTitle());
        m.put("status", v.getStatus());
        m.put("transcode_error", v.getTranscodeError());
        String base = "/media/";
        String thumb = v.getThumbnailF() != null ? base + v.getThumbnailF() : null;
        m.put("cover", thumb);
        m.put("thumbnail_url", thumb);
        m.put("thumbnailUrl", thumb);
        m.put("views", v.getViewCount());
        m.put("view_count", v.getViewCount());
        m.put("likes", v.getLikeCount());
        m.put("like_count", v.getLikeCount());
        m.put("favorites", favoriteRepository.countByVideoId(v.getId()));
        m.put("favorite_count", favoriteRepository.countByVideoId(v.getId()));
        m.put("comments", v.getCommentCount());
        m.put("comment_count", v.getCommentCount());
        m.put("duration", v.getDuration());
        m.put("video_url", v.getVideoFileF() != null ? base + v.getVideoFileF() : null);
        m.put("hls_master_url", v.getVideoFile() != null ? base + "videos/hls/" + v.getId() + "/master.m3u8" : null);
        m.put("low_mp4_url", v.getLowMp4() != null ? base + v.getLowMp4() : null);
        m.put("published_at", v.getPublishedAt());
        m.put("created_at", v.getCreatedAt());
        m.put("author", actorPayload(v.getUser()));
        if (extra != null) m.putAll(extra);
        return m;
    }
}
