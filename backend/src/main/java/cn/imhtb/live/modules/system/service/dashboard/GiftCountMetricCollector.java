package cn.imhtb.live.modules.system.service.dashboard;

import cn.imhtb.live.mappers.PresentMapper;
import cn.imhtb.live.modules.system.model.SystemDashboardMetric;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(40)
@RequiredArgsConstructor
public class GiftCountMetricCollector implements SystemDashboardMetricCollector {

    private final PresentMapper presentMapper;

    @Override
    public SystemDashboardMetric collect() {
        SystemDashboardMetric metric = new SystemDashboardMetric();
        metric.setCode("giftTotal");
        metric.setLabel("礼物配置");
        metric.setValue(presentMapper.selectCount(null));
        metric.setDescription("系统中可配置的礼物资源");
        return metric;
    }
}
