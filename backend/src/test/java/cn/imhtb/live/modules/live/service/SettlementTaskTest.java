package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.mappers.PresentRewardMapper;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.mappers.SettlementMapper;
import cn.imhtb.live.pojo.database.PresentReward;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.pojo.database.Settlement;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("SettlementTask 月度收益结算")
class SettlementTaskTest {

    private PresentRewardMapper rewardMapper;
    private RoomMapper roomMapper;
    private SettlementMapper settlementMapper;
    private SettlementTask task;

    @BeforeEach
    void setUp() {
        rewardMapper = mock(PresentRewardMapper.class);
        roomMapper = mock(RoomMapper.class);
        settlementMapper = mock(SettlementMapper.class);
        task = new SettlementTask(rewardMapper, roomMapper, settlementMapper);
    }

    @Test
    @DisplayName("无礼物记录时跳过结算")
    void shouldSkipWhenNoRewards() {
        when(rewardMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        task.monthlySettle();

        verify(settlementMapper, never()).insert(isA(Settlement.class));
    }

    @Test
    @DisplayName("有礼物记录时正确计算：30% 平台抽成")
    void shouldCalculatePlatformFeeCorrectly() {
        PresentReward reward = new PresentReward();
        reward.setRoomId(1);
        reward.setTotalPrice(new BigDecimal("10000")); // 100元

        Room room = new Room();
        room.setId(1);
        room.setUserId(100);

        when(rewardMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(reward));
        when(roomMapper.selectById(1)).thenReturn(room);
        when(settlementMapper.insert(isA(Settlement.class))).thenReturn(1);

        task.monthlySettle();

        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementMapper).insert(captor.capture());
        Settlement saved = captor.getValue();

        assertEquals(100, saved.getUserId());
        assertEquals(new BigDecimal("10000"), saved.getGiftIncome());
        // 30% 抽成: 10000 * 0.3 = 3000
        assertEquals(0, new BigDecimal("3000").compareTo(saved.getPlatformFee()));
        // 净收入: 10000 - 3000 = 7000
        assertEquals(0, new BigDecimal("7000").compareTo(saved.getNetIncome()));
        assertEquals(0, new BigDecimal("7000").compareTo(saved.getWithdrawable()));
        assertEquals(1, saved.getStatus());
    }

    @Test
    @DisplayName("多房间分别结算")
    void shouldSettlePerRoom() {
        PresentReward r1 = new PresentReward();
        r1.setRoomId(1);
        r1.setTotalPrice(new BigDecimal("1000"));

        PresentReward r2 = new PresentReward();
        r2.setRoomId(2);
        r2.setTotalPrice(new BigDecimal("2000"));

        Room room1 = new Room();
        room1.setId(1);
        room1.setUserId(10);

        Room room2 = new Room();
        room2.setId(2);
        room2.setUserId(20);

        when(rewardMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(r1, r2));
        when(roomMapper.selectById(1)).thenReturn(room1);
        when(roomMapper.selectById(2)).thenReturn(room2);
        when(settlementMapper.insert(isA(Settlement.class))).thenReturn(1);

        task.monthlySettle();

        verify(settlementMapper, times(2)).insert(isA(Settlement.class));
    }

    @Test
    @DisplayName("房间不存在或没有 userId 时跳过")
    void shouldSkipRoomWithoutOwner() {
        PresentReward reward = new PresentReward();
        reward.setRoomId(5);
        reward.setTotalPrice(new BigDecimal("500"));

        Room room = new Room();
        room.setId(5);
        room.setUserId(null); // no owner

        when(rewardMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(reward));
        when(roomMapper.selectById(5)).thenReturn(room);

        task.monthlySettle();

        verify(settlementMapper, never()).insert(isA(Settlement.class));
    }
}
