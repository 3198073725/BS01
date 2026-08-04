package com.vidsprout.modules.video.service;

import com.vidsprout.common.exception.BusinessException;
import com.vidsprout.common.exception.ResourceNotFoundException;
import com.vidsprout.modules.video.model.Video;
import com.vidsprout.modules.video.repository.VideoRepository;
import com.vidsprout.modules.user.model.User;
import com.vidsprout.modules.user.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ThumbnailService {

    private final VideoRepository videoRepository;
    private final AuthService authService;

    @Value("${app.upload.dir:./media}")
    private String uploadDir;

    @Value("${app.thumbnail.min-width:480}")
    private int minWidth;

    @Value("${app.thumbnail.min-height:270}")
    private int minHeight;

    @Value("${app.thumbnail.ratio-tolerance:0.04}")
    private double ratioTolerance;

    @Value("${app.thumbnail.max-size-bytes:5242880}")
    private long maxSizeBytes;

    public ThumbnailService(VideoRepository videoRepository, AuthService authService) {
        this.videoRepository = videoRepository;
        this.authService = authService;
    }

    @Transactional
    public Map<String, Object> pickThumbnail(UUID videoId, double tsSeconds) {
        User user = authService.getCurrentUserEntity();
        Video video = videoRepository.findByIdAndUserId(videoId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在或无权编辑"));

        String videoRel = video.getVideoFileF();
        if (videoRel == null || videoRel.isEmpty()) {
            throw new BusinessException("视频文件不存在");
        }

        Path videoPath = Path.of(uploadDir, videoRel);
        if (!Files.exists(videoPath)) {
            throw new BusinessException("视频文件不存在");
        }

        String vidKey = getVideoKey(videoRel);
        String thumbRel = "videos/thumbs/" + vidKey + ".jpg";
        Path thumbPath = Path.of(uploadDir, thumbRel);

        try {
            Files.createDirectories(thumbPath.getParent());
            extractFrame(videoPath, thumbPath, tsSeconds);
            
            video.setThumbnail(thumbRel.substring(0, Math.min(100, thumbRel.length())));
            video.setThumbnailF(thumbRel);
            videoRepository.save(video);

            String thumbUrl = "/media/" + thumbRel;
            return Map.of("thumbnail_url", thumbUrl, "success", true);

        } catch (Exception e) {
            log.error("生成缩略图失败", e);
            throw new BusinessException("生成封面失败：" + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> uploadThumbnail(UUID videoId, MultipartFile file) {
        User user = authService.getCurrentUserEntity();
        Video video = videoRepository.findByIdAndUserId(videoId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在或无权编辑"));

        if (file.isEmpty()) {
            throw new BusinessException("未收到文件");
        }

        if (file.getSize() > maxSizeBytes) {
            throw new BusinessException("图片过大（最大 5MB）");
        }

        String ext = getExtension(file.getOriginalFilename()).toLowerCase();
        if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png") && !ext.equals(".webp")) {
            throw new BusinessException("不支持的图片格式（仅支持 JPG/PNG/WEBP）");
        }

        String videoRel = video.getVideoFileF();
        if (videoRel == null || videoRel.isEmpty()) {
            throw new BusinessException("视频文件不存在");
        }

        String vidKey = getVideoKey(videoRel);
        String thumbRel = "videos/thumbs/" + vidKey + "_custom" + ext;
        Path thumbPath = Path.of(uploadDir, thumbRel);

        try {
            Files.createDirectories(thumbPath.getParent());
            file.transferTo(thumbPath.toFile());

            BufferedImage img = ImageIO.read(thumbPath.toFile());
            if (img == null) {
                Files.deleteIfExists(thumbPath);
                throw new BusinessException("无效的图片文件");
            }

            int width = img.getWidth();
            int height = img.getHeight();

            if (width < minWidth || height < minHeight) {
                Files.deleteIfExists(thumbPath);
                throw new BusinessException(String.format("图片分辨率过低（至少 %dx%d）", minWidth, minHeight));
            }

            double ratio = (double) width / height;
            double targetRatio = 16.0 / 9.0;
            if (Math.abs(ratio - targetRatio) > ratioTolerance) {
                Files.deleteIfExists(thumbPath);
                throw new BusinessException("图片宽高比例必须接近 16:9");
            }

            video.setThumbnail(thumbRel.substring(0, Math.min(100, thumbRel.length())));
            video.setThumbnailF(thumbRel);
            videoRepository.save(video);

            String thumbUrl = "/media/" + thumbRel;
            return Map.of("thumbnail_url", thumbUrl, "success", true, "width", width, "height", height);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传缩略图失败", e);
            throw new BusinessException("上传封面失败：" + e.getMessage());
        }
    }

    private void extractFrame(Path videoPath, Path outputPath, double tsSeconds) throws Exception {
        int ts = Math.max(1, (int) tsSeconds);
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y", "-ss", String.valueOf(ts),
                "-i", videoPath.toString(),
                "-frames:v", "1", "-vf", "scale=480:-1",
                outputPath.toString()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("ffmpeg: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0 || !Files.exists(outputPath)) {
            throw new BusinessException("ffmpeg 执行失败");
        }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }

    private String getVideoKey(String videoRel) {
        String filename = Paths.get(videoRel).getFileName().toString();
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(0, dot) : filename;
    }
}
