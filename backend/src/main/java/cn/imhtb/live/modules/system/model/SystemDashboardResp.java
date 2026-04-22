package cn.imhtb.live.modules.system.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SystemDashboardResp {

    private List<SystemDashboardMetric> metrics = new ArrayList<>();
}
