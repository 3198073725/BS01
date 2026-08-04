package com.vidsprout.modules.video.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

/**
 * 对齐 Django Celery 定时任务：
 * cleanup_expired_upload_sessions：每 24 小时清理超过 24 小时的分片上传会话。
 */
@Slf4j
@Component
public class ScheduledTasks {

    @Value("${app.upload.dir:./media}")
    private String uploadDir;

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredUploadSessions() {
        cleanupExpiredUploadSessions(24);
    }

    public void cleanupExpiredUploadSessions(int hours) {
        Path sessionsDir = Path.of(uploadDir, "uploads", "sessions");
        if (!Files.exists(sessionsDir)) {
            return;
        }
        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        int cleaned = 0;
        long freedBytes = 0;

        try (Stream<Path> entries = Files.list(sessionsDir)) {
            for (Path sessionPath : entries.toList()) {
                if (!Files.isDirectory(sessionPath)) {
                    continue;
                }
                Path metaPath = sessionPath.resolve("meta.json");
                try {
                    if (Files.exists(metaPath)) {
                        FileTime mtime = Files.getLastModifiedTime(metaPath);
                        if (mtime.toInstant().isAfter(cutoff)) {
                            continue;
                        }
                    }
                    long size = dirSize(sessionPath);
                    deleteRecursively(sessionPath);
                    cleaned++;
                    freedBytes += size;
                } catch (IOException e) {
                    log.warn("清理上传会话失败 {}: {}", sessionPath, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("扫描上传会话目录失败: {}", e.getMessage());
        }
        if (cleaned > 0) {
            log.info("已清理 {} 个过期上传会话，释放 {} 字节", cleaned, freedBytes);
        }
    }

    private long dirSize(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        long size = 0;
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.toList()) {
                try {
                    size += Files.size(p);
                } catch (IOException ignored) {
                }
            }
        }
        return size;
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                for (Path entry : entries.toList()) {
                    deleteRecursively(entry);
                }
            }
        }
        Files.deleteIfExists(path);
    }
}
