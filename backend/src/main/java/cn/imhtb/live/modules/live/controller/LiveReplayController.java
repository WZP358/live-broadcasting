package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.modules.live.service.ILiveReplayService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "直播回放接口")
@RestController
@RequestMapping("/api/v1/replay")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class LiveReplayController {

    private final ILiveReplayService replayService;

    @ApiOperation("获取房间回放列表")
    @GetMapping("/list")
    public ApiResponse<?> list(@RequestParam Integer roomId,
                                @RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer limit) {
        return ApiResponse.ofSuccess(replayService.listByRoom(roomId, page, limit));
    }

    @ApiOperation("获取最新回放")
    @GetMapping("/latest")
    public ApiResponse<?> latest(@RequestParam Integer roomId) {
        return ApiResponse.ofSuccess(replayService.getLatestByRoom(roomId));
    }
}
