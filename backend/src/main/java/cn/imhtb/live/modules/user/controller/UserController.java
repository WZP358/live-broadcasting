package cn.imhtb.live.modules.user.controller;

import cn.hutool.core.util.DesensitizedUtil;
import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.annotation.IgnoreToken;
import cn.imhtb.live.modules.user.model.req.UserExtraReq;
import cn.imhtb.live.modules.user.model.req.UserInfoUpdateReq;
import cn.imhtb.live.modules.user.model.req.UserPasswordUpdateReq;
import cn.imhtb.live.modules.user.model.req.UserRegisterReq;
import cn.imhtb.live.modules.user.service.IUserService;
import cn.imhtb.live.modules.user.service.impl.UserDetailsServiceImpl;
import cn.imhtb.live.pojo.AntLiveUserBo;
import cn.imhtb.live.pojo.database.User;
import cn.imhtb.live.pojo.vo.UserInfoVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * @author pinteh
 */
@Api(tags = "user", value = "用户接口")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class UserController {

    private final IUserService userService;
    private final UserDetailsServiceImpl userDetailsService;

    @IgnoreToken
    @ApiOperation("用户注册")
    @PostMapping("/register")
    public ApiResponse<Boolean> register(@RequestBody @Valid UserRegisterReq req) {
        return ApiResponse.ofSuccess(userService.register(req));
    }

    @ApiOperation("绑定邮箱或者手机号码")
    @PostMapping("/extra/bind")
    public ApiResponse<Boolean> bindUserExtra(@RequestBody @Valid UserExtraReq req) {
        return ApiResponse.ofSuccess(userService.bindUserExtra(req));
    }

    @ApiOperation("更新用户基础信息")
    @PostMapping("/basic/update")
    public ApiResponse<Boolean> updateUserInfo(@RequestBody @Valid UserInfoUpdateReq request) {
        return ApiResponse.ofSuccess(userService.updateUserInfo(request));
    }

    @ApiOperation("修改当前登录用户密码")
    @PostMapping("/password/update")
    public ApiResponse<Boolean> updatePassword(@RequestBody @Valid UserPasswordUpdateReq request) {
        return ApiResponse.ofSuccess(userService.updatePassword(request));
    }

    @ApiOperation("获取当前登录用户信息")
    @PostMapping("/info")
    public ApiResponse<UserInfoVo> getUserInfo() {
        User user = userService.getUserInfo();
        if (user == null) {
            return ApiResponse.ofError(1, "用户不存在，请重新登录");
        }
        AntLiveUserBo userBo = (AntLiveUserBo) userDetailsService.loadUserByUsername(user.getUsername());
        return ApiResponse.ofSuccess(buildUserInfo(userBo));
    }

    private UserInfoVo buildUserInfo(AntLiveUserBo user) {
        UserInfoVo userInfoVo = new UserInfoVo();
        userInfoVo.setUserId(user.getId());
        userInfoVo.setUsername(user.getUsername());
        userInfoVo.setNickName(user.getNickname());
        userInfoVo.setAvatar(user.getAvatar());
        userInfoVo.setSignature(user.getSignature());
        userInfoVo.setRoleIds(user.getRoleIds());
        userInfoVo.setBalance(user.getBalance());
        userInfoVo.setEmail(DesensitizedUtil.email(user.getEmail()));
        userInfoVo.setMobile(DesensitizedUtil.mobilePhone(user.getMobile()));
        userInfoVo.setPassword(DesensitizedUtil.password(user.getPassword()));
        return userInfoVo;
    }

}
