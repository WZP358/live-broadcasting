package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.pojo.database.Notification;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface INotificationService extends IService<Notification> {

    PageData<Notification> listByUser(Integer userId, String type, Integer page, Integer limit);

    Long countUnread(Integer userId);

    Boolean markRead(Integer notificationId, Integer userId);

    Boolean markAllRead(Integer userId);

    void saveBatch(List<Notification> notifications);

    int cleanOldRead(int retentionDays);
}
