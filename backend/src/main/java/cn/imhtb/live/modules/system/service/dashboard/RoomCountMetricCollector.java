package cn.imhtb.live.modules.system.service.dashboard;

import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.modules.system.model.SystemDashboardMetric;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Component
@Order(20)
@RequiredArgsConstructor
public class RoomCountMetricCollector implements SystemDashboardMetricCollector {

    private final RoomMapper roomMapper;

    @Override
    public SystemDashboardMetric collect() {
        SystemDashboardMetric metric = new SystemDashboardMetric();
        metric.setCode("roomTotal");
        metric.setLabel("直播间总数");
        metric.setValue(roomMapper.selectCount(null));
        metric.setDescription("已创建的直播间资源数量");
        return metric;
    }
}
