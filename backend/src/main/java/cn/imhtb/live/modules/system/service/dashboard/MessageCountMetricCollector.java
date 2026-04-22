package cn.imhtb.live.modules.system.service.dashboard;

import cn.imhtb.live.common.utils.DbSchemaInspector;
import cn.imhtb.live.mappers.MessageMapper;
import cn.imhtb.live.modules.system.model.SystemDashboardMetric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Order(50)
@RequiredArgsConstructor
public class MessageCountMetricCollector implements SystemDashboardMetricCollector {

    private final MessageMapper messageMapper;
    private final DbSchemaInspector dbSchemaInspector;

    @Override
    public SystemDashboardMetric collect() {
        SystemDashboardMetric metric = new SystemDashboardMetric();
        metric.setCode("messageTotal");
        metric.setLabel("消息总量");
        if (!dbSchemaInspector.tableExists("message")) {
            log.warn("message table is unavailable in current schema, using zero for dashboard metric");
            metric.setValue(0L);
            metric.setDescription("消息表缺失，已降级展示为 0");
            return metric;
        }
        metric.setValue(messageMapper.selectCount(null));
        metric.setDescription("站内消息与互动消息累计数量");
        return metric;
    }
}
