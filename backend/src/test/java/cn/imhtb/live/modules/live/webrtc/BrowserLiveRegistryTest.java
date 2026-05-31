package cn.imhtb.live.modules.live.webrtc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("BrowserLiveRegistry 浏览器直播注册表")
class BrowserLiveRegistryTest {

    private BrowserLiveRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new BrowserLiveRegistry();
    }

    private WebSocketSession mockSession(String id, boolean open) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(open);
        return session;
    }

    @Test
    @DisplayName("注册主播后 isBrowserLive 返回 true")
    void shouldMarkRoomAsLiveWhenBroadcasterRegisters() {
        WebSocketSession bcSession = mockSession("bc-1", true);
        registry.register(bcSession, 1, BrowserLiveRegistry.SessionRole.BROADCASTER, 100);
        assertTrue(registry.isBrowserLive(1));
    }

    @Test
    @DisplayName("仅注册观众不会让 isBrowserLive 返回 true")
    void shouldNotMarkLiveWhenOnlyViewerRegisters() {
        WebSocketSession viewer = mockSession("v-1", true);
        registry.register(viewer, 2, BrowserLiveRegistry.SessionRole.VIEWER, 200);
        assertFalse(registry.isBrowserLive(2));
    }

    @Test
    @DisplayName("未注册的房间 isBrowserLive 返回 false")
    void shouldReturnFalseForUnknownRoom() {
        assertFalse(registry.isBrowserLive(99999));
    }

    @Test
    @DisplayName("isBrowserLive(null) 返回 false")
    void shouldReturnFalseForNullRoomId() {
        assertFalse(registry.isBrowserLive(null));
    }

    @Test
    @DisplayName("remove 主播后 isBrowserLive 返回 false")
    void shouldMarkStoppedWhenBroadcasterRemoved() {
        WebSocketSession bcSession = mockSession("bc-2", true);
        registry.register(bcSession, 5, BrowserLiveRegistry.SessionRole.BROADCASTER, 300);
        assertTrue(registry.isBrowserLive(5));

        BrowserLiveRegistry.SessionMeta meta = registry.remove("bc-2");
        assertNotNull(meta);
        assertEquals(5, meta.getRoomId());
        assertEquals(BrowserLiveRegistry.SessionRole.BROADCASTER, meta.getRole());
        assertEquals(300, meta.getUserId());
        assertFalse(registry.isBrowserLive(5));
    }

    @Test
    @DisplayName("主播 session 关闭后 isBrowserLive 返回 false")
    void shouldReturnFalseWhenSessionClosed() {
        WebSocketSession openSession = mockSession("bc-3", true);
        registry.register(openSession, 10, BrowserLiveRegistry.SessionRole.BROADCASTER, 400);
        assertTrue(registry.isBrowserLive(10));

        // 当 getBroadcasterSessionId 检测到 session.isOpen() 为 false，会清理
        when(openSession.isOpen()).thenReturn(false);
        assertNull(registry.getBroadcasterSessionId(10));
    }

    @Test
    @DisplayName("getViewerSessionIds 只返回观众")
    void shouldReturnOnlyViewerIds() {
        WebSocketSession bc = mockSession("bc-vs", true);
        WebSocketSession v1 = mockSession("v-vs-1", true);
        WebSocketSession v2 = mockSession("v-vs-2", true);

        registry.register(bc, 7, BrowserLiveRegistry.SessionRole.BROADCASTER, 10);
        registry.register(v1, 7, BrowserLiveRegistry.SessionRole.VIEWER, 20);
        registry.register(v2, 7, BrowserLiveRegistry.SessionRole.VIEWER, 30);

        assertEquals(2, registry.getViewerSessionIds(7).size());
        assertTrue(registry.getViewerSessionIds(7).contains("v-vs-1"));
        assertTrue(registry.getViewerSessionIds(7).contains("v-vs-2"));
    }
}
