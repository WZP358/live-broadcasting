package cn.imhtb.live.modules.live.guard;

import cn.imhtb.live.common.enums.LiveInfoStatusEnum;
import cn.imhtb.live.common.enums.LiveRoomStatusEnum;
import cn.imhtb.live.common.utils.DbSchemaInspector;
import cn.imhtb.live.modules.live.service.IBanRecordService;
import cn.imhtb.live.modules.live.service.ILiveInfoService;
import cn.imhtb.live.modules.live.webrtc.BrowserLiveRegistry;
import cn.imhtb.live.modules.server.netty.domain.resp.GuardViolationRespDTO;
import cn.imhtb.live.modules.server.netty.service.IRoomChatService;
import cn.imhtb.live.pojo.database.BanRecord;
import cn.imhtb.live.pojo.database.LiveInfo;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.service.IRoomService;
import cn.imhtb.live.service.ITokenService;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
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

    private final GuardConfig guardConfig;
    private final IRoomService roomService;
    private final ILiveInfoService liveInfoService;
    private final IBanRecordService banRecordService;
    private final IRoomChatService roomChatService;
    private final BrowserLiveRegistry browserLiveRegistry;
    private final ITokenService tokenService;
    private final DbSchemaInspector dbSchemaInspector;
    private final RestTemplate restTemplate;
    private final Map<Integer, Long> lastCheckAt = new ConcurrentHashMap<>();
    private volatile long lastUnavailableLogAt = 0L;

    public LiveGuardService(GuardConfig guardConfig,
                            IRoomService roomService,
                            ILiveInfoService liveInfoService,
                            IBanRecordService banRecordService,
                            IRoomChatService roomChatService,
                            BrowserLiveRegistry browserLiveRegistry,
                            ITokenService tokenService,
                            DbSchemaInspector dbSchemaInspector) {
        this.guardConfig = guardConfig;
        this.roomService = roomService;
        this.liveInfoService = liveInfoService;
        this.banRecordService = banRecordService;
        this.roomChatService = roomChatService;
        this.browserLiveRegistry = browserLiveRegistry;
        this.tokenService = tokenService;
        this.dbSchemaInspector = dbSchemaInspector;
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
                : "Live content violation detected";
        banRoom(room, result.getStatus(), reason, result.getViolationType(), result.getViolationLabel(), result.getEvidence());
        result.setBanned(true);
        result.setReason(reason);
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

    private void logGuardUnavailable(ResourceAccessException e) {
        long now = System.currentTimeMillis();
        if (now - lastUnavailableLogAt < 30000L) {
            return;
        }
        lastUnavailableLogAt = now;
        log.warn("live guard service unavailable, endpoint={}, reason={}", guardConfig.getEndpoint(), e.getMostSpecificCause().getMessage());
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

    private void banRoom(Room room,
                         String status,
                         String reason,
                         String violationType,
                         String violationLabel,
                         Map<String, Object> evidence) {
        Room update = Room.builder()
                .id(room.getId())
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
        browserLiveRegistry.sendToRoom(room.getId(), signalPayload(room.getId(), data));
        lastCheckAt.remove(room.getId());
        log.warn("live room banned by guard, roomId={}, reason={}", room.getId(), reason);
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

    private String buildReason(String status, Map<String, Object> evidence, String violationType, String violationLabel) {
        String label = StringUtils.hasText(violationLabel) ? violationLabel : resolveViolationLabel(evidence);
        if (StringUtils.hasText(label)) {
            return "直播内容触发违规检测：" + label + "，直播间已封停";
        }
        return "直播内容触发违规检测，直播间已封停（" + status + "）";
    }

    private String resolveViolationType(Map<String, Object> evidence) {
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
            return "过于暴露";
        }
        if ("VIOLENCE".equals(type)) {
            return "暴力行为";
        }
        if ("WEAPON".equals(type)) {
            return "违规刀具";
        }
        return "";
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
