package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.annotation.IgnoreToken;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 直播高光时刻服务 — 记录每分钟互动数据，自动标记峰值时间段。
 *
 * <h3>高光判定规则</h3>
 * <ul>
 *   <li>弹幕峰值：该分钟弹幕数 > 近10分钟均值的 2 倍</li>
 *   <li>礼物爆发：该分钟礼物价值 > 近10分钟均值的 3 倍</li>
 *   <li>人气激增：观众数增量 > 20%</li>
 * </ul>
 */
@Slf4j
@Api(tags = "精彩片段")
@RestController
@RequestMapping("/api/v1/highlight")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class HighlightController {

    // roomId → [{minute, danmu, gift, viewers}]
    private final Map<Integer, List<Map<String, Object>>> roomTimeline = new ConcurrentHashMap<>();
    // roomId → [highlightMinutes]
    private final Map<Integer, List<Integer>> roomHighlights = new ConcurrentHashMap<>();

    /**
     * 每分钟由外部调用，记录当前互动数据快照。
     */
    @PostMapping("/snapshot")
    public ApiResponse<String> recordSnapshot(@RequestBody Map<String, Object> body) {
        int roomId = (int) body.getOrDefault("roomId", 0);
        if (roomId == 0) return ApiResponse.ofError("roomId required");

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("minute", body.getOrDefault("minute", System.currentTimeMillis() / 60000));
        snapshot.put("danmu", body.getOrDefault("danmu", 0));
        snapshot.put("gift", body.getOrDefault("gift", 0.0));
        snapshot.put("viewers", body.getOrDefault("viewers", 0));

        roomTimeline.computeIfAbsent(roomId, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(snapshot);

        // 自动检测高光时刻
        detectHighlights(roomId);
        return ApiResponse.ofSuccess("ok");
    }

    private void detectHighlights(int roomId) {
        List<Map<String, Object>> timeline = roomTimeline.get(roomId);
        if (timeline == null || timeline.size() < 10) return;

        int size = timeline.size();
        int window = Math.min(10, size - 1);

        // 计算近10分钟均值
        double avgDanmu = 0, avgGift = 0, avgViewers = 0;
        for (int i = size - window - 1; i < size - 1; i++) {
            Map<String, Object> s = timeline.get(i);
            avgDanmu += ((Number) s.get("danmu")).doubleValue();
            avgGift += ((Number) s.get("gift")).doubleValue();
            avgViewers += ((Number) s.get("viewers")).doubleValue();
        }
        avgDanmu /= window;
        avgGift /= window;
        avgViewers /= window;

        Map<String, Object> latest = timeline.get(size - 1);
        double latestDanmu = ((Number) latest.get("danmu")).doubleValue();
        double latestGift = ((Number) latest.get("gift")).doubleValue();
        double latestViewers = ((Number) latest.get("viewers")).doubleValue();

        // 判定高光
        boolean isHighlight =
            (avgDanmu > 0 && latestDanmu > avgDanmu * 2) ||
            (avgGift > 0 && latestGift > avgGift * 3) ||
            (avgViewers > 0 && latestViewers > avgViewers * 1.2);

        if (isHighlight) {
            int minute = ((Number) latest.get("minute")).intValue();
            roomHighlights.computeIfAbsent(roomId, k -> new ArrayList<>()).add(minute);
            log.info("[精彩片段] room={} minute={} danmu={} gift={} viewers={}",
                roomId, minute, latestDanmu, latestGift, latestViewers);
        }
    }

    @IgnoreToken
    @GetMapping("/list/{roomId}")
    public ApiResponse<List<Integer>> getHighlights(@PathVariable int roomId) {
        return ApiResponse.ofSuccess(
            roomHighlights.getOrDefault(roomId, Collections.emptyList()));
    }

    @IgnoreToken
    @GetMapping("/timeline/{roomId}")
    public ApiResponse<List<Map<String, Object>>> getTimeline(@PathVariable int roomId) {
        return ApiResponse.ofSuccess(
            roomTimeline.getOrDefault(roomId, Collections.emptyList()));
    }
}
