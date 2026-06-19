package cn.imhtb.live.modules.user.service.impl;

import cn.imhtb.live.common.constants.AntLiveConstant;
import cn.imhtb.live.common.exception.BusinessException;
import cn.imhtb.live.common.exception.base.UserErrorCode;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.mappers.UserMapper;
import cn.imhtb.live.modules.infra.config.RedisKey;
import cn.imhtb.live.modules.infra.utils.RedisUtils;
import cn.imhtb.live.modules.user.model.req.UserExtraReq;
import cn.imhtb.live.modules.user.model.req.UserInfoUpdateReq;
import cn.imhtb.live.modules.user.model.req.UserRegisterReq;
import cn.imhtb.live.modules.user.service.IUserService;
import cn.imhtb.live.pojo.database.User;
import cn.imhtb.live.service.IRoomService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author pinteh
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private final IRoomService roomService;

    @Override
    public void updateStatusByIds(Integer[] ids, Integer status) {
        for (Integer id : ids) {
            User update = new User();
            update.setId(id);
            update.setDisabled(status);
            updateById(update);
        }

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean register(UserRegisterReq request) {
        // 校验用户信息
        if (checkExistUsername(request.getUsername())) {
            throw new BusinessException(UserErrorCode.ERR_USERNAME_REPEAT);
        }

        User user = new User();
        BeanUtils.copyProperties(request, user);
        user.setPassword(encoder.encode(user.getPassword()));

        // 保存用户信息
        save(user);
        log.info("user register info, userId = {}", user.getId());

        // 注册后立即初始化个人直播间，保证开播准备页有稳定数据来源。
        initializeUserRegisterData(user.getId());
        return true;
    }

    /**
     * 初始化用户注册数据
     *
     * @param userId 用户标识
     */
    private void initializeUserRegisterData(Integer userId) {
        roomService.getOrInitRoomByUserId(userId);
//        billService.save(Bill.builder()
//                .userId(userId)
//                .orderNo(CommonUtil.getOrderNo())
//                .balance(BigDecimal.ZERO)
//                .billChange(BigDecimal.ZERO)
//                .type(BillTypeEnum.INCOME.getCode())
//                .mark("初始化账单")
//                .build());
    }

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return boolean
     */
    private boolean checkExistUsername(String username) {
        return count(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0;
    }

    @Override
    public Integer countToday() {
        return this.baseMapper.countToday();
    }

    @Override
    public boolean bindUserExtra(UserExtraReq userExtraReq) {
        User user = getById(UserHolder.getUserId());
        String type = normalizeExtraType(userExtraReq.getType());
        String target = userExtraReq.getVal().trim();
        String verifyCodeKey = RedisKey.getKey(RedisKey.VERIFY_CODE, type, target);
        String verifyCode = RedisUtils.get(verifyCodeKey);
        if (!StringUtils.equalsIgnoreCase(verifyCode, userExtraReq.getVerifyCode())) {
            throw new BusinessException("验证码错误或已过期");
        }
        switch (type) {
            case AntLiveConstant.EMAIL:
                user.setEmail(target);
                break;
            case AntLiveConstant.MOBILE:
                user.setMobile(target);
                break;
            default:
        }
        boolean updated = updateById(user);
        if (updated) {
            RedisUtils.delete(verifyCodeKey);
        }
        return updated;
    }

    @Override
    public boolean updateUserInfo(UserInfoUpdateReq request) {
        User user = new User();
        user.setId(UserHolder.getUserId());
        user.setNickname(StringUtils.trim(request.getNickName()));
        user.setSignature(StringUtils.trimToEmpty(request.getSignature()));
        if (StringUtils.isEmpty(user.getSignature())){
            user.setSignature("");
        }
        return updateById(user);
    }

    @Override
    public User getUserInfo() {
        return getById(UserHolder.getUserId());
    }

    @Override
    public void updateAvatar(String avatarUrl) {
        User user = new User();
        user.setId(UserHolder.getUserId());
        user.setAvatar(avatarUrl);
        updateById(user);
    }

    private String normalizeExtraType(String type) {
        if ("phone".equalsIgnoreCase(type)) {
            return AntLiveConstant.MOBILE;
        }
        return type.toLowerCase();
    }

}
