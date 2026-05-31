package cn.imhtb.live.modules.live.service.impl;

import cn.imhtb.live.mappers.GuardianSubscriptionMapper;
import cn.imhtb.live.modules.live.service.IGuardianService;
import cn.imhtb.live.modules.live.service.IUserLevelService;
import cn.imhtb.live.pojo.database.GuardianSubscription;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GuardianServiceImpl implements IGuardianService {

    private final GuardianSubscriptionMapper guardianMapper;
    private final IUserLevelService userLevelService;

    private static final int[] MONTHLY_PRICES = {0, 300, 600, 1200}; // 1=青铜 2=白银 3=黄金 (分)

    @Override
    @Transactional
    public void subscribe(Integer userId, Integer targetUserId, Integer level, boolean autoRenew) {
        if (userId.equals(targetUserId)) {
            throw new RuntimeException("不能守护自己");
        }
        if (level < 1 || level > 3) {
            throw new RuntimeException("守护等级无效");
        }
        // 检查已有守护
        GuardianSubscription existing = guardianMapper.selectOne(new LambdaQueryWrapper<GuardianSubscription>()
                .eq(GuardianSubscription::getUserId, userId)
                .eq(GuardianSubscription::getTargetUserId, targetUserId));
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            // 续费
            LocalDateTime newExpire = existing.getExpireTime().isAfter(now)
                    ? existing.getExpireTime().plusMonths(1)
                    : now.plusMonths(1);
            existing.setLevel(level);
            existing.setAmount(BigDecimal.valueOf(MONTHLY_PRICES[level]));
            existing.setExpireTime(newExpire);
            existing.setAutoRenew(autoRenew ? 1 : 0);
            existing.setStatus(1);
            guardianMapper.updateById(existing);
        } else {
            GuardianSubscription gs = new GuardianSubscription();
            gs.setUserId(userId);
            gs.setTargetUserId(targetUserId);
            gs.setLevel(level);
            gs.setAmount(BigDecimal.valueOf(MONTHLY_PRICES[level]));
            gs.setExpireTime(now.plusMonths(1));
            gs.setAutoRenew(autoRenew ? 1 : 0);
            gs.setStatus(1);
            gs.setCreateTime(now);
            guardianMapper.insert(gs);
        }
        // 守护赠送经验
        userLevelService.addExp(userId, level * 50);
    }

    @Override
    public void cancelAutoRenew(Integer userId, Integer targetUserId) {
        GuardianSubscription gs = guardianMapper.selectOne(new LambdaQueryWrapper<GuardianSubscription>()
                .eq(GuardianSubscription::getUserId, userId)
                .eq(GuardianSubscription::getTargetUserId, targetUserId));
        if (gs != null) {
            gs.setAutoRenew(0);
            guardianMapper.updateById(gs);
        }
    }

    @Override
    public Page<GuardianSubscription> myGuardians(Integer userId, int page, int limit) {
        return guardianMapper.selectPage(new Page<>(page, limit),
                new LambdaQueryWrapper<GuardianSubscription>()
                        .eq(GuardianSubscription::getUserId, userId)
                        .eq(GuardianSubscription::getStatus, 1)
                        .orderByDesc(GuardianSubscription::getCreateTime));
    }

    @Override
    public Page<GuardianSubscription> myFans(Integer targetUserId, int page, int limit) {
        return guardianMapper.selectPage(new Page<>(page, limit),
                new LambdaQueryWrapper<GuardianSubscription>()
                        .eq(GuardianSubscription::getTargetUserId, targetUserId)
                        .eq(GuardianSubscription::getStatus, 1)
                        .orderByDesc(GuardianSubscription::getCreateTime));
    }

    @Override
    public boolean isGuardian(Integer userId, Integer targetUserId) {
        return guardianMapper.selectCount(new LambdaQueryWrapper<GuardianSubscription>()
                .eq(GuardianSubscription::getUserId, userId)
                .eq(GuardianSubscription::getTargetUserId, targetUserId)
                .eq(GuardianSubscription::getStatus, 1)
                .gt(GuardianSubscription::getExpireTime, LocalDateTime.now())) > 0;
    }

    @Override
    public Integer getGuardianLevel(Integer userId, Integer targetUserId) {
        GuardianSubscription gs = guardianMapper.selectOne(new LambdaQueryWrapper<GuardianSubscription>()
                .eq(GuardianSubscription::getUserId, userId)
                .eq(GuardianSubscription::getTargetUserId, targetUserId)
                .eq(GuardianSubscription::getStatus, 1)
                .gt(GuardianSubscription::getExpireTime, LocalDateTime.now()));
        return gs != null ? gs.getLevel() : 0;
    }

    @Override
    public int getMonthlyPrice(Integer level) {
        if (level < 1 || level > 3) return 0;
        return MONTHLY_PRICES[level];
    }

    @Override
    @Transactional
    public void checkExpired() {
        guardianMapper.update(null, new LambdaUpdateWrapper<GuardianSubscription>()
                .eq(GuardianSubscription::getStatus, 1)
                .lt(GuardianSubscription::getExpireTime, LocalDateTime.now())
                .set(GuardianSubscription::getStatus, 0));
    }
}
