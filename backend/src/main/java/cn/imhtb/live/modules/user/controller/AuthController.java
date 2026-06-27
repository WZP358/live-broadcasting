package cn.imhtb.live.modules.user.controller;

import cn.hutool.core.util.DesensitizedUtil;
import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.annotation.IgnoreToken;
import cn.imhtb.live.common.utils.JwtUtil;
import cn.imhtb.live.modules.user.service.impl.UserDetailsServiceImpl;
import cn.imhtb.live.pojo.AntLiveUserBo;
import cn.imhtb.live.pojo.vo.UserInfoVo;
import cn.imhtb.live.pojo.vo.request.LoginRequest;
import cn.imhtb.live.pojo.vo.response.JwtLoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AuthController {

    private final UserDetailsServiceImpl userDetailsService;
    private final BCryptPasswordEncoder passwordEncoder;

    @IgnoreToken
    @PostMapping({"/api/login", "/login"})
    public ApiResponse<JwtLoginResponse> login(@RequestBody LoginRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getUsername())
                || !StringUtils.hasText(request.getPassword())) {
            return ApiResponse.ofError(1, "请输入账号和密码");
        }

        AntLiveUserBo user;
        try {
            user = (AntLiveUserBo) userDetailsService.loadUserByUsername(request.getUsername().trim());
        } catch (UsernameNotFoundException e) {
            return ApiResponse.ofError(1, "用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ApiResponse.ofError(1, "用户名或密码错误");
        }

        String token = JwtUtil.createTokenByParams(user.getId(), user.getNickname(), user.getUsername());
        return ApiResponse.ofSuccess(new JwtLoginResponse(token, buildUserInfo(user)));
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
