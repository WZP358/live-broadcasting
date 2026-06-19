package cn.imhtb.live.pojo.vo.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author PinTeh
 * @date 2020/3/18
 */
@Data
public class WatchResponse {

    private Integer id;

    private String cover;

    private String avatar;

    private String title;

    private String name;

    private Integer anchorUserId;

    private Integer roomId;

    private Integer liveStatus;

    private Integer watchType;

    private LocalDateTime createTime;

}
