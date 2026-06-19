package cn.imhtb.live.modules.system.service.impl;

import cn.imhtb.live.common.enums.LiveInfoStatusEnum;
import cn.imhtb.live.common.enums.LiveRoomStatusEnum;
import cn.imhtb.live.common.enums.StatusEnum;
import cn.imhtb.live.mappers.CategoryMapper;
import cn.imhtb.live.mappers.LiveInfoMapper;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.mappers.UserMapper;
import cn.imhtb.live.modules.system.model.SystemDemoStatus;
import cn.imhtb.live.modules.system.service.ISystemDemoService;
import cn.imhtb.live.pojo.database.Category;
import cn.imhtb.live.pojo.database.LiveInfo;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.pojo.database.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SystemDemoServiceImpl implements ISystemDemoService {

    public static final String DEMO_ROOM_SECRET = "PULSELIVE_DEMO_ROOM";

    private static final String DEMO_USERNAME_PREFIX = "demo_anchor_";

    private static final String LEGACY_DEMO_ROOM_SECRET_PREFIX = "demo-";

    private static final String DEFAULT_PASSWORD = "$2a$10$puULYxVheVu/sJZk7rUbvujNheV9v7afPWETHv47sjS2KAXNptTEe";

    private static final List<DemoRoomSeed> ROOM_SEEDS = List.of(
            new DemoRoomSeed(
                    "pulse-tech",
                    "科技数码",
                    "AI 编程助手实战：从提示词到工作流",
                    "小脉实验室",
                    "/demo-covers/tech-lab.jpg",
                    "/demo-videos/tech-lab.mp4",
                    "演示模式录播房间，展示 AI 助手、弹幕分析和内容审核链路。"
            ),
            new DemoRoomSeed(
                    "pulse-game",
                    "游戏直播",
                    "峡谷高光复盘：团队配合与实时弹幕互动",
                    "夜航电竞",
                    "/demo-covers/game-arena.jpg",
                    "/demo-videos/game-arena.mp4",
                    "演示模式录播房间，适合展示分类推荐、聊天室和礼物动效。"
            ),
            new DemoRoomSeed(
                    "pulse-music",
                    "娱乐直播",
                    "晚间音乐会：轻松互动与礼物打赏演示",
                    "星河电台",
                    "/demo-covers/music-room.jpg",
                    "/demo-videos/music-room.mp4",
                    "演示模式录播房间，适合展示观看页布局、送礼和关注能力。"
            ),
            new DemoRoomSeed(
                    "pulse-life",
                    "生活分享",
                    "城市漫游：边走边聊的沉浸式直播",
                    "城市观察员",
                    "/demo-covers/city-walk.jpg",
                    "/demo-videos/city-walk.mp4",
                    "演示模式录播房间，适合展示多房间同时在线和搜索能力。"
            ),
            new DemoRoomSeed(
                    "pulse-study",
                    "知识课堂",
                    "课程答辩演示：直播平台业务闭环讲解",
                    "答辩助教",
                    "/demo-covers/study-room.jpg",
                    "/demo-videos/study-room.mp4",
                    "演示模式录播房间，用于快速呈现项目亮点和课程加分点。"
            ),
            new DemoRoomSeed(
                    "pulse-ai-news",
                    "科技数码",
                    "科技早报：大模型工具链速览",
                    "模型观察员",
                    "/demo-covers/tech-lab.jpg",
                    "/demo-videos/tech-lab.mp4",
                    "演示模式录播房间，用于展示首页推荐、搜索和 AI 话题互动。"
            ),
            new DemoRoomSeed(
                    "pulse-code-review",
                    "科技数码",
                    "代码走查直播：推荐算法调优",
                    "代码巡航员",
                    "/demo-covers/tech-lab.jpg",
                    "/demo-videos/tech-lab.mp4",
                    "演示模式录播房间，适合讲解推荐算法和后台数据联动。"
            ),
            new DemoRoomSeed(
                    "pulse-mobile-game",
                    "游戏直播",
                    "手游战术板：团战复盘与弹幕提问",
                    "阿灯教练",
                    "/demo-covers/game-arena.jpg",
                    "/demo-videos/game-arena.mp4",
                    "演示模式录播房间，展示游戏分区、实时聊天室和连麦入口。"
            ),
            new DemoRoomSeed(
                    "pulse-vocal",
                    "娱乐直播",
                    "轻音乐点歌台：弹幕点歌互动",
                    "云间歌单",
                    "/demo-covers/music-room.jpg",
                    "/demo-videos/music-room.mp4",
                    "演示模式录播房间，展示礼物打赏和直播间氛围能力。"
            ),
            new DemoRoomSeed(
                    "pulse-food",
                    "生活分享",
                    "深夜厨房：家常菜答疑和陪伴聊天",
                    "小城厨房",
                    "/demo-covers/city-walk.jpg",
                    "/demo-videos/city-walk.mp4",
                    "演示模式录播房间，用于展示生活分区和长时间在线场景。"
            ),
            new DemoRoomSeed(
                    "pulse-product",
                    "科技数码",
                    "数码新品闲聊：手机与耳机怎么选",
                    "数码圆桌",
                    "/demo-covers/tech-lab.jpg",
                    "/demo-videos/tech-lab.mp4",
                    "演示模式录播房间，适合展示分类筛选、关注和相关推荐。"
            ),
            new DemoRoomSeed(
                    "pulse-exam",
                    "知识课堂",
                    "自习室陪伴：答辩材料最后检查",
                    "自习室班长",
                    "/demo-covers/study-room.jpg",
                    "/demo-videos/study-room.mp4",
                    "演示模式录播房间，适合展示课程项目答辩主线。"
            )
    );

    private final UserMapper userMapper;
    private final RoomMapper roomMapper;
    private final CategoryMapper categoryMapper;
    private final LiveInfoMapper liveInfoMapper;

    @Override
    public SystemDemoStatus status() {
        return buildStatus();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemDemoStatus enable() {
        stopRooms(listLegacyDemoRooms());
        for (DemoRoomSeed seed : ROOM_SEEDS) {
            Category category = ensureCategory(seed.getCategoryName());
            User user = ensureUser(seed);
            Room room = ensureRoom(seed, user, category);
            startDemoRoom(room, user);
        }
        return buildStatus();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemDemoStatus disable() {
        List<Room> rooms = new ArrayList<>(listDemoRooms());
        rooms.addAll(listLegacyDemoRooms());
        stopRooms(rooms);
        return buildStatus();
    }

    private void stopRooms(List<Room> rooms) {
        LocalDateTime now = LocalDateTime.now();
        for (Room room : rooms) {
            Room updateRoom = new Room();
            updateRoom.setId(room.getId());
            updateRoom.setStatus(LiveRoomStatusEnum.STOP.getCode());
            updateRoom.setUpdateTime(now);
            roomMapper.updateById(updateRoom);

            List<LiveInfo> livingInfos = liveInfoMapper.selectList(new LambdaQueryWrapper<LiveInfo>()
                    .eq(LiveInfo::getRoomId, room.getId())
                    .eq(LiveInfo::getStatus, LiveInfoStatusEnum.LIVING.getCode()));
            for (LiveInfo liveInfo : livingInfos) {
                liveInfo.setStatus(LiveInfoStatusEnum.FINISHED.getCode());
                liveInfo.setEndTime(now);
                liveInfoMapper.updateById(liveInfo);
            }
        }
    }

    private Category ensureCategory(String categoryName) {
        Category exists = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getName, categoryName)
                .last("limit 1"));
        if (exists != null) {
            if (!Objects.equals(exists.getStatus(), StatusEnum.YES.getCode())) {
                exists.setStatus(StatusEnum.YES.getCode());
                categoryMapper.updateById(exists);
            }
            return exists;
        }

        Category category = new Category();
        category.setName(categoryName);
        category.setSort(10);
        category.setStatus(StatusEnum.YES.getCode());
        category.setIsDeleted(0);
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.insert(category);
        return category;
    }

    private User ensureUser(DemoRoomSeed seed) {
        String username = DEMO_USERNAME_PREFIX + seed.getKey();
        User exists = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("limit 1"));
        if (exists != null) {
            User update = new User();
            update.setId(exists.getId());
            update.setNickname(seed.getAnchorName());
            update.setAvatar(seed.getCover());
            update.setSignature("演示模式主播账号，可由管理员一键开启或关闭。");
            update.setDisabled(StatusEnum.YES.getCode());
            userMapper.updateById(update);
            return userMapper.selectById(exists.getId());
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(DEFAULT_PASSWORD);
        user.setEmail(username + "@pulselive.demo");
        user.setAvatar(seed.getCover());
        user.setNickname(seed.getAnchorName());
        user.setSex("保密");
        user.setSignature("演示模式主播账号，可由管理员一键开启或关闭。");
        user.setRoleId(100);
        user.setDisabled(StatusEnum.YES.getCode());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    private Room ensureRoom(DemoRoomSeed seed, User user, Category category) {
        Room exists = roomMapper.selectOne(new LambdaQueryWrapper<Room>()
                .eq(Room::getUserId, user.getId())
                .eq(Room::getSecret, DEMO_ROOM_SECRET)
                .last("limit 1"));

        Room room = exists == null ? new Room() : exists;
        room.setUserId(user.getId());
        room.setTitle(seed.getTitle());
        room.setCover(seed.getCover());
        room.setSecret(DEMO_ROOM_SECRET);
        room.setIntroduce(seed.getIntroduce());
        room.setNotice("当前为管理员演示模式录播房间，关闭演示模式后会自动下线。");
        room.setRtmpUrl(seed.getPlayUrl());
        room.setDisabled(StatusEnum.YES.getCode());
        room.setStatus(LiveRoomStatusEnum.LIVING.getCode());
        room.setCategoryId(category.getId());
        room.setUpdateTime(LocalDateTime.now());
        if (exists == null) {
            room.setCreateTime(LocalDateTime.now());
            roomMapper.insert(room);
            return room;
        }
        roomMapper.updateById(room);
        return roomMapper.selectById(room.getId());
    }

    private void startDemoRoom(Room room, User user) {
        LiveInfo livingInfo = liveInfoMapper.selectOne(new LambdaQueryWrapper<LiveInfo>()
                .eq(LiveInfo::getRoomId, room.getId())
                .eq(LiveInfo::getStatus, LiveInfoStatusEnum.LIVING.getCode())
                .last("limit 1"));
        if (livingInfo != null) {
            return;
        }

        LiveInfo liveInfo = new LiveInfo();
        liveInfo.setRoomId(room.getId());
        liveInfo.setUserId(user.getId());
        liveInfo.setStatus(LiveInfoStatusEnum.LIVING.getCode());
        liveInfo.setStartTime(LocalDateTime.now());
        liveInfo.setCreateTime(LocalDateTime.now());
        liveInfo.setUpdateTime(LocalDateTime.now());
        liveInfo.setClickCount(3000L + room.getId());
        liveInfo.setMessageCount(200L + room.getId());
        liveInfo.setPresentCount(30L + room.getId());
        liveInfoMapper.insert(liveInfo);
    }

    private SystemDemoStatus buildStatus() {
        List<Room> rooms = listDemoRooms();
        SystemDemoStatus status = new SystemDemoStatus();
        status.setRoomCount(rooms.size());
        status.setLivingCount((int) rooms.stream()
                .filter(room -> Objects.equals(room.getStatus(), LiveRoomStatusEnum.LIVING.getCode()))
                .count());
        status.setEnabled(status.getRoomCount() >= ROOM_SEEDS.size() && status.getRoomCount() == status.getLivingCount());

        List<SystemDemoStatus.DemoRoomItem> items = new ArrayList<>();
        for (Room room : rooms) {
            User user = room.getUserId() == null ? null : userMapper.selectById(room.getUserId());
            Category category = room.getCategoryId() == null ? null : categoryMapper.selectById(room.getCategoryId());
            SystemDemoStatus.DemoRoomItem item = new SystemDemoStatus.DemoRoomItem();
            item.setId(room.getId());
            item.setUserId(room.getUserId());
            item.setTitle(room.getTitle());
            item.setAnchorName(user == null ? "演示主播" : user.getNickname());
            item.setCategoryName(category == null ? "演示分类" : category.getName());
            item.setCover(room.getCover());
            item.setPlayUrl(room.getRtmpUrl());
            item.setStatus(room.getStatus());
            items.add(item);
        }
        status.setRooms(items);
        return status;
    }

    private List<Room> listDemoRooms() {
        return roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getSecret, DEMO_ROOM_SECRET)
                .orderByAsc(Room::getId));
    }

    private List<Room> listLegacyDemoRooms() {
        List<User> demoUsers = userMapper.selectList(new LambdaQueryWrapper<User>()
                .likeRight(User::getUsername, DEMO_USERNAME_PREFIX));
        if (demoUsers.isEmpty()) {
            return List.of();
        }

        List<Integer> userIds = new ArrayList<>();
        for (User user : demoUsers) {
            if (user.getId() != null) {
                userIds.add(user.getId());
            }
        }
        if (userIds.isEmpty()) {
            return List.of();
        }

        return roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .in(Room::getUserId, userIds)
                .likeRight(Room::getSecret, LEGACY_DEMO_ROOM_SECRET_PREFIX)
                .orderByAsc(Room::getId));
    }

    public static boolean isDemoRoom(Room room) {
        return room != null && StringUtils.hasText(room.getSecret()) && DEMO_ROOM_SECRET.equals(room.getSecret());
    }

    @Data
    @AllArgsConstructor
    private static class DemoRoomSeed {
        private String key;
        private String categoryName;
        private String title;
        private String anchorName;
        private String cover;
        private String playUrl;
        private String introduce;
    }
}
