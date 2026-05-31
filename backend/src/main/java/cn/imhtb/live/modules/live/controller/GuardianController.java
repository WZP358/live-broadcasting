package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.modules.live.service.IGuardianService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "守护/粉丝团接口")
@RestController
@RequestMapping("/api/v1/guardian")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class GuardianController {

    private final IGuardianService guardianService;

    @ApiOperation("开通守护")
    @PostMapping("/subscribe")
    public ApiResponse<Boolean> subscribe(@RequestBody SubscribeReq req) {
        guardianService.subscribe(UserHolder.getUserId(), req.getTargetUserId(), req.getLevel(), req.isAutoRenew());
        return ApiResponse.ofSuccess(true);
    }

    @ApiOperation("取消自动续费")
    @PostMapping("/cancel-renew")
    public ApiResponse<Boolean> cancelAutoRenew(@RequestBody CancelReq req) {
        guardianService.cancelAutoRenew(UserHolder.getUserId(), req.getTargetUserId());
        return ApiResponse.ofSuccess(true);
    }

    @ApiOperation("我的守护列表")
    @GetMapping("/my-guardians")
    public ApiResponse<?> myGuardians(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "20") Integer limit) {
        return ApiResponse.ofSuccess(guardianService.myGuardians(UserHolder.getUserId(), page, limit));
    }

    @ApiOperation("我的粉丝团")
    @GetMapping("/my-fans")
    public ApiResponse<?> myFans(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "20") Integer limit) {
        return ApiResponse.ofSuccess(guardianService.myFans(UserHolder.getUserId(), page, limit));
    }

    @ApiOperation("检查是否守护")
    @GetMapping("/check")
    public ApiResponse<?> check(@RequestParam Integer targetUserId) {
        boolean isGuardian = guardianService.isGuardian(UserHolder.getUserId(), targetUserId);
        int level = guardianService.getGuardianLevel(UserHolder.getUserId(), targetUserId);
        return ApiResponse.ofSuccess(java.util.Map.of("isGuardian", isGuardian, "level", level));
    }

    @ApiOperation("守护价格")
    @GetMapping("/prices")
    public ApiResponse<?> prices() {
        return ApiResponse.ofSuccess(java.util.Map.of(
                1, guardianService.getMonthlyPrice(1),
                2, guardianService.getMonthlyPrice(2),
                3, guardianService.getMonthlyPrice(3)));
    }

    @Data public static class SubscribeReq { private Integer targetUserId; private Integer level; private boolean autoRenew; }
    @Data public static class CancelReq { private Integer targetUserId; }
}
