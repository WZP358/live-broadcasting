package cn.imhtb.live.modules.live.guard;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 直播违规检测配置
 *
 * @author system
 */
@Data
@Component
@ConfigurationProperties(prefix = "guard")
public class GuardConfig {

    /**
     * 是否启用违规检测
     */
    private boolean enabled = true;

    /**
     * Guard 服务检测接口地址
     */
    private String endpoint = "http://localhost:8000/check";

    /**
     * 前端帧上报间隔（秒），也用于后端限流
     */
    private int intervalSeconds = 2;
}
