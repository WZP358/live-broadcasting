package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.pojo.database.UserLevel;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IUserLevelService extends IService<UserLevel> {
    UserLevel getOrCreate(Integer userId);
    void addExp(Integer userId, long exp);
    int getLevel(long exp);
    int getExpToNextLevel(long exp);
}
