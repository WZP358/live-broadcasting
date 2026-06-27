package cn.imhtb.live.modules.live.service.impl;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.exception.BusinessException;
import cn.imhtb.live.mappers.LiveReplayMapper;
import cn.imhtb.live.modules.live.service.ILiveInfoService;
import cn.imhtb.live.modules.live.service.ILiveReplayService;
import cn.imhtb.live.pojo.database.LiveInfo;
import cn.imhtb.live.pojo.database.LiveReplay;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.service.IFileUploadService;
import cn.imhtb.live.service.IRoomService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class LiveReplayServiceImpl extends ServiceImpl<LiveReplayMapper, LiveReplay> implements ILiveReplayService {

    private static final Set<String> SUPPORTED_VIDEO_TYPES = Set.of(
            "video/webm",
            "video/mp4",
            "video/quicktime",
            "video/x-matroska"
    );
    private static final long MAX_RECORD_SIZE = 500L * 1024 * 1024;
    private static final DateTimeFormatter OBJECT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Lazy
    @Autowired
    private IRoomService roomService;

    private final ILiveInfoService liveInfoService;
    private final IFileUploadService fileUploadService;

    @Override
    public void startRecording(Integer roomId, Integer liveInfoId) {
        Room room = roomService.getById(roomId);
        if (room == null) {
            return;
        }

        LiveReplay replay = new LiveReplay();
        replay.setRoomId(roomId);
        replay.setUserId(room.getUserId());
        replay.setLiveInfoId(liveInfoId);
        replay.setTitle((isBlank(room.getTitle()) ? "直播" : room.getTitle()) + " 直播回放");
        replay.setCoverUrl(room.getCover());
        replay.setStatus(0);
        replay.setStartTime(LocalDateTime.now());
        replay.setCreateTime(LocalDateTime.now());
        replay.setViewCount(0);
        save(replay);
    }

    @Override
    public void stopRecording(Integer liveInfoId) {
        LiveInfo liveInfo = liveInfoService.getById(liveInfoId);
        if (liveInfo == null) {
            return;
        }

        LiveReplay replay = getOne(new LambdaQueryWrapper<LiveReplay>()
                        .eq(LiveReplay::getLiveInfoId, liveInfoId)
                        .eq(LiveReplay::getStatus, 0)
                        .last("limit 1"),
                false);
        if (replay == null) {
            return;
        }

        replay.setEndTime(LocalDateTime.now());
        long duration = Duration.between(replay.getStartTime(), replay.getEndTime()).getSeconds();
        replay.setDuration(Math.max(duration, 0));
        if (!isBlank(replay.getReplayUrl())) {
            replay.setStatus(1);
        }
        updateById(replay);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LiveReplay completeBrowserRecording(Integer roomId, Integer userId, MultipartFile file, Long durationSeconds) {
        if (roomId == null || userId == null) {
            throw new BusinessException("直播录像上传参数不完整");
        }

        Room room = roomService.getById(roomId);
        if (room == null) {
            throw new BusinessException("直播间不存在");
        }
        if (!userId.equals(room.getUserId())) {
            throw new BusinessException("只能上传自己直播间的录像");
        }
        validateRecordFile(file);

        LiveReplay replay = findRecordingReplay(roomId);
        if (replay == null) {
            replay = createReplayPlaceholder(room);
        }

        String objectName = buildRecordObjectName(roomId, userId, file);
        String replayUrl;
        try {
            replayUrl = fileUploadService.uploadFileToMinioStrict(
                    file.getInputStream(),
                    objectName,
                    file.getSize(),
                    normalizeContentType(file.getContentType())
            );
        } catch (IOException e) {
            throw new BusinessException("读取直播录像失败，请重新停止直播后再试", e);
        } catch (RuntimeException e) {
            throw new BusinessException("直播录像上传 MinIO 失败，请检查 MinIO 服务状态", e);
        }

        LocalDateTime now = LocalDateTime.now();
        replay.setReplayUrl(replayUrl);
        replay.setFileSize(file.getSize());
        replay.setEndTime(now);
        if (replay.getStartTime() == null) {
            replay.setStartTime(now);
        }
        long detectedDuration = Duration.between(replay.getStartTime(), now).getSeconds();
        replay.setDuration(durationSeconds != null && durationSeconds > 0 ? durationSeconds : Math.max(detectedDuration, 0));
        replay.setStatus(1);
        if (replay.getViewCount() == null) {
            replay.setViewCount(0);
        }
        updateById(replay);
        return replay;
    }

    @Override
    public PageData<LiveReplay> listByRoom(Integer roomId, Integer page, Integer limit) {
        Page<LiveReplay> pg = page(new Page<>(page, limit),
                new LambdaQueryWrapper<LiveReplay>()
                        .eq(LiveReplay::getRoomId, roomId)
                        .eq(LiveReplay::getStatus, 1)
                        .orderByDesc(LiveReplay::getStartTime));
        PageData<LiveReplay> result = new PageData<>();
        result.setTotal(pg.getTotal());
        result.setList(pg.getRecords());
        return result;
    }

    @Override
    public LiveReplay getLatestByRoom(Integer roomId) {
        return getOne(new LambdaQueryWrapper<LiveReplay>()
                        .eq(LiveReplay::getRoomId, roomId)
                        .eq(LiveReplay::getStatus, 1)
                        .orderByDesc(LiveReplay::getStartTime)
                        .last("limit 1"),
                false);
    }

    private LiveReplay findRecordingReplay(Integer roomId) {
        return getOne(new LambdaQueryWrapper<LiveReplay>()
                        .eq(LiveReplay::getRoomId, roomId)
                        .eq(LiveReplay::getStatus, 0)
                        .orderByDesc(LiveReplay::getStartTime)
                        .last("limit 1"),
                false);
    }

    private LiveReplay createReplayPlaceholder(Room room) {
        LiveReplay replay = new LiveReplay();
        replay.setRoomId(room.getId());
        replay.setUserId(room.getUserId());
        replay.setTitle((isBlank(room.getTitle()) ? "直播" : room.getTitle()) + " 直播回放");
        replay.setCoverUrl(room.getCover());
        replay.setStatus(0);
        replay.setStartTime(LocalDateTime.now());
        replay.setCreateTime(LocalDateTime.now());
        replay.setViewCount(0);
        save(replay);
        return replay;
    }

    private void validateRecordFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("直播录像文件为空");
        }
        if (file.getSize() > MAX_RECORD_SIZE) {
            throw new BusinessException("直播录像过大，请缩短单次演示录制时长");
        }
        String contentType = normalizeContentType(file.getContentType());
        if (!SUPPORTED_VIDEO_TYPES.contains(contentType)) {
            throw new BusinessException("直播录像格式不支持，请使用浏览器默认录制格式");
        }
    }

    private String buildRecordObjectName(Integer roomId, Integer userId, MultipartFile file) {
        String extension = resolveVideoExtension(file);
        String date = LocalDateTime.now().format(OBJECT_DATE);
        return "live-records/" + date + "/room-" + roomId + "/user-" + userId + "-" + UUID.randomUUID() + "." + extension;
    }

    private String resolveVideoExtension(MultipartFile file) {
        String contentType = normalizeContentType(file.getContentType());
        if ("video/mp4".equals(contentType)) {
            return "mp4";
        }
        if ("video/quicktime".equals(contentType)) {
            return "mov";
        }
        if ("video/x-matroska".equals(contentType)) {
            return "mkv";
        }
        return "webm";
    }

    private String normalizeContentType(String contentType) {
        if (isBlank(contentType)) {
            return "video/webm";
        }
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
