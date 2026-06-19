package cn.imhtb.live.modules.system.service;

import cn.imhtb.live.common.utils.DbSchemaInspector;
import cn.imhtb.live.mappers.NotificationMapper;
import cn.imhtb.live.mappers.UserRoleMapper;
import cn.imhtb.live.modules.server.netty.assembly.WsMsgAssembly;
import cn.imhtb.live.modules.server.netty.domain.resp.NotificationRespDTO;
import cn.imhtb.live.modules.server.netty.service.IRoomChatService;
import cn.imhtb.live.pojo.database.Notification;
import cn.imhtb.live.pojo.database.UserRole;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SystemAdminNotificationService {

    private static final String TYPE_ROOM_BAN = "room_ban";
    private static final String TYPE_LIVE_GUARD_REVIEW = "live_guard_review";

    private final DbSchemaInspector dbSchemaInspector;
    private final UserRoleMapper userRoleMapper;
    private final NotificationMapper notificationMapper;
    private final IRoomChatService roomChatService;

    public SystemAdminNotificationService(DbSchemaInspector dbSchemaInspector,
                                          UserRoleMapper userRoleMapper,
                                          NotificationMapper notificationMapper,
                                          IRoomChatService roomChatService) {
        this.dbSchemaInspector = dbSchemaInspector;
        this.userRoleMapper = userRoleMapper;
        this.notificationMapper = notificationMapper;
        this.roomChatService = roomChatService;
    }

    public void notifyRoomBan(Integer roomId, String roomTitle, String content) {
        notifyAdmins(TYPE_ROOM_BAN, "直播间封禁状态变更", buildRoomContent(roomId, roomTitle, content), roomId);
    }

    public void notifyLiveGuardReview(Integer roomId, String roomTitle, String reason) {
        notifyAdmins(TYPE_LIVE_GUARD_REVIEW, "新的直播风控审核单", buildRoomContent(roomId, roomTitle, reason), roomId);
    }

    private void notifyAdmins(String type, String title, String content, Integer relatedId) {
        List<Integer> adminUserIds = listAdminUserIds();
        if (adminUserIds.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (Integer adminUserId : adminUserIds) {
            Notification notification = buildNotification(adminUserId, type, title, content, relatedId, now);
            if (dbSchemaInspector.tableExists("notification")) {
                notificationMapper.insert(notification);
            }

            NotificationRespDTO resp = new NotificationRespDTO();
            resp.setType(type);
            resp.setTitle(title);
            resp.setContent(content);
            resp.setRelatedId(relatedId);
            resp.setCreateTime(now);
            roomChatService.sendToUser(adminUserId, WsMsgAssembly.buildNotification(resp));
        }
    }

    private List<Integer> listAdminUserIds() {
        if (!dbSchemaInspector.tableExists("user_role")) {
            return Collections.emptyList();
        }
        return userRoleMapper.selectList(new QueryWrapper<UserRole>().in("role_id", 1, 2))
                .stream()
                .map(item -> item.getUserId())
                .distinct()
                .collect(Collectors.toList());
    }

    private Notification buildNotification(Integer userId,
                                           String type,
                                           String title,
                                           String content,
                                           Integer relatedId,
                                           LocalDateTime now) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(relatedId);
        notification.setIsRead(0);
        notification.setCreateTime(now);
        notification.setUpdateTime(now);
        return notification;
    }

    private String buildRoomContent(Integer roomId, String roomTitle, String content) {
        String title = roomTitle == null || roomTitle.trim().isEmpty() ? "未命名直播间" : roomTitle;
        return content + "：房间 " + roomId + "「" + title + "」";
    }
}
