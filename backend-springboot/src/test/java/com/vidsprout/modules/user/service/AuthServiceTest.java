package com.vidsprout.modules.user.service;

import com.vidsprout.common.exception.BusinessException;
import com.vidsprout.common.exception.ResourceNotFoundException;
import com.vidsprout.common.exception.UnauthorizedException;
import com.vidsprout.modules.user.dto.LoginRequest;
import com.vidsprout.modules.user.dto.RegisterRequest;
import com.vidsprout.modules.user.dto.TokenResponse;
import com.vidsprout.modules.user.dto.UserProfileResponse;
import com.vidsprout.modules.user.model.User;
import com.vidsprout.modules.user.repository.UserRepository;
import com.vidsprout.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;
    @Mock private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId).username("testuser").email("test@example.com")
                .password("encoded_password").nickname("测试用户")
                .isActive(true).build();

    }

    @Nested
    @DisplayName("注册测试")
    class RegisterTests {

        @Test
        @DisplayName("正常注册应返回用户信息")
        void shouldRegisterSuccessfully() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword("password123");

            when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(UUID.randomUUID());
                u.setNickname("newuser");
                return u;
            });

            UserProfileResponse response = authService.register(request);

            assertNotNull(response);
            assertEquals("newuser", response.getUsername());
            assertEquals("new@example.com", response.getEmail());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("用户名已存在应抛出异常")
        void shouldThrowWhenUsernameExists() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("testuser");
            request.setEmail("new@example.com");
            request.setPassword("password123");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            assertThrows(BusinessException.class, () -> authService.register(request));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("邮箱已注册应抛出异常")
        void shouldThrowWhenEmailExists() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("test@example.com");
            request.setPassword("password123");

            when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            assertThrows(BusinessException.class, () -> authService.register(request));
        }
    }

    @Nested
    @DisplayName("登录测试")
    class LoginTests {

        @Test
        @DisplayName("正确凭据应返回Token")
        void shouldLoginSuccessfully() {
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("correct_password");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("correct_password", "encoded_password")).thenReturn(true);
            when(jwtUtils.generateAccessToken(testUserId, "testuser")).thenReturn("access_token");
            when(jwtUtils.generateRefreshToken(testUserId, "testuser")).thenReturn("refresh_token");

            TokenResponse response = authService.login(request);

            assertNotNull(response);
            assertEquals("access_token", response.getAccess());
            assertEquals("refresh_token", response.getRefresh());
        }

        @Test
        @DisplayName("密码错误应抛出异常")
        void shouldThrowWhenWrongPassword() {
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("wrong_password");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

            assertThrows(UnauthorizedException.class, () -> authService.login(request));
        }

        @Test
        @DisplayName("账号被禁用应抛出异常")
        void shouldThrowWhenAccountDisabled() {
            testUser.setIsActive(false);
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("correct_password");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            assertThrows(UnauthorizedException.class, () -> authService.login(request));
        }

        @Test
        @DisplayName("用户不存在应抛出异常")
        void shouldThrowWhenUserNotFound() {
            LoginRequest request = new LoginRequest();
            request.setUsername("nonexistent");
            request.setPassword("password");

            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThrows(UnauthorizedException.class, () -> authService.login(request));
        }
    }

    @Nested
    @DisplayName("Token刷新测试")
    class RefreshTokenTests {

        @Test
        @DisplayName("有效RefreshToken应返回新Token")
        void shouldRefreshTokenSuccessfully() {
            when(jwtUtils.validateRefreshToken("valid_refresh")).thenReturn(true);
            when(jwtUtils.getUserIdFromToken("valid_refresh")).thenReturn(testUserId);
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(jwtUtils.generateAccessToken(testUserId, "testuser")).thenReturn("new_access");
            when(jwtUtils.generateRefreshToken(testUserId, "testuser")).thenReturn("new_refresh");

            TokenResponse response = authService.refreshToken("valid_refresh");

            assertEquals("new_access", response.getAccess());
            assertEquals("new_refresh", response.getRefresh());
        }

        @Test
        @DisplayName("无效RefreshToken应抛出异常")
        void shouldThrowWhenInvalidRefreshToken() {
            when(jwtUtils.validateRefreshToken("invalid_refresh")).thenReturn(false);

            assertThrows(UnauthorizedException.class, () -> authService.refreshToken("invalid_refresh"));
        }
    }
}
