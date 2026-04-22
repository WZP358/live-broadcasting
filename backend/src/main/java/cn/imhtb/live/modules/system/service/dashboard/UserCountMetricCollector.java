package cn.imhtb.live.modules.system.service.dashboard;

import cn.imhtb.live.mappers.UserMapper;
import cn.imhtb.live.modules.system.model.SystemDashboardMetric;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Component
@Order(10)
@RequiredArgsConstructor
public class UserCountMetricCollector implements SystemDashboardMetricCollector {

    private final UserMapper userMapper;

    @Override
    public SystemDashboardMetric collect() {
        SystemDashboardMetric metric = new SystemDashboardMetric();
        metric.setCode("userTotal");
        metric.setLabel("用户总数");
        metric.setValue(userMapper.selectCount(null));
        metric.setDescription("平台当前累计注册用户");
        return metric;
    }
}
