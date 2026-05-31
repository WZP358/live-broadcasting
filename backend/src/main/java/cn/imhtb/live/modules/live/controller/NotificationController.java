package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.modules.live.service.INotificationService;
import cn.imhtb.live.pojo.database.Notification;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "通知接口")
@RestController
@RequestMapping("/api/v1/notification")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class NotificationController {

    private final INotificationService notificationService;

    @ApiOperation("获取通知列表")
    @GetMapping("/list")
    public ApiResponse<?> list(@RequestParam(required = false) String type,
                                @RequestParam(defaultValue = "10") Integer limit,
                                @RequestParam(defaultValue = "1") Integer page) {
        return ApiResponse.ofSuccess(notificationService.listByUser(UserHolder.getUserId(), type, page, limit));
    }

    @ApiOperation("获取未读数量")
    @GetMapping("/unread/count")
    public ApiResponse<Long> unreadCount() {
        return ApiResponse.ofSuccess(notificationService.countUnread(UserHolder.getUserId()));
    }

    @ApiOperation("标记已读")
    @PostMapping("/read")
    public ApiResponse<Boolean> markRead(@RequestBody MarkReadRequest request) {
        return ApiResponse.ofSuccess(notificationService.markRead(request.getNotificationId(), UserHolder.getUserId()));
    }

    @ApiOperation("全部已读")
    @PostMapping("/read/all")
    public ApiResponse<Boolean> markAllRead() {
        return ApiResponse.ofSuccess(notificationService.markAllRead(UserHolder.getUserId()));
    }

    @Data
    public static class MarkReadRequest {
        private Integer notificationId;
    }
}
