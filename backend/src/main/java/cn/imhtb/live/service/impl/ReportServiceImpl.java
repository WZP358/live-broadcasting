package cn.imhtb.live.service.impl;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.exception.BusinessException;
import cn.imhtb.live.common.utils.DbSchemaInspector;
import cn.imhtb.live.mappers.ReportMapper;
import cn.imhtb.live.modules.live.guard.LiveGuardService;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.pojo.database.Report;
import cn.imhtb.live.service.IRoomService;
import cn.imhtb.live.service.IReportService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements IReportService {

    private static final int PENDING = 0;
    private static final int ACCEPTED = 1;
    private static final int REJECTED = 2;

    private final LiveGuardService liveGuardService;
    private final IRoomService roomService;
    private final DbSchemaInspector dbSchemaInspector;

    @EventListener(ApplicationReadyEvent.class)
    public void initReportSchema() {
        dbSchemaInspector.executeQuietly("ALTER TABLE `report` MODIFY COLUMN `description` TEXT NULL COMMENT '补充说明/证据JSON'");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitReport(Report report) {
        validateSubmit(report);
        Report existing = getOne(new LambdaQueryWrapper<Report>()
                        .eq(Report::getReporterId, report.getReporterId())
                        .eq(Report::getRoomId, report.getRoomId())
                        .eq(Report::getTargetType, report.getTargetType())
                        .eq(Report::getTargetId, report.getTargetId())
                        .eq(Report::getStatus, PENDING)
                        .last("limit 1"),
                false);
        if (existing != null) {
            throw new BusinessException("该举报已进入审核队列，请勿重复提交");
        }
        report.setStatus(PENDING);
        report.setHandleResult(null);
        report.setHandlerId(null);
        report.setHandleTime(null);
        return save(report);
    }

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
    public PageData<Report> listForAdmin(Integer page, Integer limit, Integer status, String targetType) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>()
                .eq(status != null, Report::getStatus, status)
                .eq(hasText(targetType), Report::getTargetType, targetType)
                .orderByAsc(status != null && status == PENDING, Report::getCreateTime)
                .orderByDesc(status == null || status != PENDING, Report::getCreateTime);
        Page<Report> pg = page(new Page<>(page, limit), wrapper);
        PageData<Report> result = new PageData<>();
        result.setTotal(pg.getTotal());
        result.setList(pg.getRecords());
        return result;
    }

    @Override
    public PageData<Report> listPending(Integer page, Integer limit) {
        return listForAdmin(page, limit, PENDING, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleReport(Integer reportId, Integer handlerId, Integer status, String result) {
        Report report = getById(reportId);
        if (report == null || report.getStatus() == null || report.getStatus() != PENDING) {
            return false;
        }
        if (status == null || (status != ACCEPTED && status != REJECTED)) {
            return false;
        }
        if (status == ACCEPTED && LiveGuardService.TARGET_TYPE_LIVE_GUARD.equals(report.getTargetType())) {
            return liveGuardService.confirmGuardReport(report, handlerId, result);
        }
        report.setStatus(status);
        report.setHandleResult(result);
        report.setHandlerId(handlerId);
        report.setHandleTime(LocalDateTime.now());
        return updateById(report);
    }

    private void validateSubmit(Report report) {
        if (report == null || report.getReporterId() == null) {
            throw new BusinessException("请先登录后再提交举报");
        }
        if (!hasText(report.getTargetType())) {
            throw new BusinessException("举报类型不能为空");
        }
        if (!hasText(report.getReason())) {
            throw new BusinessException("举报原因不能为空");
        }
        if (report.getRoomId() == null && !"user".equals(report.getTargetType())) {
            throw new BusinessException("举报直播内容需要关联直播间");
        }
        if (!hasText(report.getTargetId())) {
            report.setTargetId(report.getRoomId() == null ? String.valueOf(report.getTargetUserId()) : String.valueOf(report.getRoomId()));
        }
        if (report.getTargetUserId() != null && report.getReporterId().equals(report.getTargetUserId())) {
            throw new BusinessException("不能举报自己");
        }
        if (report.getRoomId() != null) {
            Room room = roomService.getById(report.getRoomId());
            if (room != null && report.getReporterId().equals(room.getUserId())) {
                throw new BusinessException("不能举报自己的直播间");
            }
            if (room != null && report.getTargetUserId() == null) {
                report.setTargetUserId(room.getUserId());
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
