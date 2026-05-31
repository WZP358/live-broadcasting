package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.mappers.WatchMapper;
import cn.imhtb.live.mappers.CategoryMapper;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.pojo.database.Watch;
import cn.imhtb.live.pojo.database.Category;
import cn.imhtb.live.common.enums.LiveRoomStatusEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Api(tags = "个性化推荐")
@RestController
@RequestMapping("/api/v1/recommend")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RecommendController {

    private final RoomMapper roomMapper;
    private final WatchMapper watchMapper;
    private final CategoryMapper categoryMapper;

    @ApiOperation("推荐直播间")
    @GetMapping("/rooms")
    public ApiResponse<List<Map<String, Object>>> recommendRooms(@RequestParam(defaultValue = "12") Integer limit) {
        Integer userId = UserHolder.getUserId();
        Set<Integer> shownIds = new HashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();

        // 1. 用户关注的房间正在直播
        if (userId != null) {
            List<Watch> watches = watchMapper.selectList(new LambdaQueryWrapper<Watch>()
                    .eq(Watch::getUserId, userId)
                    .eq(Watch::getWatchType, 1));
            if (!watches.isEmpty()) {
                List<Integer> roomIds = watches.stream().map(Watch::getRoomId).collect(Collectors.toList());
                List<Room> followedLiving = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                        .in(Room::getId, roomIds)
                        .eq(Room::getStatus, LiveRoomStatusEnum.LIVING.getCode()));
                for (Room r : followedLiving) {
                    if (result.size() >= limit) break;
                    if (shownIds.add(r.getId())) {
                        result.add(roomToMap(r));
                    }
                }
            }

            // 2. 推荐同分类的热门直播间
            if (result.size() < limit && !watches.isEmpty()) {
                List<Integer> roomIds = watches.stream().map(Watch::getRoomId).collect(Collectors.toList());
                List<Room> watchedRooms = roomMapper.selectBatchIds(roomIds);
                Set<Integer> categoryIds = watchedRooms.stream()
                        .map(Room::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
                if (!categoryIds.isEmpty()) {
                    List<Room> sameCategory = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                            .in(Room::getCategoryId, categoryIds)
                            .eq(Room::getStatus, LiveRoomStatusEnum.LIVING.getCode())
                            .notIn(Room::getId, shownIds)
                            .orderByDesc(Room::getId)
                            .last("limit " + (limit - result.size())));
                    for (Room r : sameCategory) {
                        if (result.size() >= limit) break;
                        if (shownIds.add(r.getId())) {
                            result.add(roomToMap(r));
                        }
                    }
                }
            }
        }

        // 3. 补充分类名 (批量)
        if (!result.isEmpty()) {
            List<Integer> catIds = result.stream()
                    .map(m -> (Integer) m.get("categoryId")).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!catIds.isEmpty()) {
                List<Category> cats = categoryMapper.selectBatchIds(catIds);
                Map<Integer, String> catMap = cats.stream().collect(Collectors.toMap(Category::getId, Category::getName));
                for (Map<String, Object> m : result) {
                    Integer cid = (Integer) m.get("categoryId");
                    if (cid != null) m.put("categoryName", catMap.getOrDefault(cid, ""));
                }
            }
        }

        return ApiResponse.ofSuccess(result);
    }

    private Map<String, Object> roomToMap(Room r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("title", r.getTitle());
        m.put("cover", r.getCover());
        m.put("status", r.getStatus());
        m.put("categoryId", r.getCategoryId());
        return m;
    }
}
