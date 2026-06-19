package cn.imhtb.live.modules.live.webrtc;

import cn.imhtb.live.modules.live.service.LiveLifecycleService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BrowserLiveSignalHandler 连麦 PK 信令")
class BrowserLiveSignalHandlerTest {

    private BrowserLiveRegistry registry;
    private BrowserLiveSignalHandler handler;

    @BeforeEach
    void setUp() {
        registry = new BrowserLiveRegistry();
        handler = new BrowserLiveSignalHandler(registry, mock(LiveLifecycleService.class));
    }

    private WebSocketSession mockSession(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    @Test
    @DisplayName("PK 目标房间未开播时返回不可用消息")
    void shouldReturnUnavailableWhenPkTargetOffline() throws Exception {
        WebSocketSession inviter = mockSession("bc-1");
        registry.register(inviter, 1, BrowserLiveRegistry.SessionRole.BROADCASTER, 1001);

        handler.handleTextMessage(inviter, new TextMessage("{\"type\":\"pk-invite\",\"targetRoomId\":2}"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(inviter).sendMessage(captor.capture());
        JSONObject payload = JSON.parseObject(captor.getValue().getPayload());
        assertEquals("pk-unavailable", payload.getString("type"));
        assertEquals(1, payload.getInteger("roomId"));
        assertEquals(2, payload.getInteger("targetRoomId"));
    }

    @Test
    @DisplayName("PK 目标房间在线时转发给目标主播")
    void shouldRoutePkInviteToTargetBroadcaster() throws Exception {
        WebSocketSession inviter = mockSession("bc-1");
        WebSocketSession target = mockSession("bc-2");
        registry.register(inviter, 1, BrowserLiveRegistry.SessionRole.BROADCASTER, 1001);
        registry.register(target, 2, BrowserLiveRegistry.SessionRole.BROADCASTER, 1002);

        handler.handleTextMessage(inviter, new TextMessage("{\"type\":\"pk-invite\",\"targetRoomId\":2,\"inviterName\":\"主播A\"}"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(target).sendMessage(captor.capture());
        JSONObject payload = JSON.parseObject(captor.getValue().getPayload());
        assertEquals("pk-invite", payload.getString("type"));
        assertEquals(2, payload.getInteger("roomId"));
        assertEquals(1, payload.getInteger("fromRoomId"));
        assertEquals("bc-1", payload.getString("fromSessionId"));
        assertEquals("主播A", payload.getString("inviterName"));
    }
}
