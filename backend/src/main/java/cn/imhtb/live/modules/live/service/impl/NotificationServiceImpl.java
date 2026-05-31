package cn.imhtb.live.modules.live.service.impl;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.mappers.NotificationMapper;
import cn.imhtb.live.modules.live.service.INotificationService;
import cn.imhtb.live.pojo.database.Notification;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements INotificationService {

    @Override
    public PageData<Notification> listByUser(Integer userId, String type, Integer page, Integer limit) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getId);
        if (type != null && !type.isEmpty()) {
            if (type.contains(",")) {
                wrapper.in(Notification::getType, (Object[]) type.split(","));
            } else {
                wrapper.eq(Notification::getType, type);
            }
        }
        Page<Notification> result = page(new Page<>(page, limit), wrapper);
        PageData<Notification> pageData = new PageData<>();
        pageData.setTotal(result.getTotal());
        pageData.setList(result.getRecords());
        return pageData;
    }

    @Override
    public Long countUnread(Integer userId) {
        return lambdaQuery()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .count();
    }

    @Override
    public Boolean markRead(Integer notificationId, Integer userId) {
        return lambdaUpdate()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1)
                .update();
    }

    @Override
    public Boolean markAllRead(Integer userId) {
        return lambdaUpdate()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1)
                .update();
    }

    @Override
    public void saveBatch(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }
        super.saveBatch(notifications, 200);
    }

    @Override
    public int cleanOldRead(int retentionDays) {
        return lambdaUpdate()
                .eq(Notification::getIsRead, 1)
                .lt(Notification::getCreateTime, java.time.LocalDateTime.now().minusDays(retentionDays))
                .remove() ? 1 : 0;
    }
}
