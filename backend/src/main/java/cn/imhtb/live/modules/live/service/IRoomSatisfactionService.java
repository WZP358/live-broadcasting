package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.pojo.database.RoomSatisfaction;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IRoomSatisfactionService extends IService<RoomSatisfaction> {
    RoomSatisfaction submit(Integer roomId, Integer userId, Integer score);
}
