package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.mappers.PresentRewardMapper;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.mappers.SettlementMapper;
import cn.imhtb.live.pojo.database.PresentReward;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.pojo.database.Settlement;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 每月1号凌晨执行上月的收益结算
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementTask {

    private final PresentRewardMapper rewardMapper;
    private final RoomMapper roomMapper;
    private final SettlementMapper settlementMapper;

    private static final BigDecimal PLATFORM_RATE = new BigDecimal("0.30"); // 30%平台抽成

    @Scheduled(cron = "0 0 2 1 * ?") // 每月1号凌晨2点
    public void monthlySettle() {
        String period = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        log.info("开始结算周期: {}", period);

        LocalDateTime start = LocalDate.now().minusMonths(1).withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        // 查询上个月所有礼物记录
        List<PresentReward> rewards = rewardMapper.selectList(new LambdaQueryWrapper<PresentReward>()
                .ge(PresentReward::getCreateTime, start)
                .lt(PresentReward::getCreateTime, end));

        if (rewards.isEmpty()) {
            log.info("结算周期 {} 无礼物记录", period);
            return;
        }

        // 按 room 分组 (一个room对应一个主播)
        Map<Integer, List<PresentReward>> byRoom = rewards.stream()
                .collect(Collectors.groupingBy(PresentReward::getRoomId));

        for (Map.Entry<Integer, List<PresentReward>> entry : byRoom.entrySet()) {
            Integer roomId = entry.getKey();
            List<PresentReward> roomRewards = entry.getValue();

            Room room = roomMapper.selectById(roomId);
            if (room == null || room.getUserId() == null) continue;

            BigDecimal giftIncome = roomRewards.stream()
                    .map(PresentReward::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal platformFee = giftIncome.multiply(PLATFORM_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal netIncome = giftIncome.subtract(platformFee);

            Settlement settlement = new Settlement();
            settlement.setUserId(room.getUserId());
            settlement.setPeriod(period);
            settlement.setGiftIncome(giftIncome);
            settlement.setPlatformFee(platformFee);
            settlement.setNetIncome(netIncome);
            settlement.setWithdrawable(netIncome);
            settlement.setWithdrawn(BigDecimal.ZERO);
            settlement.setStatus(1); // 已结算
            settlement.setSettleTime(LocalDateTime.now());
            settlementMapper.insert(settlement);
            log.info("结算: userId={} period={} giftIncome={} netIncome={}", room.getUserId(), period, giftIncome, netIncome);
        }
        log.info("结算周期 {} 完成，共处理 {} 个房间", period, byRoom.size());
    }
}
