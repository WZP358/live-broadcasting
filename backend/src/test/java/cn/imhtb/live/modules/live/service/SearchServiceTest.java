package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.enums.LiveRoomStatusEnum;
import cn.imhtb.live.common.enums.StatusEnum;
import cn.imhtb.live.mappers.CategoryMapper;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.mappers.UserMapper;
import cn.imhtb.live.modules.live.service.impl.SearchServiceImpl;
import cn.imhtb.live.modules.live.vo.SearchResultVO;
import cn.imhtb.live.pojo.database.Category;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.pojo.database.User;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchService 搜索服务")
class SearchServiceTest {

    @Mock
    private RoomMapper roomMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CategoryMapper categoryMapper;

    @Test
    @DisplayName("搜索直播间时应按启用分类过滤，并返回直播卡片兼容字段")
    void shouldSearchRoomsByEnabledCategory() {
        SearchServiceImpl searchService = new SearchServiceImpl(roomMapper, userMapper, categoryMapper);
        Category category = createCategory(1, "技术", StatusEnum.YES.getCode());
        Room room = createRoom(10, "前端开发直播", 3, 1);
        User user = createUser(3, "小明");
        Page<Room> page = new Page<>(1, 12);
        page.setRecords(List.of(room));
        page.setTotal(1);

        when(categoryMapper.selectById(1)).thenReturn(category);
        when(roomMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(user));
        when(categoryMapper.selectBatchIds(anyCollection())).thenReturn(List.of(category));

        PageData<SearchResultVO> result = searchService.searchRooms("前端", 1, 1, 12);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        SearchResultVO item = result.getList().get(0);
        assertEquals(10, item.getId());
        assertEquals("前端开发直播", item.getTitle());
        assertEquals("小明", item.getUserInfo().getName());
        assertEquals("技术", item.getCategoryInfo().getName());

        ArgumentCaptor<Wrapper<Room>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(roomMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        assertNotNull(wrapperCaptor.getValue());
    }

    @Test
    @DisplayName("选择禁用分类时应直接返回空结果")
    void shouldReturnEmptyWhenCategoryDisabled() {
        SearchServiceImpl searchService = new SearchServiceImpl(roomMapper, userMapper, categoryMapper);
        when(categoryMapper.selectById(2)).thenReturn(createCategory(2, "禁用", StatusEnum.NO.getCode()));

        PageData<SearchResultVO> result = searchService.searchRooms("直播", 2, 1, 12);

        assertEquals(0, result.getTotal());
        assertEquals(0, result.getList().size());
        verify(roomMapper, never()).selectPage(any(Page.class), any(Wrapper.class));
    }

    private Category createCategory(Integer id, String name, Integer status) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setStatus(status);
        return category;
    }

    private Room createRoom(Integer id, String title, Integer userId, Integer categoryId) {
        Room room = new Room();
        room.setId(id);
        room.setTitle(title);
        room.setCover("/cover.png");
        room.setUserId(userId);
        room.setCategoryId(categoryId);
        room.setStatus(LiveRoomStatusEnum.LIVING.getCode());
        room.setDisabled(StatusEnum.YES.getCode());
        return room;
    }

    private User createUser(Integer id, String nickname) {
        User user = new User();
        user.setId(id);
        user.setNickname(nickname);
        user.setAvatar("/avatar.png");
        return user;
    }
}
