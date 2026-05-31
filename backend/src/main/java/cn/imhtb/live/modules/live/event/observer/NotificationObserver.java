package cn.imhtb.live.modules.live.event.observer;

import cn.imhtb.live.modules.live.event.*;
import cn.imhtb.live.modules.live.service.INotificationService;
import cn.imhtb.live.modules.server.netty.assembly.WsMsgAssembly;
import cn.imhtb.live.modules.server.netty.domain.resp.NotificationRespDTO;
import cn.imhtb.live.modules.server.netty.service.IRoomChatService;
import cn.imhtb.live.modules.user.service.IUserService;
import cn.imhtb.live.pojo.database.Notification;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.pojo.database.User;
import cn.imhtb.live.pojo.database.Watch;
import cn.imhtb.live.service.IRoomService;
import cn.imhtb.live.service.IWatchService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class NotificationObserver implements LiveEventObserver {

    private final LiveEventBus eventBus;
    private final INotificationService notificationService;
    private final IRoomChatService roomChatService;
    private final IWatchService watchService;
    private final IRoomService roomService;
    private final IUserService userService;
    private final cn.imhtb.live.mappers.NotificationPrefMapper prefMapper;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Override
    public void onEvent(LiveEvent event) {
        switch (event.getEventType()) {
            case LiveStartedEvent.TYPE:
                handleLiveStarted((LiveStartedEvent) event);
                break;
            case LiveStoppedEvent.TYPE:
                handleLiveStopped((LiveStoppedEvent) event);
                break;
            case FollowedEvent.TYPE:
                handleFollowed((FollowedEvent) event);
                break;
            default:
                log.warn("Unknown event type: {}", event.getEventType());
        }
    }

    @Async("notificationExecutor")
    protected void handleLiveStarted(LiveStartedEvent event) {
        Integer roomId = event.getRoomId();
        List<Watch> followers = watchService.list(
                new LambdaQueryWrapper<Watch>()
                        .eq(Watch::getRoomId, roomId)
                        .eq(Watch::getWatchType, 1)
        );

        if (followers.isEmpty()) {
            return;
        }

        // 获取房间和主播信息，用于富文本通知
        Room room = roomService.getById(roomId);
        User streamer = room != null ? userService.getById(room.getUserId()) : null;
        String roomTitle = room != null ? room.getTitle() : ("房间" + roomId);
        String streamerName = streamer != null && streamer.getNickname() != null
                ? streamer.getNickname() : "主播";
        String cover = room != null && room.getCover() != null ? room.getCover() : "";

        String title = "您关注的主播开播了";
        String content = streamerName + " 的直播间「" + roomTitle + "」已开始直播，快去看看吧";
        LocalDateTime now = LocalDateTime.now();

        // 批量持久化
        List<Notification> batch = new ArrayList<>(followers.size());
        for (Watch follower : followers) {
            Notification notification = new Notification();
            notification.setUserId(follower.getUserId());
            notification.setType(LiveStartedEvent.TYPE);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setRelatedId(roomId);
            notification.setIsRead(0);
            notification.setCreateTime(now);
            notification.setUpdateTime(now);
            batch.add(notification);
        }
        notificationService.saveBatch(batch);

        // WebSocket 实时推送（尊重通知偏好）
        for (Watch follower : followers) {
            try {
                if (!shouldPushToUser(follower.getUserId(), LiveStartedEvent.TYPE)) {
                    continue;
                }
                NotificationRespDTO resp = new NotificationRespDTO();
                resp.setType(LiveStartedEvent.TYPE);
                resp.setTitle(title);
                resp.setContent(content);
                resp.setRelatedId(roomId);
                resp.setCreateTime(now);
                resp.setCover(cover);
                resp.setStreamerName(streamerName);
                resp.setRoomTitle(roomTitle);

                roomChatService.sendToUser(follower.getUserId(), WsMsgAssembly.buildNotification(resp));
            } catch (Exception e) {
                log.error("推送开播通知失败: userId={}, roomId={}", follower.getUserId(), roomId, e);
            }
        }
        log.info("开播通知已推送: roomId={}, followerCount={}", roomId, followers.size());
    }

    @Async("notificationExecutor")
    protected void handleLiveStopped(LiveStoppedEvent event) {
        log.info("直播已停止: roomId={}", event.getRoomId());
    }

    @Async("notificationExecutor")
    protected void handleFollowed(FollowedEvent event) {
        Integer followedUserId = event.getUserId();
        Integer followerUserId = event.getFollowerUserId();
        if (followedUserId == null || followerUserId == null) {
            return;
        }

        // 获取关注者昵称
        User follower = userService.getById(followerUserId);
        String followerName = follower != null && follower.getNickname() != null
                ? follower.getNickname() : ("用户" + followerUserId);

        String title = "您有新的粉丝";
        String content = followerName + " 关注了您的直播间";
        LocalDateTime now = LocalDateTime.now();

        Notification notification = new Notification();
        notification.setUserId(followedUserId);
        notification.setType(FollowedEvent.TYPE);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(followerUserId);
        notification.setIsRead(0);
        notification.setCreateTime(now);
        notification.setUpdateTime(now);
        notificationService.save(notification);

        try {
            if (shouldPushToUser(followedUserId, FollowedEvent.TYPE)) {
                NotificationRespDTO resp = new NotificationRespDTO();
                resp.setType(FollowedEvent.TYPE);
                resp.setTitle(title);
                resp.setContent(content);
                resp.setRelatedId(followerUserId);
                resp.setCreateTime(now);

                roomChatService.sendToUser(followedUserId, WsMsgAssembly.buildNotification(resp));
            }
        } catch (Exception e) {
            log.error("推送关注通知失败: followedUserId={}, followerUserId={}", followedUserId, followerUserId, e);
        }
    }

    private boolean shouldPushToUser(Integer userId, String type) {
        cn.imhtb.live.pojo.database.NotificationPref pref = prefMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<cn.imhtb.live.pojo.database.NotificationPref>()
                        .eq(cn.imhtb.live.pojo.database.NotificationPref::getUserId, userId));
        if (pref == null) return true; // no preference set, allow all

        // Check type toggle
        if (LiveStartedEvent.TYPE.equals(type) && Boolean.FALSE.equals(pref.getLiveStartEnabled())) return false;
        if (FollowedEvent.TYPE.equals(type) && Boolean.FALSE.equals(pref.getFollowEnabled())) return false;

        // Check DND window
        String dndStart = pref.getDndStart();
        String dndEnd = pref.getDndEnd();
        if (dndStart != null && dndEnd != null && !dndStart.isEmpty() && !dndEnd.isEmpty()) {
            java.time.LocalTime now = java.time.LocalTime.now();
            java.time.LocalTime start = java.time.LocalTime.parse(dndStart);
            java.time.LocalTime end = java.time.LocalTime.parse(dndEnd);
            if (start.isBefore(end)) {
                if (!now.isBefore(start) && !now.isAfter(end)) return false;
            } else {
                // spans midnight
                if (!now.isBefore(start) || !now.isAfter(end)) return false;
            }
        }
        return true;
    }
}
