package com.vidsprout.modules.video.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidsprout.common.PageRequest;
import com.vidsprout.common.exception.BusinessException;
import com.vidsprout.common.exception.ResourceNotFoundException;
import com.vidsprout.common.exception.UnauthorizedException;
import com.vidsprout.modules.content.model.Category;
import com.vidsprout.modules.content.model.Tag;
import com.vidsprout.modules.content.repository.CategoryRepository;
import com.vidsprout.modules.content.repository.TagRepository;
import com.vidsprout.modules.interaction.repository.FavoriteRepository;
import com.vidsprout.modules.interaction.repository.LikeRepository;
import com.vidsprout.modules.interaction.repository.WatchLaterRepository;
import com.vidsprout.modules.user.model.User;
import com.vidsprout.modules.user.repository.UserRepository;
import com.vidsprout.modules.user.service.AuthService;
import com.vidsprout.modules.video.dto.*;
import com.vidsprout.modules.video.model.Video;
import com.vidsprout.modules.video.model.VideoTag;
import com.vidsprout.modules.video.repository.VideoRepository;
import com.vidsprout.modules.video.repository.VideoTagRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final VideoTagRepository videoTagRepository;
    private final FavoriteRepository favoriteRepository;
    private final LikeRepository likeRepository;
    private final WatchLaterRepository watchLaterRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Value("${app.video.max-size-bytes:524288000}")
    private long maxSizeBytes;

    @Value("${app.video.allowed-extensions:.mp4,.mov,.m4v,.webm,.mkv}")
    private String allowedExtensions;

    @Value("${app.upload.dir:./media}")
    private String uploadDir;

    public VideoService(VideoRepository videoRepository, AuthService authService, ObjectMapper objectMapper,
                        VideoTagRepository videoTagRepository, FavoriteRepository favoriteRepository,
                        LikeRepository likeRepository, WatchLaterRepository watchLaterRepository,
                        CategoryRepository categoryRepository, TagRepository tagRepository,
                        UserRepository userRepository) {
        this.videoRepository = videoRepository;
        this.authService = authService;
        this.objectMapper = objectMapper;
        this.videoTagRepository = videoTagRepository;
        this.favoriteRepository = favoriteRepository;
        this.likeRepository = likeRepository;
        this.watchLaterRepository = watchLaterRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<VideoResponse> listVideos(String search, UUID categoryId, List<UUID> tagIds, String tagMatch,
                                          UUID userId, String order, int page, int size) {
        User viewer = authService.getCurrentUserEntityOrNull();
        boolean ownerView = userId != null && viewer != null
                && (userId.equals(viewer.getId()) || Boolean.TRUE.equals(viewer.getIsStaff()));
        UUID targetUserId = userId;

        Specification<Video> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!ownerView) {
                predicates.add(cb.equal(root.get("status"), "published"));
                predicates.add(cb.equal(root.get("visibility"), "public"));
            }
            if (search != null && !search.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%"));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (tagIds != null && !tagIds.isEmpty()) {
                if ("all".equals(tagMatch)) {
                    long expected = tagIds.size();
                    jakarta.persistence.criteria.Subquery<Long> sq = query.subquery(Long.class);
                    jakarta.persistence.criteria.Root<VideoTag> vtRoot = sq.from(VideoTag.class);
                    sq.select(cb.count(vtRoot))
                            .where(cb.equal(vtRoot.get("video").get("id"), root.get("id")),
                                    vtRoot.get("tag").get("id").in(tagIds));
                    predicates.add(cb.equal(sq, expected));
                } else {
                    predicates.add(root.join("videoTags").get("tag").get("id").in(tagIds));
                }
            }
            if (targetUserId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), targetUserId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                Math.max(0, page - 1), Math.max(1, size), sortForOrder(order));
        Page<Video> pageResult = videoRepository.findAll(spec, pageable);
        List<VideoResponse> videos = pageResult.getContent().stream()
                .map(this::toVideoResponse)
                .collect(Collectors.toList());
        if (viewer != null && !videos.isEmpty()) {
            List<UUID> ids = videos.stream().map(VideoResponse::getId).collect(Collectors.toList());
            Set<UUID> likedIds = likeRepository.findLikedVideoIds(viewer.getId(), ids);
            Set<UUID> favIds = favoriteRepository.findFavoritedVideoIds(viewer.getId(), ids);
            videos.forEach(v -> {
                v.setLiked(likedIds.contains(v.getId()));
                v.setFavorited(favIds.contains(v.getId()));
            });
        }
        return new org.springframework.data.domain.PageImpl<>(videos, pageable, pageResult.getTotalElements());
    }

    private Sort sortForOrder(String order) {
        if (order == null) order = "";
        return switch (order.toLowerCase()) {
            case "views", "view_count", "most_viewed" -> Sort.by(Sort.Direction.DESC, "viewCount");
            case "likes", "like_count", "most_liked" -> Sort.by(Sort.Direction.DESC, "likeCount");
            case "oldest", "earliest" -> Sort.by(Sort.Direction.ASC, "publishedAt");
            case "title" -> Sort.by(Sort.Direction.ASC, "title");
            default -> Sort.by(Sort.Direction.DESC, "publishedAt");
        };
    }

    @Transactional
    public VideoResponse getVideoDetail(UUID id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在"));
        
        User currentUser = authService.getCurrentUserEntityOrNull();
        if (!canViewVideo(video, currentUser)) {
            throw new ResourceNotFoundException("视频不存在或无权查看");
        }
        
        videoRepository.incrementViewCount(id);
        return toVideoResponseWithUserInteractions(video, currentUser);
    }

    @Transactional
    public VideoResponse updateVideo(UUID id, Map<String, Object> updates) {
        User user = authService.getCurrentUserEntity();
        Video video = videoRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在或无权编辑"));

        if (updates.containsKey("title")) video.setTitle(((String) updates.get("title")).trim());
        if (updates.containsKey("description")) video.setDescription(((String) updates.get("description")).trim());
        if (updates.containsKey("visibility")) {
            String vis = (String) updates.get("visibility");
            if (List.of("public", "unlisted", "private").contains(vis)) {
                video.setVisibility(vis);
            }
        }
        if (updates.containsKey("allowComments") || updates.containsKey("allow_comments")) {
            Object v = updates.containsKey("allowComments") ? updates.get("allowComments") : updates.get("allow_comments");
            if (v instanceof Boolean) video.setAllowComments((Boolean) v);
        }
        if (updates.containsKey("allowDownload") || updates.containsKey("allow_download")) {
            Object v = updates.containsKey("allowDownload") ? updates.get("allowDownload") : updates.get("allow_download");
            if (v instanceof Boolean) video.setAllowDownload((Boolean) v);
        }
        if (updates.containsKey("status") && "published".equals(updates.get("status"))) {
            video.setStatus("published");
            video.setPublishedAt(java.time.LocalDateTime.now());
        }
        if (updates.containsKey("categoryId") || updates.containsKey("category_id")) {
            Object raw = updates.containsKey("categoryId") ? updates.get("categoryId") : updates.get("category_id");
            UUID catId = parseUuid(raw);
            video.setCategory(catId != null ? categoryRepository.findById(catId)
                    .orElseThrow(() -> new ResourceNotFoundException("分类不存在")) : null);
        }
        if (updates.containsKey("tagIds") || updates.containsKey("tag_ids")) {
            Object raw = updates.containsKey("tagIds") ? updates.get("tagIds") : updates.get("tag_ids");
            updateVideoTags(video, raw instanceof List ? (List<?>) raw : List.of());
        }

        video = videoRepository.save(video);
        return toVideoResponseWithUserInteractions(video, user);
    }

    @Transactional
    public void deleteVideo(UUID id) {
        User user = authService.getCurrentUserEntity();
        Video video = videoRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在或无权删除"));
        
        videoRepository.delete(video);
        userRepository.decrementVideoCount(user.getId(), 1);
    }

    @Transactional
    public Map<String, Object> bulkDelete(List<UUID> videoIds) {
        User user = authService.getCurrentUserEntity();
        if (videoIds.size() > 200) throw new BusinessException("一次最多处理 200 个视频");
        
        List<Video> videos = videoRepository.findAllById(videoIds);
        List<Video> owned = videos.stream()
                .filter(v -> v.getUser().getId().equals(user.getId()))
                .collect(Collectors.toList());
        
        Map<UUID, Long> counts = new HashMap<>();
        owned.forEach(v -> counts.merge(v.getUser().getId(), 1L, Long::sum));
        videoRepository.deleteAll(owned);
        counts.forEach((uid, c) -> userRepository.decrementVideoCount(uid, c.intValue()));
        
        return Map.of("removed", owned.size());
    }

    @Transactional
    public Map<String, Object> bulkUpdate(List<UUID> videoIds, Map<String, Object> updates) {
        User user = authService.getCurrentUserEntity();
        if (videoIds.size() > 200) throw new BusinessException("一次最多处理 200 个视频");
        
        List<Video> videos = videoRepository.findAllById(videoIds);
        List<Video> owned = videos.stream()
                .filter(v -> v.getUser().getId().equals(user.getId()))
                .collect(Collectors.toList());
        
        int updated = 0;
        for (Video video : owned) {
            boolean changed = false;
            if (updates.containsKey("allowComments")) {
                video.setAllowComments((Boolean) updates.get("allowComments"));
                changed = true;
            }
            if (updates.containsKey("allowDownload")) {
                video.setAllowDownload((Boolean) updates.get("allowDownload"));
                changed = true;
            }
            if (updates.containsKey("visibility")) {
                String vis = (String) updates.get("visibility");
                if (List.of("public", "unlisted", "private").contains(vis)) {
                    video.setVisibility(vis);
                    changed = true;
                }
            }
            if (changed) updated++;
        }
        
        videoRepository.saveAll(owned);
        return Map.of("updated", updated);
    }

    @Transactional
    public VideoResponse uploadVideo(MultipartFile file, String title, String description, UUID categoryId) {
        validateFile(file);
        User user = authService.getCurrentUserEntity();
        String videoKey = UUID.randomUUID().toString().replace("-", "");
        String ext = getExtension(Objects.requireNonNull(file.getOriginalFilename()));
        String videoRel = "videos/" + videoKey + ext;
        Path videoPath = Path.of(uploadDir, videoRel);

        try {
            Files.createDirectories(videoPath.getParent());
            file.transferTo(videoPath.toFile());

            Video video = Video.builder()
                    .title(title != null ? title : file.getOriginalFilename())
                    .description(description != null ? description : "")
                    .videoFile(videoRel.substring(0, Math.min(100, videoRel.length())))
                    .videoFileF(videoRel)
                    .fileSize(file.getSize())
                    .status("processing")
                    .uploadStatus("completed")
                    .visibility("public")
                    .user(user)
                    .build();

            video = videoRepository.save(video);
            userRepository.incrementVideoCount(user.getId(), 1);
            if (categoryId != null) {
                Category cat = categoryRepository.findById(categoryId).orElse(null);
                video.setCategory(cat);
                videoRepository.save(video);
            }
            return toVideoResponse(video);
        } catch (IOException e) {
            throw new BusinessException("文件保存失败：" + e.getMessage());
        }
    }

    public ChunkUploadSession initChunkUpload(ChunkInitRequest request) {
        User user = authService.getCurrentUserEntity();
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        String ext = getExtension(request.getFilename());
        Path sessionDir = Path.of(uploadDir, "uploads", "sessions", uploadId);
        Path chunksDir = sessionDir.resolve("chunks");
        try {
            Files.createDirectories(chunksDir);
            Map<String, Object> meta = new HashMap<>();
            meta.put("filename", request.getFilename());
            meta.put("filesize", request.getFilesize());
            meta.put("chunkSize", request.getChunkSize());
            meta.put("ext", ext);
            meta.put("userId", user.getId().toString());
            objectMapper.writeValue(sessionDir.resolve("meta.json").toFile(), meta);
        } catch (IOException e) {
            throw new BusinessException("创建上传会话失败");
        }
        return ChunkUploadSession.builder().uploadId(uploadId).filename(request.getFilename())
                .filesize(request.getFilesize()).chunkSize(request.getChunkSize()).ext(ext).build();
    }

    public Map<String, Object> uploadChunk(String uploadId, int index, MultipartFile file) {
        Path chunkPath = Path.of(uploadDir, "uploads", "sessions", uploadId, "chunks", index + ".part");
        try {
            Files.createDirectories(chunkPath.getParent());
            file.transferTo(chunkPath.toFile());
            return Map.of("ok", true, "index", index);
        } catch (IOException e) {
            throw new BusinessException("分片上传失败");
        }
    }

    public ChunkUploadStatus getUploadStatus(String uploadId) {
        Path sessionDir = Path.of(uploadDir, "uploads", "sessions", uploadId);
        if (!Files.exists(sessionDir)) throw new ResourceNotFoundException("上传会话不存在");
        try {
            Map<String, Object> meta = objectMapper.readValue(
                    sessionDir.resolve("meta.json").toFile(), Map.class);
            long filesize = ((Number) meta.get("filesize")).longValue();
            int chunkSize = ((Number) meta.get("chunkSize")).intValue();
            int total = (int) Math.ceil((double) filesize / chunkSize);
            List<Integer> received = new ArrayList<>(), missing = new ArrayList<>();
            Path chunkDir = sessionDir.resolve("chunks");
            for (int i = 0; i < total; i++) {
                if (Files.exists(chunkDir.resolve(i + ".part"))) received.add(i);
                else missing.add(i);
            }
            return ChunkUploadStatus.builder().uploadId(uploadId)
                    .filename((String) meta.get("filename")).filesize(filesize)
                    .totalChunks(total).receivedChunks(received).missingChunks(missing)
                    .complete(missing.isEmpty()).build();
        } catch (IOException e) {
            throw new BusinessException("读取会话状态失败");
        }
    }

    @Transactional
    public VideoResponse completeChunkUpload(String uploadId, String title, String description, UUID categoryId) {
        User user = authService.getCurrentUserEntity();
        Path sessionDir = Path.of(uploadDir, "uploads", "sessions", uploadId);
        if (!Files.exists(sessionDir)) throw new ResourceNotFoundException("上传会话不存在");
        try {
            Map<String, Object> meta = objectMapper.readValue(
                    sessionDir.resolve("meta.json").toFile(), Map.class);
            if (!user.getId().toString().equals(meta.get("userId")))
                throw new UnauthorizedException("无权操作此上传会话");
            
            String filename = (String) meta.get("filename");
            long filesize = ((Number) meta.get("filesize")).longValue();
            int chunkSize = ((Number) meta.get("chunkSize")).intValue();
            int total = (int) Math.ceil((double) filesize / chunkSize);
            String ext = (String) meta.getOrDefault("ext", ".mp4");
            String videoKey = UUID.randomUUID().toString().replace("-", "");
            String videoRel = "videos/" + videoKey + ext;
            Path videoPath = Path.of(uploadDir, videoRel);
            Files.createDirectories(videoPath.getParent());
            
            Path chunkDir = sessionDir.resolve("chunks");
            try (OutputStream out = Files.newOutputStream(videoPath)) {
                for (int i = 0; i < total; i++) {
                    Path part = chunkDir.resolve(i + ".part");
                    if (!Files.exists(part)) throw new BusinessException("缺少分片：" + i);
                    Files.copy(part, out);
                }
            }
            Video video = Video.builder().title(title != null ? title : filename)
                    .description(description != null ? description : "")
                    .videoFile(videoRel.substring(0, Math.min(100, videoRel.length())))
                    .videoFileF(videoRel).fileSize(filesize).status("processing")
                    .uploadStatus("completed").visibility("public").user(user).build();
            video = videoRepository.save(video);
            userRepository.incrementVideoCount(user.getId(), 1);
            if (categoryId != null) {
                Category cat = categoryRepository.findById(categoryId).orElse(null);
                video.setCategory(cat);
                video = videoRepository.save(video);
            }
            return toVideoResponse(video);
        } catch (IOException e) {
            throw new BusinessException("合并文件失败：" + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> retryTranscode(UUID id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在"));
        video.setStatus("processing");
        video.setTranscodeError(null);
        videoRepository.save(video);
        return Map.of("ok", true, "video_id", id.toString(), "message", "已加入转码队列");
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) throw new BusinessException("未收到文件");
        if (file.getSize() > maxSizeBytes) throw new BusinessException("视频文件过大");
        String ext = getExtension(Objects.requireNonNull(file.getOriginalFilename())).toLowerCase();
        if (!Arrays.asList(allowedExtensions.split(",")).contains(ext))
            throw new BusinessException("不支持的文件格式");
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot).toLowerCase() : "";
    }
    
    private UUID parseUuid(Object obj) {
        if (obj == null) return null;
        if (obj instanceof UUID) return (UUID) obj;
        try { return UUID.fromString(obj.toString()); }
        catch (IllegalArgumentException e) { return null; }
    }
    
    private boolean canViewVideo(Video video, User viewer) {
        if ("banned".equals(video.getStatus())) return viewer != null && viewer.getIsStaff();
        if (!"published".equals(video.getStatus()) && !isOwnerOrStaff(video, viewer)) return false;
        if ("private".equals(video.getVisibility()) && !isOwnerOrStaff(video, viewer)) return false;
        return true;
    }
    
    private boolean isOwnerOrStaff(Video video, User viewer) {
        if (viewer == null) return false;
        if (viewer.getIsStaff()) return true;
        return viewer.getId().equals(video.getUser().getId());
    }
    
    private void updateVideoTags(Video video, List<?> tagIdsRaw) {
        if (tagIdsRaw == null) return;
        List<UUID> tagIds = tagIdsRaw.stream().map(this::parseUuid).filter(Objects::nonNull).limit(3).collect(Collectors.toList());
        Set<UUID> existing = videoTagRepository.findTagIdsByVideoId(video.getId()).stream().collect(Collectors.toSet());
        Set<UUID> newIds = tagIds.stream().collect(Collectors.toSet());
        Set<UUID> toAdd = newIds.stream().filter(id -> !existing.contains(id)).collect(Collectors.toSet());
        Set<UUID> toRemove = existing.stream().filter(id -> !newIds.contains(id)).collect(Collectors.toSet());
        toAdd.forEach(tid -> {
            Tag tag = tagRepository.findById(tid).orElse(null);
            if (tag != null) videoTagRepository.save(VideoTag.builder().video(video).tag(tag).build());
        });
        toRemove.forEach(tid -> videoTagRepository.deleteByVideoIdAndTagId(video.getId(), tid));
    }

    public VideoResponse toVideoResponse(Video video) {
        String base = "/media/";
        VideoResponse r = VideoResponse.builder().id(video.getId()).title(video.getTitle())
                .description(video.getDescription())
                .videoUrl(video.getVideoFileF() != null ? base + video.getVideoFileF() : null)
                .thumbnailUrl(video.getThumbnailF() != null ? base + video.getThumbnailF() : null)
                .hlsMasterUrl(video.getVideoFile() != null ? base + "videos/hls/" + video.getId() + "/master.m3u8" : null)
                .thumbnailVttUrl(base + "videos/thumbs/" + video.getId() + ".vtt")
                .lowMp4Url(video.getLowMp4() != null ? base + video.getLowMp4() : null)
                .duration(video.getDuration()).width(video.getWidth()).height(video.getHeight())
                .fileSize(video.getFileSize()).allowComments(video.getAllowComments())
                .allowDownload(video.getAllowDownload()).visibility(video.getVisibility())
                .status(video.getStatus()).uploadStatus(video.getUploadStatus())
                .transcodeError(video.getTranscodeError())
                .viewCount(video.getViewCount()).likeCount(video.getLikeCount())
                .commentCount(video.getCommentCount())
                .ownerId(video.getUser() != null ? video.getUser().getId() : null)
                .author(video.getUser() != null ? authorPayload(video.getUser()) : null)
                .publishedAt(video.getPublishedAt()).createdAt(video.getCreatedAt()).build();
        r.setFavoriteCount(favoriteRepository.countByVideoId(video.getId()));
        List<VideoTag> vts = videoTagRepository.findByVideoIdWithTags(video.getId());
        List<Map<String, Object>> tags = vts.stream().map(vt -> {
            Tag t = vt.getTag();
            Map<String, Object> tagMap = new HashMap<>();
            tagMap.put("id", t.getId().toString());
            tagMap.put("name", t.getName());
            return tagMap;
        }).collect(Collectors.toList());
        r.setTags(tags);
        if (video.getCategory() != null) {
            Category c = video.getCategory();
            Map<String, Object> catMap = new HashMap<>();
            catMap.put("id", c.getId().toString());
            catMap.put("name", c.getName());
            r.setCategory(catMap);
        }
        return r;
    }
    
    public VideoResponse toVideoResponseWithUserInteractions(Video video, User viewer) {
        VideoResponse r = toVideoResponse(video);
        r.setCanEdit(isOwnerOrStaff(video, viewer) && !("banned".equals(video.getStatus()) && (viewer == null || !viewer.getIsStaff())));
        r.setLiked(false);
        r.setFavorited(false);
        r.setWatchLater(false);
        if (viewer != null) {
            r.setLiked(likeRepository.existsByUserIdAndVideoId(viewer.getId(), video.getId()));
            r.setFavorited(favoriteRepository.existsByUserIdAndVideoId(viewer.getId(), video.getId()));
            r.setWatchLater(watchLaterRepository.existsByUserIdAndVideoId(viewer.getId(), video.getId()));
        }
        return r;
    }

    private Map<String, Object> authorPayload(User user) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", user.getId().toString());
        m.put("name", user.getNickname() != null ? user.getNickname() : user.getUsername());
        m.put("username", user.getUsername());
        m.put("avatar_url", user.getProfilePictureF());
        return m;
    }
}
