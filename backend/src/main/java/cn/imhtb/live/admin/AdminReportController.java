package cn.imhtb.live.admin;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.service.IReportService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/report")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AdminReportController {

    private final IReportService reportService;

    @GetMapping("/list")
    public ApiResponse<?> list(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer limit) {
        return ApiResponse.ofSuccess(reportService.listPending(page, limit));
    }

    @PostMapping("/handle")
    public ApiResponse<Boolean> handle(@RequestBody HandleRequest req) {
        boolean ok = reportService.handleReport(req.getReportId(), UserHolder.getUserId(), req.getStatus(), req.getResult());
        return ok ? ApiResponse.ofSuccess(true) : ApiResponse.ofError("处理失败");
    }

    @Data
    public static class HandleRequest {
        private Integer reportId;
        private Integer status;  // 1=已处理 2=已驳回
        private String result;
    }
}
