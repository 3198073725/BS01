package com.vidsprout.modules.video.service;

import com.vidsprout.modules.video.model.Video;
import com.vidsprout.modules.video.repository.VideoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Slf4j
@Service
public class HlsTranscodeService {

    private final VideoRepository videoRepository;

    @Value("${app.upload.dir:./media}")
    private String uploadDir;

    @Value("${app.hls.segment-duration:6}")
    private int segmentDuration;

    public HlsTranscodeService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @Async
    public void transcodeToHls(UUID videoId) {
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null) return;

        String videoRel = video.getVideoFileF();
        if (videoRel == null || videoRel.isEmpty()) {
            log.warn("视频文件不存在：{}", videoId);
            return;
        }

        Path videoPath = Path.of(uploadDir, videoRel);
        if (!Files.exists(videoPath)) {
            log.warn("视频文件不存在：{}", videoPath);
            return;
        }

        String vidKey = getVideoKey(videoRel);
        Path hlsDir = Path.of(uploadDir, "videos", "hls", vidKey);
        Path thumbsDir = Path.of(uploadDir, "videos", "thumbs");

        try {
            log.info("开始 HLS 转码：{}", videoId);
            
            Files.createDirectories(hlsDir);
            Files.createDirectories(thumbsDir);

            Map<String, Object> info = probeVideo(videoPath);
            int duration = (Integer) info.getOrDefault("duration", 0);
            int width = (Integer) info.getOrDefault("width", 0);
            int height = (Integer) info.getOrDefault("height", 0);

            video.setDuration(duration);
            video.setWidth(width);
            video.setHeight(height);
            videoRepository.save(video);

            Path thumbPath = thumbsDir.resolve(vidKey + ".jpg");
            extractThumbnail(videoPath, thumbPath, duration / 2);
            
            if (Files.exists(thumbPath)) {
                video.setThumbnail(("videos/thumbs/" + vidKey + ".jpg").substring(0, 100));
                video.setThumbnailF("videos/thumbs/" + vidKey + ".jpg");
                videoRepository.save(video);
            }

            generateVttThumbnails(videoPath, vidKey, duration);

            List<HlsProfile> profiles = selectProfiles(height);
            if (profiles.isEmpty()) {
                video.setStatus("published");
                video.setTranscodeError("无可用转码配置");
                videoRepository.save(video);
                return;
            }

            List<String> variantUris = new ArrayList<>();
            for (HlsProfile profile : profiles) {
                String variantUri = transcodeProfile(videoPath, hlsDir, profile);
                if (variantUri != null) {
                    variantUris.add(variantUri);
                }
            }

            if (variantUris.isEmpty()) {
                video.setStatus("banned");
                video.setTranscodeError("转码失败");
                videoRepository.save(video);
                return;
            }

            String masterM3u8 = createMasterPlaylist(hlsDir, profiles, variantUris);
            Path masterPath = hlsDir.resolve("master.m3u8");
            Files.writeString(masterPath, masterM3u8);

            video.setLowMp4("videos/hls/" + vidKey + "/480p/index.m3u8");
            video.setStatus("published");
            video.setPublishedAt(java.time.LocalDateTime.now());
            video.setTranscodeError(null);
            videoRepository.save(video);

            log.info("HLS 转码完成：{}", videoId);

        } catch (Exception e) {
            log.error("HLS 转码失败：{}", videoId, e);
            video.setStatus("banned");
            video.setTranscodeError("转码失败：" + e.getMessage());
            videoRepository.save(video);
        }
    }

    @Async
    public void generateVttAndThumbnails(UUID videoId) {
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null) return;

        String videoRel = video.getVideoFileF();
        if (videoRel == null) return;

        Path videoPath = Path.of(uploadDir, videoRel);
        String vidKey = getVideoKey(videoRel);

        try {
            generateVttThumbnails(videoPath, vidKey, video.getDuration());
        } catch (Exception e) {
            log.error("生成 VTT 缩略图失败：{}", videoId, e);
        }
    }

    private Map<String, Object> probeVideo(Path videoPath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe", "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height:format=duration",
                "-of", "json",
                videoPath.toString()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }
        process.waitFor();

        String json = output.toString();
        Map<String, Object> result = new HashMap<>();
        
        int dot = json.indexOf("\"width\"");
        if (dot > 0) {
            int start = json.indexOf(":", dot) + 1;
            int end = json.indexOf(",", start);
            result.put("width", Integer.parseInt(json.substring(start, end).trim()));
        }
        
        dot = json.indexOf("\"height\"");
        if (dot > 0) {
            int start = json.indexOf(":", dot) + 1;
            int end = json.indexOf(",", start);
            result.put("height", Integer.parseInt(json.substring(start, end).trim()));
        }
        
        dot = json.indexOf("\"duration\"");
        if (dot > 0) {
            int start = json.indexOf(":", dot) + 1;
            int end = json.indexOf("}", start);
            String durStr = json.substring(start, end).trim().replace("\"", "");
            result.put("duration", (int) Math.floor(Double.parseDouble(durStr)));
        }

        return result;
    }

    private void extractThumbnail(Path videoPath, Path outputPath, int tsSeconds) throws Exception {
        if (tsSeconds < 1) tsSeconds = 1;
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y", "-ss", String.valueOf(tsSeconds),
                "-i", videoPath.toString(),
                "-frames:v", "1", "-vf", "scale=480:-1",
                outputPath.toString()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor();
    }

    private void generateVttThumbnails(Path videoPath, String vidKey, int duration) throws Exception {
        if (duration <= 0) return;

        int interval = Math.max(1, duration / 100);
        int thumbWidth = 160;
        Path framesDir = Path.of(uploadDir, "videos", "thumbs", vidKey + "_vtt");
        Files.createDirectories(framesDir);

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y", "-i", videoPath.toString(),
                "-vf", String.format("fps=1/%d,scale=%d:-1", interval, thumbWidth),
                framesDir.resolve("thumb_%04d.jpg").toString()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor();

        File[] frames = framesDir.toFile().listFiles((d, name) -> name.startsWith("thumb_") && name.endsWith(".jpg"));
        if (frames == null || frames.length == 0) return;

        Arrays.sort(frames);
        Path vttPath = Path.of(uploadDir, "videos", "thumbs", vidKey + ".vtt");

        try (BufferedWriter writer = Files.newBufferedWriter(vttPath)) {
            writer.write("WEBVTT\n\n");
            for (int i = 0; i < frames.length; i++) {
                int start = i * interval;
                int end = Math.min((i + 1) * interval, duration);
                writer.write(String.format("%s --> %s\n", formatTimestamp(start), formatTimestamp(end)));
                writer.write("/media/videos/thumbs/" + vidKey + "_vtt/" + frames[i].getName() + "\n\n");
            }
        }
    }

    private String formatTimestamp(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d:%02d.000", h, m, s);
    }

    private List<HlsProfile> selectProfiles(int height) {
        List<HlsProfile> profiles = new ArrayList<>();
        if (height >= 720) {
            profiles.add(new HlsProfile("720p", 720, "2500k", "5000k", 1280));
        }
        profiles.add(new HlsProfile("480p", 480, "1200k", "2400k", 854));
        return profiles;
    }

    private String transcodeProfile(Path videoPath, Path hlsDir, HlsProfile profile) throws Exception {
        Path profileDir = hlsDir.resolve(profile.name);
        Files.createDirectories(profileDir);

        String segmentPattern = profileDir.resolve(profile.name + "_%03d.ts").toString();
        String playlistPath = profileDir.resolve("index.m3u8").toString();

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y",
                "-i", videoPath.toString(),
                "-vf", String.format("scale=-2:%d:flags=lanczos:force_original_aspect_ratio=decrease", profile.height),
                "-c:v", "libx264", "-preset", "veryfast",
                "-b:v", profile.bitrate, "-bufsize", profile.bufsize,
                "-c:a", "aac", "-ar", "48000", "-b:a", "128k",
                "-hls_time", String.valueOf(segmentDuration),
                "-hls_playlist_type", "vod",
                "-hls_segment_filename", segmentPattern,
                playlistPath
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("ffmpeg [{}]: {}", profile.name, line);
            }
        }

        int exitCode = process.waitFor();
        return exitCode == 0 ? profile.name + "/index.m3u8" : null;
    }

    private String createMasterPlaylist(Path hlsDir, List<HlsProfile> profiles, List<String> variantUris) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("#EXTM3U\n");
        sb.append("#EXT-X-VERSION:3\n");

        for (int i = 0; i < profiles.size() && i < variantUris.size(); i++) {
            HlsProfile p = profiles.get(i);
            String uri = variantUris.get(i);
            int bw = Integer.parseInt(p.bitrate.replace("k", "")) * 1000;
            sb.append(String.format("#EXT-X-STREAM-INF:BANDWIDTH=%d,RESOLUTION=%dx%d\n", bw, p.width, p.height));
            sb.append(uri).append("\n");
        }

        return sb.toString();
    }

    private String getVideoKey(String videoRel) {
        String filename = Paths.get(videoRel).getFileName().toString();
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(0, dot) : filename;
    }

    static class HlsProfile {
        String name;
        int height;
        String bitrate;
        String bufsize;
        int width;

        HlsProfile(String name, int height, String bitrate, String bufsize, int width) {
            this.name = name;
            this.height = height;
            this.bitrate = bitrate;
            this.bufsize = bufsize;
            this.width = width;
        }
    }
}
