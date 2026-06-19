package cn.imhtb.live.modules.live.guard;

import cn.imhtb.live.common.enums.LiveInfoStatusEnum;
import cn.imhtb.live.common.enums.LiveRoomStatusEnum;
import cn.imhtb.live.common.utils.DbSchemaInspector;
import cn.imhtb.live.mappers.ReportMapper;
import cn.imhtb.live.modules.live.service.IBanRecordService;
import cn.imhtb.live.modules.live.service.ILiveInfoService;
import cn.imhtb.live.modules.live.webrtc.BrowserLiveRegistry;
import cn.imhtb.live.modules.server.netty.live.NettyBrowserLiveRegistry;
import cn.imhtb.live.modules.server.netty.domain.resp.GuardViolationRespDTO;
import cn.imhtb.live.modules.server.netty.service.IRoomChatService;
import cn.imhtb.live.modules.system.service.SystemAdminNotificationService;
import cn.imhtb.live.pojo.database.BanRecord;
import cn.imhtb.live.pojo.database.LiveInfo;
import cn.imhtb.live.pojo.database.Report;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.service.IFileUploadService;
import cn.imhtb.live.service.IRoomService;
import cn.imhtb.live.service.ITokenService;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class LiveGuardService {

    private static final String STATUS_SAFE = "SAFE";
    private static final String STATUS_UNAVAILABLE = "UNAVAILABLE";
    private static final String STATUS_REVIEW = "REVIEW";
    public static final String TARGET_TYPE_LIVE_GUARD = "live_guard";

    private final GuardConfig guardConfig;
    private final IRoomService roomService;
    private final ILiveInfoService liveInfoService;
    private final IBanRecordService banRecordService;
    private final ReportMapper reportMapper;
    private final IRoomChatService roomChatService;
    private final BrowserLiveRegistry browserLiveRegistry;
    private final NettyBrowserLiveRegistry nettyBrowserLiveRegistry;
    private final SystemAdminNotificationService adminNotificationService;
    private final ITokenService tokenService;
    private final DbSchemaInspector dbSchemaInspector;
    private final IFileUploadService fileUploadService;
    private final RestTemplate restTemplate;
    private final Map<Integer, Long> lastCheckAt = new ConcurrentHashMap<>();
    private volatile long lastUnavailableLogAt = 0L;

    public LiveGuardService(GuardConfig guardConfig,
                            IRoomService roomService,
                            ILiveInfoService liveInfoService,
                            IBanRecordService banRecordService,
                            ReportMapper reportMapper,
                            IRoomChatService roomChatService,
                            BrowserLiveRegistry browserLiveRegistry,
                            NettyBrowserLiveRegistry nettyBrowserLiveRegistry,
                            SystemAdminNotificationService adminNotificationService,
                            ITokenService tokenService,
                            DbSchemaInspector dbSchemaInspector,
                            IFileUploadService fileUploadService) {
        this.guardConfig = guardConfig;
        this.roomService = roomService;
        this.liveInfoService = liveInfoService;
        this.banRecordService = banRecordService;
        this.reportMapper = reportMapper;
        this.roomChatService = roomChatService;
        this.browserLiveRegistry = browserLiveRegistry;
        this.nettyBrowserLiveRegistry = nettyBrowserLiveRegistry;
        this.adminNotificationService = adminNotificationService;
        this.tokenService = tokenService;
        this.dbSchemaInspector = dbSchemaInspector;
        this.fileUploadService = fileUploadService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Transactional(rollbackFor = Exception.class)
    public GuardCheckResult checkFrame(Integer roomId, MultipartFile file) {
        if (!guardConfig.isEnabled()) {
            return skipped("Guard disabled");
        }
        Room room = roomService.getById(roomId);
        if (room == null || !Objects.equals(room.getStatus(), LiveRoomStatusEnum.LIVING.getCode())) {
            return skipped("Room is not living");
        }
        Integer currentUserId = tokenService.getUserId();
        if (!Objects.equals(room.getUserId(), currentUserId)) {
            throw new RuntimeException("No permission to check this room");
        }
        if (!allowCheck(roomId)) {
            return skipped("Too frequent");
        }

        GuardCheckResult result = callGuard(file);
        if (result.isSafe()) {
            return result;
        }

        String reason = StringUtils.hasText(result.getReason())
                ? result.getReason()
                : "Live content triggered risk detection";
        String evidenceImageUrl = saveEvidenceScreenshot(room.getId(), file);
        submitGuardReport(room, result, reason, evidenceImageUrl);
        result.setStatus(STATUS_REVIEW);
        result.setBanned(false);
        result.setReason(reason + ", submitted for admin review");
        return result;
    }

    private boolean allowCheck(Integer roomId) {
        long now = System.currentTimeMillis();
        long interval = Math.max(guardConfig.getIntervalSeconds(), 1) * 1000L;
        Long previous = lastCheckAt.get(roomId);
        if (previous != null && now - previous < interval) {
            return false;
        }
        lastCheckAt.put(roomId, now);
        return true;
    }

    @SuppressWarnings("unchecked")
    private GuardCheckResult callGuard(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartFileResource(file));

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    guardConfig.getEndpoint(),
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            Map<String, Object> payload = response.getBody();
            if (payload == null) {
                return safe();
            }

            String status = Objects.toString(payload.get("status"), STATUS_SAFE);
            boolean safe = Boolean.TRUE.equals(payload.get("is_safe")) || STATUS_SAFE.equalsIgnoreCase(status);
            Map<String, Object> evidence = payload.get("evidence") instanceof Map
                    ? (Map<String, Object>) payload.get("evidence")
                    : new HashMap<>();
            String violationType = Objects.toString(payload.get("violation_type"), "");
            String violationLabel = Objects.toString(payload.get("violation_label"), "");

            return GuardCheckResult.builder()
                    .status(status)
                    .safe(safe)
                    .banned(false)
                    .skipped(false)
                    .reason(safe ? "" : buildReason(status, evidence, violationType, violationLabel))
                    .violationType(violationType)
                    .violationLabel(violationLabel)
                    .evidence(evidence)
                    .build();
        } catch (ResourceAccessException e) {
            logGuardUnavailable(e);
            return guardUnavailable();
        } catch (Exception e) {
            log.warn("live guard check failed, endpoint={}", guardConfig.getEndpoint(), e);
            return guardUnavailable();
        }
    }

    private String saveEvidenceScreenshot(Integer roomId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }
        try {
            String filename = "guard-room-" + roomId + "-" + System.currentTimeMillis() + "-" + IdUtil.simpleUUID() + ".jpg";
            return fileUploadService.uploadFileToMinio(file.getInputStream(), filename);
        } catch (Exception e) {
            log.warn("save live guard evidence screenshot failed, roomId={}", roomId, e);
            return "";
        }
    }

    private void submitGuardReport(Room room, GuardCheckResult result, String reason, String evidenceImageUrl) {
        if (!dbSchemaInspector.tableExists("report")) {
            log.warn("live guard report skipped, report table not found, roomId={}", room.getId());
            return;
        }

        String violationType = StringUtils.hasText(result.getViolationType())
                ? result.getViolationType()
                : resolveViolationType(result.getEvidence());
        String violationLabel = StringUtils.hasText(result.getViolationLabel())
                ? result.getViolationLabel()
                : resolveViolationLabel(result.getEvidence());
        String reportReason = StringUtils.hasText(violationLabel)
                ? violationLabel
                : (StringUtils.hasText(violationType) ? violationType : reason);
        String targetId = "room:" + room.getId() + ":" + (StringUtils.hasText(violationType) ? violationType : "UNKNOWN");

        Report existing = reportMapper.selectOne(new LambdaQueryWrapper<Report>()
                        .eq(Report::getStatus, 0)
                        .eq(Report::getRoomId, room.getId())
                        .eq(Report::getTargetType, TARGET_TYPE_LIVE_GUARD)
                        .eq(Report::getTargetId, targetId)
                        .last("limit 1"),
                false);
        if (existing != null) {
            log.info("live guard report already pending, roomId={}, reportId={}", room.getId(), existing.getId());
            return;
        }

        Report report = new Report();
        report.setReporterId(0);
        report.setTargetUserId(room.getUserId());
        report.setRoomId(room.getId());
        report.setTargetType(TARGET_TYPE_LIVE_GUARD);
        report.setTargetId(targetId);
        report.setReason(reportReason);
        report.setDescription(buildGuardReportDescription(result, reason, violationType, violationLabel, evidenceImageUrl));
        report.setStatus(0);
        reportMapper.insert(report);
        adminNotificationService.notifyLiveGuardReview(room.getId(), room.getTitle(), reportReason);
        log.warn("live guard report submitted, roomId={}, reason={}", room.getId(), reportReason);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean confirmGuardReport(Report report, Integer handlerId, String handleResult) {
        if (report == null || report.getStatus() == null || report.getStatus() != 0) {
            return false;
        }
        if (!TARGET_TYPE_LIVE_GUARD.equals(report.getTargetType())) {
            return false;
        }
        Room room = roomService.getById(report.getRoomId());
        if (room == null) {
            return false;
        }

        String reason = StringUtils.hasText(handleResult) ? handleResult : report.getReason();
        banRoomAfterReview(room, "BANNED", reason, "", report.getReason(), new HashMap<>());

        report.setStatus(1);
        report.setHandleResult(reason);
        report.setHandlerId(handlerId);
        report.setHandleTime(LocalDateTime.now());
        return reportMapper.updateById(report) > 0;
    }

    private void banRoomAfterReview(Room room,
                                    String status,
                                    String reason,
                                    String violationType,
                                    String violationLabel,
                                    Map<String, Object> evidence) {
        Room update = Room.builder()
                .id(room.getId())
                .disabled(-1)
                .status(LiveRoomStatusEnum.BANNING.getCode())
                .build();
        roomService.updateById(update);
        finishLivingInfo(room.getId());
        saveBanRecord(room.getId(), reason);

        GuardViolationRespDTO data = GuardViolationRespDTO.builder()
                .status(status)
                .reason(reason)
                .violationType(StringUtils.hasText(violationType) ? violationType : resolveViolationType(evidence))
                .violationLabel(StringUtils.hasText(violationLabel) ? violationLabel : resolveViolationLabel(evidence))
                .evidence(evidence)
                .build();
        roomChatService.sendGuardViolation(room.getId(), data);
        broadcastGuardViolation(room.getId(), data);
        lastCheckAt.remove(room.getId());
        log.warn("live room banned after admin review, roomId={}, reason={}", room.getId(), reason);
    }

    public void broadcastGuardViolation(Integer roomId, GuardViolationRespDTO data) {
        Map<String, Object> payload = signalPayload(roomId, data);
        browserLiveRegistry.sendToRoom(roomId, payload);
        nettyBrowserLiveRegistry.sendToRoom(roomId, payload);
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

    private void saveBanRecord(Integer roomId, String reason) {
        if (!dbSchemaInspector.tableExists("ban_record")) {
            return;
        }
        BanRecord existing = banRecordService.getOne(new LambdaQueryWrapper<BanRecord>()
                        .eq(BanRecord::getRoomId, roomId)
                        .eq(BanRecord::getStatus, 0)
                        .orderByDesc(BanRecord::getCreateTime)
                        .last("limit 1"),
                false);
        if (existing != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        BanRecord record = new BanRecord();
        record.setRoomId(roomId);
        record.setReason(reason);
        record.setStatus(0);
        record.setStartTime(now);
        record.setResumeTime(now.plusDays(7));
        banRecordService.save(record);
    }

    private Map<String, Object> signalPayload(Integer roomId, GuardViolationRespDTO data) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "guard-violation");
        payload.put("roomId", roomId);
        payload.put("reason", data.getReason());
        payload.put("status", data.getStatus());
        payload.put("violationType", data.getViolationType());
        payload.put("violationLabel", data.getViolationLabel());
        payload.put("evidence", data.getEvidence());
        return payload;
    }

    private GuardCheckResult safe() {
        return GuardCheckResult.builder()
                .status(STATUS_SAFE)
                .safe(true)
                .banned(false)
                .skipped(false)
                .build();
    }

    private GuardCheckResult skipped(String reason) {
        return GuardCheckResult.builder()
                .status(STATUS_SAFE)
                .safe(true)
                .banned(false)
                .skipped(true)
                .reason(reason)
                .build();
    }

    private GuardCheckResult guardUnavailable() {
        return GuardCheckResult.builder()
                .status(STATUS_UNAVAILABLE)
                .safe(true)
                .banned(false)
                .skipped(true)
                .reason("Guard service unavailable")
                .build();
    }

    private void logGuardUnavailable(ResourceAccessException e) {
        long now = System.currentTimeMillis();
        if (now - lastUnavailableLogAt < 30000L) {
            return;
        }
        lastUnavailableLogAt = now;
        log.warn("live guard service unavailable, endpoint={}, reason={}", guardConfig.getEndpoint(), e.getMostSpecificCause().getMessage());
    }

    private String buildReason(String status, Map<String, Object> evidence, String violationType, String violationLabel) {
        String label = StringUtils.hasText(violationLabel) ? violationLabel : resolveViolationLabel(evidence);
        if (StringUtils.hasText(label)) {
            return "Live content triggered risk detection: " + label;
        }
        if (StringUtils.hasText(violationType)) {
            return "Live content triggered risk detection: " + violationType;
        }
        return "Live content triggered risk detection: " + status;
    }

    private String resolveViolationType(Map<String, Object> evidence) {
        if (evidence == null) {
            return "";
        }
        if (Boolean.TRUE.equals(evidence.get("nude_detected"))) {
            return "EXPOSURE";
        }
        if (Boolean.TRUE.equals(evidence.get("physical_violence"))) {
            return "VIOLENCE";
        }
        Object weapons = evidence.get("weapons");
        if (weapons instanceof List && !((List<?>) weapons).isEmpty()) {
            return "WEAPON";
        }
        return "";
    }

    private String resolveViolationLabel(Map<String, Object> evidence) {
        String type = resolveViolationType(evidence);
        if ("EXPOSURE".equals(type)) {
            return "Exposed content";
        }
        if ("VIOLENCE".equals(type)) {
            return "Violent behavior";
        }
        if ("WEAPON".equals(type)) {
            return "Weapon";
        }
        return "";
    }

    private String buildGuardReportDescription(GuardCheckResult result,
                                               String reason,
                                               String violationType,
                                               String violationLabel,
                                               String evidenceImageUrl) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("source", "live_guard");
        payload.put("status", result.getStatus());
        payload.put("reason", reason);
        payload.put("violationType", violationType);
        payload.put("violationLabel", violationLabel);
        payload.put("evidence", result.getEvidence());
        if (StringUtils.hasText(evidenceImageUrl)) {
            payload.put("evidenceImageUrl", evidenceImageUrl);
            payload.put("screenshotUrl", evidenceImageUrl);
        }
        return JSON.toJSONString(payload);
    }

    private static class MultipartFileResource extends ByteArrayResource {

        private final String filename;

        MultipartFileResource(MultipartFile file) throws IOException {
            super(file.getBytes());
            this.filename = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "frame.jpg";
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
