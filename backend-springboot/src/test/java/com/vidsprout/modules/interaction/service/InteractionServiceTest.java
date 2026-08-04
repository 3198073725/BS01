package com.vidsprout.modules.interaction.service;

import com.vidsprout.common.exception.BusinessException;
import com.vidsprout.common.exception.ResourceNotFoundException;
import com.vidsprout.common.exception.ResourceNotFoundException;
import com.vidsprout.modules.interaction.model.Follow;
import com.vidsprout.modules.interaction.model.Like;
import com.vidsprout.modules.interaction.repository.*;
import com.vidsprout.modules.user.model.User;
import com.vidsprout.modules.user.repository.UserRepository;
import com.vidsprout.modules.user.service.AuthService;
import com.vidsprout.modules.video.model.Video;
import com.vidsprout.modules.video.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InteractionServiceTest {

    @Mock private LikeRepository likeRepository;
    @Mock private FavoriteRepository favoriteRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private FollowRepository followRepository;
    @Mock private VideoRepository videoRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthService authService;

    @InjectMocks
    private InteractionService interactionService;

    private User currentUser;
    private User targetUser;
    private Video testVideo;
    private UUID userId;
    private UUID targetUserId;
    private UUID videoId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();
        videoId = UUID.randomUUID();

        currentUser = User.builder().id(userId).username("user1").nickname("用户1").isActive(true).build();
        targetUser = User.builder().id(targetUserId).username("user2").nickname("用户2").isActive(true).build();
        testVideo = Video.builder().id(videoId).title("测试视频").status("published").build();

        lenient().when(authService.getCurrentUserEntity()).thenReturn(currentUser);
    }

    @Nested
    @DisplayName("关注测试")
    class FollowTests {

        @Test
        @DisplayName("正常关注应成功")
        void shouldFollowSuccessfully() {
            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
            when(followRepository.existsByFollowerIdAndFollowingId(userId, targetUserId)).thenReturn(false);
            when(followRepository.save(any(Follow.class))).thenReturn(Follow.builder().build());

            Map<String, Object> result = interactionService.follow(targetUserId);

            assertTrue((Boolean) result.get("ok"));
            verify(followRepository).save(any(Follow.class));
        }

        @Test
        @DisplayName("不能关注自己")
        void shouldNotFollowSelf() {
            assertThrows(BusinessException.class, () -> interactionService.follow(userId));
            verify(followRepository, never()).save(any());
        }

        @Test
        @DisplayName("已关注时重复关注不应出错")
        void shouldNotErrorWhenAlreadyFollowing() {
            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
            when(followRepository.existsByFollowerIdAndFollowingId(userId, targetUserId)).thenReturn(true);

            Map<String, Object> result = interactionService.follow(targetUserId);

            assertTrue((Boolean) result.get("ok"));
            verify(followRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("点赞测试")
    class LikeTests {

        @Test
        @DisplayName("点赞视频应成功")
        void shouldToggleLikeOn() {
            when(likeRepository.findByUserIdAndVideoId(userId, videoId)).thenReturn(Optional.empty());
            when(videoRepository.findById(videoId)).thenReturn(Optional.of(testVideo));
            when(likeRepository.save(any(Like.class))).thenReturn(Like.builder().build());

            Map<String, Object> result = interactionService.toggleLike(videoId);

            assertTrue((Boolean) result.get("liked"));
            verify(videoRepository).incrementLikeCount(videoId, 1);
        }

        @Test
        @DisplayName("取消点赞应成功")
        void shouldToggleLikeOff() {
            Like existingLike = Like.builder().user(currentUser).video(testVideo).build();
            when(likeRepository.findByUserIdAndVideoId(userId, videoId)).thenReturn(Optional.of(existingLike));

            Map<String, Object> result = interactionService.toggleLike(videoId);

            assertFalse((Boolean) result.get("liked"));
            verify(videoRepository).incrementLikeCount(videoId, -1);
            verify(likeRepository).delete(existingLike);
        }

        @Test
        @DisplayName("点赞不存在的视频应抛出异常")
        void shouldThrowWhenVideoNotFound() {
            UUID nonexistentId = UUID.randomUUID();
            when(likeRepository.findByUserIdAndVideoId(userId, nonexistentId)).thenReturn(Optional.empty());
            when(videoRepository.findById(nonexistentId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> interactionService.toggleLike(nonexistentId));
        }
    }

    @Nested
    @DisplayName("取消关注测试")
    class UnfollowTests {

        @Test
        @DisplayName("正常取消关注应成功")
        void shouldUnfollowSuccessfully() {
            Follow follow = Follow.builder().follower(currentUser).following(targetUser).build();
            when(followRepository.findByFollowerIdAndFollowingId(userId, targetUserId)).thenReturn(Optional.of(follow));

            Map<String, Object> result = interactionService.unfollow(targetUserId);

            assertTrue((Boolean) result.get("ok"));
            verify(followRepository).delete(follow);
        }

        @Test
        @DisplayName("取消未关注的用户应抛出异常")
        void shouldThrowWhenNotFollowing() {
            when(followRepository.findByFollowerIdAndFollowingId(userId, targetUserId)).thenReturn(Optional.empty());

            assertThrows(BusinessException.class, () -> interactionService.unfollow(targetUserId));
        }
    }

    @Nested
    @DisplayName("获取互动状态测试")
    class GetInteractionStateTests {

        @Test
        @DisplayName("获取已点赞视频ID列表")
        void shouldGetLikedVideoIds() {
            List<UUID> videoIds = List.of(videoId);
            when(likeRepository.findLikedVideoIds(userId, videoIds)).thenReturn(Set.of(videoId));

            Set<UUID> result = interactionService.getLikedVideoIds(userId, videoIds);

            assertTrue(result.contains(videoId));
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("空列表应返回空集合")
        void shouldReturnEmptyForNullList() {
            Set<UUID> result = interactionService.getLikedVideoIds(userId, null);
            assertTrue(result.isEmpty());

            result = interactionService.getLikedVideoIds(userId, Collections.emptyList());
            assertTrue(result.isEmpty());
        }
    }
}
