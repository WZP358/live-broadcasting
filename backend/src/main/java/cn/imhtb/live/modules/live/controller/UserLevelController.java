package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.modules.live.service.IUserLevelService;
import cn.imhtb.live.pojo.database.UserLevel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "用户等级接口")
@RestController
@RequestMapping("/api/v1/level")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class UserLevelController {

    private final IUserLevelService levelService;

    @ApiOperation("获取我的等级信息")
    @GetMapping("/my")
    public ApiResponse<UserLevel> myLevel() {
        return ApiResponse.ofSuccess(levelService.getOrCreate(UserHolder.getUserId()));
    }

    @ApiOperation("获取用户等级信息")
    @GetMapping("/user/{userId}")
    public ApiResponse<UserLevel> userLevel(@PathVariable Integer userId) {
        return ApiResponse.ofSuccess(levelService.getOrCreate(userId));
    }
}
