package com.vidsprout.modules.notification.service;

import com.vidsprout.common.exception.BusinessException;
import com.vidsprout.modules.notification.model.SystemAnnouncement;
import com.vidsprout.modules.notification.model.SystemAnnouncementRead;
import com.vidsprout.modules.notification.repository.AnnouncementRepository;
import com.vidsprout.modules.notification.repository.SystemAnnouncementReadRepository;
import com.vidsprout.modules.user.model.User;
import com.vidsprout.modules.user.service.AuthService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 公告与通知服务，完整复刻 Django notifications/views.py 的公告端点行为：
 * 公告列表（含 is_read 标注、排序、分页）、详情、已读标记、未读计数、最新未读。
 */
@Service
public class NotificationService {

    private final AnnouncementRepository announcementRepository;
    private final SystemAnnouncementReadRepository readRepository;
    private final AuthService authService;

    public NotificationService(AnnouncementRepository announcementRepository,
                               SystemAnnouncementReadRepository readRepository,
                               AuthService authService) {
        this.announcementRepository = announcementRepository;
        this.readRepository = readRepository;
        this.authService = authService;
    }

    public Page<Map<String, Object>> getAnnouncements(int page, int size, boolean includeInactive) {
        User user = authService.getCurrentUserEntity();
        var pageable = PageRequest.of(Math.max(0, page - 1), size);
        Page<SystemAnnouncement> p = includeInactive
                ? announcementRepository.findAllByOrderByPinnedDescPublishedAtDescCreatedAtDesc(pageable)
                : announcementRepository.findByIsActiveTrueOrderByPinnedDescPublishedAtDescCreatedAtDesc(pageable);

        List<UUID> ids = p.getContent().stream().map(SystemAnnouncement::getId).toList();
        Set<UUID> readIds;
        if (ids.isEmpty()) {
            readIds = Set.of();
        } else {
            readIds = readRepository.findByUserIdAndAnnouncementIdIn(user.getId(), ids).stream()
                    .map(r -> r.getAnnouncement().getId())
                    .collect(Collectors.toSet());
        }

        List<Map<String, Object>> rows = p.getContent().stream()
                .map(a -> toAnnouncementMap(a, readIds.contains(a.getId())))
                .toList();
        return new PageImpl<>(rows, pageable, p.getTotalElements());
    }

    public Map<String, Object> getAnnouncement(UUID id) {
        User user = authService.getCurrentUserEntity();
        SystemAnnouncement a = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException("公告不存在"));
        boolean isRead = readRepository.existsByAnnouncementIdAndUserId(id, user.getId());
        return toAnnouncementMap(a, isRead);
    }

    public void markAnnouncementRead(UUID id) {
        User user = authService.getCurrentUserEntity();
        SystemAnnouncement a = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException("公告不存在"));
        if (!readRepository.existsByAnnouncementIdAndUserId(id, user.getId())) {
            readRepository.save(SystemAnnouncementRead.builder()
                    .announcement(a)
                    .user(user)
                    .build());
        }
    }

    public long getUnreadAnnouncementCount() {
        User user = authService.getCurrentUserEntity();
        return announcementRepository.countUnread(user.getId());
    }

    public Map<String, Object> getLatestUnreadAnnouncement() {
        User user = authService.getCurrentUserEntity();
        List<SystemAnnouncement> results = announcementRepository.findLatestUnread(
                user.getId(), PageRequest.of(0, 1));
        if (results.isEmpty()) {
            return null;
        }
        SystemAnnouncement a = results.get(0);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", a.getId().toString());
        item.put("title", a.getTitle());
        item.put("content", a.getContent());
        item.put("pinned", a.getPinned());
        item.put("published_at", a.getPublishedAt());
        return item;
    }

    private Map<String, Object> toAnnouncementMap(SystemAnnouncement a, boolean isRead) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId().toString());
        m.put("title", a.getTitle());
        m.put("content", a.getContent());
        m.put("is_active", a.getIsActive());
        m.put("pinned", a.getPinned());
        m.put("published_at", a.getPublishedAt());
        m.put("created_at", a.getCreatedAt());
        m.put("updated_at", a.getUpdatedAt());
        m.put("is_read", isRead);
        return m;
    }
}
