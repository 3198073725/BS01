-- VidSprout seed data for showcase container (idempotent)

-- Categories
INSERT INTO public.content_category (id, created_at, description, is_active, name, sort_order)
VALUES
  ('11111111-1111-1111-1111-111111111111', NOW(), '编程与技术', true, '技术', 1),
  ('22222222-2222-2222-2222-222222222222', NOW(), '娱乐休闲', true, '娱乐', 2),
  ('33333333-3333-3333-3333-333333333333', NOW(), '学习教育', true, '教育', 3),
  ('44444444-4444-4444-4444-444444444444', NOW(), '音乐分享', true, '音乐', 4)
ON CONFLICT (id) DO NOTHING;

-- Demo users (bcrypt hash of "demo12345")
INSERT INTO public.users_user (
  id, admin_role, bio, birth_date, date_joined, email, followers_count, following_count,
  gender, is_active, is_creator, is_staff, is_verified, last_active, location, nickname,
  password, phone_number, privacy_mode, profile_picture, profile_picturef,
  total_likes_received, total_views_received, updated_at, username, video_count, website,
  is_email_verified
)
VALUES
  ('a82420d1-829a-40d7-ac96-269f9fd74d83', NULL, 'VidSprout 演示账号', NULL, NOW(),
   'demo@vidsprout.com', 128, 56, 'female', true, true, false, true, NOW(), '中国',
   'VidSprout', '$2a$10$tEyKaOrTAc6hKCH/MxdxrO5a9be1z33QlGkwFGnWdnZ9LCLbKZOyW',
   NULL, 'public', NULL, NULL, 3200, 89000, NOW(), 'vidsprout', 4, NULL, true),
  ('b73afbd2-a716-49de-9319-0f74ccbc0d2e', NULL, '旅行摄影师', NULL, NOW(),
   'travel@vidsprout.com', 45, 30, 'male', true, true, false, false, NOW(), '上海',
   '旅行者', '$2a$10$hiFI77K5hOYsAHfHYwcHvOf33etH6usMZFy3dVohHBW6PAFb5/auy',
   NULL, 'public', NULL, NULL, 860, 23000, NOW(), 'traveler', 1, NULL, true)
ON CONFLICT (id) DO NOTHING;

-- Demo videos (files copied into /app/media/videos and /app/media/thumbnails)
INSERT INTO public.videos_video (
  id, allow_comments, allow_download, comment_count, created_at, description, duration,
  file_size, height, is_featured, like_count, low_mp4, published_at, status, thumbnail,
  thumbnailf, title, transcode_error, updated_at, upload_status, video_file, video_filef,
  view_count, visibility, width, category_id, user_id
)
VALUES
  ('aaaaaaaa-0000-0000-0000-000000000001', true, true, 45, NOW(), 'Spring Boot 入门教程演示视频', 12,
   1983446, 720, true, 151, NULL, NOW(), 'published', 'thumbnails/demo1.jpg',
   '/media/thumbnails/demo1.jpg', 'Spring Boot 入门教程', NULL, NOW(), 'completed',
   'videos/demo1.mp4', '/media/videos/demo1.mp4', 1242, 'public', 1280,
   '11111111-1111-1111-1111-111111111111', 'a82420d1-829a-40d7-ac96-269f9fd74d83'),
  ('aaaaaaaa-0000-0000-0000-000000000002', true, true, 30, NOW(), '竖屏演示视频，展示移动端沉浸式播放体验', 12,
   1996345, 1280, true, 100, NULL, NOW(), 'published', 'thumbnails/demo2.jpg',
   '/media/thumbnails/demo2.jpg', '竖屏沉浸式演示', NULL, NOW(), 'completed',
   'videos/demo2.mp4', '/media/videos/demo2.mp4', 843, 'public', 720,
   '22222222-2222-2222-2222-222222222222', 'a82420d1-829a-40d7-ac96-269f9fd74d83'),
  ('aaaaaaaa-0000-0000-0000-000000000003', true, false, 80, NOW(), '科技分享：Tech Talk 主题演示', 12,
   1954795, 720, false, 300, NULL, NOW(), 'published', 'thumbnails/demo3.jpg',
   '/media/thumbnails/demo3.jpg', 'Tech Talk 科技分享', NULL, NOW(), 'completed',
   'videos/demo3.mp4', '/media/videos/demo3.mp4', 2548, 'public', 1280,
   '11111111-1111-1111-1111-111111111111', 'a82420d1-829a-40d7-ac96-269f9fd74d83'),
  ('aaaaaaaa-0000-0000-0000-000000000004', true, false, 12, NOW(), '音乐与生活主题竖屏演示', 12,
   2017821, 1280, false, 88, NULL, NOW(), 'published', 'thumbnails/demo4.jpg',
   '/media/thumbnails/demo4.jpg', 'Music & Life 音乐生活', NULL, NOW(), 'completed',
   'videos/demo4.mp4', '/media/videos/demo4.mp4', 1560, 'public', 720,
   '44444444-4444-4444-4444-444444444444', 'b73afbd2-a716-49de-9319-0f74ccbc0d2e')
ON CONFLICT (id) DO NOTHING;
