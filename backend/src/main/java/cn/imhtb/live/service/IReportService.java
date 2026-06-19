package cn.imhtb.live.service;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.pojo.database.Report;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IReportService extends IService<Report> {
    boolean submitReport(Report report);
    PageData<Report> listByUser(Integer userId, Integer page, Integer limit);
    PageData<Report> listForAdmin(Integer page, Integer limit, Integer status, String targetType);

    PageData<Report> listPending(Integer page, Integer limit);
    boolean handleReport(Integer reportId, Integer handlerId, Integer status, String result);
}
