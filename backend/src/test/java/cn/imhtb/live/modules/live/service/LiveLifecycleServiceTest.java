package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.common.enums.LiveInfoStatusEnum;
import cn.imhtb.live.common.enums.LiveRoomStatusEnum;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.modules.live.event.LiveEventBus;
import cn.imhtb.live.modules.live.event.LiveStartedEvent;
import cn.imhtb.live.modules.live.event.LiveStoppedEvent;
import cn.imhtb.live.pojo.database.LiveInfo;
import cn.imhtb.live.pojo.database.Room;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@DisplayName("LiveLifecycleService 直播生命周期管理")
class LiveLifecycleServiceTest {

    private RoomMapper roomMapper;
    private ILiveInfoService liveInfoService;
    private LiveEventBus eventBus;
    private ILiveReplayService replayService;
    private LiveLifecycleService service;

    @BeforeEach
    void setUp() {
        roomMapper = mock(RoomMapper.class);
        liveInfoService = mock(ILiveInfoService.class);
        eventBus = mock(LiveEventBus.class);
        replayService = mock(ILiveReplayService.class);
        service = new LiveLifecycleService(roomMapper, liveInfoService, eventBus, replayService);
    }

    @Test
    @DisplayName("开播：设置房间状态为 LIVING，创建 LiveInfo，发送事件")
    void shouldStartLiveAndPublishEvent() {
        Room room = new Room();
        room.setId(1);
        room.setUserId(100);
        room.setStatus(LiveRoomStatusEnum.STOP.getCode());

        when(roomMapper.selectById(1)).thenReturn(room);
        doReturn(1).when(roomMapper).updateById(isA(Room.class));
        when(liveInfoService.getOne(any(LambdaQueryWrapper.class), eq(false))).thenReturn(null);
        when(liveInfoService.save(any())).thenReturn(true);

        service.markLiveStarted(1, 100);

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomMapper).updateById(roomCaptor.capture());
        assertEquals(LiveRoomStatusEnum.LIVING.getCode(), roomCaptor.getValue().getStatus());

        ArgumentCaptor<LiveInfo> infoCaptor = ArgumentCaptor.forClass(LiveInfo.class);
        verify(liveInfoService).save(infoCaptor.capture());
        assertEquals(1, infoCaptor.getValue().getRoomId());
        assertEquals(LiveInfoStatusEnum.LIVING.getCode(), infoCaptor.getValue().getStatus());

        ArgumentCaptor<LiveStartedEvent> eventCaptor = ArgumentCaptor.forClass(LiveStartedEvent.class);
        verify(eventBus).publish(eventCaptor.capture());
        assertEquals(1, eventCaptor.getValue().getRoomId());
    }

    @Test
    @DisplayName("开播：已有进行中的 LiveInfo 不重复创建 LiveInfo，也不重复发事件")
    void shouldNotDuplicateLiveInfoWhenAlreadyLiving() {
        Room room = new Room();
        room.setId(2);
        room.setUserId(200);

        LiveInfo existingLiveInfo = new LiveInfo();
        existingLiveInfo.setId(99);
        existingLiveInfo.setRoomId(2);
        existingLiveInfo.setStatus(LiveInfoStatusEnum.LIVING.getCode());

        when(roomMapper.selectById(2)).thenReturn(room);
        doReturn(1).when(roomMapper).updateById(isA(Room.class));
        when(liveInfoService.getOne(any(LambdaQueryWrapper.class), eq(false))).thenReturn(existingLiveInfo);

        service.markLiveStarted(2, 200);

        // 已有 liveInfo 时不重复创建，也不重复推送开播通知
        verify(liveInfoService, never()).save(any());
        verify(eventBus, never()).publish(any());
    }

    @Test
    @DisplayName("开播：roomId 为 null 直接返回")
    void shouldReturnEarlyWhenRoomIdNull() {
        service.markLiveStarted(null, 100);
        verify(roomMapper, never()).selectById(any());
        verify(eventBus, never()).publish(any());
    }

    @Test
    @DisplayName("下播：设置房间状态为 STOP，结束 LiveInfo，发送事件")
    void shouldStopLiveAndPublishEvent() {
        LiveInfo liveInfo = new LiveInfo();
        liveInfo.setId(10);
        liveInfo.setRoomId(3);
        liveInfo.setStatus(LiveInfoStatusEnum.LIVING.getCode());

        when(liveInfoService.getOne(any(LambdaQueryWrapper.class), eq(false))).thenReturn(liveInfo);
        when(liveInfoService.updateById(isA(LiveInfo.class))).thenReturn(true);
        doReturn(1).when(roomMapper).updateById(isA(Room.class));

        service.markLiveStopped(3);

        ArgumentCaptor<LiveInfo> infoCaptor = ArgumentCaptor.forClass(LiveInfo.class);
        verify(liveInfoService).updateById(infoCaptor.capture());
        assertEquals(LiveInfoStatusEnum.FINISHED.getCode(), infoCaptor.getValue().getStatus());
        assertNotNull(infoCaptor.getValue().getEndTime());

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomMapper).updateById(roomCaptor.capture());
        assertEquals(LiveRoomStatusEnum.STOP.getCode(), roomCaptor.getValue().getStatus());

        ArgumentCaptor<LiveStoppedEvent> eventCaptor = ArgumentCaptor.forClass(LiveStoppedEvent.class);
        verify(eventBus).publish(eventCaptor.capture());
        assertEquals(3, eventCaptor.getValue().getRoomId());
    }

    @Test
    @DisplayName("下播：roomId 为 null 直接返回")
    void shouldReturnEarlyOnStopWhenRoomIdNull() {
        service.markLiveStopped(null);
        verify(liveInfoService, never()).getOne(any(), anyBoolean());
        verify(eventBus, never()).publish(any());
    }

    @Test
    @DisplayName("下播：即使没有 LiveInfo 也要更新房间状态")
    void shouldStillUpdateRoomStatusEvenWithoutLiveInfo() {
        when(liveInfoService.getOne(any(LambdaQueryWrapper.class), eq(false))).thenReturn(null);
        doReturn(1).when(roomMapper).updateById(isA(Room.class));

        service.markLiveStopped(5);

        verify(liveInfoService, never()).updateById(any());
        verify(roomMapper).updateById(isA(Room.class));
        verify(eventBus).publish(isA(LiveStoppedEvent.class));
    }
}
