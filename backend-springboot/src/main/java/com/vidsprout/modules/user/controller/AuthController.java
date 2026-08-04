package com.vidsprout.modules.user.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidsprout.common.ApiResponse;
import com.vidsprout.modules.user.dto.*;
import com.vidsprout.modules.user.service.AuthService;
import com.vidsprout.modules.user.service.EmailService;
import com.vidsprout.security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    @Value("${app.upload.dir:./media}")
    private String uploadDir;

    @Value("${app.contact.email-to:}")
    private String contactEmailTo;

    private static final String AVATAR_DIR = "avatars";
    private static final int AVATAR_SIZE = 512;
    private static final int THUMB_SIZE = 256;
    private static final long AVATAR_MAX_BYTES = 2 * 1024 * 1024;
    private static final long AVATAR_MAX_PIXELS = 25_000_000;
    private static final long QR_CREATE_TTL = 300;
    private static final long QR_CONFIRM_TTL = 60;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public AuthController(AuthService authService,
                          RedisTemplate<String, String> redisTemplate,
                          JwtUtils jwtUtils,
                          ObjectMapper objectMapper,
                          EmailService emailService) {
        this.authService = authService;
        this.redisTemplate = redisTemplate;
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
        this.emailService = emailService;
    }

    @PostMapping("/token/")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse token = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(token, "登录成功"));
    }

    @PostMapping("/token/refresh/")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refresh");
        TokenResponse token = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(token));
    }

    @PostMapping("/token/verify/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyToken(@RequestBody Map<String, String> body) {
        authService.verifyToken(body.get("token"));
        return ResponseEntity.ok(ApiResponse.success(Map.of()));
    }

    @PostMapping("/users/register/")
    public ResponseEntity<ApiResponse<UserProfileResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserProfileResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(user));
    }

    @GetMapping("/users/me/")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser() {
        UserProfileResponse user = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping("/users/{id}/")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(@PathVariable UUID id) {
        UserProfileResponse user = authService.getUserProfile(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/users/logout/")
    public ResponseEntity<ApiResponse<Void>> logout() {
        authService.logout();
        return ResponseEntity.ok(ApiResponse.success(null, "已退出登录"));
    }

    @PostMapping("/users/change-password/")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody ChangePasswordRequest request) {
        authService.changePassword(request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "密码修改成功"));
    }

    @PatchMapping("/users/me/")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUser(@RequestBody UserUpdateRequest request) {
        UserProfileResponse user = authService.updateUser(request);
        return ResponseEntity.ok(ApiResponse.success(user, "资料更新成功"));
    }

    @GetMapping("/users/check-username/")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> checkUsername(@RequestParam String username) {
        AvailabilityResponse result = authService.checkUsername(username);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/users/check-email/")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> checkEmail(@RequestParam String email) {
        AvailabilityResponse result = authService.checkEmail(email);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/users/by-username/{username}/")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserByUsername(@PathVariable String username) {
        UserProfileResponse user = authService.getUserByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/users/verify-email/request/")
    public ResponseEntity<ApiResponse<Void>> requestEmailVerification() {
        authService.requestEmailVerification();
        return ResponseEntity.ok(ApiResponse.success(null, "验证邮件已发送"));
    }

    @PostMapping("/users/verify-email/confirm/")
    public ResponseEntity<ApiResponse<Void>> confirmEmailVerification(@RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.getUid(), request.getToken());
        return ResponseEntity.ok(ApiResponse.success(null, "邮箱验证成功"));
    }

    @PostMapping("/users/password-reset/request/")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "如果邮箱已注册，重置邮件将发送至该邮箱"));
    }

    @PostMapping("/users/password-reset/confirm/")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(@RequestBody PasswordResetConfirmRequest request) {
        authService.resetPassword(request.getUid(), request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "密码重置成功"));
    }

    @PostMapping("/users/login/send-code/")
    public ResponseEntity<ApiResponse<LoginCodeResponse>> sendLoginCode(@RequestBody LoginSendCodeRequest request) {
        LoginCodeResponse response = authService.sendLoginCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/users/login/with-code/")
    public ResponseEntity<ApiResponse<TokenResponse>> loginWithCode(@RequestBody LoginWithCodeRequest request) {
        TokenResponse token = authService.loginWithCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success(token, "登录成功"));
    }

    @GetMapping("/users/search/")
    public ResponseEntity<ApiResponse<UserSearchResponse>> searchUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false, defaultValue = "relevance") String order,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        UserSearchResponse result = authService.searchUsers(q, verified, order, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/users/username/change/")
    public ResponseEntity<ApiResponse<UsernameChangeResponse>> changeUsername(@RequestBody UsernameChangeRequest request) {
        UsernameChangeResponse result = authService.changeUsername(request.getUsername());
        return ResponseEntity.ok(ApiResponse.success(result, "用户名修改成功"));
    }

    @PostMapping("/users/email/change/request/")
    public ResponseEntity<ApiResponse<Void>> requestEmailChange(@RequestBody EmailChangeRequest request) {
        authService.requestEmailChange(request.getNewEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "改绑确认邮件已发送"));
    }

    @PostMapping("/users/email/change/confirm/")
    public ResponseEntity<ApiResponse<Void>> confirmEmailChange(@RequestBody Map<String, String> body) {
        authService.confirmEmailChange(body.get("token"));
        return ResponseEntity.ok(ApiResponse.success(null, "邮箱改绑成功"));
    }

    @GetMapping("/users/popup/stats/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserPopupStats(
            @RequestParam UUID userId,
            @RequestParam(required = false) Boolean force) {
        Map<String, Object> stats = authService.getUserPopupStats(userId, force);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/users/ping/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ping() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("ok", true)));
    }

    @PostMapping(value = "/users/avatar/upload/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                                         @RequestParam(required = false) Integer x,
                                                                         @RequestParam(required = false) Integer y,
                                                                         @RequestParam(required = false) Integer w,
                                                                         @RequestParam(required = false) Integer h) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("未收到文件"));
        }
        if (file.getSize() > AVATAR_MAX_BYTES) {
            return ResponseEntity.badRequest().body(ApiResponse.error("文件过大"));
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals(MediaType.IMAGE_JPEG_VALUE)
                && !contentType.equals(MediaType.IMAGE_PNG_VALUE))) {
            return ResponseEntity.badRequest().body(ApiResponse.error("仅支持 JPEG/PNG"));
        }
        BufferedImage img;
        try (InputStream is = file.getInputStream()) {
            img = ImageIO.read(is);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("非法图片文件"));
        }
        if (img == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("非法图片文件"));
        }
        int mw = img.getWidth();
        int mh = img.getHeight();
        if ((long) mw * mh > AVATAR_MAX_PIXELS) {
            return ResponseEntity.badRequest().body(ApiResponse.error("图片像素过大"));
        }
        int cx, cy, cw, ch;
        if (x != null && y != null && w != null && h != null) {
            cx = x; cy = y; cw = w; ch = h;
        } else {
            int side = Math.min(mw, mh);
            cx = (mw - side) / 2; cy = (mh - side) / 2;
            cw = ch = side;
        }
        int left = Math.max(0, cx), top = Math.max(0, cy);
        int right = Math.min(mw, cx + cw), bottom = Math.min(mh, cy + ch);
        if (right <= left || bottom <= top) {
            return ResponseEntity.badRequest().body(ApiResponse.error("裁剪区域无效"));
        }
        BufferedImage cropped = img.getSubimage(left, top, right - left, bottom - top);
        BufferedImage avatarImg = resizeImage(cropped, AVATAR_SIZE, AVATAR_SIZE);
        BufferedImage thumbImg = resizeImage(cropped, THUMB_SIZE, THUMB_SIZE);
        boolean isJpeg = MediaType.IMAGE_JPEG_VALUE.equals(contentType);
        String ext = isJpeg ? ".jpg" : ".png";
        UUID userId = authService.getCurrentUserEntity().getId();
        String base = userId.toString();
        String avatarFilename = base + ext;
        String thumbFilename = base + "_thumb" + ext;
        try {
            Path avatarsDir = Paths.get(uploadDir, AVATAR_DIR);
            Files.createDirectories(avatarsDir);
            if (isJpeg) {
                BufferedImage rgbAvatar = convertToRgb(avatarImg);
                BufferedImage rgbThumb = convertToRgb(thumbImg);
                ImageIO.write(rgbAvatar, "JPEG", avatarsDir.resolve(avatarFilename).toFile());
                ImageIO.write(rgbThumb, "JPEG", avatarsDir.resolve(thumbFilename).toFile());
            } else {
                ImageIO.write(avatarImg, "PNG", avatarsDir.resolve(avatarFilename).toFile());
                ImageIO.write(thumbImg, "PNG", avatarsDir.resolve(thumbFilename).toFile());
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("头像保存失败"));
        }
        String relAvatar = AVATAR_DIR + "/" + avatarFilename;
        String relThumb = AVATAR_DIR + "/" + thumbFilename;
        try {
            authService.updateProfilePicture(relAvatar, relThumb);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新头像失败"));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "profile_picture", relAvatar, "profile_picture_thumb", relThumb)));
    }

    @PostMapping("/users/contact/submit/")
    public ResponseEntity<ApiResponse<Void>> contactSubmit(@RequestBody Map<String, Object> body) {
        String name = toString(body, "name", 0, 60);
        String email = toString(body, "email", 1, 255);
        String subject = toString(body, "subject", 0, 120);
        String message = toString(body, "message", 1, 4000);
        if (message == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("内容不能为空"));
        }
        if (email != null && !email.contains("@")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("邮箱格式不正确"));
        }
        if (subject == null || subject.isEmpty()) {
            subject = message.length() > 30 ? message.substring(0, 30) + "..." : message;
        }
        String type = toString(body, "type", 0, 20);
        if (type != null) {
            type = type.toLowerCase();
            if (!type.matches("feedback|business|infringement|privacy|other")) {
                type = "other";
            }
        } else {
            type = "other";
        }
        if (contactEmailTo == null || contactEmailTo.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("服务暂不可用：未配置收件邮箱"));
        }
        String emailBody = String.format(
                "提交类型: %s\n姓名: %s\n邮箱: %s\n\n%s", type,
                name != null ? name : "(未留)", email != null ? email : "(未留)", message);
        String emailSubject = "[Contact][" + type + "] " + subject;
        try {
            emailService.sendPlainText(contactEmailTo, emailSubject, emailBody);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("发送失败，请稍后再试"));
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/users/login/qr/create/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> qrLoginCreate(HttpServletRequest request) {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        String session = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", "pending");
            data.put("ip", ip != null ? ip : "unknown");
            data.put("ua", ua != null ? ua.substring(0, Math.min(ua.length(), 200)) : "");
            data.put("created_at", System.currentTimeMillis() / 1000);
            redisTemplate.opsForValue().set("qr_login:" + session,
                    objectMapper.writeValueAsString(data), Duration.ofSeconds(QR_CREATE_TTL));
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("创建会话失败"));
        }
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null) scheme = request.getScheme();
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null) host = request.getHeader("Host");
        if (host == null) host = "localhost:8000";
        String confirmUrl = scheme + "://" + host + "/api/users/login/qr/confirm/?session=" + session;
        String encoded = URLEncoder.encode(confirmUrl, StandardCharsets.UTF_8);
        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=248x248&data=" + encoded;
        return ResponseEntity.ok(ApiResponse.success(Map.of("session", session, "qr_url", qrUrl)));
    }

    @GetMapping("/users/login/qr/status/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> qrLoginStatus(@RequestParam String session) {
        if (session == null || session.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("缺少参数"));
        }
        String key = "qr_login:" + session;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return ResponseEntity.ok(ApiResponse.success(Map.of("status", "pending")));
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(json, Map.class);
            if ("confirmed".equals(data.get("status"))) {
                redisTemplate.delete(key);
                return ResponseEntity.ok(ApiResponse.success(Map.of(
                        "status", "confirmed",
                        "access", data.get("access"),
                        "refresh", data.get("refresh"),
                        "user", data.get("user"))));
            }
            return ResponseEntity.ok(ApiResponse.success(Map.of("status", "pending")));
        } catch (JsonProcessingException e) {
            return ResponseEntity.ok(ApiResponse.success(Map.of("status", "pending")));
        }
    }

    @GetMapping("/users/login/qr/confirm/")
    public ResponseEntity<ApiResponse<Void>> qrLoginConfirmGet(@RequestParam String session) {
        return qrLoginConfirmInternal(session);
    }

    @PostMapping("/users/login/qr/confirm/")
    public ResponseEntity<ApiResponse<Void>> qrLoginConfirmPost(@RequestBody Map<String, String> body,
                                                                  @RequestParam(required = false) String sessionParam) {
        String session = body.containsKey("session") ? body.get("session") : sessionParam;
        return qrLoginConfirmInternal(session);
    }

    private ResponseEntity<ApiResponse<Void>> qrLoginConfirmInternal(String session) {
        if (session == null || session.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("缺少参数 session"));
        }
        String key = "qr_login:" + session;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("会话不存在或已过期"));
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(json, Map.class);
            if ("confirmed".equals(data.get("status"))) {
                return ResponseEntity.badRequest().body(ApiResponse.error("会话已确认，不能重复使用"));
            }
            var user = authService.getCurrentUserEntity();
            String access = jwtUtils.generateAccessToken(user.getId(), user.getUsername());
            String refresh = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());
            Map<String, Object> confirmed = new LinkedHashMap<>();
            confirmed.put("status", "confirmed");
            confirmed.put("access", access);
            confirmed.put("refresh", refresh);
            confirmed.put("user", Map.of(
                    "id", user.getId().toString(),
                    "username", user.getUsername(),
                    "nickname", user.getNickname() != null ? user.getNickname() : user.getUsername()));
            redisTemplate.opsForValue().set(key,
                    objectMapper.writeValueAsString(confirmed), Duration.ofSeconds(QR_CONFIRM_TTL));
            return ResponseEntity.noContent().build();
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("处理失败"));
        }
    }

    private BufferedImage resizeImage(BufferedImage source, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, source.getType());
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    private BufferedImage convertToRgb(BufferedImage img) {
        if (img.getType() == BufferedImage.TYPE_INT_RGB) {
            return img;
        }
        BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return rgb;
    }

    private String toString(Map<String, Object> body, String key, int minLen, int maxLen) {
        Object v = body.get(key);
        if (v == null) {
            return minLen == 0 ? null : "";
        }
        String s = v.toString().trim();
        if (s.isEmpty() || s.length() < minLen) {
            return minLen == 0 ? null : "";
        }
        if (s.length() > maxLen) {
            return s.substring(0, maxLen);
        }
        return s;
    }
}
