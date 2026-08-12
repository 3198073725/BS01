package com.vidsprout.modules.config;

import com.vidsprout.modules.config.service.SystemConfigService;
import com.vidsprout.modules.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SystemConfigServiceTest {

    @Autowired
    private SystemConfigService configService;

    @BeforeEach
    void setUpAdminContext() {
        User admin = User.builder()
                .id(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
                .username("admin")
                .email("admin@vidsprout.com")
                .password("x")
                .isActive(true)
                .adminRole("admin")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));
    }

    @Test
    void globalConfigReturnsSchemaDefaults() {
        Map<String, Object> config = configService.getGlobalConfig();

        assertThat(config.get("site_name")).isNull();
        assertThat(config.get("allow_register")).isEqualTo(true);
        assertThat(config.get("maintenance_mode")).isEqualTo(false);
        assertThat(config.get("max_upload_size_mb")).isEqualTo(500);
        assertThat(config.get("SITE_URL")).isEqualTo("http://localhost:8000");
        assertThat(config.get("THROTTLE_REGISTER")).isEqualTo("5/hour");
        assertThat(config.get("ZHIPU_MODERATION_ENABLED")).isEqualTo(true);
        assertThat(config).containsKey("config_version");
    }

    @Test
    void adminListGroupsByCategory() {
        Map<String, Object> admin = configService.getAdminConfig();

        assertThat(admin).containsKey("features");
        assertThat(admin).containsKey("site");
        assertThat(admin).containsKey("content");
        assertThat(admin).containsKey("auth");
        assertThat(admin).containsKey("throttle");
        assertThat(admin).containsKey("security");
        assertThat(admin).containsKey("media");
        assertThat(admin).containsKey("email");
        assertThat(admin).containsKey("cache");
        assertThat(admin).containsKey("celery");

        @SuppressWarnings("unchecked")
        Map<String, Object> features = (Map<String, Object>) admin.get("features");
        assertThat(features.get("label")).isEqualTo("功能开关");
        @SuppressWarnings("unchecked")
        Map<String, Object> settings = (Map<String, Object>) features.get("settings");
        @SuppressWarnings("unchecked")
        Map<String, Object> allowRegister = (Map<String, Object>) settings.get("allow_register");
        assertThat(allowRegister.get("type")).isEqualTo("bool");
        assertThat(allowRegister.get("value")).isEqualTo(true);
        assertThat(allowRegister.get("_source")).isEqualTo("default");
        assertThat(allowRegister.get("_writable")).isEqualTo(true);
    }

    @Test
    void adminUpdatePersistsAndVersionBumps() {
        Map<String, Object> result = configService.updateAdminConfig(Map.of(
                "allow_register", false,
                "max_upload_size_mb", 200,
                "SITE_URL", "http://example.com"
        ));

        assertThat(result.get("status")).isEqualTo("ok");
        @SuppressWarnings("unchecked")
        java.util.List<String> updated = (java.util.List<String>) result.get("updated");
        assertThat(updated).contains("allow_register", "max_upload_size_mb", "SITE_URL");
        assertThat(result).containsKey("version");

        Map<String, Object> config = configService.getGlobalConfig();
        assertThat(config.get("allow_register")).isEqualTo(false);
        assertThat(config.get("max_upload_size_mb")).isEqualTo(200);
        assertThat(config.get("SITE_URL")).isEqualTo("http://example.com");

        Map<String, Object> admin = configService.getAdminConfig();
        @SuppressWarnings("unchecked")
        Map<String, Object> site = (Map<String, Object>) admin.get("site");
        @SuppressWarnings("unchecked")
        Map<String, Object> siteSettings = (Map<String, Object>) site.get("settings");
        @SuppressWarnings("unchecked")
        Map<String, Object> siteUrl = (Map<String, Object>) siteSettings.get("SITE_URL");
        assertThat(siteUrl.get("_source")).isEqualTo("db");
    }

    @Test
    void unmodifiedKeysKeepDefaults() {
        configService.updateAdminConfig(Map.of("allow_register", false));

        Map<String, Object> config = configService.getGlobalConfig();
        assertThat(config.get("allow_anonymous_view")).isEqualTo(true);
        assertThat(config.get("EMAIL_VERIFY_TOKEN_MAX_AGE")).isEqualTo(86400);
        assertThat(config.get("LANGUAGE_CODE")).isEqualTo("zh-hans");
    }

    @Test
    void configVersionPersists() {
        Map<String, Object> first = configService.updateAdminConfig(Map.of("allow_likes", false));
        int version1 = (Integer) first.get("version");
        Map<String, Object> second = configService.updateAdminConfig(Map.of("allow_likes", true));
        int version2 = (Integer) second.get("version");

        assertThat(version2).isGreaterThanOrEqualTo(version1);

        Map<String, Object> config = configService.getGlobalConfig();
        assertThat(config.get("config_version")).isEqualTo(version2);
    }
}
