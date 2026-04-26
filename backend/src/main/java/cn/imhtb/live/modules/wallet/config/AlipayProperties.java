package cn.imhtb.live.modules.wallet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {

    private boolean enabled = false;

    private boolean verifySign = true;

    private String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

    private String appId = "";

    private String appPrivateKey = "";

    private String alipayPublicKey = "";

    private String notifyUrl = "";

    private String returnUrl = "http://localhost:5174/#/center/dollar/wallet";

    private String syncReturnUrl = "";

    private String charset = "UTF-8";

    private String format = "json";

    private String signType = "RSA2";

    private String productCode = "FAST_INSTANT_TRADE_PAY";

    private String subjectPrefix = "AntLive 开心果充值";
}
