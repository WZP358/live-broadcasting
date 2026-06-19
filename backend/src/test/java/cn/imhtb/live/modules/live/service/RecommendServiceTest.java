package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.common.enums.LiveRoomStatusEnum;
import cn.imhtb.live.common.enums.StatusEnum;
import cn.imhtb.live.common.enums.WatchTypeEnum;
import cn.imhtb.live.mappers.CategoryMapper;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.mappers.WatchMapper;
import cn.imhtb.live.modules.live.event.LiveStartedEvent;
import cn.imhtb.live.modules.live.service.recommend.RecommendServiceImpl;
import cn.imhtb.live.pojo.database.Category;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.pojo.database.Watch;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendService 推荐业务规则")
class RecommendServiceTest {

    @Mock
    private RoomMapper roomMapper;
    @Mock
    private WatchMapper watchMapper;
    @Mock
    private CategoryMapper categoryMapper;

    @Test
    @DisplayName("直播间文本特征维度不一致时也能稳定推荐")
    void shouldRecommendWithAlignedTfIdfDimensions() {
        RecommendServiceImpl service = new RecommendServiceImpl(roomMapper, watchMapper, categoryMapper);
        Category tech = createCategory(1, "技术");
        Room historyRoom = createRoom(10, "Java 后端 实战", "Spring MySQL 教学", 1);
        Room candidate = createRoom(20, "Java 后端 进阶", "Spring Cloud 架构", 1);
        Room other = createRoom(30, "音乐 弹唱", "吉他 民谣 点歌", 1);
        Watch history = createWatch(99, 10, WatchTypeEnum.HISTORY.getCode());

        when(categoryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(tech));
        when(roomMapper.selectList(any(Wrapper.class)))
            .thenReturn(List.of(historyRoom, candidate, other))
            .thenReturn(List.of());
        when(watchMapper.selectList(any(Wrapper.class)))
            .thenReturn(List.of())
            .thenReturn(List.of(history))
            .thenReturn(List.of());
        when(roomMapper.selectById(20)).thenReturn(candidate);

        assertDoesNotThrow(service::refreshModel);
        List<Map<String, Object>> result = assertDoesNotThrow(() -> service.recommendForUser(99, 2));

        assertFalse(result.isEmpty());
        assertEquals(20, result.get(0).get("id"));
        assertEquals("content", result.get(0).get("recommendType"));
        assertEquals("与你最近观看或关注的内容相似", result.get(0).get("recommendReason"));
    }

    @Test
    @DisplayName("直播状态变化事件应自动刷新推荐模型")
    void shouldRefreshModelWhenLiveStatusChanged() {
        RecommendServiceImpl service = new RecommendServiceImpl(roomMapper, watchMapper, categoryMapper);
        Category tech = createCategory(1, "技术");
        when(categoryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(tech));
        when(roomMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        service.onEvent(new LiveStartedEvent(1, 100));

        verify(roomMapper, atLeast(1)).selectList(any(Wrapper.class));
    }

    @Test
    @DisplayName("相似直播推荐只返回直播中且分类启用的房间")
    void shouldFilterInvalidSimilarRooms() {
        RecommendServiceImpl service = new RecommendServiceImpl(roomMapper, watchMapper, categoryMapper);
        Category tech = createCategory(1, "技术");
        Room source = createRoom(10, "Java 后端 实战", "Spring MySQL 教学", 1);
        Room stopped = createRoom(20, "Java 后端 进阶", "Spring Cloud 架构", 1);
        stopped.setStatus(LiveRoomStatusEnum.STOP.getCode());
        Room living = createRoom(30, "Java 并发 实战", "JVM 线程池", 1);

        when(categoryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(tech));
        when(roomMapper.selectList(any(Wrapper.class))).thenReturn(List.of(source, stopped, living));
        when(roomMapper.selectById(eq(20))).thenReturn(stopped);
        when(roomMapper.selectById(eq(30))).thenReturn(living);

        service.refreshModel();
        List<Map<String, Object>> result = service.similarRooms(10, 5);

        assertEquals(1, result.size());
        assertEquals(30, result.get(0).get("id"));
        assertEquals("similar", result.get(0).get("recommendType"));
        assertEquals("与当前直播间分类和内容相似", result.get(0).get("recommendReason"));
    }

    private Category createCategory(Integer id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setStatus(StatusEnum.YES.getCode());
        category.setSort(1);
        return category;
    }

    private Room createRoom(Integer id, String title, String introduce, Integer categoryId) {
        Room room = new Room();
        room.setId(id);
        room.setTitle(title);
        room.setIntroduce(introduce);
        room.setCover("/cover-" + id + ".png");
        room.setUserId(id);
        room.setCategoryId(categoryId);
        room.setStatus(LiveRoomStatusEnum.LIVING.getCode());
        room.setDisabled(StatusEnum.YES.getCode());
        return room;
    }

    private Watch createWatch(Integer userId, Integer roomId, Integer watchType) {
        Watch watch = new Watch();
        watch.setUserId(userId);
        watch.setRoomId(roomId);
        watch.setWatchType(watchType);
        return watch;
    }
}
