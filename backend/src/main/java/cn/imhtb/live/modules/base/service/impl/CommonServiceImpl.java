package cn.imhtb.live.modules.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.imhtb.live.common.constants.AntLiveConstant;
import cn.imhtb.live.common.enums.StatusEnum;
import cn.imhtb.live.common.exception.BusinessException;
import cn.imhtb.live.common.utils.AliyunSmsService;
import cn.imhtb.live.common.utils.CommonUtil;
import cn.imhtb.live.common.utils.MailUtil;
import cn.imhtb.live.mappers.CategoryMapper;
import cn.imhtb.live.modules.base.model.CategoryResp;
import cn.imhtb.live.modules.base.model.VerifyCodeReq;
import cn.imhtb.live.modules.base.service.ICommonService;
import cn.imhtb.live.modules.infra.config.RedisKey;
import cn.imhtb.live.modules.infra.utils.RedisUtils;
import cn.imhtb.live.pojo.database.Category;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class CommonServiceImpl implements ICommonService {

    private static final Duration VERIFY_CODE_TTL = Duration.ofMinutes(5);

    private final CategoryMapper categoryMapper;
    private final MailUtil mailUtil;
    private final AliyunSmsService aliyunSmsService;

    @Value("${verify-code.local-mail-fallback:false}")
    private boolean localMailFallback;

    @Override
    public List<CategoryResp> getCategories() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, StatusEnum.YES.getCode())
                .orderByDesc(Category::getSort)
                .orderByAsc(Category::getId);
        List<Category> categories = categoryMapper.selectList(wrapper);
        return BeanUtil.copyToList(categories, CategoryResp.class);
    }

    @Override
    public String sendVerifyCode(VerifyCodeReq req) {
        String verifyType = req.getVerifyType().toLowerCase();
        String target = req.getTarget().trim();
        String verifyCode = String.valueOf(CommonUtil.getRandomCode());
        String verifyCodeKey = RedisKey.getKey(RedisKey.VERIFY_CODE, verifyType, target);
        if (Boolean.TRUE.equals(RedisUtils.contains(verifyCodeKey))) {
            throw new BusinessException("验证码已发送，请稍后再试");
        }

        log.info("send verify code, type={}, target={}, code={}", verifyType, target, verifyCode);
        switch (verifyType) {
            case AntLiveConstant.EMAIL:
                return sendEmailVerifyCode(target, verifyCodeKey, verifyCode);
            case AntLiveConstant.MOBILE:
            case "phone":
                return sendMobileVerifyCode(target, verifyCodeKey, verifyCode);
            default:
                throw new BusinessException("不支持的验证码类型");
        }
    }

    private String sendEmailVerifyCode(String target, String verifyCodeKey, String verifyCode) {
        try {
            mailUtil.sendSimpleMessage(
                    target,
                    "直播平台验证码",
                    "您的验证码为：" + verifyCode + "，有效期5分钟，请勿泄露给他人。"
            );
            RedisUtils.set(verifyCodeKey, verifyCode, VERIFY_CODE_TTL);
            return "验证码发送成功";
        } catch (BusinessException e) {
            if (!localMailFallback) {
                throw e;
            }
            log.warn("email smtp unavailable, using local verify code fallback, target={}, code={}", target, verifyCode);
            RedisUtils.set(verifyCodeKey, verifyCode, VERIFY_CODE_TTL);
            return "SMTP不可用，本地验证码：" + verifyCode;
        }
    }

    private String sendMobileVerifyCode(String target, String verifyCodeKey, String verifyCode) {
        if (!StringUtils.hasText(target) || !target.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("请输入有效的手机号码");
        }
        String sentCode = aliyunSmsService.sendVerifyCode(target, verifyCode);
        RedisUtils.set(verifyCodeKey, sentCode, VERIFY_CODE_TTL);
        return "验证码发送成功";
    }
}
