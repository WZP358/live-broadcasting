package cn.imhtb.live.modules.system.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.modules.system.model.SystemDemoStatus;
import cn.imhtb.live.modules.system.service.ISystemDemoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "系统_演示模式接口")
@RestController
@RequestMapping("/api/v1/system/demo")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ROLE_ROOT','ROLE_LIVE')")
public class SystemDemoController {

    private final ISystemDemoService systemDemoService;

    @ApiOperation("获取演示模式状态")
    @GetMapping("/status")
    public ApiResponse<SystemDemoStatus> status() {
        return ApiResponse.ofSuccess(systemDemoService.status());
    }

    @ApiOperation("开启演示模式")
    @PostMapping("/enable")
    public ApiResponse<SystemDemoStatus> enable() {
        return ApiResponse.ofSuccess(systemDemoService.enable());
    }

    @ApiOperation("关闭演示模式")
    @PostMapping("/disable")
    public ApiResponse<SystemDemoStatus> disable() {
        return ApiResponse.ofSuccess(systemDemoService.disable());
    }
}
