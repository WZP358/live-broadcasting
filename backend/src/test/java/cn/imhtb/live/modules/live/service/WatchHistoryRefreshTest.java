package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.common.enums.WatchTypeEnum;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.mappers.UserMapper;
import cn.imhtb.live.mappers.WatchMapper;
import cn.imhtb.live.modules.live.event.LiveEventBus;
import cn.imhtb.live.pojo.database.Watch;
import cn.imhtb.live.service.impl.IWatchServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WatchHistoryRefresh 观看历史刷新")
class WatchHistoryRefreshTest {

    @Mock
    private WatchMapper watchMapper;
    @Mock
    private RoomMapper roomMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private LiveEventBus eventBus;

    @InjectMocks
    private IWatchServiceImpl watchService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(watchService, "baseMapper", watchMapper);
    }

    @Test
    @DisplayName("Given: 已看过同一直播间, When: 再次进入, Then: 不新增记录但刷新最近观看时间")
    void shouldRefreshUpdateTimeWhenHistoryAlreadyExists() {
        when(watchMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(watchMapper.update(any(Watch.class), any(LambdaQueryWrapper.class))).thenReturn(1);

        Boolean result = watchService.saveHistory(1, 200);

        assertTrue(result);
        verify(watchMapper, never()).insert(any(Watch.class));
        verify(watchMapper).update(
                argThat(watch -> watch.getUpdateTime() != null),
                argThat(wrapper -> wrapper != null));
    }

    @Test
    @DisplayName("Given: 首次进入直播间, When: 保存历史, Then: 写入历史记录")
    void shouldInsertHistoryForFirstView() {
        when(watchMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(watchMapper.insert(any(Watch.class))).thenReturn(1);

        Boolean result = watchService.saveHistory(1, 200);

        assertTrue(result);
        verify(watchMapper).insert(org.mockito.ArgumentMatchers.<Watch>argThat(watch ->
                watch.getUserId().equals(1)
                        && watch.getRoomId().equals(200)
                        && watch.getWatchType().equals(WatchTypeEnum.HISTORY.getCode())));
        verify(watchMapper, never()).update(any(Watch.class), any(LambdaQueryWrapper.class));
    }
}
