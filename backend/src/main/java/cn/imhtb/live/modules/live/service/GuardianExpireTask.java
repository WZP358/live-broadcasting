package cn.imhtb.live.modules.live.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GuardianExpireTask {

    private final IGuardianService guardianService;

    @Scheduled(cron = "0 30 2 * * ?") // 每天凌晨2:30
    public void checkExpiredGuardians() {
        try {
            guardianService.checkExpired();
            log.info("守护过期检查完成");
        } catch (Exception e) {
            log.error("守护过期检查失败", e);
        }
    }
}
