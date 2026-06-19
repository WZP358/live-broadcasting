package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.common.enums.LiveInfoStatusEnum;
import cn.imhtb.live.common.enums.LiveRoomStatusEnum;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.modules.system.service.impl.SystemDemoServiceImpl;
import cn.imhtb.live.modules.live.webrtc.BrowserLiveRegistry;
import cn.imhtb.live.modules.server.netty.live.NettyBrowserLiveRegistry;
import cn.imhtb.live.pojo.database.LiveInfo;
import cn.imhtb.live.pojo.database.Room;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时检测并修复异常卡在"直播中"状态的房间。
 * 场景：Netty 进程异常崩溃后，registry 内存数据丢失，
 * 但 Room.status 仍为 LIVING，导致首页假直播。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveStatusScheduledTask {

    private final RoomMapper roomMapper;
    private final ILiveInfoService liveInfoService;
    private final NettyBrowserLiveRegistry nettyBrowserLiveRegistry;
    private final BrowserLiveRegistry browserLiveRegistry;

    @Scheduled(fixedRate = 300_000) // 每 5 分钟
    public void fixStaleLivingRooms() {
        List<Room> livingRooms = roomMapper.selectList(
                new LambdaQueryWrapper<Room>()
                        .eq(Room::getStatus, LiveRoomStatusEnum.LIVING.getCode())
        );

        if (livingRooms.isEmpty()) {
            return;
        }

        int fixed = 0;
        for (Room room : livingRooms) {
            if (SystemDemoServiceImpl.isDemoRoom(room)) {
                continue;
            }
            Integer roomId = room.getId();
            // 检查是否在任一 browser live registry 中
            boolean nettyLive = nettyBrowserLiveRegistry.isBrowserLive(roomId);
            boolean wsLive = browserLiveRegistry.isBrowserLive(roomId);

            if (!nettyLive && !wsLive) {
                // 检查 LiveInfo 是否在 12 小时内有过有效开播
                LiveInfo liveInfo = liveInfoService.getOne(new LambdaQueryWrapper<LiveInfo>()
                        .eq(LiveInfo::getRoomId, roomId)
                        .eq(LiveInfo::getStatus, LiveInfoStatusEnum.LIVING.getCode())
                        .orderByDesc(LiveInfo::getCreateTime)
                        .last("limit 1"), false);

                boolean recentLive = liveInfo != null
                        && liveInfo.getStartTime() != null
                        && liveInfo.getStartTime().isAfter(LocalDateTime.now().minusHours(12));

                if (!recentLive) {
                    // 确认是僵尸状态，修复
                    room.setStatus(LiveRoomStatusEnum.STOP.getCode());
                    roomMapper.updateById(room);

                    if (liveInfo != null) {
                        liveInfo.setStatus(LiveInfoStatusEnum.FINISHED.getCode());
                        liveInfo.setEndTime(LocalDateTime.now());
                        liveInfoService.updateById(liveInfo);
                    }

                    fixed++;
                    log.warn("修复僵尸直播状态: roomId={}", roomId);
                }
            }
        }

        if (fixed > 0) {
            log.info("本轮修复僵尸直播房间: {} 个", fixed);
        }
    }
}
