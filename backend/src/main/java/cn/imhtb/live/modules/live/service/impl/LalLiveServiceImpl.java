package cn.imhtb.live.modules.live.service.impl;

import cn.hutool.crypto.digest.MD5;
import cn.imhtb.live.common.config.LalLiveConfig;
import cn.imhtb.live.common.enums.LiveInfoStatusEnum;
import cn.imhtb.live.common.enums.LiveRoomStatusEnum;
import cn.imhtb.live.common.enums.PresentRewardTypeEnum;
import cn.imhtb.live.common.utils.DbSchemaInspector;
import cn.imhtb.live.common.utils.RedisUtil;
import cn.imhtb.live.mappers.MessageMapper;
import cn.imhtb.live.mappers.PresentRewardMapper;
import cn.imhtb.live.modules.live.service.ILiveInfoService;
import cn.imhtb.live.modules.live.service.ILiveService;
import cn.imhtb.live.modules.live.vo.StopLiveStatsVo;
import cn.imhtb.live.modules.server.RedisPrefix;
import cn.imhtb.live.pojo.LiveStatusVo;
import cn.imhtb.live.pojo.StartOpenLiveVo;
import cn.imhtb.live.pojo.database.LiveInfo;
import cn.imhtb.live.pojo.database.Message;
import cn.imhtb.live.pojo.database.PresentReward;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.service.IRoomService;
import cn.imhtb.live.service.ITokenService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * lal live service
 *
 * @author pinteh
 * @since 2022/06/13
 */
@Slf4j
@Service("LalLiveService")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class LalLiveServiceImpl implements ILiveService {

    private final IRoomService roomService;
    private final ITokenService tokenService;
    private final LalLiveConfig lalLiveConfig;
    private final ILiveInfoService liveInfoService;
    private final MessageMapper messageMapper;
    private final PresentRewardMapper presentRewardMapper;
    private final RedisUtil redisUtil;
    private final DbSchemaInspector dbSchemaInspector;

    @Override
    public String getName() {
        return "lal";
    }

    @Override
    public StartOpenLiveVo applySecret() {
        Integer userId = tokenService.getUserId();
        roomService.validateReadyForLive(userId);
        Room room = roomService.getOrInitRoomByUserId(userId);
        if (Objects.isNull(room)) {
            throw new RuntimeException("未获取到直播间信息");
        }

        String digestStr = lalLiveConfig.getSecret() + room.getId();
        String digestStrHex = MD5.create().digestHex(digestStr);
        String pushSecret = String.format("%d?lal_secret=%s", room.getId(), digestStrHex);

        room.setSecret(pushSecret);
        room.setPushUrl(lalLiveConfig.getRtmpPushStream());
        room.setPullUrl(lalLiveConfig.getFlvPullStream() + room.getId() + ".flv");
        roomService.updateById(room);
        redisUtil.remove(getLiveViewerKey(room.getId()));

        StartOpenLiveVo startOpenLiveVo = new StartOpenLiveVo();
        startOpenLiveVo.setPushUrl(lalLiveConfig.getRtmpPushStream());
        startOpenLiveVo.setSecret(pushSecret);
        return startOpenLiveVo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public StopLiveStatsVo stopLive() {
        Integer userId = tokenService.getUserId();
        Room room = roomService.getOrInitRoomByUserId(userId);
        if (Objects.isNull(room) || room.getStatus() != LiveRoomStatusEnum.LIVING.getCode()) {
            log.info("stop live ignored, room is already stopped, userId = {}", userId);
            return buildEmptyStopLiveStats();
        }

        LiveInfo historyLiveInfo = getHistoryLiveInfo(room);
        if (Objects.isNull(historyLiveInfo)) {
            room.setStatus(LiveRoomStatusEnum.STOP.getCode());
            roomService.updateById(room);
            redisUtil.remove(getLiveViewerKey(room.getId()));
            log.info("stop live repaired room status without active live info, roomId = {}, userId = {}", room.getId(), userId);
            return buildEmptyStopLiveStats();
        }
        historyLiveInfo.setStatus(LiveInfoStatusEnum.FINISHED.getCode());
        historyLiveInfo.setEndTime(LocalDateTime.now());

        Long danMuCount = getMessageCount(room, historyLiveInfo);
        historyLiveInfo.setMessageCount(danMuCount);

        BigDecimal presentAmount = getPresentAmount(room, historyLiveInfo);
        historyLiveInfo.setPresentCount(presentAmount.longValue());

        Long totalViewCount = getLiveViewerCount(room.getId());
        historyLiveInfo.setClickCount(totalViewCount);
        liveInfoService.updateById(historyLiveInfo);

        room.setStatus(LiveRoomStatusEnum.STOP.getCode());
        roomService.updateById(room);
        redisUtil.remove(getLiveViewerKey(room.getId()));

        log.info("initiative stop live, roomId = {}, userId = {}", room.getId(), userId);
        return StopLiveStatsVo.builder()
                .presentAmount(presentAmount)
                .danMuCount(danMuCount)
                .totalViewCount(totalViewCount)
                .liveDurationSeconds(getLiveDurationSeconds(historyLiveInfo))
                .build();
    }

    @Override
    public LiveStatusVo getLiveStatus() {
        Integer userId = tokenService.getUserId();
        Room room = roomService.getOrInitRoomByUserId(userId);
        if (Objects.isNull(room)) {
            log.warn("room is unavailable for userId = {}, returning default live status", userId);
            return LiveStatusVo.builder()
                    .liveStatus(LiveRoomStatusEnum.STOP.getCode())
                    .livePushUrl(lalLiveConfig.getRtmpPushStream())
                    .build();
        }

        LiveStatusVo build = LiveStatusVo.builder()
                .liveStatus(room.getStatus())
                .livePushUrl(lalLiveConfig.getRtmpPushStream())
                .build();

        if (room.getStatus() == LiveRoomStatusEnum.LIVING.getCode()) {
            LiveInfo historyLiveInfo = getHistoryLiveInfo(room);
            if (historyLiveInfo != null && historyLiveInfo.getStartTime() != null) {
                build.setLivePushSecret(room.getSecret());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                build.setLiveStartTime(formatter.format(historyLiveInfo.getStartTime()));
            }
        }
        return build;
    }

    private LiveInfo getHistoryLiveInfo(Room room) {
        return liveInfoService.getOne(new LambdaQueryWrapper<LiveInfo>().eq(LiveInfo::getRoomId, room.getId())
                        .eq(LiveInfo::getStatus, LiveInfoStatusEnum.LIVING.getCode())
                        .orderByDesc(LiveInfo::getCreateTime)
                        .last("limit 1"),
                false);
    }

    private BigDecimal getPresentAmount(Room room, LiveInfo liveInfo) {
        List<PresentReward> rewards = presentRewardMapper.selectList(new LambdaQueryWrapper<PresentReward>()
                .eq(PresentReward::getRoomId, room.getId())
                .eq(PresentReward::getToId, room.getUserId())
                .and(wrapper -> wrapper.eq(PresentReward::getType, PresentRewardTypeEnum.LIVE.getCode())
                        .or()
                        .isNull(PresentReward::getType))
                .ge(PresentReward::getCreateTime, liveInfo.getStartTime())
                .le(PresentReward::getCreateTime, liveInfo.getEndTime()));
        return rewards.stream()
                .map(PresentReward::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Long getMessageCount(Room room, LiveInfo liveInfo) {
        if (!dbSchemaInspector.tableExists("message")) {
            return 0L;
        }
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>().eq(Message::getRoomId, room.getId())
                .ge(Message::getCreateTime, liveInfo.getStartTime())
                .le(Message::getCreateTime, liveInfo.getEndTime());
        return messageMapper.selectCount(wrapper);
    }

    private Long getLiveViewerCount(Integer roomId) {
        Set<Object> viewers = redisUtil.setMembers(getLiveViewerKey(roomId));
        return Objects.isNull(viewers) ? 0L : (long) viewers.size();
    }

    private Long getLiveDurationSeconds(LiveInfo liveInfo) {
        if (Objects.isNull(liveInfo.getStartTime()) || Objects.isNull(liveInfo.getEndTime())) {
            return 0L;
        }
        return Math.max(Duration.between(liveInfo.getStartTime(), liveInfo.getEndTime()).getSeconds(), 0L);
    }

    private StopLiveStatsVo buildEmptyStopLiveStats() {
        return StopLiveStatsVo.builder()
                .presentAmount(BigDecimal.ZERO)
                .danMuCount(0L)
                .totalViewCount(0L)
                .liveDurationSeconds(0L)
                .build();
    }

    private String getLiveViewerKey(Integer roomId) {
        return String.format(RedisPrefix.LIVE_VIEWER_SET_KEY, roomId);
    }
}
