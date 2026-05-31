package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.mappers.RoomTagMapper;
import cn.imhtb.live.pojo.database.RoomTag;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "房间标签")
@RestController
@RequestMapping("/api/v1/tag")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RoomTagController {

    private final RoomTagMapper tagMapper;

    @GetMapping("/room/{roomId}")
    public ApiResponse<List<RoomTag>> listByRoom(@PathVariable Integer roomId) {
        return ApiResponse.ofSuccess(tagMapper.selectList(
                new LambdaQueryWrapper<RoomTag>().eq(RoomTag::getRoomId, roomId)));
    }

    @PostMapping("/room/save")
    public ApiResponse<Boolean> save(@RequestBody SaveReq req) {
        tagMapper.delete(new LambdaQueryWrapper<RoomTag>().eq(RoomTag::getRoomId, req.getRoomId()));
        if (req.getTags() != null) {
            for (String tag : req.getTags()) {
                RoomTag rt = new RoomTag();
                rt.setRoomId(req.getRoomId());
                rt.setTagName(tag.trim());
                tagMapper.insert(rt);
            }
        }
        return ApiResponse.ofSuccess(true);
    }

    @lombok.Data
    public static class SaveReq {
        private Integer roomId;
        private List<String> tags;
    }
}
