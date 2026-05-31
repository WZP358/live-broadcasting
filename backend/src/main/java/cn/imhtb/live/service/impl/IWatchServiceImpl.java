package cn.imhtb.live.service.impl;

import cn.imhtb.live.common.PageData;
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
import java.util.stream.Collectors;

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
        LambdaQueryWrapper<Watch> wrapper = new LambdaQueryWrapper<Watch>().eq(Watch::getUserId, userId)
                .eq(Watch::getWatchType, type)
                .orderByDesc(Watch::getId);
        Page<Watch> watchPage = page(new Page<>(page, limit), wrapper);
        PageData<WatchResponse> pageData = new PageData<>();
        pageData.setTotal(watchPage.getTotal());
        pageData.setList(packageWatch(watchPage.getRecords()));
        return pageData;
    }

    @Override
    public Boolean saveHistory(Integer userId, Integer roomId) {
        Long count = lambdaQuery().eq(Watch::getUserId, userId)
                .eq(Watch::getRoomId, roomId)
                .eq(Watch::getWatchType, WatchTypeEnum.HISTORY.getCode())
                .count();
        if (count != 0) {
            return false;
        }
        Watch watch = new Watch();
        watch.setUserId(userId);
        watch.setRoomId(roomId);
        watch.setWatchType(WatchTypeEnum.HISTORY.getCode());
        return save(watch);
    }

    @Override
    public Boolean follow(Integer userId, Integer roomId) {
        Long count = lambdaQuery().eq(Watch::getUserId, userId)
                .eq(Watch::getRoomId, roomId)
                .eq(Watch::getWatchType, WatchTypeEnum.FOLLOW.getCode())
                .count();
        if (count != 0) {
            return false;
        }
        Watch watch = new Watch();
        watch.setUserId(userId);
        watch.setRoomId(roomId);
        watch.setWatchType(WatchTypeEnum.FOLLOW.getCode());
        boolean saved = save(watch);
        if (saved) {
            Room room = roomMapper.selectById(roomId);
            if (room != null) {
                eventBus.publish(new FollowedEvent(roomId, room.getUserId(), userId));
            }
        }
        return saved;
    }

    @Override
    public Boolean unFollow(Integer userId, Integer roomId) {
        return lambdaUpdate().eq(Watch::getUserId, userId).eq(Watch::getRoomId, roomId).remove();
    }

    @Override
    public Boolean clearHistory(Integer userId) {
        return remove(new LambdaQueryWrapper<Watch>()
                .eq(Watch::getUserId, userId)
                .eq(Watch::getWatchType, WatchTypeEnum.HISTORY.getCode()));
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
            response.setName(user != null && user.getNickname() != null ? user.getNickname() : "主播");
            response.setTitle(room.getTitle());
            response.setRoomId(room.getId());
            response.setLiveStatus(room.getStatus());
            list.add(response);
        }
        return list;
    }

}
