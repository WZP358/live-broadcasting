package cn.imhtb.live.service;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.pojo.database.CustomerServiceTicket;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ICustomerServiceTicketService extends IService<CustomerServiceTicket> {
    CustomerServiceTicket submit(Integer userId, String category, String title, String content);

    PageData<CustomerServiceTicket> listByUser(Integer userId, Integer page, Integer limit);

    PageData<CustomerServiceTicket> listForAdmin(Integer status, String keyword, Integer page, Integer limit);

    boolean reply(Integer ticketId, Integer handlerId, String reply, Integer status);

    boolean closeByUser(Integer ticketId, Integer userId);
}
