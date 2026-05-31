package cn.imhtb.live.modules.live.service.impl;

import cn.imhtb.live.mappers.UserLevelMapper;
import cn.imhtb.live.modules.live.service.IUserLevelService;
import cn.imhtb.live.pojo.database.UserLevel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserLevelServiceImpl extends ServiceImpl<UserLevelMapper, UserLevel> implements IUserLevelService {

    // 每级所需经验 = level^2 * 100
    private static long expForLevel(int level) {
        return (long) level * level * 100;
    }

    @Override
    public UserLevel getOrCreate(Integer userId) {
        UserLevel ul = getOne(new LambdaQueryWrapper<UserLevel>().eq(UserLevel::getUserId, userId));
        if (ul == null) {
            ul = new UserLevel();
            ul.setUserId(userId);
            ul.setExp(0L);
            ul.setLevel(1);
            ul.setUpdateTime(LocalDateTime.now());
            save(ul);
        }
        return ul;
    }

    @Override
    public void addExp(Integer userId, long exp) {
        UserLevel ul = getOrCreate(userId);
        ul.setExp(ul.getExp() + exp);
        int newLevel = getLevel(ul.getExp());
        ul.setLevel(newLevel);
        ul.setUpdateTime(LocalDateTime.now());
        updateById(ul);
    }

    @Override
    public int getLevel(long exp) {
        int level = 1;
        while (exp >= expForLevel(level + 1)) {
            level++;
            if (level >= 100) break;
        }
        return level;
    }

    @Override
    public int getExpToNextLevel(long exp) {
        int currentLevel = getLevel(exp);
        if (currentLevel >= 100) return 0;
        return (int) (expForLevel(currentLevel + 1) - exp);
    }
}
