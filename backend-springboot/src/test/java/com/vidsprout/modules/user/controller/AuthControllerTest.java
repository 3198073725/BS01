package com.vidsprout.modules.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidsprout.common.RateLimiterService;
import com.vidsprout.modules.config.service.SystemConfigService;
import com.vidsprout.modules.user.dto.LoginRequest;
import com.vidsprout.modules.user.dto.RegisterRequest;
import com.vidsprout.modules.user.dto.TokenResponse;
import com.vidsprout.modules.user.dto.UserProfileResponse;
import com.vidsprout.modules.user.service.AuthService;
import com.vidsprout.modules.user.service.EmailService;
import com.vidsprout.security.JwtAuthenticationFilter;
import com.vidsprout.security.JwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private AuthService authService;
    @MockBean private RedisTemplate<String, String> redisTemplate;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private EmailService emailService;
    @MockBean private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        when(rateLimiterService.tryAcquire(anyString(), anyString(), anyInt(), any())).thenReturn(true);
    }

    @Nested
    @DisplayName("登录接口测试")
    class LoginTests {

        @Test
        @DisplayName("POST /api/token/ 应返回200和Token")
        void shouldReturnTokenOnLogin() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password");

            TokenResponse token = TokenResponse.builder().access("access123").refresh("refresh123").build();
            when(authService.login(any(LoginRequest.class))).thenReturn(token);

            mockMvc.perform(post("/api/token/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.data.access").value("access123"))
                    .andExpect(jsonPath("$.data.refresh").value("refresh123"));
        }

        @Test
        @DisplayName("POST /api/token/ 缺少用户名应返回400")
        void shouldReturn400WhenMissingUsername() throws Exception {
            mockMvc.perform(post("/api/token/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"password\":\"test\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("注册接口测试")
    class RegisterTests {

        @Test
        @DisplayName("POST /api/users/register/ 应返回201")
        void shouldReturnCreatedOnRegister() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword("password123");

            UserProfileResponse profile = UserProfileResponse.builder()
                    .id(UUID.randomUUID()).username("newuser").email("new@example.com").build();
            when(authService.register(any(RegisterRequest.class))).thenReturn(profile);

            mockMvc.perform(post("/api/users/register/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.data.username").value("newuser"));
        }

        @Test
        @DisplayName("POST /api/users/register/ 无效邮箱应返回400")
        void shouldReturn400WhenInvalidEmail() throws Exception {
            mockMvc.perform(post("/api/users/register/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"test\",\"email\":\"invalid\",\"password\":\"pass123\"}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
