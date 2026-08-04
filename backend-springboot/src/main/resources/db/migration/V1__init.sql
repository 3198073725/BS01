-- VidSprout (Spring Boot) Database Schema - Flyway V1 baseline
-- Generated from the authoritative Hibernate-managed schema.
-- Managed by Flyway - do not edit by hand; add V2__... for changes.

CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;

CREATE TABLE public.audit_log (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    meta jsonb,
    target_id uuid,
    target_type character varying(50),
    verb character varying(100) NOT NULL,
    actor_id uuid
);

CREATE TABLE public.configs_entry (
    id uuid NOT NULL,
    content_type_id bigint,
    created_at timestamp(6) without time zone NOT NULL,
    is_active boolean,
    object_id character varying(64),
    updated_at timestamp(6) without time zone,
    value jsonb,
    key_id uuid NOT NULL
);

CREATE TABLE public.configs_key (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    default_value jsonb,
    description character varying(255),
    key character varying(64) NOT NULL,
    updated_at timestamp(6) without time zone,
    value_type character varying(16),
    namespace_id uuid NOT NULL
);

CREATE TABLE public.configs_namespace (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    description character varying(255),
    name character varying(64) NOT NULL
);

CREATE TABLE public.content_category (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    description character varying(500),
    is_active boolean,
    name character varying(100) NOT NULL,
    sort_order integer
);

CREATE TABLE public.content_tag (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    description character varying(500),
    is_active boolean,
    name character varying(100) NOT NULL,
    video_count bigint
);

CREATE TABLE public.fcm_device_token (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    device_id character varying(100),
    is_active boolean,
    last_seen timestamp(6) without time zone,
    platform character varying(20),
    token text NOT NULL,
    user_id uuid
);

CREATE TABLE public.interactions_comment (
    id uuid NOT NULL,
    content text NOT NULL,
    created_at timestamp(6) without time zone,
    is_deleted boolean,
    like_count integer,
    reply_count integer,
    updated_at timestamp(6) without time zone,
    parent_id uuid,
    user_id uuid NOT NULL,
    video_id uuid NOT NULL
);

CREATE TABLE public.interactions_favorite (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    user_id uuid NOT NULL,
    video_id uuid NOT NULL
);

CREATE TABLE public.interactions_follow (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    follower_id uuid NOT NULL,
    following_id uuid NOT NULL
);

CREATE TABLE public.interactions_history (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    progress double precision,
    watch_duration integer,
    user_id uuid NOT NULL,
    video_id uuid NOT NULL
);

CREATE TABLE public.interactions_like (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    comment_id uuid,
    user_id uuid NOT NULL,
    video_id uuid
);

CREATE TABLE public.interactions_notification (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    hidden boolean,
    read boolean,
    verb character varying(30) NOT NULL,
    actor_id uuid,
    comment_id uuid,
    user_id uuid NOT NULL,
    video_id uuid
);

CREATE TABLE public.moderation_action (
    id uuid NOT NULL,
    action character varying(50) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    reason text,
    moderator_id uuid NOT NULL,
    report_id uuid NOT NULL
);

CREATE TABLE public.mv_video_stats (
    video_id uuid NOT NULL,
    avg_completion_rate double precision,
    comment_count bigint,
    like_count bigint,
    unique_comments bigint,
    unique_likes bigint,
    view_count bigint
);

CREATE TABLE public.notification_delivery (
    id uuid NOT NULL,
    attempt_count integer,
    channel character varying(20) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    error text,
    last_attempt_at timestamp(6) without time zone,
    sent_at timestamp(6) without time zone,
    status character varying(20) NOT NULL,
    notification_id uuid NOT NULL,
    CONSTRAINT notification_delivery_attempt_count_check CHECK ((attempt_count >= 0))
);

CREATE TABLE public.playlist_videos (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    "position" integer,
    playlist_id uuid NOT NULL,
    video_id uuid NOT NULL,
    CONSTRAINT playlist_videos_position_check CHECK (("position" >= 0))
);

CREATE TABLE public.playlists (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    description text,
    name character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    visibility character varying(20) NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT playlists_visibility_check CHECK (((visibility)::text = ANY ((ARRAY['public'::character varying, 'unlisted'::character varying, 'private'::character varying])::text[])))
);

CREATE TABLE public.reports_report (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    description text,
    handled_at timestamp(6) without time zone,
    handler_note character varying(500),
    reason character varying(100),
    status character varying(20),
    target_id uuid,
    target_type character varying(50),
    handler_id uuid,
    reporter_id uuid NOT NULL
);

CREATE TABLE public.system_announcement (
    id uuid NOT NULL,
    content text NOT NULL,
    created_at timestamp(6) without time zone,
    is_pinned boolean,
    is_published boolean,
    title character varying(200) NOT NULL,
    updated_at timestamp(6) without time zone,
    is_active boolean,
    pinned boolean,
    published_at timestamp(6) without time zone
);

CREATE TABLE public.system_announcement_read (
    id uuid NOT NULL,
    read_at timestamp(6) without time zone NOT NULL,
    announcement_id uuid NOT NULL,
    user_id uuid NOT NULL
);

CREATE TABLE public.users_user (
    id uuid NOT NULL,
    admin_role character varying(20),
    bio text,
    birth_date date,
    date_joined timestamp(6) without time zone NOT NULL,
    email character varying(254) NOT NULL,
    followers_count integer,
    following_count integer,
    gender character varying(10),
    is_active boolean,
    is_creator boolean,
    is_staff boolean,
    is_verified boolean,
    last_active timestamp(6) without time zone,
    location character varying(100),
    nickname character varying(64),
    password character varying(255) NOT NULL,
    phone_number character varying(20),
    privacy_mode character varying(20),
    profile_picture character varying(100),
    profile_picturef character varying(200),
    total_likes_received integer,
    total_views_received bigint,
    updated_at timestamp(6) without time zone,
    username character varying(150) NOT NULL,
    video_count integer,
    website character varying(200),
    is_email_verified boolean
);

CREATE TABLE public.users_user_statistic (
    id uuid NOT NULL,
    active_days integer,
    comments_received integer,
    created_at timestamp(6) without time zone NOT NULL,
    date date NOT NULL,
    likes_received integer,
    login_count integer,
    new_followers integer,
    new_following integer,
    shares_received integer,
    total_views bigint,
    updated_at timestamp(6) without time zone,
    videos_uploaded integer,
    watch_time bigint,
    user_id uuid NOT NULL
);

CREATE TABLE public.videos_asset (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    kind character varying(20) NOT NULL,
    url text NOT NULL,
    video_id uuid NOT NULL,
    CONSTRAINT videos_asset_kind_check CHECK (((kind)::text = ANY ((ARRAY['thumbnail'::character varying, 'sprite'::character varying, 'gif'::character varying, 'cover'::character varying, 'watermark'::character varying])::text[])))
);

CREATE TABLE public.videos_subtitle (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    format character varying(16) NOT NULL,
    lang character varying(16) NOT NULL,
    status character varying(20) NOT NULL,
    text_content text,
    updated_at timestamp(6) without time zone,
    url text,
    video_id uuid NOT NULL,
    CONSTRAINT videos_subtitle_status_check CHECK (((status)::text = ANY ((ARRAY['pending'::character varying, 'processing'::character varying, 'ready'::character varying, 'failed'::character varying])::text[])))
);

CREATE TABLE public.videos_transcode (
    id uuid NOT NULL,
    bitrate integer,
    codec character varying(50),
    created_at timestamp(6) without time zone NOT NULL,
    height integer,
    profile character varying(50) NOT NULL,
    segment_duration integer,
    status character varying(20) NOT NULL,
    updated_at timestamp(6) without time zone,
    url text NOT NULL,
    width integer,
    video_id uuid NOT NULL,
    CONSTRAINT videos_transcode_segment_duration_check CHECK ((segment_duration > 0))
);

CREATE TABLE public.videos_video (
    id uuid NOT NULL,
    allow_comments boolean,
    allow_download boolean,
    comment_count bigint,
    created_at timestamp(6) without time zone,
    description text,
    duration integer,
    file_size bigint,
    height integer,
    is_featured boolean,
    like_count bigint,
    low_mp4 character varying(150),
    published_at timestamp(6) without time zone,
    status character varying(20),
    thumbnail character varying(100),
    thumbnailf character varying(200),
    title character varying(200) NOT NULL,
    transcode_error text,
    updated_at timestamp(6) without time zone,
    upload_status character varying(20),
    video_file character varying(100) NOT NULL,
    video_filef character varying(200),
    view_count bigint,
    visibility character varying(20),
    width integer,
    category_id uuid,
    user_id uuid NOT NULL
);

CREATE TABLE public.videos_video_tags (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    tag_id uuid NOT NULL,
    video_id uuid NOT NULL
);

CREATE TABLE public.watch_later (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    user_id uuid NOT NULL,
    video_id uuid NOT NULL
);

CREATE TABLE public.webpush_subscription (
    id uuid NOT NULL,
    auth text,
    browser character varying(50),
    created_at timestamp(6) without time zone NOT NULL,
    device character varying(100),
    endpoint text NOT NULL,
    is_active boolean,
    last_seen timestamp(6) without time zone,
    p256dh text,
    user_id uuid
);

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.configs_entry
    ADD CONSTRAINT configs_entry_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.configs_key
    ADD CONSTRAINT configs_key_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.configs_namespace
    ADD CONSTRAINT configs_namespace_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.content_category
    ADD CONSTRAINT content_category_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.content_tag
    ADD CONSTRAINT content_tag_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.fcm_device_token
    ADD CONSTRAINT fcm_device_token_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.interactions_comment
    ADD CONSTRAINT interactions_comment_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.interactions_favorite
    ADD CONSTRAINT interactions_favorite_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.interactions_follow
    ADD CONSTRAINT interactions_follow_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.interactions_history
    ADD CONSTRAINT interactions_history_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.interactions_like
    ADD CONSTRAINT interactions_like_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.interactions_notification
    ADD CONSTRAINT interactions_notification_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.moderation_action
    ADD CONSTRAINT moderation_action_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.mv_video_stats
    ADD CONSTRAINT mv_video_stats_pkey PRIMARY KEY (video_id);

ALTER TABLE ONLY public.notification_delivery
    ADD CONSTRAINT notification_delivery_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.playlist_videos
    ADD CONSTRAINT playlist_videos_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.playlists
    ADD CONSTRAINT playlists_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.reports_report
    ADD CONSTRAINT reports_report_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.system_announcement
    ADD CONSTRAINT system_announcement_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.system_announcement_read
    ADD CONSTRAINT system_announcement_read_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.interactions_like
    ADD CONSTRAINT uk24i0tchqepfj1xdr3tpp27t0b UNIQUE (user_id, comment_id);

ALTER TABLE ONLY public.interactions_follow
    ADD CONSTRAINT uk7fqj5mpkl6eh44j377eqkyigg UNIQUE (follower_id, following_id);

ALTER TABLE ONLY public.content_category
    ADD CONSTRAINT uk_cwqg19ayw7xtw2y6f918j3j6n UNIQUE (name);

ALTER TABLE ONLY public.content_tag
    ADD CONSTRAINT uk_dm37xwloevv8cxr99qbm5qxrw UNIQUE (name);

ALTER TABLE ONLY public.users_user
    ADD CONSTRAINT uk_e55b4wr80hwt4mnac6h79ihjo UNIQUE (phone_number);

ALTER TABLE ONLY public.webpush_subscription
    ADD CONSTRAINT uk_ffma5pj8vjn80lwj73cs60066 UNIQUE (endpoint);

ALTER TABLE ONLY public.users_user
    ADD CONSTRAINT uk_gec1hfp2qdcy9kf6ni6ldx9gq UNIQUE (email);

ALTER TABLE ONLY public.fcm_device_token
    ADD CONSTRAINT uk_lnb5a9bngbhyqiow17xgpn8m5 UNIQUE (token);

ALTER TABLE ONLY public.configs_namespace
    ADD CONSTRAINT uk_pcref2gcxq82peghh1j66ye8r UNIQUE (name);

ALTER TABLE ONLY public.users_user
    ADD CONSTRAINT uk_s8h7w7133s6gt8lnyuymg4xyu UNIQUE (username);

ALTER TABLE ONLY public.interactions_like
    ADD CONSTRAINT ukbtf9mtuaenjj8jb8bq9qhy8de UNIQUE (user_id, video_id);

ALTER TABLE ONLY public.interactions_history
    ADD CONSTRAINT ukdhaglp9gxw48igw0hl9uknjrj UNIQUE (user_id, video_id);

ALTER TABLE ONLY public.watch_later
    ADD CONSTRAINT ukh23kbpa391go8v9hfpsjkgf93 UNIQUE (user_id, video_id);

ALTER TABLE ONLY public.interactions_favorite
    ADD CONSTRAINT uksrfvkavhixiaq761fbanwbd9i UNIQUE (user_id, video_id);

ALTER TABLE ONLY public.users_user_statistic
    ADD CONSTRAINT unique_user_date_stat UNIQUE (user_id, date);

ALTER TABLE ONLY public.system_announcement_read
    ADD CONSTRAINT uq_announce_user_read UNIQUE (announcement_id, user_id);

ALTER TABLE ONLY public.configs_entry
    ADD CONSTRAINT uq_cfg_entry_scope UNIQUE (key_id, content_type_id, object_id);

ALTER TABLE ONLY public.configs_key
    ADD CONSTRAINT uq_cfg_key_ns_key UNIQUE (namespace_id, key);

ALTER TABLE ONLY public.playlist_videos
    ADD CONSTRAINT uq_plv_playlist_video UNIQUE (playlist_id, video_id);

ALTER TABLE ONLY public.videos_subtitle
    ADD CONSTRAINT uq_subtitle_video_lang_format UNIQUE (video_id, lang, format);

ALTER TABLE ONLY public.videos_transcode
    ADD CONSTRAINT uq_transcode_video_profile UNIQUE (video_id, profile);

ALTER TABLE ONLY public.users_user
    ADD CONSTRAINT users_user_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.users_user_statistic
    ADD CONSTRAINT users_user_statistic_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.videos_asset
    ADD CONSTRAINT videos_asset_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.videos_subtitle
    ADD CONSTRAINT videos_subtitle_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.videos_transcode
    ADD CONSTRAINT videos_transcode_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.videos_video
    ADD CONSTRAINT videos_video_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.videos_video_tags
    ADD CONSTRAINT videos_video_tags_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.watch_later
    ADD CONSTRAINT watch_later_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.webpush_subscription
    ADD CONSTRAINT webpush_subscription_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.moderation_action
    ADD CONSTRAINT fk1agfgou6nym745m72udnpepvu FOREIGN KEY (report_id) REFERENCES public.reports_report(id);

ALTER TABLE ONLY public.interactions_notification
    ADD CONSTRAINT fk4k46a1hy586bkhutqbkkx0dti FOREIGN KEY (video_id) REFERENCES public.videos_video(id);

ALTER TABLE ONLY public.videos_transcode
    ADD CONSTRAINT fk4klktp2jmdsdnh4xvpx6h7ryu FOREIGN KEY (video_id) REFERENCES public.videos_video(id);

ALTER TABLE ONLY public.webpush_subscription
    ADD CONSTRAINT fk4pvu58yv48n0hi6jj8u0grxt2 FOREIGN KEY (user_id) REFERENCES public.users_user(id);

ALTER TABLE ONLY public.interactions_history
    ADD CONSTRAINT fk4ura24ot8ul328wi2k05fo6af FOREIGN KEY (video_id) REFERENCES public.videos_video(id);

ALTER TABLE ONLY public.configs_key
    ADD CONSTRAINT fk4wba60ymeufhcdc30yy2704dk FOREIGN KEY (namespace_id) REFERENCES public.configs_namespace(id);

ALTER TABLE ONLY public.playlists
    ADD CONSTRAINT fk5fvli5mqj33xenejb1xgc0517 FOREIGN KEY (user_id) REFERENCES public.users_user(id);

ALTER TABLE ONLY public.interactions_notification
    ADD CONSTRAINT fk5u9uya6pduci1ux3gwudlve99 FOREIGN KEY (user_id) REFERENCES public.users_user(id);

ALTER TABLE ONLY public.fcm_device_token
    ADD CONSTRAINT fk5vft7awj279l5okym4jh3rcwy FOREIGN KEY (user_id) REFERENCES public.users_user(id);

ALTER TABLE ONLY public.interactions_notification
    ADD CONSTRAINT fk662c5l4rraodg3yktp109gocs FOREIGN KEY (actor_id) REFERENCES public.users_user(id);

ALTER TABLE ONLY public.videos_subtitle
    ADD CONSTRAINT fk6t6hmtrggr1m2elv3quahed9v FOREIGN KEY (video_id) REFERENCES public.videos_video(id);

ALTER TABLE ONLY public.playlist_videos
    ADD CONSTRAINT fk9ege0hmp4vvplk82e89w6mkws FOREIGN KEY (video_id) REFERENCES public.videos_video(id);

ALTER TABLE ONLY public.playlist_videos
    ADD CONSTRAINT fk9k328sk1nnj1oihl99w8w0nuw FOREIGN KEY (playlist_id) REFERENCES public.playlists(id);

ALTER TABLE ONLY public.interactions_like
    ADD CONSTRAINT fkaaqbbne8lasy876ns056obw9g FOREIGN KEY (user_id) REFERENCES public.users_user(id);

ALTER TABLE ONLY public.videos_asset
    ADD CONSTRAINT fkc700hk72fl2qo8ckhdprov8td FOREIGN KEY (video_id) REFERENCES public.videos_video(id);

ALTER TABLE ONLY public.reports_report
    ADD CONSTRAINT fkcc0rcd1oojt1wbv8vk81a4iv1 FOREIGN KEY (reporter_id) REFERENCES public.users_user(id);

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT fkcqpoxagfxgkwqvaj7n46y1mlq FOREIGN KEY (actor_id) REFERENCES public.users_user(id);

ALTER TABLE ONLY public.videos_video_tags
    ADD CONSTRAINT fkd9y7qbn8ywy1q6ily71abbssx FOREIGN KEY (video_id) REFERENCES public.videos_video(id);

ALTER TABLE ONLY public.interactions_history
    ADD CONSTRAINT fkdxjehgegho2erikampqkys6f8 FOREIGN KEY (user_id) REFERENCES public.users_user(id);

ALTER TABLE ONLY public.interactions_like
    ADD CONSTRAINT fkeodus2t2g3dupschdehk1t1cu FOREIGN KEY (comment_id) REFERENCES public.interactions_comment(id);

ALTER TABLE ONLY public.interactions_like
    ADD CONSTRAINT fkfptlb5v831uoh4sektq1p9mvn FOREIGN KEY (video_id) REFERENCES public.videos_video(id);

ALTER TABLE ONLY public.system_announcement_read
    ADD CONSTRAINT fkfs32h5e1b855ypoghn2mta19q FOREIGN KEY (user_id) REFERENCES public.users_user(id);

ALTER TABLE ONLY public.videos_video
    ADD CONSTRAINT fkghw83mjgsl3h9hpe6xydpo04o FOREIGN KEY (user_id) REFERENCES public.users_user(id);

ALTER TABLE ONLY public.interactions_favorite
    ADD CONSTRAINT fkgvhp2uoqb1w784ubo37pyol7a FOREIGN KEY (user_id) REFERENCES public.users_user(id);

ALTER TABLE ONLY public.watch_later
    ADD CONSTRAINT fkh7ns1y0ag8vta5fl10kaw6nmw FOREIGN KEY (user_id) REFERENCES public.users_user(id);

ALTER TABLE ONLY public.users_user_statistic
    ADD CONSTRAINT fkifvafxy8xta5fowivjgigkf5w FOREIGN KEY (user_id) REFERENCES public.users_user(id);

ALTER TABLE ONLY public.interactions_comment
    ADD CONSTRAINT fkjbv4xw5whqx7h47vxxdu9a90x FOREIGN KEY (video_id) REFERENCES public.videos_video(id);

ALTER TABLE ONLY public.interactions_favorite
    ADD CONSTRAINT fkjq1vg4jlqs9t9b8uua93xadgx FOREIGN KEY (video_id) REFERENCES public.videos_video(id);

ALTER TABLE ONLY public.watch_later
    ADD CONSTRAINT fkjv0ob02e940f2tkfhlthaubdf FOREIGN KEY (video_id) REFERENCES public.videos_video(id);

ALTER TABLE ONLY public.interactions_notification
    ADD CONSTRAINT fkn5nudnyjxhyvbvyi4g0w4002l FOREIGN KEY (comment_id) REFERENCES public.interactions_comment(id);

ALTER TABLE ONLY public.reports_report
    ADD CONSTRAINT fko7mue9d6ks3rcuio5vuma2wmi FOREIGN KEY (handler_id) REFERENCES public.users_user(id);

ALTER TABLE ONLY public.interactions_comment
    ADD CONSTRAINT fkohsitgrml25c96tnav0tasf7l FOREIGN KEY (parent_id) REFERENCES public.interactions_comment(id);

ALTER TABLE ONLY public.moderation_action
    ADD CONSTRAINT fkp0h12kt980es9h420ghfrn7c FOREIGN KEY (moderator_id) REFERENCES public.users_user(id);

ALTER TABLE ONLY public.notification_delivery
    ADD CONSTRAINT fkpyx9v5o9gd9xhjjism15xpjbs FOREIGN KEY (notification_id) REFERENCES public.interactions_notification(id);

ALTER TABLE ONLY public.system_announcement_read
    ADD CONSTRAINT fkqirq2cgwl708c3kpru6p2txtt FOREIGN KEY (announcement_id) REFERENCES public.system_announcement(id);

ALTER TABLE ONLY public.configs_entry
    ADD CONSTRAINT fkqix36c3g4ba8m4rougsc0l0e FOREIGN KEY (key_id) REFERENCES public.configs_key(id);

ALTER TABLE ONLY public.interactions_follow
    ADD CONSTRAINT fkso4ltc9w5qmyeheooket2qobd FOREIGN KEY (follower_id) REFERENCES public.users_user(id);

ALTER TABLE ONLY public.videos_video
    ADD CONSTRAINT fkt3fqlpeuulcx4a5snk3q619v9 FOREIGN KEY (category_id) REFERENCES public.content_category(id);

ALTER TABLE ONLY public.interactions_follow
    ADD CONSTRAINT fkt62xtr7cxedxs75w6g510r1vx FOREIGN KEY (following_id) REFERENCES public.users_user(id);

ALTER TABLE ONLY public.videos_video_tags
    ADD CONSTRAINT fkt792ms4739s8c4i6dhu8pgcfs FOREIGN KEY (tag_id) REFERENCES public.content_tag(id);

ALTER TABLE ONLY public.interactions_comment
    ADD CONSTRAINT fktne04m0chvpap29dpg9ddpe4t FOREIGN KEY (user_id) REFERENCES public.users_user(id);


-- ============================================================
-- Performance indexes (query path acceleration)
-- ============================================================
CREATE INDEX idx_users_username ON users_user(username);
CREATE INDEX idx_users_email ON users_user(email);
CREATE INDEX idx_users_date_joined ON users_user(date_joined);
CREATE INDEX idx_users_username_trgm ON users_user USING GIN (username gin_trgm_ops);
CREATE INDEX idx_users_nickname_trgm ON users_user USING GIN (nickname gin_trgm_ops);

CREATE INDEX idx_videos_published ON videos_video(published_at DESC) WHERE status = 'published';
CREATE INDEX idx_videos_title_trgm ON videos_video USING GIN (title gin_trgm_ops);
CREATE INDEX idx_videos_desc_trgm ON videos_video USING GIN (description gin_trgm_ops);
CREATE INDEX idx_videos_user_id ON videos_video(user_id);
CREATE INDEX idx_videos_category_id ON videos_video(category_id);

CREATE INDEX idx_comments_video_id ON interactions_comment(video_id);
CREATE INDEX idx_comments_parent_id ON interactions_comment(parent_id);

CREATE INDEX idx_video_tags_tag_video ON videos_video_tags(tag_id, video_id);
