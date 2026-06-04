package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.annotation.IgnoreToken;
import cn.imhtb.live.mappers.PresentRewardMapper;
import cn.imhtb.live.mappers.UserMapper;
import cn.imhtb.live.pojo.database.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 全站打赏排行榜（总榜 Top-10）。
 */
@Api(tags = "打赏排行榜")
@RestController
@RequestMapping("/api/v1/gift-rank")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class GiftRankController {

    private final PresentRewardMapper presentRewardMapper;
    private final UserMapper userMapper;

    @IgnoreToken
    @ApiOperation("全站打赏总榜 Top-10")
    @GetMapping("/total")
    public ApiResponse<List<Map<String, Object>>> totalRank() {
        // 聚合每个用户的打赏总额
        List<Map<String, Object>> rawList = presentRewardMapper.selectMaps(null);
        Map<Integer, Double> userTotals = new LinkedHashMap<>();

        for (Map<String, Object> row : rawList) {
            Integer toId = (Integer) row.get("to_id");
            Double price = row.get("total_price") != null ?
                ((Number) row.get("total_price")).doubleValue() : 0.0;
            if (toId != null && price > 0) {
                userTotals.merge(toId, price, Double::sum);
            }
        }

        // 排序取前10
        List<Map.Entry<Integer, Double>> sorted = userTotals.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(Collectors.toList());

        // 封装结果
        Set<Integer> userIds = sorted.stream().map(Map.Entry::getKey).collect(Collectors.toSet());
        List<User> users = userIds.isEmpty() ? Collections.emptyList() : userMapper.selectBatchIds(userIds);
        Map<Integer, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<Integer, Double> entry : sorted) {
            User user = userMap.get(entry.getKey());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank++);
            item.put("userId", entry.getKey());
            item.put("nickname", user != null ? user.getNickname() : "未知用户");
            item.put("avatar", user != null ? user.getAvatar() : "");
            item.put("amount", Math.round(entry.getValue() * 100.0) / 100.0);
            result.add(item);
        }

        return ApiResponse.ofSuccess(result);
    }
}
