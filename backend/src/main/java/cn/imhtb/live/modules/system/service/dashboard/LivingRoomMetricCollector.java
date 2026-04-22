package cn.imhtb.live.modules.system.service.dashboard;

import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.modules.system.model.SystemDashboardMetric;
import cn.imhtb.live.pojo.database.Room;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
@RequiredArgsConstructor
public class LivingRoomMetricCollector implements SystemDashboardMetricCollector {

    private final RoomMapper roomMapper;

    @Override
    public SystemDashboardMetric collect() {
        SystemDashboardMetric metric = new SystemDashboardMetric();
        metric.setCode("livingRoomTotal");
        metric.setLabel("当前开播");
        metric.setValue(roomMapper.selectCount(new LambdaQueryWrapper<Room>().eq(Room::getStatus, 1)));
        metric.setDescription("正在直播中的房间数量");
        return metric;
    }
}
