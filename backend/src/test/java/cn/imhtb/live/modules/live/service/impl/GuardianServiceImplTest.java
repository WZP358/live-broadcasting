package cn.imhtb.live.modules.live.service.impl;

import cn.imhtb.live.mappers.GuardianSubscriptionMapper;
import cn.imhtb.live.modules.live.service.IUserLevelService;
import cn.imhtb.live.pojo.database.GuardianSubscription;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("GuardianService 守护订阅服务")
class GuardianServiceImplTest {

    private GuardianSubscriptionMapper guardianMapper;
    private IUserLevelService userLevelService;
    private GuardianServiceImpl service;

    @BeforeEach
    void setUp() {
        guardianMapper = mock(GuardianSubscriptionMapper.class);
        userLevelService = mock(IUserLevelService.class);
        service = new GuardianServiceImpl(guardianMapper, userLevelService);
    }

    @Test
    @DisplayName("订阅：新守护应创建记录并赠送经验")
    void shouldCreateNewSubscription() {
        when(guardianMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(guardianMapper.insert(any(GuardianSubscription.class))).thenReturn(1);

        service.subscribe(10, 20, 2, true); // 白银守护，自动续费

        ArgumentCaptor<GuardianSubscription> captor = ArgumentCaptor.forClass(GuardianSubscription.class);
        verify(guardianMapper).insert(captor.capture());
        GuardianSubscription saved = captor.getValue();
        assertEquals(10, saved.getUserId());
        assertEquals(20, saved.getTargetUserId());
        assertEquals(2, saved.getLevel());
        assertEquals(1, saved.getAutoRenew());
        assertEquals(1, saved.getStatus());
        assertEquals(new BigDecimal("600"), saved.getAmount());

        verify(userLevelService).addExp(10, 100); // level 2 * 50
    }

    @Test
    @DisplayName("订阅：续费应延长过期时间")
    void shouldRenewExistingSubscription() {
        GuardianSubscription existing = new GuardianSubscription();
        existing.setId(1);
        existing.setUserId(10);
        existing.setTargetUserId(20);
        existing.setLevel(1);
        existing.setExpireTime(LocalDateTime.now().plusDays(10));
        existing.setStatus(1);

        when(guardianMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(guardianMapper.updateById(isA(GuardianSubscription.class))).thenReturn(1);

        LocalDateTime originalExpireTime = existing.getExpireTime();
        service.subscribe(10, 20, 3, false); // 升级黄金

        ArgumentCaptor<GuardianSubscription> captor = ArgumentCaptor.forClass(GuardianSubscription.class);
        verify(guardianMapper).updateById(captor.capture());
        GuardianSubscription updated = captor.getValue();
        assertEquals(3, updated.getLevel());
        assertEquals(new BigDecimal("1200"), updated.getAmount());
        assertEquals(0, updated.getAutoRenew());
        assertTrue(updated.getExpireTime().isAfter(originalExpireTime));
    }

    @Test
    @DisplayName("订阅：不能守护自己")
    void shouldPreventSelfGuardian() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.subscribe(10, 10, 1, false));
        assertEquals("不能守护自己", ex.getMessage());
    }

    @Test
    @DisplayName("订阅：无效等级应拒绝")
    void shouldRejectInvalidLevel() {
        assertThrows(RuntimeException.class, () -> service.subscribe(10, 20, 0, false));
        assertThrows(RuntimeException.class, () -> service.subscribe(10, 20, 4, false));
    }

    @Test
    @DisplayName("取消自动续费：应设置 autoRenew=0")
    void shouldCancelAutoRenew() {
        GuardianSubscription existing = new GuardianSubscription();
        existing.setId(1);
        existing.setAutoRenew(1);
        when(guardianMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(guardianMapper.updateById(isA(GuardianSubscription.class))).thenReturn(1);

        service.cancelAutoRenew(10, 20);

        ArgumentCaptor<GuardianSubscription> captor = ArgumentCaptor.forClass(GuardianSubscription.class);
        verify(guardianMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getAutoRenew());
    }

    @Test
    @DisplayName("取消自动续费：不存在记录不抛异常")
    void shouldNotThrowWhenCancelNonExistent() {
        when(guardianMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertDoesNotThrow(() -> service.cancelAutoRenew(10, 20));
    }

    @Test
    @DisplayName("getMonthlyPrice：应返回对应等级价格")
    void shouldReturnCorrectMonthlyPrice() {
        assertEquals(300, service.getMonthlyPrice(1));
        assertEquals(600, service.getMonthlyPrice(2));
        assertEquals(1200, service.getMonthlyPrice(3));
        assertEquals(0, service.getMonthlyPrice(0));
        assertEquals(0, service.getMonthlyPrice(4));
    }
}
