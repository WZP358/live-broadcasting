package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.modules.live.service.IRoomModeratorService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "房管接口")
@RestController
@RequestMapping("/api/v1/moderator")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RoomModeratorController {

    private final IRoomModeratorService moderatorService;

    @ApiOperation("任命房管")
    @PostMapping("/appoint")
    public ApiResponse<Boolean> appoint(@RequestBody AppointRequest req) {
        boolean ok = moderatorService.appoint(req.getRoomId(), req.getUserId(), UserHolder.getUserId());
        return ok ? ApiResponse.ofSuccess(true) : ApiResponse.ofError("任命失败：权限不足或已是房管");
    }

    @ApiOperation("撤销房管")
    @PostMapping("/dismiss")
    public ApiResponse<Boolean> dismiss(@RequestBody AppointRequest req) {
        boolean ok = moderatorService.dismiss(req.getRoomId(), req.getUserId(), UserHolder.getUserId());
        return ok ? ApiResponse.ofSuccess(true) : ApiResponse.ofError("撤销失败：权限不足");
    }

    @ApiOperation("获取房管列表")
    @GetMapping("/list")
    public ApiResponse<?> list(@RequestParam Integer roomId) {
        return ApiResponse.ofSuccess(moderatorService.listByRoom(roomId));
    }

    @ApiOperation("检查是否房管")
    @GetMapping("/check")
    public ApiResponse<Boolean> check(@RequestParam Integer roomId) {
        return ApiResponse.ofSuccess(moderatorService.isModerator(roomId, UserHolder.getUserId()));
    }

    @ApiOperation("禁言用户")
    @PostMapping("/mute")
    public ApiResponse<Boolean> mute(@RequestBody MuteRequest req) {
        boolean ok = moderatorService.muteUser(req.getRoomId(), UserHolder.getUserId(), req.getTargetUserId(), req.getDuration());
        return ok ? ApiResponse.ofSuccess(true) : ApiResponse.ofError("禁言失败：权限不足或目标不可禁言");
    }

    @ApiOperation("踢出用户")
    @PostMapping("/kick")
    public ApiResponse<Boolean> kick(@RequestBody KickRequest req) {
        boolean ok = moderatorService.kickUser(req.getRoomId(), UserHolder.getUserId(), req.getTargetUserId());
        return ok ? ApiResponse.ofSuccess(true) : ApiResponse.ofError("踢出失败：权限不足或目标不可踢出");
    }

    @Data
    public static class AppointRequest {
        private Integer roomId;
        private Integer userId;
    }

    @Data
    public static class MuteRequest {
        private Integer roomId;
        private Integer targetUserId;
        private Integer duration;
    }

    @Data
    public static class KickRequest {
        private Integer roomId;
        private Integer targetUserId;
    }
}
