package cn.imhtb.live.modules.system.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SystemGiftFlowSummary {

    private long totalCount;

    private BigDecimal totalAmount = BigDecimal.ZERO;

    private BigDecimal liveAmount = BigDecimal.ZERO;

    private BigDecimal videoAmount = BigDecimal.ZERO;

    private long todayCount;

    private BigDecimal todayAmount = BigDecimal.ZERO;

}
