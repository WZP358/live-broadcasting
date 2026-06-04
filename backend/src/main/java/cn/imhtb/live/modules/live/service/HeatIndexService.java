package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.common.enums.LiveRoomStatusEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 直播间热度指数服务 — Redis 实时热度排行榜。
 *
 * <h3>热度公式</h3>
 * <pre>
 *   heat = 观看人数 × 0.5 + 弹幕数 × 0.3 + 礼物价值 × 0.2 + 基础分
 * </pre>
 *
 * <p>使用 Redis Sorted Set 存储，5 秒轮询更新。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeatIndexService {

    private static final String HEAT_KEY = "live:heat:ranking";
    private static final String VIEWERS_KEY = "live:room:viewers:";
    private static final String DANMU_KEY = "live:room:danmu:";
    private static final String GIFT_KEY = "live:room:gift:";

    private final RedisTemplate<String, String> redisTemplate;
    private final RoomMapper roomMapper;

    /**
     * 更新房间观看人数（观众进入/离开时调用）。
     */
    public void updateViewers(int roomId, int delta) {
        String key = VIEWERS_KEY + roomId;
        redisTemplate.opsForValue().increment(key, delta);
        redisTemplate.expire(key, 10, TimeUnit.MINUTES);
    }

    /**
     * 记录弹幕（每条 +1）。
     */
    public void recordDanmu(int roomId) {
        String key = DANMU_KEY + roomId;
        redisTemplate.opsForValue().increment(key, 1);
        redisTemplate.expire(key, 10, TimeUnit.MINUTES);
    }

    /**
     * 记录礼物价值。
     */
    public void recordGift(int roomId, double value) {
        String key = GIFT_KEY + roomId;
        redisTemplate.opsForValue().increment(key, value);
        redisTemplate.expire(key, 10, TimeUnit.MINUTES);
    }

    /**
     * 计算所有直播中房间的热度并更新排行榜。
     */
    @Scheduled(fixedRate = 5000)
    public void refreshHeatRanking() {
        try {
            List<Room> livingRooms = roomMapper.selectList(
                new LambdaQueryWrapper<Room>()
                    .eq(Room::getStatus, LiveRoomStatusEnum.LIVING.getCode())
                    .eq(Room::getDisabled, 1));

            for (Room room : livingRooms) {
                int viewers = getIntValue(VIEWERS_KEY + room.getId());
                int danmu = getIntValue(DANMU_KEY + room.getId());
                double giftValue = getDoubleValue(GIFT_KEY + room.getId());

                double heat = viewers * 0.5 + danmu * 0.3 + giftValue * 0.2 + 1.0;
                redisTemplate.opsForZSet().add(HEAT_KEY, String.valueOf(room.getId()), heat);
            }

            // 清理下播房间
            Set<String> ranked = redisTemplate.opsForZSet().range(HEAT_KEY, 0, -1);
            if (ranked != null) {
                Set<Integer> livingIds = livingRooms.stream().map(Room::getId).collect(Collectors.toSet());
                for (String idStr : ranked) {
                    if (!livingIds.contains(Integer.parseInt(idStr))) {
                        redisTemplate.opsForZSet().remove(HEAT_KEY, idStr);
                    }
                }
            }

            redisTemplate.expire(HEAT_KEY, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("[热度排行] 刷新失败: {}", e.getMessage());
        }
    }

    /**
     * 获取 Top-N 热度房间。
     */
    public List<Map<String, Object>> getTopHeatRooms(int limit) {
        Set<ZSetOperations.TypedTuple<String>> top = redisTemplate.opsForZSet()
            .reverseRangeWithScores(HEAT_KEY, 0, limit - 1);

        if (top == null || top.isEmpty()) return Collections.emptyList();

        List<Integer> roomIds = top.stream()
            .map(t -> Integer.parseInt(t.getValue()))
            .collect(Collectors.toList());

        List<Room> rooms = roomMapper.selectBatchIds(roomIds);
        Map<Integer, Room> roomMap = rooms.stream()
            .collect(Collectors.toMap(Room::getId, r -> r));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> t : top) {
            int roomId = Integer.parseInt(t.getValue());
            Room room = roomMap.get(roomId);
            if (room == null) continue;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", room.getId());
            m.put("title", room.getTitle());
            m.put("cover", room.getCover());
            m.put("categoryId", room.getCategoryId());
            m.put("heat", Math.round(t.getScore() * 10.0) / 10.0);
            result.add(m);
        }
        return result;
    }

    private int getIntValue(String key) {
        String val = redisTemplate.opsForValue().get(key);
        return val == null ? 0 : Integer.parseInt(val);
    }

    private double getDoubleValue(String key) {
        String val = redisTemplate.opsForValue().get(key);
        return val == null ? 0.0 : Double.parseDouble(val);
    }
}
