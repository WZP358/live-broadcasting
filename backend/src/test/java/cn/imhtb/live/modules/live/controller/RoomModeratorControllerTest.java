package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.modules.live.service.IRoomModeratorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RoomModeratorController 房管接口")
class RoomModeratorControllerTest {

    private IRoomModeratorService moderatorService;
    private RoomModeratorController controller;

    @BeforeEach
    void setUp() {
        moderatorService = mock(IRoomModeratorService.class);
        controller = new RoomModeratorController(moderatorService);
        UserHolder.setUserId(42);
    }

    @AfterEach
    void tearDown() {
        UserHolder.remove();
    }

    @Test
    @DisplayName("任命房管成功")
    void shouldAppointModeratorSuccessfully() {
        when(moderatorService.appoint(1, 100, 42)).thenReturn(true);
        RoomModeratorController.AppointRequest req = new RoomModeratorController.AppointRequest();
        req.setRoomId(1);
        req.setUserId(100);

        ApiResponse<Boolean> res = controller.appoint(req);
        assertTrue(res.isSuccess());
    }

    @Test
    @DisplayName("任命房管失败返回中文错误")
    void shouldReturnChineseErrorOnAppointFail() {
        when(moderatorService.appoint(1, 100, 42)).thenReturn(false);
        RoomModeratorController.AppointRequest req = new RoomModeratorController.AppointRequest();
        req.setRoomId(1);
        req.setUserId(100);

        ApiResponse<Boolean> res = controller.appoint(req);
        assertEquals(1, res.getCode());
        assertEquals("任命失败：权限不足或已是房管", res.getMsg());
    }

    @Test
    @DisplayName("禁言用户")
    void shouldMuteUser() {
        when(moderatorService.muteUser(1, 42, 200, 300)).thenReturn(true);
        RoomModeratorController.MuteRequest req = new RoomModeratorController.MuteRequest();
        req.setRoomId(1);
        req.setTargetUserId(200);
        req.setDuration(300);

        ApiResponse<Boolean> res = controller.mute(req);
        assertTrue(res.isSuccess());
    }

    @Test
    @DisplayName("禁言失败返回中文错误")
    void shouldReturnChineseErrorOnMuteFail() {
        when(moderatorService.muteUser(eq(1), eq(42), eq(200), anyInt())).thenReturn(false);
        RoomModeratorController.MuteRequest req = new RoomModeratorController.MuteRequest();
        req.setRoomId(1);
        req.setTargetUserId(200);
        req.setDuration(60);

        ApiResponse<Boolean> res = controller.mute(req);
        assertFalse(res.isSuccess());
        assertEquals("禁言失败：权限不足或目标不可禁言", res.getMsg());
    }

    @Test
    @DisplayName("踢出用户")
    void shouldKickUser() {
        when(moderatorService.kickUser(1, 42, 300)).thenReturn(true);
        RoomModeratorController.KickRequest req = new RoomModeratorController.KickRequest();
        req.setRoomId(1);
        req.setTargetUserId(300);

        ApiResponse<Boolean> res = controller.kick(req);
        assertTrue(res.isSuccess());
    }

    @Test
    @DisplayName("踢出失败返回中文错误")
    void shouldReturnChineseErrorOnKickFail() {
        when(moderatorService.kickUser(1, 42, 300)).thenReturn(false);
        RoomModeratorController.KickRequest req = new RoomModeratorController.KickRequest();
        req.setRoomId(1);
        req.setTargetUserId(300);

        ApiResponse<Boolean> res = controller.kick(req);
        assertFalse(res.isSuccess());
        assertEquals("踢出失败：权限不足或目标不可踢出", res.getMsg());
    }

    @Test
    @DisplayName("检查是否为房管")
    void shouldCheckModeratorStatus() {
        when(moderatorService.isModerator(1, 42)).thenReturn(true);
        ApiResponse<Boolean> res = controller.check(1);
        assertTrue(res.isSuccess());
        assertTrue(res.getData());
    }

    @Test
    @DisplayName("获取房管列表")
    void shouldListModerators() {
        when(moderatorService.listByRoom(1)).thenReturn(List.of());
        ApiResponse<?> res = controller.list(1);
        assertTrue(res.isSuccess());
    }
}
