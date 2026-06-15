package cn.imhtb.live.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "pulselive.local-services")
public class LocalServiceProperties {

    private boolean enabled = true;

    private String projectRoot;

    private List<String> services = new ArrayList<>(Arrays.asList(
            "local-live",
            "maxine-denoise",
            "live-guard",
            "live-agent"
    ));

    private List<Integer> shutdownPorts = new ArrayList<>(Arrays.asList(
            10022,
            1935,
            8080,
            18765,
            8000,
            8100,
            8200
    ));
}
