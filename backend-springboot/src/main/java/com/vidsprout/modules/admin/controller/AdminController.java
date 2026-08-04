package com.vidsprout.modules.admin.controller;

import com.vidsprout.common.ApiResponse;
import com.vidsprout.modules.admin.service.AdminService;
import com.vidsprout.modules.config.service.SystemConfigService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final SystemConfigService configService;

    public AdminController(AdminService adminService, SystemConfigService configService) {
        this.adminService = adminService;
        this.configService = configService;
    }

    @GetMapping("/me/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> adminMe() {
        return ResponseEntity.ok(ApiResponse.success(adminService.adminMe()));
    }

    @GetMapping("/analytics/overview/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyticsOverview() {
        return ResponseEntity.ok(ApiResponse.success(adminService.analyticsOverview()));
    }

    @GetMapping("/users/")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUsers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Map<String, Object>> users = adminService.getUsers(search, page, size);
        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .ok(true).results(users.getContent()).page(page).pageSize(size)
                .total((int) users.getTotalElements()).hasNext(users.hasNext()).build());
    }

    @GetMapping("/users/{id}/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUserDetail(id)));
    }

    @PatchMapping("/users/{id}/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUser(@PathVariable UUID id, @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updateUser(id, updates)));
    }

    @PostMapping("/users/{id}/force-logout/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> forceLogout(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.forceLogout(id)));
    }

    @GetMapping("/videos/")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getVideos(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Map<String, Object>> videos = adminService.getVideosList(search, status, page, size);
        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .ok(true).results(videos.getContent()).page(page).pageSize(size)
                .total((int) videos.getTotalElements()).hasNext(videos.hasNext()).build());
    }

    @GetMapping("/videos/{id}/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVideoDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getVideoDetail(id)));
    }

    @PostMapping("/videos/batch-approve/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchApprove(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(adminService.batchApprove(body)));
    }

    @PostMapping("/videos/bulk-update/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkUpdate(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(adminService.bulkUpdate(body)));
    }

    @PostMapping("/videos/bulk-delete/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkDelete(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(adminService.bulkDelete(body)));
    }

    @PostMapping("/videos/{id}/retry-transcode/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> retryTranscode(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.retryTranscode(id)));
    }

    @PatchMapping("/videos/{id}/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> patchVideo(@PathVariable UUID id, @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updateAdminVideo(id, updates)));
    }

    @DeleteMapping("/videos/{id}/")
    public ResponseEntity<ApiResponse<Void>> deleteVideo(@PathVariable UUID id) {
        adminService.deleteAdminVideo(id);
        return ResponseEntity.ok(ApiResponse.success(null, "删除成功"));
    }

    @GetMapping("/comments/")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getComments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Map<String, Object>> comments = adminService.getComments(page, size);
        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .ok(true).results(comments.getContent()).page(page).pageSize(size)
                .total((int) comments.getTotalElements()).hasNext(comments.hasNext()).build());
    }

    @DeleteMapping("/comments/{id}/")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable UUID id) {
        adminService.deleteComment(id);
        return ResponseEntity.ok(ApiResponse.success(null, "删除成功"));
    }

    @GetMapping("/categories/")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategories(
            @RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getCategories(q)));
    }

    @PostMapping("/categories/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createCategory(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.createCategory(body.get("name"), body.get("description"))));
    }

    @GetMapping("/categories/{id}/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCategory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getCategory(id)));
    }

    @PatchMapping("/categories/{id}/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCategory(@PathVariable UUID id,
                                                                           @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.updateCategory(id, body.get("name"), body.get("description"))));
    }

    @DeleteMapping("/categories/{id}/")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        adminService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success(null, "删除成功"));
    }

    @GetMapping("/tags/")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTags(
            @RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getTags(q)));
    }

    @PostMapping("/tags/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTag(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.createTag(body.get("name"), body.get("description"))));
    }

    @GetMapping("/tags/{id}/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTag(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getTag(id)));
    }

    @PatchMapping("/tags/{id}/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateTag(@PathVariable UUID id,
                                                                      @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updateTag(id, body.get("name"))));
    }

    @DeleteMapping("/tags/{id}/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteTag(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.deleteTag(id)));
    }

    @GetMapping("/reports/")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String status) {
        Page<Map<String, Object>> reports = adminService.getReports(status, page, size);
        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .ok(true).results(reports.getContent()).page(page).pageSize(size)
                .total((int) reports.getTotalElements()).hasNext(reports.hasNext()).build());
    }

    @GetMapping("/audit-logs/")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Map<String, Object>> logs = adminService.getAuditLogs(page, size);
        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .ok(true).results(logs.getContent()).page(page).pageSize(size)
                .total((int) logs.getTotalElements()).hasNext(logs.hasNext()).build());
    }

    @GetMapping("/announcements/")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAnnouncements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Map<String, Object>> announcements = adminService.getAnnouncements(page, size);
        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .ok(true).results(announcements.getContent()).page(page).pageSize(size)
                .total((int) announcements.getTotalElements()).hasNext(announcements.hasNext()).build());
    }

    @GetMapping("/announcements/{id}/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnnouncement(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAnnouncement(id)));
    }

    @PostMapping("/announcements/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createAnnouncement(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(adminService.createAnnouncement(body)));
    }

    @PatchMapping("/announcements/{id}/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateAnnouncement(@PathVariable UUID id,
                                                                               @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updateAnnouncement(id, body)));
    }

    @DeleteMapping("/announcements/{id}/")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(@PathVariable UUID id) {
        adminService.deleteAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success(null, "删除成功"));
    }

    @PostMapping("/tags/bulk-delete/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkDeleteTags(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> rawIds = (List<String>) body.get("ids");
        List<UUID> ids = rawIds.stream().map(UUID::fromString).toList();
        return ResponseEntity.ok(ApiResponse.success(adminService.bulkDeleteTags(ids)));
    }

    @PostMapping("/tags/merge/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> mergeTags(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.mergeTags(UUID.fromString(body.get("source")), UUID.fromString(body.get("target")))));
    }

    @GetMapping("/videos/transcode-failures/")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTranscodeFailures(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getTranscodeFailures(page, size)));
    }

    @PostMapping("/switch-user/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> switchUser(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.switchUser(body.get("target_username"), body.get("target_password"))));
    }

    @PostMapping("/impersonate-exit/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> impersonateExit() {
        return ResponseEntity.ok(ApiResponse.success(adminService.impersonateExit()));
    }

    @GetMapping("/reports/{id}/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReportDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getReportDetail(id)));
    }

    @PostMapping("/reports/{id}/handle/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleReport(@PathVariable UUID id,
                                                                          @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.handleReport(id, body.get("action"), body.get("notes"))));
    }

    @GetMapping("/comments/{id}/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCommentDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getCommentDetail(id)));
    }

    @GetMapping("/videos/metrics-trend/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMetricsTrend(
            @RequestParam(defaultValue = "upload") String metric,
            @RequestParam(defaultValue = "7d") String range) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getMetricsTrend(metric, range)));
    }

    @GetMapping("/audit-logs/automod-summary/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAutomodSummary(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAutomodSummary(days)));
    }

    @PostMapping("/audit-logs/automod-rules/apply/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyAutomodRule(@RequestBody Map<String, String> body) {
        String ruleKind = body.getOrDefault("rule_kind", "");
        String contentType = body.getOrDefault("content_type", "comment");
        String action = body.getOrDefault("action", "add");
        String value = body.getOrDefault("value", "");
        if (value.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("审核规则值不能为空"));
        }
        String configKey;
        if ("keyword".equals(ruleKind) && "comment".equals(contentType)) {
            configKey = "COMMENT_BLOCKED_KEYWORDS";
        } else if ("keyword".equals(ruleKind) && "video".equals(contentType)) {
            configKey = "VIDEO_BLOCKED_KEYWORDS";
        } else if ("canonical".equals(ruleKind) && "comment".equals(contentType)) {
            configKey = "COMMENT_CANONICAL_RULES";
        } else if ("pattern".equals(ruleKind) && "comment".equals(contentType)) {
            configKey = "COMMENT_PATTERN_RULES";
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("不支持的审核规则类型"));
        }
        Map<String, Object> current = configService.getAdminConfig();
        Map<String, Object> updates = new java.util.LinkedHashMap<>();
        updates.put(configKey, value);
        configService.updateAdminConfig(updates);
        return ResponseEntity.ok(ApiResponse.success(Map.of("ok", true, "rule_kind", ruleKind,
                "content_type", contentType, "action", action)));
    }

    @PostMapping("/moderation/check/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> moderationHealthCheck() {
        return ResponseEntity.ok(ApiResponse.success(adminService.moderationHealthCheck()));
    }
}
