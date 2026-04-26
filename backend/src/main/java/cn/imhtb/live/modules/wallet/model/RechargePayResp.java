package cn.imhtb.live.modules.wallet.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RechargePayResp {

    private String outTradeNo;

    private String payHtml;
}
