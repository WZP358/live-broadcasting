package cn.imhtb.live.modules.server.netty.domain.resp;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationRespDTO {
    private String type;
    private String title;
    private String content;
    private Integer relatedId;
    private LocalDateTime createTime;
    private String cover;
    private String streamerName;
    private String roomTitle;
}
