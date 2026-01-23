-- BS01 功能包 SQL：转码/HLS、搜索（trigram/TSV + 字幕）、播放清单/稍后看、举报与审核、多信息流与推荐基线、WebPush/FCM 推送

BEGIN;

-- 扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS vector;

-- ---------------------------
-- 现有表的约束与索引
-- ---------------------------
-- 视频表（videos_video）约束
ALTER TABLE videos_video
  ADD CONSTRAINT chk_video_status CHECK (status IN ('draft','processing','published','banned')),
  ADD CONSTRAINT chk_upload_status CHECK (upload_status IN ('pending','uploading','completed','failed')),
  ADD CONSTRAINT chk_video_nonnegatives CHECK (duration >= 0 AND width >= 0 AND height >= 0 AND file_size >= 0);

-- 观看历史的唯一性与范围
ALTER TABLE interactions_history
  ADD CONSTRAINT uq_history_user_video UNIQUE (user_id, video_id),
  ADD CONSTRAINT chk_history_progress CHECK (progress >= 0 AND progress <= 1);

-- 关注关系：禁止自关注
ALTER TABLE interactions_follow
  ADD CONSTRAINT chk_not_self_follow CHECK (follower_id <> followed_id);

-- 标签反查索引
CREATE INDEX IF NOT EXISTS idx_video_tags_tag_video ON videos_video_tags(tag_id, video_id);

-- 评论列表排序索引
CREATE INDEX IF NOT EXISTS idx_comment_video_created ON interactions_comment(video_id, created_at DESC);

-- 通知常用查询索引
CREATE INDEX IF NOT EXISTS idx_notification_user_unread ON notifications_notification(recipient_id, is_read, created_at DESC);

-- 已发布视频的快速列表（部分索引）
CREATE INDEX IF NOT EXISTS idx_videos_published ON videos_video (published_at DESC) WHERE status = 'published';

-- 标题/描述的 trigram 索引（模糊匹配）
CREATE INDEX IF NOT EXISTS idx_videos_title_trgm ON videos_video USING gin (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_videos_desc_trgm ON videos_video USING gin (description gin_trgm_ops);

-- 视频标题+描述的全文检索向量列（生成列）
ALTER TABLE videos_video
  ADD COLUMN IF NOT EXISTS search_tsv tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('simple', coalesce(unaccent(title),'')), 'A') ||
    setweight(to_tsvector('simple', coalesce(unaccent(description),'')), 'B')
  ) STORED;
CREATE INDEX IF NOT EXISTS idx_videos_search_tsv ON videos_video USING gin (search_tsv);

-- ---------------------------
-- HLS/转码 与 字幕
-- ---------------------------
CREATE TABLE IF NOT EXISTS videos_transcode (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  video_id UUID NOT NULL REFERENCES videos_video(id) ON DELETE CASCADE,
  profile VARCHAR(50) NOT NULL, -- 例如：1080p、720p、480p、audio
  url TEXT NOT NULL,            -- HLS 播放列表或文件 URL
  status VARCHAR(20) NOT NULL DEFAULT 'pending', -- 状态：pending、processing、ready、failed
  width INTEGER,
  height INTEGER,
  bitrate INTEGER,
  codec VARCHAR(50),
  segment_duration INTEGER DEFAULT 6,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now(),
  UNIQUE(video_id, profile)
);
CREATE INDEX IF NOT EXISTS idx_transcode_video ON videos_transcode(video_id);
CREATE INDEX IF NOT EXISTS idx_transcode_status ON videos_transcode(status);

CREATE TABLE IF NOT EXISTS videos_asset (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  video_id UUID NOT NULL REFERENCES videos_video(id) ON DELETE CASCADE,
  kind VARCHAR(20) NOT NULL, -- 资源类型：thumbnail（缩略图）、sprite（雪碧图）、gif、cover（封面）
  url TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_asset_video ON videos_asset(video_id);

CREATE TABLE IF NOT EXISTS videos_subtitle (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  video_id UUID NOT NULL REFERENCES videos_video(id) ON DELETE CASCADE,
  lang VARCHAR(16) NOT NULL,
  format VARCHAR(16) NOT NULL, -- 格式：vtt、srt
  text_content TEXT,           -- 可选：当存储文本副本时填充
  url TEXT,                    -- 可选：文件 URL
  status VARCHAR(20) NOT NULL DEFAULT 'ready', -- 状态：pending、processing、ready、failed
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now(),
  UNIQUE(video_id, lang, format),
  tsv tsvector GENERATED ALWAYS AS (to_tsvector('simple', coalesce(unaccent(text_content),''))) STORED
);
CREATE INDEX IF NOT EXISTS idx_subtitle_video ON videos_subtitle(video_id);
CREATE INDEX IF NOT EXISTS idx_subtitle_tsv ON videos_subtitle USING gin (tsv);

-- ---------------------------
-- 播放清单 与 稍后看
-- ---------------------------
CREATE TABLE IF NOT EXISTS playlists (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES users_user(id) ON DELETE CASCADE,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  visibility VARCHAR(20) NOT NULL DEFAULT 'public', -- 可见性：public、unlisted、private
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_playlists_user ON playlists(user_id);

CREATE TABLE IF NOT EXISTS playlist_videos (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  playlist_id UUID NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,
  video_id UUID NOT NULL REFERENCES videos_video(id) ON DELETE CASCADE,
  position INTEGER DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT now(),
  UNIQUE(playlist_id, video_id)
);
CREATE INDEX IF NOT EXISTS idx_playlist_videos_playlist_pos ON playlist_videos(playlist_id, position);

CREATE TABLE IF NOT EXISTS watch_later (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES users_user(id) ON DELETE CASCADE,
  video_id UUID NOT NULL REFERENCES videos_video(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ DEFAULT now(),
  UNIQUE(user_id, video_id)
);
CREATE INDEX IF NOT EXISTS idx_watch_later_user ON watch_later(user_id);
CREATE INDEX IF NOT EXISTS idx_watch_later_video ON watch_later(video_id);

-- ---------------------------
-- 举报 与 审核
-- ---------------------------
CREATE TABLE IF NOT EXISTS reports_report (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  reporter_id UUID NOT NULL REFERENCES users_user(id) ON DELETE CASCADE,
  target_type VARCHAR(50) NOT NULL,  -- 目标类型：video、comment、user
  target_id UUID NOT NULL,
  reason_code VARCHAR(50) NOT NULL,  -- 原因：spam、abuse、copyright、other
  description TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'pending', -- 状态：pending、reviewing、resolved、rejected
  handled_by UUID REFERENCES users_user(id) ON DELETE SET NULL,
  handled_at TIMESTAMPTZ,
  moderator_notes TEXT,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_report_target ON reports_report(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_report_status ON reports_report(status);
CREATE INDEX IF NOT EXISTS idx_report_created ON reports_report(created_at DESC);

CREATE TABLE IF NOT EXISTS moderation_action (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  report_id UUID NOT NULL REFERENCES reports_report(id) ON DELETE CASCADE,
  moderator_id UUID NOT NULL REFERENCES users_user(id) ON DELETE CASCADE,
  action VARCHAR(50) NOT NULL, -- 动作：remove_content、ban_user、warn_user、ignore
  reason TEXT,
  created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_moderation_report ON moderation_action(report_id);

CREATE TABLE IF NOT EXISTS audit_log (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  actor_id UUID REFERENCES users_user(id) ON DELETE SET NULL,
  verb VARCHAR(100) NOT NULL,
  target_type VARCHAR(50),
  target_id UUID,
  meta JSONB,
  created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_audit_log_created ON audit_log(created_at DESC);

-- ---------------------------
-- WebPush / FCM 推送投递
-- ---------------------------
CREATE TABLE IF NOT EXISTS webpush_subscription (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES users_user(id) ON DELETE CASCADE,
  endpoint TEXT UNIQUE NOT NULL,
  p256dh TEXT,
  auth TEXT,
  browser VARCHAR(50),
  device VARCHAR(100),
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMPTZ DEFAULT now(),
  last_seen TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_webpush_user ON webpush_subscription(user_id);

CREATE TABLE IF NOT EXISTS fcm_device_token (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES users_user(id) ON DELETE CASCADE,
  token TEXT UNIQUE NOT NULL,
  device_id VARCHAR(100),
  platform VARCHAR(20), -- 平台：android、ios
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMPTZ DEFAULT now(),
  last_seen TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_fcm_user ON fcm_device_token(user_id);

CREATE TABLE IF NOT EXISTS notification_delivery (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  notification_id UUID NOT NULL REFERENCES notifications_notification(id) ON DELETE CASCADE,
  channel VARCHAR(20) NOT NULL, -- 通道：webpush、fcm
  status VARCHAR(20) NOT NULL DEFAULT 'pending', -- 状态：pending、sent、failed
  attempt_count INTEGER NOT NULL DEFAULT 0,
  last_attempt_at TIMESTAMPTZ,
  error TEXT,
  created_at TIMESTAMPTZ DEFAULT now(),
  sent_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_delivery_notification ON notification_delivery(notification_id);
CREATE INDEX IF NOT EXISTS idx_delivery_status ON notification_delivery(status);
CREATE INDEX IF NOT EXISTS idx_delivery_created ON notification_delivery(created_at DESC);

-- ---------------------------
-- 工具触发器：维护 updated_at 字段
-- ---------------------------
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;$$ LANGUAGE plpgsql;

-- 挂载触发器
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='videos_video' AND column_name='updated_at') THEN
    IF NOT EXISTS (
      SELECT 1 FROM pg_trigger WHERE tgname='trg_videos_video_updated_at'
    ) THEN
      CREATE TRIGGER trg_videos_video_updated_at BEFORE UPDATE ON videos_video
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
  END IF;
END$$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='interactions_comment' AND column_name='updated_at') THEN
    IF NOT EXISTS (
      SELECT 1 FROM pg_trigger WHERE tgname='trg_comment_updated_at'
    ) THEN
      CREATE TRIGGER trg_comment_updated_at BEFORE UPDATE ON interactions_comment
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
  END IF;
END$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger WHERE tgname='trg_transcode_updated_at'
  ) THEN
    CREATE TRIGGER trg_transcode_updated_at BEFORE UPDATE ON videos_transcode
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger WHERE tgname='trg_subtitle_updated_at'
  ) THEN
    CREATE TRIGGER trg_subtitle_updated_at BEFORE UPDATE ON videos_subtitle
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger WHERE tgname='trg_playlists_updated_at'
  ) THEN
    CREATE TRIGGER trg_playlists_updated_at BEFORE UPDATE ON playlists
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END$$;

-- ---------------------------
-- 热度评分与信息流
-- ---------------------------
CREATE OR REPLACE FUNCTION compute_hot_score(views BIGINT, likes BIGINT, published_at TIMESTAMPTZ)
RETURNS DOUBLE PRECISION LANGUAGE sql AS $$
  SELECT (
    (COALESCE(likes,0) * 3.0 + COALESCE(views,0) * 0.5)
    / pow(GREATEST(EXTRACT(EPOCH FROM now() - COALESCE(published_at, now())) / 3600.0 + 2.0, 1.0), 1.5)
  );
$$;

-- 关注流（用户专属）
CREATE OR REPLACE FUNCTION rec_feed_following(p_user UUID, p_limit INTEGER DEFAULT 50)
RETURNS SETOF UUID LANGUAGE sql AS $$
  SELECT v.id
  FROM videos_video v
  JOIN interactions_follow f ON f.followed_id = v.user_id
  WHERE f.follower_id = p_user AND v.status = 'published'
  ORDER BY v.published_at DESC NULLS LAST
  LIMIT p_limit;
$$;

-- 热门流（全局）
CREATE OR REPLACE FUNCTION rec_feed_popular(p_limit INTEGER DEFAULT 50)
RETURNS SETOF UUID LANGUAGE sql AS $$
  SELECT v.id
  FROM videos_video v
  WHERE v.status = 'published'
  ORDER BY compute_hot_score(v.view_count, v.like_count, v.published_at) DESC NULLS LAST
  LIMIT p_limit;
$$;

-- 推荐基线：基于观看历史（标签/类目偏好）+ 热度得分
CREATE OR REPLACE FUNCTION rec_baseline(p_user UUID, p_limit INTEGER DEFAULT 50)
RETURNS SETOF UUID LANGUAGE sql AS $$
WITH hist AS (
  SELECT video_id
  FROM interactions_history
  WHERE user_id = p_user
  ORDER BY created_at DESC
  LIMIT 100
),
pref_tags AS (
  SELECT vt.tag_id, COUNT(*) AS cnt
  FROM videos_video_tags vt
  WHERE vt.video_id IN (SELECT video_id FROM hist)
  GROUP BY vt.tag_id
),
pref_cat AS (
  SELECT v.category_id, COUNT(*) AS cnt
  FROM videos_video v
  WHERE v.id IN (SELECT video_id FROM hist) AND v.category_id IS NOT NULL
  GROUP BY v.category_id
),
blocked AS (
  SELECT h.video_id FROM interactions_history h WHERE h.user_id = p_user
),
cand AS (
  SELECT v.id, v.category_id, v.view_count, v.like_count, v.published_at
  FROM videos_video v
  WHERE v.status='published'
    AND v.user_id <> p_user
    AND NOT EXISTS (SELECT 1 FROM blocked b WHERE b.video_id = v.id)
),
tag_score AS (
  SELECT vt.video_id, SUM(pt.cnt) AS score
  FROM videos_video_tags vt
  JOIN pref_tags pt ON pt.tag_id = vt.tag_id
  GROUP BY vt.video_id
),
cat_score AS (
  SELECT c.id AS video_id, pc.cnt AS score
  FROM cand c
  JOIN pref_cat pc ON pc.category_id = c.category_id
),
hot AS (
  SELECT id AS video_id, compute_hot_score(view_count, like_count, published_at) AS score
  FROM cand
),
scores AS (
  SELECT c.id AS video_id,
         COALESCE(ts.score, 0) AS tag_s,
         COALESCE(cs.score, 0) AS cat_s,
         COALESCE(h.score, 0) AS hot_s,
         (COALESCE(ts.score,0) * 1.0 + COALESCE(cs.score,0) * 0.5 + COALESCE(h.score,0) * 1.0) AS total
  FROM cand c
  LEFT JOIN tag_score ts ON ts.video_id = c.id
  LEFT JOIN cat_score cs ON cs.video_id = c.id
  LEFT JOIN hot h ON h.video_id = c.id
)
SELECT video_id
FROM scores
ORDER BY total DESC NULLS LAST
LIMIT p_limit;
$$;

-- ---------------------------
-- 搜索函数（TSV+字幕 以及 trigram 模糊）
-- ---------------------------
CREATE OR REPLACE FUNCTION search_videos_tsv(p_query TEXT, p_limit INTEGER DEFAULT 50)
RETURNS TABLE(video_id UUID, rank DOUBLE PRECISION) LANGUAGE sql AS $$
WITH q AS (
  SELECT plainto_tsquery('simple', unaccent(p_query)) AS tsq
)
SELECT v.id AS video_id,
       ts_rank_cd(
         v.search_tsv || COALESCE(sub.tsv, to_tsvector('simple','')),
         q.tsq
       ) AS rank
FROM videos_video v
CROSS JOIN q
LEFT JOIN (
  SELECT s.video_id, to_tsvector('simple', COALESCE(unaccent(string_agg(s.text_content, ' ')), '')) AS tsv
  FROM videos_subtitle s
  WHERE s.status = 'ready'
  GROUP BY s.video_id
) sub ON sub.video_id = v.id
WHERE (v.search_tsv || COALESCE(sub.tsv, to_tsvector('simple',''))) @@ q.tsq
  AND v.status='published'
ORDER BY rank DESC NULLS LAST
LIMIT p_limit;
$$;

CREATE OR REPLACE FUNCTION search_videos_trgm(p_query TEXT, p_limit INTEGER DEFAULT 50)
RETURNS TABLE(video_id UUID, score DOUBLE PRECISION) LANGUAGE sql AS $$
SELECT v.id AS video_id,
       GREATEST(similarity(v.title, p_query), similarity(COALESCE(v.description,''), p_query)) AS score
FROM videos_video v
WHERE v.status='published'
  AND (v.title ILIKE ('%' || p_query || '%') OR v.description ILIKE ('%' || p_query || '%'))
ORDER BY score DESC NULLS LAST
LIMIT p_limit;
$$;

COMMIT;
-- BS01 功能包 SQL（中文追加）：约束/索引/物化视图/搜索优化/监控

-- 审计日志常用查询索引
CREATE INDEX IF NOT EXISTS idx_audit_log_actor ON audit_log(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_target ON audit_log(target_type, target_id);

-- 举报目标类型检查约束
ALTER TABLE reports_report 
  ADD CONSTRAINT chk_report_target_type 
  CHECK (target_type IN ('video','comment','user'));

-- 播放列表可见性约束
ALTER TABLE playlists 
  ADD CONSTRAINT chk_playlist_visibility 
  CHECK (visibility IN ('public','unlisted','private'));

-- 转码状态与规格配置约束
ALTER TABLE videos_transcode 
  ADD CONSTRAINT chk_transcode_status 
  CHECK (status IN ('pending','processing','ready','failed')),
  ADD CONSTRAINT chk_transcode_profile 
  CHECK (profile IN ('1080p','720p','480p','360p','240p','audio'));

-- 资源类型约束（含 watermark）
ALTER TABLE videos_asset 
  ADD CONSTRAINT chk_asset_kind 
  CHECK (kind IN ('thumbnail','sprite','gif','cover','watermark'));

-- 视频统计物化视图（减少复杂聚合查询）
DROP MATERIALIZED VIEW IF EXISTS mv_video_stats;
CREATE MATERIALIZED VIEW mv_video_stats AS
SELECT 
  v.id as video_id,
  v.view_count,
  v.like_count,
  v.comment_count,
  COUNT(DISTINCT l.user_id) as unique_likes,
  COUNT(DISTINCT c.user_id) as unique_comments,
  AVG(h.progress) as avg_completion_rate
FROM videos_video v
LEFT JOIN interactions_like l ON v.id = l.video_id
LEFT JOIN interactions_comment c ON v.id = c.video_id
LEFT JOIN interactions_history h ON v.id = h.video_id
WHERE v.status = 'published'
GROUP BY v.id;
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_video_stats_video ON mv_video_stats (video_id);

-- 修订热度函数（确保分母安全）
CREATE OR REPLACE FUNCTION compute_hot_score(views BIGINT, likes BIGINT, published_at TIMESTAMPTZ)
RETURNS DOUBLE PRECISION LANGUAGE sql AS $$
  SELECT (
    (COALESCE(likes,0) * 3.0 + COALESCE(views,0) * 0.5)
    / pow(GREATEST(EXTRACT(EPOCH FROM now() - COALESCE(published_at, now())) / 3600.0 + 2.0, 1.0), 1.5)
  );
$$;

-- 优化的 TSV 搜索函数（STABLE，预聚合字幕）
CREATE OR REPLACE FUNCTION search_videos_tsv(p_query TEXT, p_limit INTEGER DEFAULT 50)
RETURNS TABLE(video_id UUID, rank DOUBLE PRECISION) LANGUAGE sql STABLE AS $$
WITH q AS (
  SELECT plainto_tsquery('simple', unaccent(p_query)) AS tsq
),
sub_agg AS (
  SELECT s.video_id, 
         to_tsvector('simple', COALESCE(unaccent(string_agg(s.text_content, ' ')), '')) AS tsv
  FROM videos_subtitle s
  WHERE s.status = 'ready'
  GROUP BY s.video_id
)
SELECT v.id AS video_id,
       ts_rank_cd(v.search_tsv || COALESCE(sa.tsv, to_tsvector('simple','')), q.tsq) AS rank
FROM videos_video v
CROSS JOIN q
LEFT JOIN sub_agg sa ON sa.video_id = v.id
WHERE (v.search_tsv || COALESCE(sa.tsv, to_tsvector('simple',''))) @@ q.tsq
  AND v.status = 'published'
ORDER BY rank DESC
LIMIT p_limit;
$$;

-- 系统监控视图
CREATE OR REPLACE VIEW db_monitor AS
SELECT 
  schemaname,
  relname,
  seq_scan,
  seq_tup_read,
  idx_scan,
  idx_tup_fetch,
  n_tup_ins,
  n_tup_upd,
  n_tup_del
FROM pg_stat_user_tables 
ORDER BY n_tup_ins + n_tup_upd + n_tup_del DESC;

-- 自动清理示例（按需调整）
-- DELETE FROM interactions_history WHERE created_at < now() - interval '6 months';

-- ===== 以下为改进追加 (合并自 bs01_full_features_zh_improvements.sql) =====

-- =================== 改进：物化视图刷新策略 ===================
-- 需要 pg_cron 扩展时可配合使用（下方提供示例）
CREATE OR REPLACE FUNCTION refresh_video_stats()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  REFRESH MATERIALIZED VIEW mv_video_stats;
  RETURN NULL;
END;
$$;

-- 在关键表上添加触发器（示例，可按需扩展到点赞/评论/历史）
DROP TRIGGER IF EXISTS trg_refresh_stats ON videos_video;
CREATE TRIGGER trg_refresh_stats 
  AFTER INSERT OR UPDATE OR DELETE ON videos_video
  FOR EACH STATEMENT EXECUTE FUNCTION refresh_video_stats();

-- =================== 改进：搜索函数性能优化 ===================
CREATE OR REPLACE FUNCTION search_videos_tsv(p_query TEXT, p_limit INTEGER DEFAULT 50)
RETURNS TABLE(video_id UUID, rank DOUBLE PRECISION) LANGUAGE sql STABLE AS $$
WITH q AS (
  SELECT plainto_tsquery('simple', unaccent(p_query)) AS tsq
),
sub_agg AS (
  SELECT s.video_id, 
         to_tsvector('simple', COALESCE(unaccent(string_agg(s.text_content, ' ')), '')) AS tsv
  FROM videos_subtitle s
  WHERE s.status = 'ready'
  GROUP BY s.video_id
)
SELECT v.id AS video_id,
       ts_rank_cd(
         v.search_tsv || COALESCE(sa.tsv, to_tsvector('simple','')),
         q.tsq
       ) AS rank
FROM videos_video v
CROSS JOIN q
LEFT JOIN sub_agg sa ON sa.video_id = v.id
WHERE (v.search_tsv || COALESCE(sa.tsv, to_tsvector('simple',''))) @@ q.tsq
  AND v.status = 'published'
ORDER BY rank DESC
LIMIT p_limit;
$$;

-- =================== 改进：关键索引 ===================
-- 推荐算法需要的索引（仅已发布）
CREATE INDEX IF NOT EXISTS idx_video_published_category 
ON videos_video(published_at DESC, category_id) 
WHERE status = 'published';

-- 播放列表排序索引
CREATE INDEX IF NOT EXISTS idx_playlist_videos_order 
ON playlist_videos(playlist_id, position DESC, created_at DESC);

-- 转码状态查询优化索引（仅处理/排队状态）
CREATE INDEX IF NOT EXISTS idx_transcode_video_status 
ON videos_transcode(video_id, status) 
WHERE status IN ('processing','pending');

-- =================== 性能优化建议（注释） ===================
-- 分区策略示例：超大型表（如观看历史）可按月分区
-- CREATE TABLE interactions_history_2025_01 
-- PARTITION OF interactions_history 
-- FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

-- PostgreSQL 配置建议（postgresql.conf，需 DBA 评估）
-- max_connections = 200
-- shared_buffers = 4GB
-- work_mem = 64MB
-- random_page_cost = 1.1
-- effective_cache_size = 12GB

-- =================== 潜在问题修复 ===================
-- set_updated_at 防递归（通常不需要递归，但加防护更稳妥）
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
  IF TG_OP = 'UPDATE' THEN
    IF NEW.updated_at IS NOT DISTINCT FROM OLD.updated_at THEN
      NEW.updated_at := now();
    END IF;
    RETURN NEW;
  END IF;
  NEW.updated_at := now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 物化视图更新策略（作业函数）
CREATE OR REPLACE FUNCTION refresh_video_stats_job()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  REFRESH MATERIALIZED VIEW mv_video_stats;
END;
$$;

-- 使用 pg_cron 定时任务（需要 CREATE EXTENSION pg_cron，并确保已配置）
-- SELECT cron.schedule('refresh-video-stats', '0 2 * * *', $$SELECT refresh_video_stats_job();$$);

-- =================== 扩展监控视图 ===================
CREATE OR REPLACE VIEW db_performance_monitor AS
SELECT 
  schemaname,
  relname,
  seq_scan,
  seq_tup_read,
  idx_scan,
  idx_tup_fetch,
  n_tup_ins,
  n_tup_upd,
  n_tup_del,
  CASE WHEN seq_scan = 0 THEN 0 
       ELSE (idx_scan::float / NULLIF(seq_scan,0)) * 100 
  END as index_efficiency
FROM pg_stat_user_tables 
ORDER BY n_tup_ins + n_tup_upd + n_tup_del DESC;

-- 自动清理策略（函数示例）
CREATE OR REPLACE FUNCTION cleanup_old_history()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  DELETE FROM interactions_history 
  WHERE created_at < now() - interval '6 months';
END;
$$;
