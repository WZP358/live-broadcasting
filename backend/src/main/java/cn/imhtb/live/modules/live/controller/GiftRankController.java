package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.annotation.IgnoreToken;
import cn.imhtb.live.common.enums.PresentRewardTypeEnum;
import cn.imhtb.live.mappers.PresentRewardMapper;
import cn.imhtb.live.mappers.UserMapper;
import cn.imhtb.live.pojo.database.PresentReward;
import cn.imhtb.live.pojo.database.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gift ranking built from recent real reward records.
 */
@Api(tags = "Gift ranking")
@RestController
@RequestMapping("/api/v1/gift-rank")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class GiftRankController {

    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_LIMIT = 50;

    private final PresentRewardMapper presentRewardMapper;
    private final UserMapper userMapper;

    @IgnoreToken
    @ApiOperation("Gift ranking")
    @GetMapping("/total")
    public ApiResponse<List<Map<String, Object>>> totalRank(
            @RequestParam(defaultValue = "30") Integer days,
            @RequestParam(defaultValue = "10") Integer limit) {
        int queryDays = days == null || days <= 0 ? DEFAULT_DAYS : days;
        int queryLimit = Math.max(1, Math.min(limit == null ? 10 : limit, MAX_LIMIT));

        LambdaQueryWrapper<PresentReward> wrapper = new LambdaQueryWrapper<PresentReward>()
                .isNotNull(PresentReward::getToId)
                .gt(PresentReward::getTotalPrice, BigDecimal.ZERO)
                .ge(PresentReward::getCreateTime, LocalDateTime.now().minusDays(queryDays))
                .and(w -> w.eq(PresentReward::getType, PresentRewardTypeEnum.LIVE.getCode())
                        .or()
                        .isNull(PresentReward::getType));
        List<PresentReward> rewards = presentRewardMapper.selectList(wrapper);
        if (rewards.isEmpty()) {
            return ApiResponse.ofSuccess(Collections.emptyList());
        }

        Map<Integer, BigDecimal> userTotals = new HashMap<>();
        for (PresentReward reward : rewards) {
            BigDecimal totalPrice = reward.getTotalPrice() == null ? BigDecimal.ZERO : reward.getTotalPrice();
            if (reward.getToId() != null && totalPrice.compareTo(BigDecimal.ZERO) > 0) {
                userTotals.merge(reward.getToId(), totalPrice, BigDecimal::add);
            }
        }

        List<Map.Entry<Integer, BigDecimal>> sorted = userTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(queryLimit)
                .collect(Collectors.toList());

        Set<Integer> userIds = sorted.stream().map(Map.Entry::getKey).collect(Collectors.toSet());
        List<User> users = userIds.isEmpty() ? Collections.emptyList() : userMapper.selectBatchIds(userIds);
        Map<Integer, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<Integer, BigDecimal> entry : sorted) {
            User user = userMap.get(entry.getKey());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank++);
            item.put("userId", entry.getKey());
            item.put("nickname", user != null ? user.getNickname() : "Unknown user");
            item.put("avatar", user != null ? user.getAvatar() : "");
            item.put("amount", entry.getValue().setScale(2, BigDecimal.ROUND_HALF_UP));
            result.add(item);
        }

        return ApiResponse.ofSuccess(result);
    }
}
