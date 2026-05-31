package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.pojo.database.Report;
import cn.imhtb.live.service.IReportService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "举报接口")
@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ReportController {

    private final IReportService reportService;

    @ApiOperation("提交举报")
    @PostMapping("/submit")
    public ApiResponse<Boolean> submit(@RequestBody SubmitRequest req) {
        Report report = new Report();
        report.setReporterId(UserHolder.getUserId());
        report.setTargetUserId(req.getTargetUserId());
        report.setRoomId(req.getRoomId());
        report.setTargetType(req.getTargetType());
        report.setTargetId(req.getTargetId());
        report.setReason(req.getReason());
        report.setDescription(req.getDescription());
        report.setStatus(0);
        return reportService.save(report)
                ? ApiResponse.ofSuccess(true)
                : ApiResponse.ofError("提交失败");
    }

    @ApiOperation("我的举报列表")
    @GetMapping("/my")
    public ApiResponse<PageData<Report>> myReports(@RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer limit) {
        return ApiResponse.ofSuccess(reportService.listByUser(UserHolder.getUserId(), page, limit));
    }

    @Data
    public static class SubmitRequest {
        private Integer targetUserId;
        private Integer roomId;
        private String targetType;
        private String targetId;
        private String reason;
        private String description;
    }
}
