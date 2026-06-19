package cn.imhtb.live.modules.server.netty.domain.req;

import lombok.Data;

/**
 * @author pinteh
 * @date 2023/6/13
 */
@Data
public class WsEnterReqDTO {

    private Integer roomId;

    /**
     * 主播工作台监听房间互动事件时使用。
     * 这类连接需要接收礼物/弹幕广播，但不计入观众在线人数。
     */
    private Boolean anchorMonitor;

}
