package com.vidsprout.modules.interaction.controller;

import com.vidsprout.common.ApiResponse;
import com.vidsprout.modules.interaction.dto.CommentRequest;
import com.vidsprout.modules.interaction.dto.CommentResponse;
import com.vidsprout.modules.interaction.service.InteractionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/interactions")
public class InteractionController {

    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @PostMapping("/follow/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> follow(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(interactionService.follow(parseUuid(body.get("user_id")))));
    }

    @PostMapping("/unfollow/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unfollow(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(interactionService.unfollow(parseUuid(body.get("user_id")))));
    }

    @GetMapping("/followers/")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFollowers(
            @RequestParam(required = false) UUID user_id,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String order,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> results = interactionService.getFollowers(user_id, page, size);
        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .ok(true).results(results).page(page).pageSize(size)
                .hasNext(results.size() >= size).build());
    }

    @GetMapping("/following/")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFollowing(
            @RequestParam(required = false) UUID user_id,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String order,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> results = interactionService.getFollowing(user_id, page, size);
        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .ok(true).results(results).page(page).pageSize(size)
                .hasNext(results.size() >= size).build());
    }

    @GetMapping("/relationship/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRelationship(@RequestParam UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(interactionService.getRelationship(userId)));
    }

    @PostMapping("/like/toggle/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleLike(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(interactionService.toggleLike(parseUuid(body.get("video_id")))));
    }

    @PostMapping("/favorite/toggle/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleFavorite(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(interactionService.toggleFavorite(parseUuid(body.get("video_id")))));
    }

    @GetMapping("/likes/")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLikes(
            @RequestParam(required = false) UUID user_id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        org.springframework.data.domain.Page<Map<String, Object>> p = interactionService.getLikes(user_id, page, size);
        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .ok(true).results(p.getContent()).page(page).pageSize(size)
                .total((int) p.getTotalElements()).hasNext(p.hasNext()).build());
    }

    @GetMapping("/favorites/")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFavorites(
            @RequestParam(required = false) UUID user_id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        org.springframework.data.domain.Page<Map<String, Object>> p = interactionService.getFavorites(user_id, page, size);
        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .ok(true).results(p.getContent()).page(page).pageSize(size)
                .total((int) p.getTotalElements()).hasNext(p.hasNext()).build());
    }

    @GetMapping("/comments/")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @RequestParam UUID videoId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "created") String sort) {
        return ResponseEntity.ok(ApiResponse.success(interactionService.getComments(videoId, page, size)));
    }

    @PostMapping("/comments/")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(@RequestBody CommentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(interactionService.createComment(request)));
    }

    @GetMapping("/comments/replies/")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getReplies(
            @RequestParam UUID parentId, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(interactionService.getReplies(parentId, page, size)));
    }

    @DeleteMapping("/comments/{id}/")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable UUID id) {
        interactionService.deleteComment(id);
        return ResponseEntity.ok(ApiResponse.success(null, "删除成功"));
    }

    @PostMapping("/comments/{id}/like/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleCommentLike(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(interactionService.toggleCommentLike(id)));
    }

    @PostMapping("/reports/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createReport(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(interactionService.createReport(body)));
    }

    @PostMapping("/watch-later/toggle/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleWatchLater(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(interactionService.toggleWatchLater(parseUuid(body.get("video_id")))));
    }

    @GetMapping("/watch-later/")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getWatchLaterList(
            @RequestParam(required = false) UUID user_id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        org.springframework.data.domain.Page<Map<String, Object>> p = interactionService.getWatchLaterList(user_id, page, size);
        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .ok(true).results(p.getContent()).page(page).pageSize(size)
                .total((int) p.getTotalElements()).hasNext(p.hasNext()).build());
    }

    @PostMapping("/history/record/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recordHistory(@RequestBody Map<String, Object> body) {
        Integer watchDuration = body.get("watch_duration") != null ? ((Number) body.get("watch_duration")).intValue() : null;
        Double progress = body.get("progress") != null ? ((Number) body.get("progress")).doubleValue() : null;
        return ResponseEntity.ok(ApiResponse.success(interactionService.recordHistory(
                parseUuid(body.get("video_id")),
                watchDuration,
                progress)));
    }

    @GetMapping("/history/")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHistoryList(
            @RequestParam(required = false) UUID user_id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        org.springframework.data.domain.Page<Map<String, Object>> p = interactionService.getHistoryList(user_id, page, size);
        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .ok(true).results(p.getContent()).page(page).pageSize(size)
                .total((int) p.getTotalElements()).hasNext(p.hasNext()).build());
    }

    @PostMapping("/history/bulk-remove/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkRemoveHistory(@RequestBody Map<String, Object> body) {
        List<UUID> ids = parseUuidList(body.getOrDefault("video_ids", body.get("ids")));
        return ResponseEntity.ok(ApiResponse.success(interactionService.bulkRemoveHistory(ids)));
    }

    @PostMapping("/likes/bulk-unlike/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkUnlike(@RequestBody Map<String, Object> body) {
        List<UUID> ids = parseUuidList(body.getOrDefault("video_ids", body.get("ids")));
        return ResponseEntity.ok(ApiResponse.success(interactionService.bulkRemoveLikes(ids)));
    }

    @PostMapping("/favorites/bulk-remove/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkRemoveFavorite(@RequestBody Map<String, Object> body) {
        List<UUID> ids = parseUuidList(body.getOrDefault("video_ids", body.get("ids")));
        return ResponseEntity.ok(ApiResponse.success(interactionService.bulkRemoveFavorites(ids)));
    }

    @PostMapping("/watch-later/bulk-remove/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkRemoveWatchLater(@RequestBody Map<String, Object> body) {
        List<UUID> ids = parseUuidList(body.getOrDefault("video_ids", body.get("ids")));
        return ResponseEntity.ok(ApiResponse.success(interactionService.bulkRemoveWatchLater(ids)));
    }

    @GetMapping("/notifications/")
    public ResponseEntity<ApiResponse<List<?>>> getNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean unread) {
        return ResponseEntity.ok(ApiResponse.success(interactionService.getNotifications(page, size, unread)));
    }

    @PostMapping("/notifications/mark-read/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markRead(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> rawIds = (List<String>) body.getOrDefault("ids", List.of());
        List<UUID> ids = rawIds.stream().map(UUID::fromString).toList();
        return ResponseEntity.ok(ApiResponse.success(interactionService.markNotificationsRead(ids)));
    }

    @PostMapping("/notifications/mark-all-read/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllRead() {
        return ResponseEntity.ok(ApiResponse.success(interactionService.markAllNotificationsRead()));
    }

    @PostMapping("/notifications/clear/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> clearAll() {
        return ResponseEntity.ok(ApiResponse.success(interactionService.clearAllNotifications()));
    }

    @GetMapping("/notifications/unread-count/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadCount() {
        return ResponseEntity.ok(ApiResponse.success(interactionService.getUnreadNotificationCount()));
    }

    private UUID parseUuid(Object obj) {
        if (obj == null) return null;
        if (obj instanceof UUID) return (UUID) obj;
        try { return UUID.fromString(obj.toString()); }
        catch (IllegalArgumentException e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private List<UUID> parseUuidList(Object obj) {
        if (!(obj instanceof List)) return List.of();
        List<UUID> ids = new ArrayList<>();
        for (Object item : (List<Object>) obj) {
            UUID id = parseUuid(item);
            if (id != null) ids.add(id);
        }
        return ids;
    }
}
