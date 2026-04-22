package cn.imhtb.live.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Local live media server config.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "lal")
public class LalLiveConfig {

    private String secret;

    private String rtmpPushStream;

    private String flvPullStream;

    private String hlsPullStream;
}
