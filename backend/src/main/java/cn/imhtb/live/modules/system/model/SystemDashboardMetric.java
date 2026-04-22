package cn.imhtb.live.modules.system.model;

import lombok.Data;

@Data
public class SystemDashboardMetric {

    private String code;

    private String label;

    private Long value;

    private String description;
}
