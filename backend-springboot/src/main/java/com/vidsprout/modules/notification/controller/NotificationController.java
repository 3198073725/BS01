package com.vidsprout.modules.notification.controller;

import com.vidsprout.common.ApiResponse;
import com.vidsprout.modules.interaction.service.InteractionService;
import com.vidsprout.modules.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final InteractionService interactionService;

    public NotificationController(NotificationService notificationService, InteractionService interactionService) {
        this.notificationService = notificationService;
        this.interactionService = interactionService;
    }

    @GetMapping("/")
    public ResponseEntity<ApiResponse<List<? extends Map<String, Object>>>> listNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String unread) {
        Boolean unreadOnly = "1".equals(unread) || "true".equalsIgnoreCase(unread);
        List<? extends Map<String, Object>> results = interactionService.getNotifications(page, size, unreadOnly);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/unread-count/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotifUnreadCount() {
        return ResponseEntity.ok(ApiResponse.success(interactionService.getUnreadNotificationCount()));
    }

    @PostMapping("/mark-read/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markRead(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> rawIds = (List<String>) body.getOrDefault("ids", List.of());
        List<UUID> ids = new ArrayList<>();
        for (String raw : rawIds) {
            try { ids.add(UUID.fromString(raw)); } catch (IllegalArgumentException ignored) {}
        }
        return ResponseEntity.ok(ApiResponse.success(interactionService.markNotificationsRead(ids)));
    }

    @PostMapping("/mark-all-read/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllRead() {
        return ResponseEntity.ok(ApiResponse.success(interactionService.markAllNotificationsRead()));
    }

    @PostMapping("/clear/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> clearAll() {
        return ResponseEntity.ok(ApiResponse.success(interactionService.clearAllNotifications()));
    }

    @GetMapping("/announcements/")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAnnouncements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        Page<Map<String, Object>> p = notificationService.getAnnouncements(page, size, includeInactive);
        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .ok(true)
                .results(p.getContent())
                .page(page)
                .pageSize(size)
                .total((int) p.getTotalElements())
                .hasNext(p.hasNext())
                .build());
    }

    @GetMapping("/announcements/{id}/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnnouncement(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getAnnouncement(id)));
    }

    @PostMapping("/announcements/{id}/read/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markRead(@PathVariable UUID id) {
        notificationService.markAnnouncementRead(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("ok", true)));
    }

    @GetMapping("/announcements/unread-count/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadCount() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("unread", notificationService.getUnreadAnnouncementCount())));
    }

    @GetMapping("/announcements/latest-unread/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLatestUnread() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("announcement", notificationService.getLatestUnreadAnnouncement())));
    }
}
