package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.annotation.IgnoreToken;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.modules.live.service.recommend.IRecommendService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 推荐控制器 — 基于余弦相似度的混合推荐。
 *
 * <h3>端点说明</h3>
 * <table>
 *   <tr><th>端点</th><th>方法</th><th>说明</th></tr>
 *   <tr><td>/api/v1/recommend/rooms</td><td>GET</td><td>个性化推荐首页</td></tr>
 *   <tr><td>/api/v1/recommend/similar-rooms/{roomId}</td><td>GET</td><td>物品-物品相似推荐</td></tr>
 *   <tr><td>/api/v1/recommend/refresh</td><td>POST</td><td>刷新推荐模型</td></tr>
 * </table>
 *
 * @author PulseLive Recommendation Team
 */
@Api(tags = "个性化推荐（余弦相似度）")
@RestController
@RequestMapping("/api/v1/recommend")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RecommendController {

    private final IRecommendService recommendService;

    @IgnoreToken
    @ApiOperation("个性化推荐直播间（基于余弦相似度的混合推荐）")
    @GetMapping("/rooms")
    public ApiResponse<List<Map<String, Object>>> recommendRooms(
            @RequestParam(defaultValue = "12") Integer limit) {
        Integer userId = UserHolder.getUserId();
        return ApiResponse.ofSuccess(recommendService.recommendForUser(userId, limit));
    }

    @IgnoreToken
    @ApiOperation("查找与指定直播间最相似的直播间（余弦相似度）")
    @GetMapping("/similar-rooms/{roomId}")
    public ApiResponse<List<Map<String, Object>>> similarRooms(
            @PathVariable Integer roomId,
            @RequestParam(defaultValue = "6") Integer limit) {
        return ApiResponse.ofSuccess(recommendService.similarRooms(roomId, limit));
    }

    @ApiOperation("手动刷新推荐模型")
    @PostMapping("/refresh")
    public ApiResponse<String> refreshModel() {
        recommendService.refreshModel();
        return ApiResponse.ofSuccess("推荐模型已刷新");
    }
}
