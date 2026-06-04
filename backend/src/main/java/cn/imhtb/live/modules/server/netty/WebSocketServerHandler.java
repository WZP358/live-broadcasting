package cn.imhtb.live.modules.server.netty;

import cn.imhtb.live.common.utils.SpringContextUtil;
import cn.imhtb.live.modules.server.netty.domain.req.WsMsgReqDTO;
import cn.imhtb.live.modules.server.netty.enums.WsReqTypeEnum;
import cn.imhtb.live.modules.server.netty.live.NettyBrowserLiveService;
import cn.imhtb.live.modules.server.netty.service.IRoomChatService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class WebSocketServerHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        getBrowserLiveBean().handleDisconnect(ctx.channel());
        getRoomChatBean().exit(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        getBrowserLiveBean().handleDisconnect(ctx.channel());
        getRoomChatBean().exit(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof java.io.IOException) {
            log.debug("WebSocket 客户端断开, channel={}", ctx.channel().id());
        } else {
            log.error("WebSocket 异常, channel={}", ctx.channel().id(), cause);
        }
        getBrowserLiveBean().handleDisconnect(ctx.channel());
        getRoomChatBean().exit(ctx.channel());
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                log.warn("{} 读空闲超时 30 秒，主动关闭连接", ctx.channel().id());
                getBrowserLiveBean().handleDisconnect(ctx.channel());
                getRoomChatBean().exit(ctx.channel());
                ctx.close();
                return;
            }
        } else if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            log.info("{} WebSocket 握手完成", ctx.channel().id());
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String text = frame.text();
        try {
            JSONObject body = JSON.parseObject(text);
            if (body != null && getBrowserLiveBean().supports(body)) {
                getBrowserLiveBean().handle(ctx.channel(), body);
                return;
            }

            WsMsgReqDTO wsMsgReqDTO = JSON.parseObject(text, WsMsgReqDTO.class);
            WsReqTypeEnum wsReqTypeEnum = WsReqTypeEnum.of(wsMsgReqDTO.getMsgType());
            if (Objects.isNull(wsReqTypeEnum)) {
                log.warn("未知消息类型: msgType={}", wsMsgReqDTO.getMsgType());
                return;
            }

            switch (wsReqTypeEnum) {
                case ENTER:
                    getRoomChatBean().enter(ctx.channel(), wsMsgReqDTO.getData());
                    break;
                case EXIT:
                    getRoomChatBean().exit(ctx.channel());
                    break;
                case HEARTBEAT:
                    log.debug("房间聊天心跳: channel={}", ctx.channel().id());
                    break;
                default:
                    log.warn("未处理的消息类型: {}", wsMsgReqDTO.getMsgType());
                    break;
            }
        } catch (Exception e) {
            log.error("处理 WebSocket 消息失败: {}", text, e);
        }
    }

    private IRoomChatService getRoomChatBean() {
        return SpringContextUtil.getBean(IRoomChatService.class);
    }

    private NettyBrowserLiveService getBrowserLiveBean() {
        return SpringContextUtil.getBean(NettyBrowserLiveService.class);
    }
}
