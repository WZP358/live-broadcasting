package cn.imhtb.live.service.impl;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.enums.StatusEnum;
import cn.imhtb.live.common.enums.WatchTypeEnum;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.mappers.UserMapper;
import cn.imhtb.live.mappers.WatchMapper;
import cn.imhtb.live.modules.live.event.FollowedEvent;
import cn.imhtb.live.modules.live.event.LiveEventBus;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.pojo.database.User;
import cn.imhtb.live.pojo.database.Watch;
import cn.imhtb.live.pojo.vo.response.WatchResponse;
import cn.imhtb.live.service.IWatchService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.time.LocalDateTime;

/**
 * @author PinTeh
 * @date 2020/3/18
 */
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class IWatchServiceImpl extends ServiceImpl<WatchMapper, Watch> implements IWatchService {

    private final RoomMapper roomMapper;
    private final UserMapper userMapper;
    private final LiveEventBus eventBus;

    @Override
    public PageData<WatchResponse> listWatches(Integer userId, Integer type, Integer limit, Integer page) {
        if (userId == null || !isSupportedWatchType(type)) {
            return new PageData<>(0L, new ArrayList<>());
        }
        int currentPage = page == null || page < 1 ? 1 : page;
        int pageSize = limit == null || limit < 1 ? 10 : limit;
        LambdaQueryWrapper<Watch> wrapper = new LambdaQueryWrapper<Watch>().eq(Watch::getUserId, userId)
                .eq(Watch::getWatchType, type)
                .orderByDesc(Watch::getUpdateTime)
                .orderByDesc(Watch::getId);
        Page<Watch> watchPage = baseMapper.selectPage(new Page<>(currentPage, pageSize), wrapper);
        PageData<WatchResponse> pageData = new PageData<>();
        pageData.setTotal(watchPage.getTotal());
        pageData.setList(packageWatch(watchPage.getRecords()));
        return pageData;
    }

    @Override
    public Boolean saveHistory(Integer userId, Integer roomId) {
        if (userId == null || roomId == null) {
            return false;
        }
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<Watch>().eq(Watch::getUserId, userId)
                .eq(Watch::getRoomId, roomId)
                .eq(Watch::getWatchType, WatchTypeEnum.HISTORY.getCode()));
        if (count != 0) {
            Watch update = new Watch();
            update.setUpdateTime(LocalDateTime.now());
            return baseMapper.update(update, new LambdaQueryWrapper<Watch>()
                    .eq(Watch::getUserId, userId)
                    .eq(Watch::getRoomId, roomId)
                    .eq(Watch::getWatchType, WatchTypeEnum.HISTORY.getCode())) > 0;
        }
        Watch watch = new Watch();
        watch.setUserId(userId);
        watch.setRoomId(roomId);
        watch.setWatchType(WatchTypeEnum.HISTORY.getCode());
        return baseMapper.insert(watch) > 0;
    }

    @Override
    public Boolean follow(Integer userId, Integer roomId) {
        if (userId == null || roomId == null) {
            return false;
        }
        Room room = roomMapper.selectById(roomId);
        if (room == null || Objects.equals(room.getUserId(), userId)) {
            return false;
        }
        if (Objects.equals(room.getDisabled(), StatusEnum.NO.getCode())) {
            return false;
        }
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<Watch>().eq(Watch::getUserId, userId)
                .eq(Watch::getRoomId, roomId)
                .eq(Watch::getWatchType, WatchTypeEnum.FOLLOW.getCode()));
        if (count != 0) {
            return false;
        }
        Watch watch = new Watch();
        watch.setUserId(userId);
        watch.setRoomId(roomId);
        watch.setWatchType(WatchTypeEnum.FOLLOW.getCode());
        boolean saved = baseMapper.insert(watch) > 0;
        if (saved) {
            eventBus.publish(new FollowedEvent(roomId, room.getUserId(), userId));
        }
        return saved;
    }

    @Override
    public Boolean unFollow(Integer userId, Integer roomId) {
        if (userId == null || roomId == null) {
            return false;
        }
        return baseMapper.delete(new LambdaQueryWrapper<Watch>()
                .eq(Watch::getUserId, userId)
                .eq(Watch::getRoomId, roomId)
                .eq(Watch::getWatchType, WatchTypeEnum.FOLLOW.getCode())) > 0;
    }

    @Override
    public Boolean clearHistory(Integer userId) {
        if (userId == null) {
            return false;
        }
        return baseMapper.delete(new LambdaQueryWrapper<Watch>()
                .eq(Watch::getUserId, userId)
                .eq(Watch::getWatchType, WatchTypeEnum.HISTORY.getCode())) > 0;
    }


    private List<WatchResponse> packageWatch(List<Watch> watches) {
        if (watches == null || watches.isEmpty()) {
            return new ArrayList<>();
        }
        List<WatchResponse> list = new ArrayList<>();
        for (Watch watch : watches) {
            Room room = roomMapper.selectById(watch.getRoomId());
            if (Objects.isNull(room)) {
                continue;
            }
            User user = userMapper.selectById(room.getUserId());
            WatchResponse response = new WatchResponse();
            response.setId(watch.getId());
            response.setCover(room.getCover());
            response.setAvatar(user != null ? user.getAvatar() : null);
            response.setAnchorUserId(room.getUserId());
            response.setName(user != null && user.getNickname() != null ? user.getNickname() : "主播");
            response.setTitle(room.getTitle());
            response.setRoomId(room.getId());
            response.setLiveStatus(room.getStatus());
            response.setWatchType(watch.getWatchType());
            response.setCreateTime(watch.getCreateTime());
            list.add(response);
        }
        return list;
    }

    private boolean isSupportedWatchType(Integer type) {
        return Objects.equals(type, WatchTypeEnum.HISTORY.getCode())
                || Objects.equals(type, WatchTypeEnum.FOLLOW.getCode());
    }

}
