package cn.imhtb.live.modules.live.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定期清理已读的旧通知，防止 notification 表无限增长。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupTask {

    private final INotificationService notificationService;

    private static final int RETENTION_DAYS = 90;

    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨 3 点
    public void cleanOldNotifications() {
        try {
            int deleted = notificationService.cleanOldRead(RETENTION_DAYS);
            if (deleted > 0) {
                log.info("清理旧通知完成: 删除 {} 条（{} 天前已读）", deleted, RETENTION_DAYS);
            }
        } catch (Exception e) {
            log.error("清理旧通知失败", e);
        }
    }
}
