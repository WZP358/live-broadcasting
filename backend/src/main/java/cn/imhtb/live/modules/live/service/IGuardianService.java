package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.pojo.database.GuardianSubscription;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface IGuardianService {
    /** 开通/续费守护 */
    void subscribe(Integer userId, Integer targetUserId, Integer level, boolean autoRenew);

    /** 取消自动续费 */
    void cancelAutoRenew(Integer userId, Integer targetUserId);

    /** 我的守护(我守护的主播) */
    Page<GuardianSubscription> myGuardians(Integer userId, int page, int limit);

    /** 守护我的粉丝 */
    Page<GuardianSubscription> myFans(Integer targetUserId, int page, int limit);

    /** 检查是否守护了该主播 */
    boolean isGuardian(Integer userId, Integer targetUserId);

    /** 获取守护等级 */
    Integer getGuardianLevel(Integer userId, Integer targetUserId);

    /** 守护价格配置 */
    int getMonthlyPrice(Integer level);

    /** 每日过期检查 */
    void checkExpired();
}
