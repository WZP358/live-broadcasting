package cn.imhtb.live.common.utils;

import cn.imhtb.live.common.exception.BusinessException;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class AliyunSmsService {

    @Value("${aliyun.sms.enabled:true}")
    private boolean enabled;

    @Value("${aliyun.sms.accessKeyId:}")
    private String accessKeyId;

    @Value("${aliyun.sms.accessKeySecret:}")
    private String accessKeySecret;

    @Value("${aliyun.sms.endpoint:dypnsapi.aliyuncs.com}")
    private String endpoint;

    @Value("${aliyun.sms.signName:}")
    private String signName;

    @Value("${aliyun.sms.templateCode:}")
    private String templateCode;

    @Value("${aliyun.sms.templateParam:{\"code\":\"##code##\",\"min\":\"5\"}}")
    private String templateParam;

    @Value("${aliyun.sms.countryCode:}")
    private String countryCode;

    @Value("${aliyun.sms.schemeName:}")
    private String schemeName;

    @Value("${aliyun.sms.smsUpExtendCode:}")
    private String smsUpExtendCode;

    @Value("${aliyun.sms.codeLength:6}")
    private Long codeLength;

    @Value("${aliyun.sms.codeType:1}")
    private Long codeType;

    @Value("${aliyun.sms.validTimeSeconds:300}")
    private Long validTimeSeconds;

    @Value("${aliyun.sms.duplicatePolicy:1}")
    private Long duplicatePolicy;

    @Value("${aliyun.sms.intervalSeconds:10}")
    private Long intervalSeconds;

    @Value("${aliyun.sms.returnVerifyCode:true}")
    private Boolean returnVerifyCode;

    @Value("${aliyun.sms.connectTimeoutMillis:60000}")
    private Integer connectTimeoutMillis;

    @Value("${aliyun.sms.readTimeoutMillis:60000}")
    private Integer readTimeoutMillis;

    public String sendVerifyCode(String phoneNumber, String fallbackCode) {
        if (!enabled) {
            throw new BusinessException("阿里云短信服务未启用");
        }
        if (!StringUtils.hasText(accessKeyId)
                || !StringUtils.hasText(accessKeySecret)
                || !StringUtils.hasText(signName)
                || !StringUtils.hasText(templateCode)) {
            throw new BusinessException("阿里云短信配置未完整，请填写AccessKey、赠送签名和模板CODE");
        }

        try {
            Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret)
                    .setEndpoint(endpoint)
                    .setConnectTimeout(connectTimeoutMillis)
                    .setReadTimeout(readTimeoutMillis);
            Client client = new Client(config);

            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setPhoneNumber(phoneNumber)
                    .setTemplateParam(templateParam)
                    .setCodeLength(codeLength)
                    .setCodeType(codeType)
                    .setValidTime(validTimeSeconds)
                    .setDuplicatePolicy(duplicatePolicy)
                    .setInterval(intervalSeconds)
                    .setReturnVerifyCode(returnVerifyCode);
            if (StringUtils.hasText(countryCode)) {
                request.setCountryCode(countryCode);
            }
            if (StringUtils.hasText(schemeName)) {
                request.setSchemeName(schemeName);
            }
            if (StringUtils.hasText(smsUpExtendCode)) {
                request.setSmsUpExtendCode(smsUpExtendCode);
            }

            RuntimeOptions runtime = new RuntimeOptions()
                    .setConnectTimeout(connectTimeoutMillis)
                    .setReadTimeout(readTimeoutMillis);
            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCodeWithOptions(request, runtime);
            SendSmsVerifyCodeResponseBody body = response == null ? null : response.getBody();
            if (body == null || !Boolean.TRUE.equals(body.getSuccess()) || !"OK".equalsIgnoreCase(body.getCode())) {
                String code = body == null ? "UNKNOWN" : body.getCode();
                String message = body == null ? "empty response" : body.getMessage();
                log.error("aliyun sms verify code send failed, phone={}, code={}, message={}", phoneNumber, code, message);
                throw new BusinessException("短信发送失败：" + message);
            }

            String verifyCode = fallbackCode;
            if (Boolean.TRUE.equals(returnVerifyCode)
                    && body.getModel() != null
                    && StringUtils.hasText(body.getModel().getVerifyCode())) {
                verifyCode = body.getModel().getVerifyCode();
            }

            log.info("aliyun sms verify code sent successfully, phone={}, requestId={}, bizId={}",
                    phoneNumber,
                    body.getModel() == null ? null : body.getModel().getRequestId(),
                    body.getModel() == null ? null : body.getModel().getBizId());
            return verifyCode;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("aliyun sms verify code send error, phone={}", phoneNumber, e);
            throw new BusinessException("短信发送失败：" + e.getMessage());
        }
    }
}
