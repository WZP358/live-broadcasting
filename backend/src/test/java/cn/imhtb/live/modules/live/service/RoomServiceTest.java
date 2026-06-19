package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.enums.LiveRoomStatusEnum;
import cn.imhtb.live.common.enums.StatusEnum;
import cn.imhtb.live.common.exception.BusinessException;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.mappers.UserMapper;
import cn.imhtb.live.modules.live.service.impl.CategoryServiceImpl;
import cn.imhtb.live.modules.live.vo.RoomRespVo;
import cn.imhtb.live.pojo.database.Category;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.pojo.database.User;
import cn.imhtb.live.service.ITokenService;
import cn.imhtb.live.service.IWatchService;
import cn.imhtb.live.service.impl.RoomServiceImpl;
import cn.imhtb.live.modules.live.service.ILiveInfoService;
import cn.imhtb.live.common.config.LalLiveConfig;
import cn.imhtb.live.modules.live.webrtc.BrowserLiveRegistry;
import cn.imhtb.live.modules.server.netty.live.NettyBrowserLiveService;
import cn.imhtb.live.pojo.vo.request.RoomInfoSaveRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomService 直播间服务")
class RoomServiceTest {

    @Mock
    private RoomMapper roomMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ITokenService tokenService;
    @Mock
    private IWatchService watchService;
    @Mock
    private CategoryServiceImpl categoryService;
    @Mock
    private ILiveInfoService liveInfoService;
    @Mock
    private LalLiveConfig lalLiveConfig;
    @Mock
    private BrowserLiveRegistry browserLiveRegistry;
    @Mock
    private NettyBrowserLiveService nettyBrowserLiveService;

    @InjectMocks
    private RoomServiceImpl roomService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(roomService, "baseMapper", roomMapper);
    }

    // ─── Story: 获取正在直播房间列表 ──────────────────────

    @Nested
    @DisplayName("Story: 游客浏览正在直播的房间列表")
    class GetLivingRooms {

        @Test
        @DisplayName("Given: 有5个直播房间, When: 查询第1页每页2条, Then: 返回2条数据, total=5")
        void shouldReturnPaginatedLivingRooms() {
            List<Room> livingRooms = Arrays.asList(
                    buildRoom(1, "房间A", 1),
                    buildRoom(2, "房间B", 1),
                    buildRoom(3, "房间C", 1),
                    buildRoom(4, "房间D", 1),
                    buildRoom(5, "房间E", 1)
            );

            when(roomMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(livingRooms);
            when(userMapper.selectById(anyInt())).thenReturn(createUser(1, "测试用户"));
            when(categoryService.getById(anyInt())).thenReturn(createCategory(1, "游戏"));
            when(browserLiveRegistry.isBrowserLive(anyInt())).thenReturn(true);
            when(lalLiveConfig.getHlsPullStream()).thenReturn(null);

            PageData<RoomRespVo> result = roomService.getLivingRooms(null, 1, 2);

            assertNotNull(result);
            assertEquals(5, result.getTotal());
            assertEquals(2, result.getList().size());
            assertEquals(1, result.getList().get(0).getId());
            assertEquals(2, result.getList().get(1).getId());
        }

        @Test
        @DisplayName("Given: 没有在直播的房间, When: 查询, Then: 返回空列表")
        void shouldReturnEmptyWhenNoLivingRooms() {
            when(roomMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList());

            PageData<RoomRespVo> result = roomService.getLivingRooms(null, 1, 10);

            assertNotNull(result);
            assertEquals(0, result.getTotal());
            assertTrue(result.getList().isEmpty());
        }

        @Test
        @DisplayName("When: pageNo 为 null, Then: 默认使用第1页")
        void shouldDefaultToFirstPageWhenPageNoIsNull() {
            List<Room> rooms = Arrays.asList(buildRoom(1, "房间A", 1));
            when(roomMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(rooms);
            when(userMapper.selectById(anyInt())).thenReturn(createUser(1, "主播1"));
            when(categoryService.getById(anyInt())).thenReturn(createCategory(1, "游戏"));
            when(browserLiveRegistry.isBrowserLive(anyInt())).thenReturn(true);
            when(lalLiveConfig.getHlsPullStream()).thenReturn(null);

            PageData<RoomRespVo> result = roomService.getLivingRooms(null, null, 10);

            assertNotNull(result);
            assertEquals(1, result.getList().size());
        }
    }

    // ─── Story: 直播间包装为响应对象 ──────────────────────

    @Nested
    @DisplayName("Story: 直播间详情包含主播和分类信息")
    class GetRoomInfo {

        @Test
        @DisplayName("Given: 房间存在, When: 查询详情, Then: 返回包含主播信息和分类信息的响应")
        void shouldReturnRoomWithUserAndCategoryInfo() {
            Room room = buildRoom(100, "测试直播间", 5);
            User user = createUser(5, "主播小红");
            Category category = createCategory(10, "娱乐");

            when(roomMapper.selectById(100)).thenReturn(room);
            when(userMapper.selectById(5)).thenReturn(user);
            when(categoryService.getById(10)).thenReturn(category);
            when(browserLiveRegistry.isBrowserLive(100)).thenReturn(false);
            when(nettyBrowserLiveService.isBrowserLive(100)).thenReturn(false);
            when(lalLiveConfig.getHlsPullStream()).thenReturn(null);

            RoomRespVo result = roomService.getRoomInfo(100);

            assertNotNull(result);
            assertEquals(100, result.getId());
            assertEquals("测试直播间", result.getTitle());
            assertNotNull(result.getUserInfo());
            assertEquals("主播小红", result.getUserInfo().getName());
            assertNotNull(result.getCategoryInfo());
            assertEquals("娱乐", result.getCategoryInfo().getName());
        }

        @Test
        @DisplayName("Given: 房间不存在, When: 查询详情, Then: 返回 null")
        void shouldReturnNullWhenRoomNotFound() {
            when(roomMapper.selectById(999)).thenReturn(null);

            RoomRespVo result = roomService.getRoomInfo(999);

            assertNull(result);
        }
    }

    // ─── Story: 开播资料分类校验 ─────────────────────────

    @Nested
    @DisplayName("Story: 主播保存开播资料时校验分类")
    class SaveRoomInfo {

        @Test
        @DisplayName("Given: 分类已启用, When: 保存资料, Then: 更新直播间")
        void shouldSaveRoomInfoWhenCategoryEnabled() {
            RoomInfoSaveRequest request = buildRoomInfoRequest(1);
            when(tokenService.getUserId()).thenReturn(7);
            when(roomMapper.selectOne(any(LambdaQueryWrapper.class), eq(false))).thenReturn(buildRoom(100, "旧标题", 7));
            when(categoryService.getById(1)).thenReturn(createCategory(1, "游戏", StatusEnum.YES.getCode()));
            when(roomMapper.updateById(any(Room.class))).thenReturn(1);

            boolean result = roomService.saveInfo(request);

            assertTrue(result);
            verify(roomMapper).updateById(argThat((Room room) ->
                    room.getId().equals(100)
                            && "新标题".equals(room.getTitle())
                            && room.getCategoryId().equals(1)));
        }

        @Test
        @DisplayName("Given: 分类已禁用, When: 保存资料, Then: 拒绝更新")
        void shouldRejectDisabledCategoryWhenSavingRoomInfo() {
            RoomInfoSaveRequest request = buildRoomInfoRequest(2);
            when(tokenService.getUserId()).thenReturn(7);
            when(roomMapper.selectOne(any(LambdaQueryWrapper.class), eq(false))).thenReturn(buildRoom(100, "旧标题", 7));
            when(categoryService.getById(2)).thenReturn(createCategory(2, "禁用分类", StatusEnum.NO.getCode()));

            boolean result = roomService.saveInfo(request);

            assertFalse(result);
            verify(roomMapper, never()).updateById(any(Room.class));
        }

        @Test
        @DisplayName("Given: 分类不存在, When: 保存资料, Then: 拒绝更新")
        void shouldRejectMissingCategoryWhenSavingRoomInfo() {
            RoomInfoSaveRequest request = buildRoomInfoRequest(999);
            when(tokenService.getUserId()).thenReturn(7);
            when(roomMapper.selectOne(any(LambdaQueryWrapper.class), eq(false))).thenReturn(buildRoom(100, "旧标题", 7));
            when(categoryService.getById(999)).thenReturn(null);

            boolean result = roomService.saveInfo(request);

            assertFalse(result);
            verify(roomMapper, never()).updateById(any(Room.class));
        }
    }

    @Nested
    @DisplayName("Story: 开播前校验直播间分类")
    class ValidateReadyForLive {

        @Test
        @DisplayName("Given: 房间有标题和启用分类, When: 开播校验, Then: 通过")
        void shouldPassWhenRoomHasEnabledCategory() {
            Room room = buildRoom(100, "新标题", 7);
            room.setCategoryId(1);
            when(roomMapper.selectOne(any(LambdaQueryWrapper.class), eq(false))).thenReturn(room);
            when(categoryService.getById(1)).thenReturn(createCategory(1, "游戏", StatusEnum.YES.getCode()));

            assertDoesNotThrow(() -> roomService.validateReadyForLive(7));
        }

        @Test
        @DisplayName("Given: 房间未选择分类, When: 开播校验, Then: 抛出业务异常")
        void shouldRejectLiveStartWhenCategoryMissing() {
            Room room = buildRoom(100, "新标题", 7);
            room.setCategoryId(null);
            when(roomMapper.selectOne(any(LambdaQueryWrapper.class), eq(false))).thenReturn(room);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> roomService.validateReadyForLive(7));
            assertEquals("请先选择可用的直播分类", exception.getMsg());
        }

        @Test
        @DisplayName("Given: 分类已禁用, When: 开播校验, Then: 抛出业务异常")
        void shouldRejectLiveStartWhenCategoryDisabled() {
            Room room = buildRoom(100, "新标题", 7);
            room.setCategoryId(2);
            when(roomMapper.selectOne(any(LambdaQueryWrapper.class), eq(false))).thenReturn(room);
            when(categoryService.getById(2)).thenReturn(createCategory(2, "禁用分类", StatusEnum.NO.getCode()));

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> roomService.validateReadyForLive(7));
            assertEquals("请先选择可用的直播分类", exception.getMsg());
        }
    }

    // ─── Story: 默认直播间标题 ────────────────────────────

    @Nested
    @DisplayName("Story: 新用户自动创建默认直播间")
    class DefaultRoomCreation {

        @Test
        @DisplayName("Given: 用户有昵称, When: 创建默认直播间, Then: 标题为「昵称的直播间」")
        void shouldUseNicknameForDefaultTitle() {
            User user = new User();
            user.setId(1);
            user.setNickname("小明");
            user.setUsername("xiaoming");

            when(userMapper.selectById(1)).thenReturn(user);
            when(roomMapper.insert(any(Room.class))).thenReturn(1);
            when(roomMapper.selectOne(any(LambdaQueryWrapper.class), eq(false))).thenReturn(null);

            Room result = roomService.getOrInitRoomByUserId(1);

            assertNotNull(result);
            assertEquals("小明的直播间", result.getTitle());
            assertEquals("这个主播还没有填写直播间简介", result.getIntroduce());
            assertEquals("欢迎来到直播间", result.getNotice());
            verify(roomMapper).insert(argThat((Room room) ->
                    room.getUserId().equals(1)
                            && "小明的直播间".equals(room.getTitle())
                            && room.getStatus().equals(LiveRoomStatusEnum.STOP.getCode())));
        }
    }

    // ─── Helpers ──────────────────────────────────────────

    private Room buildRoom(Integer id, String title, Integer userId) {
        Room room = new Room();
        room.setId(id);
        room.setTitle(title);
        room.setUserId(userId);
        room.setCategoryId(10);
        room.setStatus(LiveRoomStatusEnum.LIVING.getCode());
        room.setDisabled(StatusEnum.YES.getCode());
        return room;
    }

    private User createUser(Integer id, String nickname) {
        User user = new User();
        user.setId(id);
        user.setNickname(nickname);
        user.setUsername(nickname);
        return user;
    }

    private Category createCategory(Integer id, String name) {
        return createCategory(id, name, StatusEnum.YES.getCode());
    }

    private Category createCategory(Integer id, String name, Integer status) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setStatus(status);
        return category;
    }

    private RoomInfoSaveRequest buildRoomInfoRequest(Integer categoryId) {
        RoomInfoSaveRequest request = new RoomInfoSaveRequest();
        request.setTitle("新标题");
        request.setCover("/cover.png");
        request.setCid(categoryId);
        request.setNotice("公告");
        request.setIntroduce("简介");
        return request;
    }
}
