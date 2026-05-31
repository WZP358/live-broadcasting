package cn.imhtb.live.modules.live.service.impl;

import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.mappers.RoomModeratorMapper;
import cn.imhtb.live.modules.live.service.IRoomModeratorService;
import cn.imhtb.live.modules.server.netty.assembly.WsMsgAssembly;
import cn.imhtb.live.modules.server.netty.service.IRoomChatService;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.pojo.database.RoomModerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RoomModeratorServiceImpl extends ServiceImpl<RoomModeratorMapper, RoomModerator> implements IRoomModeratorService {

    private final RoomMapper roomMapper;
    private final IRoomChatService roomChatService;

    @Override
    public boolean appoint(Integer roomId, Integer userId, Integer appointedBy) {
        Room room = roomMapper.selectById(roomId);
        if (room == null || !Objects.equals(room.getUserId(), appointedBy)) {
            return false; // only room owner can appoint
        }
        if (Objects.equals(room.getUserId(), userId)) {
            return false; // owner can't be moderator of own room
        }
        Long count = lambdaQuery()
                .eq(RoomModerator::getRoomId, roomId)
                .eq(RoomModerator::getUserId, userId)
                .count();
        if (count > 0) {
            return false; // already a moderator
        }
        RoomModerator mod = new RoomModerator();
        mod.setRoomId(roomId);
        mod.setUserId(userId);
        mod.setAppointedBy(appointedBy);
        mod.setCreateTime(LocalDateTime.now());
        return save(mod);
    }

    @Override
    public boolean dismiss(Integer roomId, Integer userId, Integer dismissedBy) {
        Room room = roomMapper.selectById(roomId);
        if (room == null || !Objects.equals(room.getUserId(), dismissedBy)) {
            return false;
        }
        return lambdaUpdate()
                .eq(RoomModerator::getRoomId, roomId)
                .eq(RoomModerator::getUserId, userId)
                .remove();
    }

    @Override
    public List<RoomModerator> listByRoom(Integer roomId) {
        return lambdaQuery().eq(RoomModerator::getRoomId, roomId).list();
    }

    @Override
    public boolean isModerator(Integer roomId, Integer userId) {
        if (userId == null) return false;
        Room room = roomMapper.selectById(roomId);
        if (room != null && Objects.equals(room.getUserId(), userId)) {
            return true; // owner is always a moderator
        }
        return lambdaQuery()
                .eq(RoomModerator::getRoomId, roomId)
                .eq(RoomModerator::getUserId, userId)
                .count() > 0;
    }

    @Override
    public boolean muteUser(Integer roomId, Integer moderatorId, Integer targetUserId, Integer durationSeconds) {
        if (!isModerator(roomId, moderatorId)) {
            return false;
        }
        Room room = roomMapper.selectById(roomId);
        if (room != null && Objects.equals(room.getUserId(), targetUserId)) {
            return false; // can't mute room owner
        }
        roomChatService.muteUser(roomId, targetUserId, durationSeconds);
        return true;
    }

    @Override
    public boolean kickUser(Integer roomId, Integer moderatorId, Integer targetUserId) {
        if (!isModerator(roomId, moderatorId)) {
            return false;
        }
        Room room = roomMapper.selectById(roomId);
        if (room != null && Objects.equals(room.getUserId(), targetUserId)) {
            return false; // can't kick room owner
        }
        roomChatService.kickUser(roomId, targetUserId);
        return true;
    }
}
