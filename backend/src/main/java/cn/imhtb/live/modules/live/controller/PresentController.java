package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.annotation.IgnoreToken;
import cn.imhtb.live.modules.infra.model.PageQuery;
import cn.imhtb.live.modules.live.service.ILiveGiftService;
import cn.imhtb.live.modules.live.vo.PresentRespVo;
import cn.imhtb.live.modules.live.vo.RewardReqVo;
import cn.imhtb.live.pojo.vo.request.SendPresentRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * @author PinTeh
 */
@Api(tags = "礼物接口")
@RestController
@RequestMapping("/api/v1/present")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PresentController {

    private final ILiveGiftService liveGiftService;

    @IgnoreToken
    @ApiOperation("获取礼物列表")
    @GetMapping("/list")
    public ApiResponse<List<PresentRespVo>> list() {
        return ApiResponse.ofSuccess(liveGiftService.list());
    }

    @ApiOperation("赠送礼物")
    @PostMapping("/live/reward")
    public ApiResponse<?> live(@RequestBody SendPresentRequest sendPresentRequest) {
        liveGiftService.createReward(sendPresentRequest.toRewardReqVo());
        return ApiResponse.ofSuccess();
    }

    @ApiOperation("赠送礼物")
    @PostMapping("/reward")
    public ApiResponse<Boolean> reward(@RequestBody @Valid RewardReqVo rewardReqVo) {
        liveGiftService.createReward(rewardReqVo);
        return ApiResponse.ofSuccess(true);
    }

    @ApiOperation("获取赠送礼物记录")
    @PostMapping("/reward/records")
    public ApiResponse<PageData<?>> rewardRecords(@RequestBody PageQuery pageQuery) {
        return ApiResponse.ofSuccess(liveGiftService.rewardList(pageQuery.getPageNo(), pageQuery.getPageSize()));
    }

}
