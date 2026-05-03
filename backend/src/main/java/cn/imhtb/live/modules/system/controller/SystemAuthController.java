package cn.imhtb.live.modules.system.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.PageData;
import cn.imhtb.live.mappers.AuthMapper;
import cn.imhtb.live.pojo.database.AuthInfo;
import cn.imhtb.live.pojo.vo.request.IdsRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/system/auth")
public class SystemAuthController {

    private final AuthMapper authMapper;

    public SystemAuthController(AuthMapper authMapper) {
        this.authMapper = authMapper;
    }

    @GetMapping("/page")
    public ApiResponse<PageData<AuthInfo>> page(@RequestParam(defaultValue = "1") Integer pageNo,
                                                @RequestParam(defaultValue = "10") Integer pageSize,
                                                @RequestParam(required = false) Integer userId,
                                                @RequestParam(required = false) Integer status) {
        Page<AuthInfo> page = authMapper.selectPage(new Page<>(pageNo, pageSize), new QueryWrapper<AuthInfo>()
                .eq(userId != null, "user_id", userId)
                .eq(status != null, "status", status)
                .orderByDesc("id"));
        return ApiResponse.ofSuccess(new PageData<>(page.getTotal(), page.getRecords()));
    }

    @PostMapping("/save")
    public ApiResponse<Boolean> save(@RequestBody AuthInfo authInfo) {
        if (authInfo.getId() == null) {
            return ApiResponse.ofSuccess(authMapper.insert(authInfo) > 0);
        }
        return ApiResponse.ofSuccess(authMapper.updateById(authInfo) > 0);
    }

    @PostMapping("/delete")
    public ApiResponse<Boolean> delete(@RequestBody IdsRequest request) {
        return ApiResponse.ofSuccess(authMapper.deleteBatchIds(Arrays.asList(request.getIds())) > 0);
    }

    @PostMapping("/status")
    public ApiResponse<Boolean> status(@RequestBody IdsRequest request) {
        if (request.getIds() == null || request.getIds().length == 0 || request.getType() == null) {
            return ApiResponse.ofSuccess(false);
        }
        int count = authMapper.update(null, new UpdateWrapper<AuthInfo>()
                .set("status", request.getType())
                .in("id", Arrays.asList(request.getIds())));
        return ApiResponse.ofSuccess(count > 0);
    }
}
