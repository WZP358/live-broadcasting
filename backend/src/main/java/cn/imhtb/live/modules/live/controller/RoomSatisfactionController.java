package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.modules.live.service.IRoomSatisfactionService;
import cn.imhtb.live.pojo.database.RoomSatisfaction;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/room/satisfaction")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RoomSatisfactionController {

    private final IRoomSatisfactionService satisfactionService;

    @PostMapping("/submit")
    public ApiResponse<RoomSatisfaction> submit(@RequestBody SubmitReq req) {
        return ApiResponse.ofSuccess(satisfactionService.submit(req.getRoomId(), UserHolder.getUserId(), req.getScore()));
    }

    @Data
    public static class SubmitReq {
        private Integer roomId;
        private Integer score;
    }
}
