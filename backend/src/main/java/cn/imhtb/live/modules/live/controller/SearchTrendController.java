package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.annotation.IgnoreToken;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 热门搜索 — Redis Sorted Set 记录搜索词频，返回趋势热词。
 */
@Api(tags = "热门搜索")
@RestController
@RequestMapping("/api/v1/search-trend")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class SearchTrendController {

    private static final String TREND_KEY = "search:trend";
    private final RedisTemplate<String, String> redisTemplate;

    @IgnoreToken
    @ApiOperation("记录搜索关键词")
    @PostMapping("/record")
    public ApiResponse<String> record(@RequestBody Map<String, String> body) {
        String keyword = body.get("keyword");
        if (keyword == null || keyword.trim().isEmpty()) {
            return ApiResponse.ofError("关键词不能为空");
        }
        String kw = keyword.trim().toLowerCase();
        redisTemplate.opsForZSet().incrementScore(TREND_KEY, kw, 1.0);
        return ApiResponse.ofSuccess("ok");
    }

    @IgnoreToken
    @ApiOperation("获取热门搜索 Top-N")
    @GetMapping("/hot")
    public ApiResponse<List<Map<String, Object>>> hot(@RequestParam(defaultValue = "8") int limit) {
        Set<ZSetOperations.TypedTuple<String>> top =
            redisTemplate.opsForZSet().reverseRangeWithScores(TREND_KEY, 0, limit - 1);

        if (top == null || top.isEmpty()) {
            return ApiResponse.ofSuccess(Collections.emptyList());
        }

        List<Map<String, Object>> result = top.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("keyword", t.getValue());
            m.put("count", t.getScore().intValue());
            return m;
        }).collect(Collectors.toList());

        return ApiResponse.ofSuccess(result);
    }
}
