package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.common.enums.LiveInfoStatusEnum;
import cn.imhtb.live.common.enums.LiveRoomStatusEnum;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.modules.live.event.LiveEventBus;
import cn.imhtb.live.modules.live.event.LiveStartedEvent;
import cn.imhtb.live.modules.live.event.LiveStoppedEvent;
import cn.imhtb.live.pojo.database.LiveInfo;
import cn.imhtb.live.pojo.database.Room;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveLifecycleService {

    private final RoomMapper roomMapper;
    private final ILiveInfoService liveInfoService;
    private final LiveEventBus eventBus;
    private final ILiveReplayService replayService;

    @Transactional(rollbackFor = Exception.class)
    public void markLiveStarted(Integer roomId, Integer userId) {
        if (roomId == null) {
            return;
        }
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            log.warn("mark live started ignored, room not found, roomId={}", roomId);
            return;
        }

        Room updateRoom = new Room();
        updateRoom.setId(roomId);
        updateRoom.setStatus(LiveRoomStatusEnum.LIVING.getCode());
        roomMapper.updateById(updateRoom);

        LiveInfo liveInfo = getLivingInfo(roomId);
        if (liveInfo != null) {
            return;
        }

        LiveInfo newLiveInfo = new LiveInfo();
        newLiveInfo.setRoomId(roomId);
        newLiveInfo.setUserId(userId != null ? userId : room.getUserId());
        newLiveInfo.setStatus(LiveInfoStatusEnum.LIVING.getCode());
        newLiveInfo.setStartTime(LocalDateTime.now());
        liveInfoService.save(newLiveInfo);

        // 开始录制回放
        try {
            replayService.startRecording(roomId, newLiveInfo.getId());
        } catch (Exception e) {
            log.error("开始录制回放失败: roomId={}", roomId, e);
        }

        eventBus.publish(new LiveStartedEvent(roomId, newLiveInfo.getUserId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void markLiveStopped(Integer roomId) {
        if (roomId == null) {
            return;
        }

        LiveInfo liveInfo = getLivingInfo(roomId);
        if (liveInfo != null) {
            liveInfo.setStatus(LiveInfoStatusEnum.FINISHED.getCode());
            liveInfo.setEndTime(LocalDateTime.now());
            liveInfoService.updateById(liveInfo);

            // 停止录制，生成回放
            try {
                replayService.stopRecording(liveInfo.getId());
            } catch (Exception e) {
                log.error("停止录制回放失败: roomId={}", roomId, e);
            }
        }

        Room updateRoom = new Room();
        updateRoom.setId(roomId);
        updateRoom.setStatus(LiveRoomStatusEnum.STOP.getCode());
        roomMapper.updateById(updateRoom);

        eventBus.publish(new LiveStoppedEvent(roomId));
    }

    private LiveInfo getLivingInfo(Integer roomId) {
        return liveInfoService.getOne(new LambdaQueryWrapper<LiveInfo>()
                        .eq(LiveInfo::getRoomId, roomId)
                        .eq(LiveInfo::getStatus, LiveInfoStatusEnum.LIVING.getCode())
                        .orderByDesc(LiveInfo::getCreateTime)
                        .last("limit 1"),
                false);
    }
}
