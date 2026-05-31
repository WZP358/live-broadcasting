package cn.imhtb.live.modules.live.service.impl;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.enums.LiveInfoStatusEnum;
import cn.imhtb.live.mappers.LiveReplayMapper;
import cn.imhtb.live.modules.live.service.ILiveReplayService;
import cn.imhtb.live.pojo.database.LiveInfo;
import cn.imhtb.live.pojo.database.LiveReplay;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.service.IRoomService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class LiveReplayServiceImpl extends ServiceImpl<LiveReplayMapper, LiveReplay> implements ILiveReplayService {

    @Lazy
    @Autowired
    private IRoomService roomService;

    private final cn.imhtb.live.modules.live.service.ILiveInfoService liveInfoService;

    @Override
    public void startRecording(Integer roomId, Integer liveInfoId) {
        Room room = roomService.getById(roomId);
        if (room == null) return;

        LiveReplay replay = new LiveReplay();
        replay.setRoomId(roomId);
        replay.setUserId(room.getUserId());
        replay.setLiveInfoId(liveInfoId);
        replay.setTitle(room.getTitle() + " 直播回放");
        replay.setCoverUrl(room.getCover());
        replay.setStatus(0); // 录制中
        replay.setStartTime(LocalDateTime.now());
        replay.setCreateTime(LocalDateTime.now());
        save(replay);
    }

    @Override
    public void stopRecording(Integer liveInfoId) {
        LiveInfo liveInfo = liveInfoService.getById(liveInfoId);
        if (liveInfo == null) return;

        LiveReplay replay = getOne(new LambdaQueryWrapper<LiveReplay>()
                .eq(LiveReplay::getLiveInfoId, liveInfoId)
                .eq(LiveReplay::getStatus, 0));
        if (replay == null) return;

        replay.setEndTime(LocalDateTime.now());
        long duration = Duration.between(replay.getStartTime(), replay.getEndTime()).getSeconds();
        replay.setDuration(Math.max(duration, 0));
        replay.setStatus(1); // 已就绪
        // 回放URL：LAL默认录制路径 /record/{streamKey}/{date}.flv
        String streamKey = liveInfo.getRoomId() + "_" + liveInfo.getUserId();
        replay.setReplayUrl("/live/record/" + streamKey + "/"
                + replay.getStartTime().toLocalDate() + ".flv");
        updateById(replay);
    }

    @Override
    public PageData<LiveReplay> listByRoom(Integer roomId, Integer page, Integer limit) {
        Page<LiveReplay> pg = page(new Page<>(page, limit),
                new LambdaQueryWrapper<LiveReplay>()
                        .eq(LiveReplay::getRoomId, roomId)
                        .eq(LiveReplay::getStatus, 1)
                        .orderByDesc(LiveReplay::getStartTime));
        PageData<LiveReplay> result = new PageData<>();
        result.setTotal(pg.getTotal());
        result.setList(pg.getRecords());
        return result;
    }

    @Override
    public LiveReplay getLatestByRoom(Integer roomId) {
        return getOne(new LambdaQueryWrapper<LiveReplay>()
                .eq(LiveReplay::getRoomId, roomId)
                .eq(LiveReplay::getStatus, 1)
                .orderByDesc(LiveReplay::getStartTime)
                .last("limit 1"));
    }
}
