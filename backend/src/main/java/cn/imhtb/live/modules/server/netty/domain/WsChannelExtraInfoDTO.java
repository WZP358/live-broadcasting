package cn.imhtb.live.modules.server.netty.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author pinteh
 * @date 2023/6/4
 */
@Data
@Accessors(chain = true)
public class WsChannelExtraInfoDTO {

    /**
     * 用户id
     */
    private Integer userId;

    /**
     * 所在的房间列表
     */
    private Set<Integer> roomIds;

    /**
     * 主播工作台观察的房间，不计入观众在线人数。
     */
    private Set<Integer> anchorMonitorRoomIds;

    public WsChannelExtraInfoDTO addRoomId(Integer roomId){
        if (Objects.isNull(this.roomIds)){
            this.roomIds = new HashSet<>();
        }
        this.roomIds.add(roomId);
        return this;
    }

    public WsChannelExtraInfoDTO markAnchorMonitor(Integer roomId, boolean anchorMonitor) {
        if (Objects.isNull(this.anchorMonitorRoomIds)) {
            this.anchorMonitorRoomIds = new HashSet<>();
        }
        if (anchorMonitor) {
            this.anchorMonitorRoomIds.add(roomId);
        } else {
            this.anchorMonitorRoomIds.remove(roomId);
        }
        return this;
    }

    public boolean isAnchorMonitor(Integer roomId) {
        return Objects.nonNull(this.anchorMonitorRoomIds) && this.anchorMonitorRoomIds.contains(roomId);
    }

    public static WsChannelExtraInfoDTO init(){
        WsChannelExtraInfoDTO wsChannelExtraInfo = new WsChannelExtraInfoDTO();
        wsChannelExtraInfo.setRoomIds(new HashSet<>());
        wsChannelExtraInfo.setAnchorMonitorRoomIds(new HashSet<>());
        return wsChannelExtraInfo;
    }
}
