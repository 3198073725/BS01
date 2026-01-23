-- BS01 consolidated SQL generated on 2026-01-18T03:58:32Z
-- Django: 6.0.1
-- Database: 
\n-- =========================
-- App: users
-- =========================\n
-- Migration: users 0001_initial
BEGIN;
--
-- Create model User
--
CREATE TABLE "users_user" ("id" uuid NOT NULL PRIMARY KEY, "username" varchar(150) NOT NULL UNIQUE, "email" varchar(254) NOT NULL UNIQUE, "password" varchar(128) NOT NULL, "profile_picture" varchar(100) NULL, "bio" text NULL, "is_active" boolean NOT NULL, "is_staff" boolean NOT NULL, "date_joined" timestamp with time zone NOT NULL, "last_login" timestamp with time zone NULL);
CREATE INDEX "users_user_username_06e46fe6_like" ON "users_user" ("username" varchar_pattern_ops);
CREATE INDEX "users_user_email_243f6e77_like" ON "users_user" ("email" varchar_pattern_ops);
COMMIT;
\n-- End Migration: users 0001_initial\n
-- Migration: users 0002_user_birth_date_user_followers_count_and_more
BEGIN;
--
-- Add field birth_date to user
--
ALTER TABLE "users_user" ADD COLUMN "birth_date" date NULL;
--
-- Add field followers_count to user
--
ALTER TABLE "users_user" ADD COLUMN "followers_count" integer DEFAULT 0 NOT NULL CHECK ("followers_count" >= 0);
ALTER TABLE "users_user" ALTER COLUMN "followers_count" DROP DEFAULT;
--
-- Add field following_count to user
--
ALTER TABLE "users_user" ADD COLUMN "following_count" integer DEFAULT 0 NOT NULL CHECK ("following_count" >= 0);
ALTER TABLE "users_user" ALTER COLUMN "following_count" DROP DEFAULT;
--
-- Add field gender to user
--
ALTER TABLE "users_user" ADD COLUMN "gender" varchar(10) NULL;
--
-- Add field is_creator to user
--
ALTER TABLE "users_user" ADD COLUMN "is_creator" boolean DEFAULT false NOT NULL;
ALTER TABLE "users_user" ALTER COLUMN "is_creator" DROP DEFAULT;
--
-- Add field is_verified to user
--
ALTER TABLE "users_user" ADD COLUMN "is_verified" boolean DEFAULT false NOT NULL;
ALTER TABLE "users_user" ALTER COLUMN "is_verified" DROP DEFAULT;
--
-- Add field last_active to user
--
ALTER TABLE "users_user" ADD COLUMN "last_active" timestamp with time zone NULL;
--
-- Add field location to user
--
ALTER TABLE "users_user" ADD COLUMN "location" varchar(100) NULL;
--
-- Add field nickname to user
--
ALTER TABLE "users_user" ADD COLUMN "nickname" varchar(64) NULL;
--
-- Add field phone_number to user
--
ALTER TABLE "users_user" ADD COLUMN "phone_number" varchar(20) NULL UNIQUE;
--
-- Add field privacy_mode to user
--
ALTER TABLE "users_user" ADD COLUMN "privacy_mode" varchar(20) NULL;
--
-- Add field total_likes_received to user
--
ALTER TABLE "users_user" ADD COLUMN "total_likes_received" integer DEFAULT 0 NOT NULL CHECK ("total_likes_received" >= 0);
ALTER TABLE "users_user" ALTER COLUMN "total_likes_received" DROP DEFAULT;
--
-- Add field total_views_received to user
--
ALTER TABLE "users_user" ADD COLUMN "total_views_received" bigint DEFAULT 0 NOT NULL CHECK ("total_views_received" >= 0);
ALTER TABLE "users_user" ALTER COLUMN "total_views_received" DROP DEFAULT;
--
-- Add field updated_at to user
--
ALTER TABLE "users_user" ADD COLUMN "updated_at" timestamp with time zone DEFAULT '2026-01-18T03:58:34.007698+00:00'::timestamptz NOT NULL;
ALTER TABLE "users_user" ALTER COLUMN "updated_at" DROP DEFAULT;
--
-- Add field video_count to user
--
ALTER TABLE "users_user" ADD COLUMN "video_count" integer DEFAULT 0 NOT NULL CHECK ("video_count" >= 0);
ALTER TABLE "users_user" ALTER COLUMN "video_count" DROP DEFAULT;
--
-- Add field website to user
--
ALTER TABLE "users_user" ADD COLUMN "website" varchar(200) NULL;
--
-- Create index idx_users_username on field(s) username of model user
--
CREATE INDEX "idx_users_username" ON "users_user" ("username");
--
-- Create index idx_users_email on field(s) email of model user
--
CREATE INDEX "idx_users_email" ON "users_user" ("email");
--
-- Create index idx_users_joined on field(s) date_joined of model user
--
CREATE INDEX "idx_users_joined" ON "users_user" ("date_joined");
--
-- Create index idx_users_followers_cnt on field(s) followers_count of model user
--
CREATE INDEX "idx_users_followers_cnt" ON "users_user" ("followers_count");
CREATE INDEX "users_user_phone_number_aff54ffd_like" ON "users_user" ("phone_number" varchar_pattern_ops);
COMMIT;
\n-- End Migration: users 0002_user_birth_date_user_followers_count_and_more\n
-- Migration: users 0003_alter_user_options_alter_user_bio_and_more
BEGIN;
--
-- Change Meta options on user
--
-- (no-op)
--
-- Alter field bio on user
--
-- (no-op)
--
-- Alter field birth_date on user
--
-- (no-op)
--
-- Alter field date_joined on user
--
-- (no-op)
--
-- Alter field email on user
--
-- (no-op)
--
-- Alter field followers_count on user
--
-- (no-op)
--
-- Alter field following_count on user
--
-- (no-op)
--
-- Alter field gender on user
--
ALTER TABLE "users_user" ALTER COLUMN "gender" SET DEFAULT 'private';
UPDATE "users_user" SET "gender" = 'private' WHERE "gender" IS NULL; SET CONSTRAINTS ALL IMMEDIATE;
ALTER TABLE "users_user" ALTER COLUMN "gender" SET NOT NULL;
ALTER TABLE "users_user" ALTER COLUMN "gender" DROP DEFAULT;
--
-- Alter field is_active on user
--
-- (no-op)
--
-- Alter field is_creator on user
--
-- (no-op)
--
-- Alter field is_staff on user
--
-- (no-op)
--
-- Alter field is_verified on user
--
-- (no-op)
--
-- Alter field last_active on user
--
-- (no-op)
--
-- Alter field last_login on user
--
-- (no-op)
--
-- Alter field location on user
--
-- (no-op)
--
-- Alter field nickname on user
--
-- (no-op)
--
-- Alter field password on user
--
-- (no-op)
--
-- Alter field phone_number on user
--
-- (no-op)
--
-- Alter field privacy_mode on user
--
ALTER TABLE "users_user" ALTER COLUMN "privacy_mode" SET DEFAULT 'public';
UPDATE "users_user" SET "privacy_mode" = 'public' WHERE "privacy_mode" IS NULL; SET CONSTRAINTS ALL IMMEDIATE;
ALTER TABLE "users_user" ALTER COLUMN "privacy_mode" SET NOT NULL;
ALTER TABLE "users_user" ALTER COLUMN "privacy_mode" DROP DEFAULT;
--
-- Alter field profile_picture on user
--
-- (no-op)
--
-- Alter field total_likes_received on user
--
-- (no-op)
--
-- Alter field total_views_received on user
--
-- (no-op)
--
-- Alter field updated_at on user
--
-- (no-op)
--
-- Alter field username on user
--
-- (no-op)
--
-- Alter field video_count on user
--
-- (no-op)
--
-- Alter field website on user
--
-- (no-op)
--
-- Create model UserStatistic
--
CREATE TABLE "users_user_statistic" ("id" uuid NOT NULL PRIMARY KEY, "date" date NOT NULL, "new_followers" integer NOT NULL CHECK ("new_followers" >= 0), "new_following" integer NOT NULL CHECK ("new_following" >= 0), "likes_received" integer NOT NULL CHECK ("likes_received" >= 0), "comments_received" integer NOT NULL CHECK ("comments_received" >= 0), "shares_received" integer NOT NULL CHECK ("shares_received" >= 0), "videos_uploaded" integer NOT NULL CHECK ("videos_uploaded" >= 0), "total_views" bigint NOT NULL CHECK ("total_views" >= 0), "watch_time" bigint NOT NULL CHECK ("watch_time" >= 0), "login_count" integer NOT NULL CHECK ("login_count" >= 0), "active_days" integer NOT NULL CHECK ("active_days" >= 0), "created_at" timestamp with time zone NOT NULL, "updated_at" timestamp with time zone NOT NULL, "user_id" uuid NOT NULL, CONSTRAINT "unique_user_date_stat" UNIQUE ("user_id", "date"));
ALTER TABLE "users_user_statistic" ADD CONSTRAINT "users_user_statistic_user_id_9d61004a_fk_users_user_id" FOREIGN KEY ("user_id") REFERENCES "users_user" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "users_user_statistic_user_id_9d61004a" ON "users_user_statistic" ("user_id");
CREATE INDEX "users_user__user_id_bfded9_idx" ON "users_user_statistic" ("user_id", "date");
COMMIT;
\n-- End Migration: users 0003_alter_user_options_alter_user_bio_and_more\n
\n-- =========================
-- App: content
-- =========================\n
-- Migration: content 0001_initial
BEGIN;
--
-- Create model Category
--
CREATE TABLE "content_category" ("id" uuid NOT NULL PRIMARY KEY, "name" varchar(100) NOT NULL UNIQUE, "description" text NULL, "created_at" timestamp with time zone NOT NULL);
--
-- Create model Tag
--
CREATE TABLE "content_tag" ("id" uuid NOT NULL PRIMARY KEY, "name" varchar(50) NOT NULL UNIQUE, "created_at" timestamp with time zone NOT NULL);
--
-- Create model Report
--
CREATE TABLE "reports_report" ("id" uuid NOT NULL PRIMARY KEY, "target_type" varchar(50) NOT NULL, "target_id" uuid NOT NULL, "reason_code" varchar(50) NOT NULL, "description" text NULL, "status" varchar(20) NOT NULL, "handled_at" timestamp with time zone NULL, "moderator_notes" text NULL, "created_at" timestamp with time zone NOT NULL, "updated_at" timestamp with time zone NOT NULL, "handled_by" uuid NULL, "reporter_id" uuid NOT NULL);
--
-- Create model ModerationAction
--
CREATE TABLE "moderation_action" ("id" uuid NOT NULL PRIMARY KEY, "action" varchar(50) NOT NULL, "reason" text NULL, "created_at" timestamp with time zone NOT NULL, "moderator_id" uuid NOT NULL, "report_id" uuid NOT NULL);
--
-- Create model AuditLog
--
CREATE TABLE "audit_log" ("id" uuid NOT NULL PRIMARY KEY, "verb" varchar(100) NOT NULL, "target_type" varchar(50) NULL, "target_id" uuid NULL, "meta" jsonb NULL, "created_at" timestamp with time zone NOT NULL, "actor_id" uuid NULL);
--
-- Create index idx_report_target on field(s) target_type, target_id of model report
--
CREATE INDEX "idx_report_target" ON "reports_report" ("target_type", "target_id");
--
-- Create index idx_report_status on field(s) status of model report
--
CREATE INDEX "idx_report_status" ON "reports_report" ("status");
--
-- Create index idx_report_created on field(s) -created_at of model report
--
CREATE INDEX "idx_report_created" ON "reports_report" ("created_at" DESC);
--
-- Create constraint chk_report_target_type on model report
--
ALTER TABLE "reports_report" ADD CONSTRAINT "chk_report_target_type" CHECK ("target_type" IN ('video', 'comment', 'user'));
--
-- Create index idx_moderation_report on field(s) report of model moderationaction
--
CREATE INDEX "idx_moderation_report" ON "moderation_action" ("report_id");
CREATE INDEX "content_category_name_164da46d_like" ON "content_category" ("name" varchar_pattern_ops);
CREATE INDEX "content_tag_name_ad59e89d_like" ON "content_tag" ("name" varchar_pattern_ops);
ALTER TABLE "reports_report" ADD CONSTRAINT "reports_report_handled_by_19913131_fk_users_user_id" FOREIGN KEY ("handled_by") REFERENCES "users_user" ("id") DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "reports_report" ADD CONSTRAINT "reports_report_reporter_id_d54be306_fk_users_user_id" FOREIGN KEY ("reporter_id") REFERENCES "users_user" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "reports_report_handled_by_19913131" ON "reports_report" ("handled_by");
CREATE INDEX "reports_report_reporter_id_d54be306" ON "reports_report" ("reporter_id");
ALTER TABLE "moderation_action" ADD CONSTRAINT "moderation_action_moderator_id_3f24e72f_fk_users_user_id" FOREIGN KEY ("moderator_id") REFERENCES "users_user" ("id") DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "moderation_action" ADD CONSTRAINT "moderation_action_report_id_26a21dbf_fk_reports_report_id" FOREIGN KEY ("report_id") REFERENCES "reports_report" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "moderation_action_moderator_id_3f24e72f" ON "moderation_action" ("moderator_id");
CREATE INDEX "moderation_action_report_id_26a21dbf" ON "moderation_action" ("report_id");
ALTER TABLE "audit_log" ADD CONSTRAINT "audit_log_actor_id_dfa07704_fk_users_user_id" FOREIGN KEY ("actor_id") REFERENCES "users_user" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "audit_log_actor_id_dfa07704" ON "audit_log" ("actor_id");
CREATE INDEX "idx_audit_log_created" ON "audit_log" ("created_at" DESC);
CREATE INDEX "idx_audit_log_actor" ON "audit_log" ("actor_id");
CREATE INDEX "idx_audit_log_target" ON "audit_log" ("target_type", "target_id");
COMMIT;
\n-- End Migration: content 0001_initial\n
-- Migration: content 0002_alter_auditlog_options_alter_category_options_and_more
BEGIN;
--
-- Change Meta options on auditlog
--
-- (no-op)
--
-- Change Meta options on category
--
-- (no-op)
--
-- Change Meta options on moderationaction
--
-- (no-op)
--
-- Change Meta options on report
--
-- (no-op)
--
-- Change Meta options on tag
--
-- (no-op)
--
-- Alter field actor on auditlog
--
-- (no-op)
--
-- Alter field created_at on auditlog
--
-- (no-op)
--
-- Alter field id on auditlog
--
-- (no-op)
--
-- Alter field meta on auditlog
--
-- (no-op)
--
-- Alter field target_id on auditlog
--
-- (no-op)
--
-- Alter field target_type on auditlog
--
-- (no-op)
--
-- Alter field verb on auditlog
--
-- (no-op)
--
-- Alter field created_at on category
--
-- (no-op)
--
-- Alter field description on category
--
-- (no-op)
--
-- Alter field id on category
--
-- (no-op)
--
-- Alter field name on category
--
-- (no-op)
--
-- Alter field action on moderationaction
--
-- (no-op)
--
-- Alter field created_at on moderationaction
--
-- (no-op)
--
-- Alter field id on moderationaction
--
-- (no-op)
--
-- Alter field moderator on moderationaction
--
-- (no-op)
--
-- Alter field reason on moderationaction
--
-- (no-op)
--
-- Alter field report on moderationaction
--
-- (no-op)
--
-- Alter field created_at on report
--
-- (no-op)
--
-- Alter field description on report
--
-- (no-op)
--
-- Alter field handled_at on report
--
-- (no-op)
--
-- Alter field handled_by on report
--
-- (no-op)
--
-- Alter field id on report
--
-- (no-op)
--
-- Alter field moderator_notes on report
--
-- (no-op)
--
-- Alter field reason_code on report
--
-- (no-op)
--
-- Alter field reporter on report
--
-- (no-op)
--
-- Alter field status on report
--
-- (no-op)
--
-- Alter field target_id on report
--
-- (no-op)
--
-- Alter field target_type on report
--
-- (no-op)
--
-- Alter field updated_at on report
--
-- (no-op)
--
-- Alter field created_at on tag
--
-- (no-op)
--
-- Alter field id on tag
--
-- (no-op)
--
-- Alter field name on tag
--
-- (no-op)
COMMIT;
\n-- End Migration: content 0002_alter_auditlog_options_alter_category_options_and_more\n
\n-- =========================
-- App: videos
-- =========================\n
-- Migration: videos 0001_initial
BEGIN;
--
-- Create model Playlist
--
CREATE TABLE "playlists" ("id" uuid NOT NULL PRIMARY KEY, "name" varchar(255) NOT NULL, "description" text NULL, "visibility" varchar(20) NOT NULL, "created_at" timestamp with time zone NOT NULL, "updated_at" timestamp with time zone NOT NULL, "user_id" uuid NOT NULL);
--
-- Create model Video
--
CREATE TABLE "videos_video" ("id" uuid NOT NULL PRIMARY KEY, "title" varchar(200) NOT NULL, "description" text NULL, "video_file" varchar(100) NOT NULL, "thumbnail" varchar(100) NULL, "duration" integer NOT NULL, "width" integer NOT NULL, "height" integer NOT NULL, "file_size" bigint NOT NULL, "status" varchar(20) NOT NULL, "upload_status" varchar(20) NOT NULL, "view_count" bigint NOT NULL, "like_count" bigint NOT NULL, "comment_count" bigint NOT NULL, "created_at" timestamp with time zone NOT NULL, "updated_at" timestamp with time zone NOT NULL, "published_at" timestamp with time zone NULL, "category_id" uuid NULL, "user_id" uuid NOT NULL);
--
-- Create model PlaylistVideo
--
CREATE TABLE "playlist_videos" ("id" uuid NOT NULL PRIMARY KEY, "position" integer NOT NULL, "created_at" timestamp with time zone NOT NULL, "playlist_id" uuid NOT NULL, "video_id" uuid NOT NULL);
--
-- Create model VideoAsset
--
CREATE TABLE "videos_asset" ("id" uuid NOT NULL PRIMARY KEY, "kind" varchar(20) NOT NULL, "url" text NOT NULL, "created_at" timestamp with time zone NOT NULL, "video_id" uuid NOT NULL);
--
-- Create model VideoSubtitle
--
CREATE TABLE "videos_subtitle" ("id" uuid NOT NULL PRIMARY KEY, "lang" varchar(16) NOT NULL, "format" varchar(16) NOT NULL, "text_content" text NULL, "url" text NULL, "status" varchar(20) NOT NULL, "created_at" timestamp with time zone NOT NULL, "updated_at" timestamp with time zone NOT NULL, "video_id" uuid NOT NULL);
--
-- Create model VideoTag
--
CREATE TABLE "videos_video_tags" ("id" uuid NOT NULL PRIMARY KEY, "created_at" timestamp with time zone NOT NULL, "tag_id" uuid NOT NULL, "video_id" uuid NOT NULL);
--
-- Create model VideoTranscode
--
CREATE TABLE "videos_transcode" ("id" uuid NOT NULL PRIMARY KEY, "profile" varchar(50) NOT NULL, "url" text NOT NULL, "status" varchar(20) NOT NULL, "width" integer NULL, "height" integer NULL, "bitrate" integer NULL, "codec" varchar(50) NULL, "segment_duration" integer NOT NULL, "created_at" timestamp with time zone NOT NULL, "updated_at" timestamp with time zone NOT NULL, "video_id" uuid NOT NULL);
--
-- Create model WatchLater
--
CREATE TABLE "watch_later" ("id" uuid NOT NULL PRIMARY KEY, "created_at" timestamp with time zone NOT NULL, "user_id" uuid NOT NULL, "video_id" uuid NOT NULL);
--
-- Create index idx_playlists_user on field(s) user of model playlist
--
CREATE INDEX "idx_playlists_user" ON "playlists" ("user_id");
--
-- Create constraint chk_playlist_visibility on model playlist
--
ALTER TABLE "playlists" ADD CONSTRAINT "chk_playlist_visibility" CHECK ("visibility" IN ('public', 'unlisted', 'private'));
--
-- Raw SQL operation
--
CREATE EXTENSION IF NOT EXISTS pg_trgm;
--
-- Raw SQL operation
--
CREATE EXTENSION IF NOT EXISTS unaccent;
--
-- Create index idx_videos_published on field(s) -published_at of model video
--
CREATE INDEX "idx_videos_published" ON "videos_video" ("published_at" DESC) WHERE "status" = 'published';
--
-- Create index idx_videos_title_trgm on field(s) title of model video
--
CREATE INDEX "idx_videos_title_trgm" ON "videos_video" USING gin ("title" gin_trgm_ops);
--
-- Create index idx_videos_desc_trgm on field(s) description of model video
--
CREATE INDEX "idx_videos_desc_trgm" ON "videos_video" USING gin ("description" gin_trgm_ops);
--
-- Create constraint chk_video_status on model video
--
ALTER TABLE "videos_video" ADD CONSTRAINT "chk_video_status" CHECK ("status" IN ('draft', 'processing', 'published', 'banned'));
--
-- Create constraint chk_upload_status on model video
--
ALTER TABLE "videos_video" ADD CONSTRAINT "chk_upload_status" CHECK ("upload_status" IN ('pending', 'uploading', 'completed', 'failed'));
--
-- Create constraint chk_video_nonnegatives on model video
--
ALTER TABLE "videos_video" ADD CONSTRAINT "chk_video_nonnegatives" CHECK (("duration" >= 0 AND "width" >= 0 AND "height" >= 0 AND "file_size" >= 0));
--
-- Create index idx_plv_pos on field(s) playlist, position of model playlistvideo
--
CREATE INDEX "idx_plv_pos" ON "playlist_videos" ("playlist_id", "position");
--
-- Alter unique_together for playlistvideo (1 constraint(s))
--
ALTER TABLE "playlist_videos" ADD CONSTRAINT "playlist_videos_playlist_id_video_id_4bec3449_uniq" UNIQUE ("playlist_id", "video_id");
--
-- Create index idx_asset_video on field(s) video of model videoasset
--
CREATE INDEX "idx_asset_video" ON "videos_asset" ("video_id");
--
-- Create constraint chk_asset_kind on model videoasset
--
ALTER TABLE "videos_asset" ADD CONSTRAINT "chk_asset_kind" CHECK ("kind" IN ('thumbnail', 'sprite', 'gif', 'cover', 'watermark'));
--
-- Create index idx_subtitle_video on field(s) video of model videosubtitle
--
CREATE INDEX "idx_subtitle_video" ON "videos_subtitle" ("video_id");
--
-- Create constraint chk_subtitle_status on model videosubtitle
--
ALTER TABLE "videos_subtitle" ADD CONSTRAINT "chk_subtitle_status" CHECK ("status" IN ('pending', 'processing', 'ready', 'failed'));
--
-- Alter unique_together for videosubtitle (1 constraint(s))
--
ALTER TABLE "videos_subtitle" ADD CONSTRAINT "videos_subtitle_video_id_lang_format_46ea09dd_uniq" UNIQUE ("video_id", "lang", "format");
--
-- Create index idx_video_tags_tag_video on field(s) tag, video of model videotag
--
CREATE INDEX "idx_video_tags_tag_video" ON "videos_video_tags" ("tag_id", "video_id");
--
-- Alter unique_together for videotag (1 constraint(s))
--
ALTER TABLE "videos_video_tags" ADD CONSTRAINT "videos_video_tags_video_id_tag_id_f8d6ba70_uniq" UNIQUE ("video_id", "tag_id");
--
-- Create index idx_transcode_video on field(s) video of model videotranscode
--
CREATE INDEX "idx_transcode_video" ON "videos_transcode" ("video_id");
--
-- Create index idx_transcode_status on field(s) status of model videotranscode
--
CREATE INDEX "idx_transcode_status" ON "videos_transcode" ("status") WHERE "status" IN ('processing', 'pending');
--
-- Create constraint chk_transcode_status on model videotranscode
--
ALTER TABLE "videos_transcode" ADD CONSTRAINT "chk_transcode_status" CHECK ("status" IN ('pending', 'processing', 'ready', 'failed'));
--
-- Alter unique_together for videotranscode (1 constraint(s))
--
ALTER TABLE "videos_transcode" ADD CONSTRAINT "videos_transcode_video_id_profile_8d4848fb_uniq" UNIQUE ("video_id", "profile");
--
-- Create index idx_watch_later_user on field(s) user of model watchlater
--
CREATE INDEX "idx_watch_later_user" ON "watch_later" ("user_id");
--
-- Create index idx_watch_later_video on field(s) video of model watchlater
--
CREATE INDEX "idx_watch_later_video" ON "watch_later" ("video_id");
--
-- Alter unique_together for watchlater (1 constraint(s))
--
ALTER TABLE "watch_later" ADD CONSTRAINT "watch_later_user_id_video_id_aa022858_uniq" UNIQUE ("user_id", "video_id");
ALTER TABLE "playlists" ADD CONSTRAINT "playlists_user_id_b4325f92_fk_users_user_id" FOREIGN KEY ("user_id") REFERENCES "users_user" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "playlists_user_id_b4325f92" ON "playlists" ("user_id");
ALTER TABLE "videos_video" ADD CONSTRAINT "videos_video_category_id_192e505b_fk_content_category_id" FOREIGN KEY ("category_id") REFERENCES "content_category" ("id") DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "videos_video" ADD CONSTRAINT "videos_video_user_id_5a0149a4_fk_users_user_id" FOREIGN KEY ("user_id") REFERENCES "users_user" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "videos_video_category_id_192e505b" ON "videos_video" ("category_id");
CREATE INDEX "videos_video_user_id_5a0149a4" ON "videos_video" ("user_id");
ALTER TABLE "playlist_videos" ADD CONSTRAINT "playlist_videos_playlist_id_7868d224_fk_playlists_id" FOREIGN KEY ("playlist_id") REFERENCES "playlists" ("id") DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "playlist_videos" ADD CONSTRAINT "playlist_videos_video_id_86826b79_fk_videos_video_id" FOREIGN KEY ("video_id") REFERENCES "videos_video" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "playlist_videos_playlist_id_7868d224" ON "playlist_videos" ("playlist_id");
CREATE INDEX "playlist_videos_video_id_86826b79" ON "playlist_videos" ("video_id");
ALTER TABLE "videos_asset" ADD CONSTRAINT "videos_asset_video_id_fce6283a_fk_videos_video_id" FOREIGN KEY ("video_id") REFERENCES "videos_video" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "videos_asset_video_id_fce6283a" ON "videos_asset" ("video_id");
ALTER TABLE "videos_subtitle" ADD CONSTRAINT "videos_subtitle_video_id_7548f833_fk_videos_video_id" FOREIGN KEY ("video_id") REFERENCES "videos_video" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "videos_subtitle_video_id_7548f833" ON "videos_subtitle" ("video_id");
ALTER TABLE "videos_video_tags" ADD CONSTRAINT "videos_video_tags_tag_id_2673cfc8_fk_content_tag_id" FOREIGN KEY ("tag_id") REFERENCES "content_tag" ("id") DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "videos_video_tags" ADD CONSTRAINT "videos_video_tags_video_id_8220dbb8_fk_videos_video_id" FOREIGN KEY ("video_id") REFERENCES "videos_video" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "videos_video_tags_tag_id_2673cfc8" ON "videos_video_tags" ("tag_id");
CREATE INDEX "videos_video_tags_video_id_8220dbb8" ON "videos_video_tags" ("video_id");
ALTER TABLE "videos_transcode" ADD CONSTRAINT "videos_transcode_video_id_72763ad6_fk_videos_video_id" FOREIGN KEY ("video_id") REFERENCES "videos_video" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "videos_transcode_video_id_72763ad6" ON "videos_transcode" ("video_id");
ALTER TABLE "watch_later" ADD CONSTRAINT "watch_later_user_id_e708bb2e_fk_users_user_id" FOREIGN KEY ("user_id") REFERENCES "users_user" ("id") DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "watch_later" ADD CONSTRAINT "watch_later_video_id_52f2720e_fk_videos_video_id" FOREIGN KEY ("video_id") REFERENCES "videos_video" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "watch_later_user_id_e708bb2e" ON "watch_later" ("user_id");
CREATE INDEX "watch_later_video_id_52f2720e" ON "watch_later" ("video_id");
COMMIT;
\n-- End Migration: videos 0001_initial\n
-- Migration: videos 0002_search_tsv
BEGIN;
--
-- Raw SQL operation
--

-- Ensure unaccent is available (idempotent)
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Add TSV columns
ALTER TABLE videos_video ADD COLUMN IF NOT EXISTS search_tsv tsvector;
ALTER TABLE videos_subtitle ADD COLUMN IF NOT EXISTS tsv tsvector;

-- GIN indexes on TSV columns
CREATE INDEX IF NOT EXISTS idx_videos_search_tsv ON videos_video USING gin (search_tsv);
CREATE INDEX IF NOT EXISTS idx_subtitle_tsv ON videos_subtitle USING gin (tsv);

-- Triggers to maintain TSV columns
CREATE OR REPLACE FUNCTION videos_video_tsv_trigger() RETURNS trigger AS $$
BEGIN
  NEW.search_tsv :=
    setweight(to_tsvector('simple', coalesce(unaccent(NEW.title),'')), 'A') ||
    setweight(to_tsvector('simple', coalesce(unaccent(NEW.description),'')), 'B');
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_videos_video_tsv ON videos_video;
CREATE TRIGGER trg_videos_video_tsv BEFORE INSERT OR UPDATE OF title, description ON videos_video
FOR EACH ROW EXECUTE FUNCTION videos_video_tsv_trigger();

CREATE OR REPLACE FUNCTION videos_subtitle_tsv_trigger() RETURNS trigger AS $$
BEGIN
  NEW.tsv := to_tsvector('simple', coalesce(unaccent(NEW.text_content),''));
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_videos_subtitle_tsv ON videos_subtitle;
CREATE TRIGGER trg_videos_subtitle_tsv BEFORE INSERT OR UPDATE OF text_content ON videos_subtitle
FOR EACH ROW EXECUTE FUNCTION videos_subtitle_tsv_trigger();

-- Backfill existing rows (idempotent with WHERE ... IS NULL)
UPDATE videos_video
SET search_tsv =
  setweight(to_tsvector('simple', coalesce(unaccent(title),'')), 'A') ||
  setweight(to_tsvector('simple', coalesce(unaccent(description),'')), 'B')
WHERE search_tsv IS NULL;

UPDATE videos_subtitle
SET tsv = to_tsvector('simple', coalesce(unaccent(text_content),''))
WHERE tsv IS NULL;

COMMIT;
\n-- End Migration: videos 0002_search_tsv\n
-- Migration: videos 0003_updated_at_trigger
BEGIN;
--
-- Raw SQL operation
--

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_videos_video_updated_at ON videos_video;
CREATE TRIGGER trg_videos_video_updated_at BEFORE UPDATE ON videos_video
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMIT;
\n-- End Migration: videos 0003_updated_at_trigger\n
-- Migration: videos 0004_playlistvideo_chk_plv_position_nonneg_and_more
BEGIN;
--
-- Create constraint chk_plv_position_nonneg on model playlistvideo
--
ALTER TABLE "playlist_videos" ADD CONSTRAINT "chk_plv_position_nonneg" CHECK ("position" >= 0);
--
-- Create constraint chk_transcode_segment_positive on model videotranscode
--
ALTER TABLE "videos_transcode" ADD CONSTRAINT "chk_transcode_segment_positive" CHECK ("segment_duration" > 0);
COMMIT;
\n-- End Migration: videos 0004_playlistvideo_chk_plv_position_nonneg_and_more\n
-- Migration: videos 0005_playlistvideo_idx_plv_pos_created
BEGIN;
--
-- Create index idx_plv_pos_created on field(s) playlist, position, created_at of model playlistvideo
--
CREATE INDEX "idx_plv_pos_created" ON "playlist_videos" ("playlist_id", "position", "created_at");
COMMIT;
\n-- End Migration: videos 0005_playlistvideo_idx_plv_pos_created\n
-- Migration: videos 0006_alter_playlist_options_alter_playlistvideo_options_and_more
BEGIN;
--
-- Change Meta options on playlist
--
-- (no-op)
--
-- Change Meta options on playlistvideo
--
-- (no-op)
--
-- Change Meta options on video
--
-- (no-op)
--
-- Change Meta options on videoasset
--
-- (no-op)
--
-- Change Meta options on videosubtitle
--
-- (no-op)
--
-- Change Meta options on videotag
--
-- (no-op)
--
-- Change Meta options on videotranscode
--
-- (no-op)
--
-- Change Meta options on watchlater
--
-- (no-op)
--
-- Alter field created_at on playlist
--
-- (no-op)
--
-- Alter field description on playlist
--
-- (no-op)
--
-- Alter field name on playlist
--
-- (no-op)
--
-- Alter field updated_at on playlist
--
-- (no-op)
--
-- Alter field user on playlist
--
-- (no-op)
--
-- Alter field visibility on playlist
--
-- (no-op)
--
-- Alter field created_at on playlistvideo
--
-- (no-op)
--
-- Alter field playlist on playlistvideo
--
-- (no-op)
--
-- Alter field position on playlistvideo
--
-- (no-op)
--
-- Alter field video on playlistvideo
--
-- (no-op)
--
-- Alter field category on video
--
-- (no-op)
--
-- Alter field comment_count on video
--
-- (no-op)
--
-- Alter field created_at on video
--
-- (no-op)
--
-- Alter field description on video
--
-- (no-op)
--
-- Alter field duration on video
--
-- (no-op)
--
-- Alter field file_size on video
--
-- (no-op)
--
-- Alter field height on video
--
-- (no-op)
--
-- Alter field like_count on video
--
-- (no-op)
--
-- Alter field published_at on video
--
-- (no-op)
--
-- Alter field status on video
--
-- (no-op)
--
-- Alter field thumbnail on video
--
-- (no-op)
--
-- Alter field title on video
--
-- (no-op)
--
-- Alter field updated_at on video
--
-- (no-op)
--
-- Alter field upload_status on video
--
-- (no-op)
--
-- Alter field user on video
--
-- (no-op)
--
-- Alter field video_file on video
--
-- (no-op)
--
-- Alter field view_count on video
--
-- (no-op)
--
-- Alter field width on video
--
-- (no-op)
--
-- Alter field created_at on videoasset
--
-- (no-op)
--
-- Alter field kind on videoasset
--
-- (no-op)
--
-- Alter field url on videoasset
--
-- (no-op)
--
-- Alter field video on videoasset
--
-- (no-op)
--
-- Alter field created_at on videosubtitle
--
-- (no-op)
--
-- Alter field format on videosubtitle
--
-- (no-op)
--
-- Alter field lang on videosubtitle
--
-- (no-op)
--
-- Alter field status on videosubtitle
--
-- (no-op)
--
-- Alter field text_content on videosubtitle
--
-- (no-op)
--
-- Alter field updated_at on videosubtitle
--
-- (no-op)
--
-- Alter field url on videosubtitle
--
-- (no-op)
--
-- Alter field video on videosubtitle
--
-- (no-op)
--
-- Alter field created_at on videotag
--
-- (no-op)
--
-- Alter field tag on videotag
--
-- (no-op)
--
-- Alter field video on videotag
--
-- (no-op)
--
-- Alter field bitrate on videotranscode
--
-- (no-op)
--
-- Alter field codec on videotranscode
--
-- (no-op)
--
-- Alter field created_at on videotranscode
--
-- (no-op)
--
-- Alter field height on videotranscode
--
-- (no-op)
--
-- Alter field profile on videotranscode
--
-- (no-op)
--
-- Alter field segment_duration on videotranscode
--
-- (no-op)
--
-- Alter field status on videotranscode
--
-- (no-op)
--
-- Alter field updated_at on videotranscode
--
-- (no-op)
--
-- Alter field url on videotranscode
--
-- (no-op)
--
-- Alter field video on videotranscode
--
-- (no-op)
--
-- Alter field width on videotranscode
--
-- (no-op)
--
-- Alter field created_at on watchlater
--
-- (no-op)
--
-- Alter field user on watchlater
--
-- (no-op)
--
-- Alter field video on watchlater
--
-- (no-op)
COMMIT;
\n-- End Migration: videos 0006_alter_playlist_options_alter_playlistvideo_options_and_more\n
\n-- =========================
-- App: interactions
-- =========================\n
-- Migration: interactions 0001_initial
BEGIN;
--
-- Create model Comment
--
CREATE TABLE "interactions_comment" ("id" uuid NOT NULL PRIMARY KEY, "content" text NOT NULL, "created_at" timestamp with time zone NOT NULL, "updated_at" timestamp with time zone NOT NULL, "parent_id" uuid NULL, "user_id" uuid NOT NULL, "video_id" uuid NOT NULL);
--
-- Create model Favorite
--
CREATE TABLE "interactions_favorite" ("id" uuid NOT NULL PRIMARY KEY, "created_at" timestamp with time zone NOT NULL, "user_id" uuid NOT NULL, "video_id" uuid NOT NULL);
--
-- Create model Follow
--
CREATE TABLE "interactions_follow" ("id" uuid NOT NULL PRIMARY KEY, "created_at" timestamp with time zone NOT NULL, "followed_id" uuid NOT NULL, "follower_id" uuid NOT NULL, CONSTRAINT "chk_not_self_follow" CHECK (NOT ("follower_id" = ("followed_id"))));
--
-- Create model History
--
CREATE TABLE "interactions_history" ("id" uuid NOT NULL PRIMARY KEY, "watch_duration" integer NOT NULL, "progress" double precision NOT NULL, "created_at" timestamp with time zone NOT NULL, "user_id" uuid NOT NULL, "video_id" uuid NOT NULL, CONSTRAINT "chk_history_progress" CHECK (("progress" >= 0.0 AND "progress" <= 1.0)));
--
-- Create model Like
--
CREATE TABLE "interactions_like" ("id" uuid NOT NULL PRIMARY KEY, "created_at" timestamp with time zone NOT NULL, "user_id" uuid NOT NULL, "video_id" uuid NOT NULL);
ALTER TABLE "interactions_comment" ADD CONSTRAINT "interactions_comment_parent_id_e2664b28_fk_interacti" FOREIGN KEY ("parent_id") REFERENCES "interactions_comment" ("id") DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "interactions_comment" ADD CONSTRAINT "interactions_comment_user_id_3985ecff_fk_users_user_id" FOREIGN KEY ("user_id") REFERENCES "users_user" ("id") DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "interactions_comment" ADD CONSTRAINT "interactions_comment_video_id_ccb0cd74_fk_videos_video_id" FOREIGN KEY ("video_id") REFERENCES "videos_video" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "interactions_comment_parent_id_e2664b28" ON "interactions_comment" ("parent_id");
CREATE INDEX "interactions_comment_user_id_3985ecff" ON "interactions_comment" ("user_id");
CREATE INDEX "interactions_comment_video_id_ccb0cd74" ON "interactions_comment" ("video_id");
CREATE INDEX "idx_comment_video_created" ON "interactions_comment" ("video_id", "created_at" DESC);
ALTER TABLE "interactions_favorite" ADD CONSTRAINT "interactions_favorite_user_id_video_id_fe319909_uniq" UNIQUE ("user_id", "video_id");
ALTER TABLE "interactions_favorite" ADD CONSTRAINT "interactions_favorite_user_id_d33f3e84_fk_users_user_id" FOREIGN KEY ("user_id") REFERENCES "users_user" ("id") DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "interactions_favorite" ADD CONSTRAINT "interactions_favorite_video_id_467ac423_fk_videos_video_id" FOREIGN KEY ("video_id") REFERENCES "videos_video" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "interactions_favorite_user_id_d33f3e84" ON "interactions_favorite" ("user_id");
CREATE INDEX "interactions_favorite_video_id_467ac423" ON "interactions_favorite" ("video_id");
CREATE INDEX "idx_favorite_user" ON "interactions_favorite" ("user_id");
CREATE INDEX "idx_favorite_video" ON "interactions_favorite" ("video_id");
ALTER TABLE "interactions_follow" ADD CONSTRAINT "interactions_follow_follower_id_followed_id_c3b1d25a_uniq" UNIQUE ("follower_id", "followed_id");
ALTER TABLE "interactions_follow" ADD CONSTRAINT "interactions_follow_followed_id_2e240992_fk_users_user_id" FOREIGN KEY ("followed_id") REFERENCES "users_user" ("id") DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "interactions_follow" ADD CONSTRAINT "interactions_follow_follower_id_66edc7f2_fk_users_user_id" FOREIGN KEY ("follower_id") REFERENCES "users_user" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "interactions_follow_followed_id_2e240992" ON "interactions_follow" ("followed_id");
CREATE INDEX "interactions_follow_follower_id_66edc7f2" ON "interactions_follow" ("follower_id");
CREATE INDEX "idx_follow_follower" ON "interactions_follow" ("follower_id");
CREATE INDEX "idx_follow_followed" ON "interactions_follow" ("followed_id");
ALTER TABLE "interactions_history" ADD CONSTRAINT "interactions_history_user_id_video_id_89676f58_uniq" UNIQUE ("user_id", "video_id");
ALTER TABLE "interactions_history" ADD CONSTRAINT "interactions_history_user_id_c688d5a0_fk_users_user_id" FOREIGN KEY ("user_id") REFERENCES "users_user" ("id") DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "interactions_history" ADD CONSTRAINT "interactions_history_video_id_912537c6_fk_videos_video_id" FOREIGN KEY ("video_id") REFERENCES "videos_video" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "interactions_history_user_id_c688d5a0" ON "interactions_history" ("user_id");
CREATE INDEX "interactions_history_video_id_912537c6" ON "interactions_history" ("video_id");
CREATE INDEX "idx_history_user" ON "interactions_history" ("user_id");
CREATE INDEX "idx_history_video" ON "interactions_history" ("video_id");
ALTER TABLE "interactions_like" ADD CONSTRAINT "interactions_like_user_id_video_id_5d051dec_uniq" UNIQUE ("user_id", "video_id");
ALTER TABLE "interactions_like" ADD CONSTRAINT "interactions_like_user_id_4026ba55_fk_users_user_id" FOREIGN KEY ("user_id") REFERENCES "users_user" ("id") DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "interactions_like" ADD CONSTRAINT "interactions_like_video_id_6e7a754d_fk_videos_video_id" FOREIGN KEY ("video_id") REFERENCES "videos_video" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "interactions_like_user_id_4026ba55" ON "interactions_like" ("user_id");
CREATE INDEX "interactions_like_video_id_6e7a754d" ON "interactions_like" ("video_id");
CREATE INDEX "idx_like_user" ON "interactions_like" ("user_id");
CREATE INDEX "idx_like_video" ON "interactions_like" ("video_id");
COMMIT;
\n-- End Migration: interactions 0001_initial\n
-- Migration: interactions 0002_comment_updated_at_trigger
BEGIN;
--
-- Raw SQL operation
--

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_comment_updated_at ON interactions_comment;
CREATE TRIGGER trg_comment_updated_at BEFORE UPDATE ON interactions_comment
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMIT;
\n-- End Migration: interactions 0002_comment_updated_at_trigger\n
-- Migration: interactions 0003_history_chk_history_watch_duration_nonneg
BEGIN;
--
-- Create constraint chk_history_watch_duration_nonneg on model history
--
ALTER TABLE "interactions_history" ADD CONSTRAINT "chk_history_watch_duration_nonneg" CHECK ("watch_duration" >= 0);
COMMIT;
\n-- End Migration: interactions 0003_history_chk_history_watch_duration_nonneg\n
-- Migration: interactions 0004_comment_idx_comment_user_comment_idx_comment_parent
BEGIN;
--
-- Create index idx_comment_user on field(s) user of model comment
--
CREATE INDEX "idx_comment_user" ON "interactions_comment" ("user_id");
--
-- Create index idx_comment_parent on field(s) parent of model comment
--
CREATE INDEX "idx_comment_parent" ON "interactions_comment" ("parent_id");
COMMIT;
\n-- End Migration: interactions 0004_comment_idx_comment_user_comment_idx_comment_parent\n
-- Migration: interactions 0005_alter_comment_options_alter_favorite_options_and_more
BEGIN;
--
-- Change Meta options on comment
--
-- (no-op)
--
-- Change Meta options on favorite
--
-- (no-op)
--
-- Change Meta options on follow
--
-- (no-op)
--
-- Change Meta options on history
--
-- (no-op)
--
-- Change Meta options on like
--
-- (no-op)
--
-- Alter field content on comment
--
-- (no-op)
--
-- Alter field created_at on comment
--
-- (no-op)
--
-- Alter field id on comment
--
-- (no-op)
--
-- Alter field parent on comment
--
-- (no-op)
--
-- Alter field updated_at on comment
--
-- (no-op)
--
-- Alter field user on comment
--
-- (no-op)
--
-- Alter field video on comment
--
-- (no-op)
--
-- Alter field created_at on favorite
--
-- (no-op)
--
-- Alter field id on favorite
--
-- (no-op)
--
-- Alter field user on favorite
--
-- (no-op)
--
-- Alter field video on favorite
--
-- (no-op)
--
-- Alter field created_at on follow
--
-- (no-op)
--
-- Alter field followed on follow
--
-- (no-op)
--
-- Alter field follower on follow
--
-- (no-op)
--
-- Alter field id on follow
--
-- (no-op)
--
-- Alter field created_at on history
--
-- (no-op)
--
-- Alter field id on history
--
-- (no-op)
--
-- Alter field progress on history
--
-- (no-op)
--
-- Alter field user on history
--
-- (no-op)
--
-- Alter field video on history
--
-- (no-op)
--
-- Alter field watch_duration on history
--
-- (no-op)
--
-- Alter field created_at on like
--
-- (no-op)
--
-- Alter field id on like
--
-- (no-op)
--
-- Alter field user on like
--
-- (no-op)
--
-- Alter field video on like
--
-- (no-op)
COMMIT;
\n-- End Migration: interactions 0005_alter_comment_options_alter_favorite_options_and_more\n
\n-- =========================
-- App: notifications
-- =========================\n
-- Migration: notifications 0001_initial
BEGIN;
--
-- Create model Notification
--
CREATE TABLE "notifications_notification" ("id" uuid NOT NULL PRIMARY KEY, "is_read" boolean NOT NULL, "created_at" timestamp with time zone NOT NULL, "recipient_id" uuid NOT NULL);
--
-- Create model NotificationDelivery
--
CREATE TABLE "notification_delivery" ("id" uuid NOT NULL PRIMARY KEY, "channel" varchar(20) NOT NULL, "status" varchar(20) NOT NULL, "attempt_count" integer NOT NULL, "last_attempt_at" timestamp with time zone NULL, "error" text NULL, "created_at" timestamp with time zone NOT NULL, "sent_at" timestamp with time zone NULL, "notification_id" uuid NOT NULL);
--
-- Create model WebPushSubscription
--
CREATE TABLE "webpush_subscription" ("id" uuid NOT NULL PRIMARY KEY, "endpoint" text NOT NULL UNIQUE, "p256dh" text NULL, "auth" text NULL, "browser" varchar(50) NULL, "device" varchar(100) NULL, "is_active" boolean NOT NULL, "created_at" timestamp with time zone NOT NULL, "last_seen" timestamp with time zone NULL, "user_id" uuid NULL);
--
-- Create model FCMDeviceToken
--
CREATE TABLE "fcm_device_token" ("id" uuid NOT NULL PRIMARY KEY, "token" text NOT NULL UNIQUE, "device_id" varchar(100) NULL, "platform" varchar(20) NULL, "is_active" boolean NOT NULL, "created_at" timestamp with time zone NOT NULL, "last_seen" timestamp with time zone NULL, "user_id" uuid NULL);
--
-- Create index idx_notification_user_unread on field(s) recipient, is_read, -created_at of model notification
--
CREATE INDEX "idx_notification_user_unread" ON "notifications_notification" ("recipient_id", "is_read", "created_at" DESC);
--
-- Create index idx_delivery_notification on field(s) notification of model notificationdelivery
--
CREATE INDEX "idx_delivery_notification" ON "notification_delivery" ("notification_id");
--
-- Create index idx_delivery_status on field(s) status of model notificationdelivery
--
CREATE INDEX "idx_delivery_status" ON "notification_delivery" ("status");
--
-- Create index idx_delivery_created on field(s) -created_at of model notificationdelivery
--
CREATE INDEX "idx_delivery_created" ON "notification_delivery" ("created_at" DESC);
--
-- Create index idx_webpush_user on field(s) user of model webpushsubscription
--
CREATE INDEX "idx_webpush_user" ON "webpush_subscription" ("user_id");
ALTER TABLE "notifications_notification" ADD CONSTRAINT "notifications_notifi_recipient_id_d055f3f0_fk_users_use" FOREIGN KEY ("recipient_id") REFERENCES "users_user" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "notifications_notification_recipient_id_d055f3f0" ON "notifications_notification" ("recipient_id");
ALTER TABLE "notification_delivery" ADD CONSTRAINT "notification_deliver_notification_id_467c8f80_fk_notificat" FOREIGN KEY ("notification_id") REFERENCES "notifications_notification" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "notification_delivery_notification_id_467c8f80" ON "notification_delivery" ("notification_id");
ALTER TABLE "webpush_subscription" ADD CONSTRAINT "webpush_subscription_user_id_1f81a120_fk_users_user_id" FOREIGN KEY ("user_id") REFERENCES "users_user" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "webpush_subscription_endpoint_926ff2b5_like" ON "webpush_subscription" ("endpoint" text_pattern_ops);
CREATE INDEX "webpush_subscription_user_id_1f81a120" ON "webpush_subscription" ("user_id");
ALTER TABLE "fcm_device_token" ADD CONSTRAINT "fcm_device_token_user_id_22d9b5dd_fk_users_user_id" FOREIGN KEY ("user_id") REFERENCES "users_user" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "fcm_device_token_token_52db6b47_like" ON "fcm_device_token" ("token" text_pattern_ops);
CREATE INDEX "fcm_device_token_user_id_22d9b5dd" ON "fcm_device_token" ("user_id");
CREATE INDEX "idx_fcm_user" ON "fcm_device_token" ("user_id");
COMMIT;
\n-- End Migration: notifications 0001_initial\n
-- Migration: notifications 0002_notificationdelivery_chk_delivery_attempt_nonneg
BEGIN;
--
-- Create constraint chk_delivery_attempt_nonneg on model notificationdelivery
--
ALTER TABLE "notification_delivery" ADD CONSTRAINT "chk_delivery_attempt_nonneg" CHECK ("attempt_count" >= 0);
COMMIT;
\n-- End Migration: notifications 0002_notificationdelivery_chk_delivery_attempt_nonneg\n
-- Migration: notifications 0003_notification_data_notification_notification_type
BEGIN;
--
-- Add field data to notification
--
ALTER TABLE "notifications_notification" ADD COLUMN "data" jsonb DEFAULT '{}'::jsonb NOT NULL;
ALTER TABLE "notifications_notification" ALTER COLUMN "data" DROP DEFAULT;
--
-- Add field notification_type to notification
--
ALTER TABLE "notifications_notification" ADD COLUMN "notification_type" varchar(50) DEFAULT 'generic' NOT NULL;
ALTER TABLE "notifications_notification" ALTER COLUMN "notification_type" DROP DEFAULT;
COMMIT;
\n-- End Migration: notifications 0003_notification_data_notification_notification_type\n
-- Migration: notifications 0004_alter_fcmdevicetoken_options_and_more
BEGIN;
--
-- Change Meta options on fcmdevicetoken
--
-- (no-op)
--
-- Change Meta options on notification
--
-- (no-op)
--
-- Change Meta options on notificationdelivery
--
-- (no-op)
--
-- Change Meta options on webpushsubscription
--
-- (no-op)
--
-- Alter field created_at on fcmdevicetoken
--
-- (no-op)
--
-- Alter field device_id on fcmdevicetoken
--
-- (no-op)
--
-- Alter field is_active on fcmdevicetoken
--
-- (no-op)
--
-- Alter field last_seen on fcmdevicetoken
--
-- (no-op)
--
-- Alter field platform on fcmdevicetoken
--
-- (no-op)
--
-- Alter field token on fcmdevicetoken
--
-- (no-op)
--
-- Alter field user on fcmdevicetoken
--
-- (no-op)
--
-- Alter field created_at on notification
--
-- (no-op)
--
-- Alter field data on notification
--
-- (no-op)
--
-- Alter field is_read on notification
--
-- (no-op)
--
-- Alter field notification_type on notification
--
-- (no-op)
--
-- Alter field recipient on notification
--
-- (no-op)
--
-- Alter field attempt_count on notificationdelivery
--
-- (no-op)
--
-- Alter field channel on notificationdelivery
--
-- (no-op)
--
-- Alter field created_at on notificationdelivery
--
-- (no-op)
--
-- Alter field error on notificationdelivery
--
-- (no-op)
--
-- Alter field last_attempt_at on notificationdelivery
--
-- (no-op)
--
-- Alter field notification on notificationdelivery
--
-- (no-op)
--
-- Alter field sent_at on notificationdelivery
--
-- (no-op)
--
-- Alter field status on notificationdelivery
--
-- (no-op)
--
-- Alter field auth on webpushsubscription
--
-- (no-op)
--
-- Alter field browser on webpushsubscription
--
-- (no-op)
--
-- Alter field created_at on webpushsubscription
--
-- (no-op)
--
-- Alter field device on webpushsubscription
--
-- (no-op)
--
-- Alter field endpoint on webpushsubscription
--
-- (no-op)
--
-- Alter field is_active on webpushsubscription
--
-- (no-op)
--
-- Alter field last_seen on webpushsubscription
--
-- (no-op)
--
-- Alter field p256dh on webpushsubscription
--
-- (no-op)
--
-- Alter field user on webpushsubscription
--
-- (no-op)
COMMIT;
\n-- End Migration: notifications 0004_alter_fcmdevicetoken_options_and_more\n
\n-- =========================
-- App: configs
-- =========================\n
-- Migration: configs 0001_initial
BEGIN;
--
-- Create model ConfigNamespace
--
CREATE TABLE "configs_namespace" ("id" uuid NOT NULL PRIMARY KEY, "name" varchar(64) NOT NULL UNIQUE, "description" varchar(255) NULL, "created_at" timestamp with time zone NOT NULL);
--
-- Create model ConfigKey
--
CREATE TABLE "configs_key" ("id" uuid NOT NULL PRIMARY KEY, "key" varchar(64) NOT NULL, "value_type" varchar(16) NOT NULL, "default_value" jsonb NULL, "description" varchar(255) NULL, "created_at" timestamp with time zone NOT NULL, "updated_at" timestamp with time zone NOT NULL, "namespace_id" uuid NOT NULL);
--
-- Create model ConfigEntry
--
CREATE TABLE "configs_entry" ("id" uuid NOT NULL PRIMARY KEY, "object_id" varchar(64) NULL, "value" jsonb NULL, "is_active" boolean NOT NULL, "created_at" timestamp with time zone NOT NULL, "updated_at" timestamp with time zone NOT NULL, "content_type_id" integer NULL, "key_id" uuid NOT NULL);
--
-- Create index idx_cfg_key_ns_key on field(s) namespace, key of model configkey
--
CREATE INDEX "idx_cfg_key_ns_key" ON "configs_key" ("namespace_id", "key");
--
-- Alter unique_together for configkey (1 constraint(s))
--
ALTER TABLE "configs_key" ADD CONSTRAINT "configs_key_namespace_id_key_935a8ea3_uniq" UNIQUE ("namespace_id", "key");
CREATE INDEX "configs_namespace_name_ef29adf8_like" ON "configs_namespace" ("name" varchar_pattern_ops);
ALTER TABLE "configs_key" ADD CONSTRAINT "configs_key_namespace_id_0750f432_fk_configs_namespace_id" FOREIGN KEY ("namespace_id") REFERENCES "configs_namespace" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "configs_key_namespace_id_0750f432" ON "configs_key" ("namespace_id");
ALTER TABLE "configs_entry" ADD CONSTRAINT "configs_entry_key_id_content_type_id_object_id_74114c18_uniq" UNIQUE ("key_id", "content_type_id", "object_id");
ALTER TABLE "configs_entry" ADD CONSTRAINT "configs_entry_content_type_id_ede3d6c7_fk_django_co" FOREIGN KEY ("content_type_id") REFERENCES "django_content_type" ("id") DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE "configs_entry" ADD CONSTRAINT "configs_entry_key_id_becd391b_fk_configs_key_id" FOREIGN KEY ("key_id") REFERENCES "configs_key" ("id") DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX "configs_entry_content_type_id_ede3d6c7" ON "configs_entry" ("content_type_id");
CREATE INDEX "configs_entry_key_id_becd391b" ON "configs_entry" ("key_id");
CREATE INDEX "idx_cfg_entry_key" ON "configs_entry" ("key_id");
CREATE INDEX "idx_cfg_entry_scope" ON "configs_entry" ("key_id", "content_type_id", "object_id");
CREATE INDEX "idx_cfg_entry_updated" ON "configs_entry" ("updated_at" DESC);
COMMIT;
\n-- End Migration: configs 0001_initial\n
-- Migration: configs 0002_alter_configentry_options_alter_configkey_options_and_more
BEGIN;
--
-- Change Meta options on configentry
--
-- (no-op)
--
-- Change Meta options on configkey
--
-- (no-op)
--
-- Change Meta options on confignamespace
--
-- (no-op)
--
-- Alter field content_type on configentry
--
-- (no-op)
--
-- Alter field created_at on configentry
--
-- (no-op)
--
-- Alter field id on configentry
--
-- (no-op)
--
-- Alter field is_active on configentry
--
-- (no-op)
--
-- Alter field key on configentry
--
-- (no-op)
--
-- Alter field object_id on configentry
--
-- (no-op)
--
-- Alter field updated_at on configentry
--
-- (no-op)
--
-- Alter field value on configentry
--
-- (no-op)
--
-- Alter field created_at on configkey
--
-- (no-op)
--
-- Alter field default_value on configkey
--
-- (no-op)
--
-- Alter field description on configkey
--
-- (no-op)
--
-- Alter field id on configkey
--
-- (no-op)
--
-- Alter field key on configkey
--
-- (no-op)
--
-- Alter field namespace on configkey
--
-- (no-op)
--
-- Alter field updated_at on configkey
--
-- (no-op)
--
-- Alter field value_type on configkey
--
-- (no-op)
--
-- Alter field created_at on confignamespace
--
-- (no-op)
--
-- Alter field description on confignamespace
--
-- (no-op)
--
-- Alter field id on confignamespace
--
-- (no-op)
--
-- Alter field name on confignamespace
--
-- (no-op)
COMMIT;
\n-- End Migration: configs 0002_alter_configentry_options_alter_configkey_options_and_more\n
\n-- =========================
-- App: analytics
-- =========================\n
-- Migration: analytics 0001_mv_video_stats
BEGIN;
--
-- Raw SQL operation
--

-- create materialized view with drop-if-exists safeguard
DROP MATERIALIZED VIEW IF EXISTS mv_video_stats;
CREATE MATERIALIZED VIEW mv_video_stats AS
SELECT 
  v.id as video_id,
  v.view_count,
  v.like_count,
  v.comment_count,
  COUNT(DISTINCT l.user_id) as unique_likes,
  COUNT(DISTINCT c.user_id) as unique_comments,
  COALESCE(AVG(h.progress), 0) as avg_completion_rate
FROM videos_video v
LEFT JOIN interactions_like l ON v.id = l.video_id
LEFT JOIN interactions_comment c ON v.id = c.video_id
LEFT JOIN interactions_history h ON v.id = h.video_id
WHERE v.status = 'published'
GROUP BY v.id;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_video_stats_video ON mv_video_stats (video_id);

-- non-concurrent refresh function (trigger-safe)
CREATE OR REPLACE FUNCTION refresh_video_stats()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  REFRESH MATERIALIZED VIEW mv_video_stats;
  RETURN NULL;
END;
$$;

-- attach trigger on videos table (statement-level)
DROP TRIGGER IF EXISTS trg_refresh_stats ON videos_video;
CREATE TRIGGER trg_refresh_stats 
  AFTER INSERT OR UPDATE OR DELETE ON videos_video
  FOR EACH STATEMENT EXECUTE FUNCTION refresh_video_stats();

COMMIT;
\n-- End Migration: analytics 0001_mv_video_stats\n
-- Migration: analytics 0002_initial
BEGIN;
--
-- Create model VideoStats
--
-- (no-op)
COMMIT;
\n-- End Migration: analytics 0002_initial\n
\n-- EOF
