package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.mappers.NotificationPrefMapper;
import cn.imhtb.live.pojo.database.NotificationPref;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "通知偏好")
@RestController
@RequestMapping("/api/v1/notification/pref")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class NotificationPrefController {

    private final NotificationPrefMapper prefMapper;

    @GetMapping
    public ApiResponse<NotificationPref> get() {
        NotificationPref pref = prefMapper.selectOne(
                new LambdaQueryWrapper<NotificationPref>().eq(NotificationPref::getUserId, UserHolder.getUserId()));
        if (pref == null) {
            pref = new NotificationPref();
            pref.setUserId(UserHolder.getUserId());
            pref.setLiveStartEnabled(true);
            pref.setFollowEnabled(true);
        }
        return ApiResponse.ofSuccess(pref);
    }

    @PostMapping
    public ApiResponse<Boolean> save(@RequestBody SaveReq req) {
        NotificationPref pref = prefMapper.selectOne(
                new LambdaQueryWrapper<NotificationPref>().eq(NotificationPref::getUserId, UserHolder.getUserId()));
        if (pref == null) {
            pref = new NotificationPref();
            pref.setUserId(UserHolder.getUserId());
        }
        pref.setLiveStartEnabled(req.getLiveStartEnabled());
        pref.setFollowEnabled(req.getFollowEnabled());
        pref.setDndStart(req.getDndStart());
        pref.setDndEnd(req.getDndEnd());
        boolean ok = pref.getId() == null ? prefMapper.insert(pref) > 0 : prefMapper.updateById(pref) > 0;
        return ok ? ApiResponse.ofSuccess(true) : ApiResponse.ofError("保存失败");
    }

    @Data
    public static class SaveReq {
        private Boolean liveStartEnabled;
        private Boolean followEnabled;
        private String dndStart;
        private String dndEnd;
    }
}
