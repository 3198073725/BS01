package com.vidsprout.modules.config.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidsprout.modules.config.model.ConfigEntry;
import com.vidsprout.modules.config.model.ConfigKey;
import com.vidsprout.modules.config.model.ConfigNamespace;
import com.vidsprout.modules.config.repository.ConfigEntryRepository;
import com.vidsprout.modules.config.repository.ConfigKeyRepository;
import com.vidsprout.modules.config.repository.ConfigNamespaceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 系统配置服务，完整复刻 Django apps/configs/views.py 的行为：
 * 三级优先级 env > 数据库 > schema 默认值；ADMIN_OVERRIDE_KEYS 数据库优先。
 */
@Service
public class SystemConfigService {

    private final ConfigNamespaceRepository namespaceRepository;
    private final ConfigKeyRepository keyRepository;
    private final ConfigEntryRepository entryRepository;
    private final Environment environment;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.config.cache-ttl-seconds:300}")
    private long cacheTtlSeconds;

    private static final String CACHE_GLOBAL = "config:global";
    private static final String CACHE_ADMIN = "config:admin";

    public SystemConfigService(ConfigNamespaceRepository namespaceRepository,
                               ConfigKeyRepository keyRepository,
                               ConfigEntryRepository entryRepository,
                               Environment environment,
                               ObjectMapper objectMapper,
                               RedisTemplate<String, String> redisTemplate) {
        this.namespaceRepository = namespaceRepository;
        this.keyRepository = keyRepository;
        this.entryRepository = entryRepository;
        this.environment = environment;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    public record SettingMeta(String type, String label, Object defaultValue, String help, List<String> options) {
    }

    public record CategoryMeta(String label, Map<String, SettingMeta> settings) {
    }

    private static final Set<String> ADMIN_OVERRIDE_KEYS = Set.of("SITE_URL", "FRONTEND_URL");

    private static final Set<String> RELOAD_REQUIRED_KEYS = Set.of(
            "SITE_URL", "FRONTEND_URL", "LANGUAGE_CODE", "TIME_ZONE",
            "EMAIL_BACKEND", "DEFAULT_FROM_EMAIL", "EMAIL_HOST", "EMAIL_PORT",
            "EMAIL_USE_TLS", "EMAIL_USE_SSL", "EMAIL_TIMEOUT",
            "REDIS_URL", "REDIS_HOST", "REDIS_PORT", "REDIS_DB", "REDIS_MAX_CONNECTIONS",
            "CACHE_KEY_PREFIX", "CACHE_DEFAULT_TIMEOUT", "USE_REDIS_CACHE",
            "CELERY_BROKER_URL", "CELERY_RESULT_BACKEND", "CELERY_TASK_ALWAYS_EAGER",
            "CELERY_TASK_TIME_LIMIT", "CELERY_TASK_SOFT_TIME_LIMIT");

    private static final String COMMENT_BLOCKED_KEYWORDS_DEFAULT = String.join(",",
            "操你妈", "草你妈", "艹你妈", "草泥马", "曹尼玛", "槽尼玛", "槽你妈", "草尼玛",
            "cao ni ma", "caonima", "cnm", "cao nm", "傻逼", "煞笔", "妈的", "他妈的", "你妈的");

    private static final String COMMENT_CANONICAL_RULES_DEFAULT = String.join("\n",
            "艹=操", "草你妈=操你妈", "草泥马=操你妈", "草尼玛=操你妈", "曹尼玛=操你妈",
            "曹你妈=操你妈", "槽尼玛=操你妈", "槽你妈=操你妈", "日你妈=操你妈",
            "caonima=操你妈", "cao n i ma=操你妈", "caonm=操你妈", "caonimaa=操你妈", "cnm=操你妈",
            "傻b=傻逼", "傻比=傻逼", "煞b=傻逼");

    private static final String COMMENT_PATTERN_RULES_DEFAULT = "s[\\s._-]*b=sb";

    private static final Map<String, CategoryMeta> SCHEMA = buildSchema();

    private static SettingMeta s(String type, String label, Object def, String help) {
        return new SettingMeta(type, label, def, help, List.of());
    }

    private static SettingMeta sSelect(String label, Object def, String help, String... options) {
        return new SettingMeta("select", label, def, help, List.of(options));
    }

    private static Map<String, CategoryMeta> buildSchema() {
        Map<String, CategoryMeta> schema = new LinkedHashMap<>();

        Map<String, SettingMeta> features = new LinkedHashMap<>();
        features.put("show_api_base", s("bool", "显示API地址入口", true, "控制各端是否显示API基址切换按钮"));
        features.put("allow_register", s("bool", "允许新用户注册", true, "关闭后禁止新用户注册"));
        features.put("allow_anonymous_view", s("bool", "允许游客观看视频", true, "关闭后必须登录才能观看"));
        features.put("maintenance_mode", s("bool", "全站维护模式", false, "开启后全站显示维护页面"));
        features.put("allow_comments", s("bool", "启用全局评论", true, "关闭后禁用所有评论功能"));
        features.put("allow_likes", s("bool", "启用全局点赞", true, "关闭后禁用所有点赞功能"));
        features.put("video_auto_publish", s("bool", "视频转码后自动发布", true, "关闭后需手动审核发布"));
        features.put("allow_download", s("bool", "允许用户下载视频", false, "开启后用户可下载视频"));
        schema.put("features", new CategoryMeta("功能开关", features));

        Map<String, SettingMeta> site = new LinkedHashMap<>();
        site.put("SITE_URL", s("string", "站点URL", "http://localhost:8000", "后端API地址"));
        site.put("FRONTEND_URL", s("string", "前端URL", "", "前端页面地址"));
        site.put("DEBUG", s("bool", "调试模式", false, "开启后显示详细错误信息"));
        site.put("LANGUAGE_CODE", sSelect("语言", "zh-hans", "站点默认语言", "zh-hans", "en"));
        site.put("TIME_ZONE", s("string", "时区", "Asia/Shanghai", "如 Asia/Shanghai"));
        schema.put("site", new CategoryMeta("站点设置", site));

        Map<String, SettingMeta> content = new LinkedHashMap<>();
        content.put("home_layout", sSelect("首页布局", "grid", "grid=宫格, waterfall=瀑布流, single=单列大图", "grid", "waterfall", "single"));
        content.put("recommend_algorithm", sSelect("推荐算法", "latest", "latest=最新优先, hot=最热优先, random=随机散播", "latest", "hot", "random"));
        content.put("max_upload_size_mb", s("int", "最大上传限制(MB)", 500, "视频文件最大允许上传大小"));
        content.put("featured_video_ids", s("string", "热门推荐视频ID列表", "", "每行一个视频ID，按优先级排序"));
        content.put("featured_limit", s("int", "热门推荐显示数量", 10, "热门推荐区最多显示的视频数量（1-20）"));
        content.put("AUTO_MODERATION_ENABLED", s("bool", "启用自动质控", true, "命中文本规则时自动拦截评论并阻止视频进入正常发布流"));
        content.put("COMMENT_AUTOMOD_ENABLED", s("bool", "评论自动质控", true, "评论发布前执行敏感词拦截"));
        content.put("COMMENT_BLOCKED_KEYWORDS", s("string", "评论敏感词", COMMENT_BLOCKED_KEYWORDS_DEFAULT, "逗号或换行分隔，命中后拒绝发表评论；留空时使用系统内置兜底词表"));
        content.put("COMMENT_CANONICAL_RULES", s("string", "评论归一化规则", COMMENT_CANONICAL_RULES_DEFAULT, "每行一条，格式为 变体=标准词，例如 草拟吗=操你妈；用于同音字、空格符号绕过归一化"));
        content.put("COMMENT_PATTERN_RULES", s("string", "评论正则规则", COMMENT_PATTERN_RULES_DEFAULT, "每行一条，格式为 正则=标签，例如 n[1i]m[a4]=你妈；用于缩写、字母数字混写等高级匹配"));
        content.put("VIDEO_AUTOMOD_ENABLED", s("bool", "视频自动质控", true, "视频标题/描述/文件名发布前执行敏感词拦截"));
        content.put("VIDEO_BLOCKED_KEYWORDS", s("string", "视频敏感词", "", "逗号或换行分隔，命中后视频保持草稿并记录审计"));
        content.put("AUTOMOD_REJECT_MESSAGE", s("string", "自动质控提示语", "内容未通过自动质控，请修改后重试", "评论或视频命中规则时返回给用户的提示"));
        content.put("ZHIPU_MODERATION_ENABLED", s("bool", "启用智谱AI质控", true, "开启后优先调用智谱AI内容安全 API 做真实质控"));
        content.put("ZHIPU_API_KEY", s("string", "智谱AI API Key", "", "服务端审核使用，建议通过 .env 配置"));
        content.put("ZHIPU_BASE_URL", s("string", "智谱AI Base URL", "https://open.bigmodel.cn/api/paas/v4", "兼容代理或网关时可覆盖"));
        content.put("ZHIPU_MODERATION_MODEL", s("string", "智谱AI 审核模型", "moderation", "智谱官方内容安全模型"));
        content.put("ZHIPU_MODERATION_TIMEOUT_SECONDS", s("int", "智谱AI 审核超时(秒)", 15, "评论和视频文本审核调用超时"));
        content.put("ZHIPU_MODERATION_FAIL_CLOSED", s("bool", "审核故障时拒绝", false, "开启后审核服务故障会直接拦截内容"));
        content.put("ZHIPU_MODERATION_BLOCKED_CATEGORIES", s("string", "拦截分类", "porn,abuse,violence,contraband,politics,crime", "逗号分隔，命中这些风险类型则拦截"));
        content.put("MODERATION_MEDIA_PUBLIC_BASE_URL", s("string", "媒体审核公网地址", "", "智谱审核图片/视频时需要可公网访问的媒体地址基址，留空时回退到 SITE_URL"));
        schema.put("content", new CategoryMeta("内容策略", content));

        Map<String, SettingMeta> auth = new LinkedHashMap<>();
        auth.put("REGISTRATION_REQUIRE_CAPTCHA", s("bool", "注册需验证码", false, "开启后注册需要验证码"));
        auth.put("EMAIL_VERIFY_TOKEN_MAX_AGE", s("int", "邮箱验证有效期(秒)", 86400, "默认24小时"));
        auth.put("PASSWORD_RESET_TOKEN_MAX_AGE", s("int", "密码重置有效期(秒)", 3600, "默认1小时"));
        auth.put("EMAIL_CHANGE_TOKEN_MAX_AGE", s("int", "邮箱改绑确认有效期(秒)", 86400, "默认24小时"));
        auth.put("EMAIL_CHECK_MX", s("bool", "检查邮箱MX记录", false, "验证邮箱域名是否有效"));
        auth.put("AVATAR_MAX_SIZE_BYTES", s("int", "头像最大大小(字节)", 2097152, "默认2MB"));
        auth.put("AVATAR_MAX_PIXELS", s("int", "头像最大像素", 25000000, "默认25MP"));
        auth.put("VIDEO_MAX_SIZE_BYTES", s("int", "视频最大大小(字节)", 524288000, "默认500MB"));
        auth.put("CHUNK_SIZE_BYTES", s("int", "分片上传大小(字节)", 5242880, "默认5MB"));
        auth.put("REFRESH_TOKEN_LIFETIME_DAYS", s("int", "刷新令牌有效期(天)", 60, "JWT刷新令牌过期时间"));
        schema.put("auth", new CategoryMeta("用户认证", auth));

        Map<String, SettingMeta> throttle = new LinkedHashMap<>();
        throttle.put("throttling_enabled", s("bool", "启用限流", true, "关闭后取消所有限流"));
        throttle.put("throttle_anon_rate", s("int", "匿名用户限流(次/小时)", 100, ""));
        throttle.put("throttle_user_rate", s("int", "登录用户限流(次/小时)", 1000, ""));
        throttle.put("throttle_recommendation_rate", s("int", "推荐接口限流(次/小时)", 1800, ""));
        throttle.put("THROTTLE_REGISTER", s("string", "注册限流", "5/hour", "如 5/hour"));
        throttle.put("THROTTLE_LOGIN_PASSWORD", s("string", "密码登录限流", "60/hour", ""));
        throttle.put("THROTTLE_LOGIN_CODE", s("string", "验证码登录限流", "30/hour", ""));
        throttle.put("THROTTLE_VIDEO_UPLOAD", s("string", "视频上传限流", "20/hour", ""));
        schema.put("throttle", new CategoryMeta("限流控制", throttle));

        Map<String, SettingMeta> security = new LinkedHashMap<>();
        security.put("LOGIN_CODE_MIN_INTERVAL_SECONDS", s("int", "验证码最小间隔(秒)", 60, "发送验证码间隔"));
        security.put("LOGIN_CODE_DAILY_LIMIT_EMAIL", s("int", "单邮箱日限", 20, "同一邮箱每天最多"));
        security.put("LOGIN_CODE_DAILY_LIMIT_IP", s("int", "单IP日限", 200, "同一IP每天最多"));
        security.put("LOGIN_CODE_LOGIN_FAIL_WINDOW_SECONDS", s("int", "验证码登录失败统计窗口(秒)", 600, "默认10分钟"));
        security.put("LOGIN_CODE_LOGIN_FAIL_MAX_TRIES_EMAIL", s("int", "验证码登录单邮箱失败上限", 5, "超过后进入冷却"));
        security.put("LOGIN_CODE_LOGIN_FAIL_MAX_TRIES_IP", s("int", "验证码登录单IP失败上限", 50, "超过后进入冷却"));
        security.put("LOGIN_CODE_LOGIN_FAIL_COOLDOWN_SECONDS", s("int", "验证码登录冷却时间(秒)", 300, "默认5分钟"));
        security.put("USERNAME_CHANGE_COOLDOWN_DAYS", s("int", "改名冷却天数", 30, "两次修改用户名之间的最短间隔"));
        security.put("LOGIN_PASSWORD_FAIL_WINDOW_SECONDS", s("int", "密码登录失败统计窗口(秒)", 600, "默认10分钟"));
        security.put("LOGIN_PASSWORD_FAIL_MAX_TRIES_USERNAME", s("int", "用户名错误次数", 5, "密码错误锁定"));
        security.put("LOGIN_PASSWORD_FAIL_MAX_TRIES_IP", s("int", "IP错误次数", 50, ""));
        security.put("LOGIN_PASSWORD_FAIL_COOLDOWN_SECONDS", s("int", "锁定时间(秒)", 300, "默认5分钟"));
        security.put("POPUP_STATS_CACHE_SECONDS", s("int", "个人弹窗统计缓存(秒)", 120, "默认2分钟"));
        schema.put("security", new CategoryMeta("安全风控", security));

        Map<String, SettingMeta> media = new LinkedHashMap<>();
        media.put("THUMBNAIL_MAX_SIZE_BYTES", s("int", "封面最大大小(字节)", 5242880, "默认5MB"));
        media.put("THUMBNAIL_MIN_WIDTH", s("int", "封面最小宽度(像素)", 480, "默认480"));
        media.put("THUMBNAIL_MIN_HEIGHT", s("int", "封面最小高度(像素)", 270, "默认270"));
        media.put("THUMBNAIL_RATIO_TOL", s("string", "封面比例容差", "0.04", "与16:9比例允许的偏差，例如0.04"));
        schema.put("media", new CategoryMeta("媒体限制", media));

        Map<String, SettingMeta> email = new LinkedHashMap<>();
        email.put("EMAIL_BACKEND", sSelect("邮件后端", "django.core.mail.backends.console.EmailBackend", "",
                "django.core.mail.backends.console.EmailBackend", "django.core.mail.backends.smtp.EmailBackend"));
        email.put("DEFAULT_FROM_EMAIL", s("string", "发件人", "no-reply@example.com", ""));
        email.put("EMAIL_HOST", s("string", "SMTP主机", "localhost", ""));
        email.put("EMAIL_PORT", s("int", "SMTP端口", 25, ""));
        email.put("EMAIL_USE_TLS", s("bool", "使用TLS", false, ""));
        email.put("EMAIL_USE_SSL", s("bool", "使用SSL", false, ""));
        email.put("EMAIL_TIMEOUT", s("int", "超时(秒)", 10, ""));
        schema.put("email", new CategoryMeta("邮件服务", email));

        Map<String, SettingMeta> cache = new LinkedHashMap<>();
        cache.put("USE_REDIS_CACHE", s("bool", "使用Redis缓存", false, "关闭使用内存缓存"));
        cache.put("REDIS_URL", s("string", "Redis URL", "", "如 redis://127.0.0.1:6379/0"));
        cache.put("REDIS_HOST", s("string", "Redis主机", "127.0.0.1", ""));
        cache.put("REDIS_PORT", s("int", "Redis端口", 6379, ""));
        cache.put("REDIS_DB", s("int", "Redis数据库", 0, "0-15"));
        cache.put("REDIS_MAX_CONNECTIONS", s("int", "最大连接数", 50, ""));
        cache.put("CACHE_KEY_PREFIX", s("string", "缓存前缀", "bs01", ""));
        cache.put("CACHE_DEFAULT_TIMEOUT", s("int", "缓存超时(秒)", 60, ""));
        schema.put("cache", new CategoryMeta("缓存设置", cache));

        Map<String, SettingMeta> celery = new LinkedHashMap<>();
        celery.put("CELERY_BROKER_URL", s("string", "Broker URL", "", "消息队列地址"));
        celery.put("CELERY_RESULT_BACKEND", s("string", "结果后端", "", ""));
        celery.put("CELERY_TASK_ALWAYS_EAGER", s("bool", "同步执行", false, "开发调试用"));
        celery.put("CELERY_TASK_TIME_LIMIT", s("int", "任务硬超时(秒)", 3600, ""));
        celery.put("CELERY_TASK_SOFT_TIME_LIMIT", s("int", "任务软超时(秒)", 3300, ""));
        schema.put("celery", new CategoryMeta("任务队列", celery));

        return schema;
    }

    private ConfigNamespace getOrCreateNamespace() {
        return namespaceRepository.findByName("system").orElseGet(() -> {
            ConfigNamespace ns = ConfigNamespace.builder()
                    .name("system")
                    .description("System Global Settings")
                    .build();
            return namespaceRepository.save(ns);
        });
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("配置值序列化失败: " + value, e);
        }
    }

    private Object fromJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("配置值反序列化失败: " + json, e);
        }
    }

    private Map<String, Object> collectDbValues(ConfigNamespace ns) {
        List<ConfigKey> keys = keyRepository.findByNamespace(ns);
        if (keys.isEmpty()) {
            return new LinkedHashMap<>();
        }
        List<UUID> keyIds = keys.stream().map(ConfigKey::getId).toList();
        Map<String, Object> dbValues = new LinkedHashMap<>();
        for (ConfigEntry e : entryRepository.findByKeyIdInAndContentTypeIdIsNullAndObjectIdIsNullAndIsActiveTrue(keyIds)) {
            dbValues.put(e.getKey().getKey(), fromJson(e.getValue()));
        }
        return dbValues;
    }

    private Object resolveEnvValue(String key) {
        Object fromProps = environment.getProperty("app.env-config." + key);
        if (fromProps != null) {
            return fromProps;
        }
        return System.getenv(key);
    }

    private boolean isWritable(String key, Object envValue) {
        if (ADMIN_OVERRIDE_KEYS.contains(key)) {
            return true;
        }
        return envValue == null;
    }

    private boolean requiresReload(List<String> changedKeys) {
        if (changedKeys == null || changedKeys.isEmpty()) {
            return false;
        }
        return changedKeys.stream().anyMatch(RELOAD_REQUIRED_KEYS::contains);
    }

    public Map<String, Object> getGlobalConfig() {
        if (cacheTtlSeconds > 0) {
            try {
                String cached = redisTemplate.opsForValue().get(CACHE_GLOBAL);
                if (cached != null) {
                    return objectMapper.readValue(cached, Map.class);
                }
            } catch (Exception ignored) {
            }
        }
        Map<String, Object> config = computeGlobalConfig();
        if (cacheTtlSeconds > 0) {
            try {
                redisTemplate.opsForValue().set(CACHE_GLOBAL, objectMapper.writeValueAsString(config),
                        Duration.ofSeconds(cacheTtlSeconds));
            } catch (Exception ignored) {
            }
        }
        return config;
    }

    private Map<String, Object> computeGlobalConfig() {
        ConfigNamespace ns = getOrCreateNamespace();
        List<ConfigKey> keys = keyRepository.findByNamespace(ns);
        Map<String, Object> keyDefaults = new LinkedHashMap<>();
        for (ConfigKey k : keys) {
            keyDefaults.put(k.getKey(), fromJson(k.getDefaultValue()));
        }
        Map<String, Object> dbValues = collectDbValues(ns);

        Map<String, Object> config = new LinkedHashMap<>();
        for (CategoryMeta category : SCHEMA.values()) {
            for (Map.Entry<String, SettingMeta> en : category.settings().entrySet()) {
                String key = en.getKey();
                SettingMeta meta = en.getValue();
                Object envValue = resolveEnvValue(key);
                Object value;
                if (ADMIN_OVERRIDE_KEYS.contains(key) && dbValues.containsKey(key)) {
                    value = dbValues.get(key);
                } else if (envValue != null) {
                    value = envValue;
                } else if (dbValues.containsKey(key)) {
                    value = dbValues.get(key);
                } else if (keyDefaults.containsKey(key) && keyDefaults.get(key) != null) {
                    value = keyDefaults.get(key);
                } else {
                    value = meta.defaultValue();
                }
                config.put(key, value);
            }
        }
        if (!config.containsKey("config_version")) {
            config.put("config_version", (int) (System.currentTimeMillis() / 1000));
        }
        return config;
    }

    public Map<String, Object> getAdminConfig() {
        if (cacheTtlSeconds > 0) {
            try {
                String cached = redisTemplate.opsForValue().get(CACHE_ADMIN);
                if (cached != null) {
                    return objectMapper.readValue(cached, Map.class);
                }
            } catch (Exception ignored) {
            }
        }
        Map<String, Object> config = computeAdminConfig();
        if (cacheTtlSeconds > 0) {
            try {
                redisTemplate.opsForValue().set(CACHE_ADMIN, objectMapper.writeValueAsString(config),
                        Duration.ofSeconds(cacheTtlSeconds));
            } catch (Exception ignored) {
            }
        }
        return config;
    }

    private Map<String, Object> computeAdminConfig() {
        ConfigNamespace ns = getOrCreateNamespace();
        Map<String, Object> dbValues = collectDbValues(ns);

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, CategoryMeta> catEntry : SCHEMA.entrySet()) {
            String categoryName = catEntry.getKey();
            CategoryMeta category = catEntry.getValue();
            Map<String, Object> categoryObj = new LinkedHashMap<>();
            categoryObj.put("label", category.label());
            Map<String, Object> settings = new LinkedHashMap<>();
            for (Map.Entry<String, SettingMeta> en : category.settings().entrySet()) {
                String key = en.getKey();
                SettingMeta meta = en.getValue();
                Object envValue = resolveEnvValue(key);
                boolean preferDb = ADMIN_OVERRIDE_KEYS.contains(key);
                boolean writable = isWritable(key, envValue);
                Object finalValue;
                String source;
                if (preferDb && dbValues.containsKey(key)) {
                    finalValue = dbValues.get(key);
                    source = "db";
                } else if (envValue != null) {
                    finalValue = envValue;
                    source = "env";
                } else if (dbValues.containsKey(key)) {
                    finalValue = dbValues.get(key);
                    source = "db";
                } else {
                    finalValue = meta.defaultValue();
                    source = "default";
                }

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("type", meta.type());
                item.put("label", meta.label());
                item.put("default", meta.defaultValue());
                item.put("help", meta.help());
                if (!meta.options().isEmpty()) {
                    item.put("options", meta.options());
                }
                item.put("value", finalValue);
                item.put("_source", source);
                item.put("_writable", writable);
                settings.put(key, item);
            }
            categoryObj.put("settings", settings);
            result.put(categoryName, categoryObj);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> updateAdminConfig(Map<String, Object> data) {
        ConfigNamespace ns = getOrCreateNamespace();
        List<String> updatedKeys = new ArrayList<>();
        if (data != null) {
            for (Map.Entry<String, Object> en : data.entrySet()) {
                String k = en.getKey();
                Object v = en.getValue();
                Object envValue = resolveEnvValue(k);
                if (!isWritable(k, envValue)) {
                    continue;
                }
                ConfigKey keyObj = keyRepository.findByNamespaceIdAndKey(ns.getId(), k).orElseGet(() -> {
                    ConfigKey nk = ConfigKey.builder()
                            .namespace(ns)
                            .key(k)
                            .valueType("json")
                            .defaultValue(toJson(v))
                            .build();
                    return keyRepository.save(nk);
                });
                ConfigEntry entry = entryRepository.findByKeyIdAndContentTypeIdAndObjectId(
                        keyObj.getId(), null, null).orElseGet(() -> {
                    ConfigEntry ne = new ConfigEntry();
                    ne.setKey(keyObj);
                    ne.setContentTypeId(null);
                    ne.setObjectId(null);
                    ne.setIsActive(true);
                    return ne;
                });
                entry.setValue(toJson(v));
                entry.setIsActive(true);
                entryRepository.save(entry);
                updatedKeys.add(k);
            }
        }

        int version = (int) (System.currentTimeMillis() / 1000);
        ConfigKey versionKey = keyRepository.findByNamespaceIdAndKey(ns.getId(), "config_version").orElseGet(() -> {
            ConfigKey nk = ConfigKey.builder()
                    .namespace(ns)
                    .key("config_version")
                    .valueType("int")
                    .defaultValue(toJson(version))
                    .build();
            return keyRepository.save(nk);
        });
        ConfigEntry versionEntry = entryRepository.findByKeyIdAndContentTypeIdAndObjectId(
                versionKey.getId(), null, null).orElseGet(() -> {
            ConfigEntry ne = new ConfigEntry();
            ne.setKey(versionKey);
            ne.setContentTypeId(null);
            ne.setObjectId(null);
            ne.setIsActive(true);
            return ne;
        });
        versionEntry.setValue(toJson(version));
        versionEntry.setIsActive(true);
        entryRepository.save(versionEntry);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "ok");
        result.put("updated", updatedKeys);
        result.put("version", version);
        result.put("reload_required", requiresReload(updatedKeys));
        try {
            redisTemplate.delete(CACHE_GLOBAL);
            redisTemplate.delete(CACHE_ADMIN);
        } catch (Exception ignored) {
        }
        return result;
    }
}
