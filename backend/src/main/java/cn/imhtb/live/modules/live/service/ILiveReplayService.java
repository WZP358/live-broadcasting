package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.pojo.database.LiveReplay;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

public interface ILiveReplayService extends IService<LiveReplay> {
    void startRecording(Integer roomId, Integer liveInfoId);
    void stopRecording(Integer liveInfoId);
    LiveReplay completeBrowserRecording(Integer roomId, Integer userId, MultipartFile file, Long durationSeconds);
    PageData<LiveReplay> listByRoom(Integer roomId, Integer page, Integer limit);
    LiveReplay getLatestByRoom(Integer roomId);
}
