package cn.imhtb.live.admin;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.service.ICustomerServiceTicketService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/customer-service")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AdminCustomerServiceController {

    private final ICustomerServiceTicketService customerServiceTicketService;

    @GetMapping("/list")
    public ApiResponse<?> list(@RequestParam(required = false) Integer status,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer limit) {
        return ApiResponse.ofSuccess(customerServiceTicketService.listForAdmin(status, keyword, page, limit));
    }

    @PostMapping("/reply")
    public ApiResponse<Boolean> reply(@RequestBody ReplyRequest req) {
        boolean ok = req != null && customerServiceTicketService.reply(
                req.getTicketId(),
                UserHolder.getUserId(),
                req.getReply(),
                req.getStatus()
        );
        return ok ? ApiResponse.ofSuccess(true) : ApiResponse.ofError("处理失败");
    }

    @Data
    public static class ReplyRequest {
        private Integer ticketId;
        private String reply;
        private Integer status;
    }
}
