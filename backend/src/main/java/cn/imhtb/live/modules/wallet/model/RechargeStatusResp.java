package cn.imhtb.live.modules.wallet.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RechargeStatusResp {

    private String outTradeNo;

    private String tradeStatus;

    private boolean paid;

    private BigDecimal amount;

    private BigDecimal balance;

    private String message;
}
