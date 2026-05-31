package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.pojo.database.RoomModerator;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IRoomModeratorService extends IService<RoomModerator> {
    boolean appoint(Integer roomId, Integer userId, Integer appointedBy);
    boolean dismiss(Integer roomId, Integer userId, Integer dismissedBy);
    List<RoomModerator> listByRoom(Integer roomId);
    boolean isModerator(Integer roomId, Integer userId);
    boolean muteUser(Integer roomId, Integer moderatorId, Integer targetUserId, Integer durationSeconds);
    boolean kickUser(Integer roomId, Integer moderatorId, Integer targetUserId);
}
