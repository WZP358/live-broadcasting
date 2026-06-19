package cn.imhtb.live.modules.wallet.service;

import cn.imhtb.live.common.exception.BusinessException;
import cn.imhtb.live.modules.wallet.config.AlipayProperties;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlipayRechargeService {

    private final AlipayProperties properties;

    public String createPagePay(String outTradeNo, BigDecimal amount, String subject) {
        ensureConfigured();
        try {
            AlipayClient alipayClient = new DefaultAlipayClient(
                    properties.getGatewayUrl(),
                    properties.getAppId(),
                    properties.getAppPrivateKey(),
                    properties.getFormat(),
                    properties.getCharset(),
                    properties.getAlipayPublicKey(),
                    properties.getSignType()
            );

            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            String notifyUrl = normalizeOptionalUrl(properties.getNotifyUrl(), "支付宝异步通知地址 notifyUrl");
            if (StringUtils.hasText(notifyUrl)) {
                request.setNotifyUrl(notifyUrl);
            }
            request.setReturnUrl(resolveSyncReturnUrl());
            request.setBizContent(JSON.toJSONString(buildBizContent(outTradeNo, amount, subject)));
            return alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            throw new BusinessException("支付宝下单失败：" + e.getErrMsg(), e);
        }
    }

    public boolean verify(Map<String, String> params) {
        ensureConfigured();
        if (!properties.isVerifySign()) {
            return true;
        }
        try {
            return AlipaySignature.rsaCheckV1(
                    params,
                    properties.getAlipayPublicKey(),
                    properties.getCharset(),
                    properties.getSignType()
            );
        } catch (AlipayApiException e) {
            throw new BusinessException("支付宝回调验签失败：" + e.getErrMsg(), e);
        }
    }

    private Map<String, Object> buildBizContent(String outTradeNo, BigDecimal amount, String subject) {
        Map<String, Object> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("total_amount", amount.toPlainString());
        bizContent.put("subject", subject);
        bizContent.put("product_code", properties.getProductCode());
        return bizContent;
    }

    private String resolveSyncReturnUrl() {
        if (StringUtils.hasText(properties.getSyncReturnUrl())) {
            return normalizeRequiredUrl(properties.getSyncReturnUrl(), "支付宝同步回跳地址 syncReturnUrl");
        }
        if (StringUtils.hasText(properties.getNotifyUrl()) && properties.getNotifyUrl().endsWith("/notify")) {
            return normalizeRequiredUrl(
                    properties.getNotifyUrl().substring(0, properties.getNotifyUrl().length() - "/notify".length()) + "/return",
                    "支付宝同步回跳地址"
            );
        }
        return normalizeRequiredUrl(properties.getReturnUrl(), "支付宝同步回跳地址 returnUrl");
    }

    private void ensureConfigured() {
        if (!properties.isEnabled()) {
            throw new BusinessException("支付宝沙箱支付未启用，请先配置 alipay.enabled=true");
        }
        if (!StringUtils.hasText(properties.getAppId())
                || !StringUtils.hasText(properties.getAppPrivateKey())) {
            throw new BusinessException("支付宝沙箱配置未完整，请填写 appId 和应用私钥");
        }
        if (properties.isVerifySign() && !StringUtils.hasText(properties.getAlipayPublicKey())) {
            throw new BusinessException("支付宝验签已启用，请填写支付宝公钥");
        }
        normalizeRequiredUrl(properties.getGatewayUrl(), "支付宝网关地址 gatewayUrl");
        resolveSyncReturnUrl();
    }

    private String normalizeRequiredUrl(String value, String name) {
        String url = normalizeOptionalUrl(value, name);
        if (!StringUtils.hasText(url)) {
            throw new BusinessException(name + "不能为空");
        }
        return url;
    }

    private String normalizeOptionalUrl(String value, String name) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String url = value.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new BusinessException(name + "必须以 http:// 或 https:// 开头");
        }
        return url;
    }
}
