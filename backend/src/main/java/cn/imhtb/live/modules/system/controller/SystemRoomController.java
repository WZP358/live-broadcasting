package cn.imhtb.live.modules.system.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.PageData;
import cn.imhtb.live.modules.infra.controller.AbstractBaseController;
import cn.imhtb.live.modules.infra.model.PageQuery;
import cn.imhtb.live.modules.system.model.SystemRoomDetail;
import cn.imhtb.live.modules.system.model.SystemRoomQuery;
import cn.imhtb.live.modules.system.model.SystemRoomUpdate;
import cn.imhtb.live.modules.system.service.ISystemRoomService;
import cn.imhtb.live.common.holder.UserHolder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author pinteh
 * @date 2025/12/07
 */
@Api(tags = "系统_直播间管理接口")
@RequestMapping("/api/v1/system/room")
@RestController
public class SystemRoomController extends AbstractBaseController<ISystemRoomService, SystemRoomDetail, SystemRoomQuery, SystemRoomDetail, SystemRoomUpdate> {

    private final ISystemRoomService systemRoomService;

    public SystemRoomController(ISystemRoomService systemRoomService) {
        this.systemRoomService = systemRoomService;
    }

    /**
     * 重写分页查询方法，返回包含主播信息的直播间数据
     */
    @Override
    @ApiOperation("获取分页数据（包含主播信息）")
    @GetMapping("/page")
    public ApiResponse<PageData<SystemRoomDetail>> page(SystemRoomQuery query, PageQuery pageQuery) {
        return ApiResponse.ofSuccess(systemRoomService.page(query, pageQuery));
    }

    @ApiOperation("切换直播间封禁状态")
    @PostMapping("/toggleStatus")
    public ApiResponse<Boolean> toggleStatus(@RequestBody ToggleStatusRequest request) {
        if (request == null || request.getId() == null || request.getDisabled() == null) {
            return ApiResponse.ofError("缺少直播间状态参数");
        }
        boolean ok = systemRoomService.toggleStatus(request.getId(), request.getDisabled(), UserHolder.getUserId());
        return ok ? ApiResponse.ofSuccess(true) : ApiResponse.ofError("直播间状态更新失败");
    }

    public static class ToggleStatusRequest {
        private Integer id;
        private Integer disabled;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer getDisabled() {
            return disabled;
        }

        public void setDisabled(Integer disabled) {
            this.disabled = disabled;
        }
    }
}
