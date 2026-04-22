package cn.imhtb.live.modules.system.service.impl;

import cn.imhtb.live.modules.system.model.SystemDashboardResp;
import cn.imhtb.live.modules.system.service.ISystemDashboardService;
import cn.imhtb.live.modules.system.service.dashboard.SystemDashboardMetricCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SystemDashboardServiceImpl implements ISystemDashboardService {

    private final List<SystemDashboardMetricCollector> metricCollectors;

    @Override
    public SystemDashboardResp getSummary() {
        SystemDashboardResp resp = new SystemDashboardResp();
        resp.setMetrics(metricCollectors.stream()
                .map(collector -> {
                    try {
                        return collector.collect();
                    } catch (Exception exception) {
                        log.warn("dashboard metric collector failed, collector = {}", collector.getClass().getName(), exception);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList()));
        return resp;
    }
}
