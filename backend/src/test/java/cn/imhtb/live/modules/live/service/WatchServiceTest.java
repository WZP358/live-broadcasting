package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.enums.StatusEnum;
import cn.imhtb.live.common.enums.WatchTypeEnum;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.mappers.UserMapper;
import cn.imhtb.live.mappers.WatchMapper;
import cn.imhtb.live.modules.live.event.FollowedEvent;
import cn.imhtb.live.modules.live.event.LiveEventBus;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.pojo.database.User;
import cn.imhtb.live.pojo.database.Watch;
import cn.imhtb.live.pojo.vo.response.WatchResponse;
import cn.imhtb.live.service.impl.IWatchServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WatchService 关注历史服务")
class WatchServiceTest {

    @Mock
    private WatchMapper watchMapper;
    @Mock
    private RoomMapper roomMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private LiveEventBus eventBus;

    @InjectMocks
    private IWatchServiceImpl watchService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(watchService, "baseMapper", watchMapper);
    }

    // ─── Story: 关注直播间 ─────────────────────────────────

    @Nested
    @DisplayName("Story: 用户关注直播间")
    class FollowRoom {

        @Test
        @DisplayName("Given: 未关注, When: 点击关注, Then: 关注成功并触发事件")
        void shouldFollowSuccessfully() {
            when(roomMapper.selectById(100)).thenReturn(createRoom(100, "房间A", 9));
            when(watchMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(watchMapper.insert(any(Watch.class))).thenReturn(1);

            Boolean result = watchService.follow(1, 100);

            assertTrue(result);
            verify(watchMapper).insert(argThat((Watch watch) ->
                    watch.getUserId().equals(1)
                            && watch.getRoomId().equals(100)
                            && watch.getWatchType().equals(WatchTypeEnum.FOLLOW.getCode())));
            verify(eventBus).publish(any(FollowedEvent.class));
        }

        @Test
        @DisplayName("Given: 已关注, When: 再次关注, Then: 返回 false 不重复关注")
        void shouldNotDuplicateFollow() {
            when(roomMapper.selectById(100)).thenReturn(createRoom(100, "房间A", 9));
            when(watchMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            Boolean result = watchService.follow(1, 100);

            assertFalse(result);
            verify(watchMapper, never()).insert(any(Watch.class));
            verify(eventBus, never()).publish(any(FollowedEvent.class));
        }

        @Test
        @DisplayName("Given: 用户进入自己的直播间, When: 点击关注, Then: 拒绝关注自己")
        void shouldRejectSelfFollow() {
            when(roomMapper.selectById(100)).thenReturn(createRoom(100, "自己的房间", 1));

            Boolean result = watchService.follow(1, 100);

            assertFalse(result);
            verify(watchMapper, never()).selectCount(any(LambdaQueryWrapper.class));
            verify(watchMapper, never()).insert(any(Watch.class));
        }

        @Test
        @DisplayName("Given: 房间已封禁, When: 点击关注, Then: 拒绝关注不可用房间")
        void shouldRejectDisabledRoomFollow() {
            Room room = createRoom(100, "封禁房间", 9);
            room.setDisabled(StatusEnum.NO.getCode());
            when(roomMapper.selectById(100)).thenReturn(room);

            Boolean result = watchService.follow(1, 100);

            assertFalse(result);
            verify(watchMapper, never()).selectCount(any(LambdaQueryWrapper.class));
            verify(watchMapper, never()).insert(any(Watch.class));
        }

        @Test
        @DisplayName("When: 取消关注, Then: 只删除关注记录")
        void shouldUnfollowSuccessfully() {
            when(watchMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

            Boolean result = watchService.unFollow(1, 100);

            assertTrue(result);
            ArgumentCaptor<LambdaQueryWrapper<Watch>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(watchMapper).delete(wrapperCaptor.capture());
            assertNotNull(wrapperCaptor.getValue());
        }
    }

    // ─── Story: 浏览历史 ──────────────────────────────────

    @Nested
    @DisplayName("Story: 用户浏览记录管理")
    class WatchHistory {

        @Test
        @DisplayName("Given: 未浏览过, When: 进入直播间, Then: 保存历史记录")
        void shouldSaveHistoryForNewRoom() {
            when(watchMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(watchMapper.insert(any(Watch.class))).thenReturn(1);

            Boolean result = watchService.saveHistory(1, 200);

            assertTrue(result);
            verify(watchMapper).insert(any(Watch.class));
        }

        @Test
        @DisplayName("Given: 已浏览过, When: 再次进入, Then: 刷新最近观看时间")
        void shouldRefreshHistoryWhenRoomAlreadyWatched() {
            when(watchMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
            when(watchMapper.update(any(Watch.class), any(LambdaQueryWrapper.class))).thenReturn(1);

            Boolean result = watchService.saveHistory(1, 200);

            assertTrue(result);
            verify(watchMapper, never()).insert(any(Watch.class));
            verify(watchMapper).update(argThat(watch -> watch.getUpdateTime() != null), any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("When: 清除历史, Then: 删除所有历史记录")
        void shouldClearAllHistory() {
            when(watchMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(3);

            Boolean result = watchService.clearHistory(1);

            assertTrue(result);
        }
    }

    // ─── Story: 获取关注/历史列表 ─────────────────────────

    @Nested
    @DisplayName("Story: 查看关注和历史列表")
    class ListWatches {

        @Test
        @DisplayName("Given: 有2条关注, When: 查询关注列表, Then: 返回2条数据")
        void shouldReturnFollowList() {
            LocalDateTime createTime = LocalDateTime.now();
            Watch w1 = Watch.builder().id(1).userId(1).roomId(100).watchType(WatchTypeEnum.FOLLOW.getCode()).createTime(createTime).build();
            Watch w2 = Watch.builder().id(2).userId(1).roomId(200).watchType(WatchTypeEnum.FOLLOW.getCode()).createTime(createTime).build();
            Page<Watch> mockPage = new Page<>(1, 10);
            mockPage.setRecords(Arrays.asList(w1, w2));
            mockPage.setTotal(2);

            when(watchMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);
            when(roomMapper.selectById(100)).thenReturn(createRoom(100, "房间A", 10));
            when(roomMapper.selectById(200)).thenReturn(createRoom(200, "房间B", 20));
            when(userMapper.selectById(anyInt())).thenReturn(createUser("主播"));

            PageData<WatchResponse> result = watchService.listWatches(1, WatchTypeEnum.FOLLOW.getCode(), 10, 1);

            assertNotNull(result);
            assertEquals(2, result.getTotal());
            assertEquals(2, result.getList().size());
            assertEquals("主播", result.getList().get(0).getName());
            assertEquals("/avatar.png", result.getList().get(0).getAvatar());
            assertEquals(10, result.getList().get(0).getAnchorUserId());
            assertEquals(WatchTypeEnum.FOLLOW.getCode(), result.getList().get(0).getWatchType());
            assertEquals(createTime, result.getList().get(0).getCreateTime());
        }

        @Test
        @DisplayName("Given: 无关注记录, When: 查询, Then: 返回空列表")
        void shouldReturnEmptyWhenNoWatches() {
            Page<Watch> emptyPage = new Page<>(1, 10);
            emptyPage.setRecords(Arrays.asList());
            emptyPage.setTotal(0);

            when(watchMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(emptyPage);

            PageData<WatchResponse> result = watchService.listWatches(1, WatchTypeEnum.FOLLOW.getCode(), 10, 1);

            assertNotNull(result);
            assertEquals(0, result.getTotal());
            assertTrue(result.getList().isEmpty());
        }

        @Test
        @DisplayName("Given: 非法类型, When: 查询, Then: 返回空分页且不访问数据库")
        void shouldReturnEmptyWhenWatchTypeUnsupported() {
            PageData<WatchResponse> result = watchService.listWatches(1, 99, 10, 1);

            assertNotNull(result);
            assertEquals(0, result.getTotal());
            assertTrue(result.getList().isEmpty());
            verify(watchMapper, never()).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }
    }

    // ─── Helpers ──────────────────────────────────────────

    private Room createRoom(Integer id, String title, Integer userId) {
        Room room = new Room();
        room.setId(id);
        room.setTitle(title);
        room.setUserId(userId);
        room.setCover("/cover.png");
        room.setStatus(1);
        room.setDisabled(StatusEnum.YES.getCode());
        return room;
    }

    private User createUser(String nickname) {
        User user = new User();
        user.setId(1);
        user.setNickname(nickname);
        user.setAvatar("/avatar.png");
        return user;
    }
}
