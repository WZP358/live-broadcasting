package cn.imhtb.live.modules.system.service.impl;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.enums.LiveInfoStatusEnum;
import cn.imhtb.live.common.enums.LiveRoomStatusEnum;
import cn.imhtb.live.common.enums.StatusEnum;
import cn.imhtb.live.common.utils.DbSchemaInspector;
import cn.imhtb.live.mappers.BanRecordMapper;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.modules.infra.model.PageQuery;
import cn.imhtb.live.modules.live.guard.LiveGuardService;
import cn.imhtb.live.modules.live.service.ILiveInfoService;
import cn.imhtb.live.modules.server.netty.domain.resp.GuardViolationRespDTO;
import cn.imhtb.live.modules.infra.service.impl.BaseServiceImpl;
import cn.imhtb.live.modules.system.model.SystemRoomDetail;
import cn.imhtb.live.modules.system.model.SystemRoomQuery;
import cn.imhtb.live.modules.system.model.SystemRoomUpdate;
import cn.imhtb.live.modules.system.service.ISystemRoomService;
import cn.imhtb.live.modules.system.service.SystemAdminNotificationService;
import cn.imhtb.live.pojo.database.BanRecord;
import cn.imhtb.live.pojo.database.LiveInfo;
import cn.imhtb.live.pojo.database.Room;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * @author pinteh
 * @date 2025/12/07
 */
@Service
public class SystemRoomServiceImpl extends BaseServiceImpl<RoomMapper, Room, SystemRoomDetail, SystemRoomQuery, SystemRoomDetail, SystemRoomUpdate> implements ISystemRoomService {

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private ILiveInfoService liveInfoService;

    @Autowired
    private BanRecordMapper banRecordMapper;

    @Autowired
    private DbSchemaInspector dbSchemaInspector;

    @Autowired
    private LiveGuardService liveGuardService;

    @Autowired
    private SystemAdminNotificationService adminNotificationService;

    @Override
    public PageData<SystemRoomDetail> page(SystemRoomQuery query, PageQuery pageQuery) {
        // 构建查询条件
        QueryWrapper<Room> queryWrapper = buildQueryWrapper(query);
        
        // 使用自定义的 pageDetail 方法查询，会关联主播信息
        Page<SystemRoomDetail> page = roomMapper.pageDetail(
            new Page<>(pageQuery.getPageNo(), pageQuery.getPageSize()),
            queryWrapper
        );
        
        // 封装返回结果
        PageData<SystemRoomDetail> pageData = new PageData<>();
        pageData.setTotal(page.getTotal());
        pageData.setList(page.getRecords());
        return pageData;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleStatus(Integer roomId, Integer disabled, Integer handlerId) {
        if (roomId == null || disabled == null) {
            return false;
        }
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            return false;
        }

        boolean ban = StatusEnum.NO.getCode() == disabled;
        Room update = new Room();
        update.setId(roomId);
        update.setDisabled(disabled);
        update.setStatus(ban ? LiveRoomStatusEnum.BANNING.getCode() : LiveRoomStatusEnum.STOP.getCode());
        int updated = roomMapper.updateById(update);
        if (updated <= 0) {
            return false;
        }

        if (ban) {
            String reason = "管理员手动封禁直播间";
            finishLivingInfo(roomId);
            saveBanRecord(roomId, reason, handlerId);
            broadcastRoomBan(roomId, reason);
            adminNotificationService.notifyRoomBan(room.getId(), room.getTitle(), reason);
        } else {
            closeBanRecord(roomId, handlerId);
            adminNotificationService.notifyRoomBan(room.getId(), room.getTitle(), "管理员已解封直播间");
        }
        return true;
    }

    private void finishLivingInfo(Integer roomId) {
        LiveInfo liveInfo = liveInfoService.getOne(new LambdaQueryWrapper<LiveInfo>()
                        .eq(LiveInfo::getRoomId, roomId)
                        .eq(LiveInfo::getStatus, LiveInfoStatusEnum.LIVING.getCode())
                        .orderByDesc(LiveInfo::getCreateTime)
                        .last("limit 1"),
                false);
        if (liveInfo == null) {
            return;
        }
        liveInfo.setStatus(LiveInfoStatusEnum.FINISHED.getCode());
        liveInfo.setEndTime(LocalDateTime.now());
        liveInfoService.updateById(liveInfo);
    }

    private void saveBanRecord(Integer roomId, String reason, Integer handlerId) {
        if (!dbSchemaInspector.tableExists("ban_record")) {
            return;
        }
        BanRecord existing = banRecordMapper.selectOne(new LambdaQueryWrapper<BanRecord>()
                        .eq(BanRecord::getRoomId, roomId)
                        .eq(BanRecord::getStatus, 0)
                        .last("limit 1"),
                false);
        if (existing != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        BanRecord record = new BanRecord();
        record.setRoomId(roomId);
        record.setReason(reason);
        record.setMark(handlerId == null ? "系统操作" : "管理员ID：" + handlerId);
        record.setStatus(0);
        record.setStartTime(now);
        record.setResumeTime(now.plusDays(7));
        banRecordMapper.insert(record);
    }

    private void closeBanRecord(Integer roomId, Integer handlerId) {
        if (!dbSchemaInspector.tableExists("ban_record")) {
            return;
        }
        BanRecord record = banRecordMapper.selectOne(new LambdaQueryWrapper<BanRecord>()
                        .eq(BanRecord::getRoomId, roomId)
                        .eq(BanRecord::getStatus, 0)
                        .orderByDesc(BanRecord::getCreateTime)
                        .last("limit 1"),
                false);
        if (record == null) {
            return;
        }
        record.setStatus(1);
        record.setMark(handlerId == null ? "手动恢复" : "管理员ID：" + handlerId + " 手动恢复");
        record.setResumeTime(LocalDateTime.now());
        banRecordMapper.updateById(record);
    }

    private void broadcastRoomBan(Integer roomId, String reason) {
        GuardViolationRespDTO data = GuardViolationRespDTO.builder()
                .status("BANNED")
                .reason(reason)
                .violationType("ADMIN_BAN")
                .violationLabel("管理员封禁")
                .evidence(new HashMap<>())
                .build();
        liveGuardService.broadcastGuardViolation(roomId, data);
    }

    /**
     * 从父类抽取出 buildQueryWrapper 方法
     */
    private QueryWrapper<Room> buildQueryWrapper(SystemRoomQuery query) {
        QueryWrapper<Room> queryWrapper = new QueryWrapper<>();
        if (query.getId() != null) {
            queryWrapper.eq("r.id", query.getId());
        }
        if (query.getUserId() != null) {
            queryWrapper.eq("r.user_id", query.getUserId());
        }
        if (query.getTitle() != null && !query.getTitle().isEmpty()) {
            queryWrapper.like("r.title", query.getTitle());
        }
        if (query.getStatus() != null) {
            queryWrapper.eq("r.status", query.getStatus());
        }
        if (query.getCategoryId() != null) {
            queryWrapper.eq("r.category_id", query.getCategoryId());
        }
        if (query.getDisabled() != null) {
            queryWrapper.eq("r.disabled", query.getDisabled());
        }
        return queryWrapper;
    }
}
