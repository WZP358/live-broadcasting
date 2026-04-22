package cn.imhtb.live.modules.system.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.modules.system.model.SystemDashboardResp;
import cn.imhtb.live.modules.system.service.ISystemDashboardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "系统_仪表盘接口")
@RestController
@RequestMapping("/api/v1/system/dashboard")
@RequiredArgsConstructor
public class SystemDashboardController {

    private final ISystemDashboardService systemDashboardService;

    @ApiOperation("获取后台仪表盘汇总数据")
    @GetMapping("/summary")
    public ApiResponse<SystemDashboardResp> getSummary() {
        return ApiResponse.ofSuccess(systemDashboardService.getSummary());
    }
}
