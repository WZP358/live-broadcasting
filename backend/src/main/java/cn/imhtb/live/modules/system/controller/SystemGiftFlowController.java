package cn.imhtb.live.modules.system.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.PageData;
import cn.imhtb.live.modules.system.model.SystemGiftFlowQuery;
import cn.imhtb.live.modules.system.model.SystemGiftFlowRecord;
import cn.imhtb.live.modules.system.model.SystemGiftFlowSummary;
import cn.imhtb.live.modules.system.service.ISystemGiftFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/gift-flow")
@RequiredArgsConstructor
public class SystemGiftFlowController {

    private final ISystemGiftFlowService giftFlowService;

    @GetMapping("/page")
    public ApiResponse<PageData<SystemGiftFlowRecord>> page(SystemGiftFlowQuery query,
                                                            @RequestParam(defaultValue = "1") Integer pageNo,
                                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.ofSuccess(giftFlowService.page(query, pageNo, pageSize));
    }

    @GetMapping("/summary")
    public ApiResponse<SystemGiftFlowSummary> summary(SystemGiftFlowQuery query) {
        return ApiResponse.ofSuccess(giftFlowService.summary(query));
    }

}
