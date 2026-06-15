package cn.imhtb.live.service.impl;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.mappers.CustomerServiceTicketMapper;
import cn.imhtb.live.pojo.database.CustomerServiceTicket;
import cn.imhtb.live.service.ICustomerServiceTicketService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CustomerServiceTicketServiceImpl extends ServiceImpl<CustomerServiceTicketMapper, CustomerServiceTicket>
        implements ICustomerServiceTicketService {

    @Override
    public CustomerServiceTicket submit(Integer userId, String category, String title, String content) {
        CustomerServiceTicket ticket = new CustomerServiceTicket();
        ticket.setUserId(userId);
        ticket.setCategory(StringUtils.defaultIfBlank(category, "general"));
        ticket.setTitle(StringUtils.abbreviate(StringUtils.defaultString(title).trim(), 80));
        ticket.setContent(StringUtils.abbreviate(StringUtils.defaultString(content).trim(), 1000));
        ticket.setStatus(0);
        LocalDateTime now = LocalDateTime.now();
        ticket.setCreateTime(now);
        ticket.setUpdateTime(now);
        save(ticket);
        return ticket;
    }

    @Override
    public PageData<CustomerServiceTicket> listByUser(Integer userId, Integer page, Integer limit) {
        Page<CustomerServiceTicket> pg = page(new Page<>(page, limit),
                new LambdaQueryWrapper<CustomerServiceTicket>()
                        .eq(CustomerServiceTicket::getUserId, userId)
                        .orderByDesc(CustomerServiceTicket::getUpdateTime)
                        .orderByDesc(CustomerServiceTicket::getCreateTime));
        PageData<CustomerServiceTicket> result = new PageData<>();
        result.setTotal(pg.getTotal());
        result.setList(pg.getRecords());
        return result;
    }

    @Override
    public PageData<CustomerServiceTicket> listForAdmin(Integer status, String keyword, Integer page, Integer limit) {
        LambdaQueryWrapper<CustomerServiceTicket> wrapper = new LambdaQueryWrapper<CustomerServiceTicket>()
                .eq(status != null, CustomerServiceTicket::getStatus, status)
                .and(StringUtils.isNotBlank(keyword), item -> item
                        .like(CustomerServiceTicket::getTitle, keyword)
                        .or()
                        .like(CustomerServiceTicket::getContent, keyword)
                        .or()
                        .like(CustomerServiceTicket::getCategory, keyword))
                .orderByAsc(CustomerServiceTicket::getStatus)
                .orderByDesc(CustomerServiceTicket::getUpdateTime)
                .orderByDesc(CustomerServiceTicket::getCreateTime);
        Page<CustomerServiceTicket> pg = page(new Page<>(page, limit), wrapper);
        PageData<CustomerServiceTicket> result = new PageData<>();
        result.setTotal(pg.getTotal());
        result.setList(pg.getRecords());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reply(Integer ticketId, Integer handlerId, String reply, Integer status) {
        CustomerServiceTicket ticket = getById(ticketId);
        if (ticket == null) {
            return false;
        }
        int nextStatus = status == null ? 1 : status;
        if (nextStatus != 1 && nextStatus != 2 && nextStatus != 0) {
            return false;
        }
        ticket.setHandlerId(handlerId);
        ticket.setReply(StringUtils.abbreviate(StringUtils.defaultString(reply).trim(), 1000));
        ticket.setStatus(nextStatus);
        ticket.setReplyTime(LocalDateTime.now());
        ticket.setUpdateTime(LocalDateTime.now());
        return updateById(ticket);
    }

    @Override
    public boolean closeByUser(Integer ticketId, Integer userId) {
        CustomerServiceTicket ticket = getById(ticketId);
        if (ticket == null || !userId.equals(ticket.getUserId())) {
            return false;
        }
        ticket.setStatus(2);
        ticket.setUpdateTime(LocalDateTime.now());
        return updateById(ticket);
    }
}
