package cn.imhtb.live.pojo.database;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("live_replay")
public class LiveReplay {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer roomId;
    private Integer userId;
    private Integer liveInfoId;
    private String title;
    private String replayUrl;
    private String coverUrl;
    private Long duration;        // 秒
    private Integer status;       // 0=录制中 1=已就绪 2=已删除
    private Long fileSize;        // 字节
    private Integer viewCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
}
