package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.mappers.*;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.common.enums.LiveRoomStatusEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Api(tags = "主播数据看板")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DashboardController {

    private final RoomMapper roomMapper;
    private final WatchMapper watchMapper;
    private final PresentRewardMapper rewardMapper;
    private final GuardianSubscriptionMapper guardianMapper;
    private final LiveInfoMapper liveInfoMapper;

    @ApiOperation("我的直播间列表(数据概览)")
    @GetMapping("/my-rooms")
    public ApiResponse<List<Map<String, Object>>> myRooms() {
        Integer userId = UserHolder.getUserId();
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getUserId, userId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Room r : rooms) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("title", r.getTitle());
            m.put("cover", r.getCover());
            m.put("status", r.getStatus());
            // 关注数
            Long watchCount = watchMapper.selectCount(new LambdaQueryWrapper<cn.imhtb.live.pojo.database.Watch>()
                    .eq(cn.imhtb.live.pojo.database.Watch::getRoomId, r.getId())
                    .eq(cn.imhtb.live.pojo.database.Watch::getWatchType, 1));
            m.put("watchCount", watchCount);
            // 粉丝团人数
            Long fanCount = guardianMapper.selectCount(new LambdaQueryWrapper<cn.imhtb.live.pojo.database.GuardianSubscription>()
                    .eq(cn.imhtb.live.pojo.database.GuardianSubscription::getTargetUserId, userId)
                    .eq(cn.imhtb.live.pojo.database.GuardianSubscription::getStatus, 1));
            m.put("fanCount", fanCount);
            // 今日礼物收入
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            List<cn.imhtb.live.pojo.database.PresentReward> todayRewards = rewardMapper.selectList(
                    new LambdaQueryWrapper<cn.imhtb.live.pojo.database.PresentReward>()
                            .eq(cn.imhtb.live.pojo.database.PresentReward::getRoomId, r.getId())
                            .apply("DATE(create_time) = {0}", today));
            BigDecimal todayIncome = todayRewards.stream()
                    .map(cn.imhtb.live.pojo.database.PresentReward::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            m.put("todayIncome", todayIncome);
            result.add(m);
        }
        return ApiResponse.ofSuccess(result);
    }

    @ApiOperation("直播间详细统计")
    @GetMapping("/room-stats/{roomId}")
    public ApiResponse<Map<String, Object>> roomStats(@PathVariable Integer roomId) {
        Map<String, Object> stats = new HashMap<>();
        // 今日观看人数 (简化：统计今日礼物送出人数)
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Long todayViewers = rewardMapper.selectCount(new LambdaQueryWrapper<cn.imhtb.live.pojo.database.PresentReward>()
                .eq(cn.imhtb.live.pojo.database.PresentReward::getRoomId, roomId)
                .apply("DATE(create_time) = {0}", today));
        stats.put("todayViewers", todayViewers);
        // 关注数
        Long watchCount = watchMapper.selectCount(new LambdaQueryWrapper<cn.imhtb.live.pojo.database.Watch>()
                .eq(cn.imhtb.live.pojo.database.Watch::getRoomId, roomId)
                .eq(cn.imhtb.live.pojo.database.Watch::getWatchType, 1));
        stats.put("watchCount", watchCount);
        // 近7天礼物收入
        List<Map<String, Object>> dailyIncome = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            List<cn.imhtb.live.pojo.database.PresentReward> dayRewards = rewardMapper.selectList(
                    new LambdaQueryWrapper<cn.imhtb.live.pojo.database.PresentReward>()
                            .eq(cn.imhtb.live.pojo.database.PresentReward::getRoomId, roomId)
                            .apply("DATE(create_time) = {0}", date));
            BigDecimal income = dayRewards.stream()
                    .map(cn.imhtb.live.pojo.database.PresentReward::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> day = new HashMap<>();
            day.put("date", date);
            day.put("income", income);
            dailyIncome.add(day);
        }
        stats.put("dailyIncome", dailyIncome);
        return ApiResponse.ofSuccess(stats);
    }
}
