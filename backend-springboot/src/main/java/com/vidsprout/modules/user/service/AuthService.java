package com.vidsprout.modules.user.service;

import com.vidsprout.common.exception.BusinessException;
import com.vidsprout.common.exception.ResourceNotFoundException;
import com.vidsprout.common.exception.UnauthorizedException;
import com.vidsprout.modules.user.dto.*;
import com.vidsprout.modules.user.model.User;
import com.vidsprout.modules.user.repository.UserRepository;
import com.vidsprout.security.JwtUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils,
                       RedisTemplate<String, String> redisTemplate,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("用户名或密码错误"));
        if (!user.getIsActive()) {
            throw new UnauthorizedException("账号已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("用户名或密码错误");
        }
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());
        return TokenResponse.builder().access(accessToken).refresh(refreshToken).build();
    }

    public TokenResponse refreshToken(String refreshToken) {
        if (!jwtUtils.validateRefreshToken(refreshToken)) {
            throw new UnauthorizedException("刷新令牌无效或已过期");
        }
        UUID userId = jwtUtils.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("用户不存在"));
        String newAccessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername());
        String newRefreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());
        return TokenResponse.builder().access(newAccessToken).refresh(newRefreshToken).build();
    }

    public void verifyToken(String token) {
        if (!jwtUtils.validateToken(token)) {
            throw new UnauthorizedException("令牌无效");
        }
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessException("用户名已存在");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("邮箱已被注册");
        }
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getUsername())
                .isActive(true)
                .build();
        user = userRepository.save(user);
        return toProfileResponse(user);
    }

    public UserProfileResponse getCurrentUser() {
        User user = getCurrentUserEntity();
        return toProfileResponse(user);
    }

    public UserProfileResponse getUserProfile(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        return toProfileResponse(user, isViewingSelf(user.getId()));
    }

    public User getCurrentUserEntity() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        if (principal instanceof String && !"anonymousUser".equals(principal)) {
            UUID userId = UUID.fromString((String) principal);
            return userRepository.findById(userId)
                    .orElseThrow(() -> new UnauthorizedException("用户未登录"));
        }
        throw new UnauthorizedException("用户未登录");
    }

    public User getCurrentUserEntityOrNull() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        if (principal instanceof String && !"anonymousUser".equals(principal)) {
            UUID userId = UUID.fromString((String) principal);
            return userRepository.findById(userId).orElse(null);
        }
        return null;
    }

    public void logout() {
        User user = getCurrentUserEntity();
        long now = System.currentTimeMillis() / 1000;
        String key = "logout_after:" + user.getId().toString();
        redisTemplate.opsForValue().set(key, String.valueOf(now), Duration.ofDays(7));
    }

    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        User user = getCurrentUserEntity();
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public UserProfileResponse updateUser(UserUpdateRequest request) {
        User user = getCurrentUserEntity();
        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getBirthDate() != null) user.setBirthDate(request.getBirthDate());
        if (request.getLocation() != null) user.setLocation(request.getLocation());
        if (request.getWebsite() != null) user.setWebsite(request.getWebsite());
        if (request.getPrivacyMode() != null) user.setPrivacyMode(request.getPrivacyMode());
        user = userRepository.save(user);
        return toProfileResponse(user);
    }

    public AvailabilityResponse checkUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return AvailabilityResponse.builder().available(false).reason("用户名不能为空").build();
        }
        if (userRepository.findByUsername(username).isPresent()) {
            return AvailabilityResponse.builder().available(false).reason("用户名已被使用").build();
        }
        return AvailabilityResponse.builder().available(true).reason(null).build();
    }

    public AvailabilityResponse checkEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return AvailabilityResponse.builder().available(false).reason("邮箱不能为空").build();
        }
        if (userRepository.findByEmail(email).isPresent()) {
            return AvailabilityResponse.builder().available(false).reason("邮箱已被注册").build();
        }
        return AvailabilityResponse.builder().available(true).reason(null).build();
    }

    public UserProfileResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        return toProfileResponse(user, isViewingSelf(user.getId()));
    }

    @Transactional
    public void requestEmailVerification() {
        User user = getCurrentUserEntity();
        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new BusinessException("邮箱已验证");
        }
        String token = java.util.UUID.randomUUID().toString();
        String key = "email_verify:" + user.getId();
        redisTemplate.opsForValue().set(key, token, Duration.ofHours(24));
        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(),
                user.getId().toString(), token);
    }

    @Transactional
    public void verifyEmail(String uid, String token) {
        UUID userId;
        try {
            userId = UUID.fromString(uid);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的用户 ID");
        }
        String key = "email_verify:" + userId;
        String storedToken = redisTemplate.opsForValue().get(key);
        if (storedToken == null || !storedToken.equals(token)) {
            throw new BusinessException("验证链接无效或已过期");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        user.setIsEmailVerified(true);
        userRepository.save(user);
        redisTemplate.delete(key);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            String token = java.util.UUID.randomUUID().toString();
            String key = "password_reset:" + user.getId();
            redisTemplate.opsForValue().set(key, token, Duration.ofHours(1));
            emailService.sendPasswordResetEmail(email, user.getUsername(),
                    user.getId().toString(), token);
        }
    }

    @Transactional
    public void resetPassword(String uid, String token, String newPassword) {
        UUID userId;
        try {
            userId = UUID.fromString(uid);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的用户 ID");
        }
        String key = "password_reset:" + userId;
        String storedToken = redisTemplate.opsForValue().get(key);
        if (storedToken == null || !storedToken.equals(token)) {
            throw new BusinessException("重置链接无效或已过期");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        redisTemplate.delete(key);
    }

    public LoginCodeResponse sendLoginCode(String email) {
        String normalized = email != null ? email.trim().toLowerCase() : "";
        String cooldownKey = "login_code_cooldown:" + normalized;
        String cooldown = redisTemplate.opsForValue().get(cooldownKey);
        if (cooldown != null) {
            return LoginCodeResponse.builder().coolDownSeconds(Integer.parseInt(cooldown)).build();
        }
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));
        String codeKey = "login_code:" + normalized;
        redisTemplate.opsForValue().set(codeKey, code, Duration.ofMinutes(5));
        redisTemplate.opsForValue().set(cooldownKey, "60", Duration.ofSeconds(60));
        User user = userRepository.findByEmail(normalized).orElse(null);
        if (user != null) {
            emailService.sendLoginCodeEmail(email, user.getUsername(), code);
        } else {
            emailService.sendLoginCodeEmail(email, "用户", code);
        }
        return LoginCodeResponse.builder().coolDownSeconds(null).build();
    }

    @Transactional
    public TokenResponse loginWithCode(String email, String code) {
        String normalized = email != null ? email.trim().toLowerCase() : "";
        String failKey = "login_code_fail:" + normalized;
        String lock = redisTemplate.opsForValue().get("login_code_lock:" + normalized);
        if (lock != null) {
            throw new BusinessException("验证码错误次数过多，请15分钟后再试");
        }
        String codeKey = "login_code:" + normalized;
        String storedCode = redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null || !storedCode.equals(code)) {
            String fails = redisTemplate.opsForValue().get(failKey);
            int count = fails != null ? Integer.parseInt(fails) : 0;
            count++;
            if (count >= 5) {
                redisTemplate.opsForValue().set("login_code_lock:" + normalized, "1", Duration.ofMinutes(15));
                redisTemplate.delete(failKey);
                throw new BusinessException("验证码错误次数过多，请15分钟后再试");
            }
            redisTemplate.opsForValue().set(failKey, String.valueOf(count), Duration.ofMinutes(15));
            throw new BusinessException("验证码无效或已过期");
        }
        redisTemplate.delete(codeKey);
        redisTemplate.delete(failKey);
        User user = userRepository.findByEmail(normalized).orElse(null);
        if (user == null) {
            user = User.builder()
                    .email(email)
                    .username("user_" + System.currentTimeMillis())
                    .nickname("用户" + System.currentTimeMillis() % 10000)
                    .password("")
                    .isActive(true)
                    .build();
            user = userRepository.save(user);
        }
        if (!user.getIsActive()) {
            throw new BusinessException("账号已被禁用");
        }
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());
        return TokenResponse.builder().access(accessToken).refresh(refreshToken).build();
    }

    public UserSearchResponse searchUsers(String q, Boolean verified, String order, Integer page, Integer size) {
        page = page != null ? page : 1;
        size = size != null ? size : 20;
        PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), size);
        Page<User> userPage = (q != null && !q.trim().isEmpty())
                ? userRepository.searchUsers(q.trim(), pageRequest)
                : userRepository.findActiveUsers(pageRequest);
        java.util.stream.Stream<User> userStream = userPage.getContent().stream();
        if (Boolean.TRUE.equals(verified)) {
            userStream = userStream.filter(User::getIsVerified);
        }
        List<UserProfileResponse> results = userStream
                .map(this::toProfileResponse)
                .collect(java.util.stream.Collectors.toList());
        return UserSearchResponse.builder()
                .results(results)
                .count((int) userPage.getTotalElements())
                .page(page)
                .size(size)
                .build();
    }

    @Transactional
    public UsernameChangeResponse changeUsername(String newUsername) {
        User user = getCurrentUserEntity();
        if (newUsername.equals(user.getUsername())) {
            throw new BusinessException("新用户名与当前相同");
        }
        String cooldownKey = "username_change:" + user.getId();
        String cooldown = redisTemplate.opsForValue().get(cooldownKey);
        if (cooldown != null) {
            throw new BusinessException("改名冷却期内，请 " + cooldown + " 天后再试");
        }
        if (userRepository.findByUsername(newUsername).isPresent()) {
            throw new BusinessException("用户名已被使用");
        }
        user.setUsername(newUsername);
        user.setNickname(newUsername);
        userRepository.save(user);
        redisTemplate.opsForValue().set(cooldownKey, "90", Duration.ofDays(90));
        return UsernameChangeResponse.builder().username(newUsername).coolDownDays(90).build();
    }

    @Transactional
    public void requestEmailChange(String newEmail) {
        User user = getCurrentUserEntity();
        if (userRepository.findByEmail(newEmail).isPresent()) {
            throw new BusinessException("邮箱已被使用");
        }
        String token = java.util.UUID.randomUUID().toString();
        String key = "email_change:" + user.getId();
        redisTemplate.opsForValue().set(key, token + ":" + newEmail, Duration.ofHours(24));
        emailService.sendVerificationEmail(newEmail, user.getUsername(),
                user.getId().toString(), token);
    }

    @Transactional
    public void confirmEmailChange(String token) {
        User user = getCurrentUserEntity();
        String key = "email_change:" + user.getId();
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || !value.startsWith(token + ":")) {
            throw new BusinessException("改绑链接无效或已过期");
        }
        String newEmail = value.substring(token.length() + 1);
        if (userRepository.findByEmail(newEmail).isPresent()) {
            throw new BusinessException("邮箱已被使用");
        }
        user.setEmail(newEmail);
        user.setIsEmailVerified(true);
        userRepository.save(user);
        redisTemplate.delete(key);
    }

    public Map<String, Object> getUserPopupStats(UUID userId, Boolean force) {
        String cacheKey = "user_popup:" + userId;
        if (Boolean.TRUE.equals(force)) {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return Map.of("cached", true);
            }
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("id", user.getId());
        stats.put("username", user.getUsername());
        stats.put("nickname", user.getNickname());
        stats.put("profile_picture", user.getProfilePicture());
        stats.put("followers_count", user.getFollowersCount());
        stats.put("following_count", user.getFollowingCount());
        stats.put("likes_count", user.getTotalLikesReceived());
        stats.put("video_count", user.getVideoCount());
        redisTemplate.opsForValue().set(cacheKey, "cached", Duration.ofMinutes(10));
        return stats;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    private UserProfileResponse toProfileResponse(User user) {
        return toProfileResponse(user, true);
    }

    private boolean isViewingSelf(UUID targetId) {
        try {
            User current = getCurrentUserEntityOrNull();
            return current != null && current.getId().equals(targetId);
        } catch (Exception e) {
            return false;
        }
    }

    private UserProfileResponse toProfileResponse(User user, boolean self) {
        UserProfileResponse response = UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .bio(user.getBio())
                .profilePicture(user.getProfilePicture())
                .profilePictureUrl(user.getProfilePictureF())
                .gender(user.getGender())
                .location(user.getLocation())
                .website(user.getWebsite())
                .isVerified(user.getIsVerified())
                .isCreator(user.getIsCreator())
                .privacyMode(user.getPrivacyMode())
                .followersCount(user.getFollowersCount())
                .followingCount(user.getFollowingCount())
                .videoCount(user.getVideoCount())
                .totalLikesReceived(user.getTotalLikesReceived())
                .totalViewsReceived(user.getTotalViewsReceived())
                .dateJoined(user.getDateJoined())
                .lastActive(user.getLastActive())
                .build();
        if (self) {
            response.setEmail(user.getEmail());
            response.setIsEmailVerified(user.getIsEmailVerified());
            response.setAdminRole(user.getAdminRole());
        }
        return response;
    }

    public void updateProfilePicture(String profilePicture, String profilePictureF) {
        User user = getCurrentUserEntity();
        user.setProfilePicture(profilePicture);
        user.setProfilePictureF(profilePictureF);
        userRepository.save(user);
    }
}
