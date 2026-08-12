package com.vidsprout.modules.video.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidsprout.common.exception.BusinessException;
import com.vidsprout.modules.content.repository.CategoryRepository;
import com.vidsprout.modules.content.repository.TagRepository;
import com.vidsprout.modules.interaction.repository.FavoriteRepository;
import com.vidsprout.modules.interaction.repository.LikeRepository;
import com.vidsprout.modules.interaction.repository.WatchLaterRepository;
import com.vidsprout.modules.user.repository.UserRepository;
import com.vidsprout.modules.user.service.AuthService;
import com.vidsprout.modules.video.repository.VideoRepository;
import com.vidsprout.modules.video.repository.VideoTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class VideoServiceSecurityTest {

    private VideoService service;

    @BeforeEach
    void setUp() {
        service = new VideoService(
                mock(VideoRepository.class), mock(AuthService.class), new ObjectMapper(),
                mock(VideoTagRepository.class), mock(FavoriteRepository.class),
                mock(LikeRepository.class), mock(WatchLaterRepository.class),
                mock(CategoryRepository.class), mock(TagRepository.class),
                mock(UserRepository.class));
        ReflectionTestUtils.setField(service, "uploadDir", "/tmp/media");
        ReflectionTestUtils.setField(service, "allowedExtensions", ".mp4,.mov,.m4v,.webm,.mkv");
    }

    private Path sessionDir(String uploadId) throws Exception {
        Method m = VideoService.class.getDeclaredMethod("sessionDir", String.class);
        m.setAccessible(true);
        try {
            return (Path) m.invoke(service, uploadId);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            throw new RuntimeException(cause);
        }
    }

    private boolean allowed(String ext) throws Exception {
        Method m = VideoService.class.getDeclaredMethod("isAllowedExtension", String.class);
        m.setAccessible(true);
        return (boolean) m.invoke(service, ext);
    }

    @Test
    @DisplayName("合法 32 位十六进制 uploadId 生成会话目录")
    void acceptsValidUploadId() throws Exception {
        Path p = sessionDir("0123456789abcdef0123456789abcdef");
        assertTrue(p.toString().contains("sessions/0123456789abcdef0123456789abcdef"));
    }

    @Test
    @DisplayName("路径穿越 uploadId 被拒绝")
    void rejectsTraversalUploadId() {
        assertThrows(BusinessException.class, () -> sessionDir("../../../../tmp/x"));
        assertThrows(BusinessException.class, () -> sessionDir("..%2f..%2fetc"));
        assertThrows(BusinessException.class, () -> sessionDir("abc"));
        assertThrows(BusinessException.class, () -> sessionDir("/etc/passwd"));
    }

    @Test
    @DisplayName("分片上传扩展名白名单：拒绝 html/svg")
    void rejectsHtmlSvgExtension() throws Exception {
        assertEquals(false, allowed(".html"));
        assertEquals(false, allowed(".svg"));
        assertEquals(false, allowed(".js"));
        assertEquals(false, allowed("html"));
    }

    @Test
    @DisplayName("分片上传扩展名白名单：接受视频格式")
    void acceptsVideoExtensions() throws Exception {
        assertEquals(true, allowed(".mp4"));
        assertEquals(true, allowed(".MP4"));
        assertEquals(true, allowed(".webm"));
        assertEquals(true, allowed(".mkv"));
    }

    @Test
    @DisplayName("空 uploadId 拒绝")
    void rejectsNullUploadId() {
        assertThrows(BusinessException.class, () -> sessionDir(null));
    }

    @Test
    @DisplayName("sessionDir 归一化后仍在 uploadDir 之下")
    void normalizedSessionStaysInsideUploadDir() throws Exception {
        Path p = sessionDir("0123456789abcdef0123456789abcdef");
        assertDoesNotThrow(() -> p.normalize().startsWith(Path.of("/tmp/media").normalize()));
    }
}
