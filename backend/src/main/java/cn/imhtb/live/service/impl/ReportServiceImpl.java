package cn.imhtb.live.service.impl;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.mappers.ReportMapper;
import cn.imhtb.live.modules.live.guard.LiveGuardService;
import cn.imhtb.live.pojo.database.Report;
import cn.imhtb.live.service.IReportService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements IReportService {

    private final LiveGuardService liveGuardService;

    @Override
    public PageData<Report> listByUser(Integer userId, Integer page, Integer limit) {
        Page<Report> pg = page(new Page<>(page, limit),
                new LambdaQueryWrapper<Report>()
                        .eq(Report::getReporterId, userId)
                        .orderByDesc(Report::getCreateTime));
        PageData<Report> result = new PageData<>();
        result.setTotal(pg.getTotal());
        result.setList(pg.getRecords());
        return result;
    }

    @Override
    public PageData<Report> listPending(Integer page, Integer limit) {
        Page<Report> pg = page(new Page<>(page, limit),
                new LambdaQueryWrapper<Report>()
                        .eq(Report::getStatus, 0)
                        .orderByAsc(Report::getCreateTime));
        PageData<Report> result = new PageData<>();
        result.setTotal(pg.getTotal());
        result.setList(pg.getRecords());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleReport(Integer reportId, Integer handlerId, Integer status, String result) {
        Report report = getById(reportId);
        if (report == null || report.getStatus() == null || report.getStatus() != 0) {
            return false;
        }
        if (status == null || (status != 1 && status != 2)) {
            return false;
        }
        if (status == 1 && LiveGuardService.TARGET_TYPE_LIVE_GUARD.equals(report.getTargetType())) {
            return liveGuardService.confirmGuardReport(report, handlerId, result);
        }
        report.setStatus(status);
        report.setHandleResult(result);
        report.setHandlerId(handlerId);
        report.setHandleTime(LocalDateTime.now());
        return updateById(report);
    }
}
