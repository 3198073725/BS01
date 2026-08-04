package com.vidsprout.modules.admin.service;

import com.vidsprout.common.exception.BusinessException;
import com.vidsprout.modules.content.model.Report;
import com.vidsprout.modules.content.model.Tag;
import com.vidsprout.modules.content.model.Category;
import com.vidsprout.modules.content.model.AuditLog;
import com.vidsprout.modules.content.model.ModerationAction;
import com.vidsprout.modules.content.repository.*;
import com.vidsprout.modules.interaction.model.Comment;
import com.vidsprout.modules.interaction.model.Notification;
import com.vidsprout.modules.interaction.repository.CommentRepository;
import com.vidsprout.modules.interaction.repository.NotificationRepository;
import com.vidsprout.modules.notification.model.SystemAnnouncement;
import com.vidsprout.modules.notification.repository.AnnouncementRepository;
import com.vidsprout.modules.user.model.User;
import com.vidsprout.modules.user.repository.UserRepository;
import com.vidsprout.modules.user.service.AuthService;
import com.vidsprout.modules.video.model.Video;
import com.vidsprout.modules.video.model.VideoTag;
import com.vidsprout.modules.video.repository.VideoRepository;
import com.vidsprout.modules.video.repository.VideoTagRepository;
import com.vidsprout.security.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;
    private final AnnouncementRepository announcementRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final VideoTagRepository videoTagRepository;
    private final ReportRepository reportRepository;
    private final AuditLogRepository auditLogRepository;
    private final ModerationActionRepository moderationActionRepository;
    private final NotificationRepository notificationRepository;
    private final AuthService authService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    public AdminService(UserRepository userRepository, VideoRepository videoRepository,
                        CommentRepository commentRepository, AnnouncementRepository announcementRepository,
                        CategoryRepository categoryRepository, TagRepository tagRepository,
                        VideoTagRepository videoTagRepository, ReportRepository reportRepository,
                        AuditLogRepository auditLogRepository, ModerationActionRepository moderationActionRepository,
                        NotificationRepository notificationRepository, AuthService authService,
                        JwtUtils jwtUtils, PasswordEncoder passwordEncoder,
                        RedisTemplate<String, String> redisTemplate) {
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
        this.commentRepository = commentRepository;
        this.announcementRepository = announcementRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.videoTagRepository = videoTagRepository;
        this.reportRepository = reportRepository;
        this.auditLogRepository = auditLogRepository;
        this.moderationActionRepository = moderationActionRepository;
        this.notificationRepository = notificationRepository;
        this.authService = authService;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
    }

    private static final Set<String> REVIEWER_UP = Set.of("reviewer", "moderator", "admin", "super_admin");
    private static final Set<String> MODERATOR_UP = Set.of("moderator", "admin", "super_admin");
    private static final Set<String> ADMIN_UP = Set.of("admin", "super_admin");
    private static final Set<String> SUPER_ADMIN = Set.of("super_admin");

    private User currentUser() {
        return authService.getCurrentUserEntity();
    }

    private void requireRole(Set<String> allowed) {
        String role = currentUser().getAdminRole() != null ? currentUser().getAdminRole() : "";
        if (!allowed.contains(role)) {
            throw new BusinessException("权限不足");
        }
    }

    private void requireReviewer() { requireRole(REVIEWER_UP); }
    private void requireModerator() { requireRole(MODERATOR_UP); }
    private void requireAdmin() { requireRole(ADMIN_UP); }
    private void requireSuperAdmin() { requireRole(SUPER_ADMIN); }

    // ==================== ME ====================

    public Map<String, Object> adminMe() {
        User u = currentUser();
        return Map.of("id", u.getId(), "username", u.getUsername(),
                "is_staff", u.getIsStaff() != null && u.getIsStaff(),
                "admin_role", u.getAdminRole() != null ? u.getAdminRole() : "");
    }

    // ==================== ANALYTICS OVERVIEW ====================

    public Map<String, Object> analyticsOverview() {
        requireReviewer();
        return Map.of(
                "totalUsers", userRepository.count(),
                "totalVideos", videoRepository.count(),
                "totalComments", commentRepository.count(),
                "todayUploads", 0
        );
    }

    // ==================== USERS ====================

    public Page<Map<String, Object>> getUsers(String search, int page, int size) {
        requireReviewer();
        var pr = PageRequest.of(Math.max(0, page - 1), size);
        Page<User> users = search.isEmpty() ? userRepository.findActiveUsers(pr) : userRepository.searchUsers(search, pr);
        return users.map(this::userListMap);
    }

    private Map<String, Object> userListMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("email", u.getEmail());
        m.put("nickname", u.getNickname());
        m.put("profile_picture", u.getProfilePictureF());
        m.put("is_active", u.getIsActive());
        m.put("is_verified", u.getIsVerified());
        m.put("is_creator", u.getIsCreator());
        m.put("is_staff", u.getIsStaff());
        m.put("admin_role", u.getAdminRole());
        m.put("followers_count", u.getFollowersCount());
        m.put("following_count", u.getFollowingCount());
        m.put("video_count", u.getVideoCount());
        m.put("date_joined", u.getDateJoined());
        m.put("last_active", u.getLastActive());
        return m;
    }

    public Map<String, Object> getUserDetail(UUID id) {
        requireReviewer();
        User u = userRepository.findById(id).orElseThrow(() -> new BusinessException("用户不存在"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("email", u.getEmail());
        m.put("nickname", u.getNickname());
        m.put("bio", u.getBio());
        m.put("is_active", u.getIsActive());
        m.put("is_verified", u.getIsVerified());
        m.put("is_creator", u.getIsCreator());
        m.put("is_staff", u.getIsStaff());
        m.put("admin_role", u.getAdminRole());
        m.put("date_joined", u.getDateJoined());
        m.put("last_active", u.getLastActive());
        return m;
    }

    @Transactional
    public Map<String, Object> updateUser(UUID id, Map<String, Object> updates) {
        requireAdmin();
        User u = userRepository.findById(id).orElseThrow(() -> new BusinessException("用户不存在"));
        if (updates.containsKey("isActive") || updates.containsKey("is_active"))
            u.setIsActive((Boolean) (updates.containsKey("isActive") ? updates.get("isActive") : updates.get("is_active")));
        if (updates.containsKey("isVerified") || updates.containsKey("is_verified"))
            u.setIsVerified((Boolean) (updates.containsKey("isVerified") ? updates.get("isVerified") : updates.get("is_verified")));
        if (updates.containsKey("isCreator") || updates.containsKey("is_creator"))
            u.setIsCreator((Boolean) (updates.containsKey("isCreator") ? updates.get("isCreator") : updates.get("is_creator")));
        if (updates.containsKey("adminRole") || updates.containsKey("admin_role")) {
            requireSuperAdmin();
            Object role = updates.containsKey("adminRole") ? updates.get("adminRole") : updates.get("admin_role");
            u.setAdminRole(role != null ? role.toString() : null);
        }
        if (updates.containsKey("isStaff") || updates.containsKey("is_staff")) {
            requireSuperAdmin();
            u.setIsStaff((Boolean) (updates.containsKey("isStaff") ? updates.get("isStaff") : updates.get("is_staff")));
        }
        if (updates.containsKey("nickname")) u.setNickname(updates.get("nickname").toString());
        if (updates.containsKey("bio")) u.setBio(updates.get("bio").toString());
        userRepository.save(u);
        return userListMap(u);
    }

    public Map<String, Object> forceLogout(UUID id) {
        requireAdmin();
        long now = System.currentTimeMillis() / 1000;
        redisTemplate.opsForValue().set("logout_after:" + id, String.valueOf(now), Duration.ofDays(7));
        return Map.of("ok", true, "message", "已强制下线");
    }

    // ==================== VIDEOS ====================

    public Page<Map<String, Object>> getVideosList(String search, String status, int page, int size) {
        requireReviewer();
        Page<Video> p = videoRepository.findAll(PageRequest.of(Math.max(0, page - 1), size));
        return p.map(v -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", v.getId().toString());
            m.put("title", v.getTitle());
            m.put("status", v.getStatus());
            m.put("visibility", v.getVisibility());
            m.put("view_count", v.getViewCount());
            m.put("like_count", v.getLikeCount());
            m.put("comment_count", v.getCommentCount());
            m.put("is_featured", v.getIsFeatured());
            m.put("thumbnail_url", v.getThumbnailF() != null ? "/media/" + v.getThumbnailF() : null);
            m.put("allow_comments", v.getAllowComments());
            m.put("allow_download", v.getAllowDownload());
            m.put("transcode_error", v.getTranscodeError());
            m.put("created_at", v.getCreatedAt());
            m.put("updated_at", v.getUpdatedAt());
            m.put("published_at", v.getPublishedAt());
            if (v.getUser() != null) {
                Map<String, Object> owner = new LinkedHashMap<>();
                owner.put("id", v.getUser().getId().toString());
                owner.put("username", v.getUser().getUsername());
                owner.put("nickname", v.getUser().getNickname());
                owner.put("avatar_url", v.getUser().getProfilePictureF());
                owner.put("is_verified", v.getUser().getIsVerified());
                m.put("owner", owner);
                m.put("owner_id", v.getUser().getId().toString());
                m.put("owner_username", v.getUser().getUsername());
            }
            if (v.getCategory() != null) {
                m.put("category", Map.of("id", v.getCategory().getId().toString(), "name", v.getCategory().getName()));
            }
            List<VideoTag> vts = videoTagRepository.findByVideoIdWithTags(v.getId());
            if (!vts.isEmpty()) {
                m.put("tags", vts.stream().map(vt -> Map.<String, Object>of("id", vt.getTag().getId().toString(), "name", vt.getTag().getName())).collect(Collectors.toList()));
            }
            return m;
        });
    }

    public Map<String, Object> getVideoDetail(UUID id) {
        requireReviewer();
        Video v = videoRepository.findById(id).orElseThrow(() -> new BusinessException("视频不存在"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", v.getId());
        m.put("title", v.getTitle());
        m.put("status", v.getStatus());
        m.put("visibility", v.getVisibility());
        m.put("description", v.getDescription());
        m.put("view_count", v.getViewCount());
        m.put("like_count", v.getLikeCount());
        m.put("comment_count", v.getCommentCount());
        m.put("is_featured", v.getIsFeatured());
        m.put("allow_comments", v.getAllowComments());
        m.put("allow_download", v.getAllowDownload());
        m.put("transcode_error", v.getTranscodeError());
        m.put("created_at", v.getCreatedAt());
        m.put("published_at", v.getPublishedAt());
        if (v.getUser() != null) {
            m.put("author", Map.of("id", v.getUser().getId(), "username", v.getUser().getUsername(),
                    "nickname", v.getUser().getNickname()));
        }
        if (v.getCategory() != null) {
            m.put("category", Map.of("id", v.getCategory().getId(), "name", v.getCategory().getName()));
        }
        return m;
    }

    @Transactional
    public Map<String, Object> batchApprove(Map<String, Object> body) {
        requireReviewer();
        @SuppressWarnings("unchecked")
        List<String> rawIds = (List<String>) body.getOrDefault("video_ids", body.getOrDefault("ids", List.of()));
        List<UUID> videoIds = rawIds.stream().map(UUID::fromString).toList();
        videoRepository.findAllById(videoIds).forEach(v -> {
            v.setStatus("published");
            v.setPublishedAt(LocalDateTime.now());
            videoRepository.save(v);
        });
        return Map.of("ok", true, "count", videoIds.size());
    }

    @Transactional
    public Map<String, Object> bulkUpdate(Map<String, Object> body) {
        requireReviewer();
        @SuppressWarnings("unchecked")
        List<String> rawIds = (List<String>) body.getOrDefault("video_ids", body.getOrDefault("ids", List.of()));
        List<UUID> ids = rawIds.stream().map(UUID::fromString).toList();
        videoRepository.findAllById(ids).forEach(v -> {
            if (body.containsKey("status")) v.setStatus((String) body.get("status"));
            if (body.containsKey("visibility")) v.setVisibility((String) body.get("visibility"));
            if (body.containsKey("isFeatured") || body.containsKey("is_featured")) {
                Boolean val = (Boolean) (body.containsKey("isFeatured") ? body.get("isFeatured") : body.get("is_featured"));
                v.setIsFeatured(val);
            }
            videoRepository.save(v);
        });
        return Map.of("ok", true);
    }

    @Transactional
    public Map<String, Object> bulkDelete(Map<String, Object> body) {
        requireModerator();
        @SuppressWarnings("unchecked")
        List<String> rawIds = (List<String>) body.getOrDefault("video_ids", body.getOrDefault("ids", List.of()));
        List<UUID> videoIds = rawIds.stream().map(UUID::fromString).toList();
        videoRepository.deleteAllById(videoIds);
        return Map.of("ok", true, "count", videoIds.size());
    }

    @Transactional
    public Map<String, Object> retryTranscode(UUID id) {
        requireModerator();
        Video v = videoRepository.findById(id).orElseThrow(() -> new BusinessException("视频不存在"));
        v.setStatus("processing");
        v.setTranscodeError(null);
        videoRepository.save(v);
        return Map.of("ok", true);
    }

    @Transactional
    public Map<String, Object> updateAdminVideo(UUID id, Map<String, Object> updates) {
        requireReviewer();
        Video v = videoRepository.findById(id).orElseThrow(() -> new BusinessException("视频不存在"));
        if (updates.containsKey("title")) v.setTitle(((String) updates.get("title")).trim());
        if (updates.containsKey("description")) v.setDescription((String) updates.get("description"));
        if (updates.containsKey("visibility")) v.setVisibility((String) updates.get("visibility"));
        if (updates.containsKey("status")) v.setStatus((String) updates.get("status"));
        if (updates.containsKey("isFeatured") || updates.containsKey("is_featured")) {
            Boolean val = (Boolean) (updates.containsKey("isFeatured") ? updates.get("isFeatured") : updates.get("is_featured"));
            v.setIsFeatured(val);
        }
        if (updates.containsKey("allowComments") || updates.containsKey("allow_comments")) {
            Boolean val = (Boolean) (updates.containsKey("allowComments") ? updates.get("allowComments") : updates.get("allow_comments"));
            v.setAllowComments(val);
        }
        if (updates.containsKey("allowDownload") || updates.containsKey("allow_download")) {
            Boolean val = (Boolean) (updates.containsKey("allowDownload") ? updates.get("allowDownload") : updates.get("allow_download"));
            v.setAllowDownload(val);
        }
        videoRepository.save(v);
        return getVideoDetail(id);
    }

    @Transactional
    public void deleteAdminVideo(UUID id) {
        requireModerator();
        videoRepository.deleteById(id);
    }

    // ==================== COMMENTS ====================

    public Page<Map<String, Object>> getComments(int page, int size) {
        requireReviewer();
        return commentRepository.findAll(PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("content", c.getContent());
                    m.put("created_at", c.getCreatedAt());
                    m.put("updated_at", c.getUpdatedAt());
                    m.put("like_count", c.getLikeCount());
                    m.put("reply_count", c.getReplyCount());
                    m.put("is_deleted", c.getIsDeleted());
                    if (c.getUser() != null) {
                        m.put("user", Map.of("id", c.getUser().getId(), "username", c.getUser().getUsername(),
                                "nickname", c.getUser().getNickname(), "avatar_url", c.getUser().getProfilePictureF()));
                    }
                    if (c.getVideo() != null) {
                        m.put("video", Map.of("id", c.getVideo().getId(), "title", c.getVideo().getTitle()));
                    }
                    return m;
                });
    }

    @Transactional
    public void deleteComment(UUID id) {
        requireModerator();
        commentRepository.deleteById(id);
    }

    // ==================== CATEGORIES ====================

    public List<Map<String, Object>> getCategories(String q) {
        requireReviewer();
        List<Category> cats;
        if (q != null && !q.isEmpty()) {
            cats = categoryRepository.findByNameContainingIgnoreCase(q);
        } else {
            cats = categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        }
        return cats.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId().toString());
            m.put("name", c.getName());
            m.put("description", c.getDescription());
            m.put("created_at", c.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createCategory(String name, String description) {
        requireAdmin();
        if (name == null || name.trim().isEmpty()) throw new BusinessException("名称必填");
        if (categoryRepository.findByNameIgnoreCase(name.trim()).isPresent()) throw new BusinessException("已存在同名分类");
        Category c = categoryRepository.save(Category.builder().name(name.trim()).description(description).build());
        return Map.of("id", c.getId().toString(), "name", c.getName(), "description", c.getDescription());
    }

    public Map<String, Object> getCategory(UUID id) {
        requireReviewer();
        Category c = categoryRepository.findById(id).orElseThrow(() -> new BusinessException("分类不存在"));
        return Map.of("id", c.getId().toString(), "name", c.getName(), "description", c.getDescription());
    }

    @Transactional
    public Map<String, Object> updateCategory(UUID id, String name, String description) {
        requireAdmin();
        Category c = categoryRepository.findById(id).orElseThrow(() -> new BusinessException("分类不存在"));
        if (name != null && !name.trim().isEmpty()) c.setName(name.trim());
        if (description != null) c.setDescription(description);
        categoryRepository.save(c);
        return Map.of("id", c.getId().toString(), "name", c.getName(), "description", c.getDescription());
    }

    @Transactional
    public void deleteCategory(UUID id) {
        requireAdmin();
        categoryRepository.deleteById(id);
    }

    // ==================== TAGS ====================

    public List<Map<String, Object>> getTags(String q) {
        requireReviewer();
        List<Tag> tags;
        if (q != null && !q.isEmpty()) {
            tags = tagRepository.findByNameContainingIgnoreCase(q);
        } else {
            tags = tagRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        }
        return tags.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId().toString());
            m.put("name", t.getName());
            m.put("description", t.getDescription());
            m.put("created_at", t.getCreatedAt());
            m.put("usage_count", videoTagRepository.countByTagId(t.getId()));
            return m;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createTag(String name, String description) {
        requireAdmin();
        if (name == null || name.trim().isEmpty()) throw new BusinessException("名称必填");
        if (tagRepository.findByName(name.trim()).isPresent()) throw new BusinessException("已存在同名标签");
        Tag t = tagRepository.save(Tag.builder()
                .name(name.trim())
                .description(description != null ? description : "")
                .build());
        return Map.of("id", t.getId().toString(), "name", t.getName(), "description", t.getDescription());
    }

    public Map<String, Object> getTag(UUID id) {
        requireReviewer();
        Tag t = tagRepository.findById(id).orElseThrow(() -> new BusinessException("标签不存在"));
        return Map.of("id", t.getId().toString(), "name", t.getName(), "description", t.getDescription(),
                "usage_count", videoTagRepository.countByTagId(t.getId()));
    }

    @Transactional
    public Map<String, Object> updateTag(UUID id, String name) {
        requireAdmin();
        Tag t = tagRepository.findById(id).orElseThrow(() -> new BusinessException("标签不存在"));
        if (name != null && !name.trim().isEmpty()) t.setName(name.trim());
        tagRepository.save(t);
        return Map.of("id", t.getId().toString(), "name", t.getName(), "description", t.getDescription());
    }

    @Transactional
    public Map<String, Object> deleteTag(UUID id) {
        requireAdmin();
        long usage = videoTagRepository.countByTagId(id);
        if (usage > 0) {
            throw new BusinessException("该标签已被视频使用，不可删除");
        }
        tagRepository.deleteById(id);
        return Map.of("ok", true);
    }

    // ==================== REPORTS ====================

    public Page<Map<String, Object>> getReports(String status, int page, int size) {
        requireReviewer();
        var pr = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Report> reports = (status != null && !status.isEmpty())
                ? reportRepository.findByStatusOrderByCreatedAtDesc(status, pr)
                : reportRepository.findAll(pr);
        return reports.map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("target_type", r.getTargetType());
            m.put("target_id", r.getTargetId());
            m.put("reason_code", r.getReason());
            m.put("description", r.getDescription());
            m.put("status", r.getStatus());
            m.put("created_at", r.getCreatedAt());
            m.put("reporter", userBrief(r.getReporter()));
            m.put("target_info", buildTargetDetail(r.getTargetType(), r.getTargetId()));
            return m;
        });
    }

    // ==================== AUDIT LOGS ====================

    public Page<Map<String, Object>> getAuditLogs(int page, int size) {
        requireReviewer();
        return auditLogRepository.findAll(PageRequest.of(Math.max(0, page - 1), size,
                        Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("verb", a.getVerb());
                    m.put("target_type", a.getTargetType());
                    m.put("target_id", a.getTargetId());
                    m.put("meta", a.getMeta());
                    m.put("created_at", a.getCreatedAt());
                    m.put("actor", userBrief(a.getActor()));
                    return m;
                });
    }

    // ==================== ANNOUNCEMENTS ====================

    public Page<Map<String, Object>> getAnnouncements(int page, int size) {
        requireReviewer();
        return announcementRepository.findAllByOrderByPinnedDescPublishedAtDescCreatedAtDesc(
                        PageRequest.of(Math.max(0, page - 1), size))
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("title", a.getTitle());
                    m.put("content", a.getContent());
                    m.put("pinned", a.getPinned());
                    m.put("is_active", a.getIsActive());
                    m.put("published_at", a.getPublishedAt());
                    m.put("created_at", a.getCreatedAt());
                    m.put("updated_at", a.getUpdatedAt());
                    return m;
                });
    }

    public Map<String, Object> getAnnouncement(UUID id) {
        requireReviewer();
        SystemAnnouncement a = announcementRepository.findById(id).orElseThrow(() -> new BusinessException("公告不存在"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("title", a.getTitle());
        m.put("content", a.getContent());
        m.put("pinned", a.getPinned());
        m.put("is_active", a.getIsActive());
        m.put("published_at", a.getPublishedAt());
        m.put("created_at", a.getCreatedAt());
        m.put("updated_at", a.getUpdatedAt());
        return m;
    }

    @Transactional
    public Map<String, Object> createAnnouncement(Map<String, String> body) {
        requireAdmin();
        SystemAnnouncement a = announcementRepository.save(SystemAnnouncement.builder()
                .title(body.get("title")).content(body.get("content"))
                .pinned(Boolean.parseBoolean(body.getOrDefault("pinned", "false")))
                .isActive(Boolean.parseBoolean(body.getOrDefault("isActive", "true")))
                .build());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("title", a.getTitle());
        m.put("content", a.getContent());
        m.put("pinned", a.getPinned());
        m.put("is_active", a.getIsActive());
        m.put("published_at", a.getPublishedAt());
        m.put("created_at", a.getCreatedAt());
        m.put("updated_at", a.getUpdatedAt());
        return m;
    }

    @Transactional
    public Map<String, Object> updateAnnouncement(UUID id, Map<String, Object> body) {
        requireAdmin();
        SystemAnnouncement a = announcementRepository.findById(id).orElseThrow(() -> new BusinessException("公告不存在"));
        if (body.containsKey("title")) a.setTitle((String) body.get("title"));
        if (body.containsKey("content")) a.setContent((String) body.get("content"));
        if (body.containsKey("pinned")) a.setPinned((Boolean) body.get("pinned"));
        if (body.containsKey("isActive") || body.containsKey("is_active"))
            a.setIsActive((Boolean) (body.containsKey("isActive") ? body.get("isActive") : body.get("is_active")));
        if (body.containsKey("published_at")) a.setPublishedAt(LocalDateTime.parse(body.get("published_at").toString()));
        a = announcementRepository.save(a);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("title", a.getTitle());
        m.put("content", a.getContent());
        m.put("pinned", a.getPinned());
        m.put("is_active", a.getIsActive());
        m.put("published_at", a.getPublishedAt());
        m.put("created_at", a.getCreatedAt());
        m.put("updated_at", a.getUpdatedAt());
        return m;
    }

    @Transactional
    public void deleteAnnouncement(UUID id) {
        requireAdmin();
        announcementRepository.deleteById(id);
    }

    // ==================== TAGS BULK DELETE ====================

    @Transactional
    public Map<String, Object> bulkDeleteTags(List<UUID> ids) {
        requireAdmin();
        int removed = 0;
        List<String> blocked = new ArrayList<>();
        for (UUID tid : ids) {
            Tag t = tagRepository.findById(tid).orElse(null);
            if (t == null) continue;
            if (videoTagRepository.countByTagId(t.getId()) > 0) {
                blocked.add(t.getId().toString());
                continue;
            }
            tagRepository.delete(t);
            removed++;
        }
        if (!blocked.isEmpty()) {
            throw new BusinessException("部分标签已被使用，未删除: " + String.join(",", blocked));
        }
        return Map.of("removed", removed);
    }

    // ==================== TAGS MERGE ====================

    @Transactional
    public Map<String, Object> mergeTags(UUID sourceId, UUID targetId) {
        requireAdmin();
        if (sourceId.equals(targetId)) {
            throw new BusinessException("source 与 target 不能相同");
        }
        Tag source = tagRepository.findById(sourceId).orElseThrow(() -> new BusinessException("源标签不存在"));
        Tag target = tagRepository.findById(targetId).orElseThrow(() -> new BusinessException("目标标签不存在"));

        List<VideoTag> sourceVts = videoTagRepository.findByTagId(source.getId());
        List<UUID> existingTargetVideoIds = videoTagRepository.findTagIdsByVideoId(target.getId());
        Set<UUID> existingSet = new HashSet<>(existingTargetVideoIds);
        int moved = 0;
        for (VideoTag vt : sourceVts) {
            if (!existingSet.contains(vt.getVideo().getId())) {
                videoTagRepository.save(VideoTag.builder().video(vt.getVideo()).tag(target).build());
                moved++;
            }
            videoTagRepository.delete(vt);
        }
        tagRepository.delete(source);
        return Map.of("merged", 1, "moved", moved, "target", target.getId().toString());
    }

    // ==================== VIDEO TRANSCODE FAILURES ====================

    public List<Map<String, Object>> getTranscodeFailures(int page, int size) {
        requireReviewer();
        Page<Video> p = videoRepository.findByTranscodeErrorIsNotNull(
                PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "updatedAt")));
        return p.getContent().stream().map(v -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", v.getId().toString());
            m.put("title", v.getTitle());
            m.put("status", v.getStatus());
            m.put("transcode_error", v.getTranscodeError());
            m.put("updated_at", v.getUpdatedAt());
            m.put("owner_id", v.getUser() != null ? v.getUser().getId().toString() : null);
            return m;
        }).collect(Collectors.toList());
    }

    // ==================== SWITCH USER ====================

    public Map<String, Object> switchUser(String targetUsername, String targetPassword) {
        requireSuperAdmin();
        User target = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new BusinessException("管理员凭据无效"));
        if (target.getIsStaff() == null || !target.getIsStaff()) {
            throw new BusinessException("目标用户不是管理员");
        }
        String role = target.getAdminRole() != null ? target.getAdminRole() : "";
        if (!role.matches("reviewer|moderator|admin|super_admin")) {
            throw new BusinessException("目标用户不是管理员");
        }
        if (!passwordEncoder.matches(targetPassword, target.getPassword())) {
            throw new BusinessException("管理员凭据无效");
        }
        String access = jwtUtils.generateAccessToken(target.getId(), target.getUsername());
        String refresh = jwtUtils.generateRefreshToken(target.getId(), target.getUsername());
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", target.getId().toString());
        user.put("username", target.getUsername());
        user.put("nickname", target.getNickname() != null ? target.getNickname() : target.getUsername());
        user.put("is_staff", target.getIsStaff());
        user.put("admin_role", role);
        return Map.of("access", access, "refresh", refresh, "user", user, "switched", true);
    }

    public Map<String, Object> impersonateExit() {
        return Map.of("ok", true);
    }

    // ==================== REPORT DETAIL ====================

    public Map<String, Object> getReportDetail(UUID id) {
        requireReviewer();
        Report r = reportRepository.findById(id).orElseThrow(() -> new BusinessException("举报不存在"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", r.getId().toString());
        data.put("reporter", userBrief(r.getReporter()));
        data.put("target_type", r.getTargetType());
        data.put("target_id", r.getTargetId() != null ? r.getTargetId().toString() : null);
        data.put("reason_code", r.getReason());
        data.put("description", r.getDescription());
        data.put("status", r.getStatus());
        data.put("handled_by", userBrief(r.getHandler()));
        data.put("handled_at", r.getHandledAt());
        data.put("moderator_notes", r.getHandlerNote());
        data.put("created_at", r.getCreatedAt());

        Map<String, Object> targetDetail = buildTargetDetail(r.getTargetType(), r.getTargetId());
        data.put("target_detail", targetDetail);

        List<ModerationAction> actions = moderationActionRepository.findByReportIdOrderByCreatedAtDesc(id);
        data.put("actions", actions.stream().map(a -> {
            Map<String, Object> am = new LinkedHashMap<>();
            am.put("id", a.getId().toString());
            am.put("action", a.getAction());
            am.put("reason", a.getReason());
            am.put("moderator", userBrief(a.getModerator()));
            am.put("created_at", a.getCreatedAt());
            return am;
        }).collect(Collectors.toList()));
        return data;
    }

    private Map<String, Object> buildTargetDetail(String targetType, UUID targetId) {
        if (targetId == null) return null;
        try {
            if ("video".equals(targetType)) {
                Video v = videoRepository.findById(targetId).orElse(null);
                if (v == null) return null;
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("id", v.getId().toString());
                d.put("title", v.getTitle());
                d.put("description", v.getDescription());
                d.put("author", userBrief(v.getUser()));
                d.put("status", v.getStatus());
                d.put("visibility", v.getVisibility());
                d.put("created_at", v.getCreatedAt());
                return d;
            } else if ("comment".equals(targetType)) {
                Comment c = commentRepository.findById(targetId).orElse(null);
                if (c == null) return null;
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("id", c.getId().toString());
                d.put("content", c.getContent());
                d.put("author", userBrief(c.getUser()));
                if (c.getVideo() != null) {
                    d.put("video", Map.of("id", c.getVideo().getId().toString(), "title", c.getVideo().getTitle()));
                }
                d.put("created_at", c.getCreatedAt());
                return d;
            } else if ("user".equals(targetType)) {
                User u = userRepository.findById(targetId).orElse(null);
                if (u == null) return null;
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("id", u.getId().toString());
                d.put("username", u.getUsername());
                d.put("nickname", u.getNickname());
                d.put("is_active", u.getIsActive());
                d.put("is_verified", u.getIsVerified());
                d.put("date_joined", u.getDateJoined());
                return d;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Map<String, Object> userBrief(User u) {
        if (u == null) return null;
        return Map.of("id", u.getId().toString(), "username",
                u.getUsername() != null ? u.getUsername() : "",
                "nickname", u.getNickname() != null ? u.getNickname() : "");
    }

    // ==================== REPORT HANDLE ====================

    @Transactional
    public Map<String, Object> handleReport(UUID id, String action, String notes) {
        requireReviewer();
        Report r = reportRepository.findById(id).orElseThrow(() -> new BusinessException("举报不存在"));
        if (!r.getStatus().equals("pending") && !r.getStatus().equals("escalated")) {
            throw new BusinessException("该举报当前状态为 " + r.getStatus() + "，不能继续处理");
        }
        String resolvedAction = action;
        if (action == null || action.isEmpty()) {
            throw new BusinessException("必填，例如: dismiss, warn, delete_content, ban_user, escalate");
        }
        if (!action.matches("dismiss|warn|delete_content|ban_user|escalate")) {
            throw new BusinessException("非法值，可选: dismiss, warn, delete_content, ban_user, escalate");
        }

        String actionResult;
        if ("delete_content".equals(action)) {
            if ("video".equals(r.getTargetType()) && r.getTargetId() != null) {
                videoRepository.deleteById(r.getTargetId());
                actionResult = "视频已删除";
            } else if ("comment".equals(r.getTargetType()) && r.getTargetId() != null) {
                commentRepository.deleteById(r.getTargetId());
                actionResult = "评论已删除";
            } else {
                throw new BusinessException("目标内容不存在");
            }
        } else if ("ban_user".equals(action)) {
            if (!"user".equals(r.getTargetType()) || r.getTargetId() == null) {
                throw new BusinessException("仅用户举报支持封禁");
            }
            User target = userRepository.findById(r.getTargetId()).orElseThrow(() -> new BusinessException("目标用户不存在"));
            target.setIsActive(false);
            userRepository.save(target);
            actionResult = "用户已封禁";
        } else if ("warn".equals(action)) {
            actionResult = "已记录警告";
        } else if ("dismiss".equals(action)) {
            actionResult = "举报已驳回";
        } else {
            actionResult = "举报已升级";
        }

        User moderator = currentUser();
        if ("escalate".equals(action)) {
            r.setStatus("escalated");
        } else {
            r.setStatus("resolved");
        }
        r.setHandler(moderator);
        r.setHandledAt(LocalDateTime.now());
        if (notes != null && !notes.isEmpty()) r.setHandlerNote(notes);
        reportRepository.save(r);

        moderationActionRepository.save(ModerationAction.builder()
                .report(r).moderator(moderator).action(resolvedAction).reason(notes).build());

        return Map.of("report_id", r.getId().toString(), "status", r.getStatus(),
                "action", resolvedAction, "action_result", actionResult, "handled_at", r.getHandledAt());
    }

    // ==================== COMMENT DETAIL (admin) ====================

    public Map<String, Object> getCommentDetail(UUID id) {
        requireReviewer();
        Comment c = commentRepository.findById(id).orElseThrow(() -> new BusinessException("评论不存在"));
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("id", c.getId().toString());
        d.put("content", c.getContent());
        d.put("author", userBrief(c.getUser()));
        if (c.getVideo() != null) {
            d.put("video", Map.of("id", c.getVideo().getId().toString(), "title", c.getVideo().getTitle()));
        }
        d.put("created_at", c.getCreatedAt());
        return d;
    }

    // ==================== METRICS TREND ====================

    public Map<String, Object> getMetricsTrend(String metric, String range) {
        requireReviewer();
        int days = "30d".equals(range) ? 30 : 7;
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate start = today.minusDays(days - 1);
        long total = 0;
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            java.time.LocalDate d = start.plusDays(i);
            trend.add(Map.of("date", d.toString(), "value", 0));
        }
        if ("view".equals(metric)) {
            total = videoRepository.sumViewCount();
        } else if ("like".equals(metric)) {
            total = videoRepository.sumLikeCount();
        }
        return Map.of("metric", metric, "range", days + "d", "trend", trend, "total", total);
    }

    // ==================== AUTOMOD SUMMARY ====================

    public Map<String, Object> getAutomodSummary(int days) {
        requireReviewer();
        days = Math.max(1, Math.min(90, days));
        Page<AuditLog> logs = auditLogRepository.findAll(PageRequest.of(0, 500,
                Sort.by(Sort.Direction.DESC, "createdAt")));
        return Map.of("days", days, "total_rules", 0, "total_hits", 0, "results", List.of());
    }

    // ==================== MODERATION HEALTH CHECK ====================

    public Map<String, Object> moderationHealthCheck() {
        requireAdmin();
        return Map.of("ok", true, "status", "not_configured",
                "message", "智谱审核服务未在 Spring 端配置，Django 端运行中");
    }
}
