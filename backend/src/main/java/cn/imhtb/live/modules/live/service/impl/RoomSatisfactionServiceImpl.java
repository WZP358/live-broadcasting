package cn.imhtb.live.modules.live.service.impl;

import cn.imhtb.live.common.exception.BusinessException;
import cn.imhtb.live.common.utils.DbSchemaInspector;
import cn.imhtb.live.mappers.RoomSatisfactionMapper;
import cn.imhtb.live.modules.live.service.IRoomSatisfactionService;
import cn.imhtb.live.pojo.database.RoomSatisfaction;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RoomSatisfactionServiceImpl extends ServiceImpl<RoomSatisfactionMapper, RoomSatisfaction>
        implements IRoomSatisfactionService {

    private final DbSchemaInspector dbSchemaInspector;

    @EventListener(ApplicationReadyEvent.class)
    public void initSchema() {
        dbSchemaInspector.executeQuietly("""
                CREATE TABLE IF NOT EXISTS `room_satisfaction` (
                  `id` INT AUTO_INCREMENT PRIMARY KEY,
                  `room_id` INT NOT NULL,
                  `user_id` INT NOT NULL,
                  `score` TINYINT NOT NULL,
                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                  UNIQUE KEY `uk_room_user` (`room_id`, `user_id`),
                  KEY `idx_room_score` (`room_id`, `score`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播间满意度评分'
                """);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoomSatisfaction submit(Integer roomId, Integer userId, Integer score) {
        if (roomId == null || userId == null) {
            throw new BusinessException("请先登录后再提交评分");
        }
        if (score == null || score < 1 || score > 5) {
            throw new BusinessException("评分只能是 1 到 5 星");
        }

        RoomSatisfaction record = getOne(new LambdaQueryWrapper<RoomSatisfaction>()
                        .eq(RoomSatisfaction::getRoomId, roomId)
                        .eq(RoomSatisfaction::getUserId, userId)
                        .last("limit 1"),
                false);
        if (record == null) {
            record = new RoomSatisfaction();
            record.setRoomId(roomId);
            record.setUserId(userId);
            record.setCreateTime(LocalDateTime.now());
        }
        record.setScore(score);
        saveOrUpdate(record);
        return record;
    }
}
