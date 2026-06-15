package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.pojo.database.CustomerServiceTicket;
import cn.imhtb.live.service.ICustomerServiceTicketService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer-service")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class CustomerServiceController {

    private final ICustomerServiceTicketService customerServiceTicketService;

    @PostMapping("/submit")
    public ApiResponse<CustomerServiceTicket> submit(@RequestBody SubmitRequest req) {
        if (req == null || StringUtils.isBlank(req.getTitle()) || StringUtils.isBlank(req.getContent())) {
            return ApiResponse.ofError("请填写问题标题和详细描述");
        }
        CustomerServiceTicket ticket = customerServiceTicketService.submit(
                UserHolder.getUserId(),
                req.getCategory(),
                req.getTitle(),
                req.getContent()
        );
        return ApiResponse.ofSuccess(ticket);
    }

    @GetMapping("/my")
    public ApiResponse<PageData<CustomerServiceTicket>> myTickets(@RequestParam(defaultValue = "1") Integer page,
                                                                  @RequestParam(defaultValue = "10") Integer limit) {
        return ApiResponse.ofSuccess(customerServiceTicketService.listByUser(UserHolder.getUserId(), page, limit));
    }

    @PostMapping("/close")
    public ApiResponse<Boolean> close(@RequestBody CloseRequest req) {
        boolean ok = req != null && customerServiceTicketService.closeByUser(req.getTicketId(), UserHolder.getUserId());
        return ok ? ApiResponse.ofSuccess(true) : ApiResponse.ofError("关闭失败");
    }

    @Data
    public static class SubmitRequest {
        private String category;
        private String title;
        private String content;
    }

    @Data
    public static class CloseRequest {
        private Integer ticketId;
    }
}
