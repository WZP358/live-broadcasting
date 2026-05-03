package cn.imhtb.live.service.impl;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.config.LalLiveConfig;
import cn.imhtb.live.common.enums.LiveInfoStatusEnum;
import cn.imhtb.live.common.enums.StatusEnum;
import cn.imhtb.live.common.enums.LiveRoomStatusEnum;
import cn.imhtb.live.common.enums.WatchTypeEnum;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.mappers.UserMapper;
import cn.imhtb.live.modules.live.service.ICategoryService;
import cn.imhtb.live.modules.live.service.ILiveInfoService;
import cn.imhtb.live.modules.live.vo.RoomRespVo;
import cn.imhtb.live.modules.live.webrtc.BrowserLiveRegistry;
import cn.imhtb.live.modules.server.netty.live.NettyBrowserLiveService;
import cn.imhtb.live.pojo.database.LiveInfo;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.pojo.database.User;
import cn.imhtb.live.pojo.database.Category;
import cn.imhtb.live.pojo.database.Watch;
import cn.imhtb.live.pojo.vo.RoomExtraInfoResp;
import cn.imhtb.live.pojo.vo.request.RoomInfoSaveRequest;
import cn.imhtb.live.service.IRoomService;
import cn.imhtb.live.service.ITokenService;
import cn.imhtb.live.service.IWatchService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * @author PinTeh
 */
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room> implements IRoomService {

    private final UserMapper userMapper;
    private final ITokenService tokenService;
    private final IWatchService watchService;
    private final ICategoryService categoryService;
    private final ILiveInfoService liveInfoService;
    private final LalLiveConfig lalLiveConfig;
    private final BrowserLiveRegistry browserLiveRegistry;
    private final NettyBrowserLiveService nettyBrowserLiveService;

    @Override
    public Room getOrInitRoomByUserId(Integer userId) {
        if (userId == null) {
            return null;
        }

        Room room = getOne(new LambdaQueryWrapper<Room>().eq(Room::getUserId, userId).last("limit 1"), false);
        if (room != null) {
            return room;
        }

        User user = userMapper.selectById(userId);
        Room newRoom = Room.builder()
                .userId(userId)
                .title(buildDefaultTitle(user))
                .introduce("这个主播还没有填写直播间简介")
                .notice("欢迎来到直播间")
                .cover(resolveDefaultCover(user))
                .disabled(StatusEnum.YES.getCode())
                .status(LiveRoomStatusEnum.STOP.getCode())
                .build();
        save(newRoom);
        return newRoom;
    }

    @Override
    public void updateCover(String coverUrl) {
        Room existsRoom = getOrInitRoomByUserId(UserHolder.getUserId());
        if (existsRoom == null) {
            return;
        }
        Room room = new Room();
        room.setId(existsRoom.getId());
        room.setCover(coverUrl);
        updateById(room);
    }

    @Override
    public boolean saveInfo(RoomInfoSaveRequest request) {
        Room room = getOrInitRoomByUserId(tokenService.getUserId());
        if (room == null) {
            return false;
        }
        Room updateRoom = new Room();
        updateRoom.setId(room.getId());
        updateRoom.setTitle(request.getTitle());
        updateRoom.setCover(request.getCover());
        updateRoom.setCategoryId(request.getCid());
        updateRoom.setNotice(request.getNotice());
        updateRoom.setIntroduce(request.getIntroduce());
        return updateById(updateRoom);
    }

    @Override
    public PageData<RoomRespVo> getLivingRooms(Integer cid, Integer pageNo, Integer pageSize) {
        int currentPage = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;

        List<Room> livingRooms = list(new LambdaQueryWrapper<Room>()
                .eq(cid != null, Room::getCategoryId, cid)
                .eq(Room::getStatus, LiveRoomStatusEnum.LIVING.getCode())
                .eq(Room::getDisabled, StatusEnum.YES.getCode()))
                .stream()
                .filter(this::isRoomReallyLiving)
                .collect(Collectors.toList());

        List<RoomRespVo> collect = livingRooms.stream()
                .skip((long) (currentPage - 1) * size)
                .limit(size)
                .map(this::packageRoomResponse)
                .collect(Collectors.toList());

        PageData<RoomRespVo> pageData = new PageData<>();
        pageData.setList(collect);
        pageData.setTotal((long) livingRooms.size());
        return pageData;
    }

    @Override
    public RoomRespVo getRoomInfo(Integer roomId) {
        Room room = getById(roomId);
        if (room == null){
            return null;
        }
        return this.packageRoomResponse(room);
    }

    @Override
    public RoomExtraInfoResp getExtraInfo(Integer userId, Integer rid) {
        RoomExtraInfoResp resp = new RoomExtraInfoResp();
        long count = watchService.count(new LambdaQueryWrapper<Watch>()
                .eq(Watch::getRoomId, rid)
                .eq(Watch::getUserId, userId)
                .eq(Watch::getWatchType, WatchTypeEnum.FOLLOW.getCode()));
        resp.setFollow(count > 0);
        return resp;
    }

    private RoomRespVo packageRoomResponse(Room room) {
        //TODO
        User user = userMapper.selectById(room.getUserId());
        Category category = categoryService.getById(room.getCategoryId());
        RoomRespVo response = new RoomRespVo();
        response.setId(room.getId());
        response.setTitle(room.getTitle());
        response.setPullUrl(resolvePullUrl(room));
        response.setBrowserLive(browserLiveRegistry.isBrowserLive(room.getId()) || nettyBrowserLiveService.isBrowserLive(room.getId()));
        response.setCover(room.getCover());
        response.setIntroduce(room.getIntroduce());
        if (user != null) {
            response.setUserInfo(new RoomRespVo.UserInfoVo(user.getId(), user.getNickname(), user.getAvatar()));
        }
        if (category != null) {
            response.setCategoryInfo(new RoomRespVo.CategoryInfoVo(category.getId(), category.getName()));
        }
        response.setStatus(room.getStatus());
        return response;
    }

    private String buildDefaultTitle(User user) {
        String nickname = user == null ? null : user.getNickname();
        String username = user == null ? null : user.getUsername();
        if (StringUtils.hasText(nickname)) {
            return nickname + "的直播间";
        }
        if (StringUtils.hasText(username)) {
            return username + "的直播间";
        }
        return "默认直播间";
    }

    private String resolveDefaultCover(User user) {
        if (user != null && StringUtils.hasText(user.getAvatar())) {
            return user.getAvatar();
        }
        return null;
    }

    private String resolvePullUrl(Room room) {
        if (StringUtils.hasText(room.getPullUrl())) {
            return room.getPullUrl();
        }
        if (room.getId() == null) {
            return null;
        }
        if (StringUtils.hasText(lalLiveConfig.getHlsPullStream())) {
            return lalLiveConfig.getHlsPullStream() + room.getId() + ".m3u8";
        }
        if (StringUtils.hasText(lalLiveConfig.getFlvPullStream())) {
            return lalLiveConfig.getFlvPullStream() + room.getId() + ".flv";
        }
        return null;
    }

    private boolean isRoomReallyLiving(Room room) {
        if (room == null || room.getId() == null) {
            return false;
        }
        if (browserLiveRegistry.isBrowserLive(room.getId()) || nettyBrowserLiveService.isBrowserLive(room.getId())) {
            return true;
        }

        LiveInfo liveInfo = liveInfoService.getOne(new LambdaQueryWrapper<LiveInfo>()
                        .eq(LiveInfo::getRoomId, room.getId())
                        .eq(LiveInfo::getStatus, LiveInfoStatusEnum.LIVING.getCode())
                        .orderByDesc(LiveInfo::getCreateTime)
                        .last("limit 1"),
                false);
        if (liveInfo == null || liveInfo.getStartTime() == null) {
            return false;
        }
        return liveInfo.getStartTime().isAfter(LocalDateTime.now().minusHours(12));
    }

}
