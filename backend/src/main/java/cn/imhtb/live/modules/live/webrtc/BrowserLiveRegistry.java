package cn.imhtb.live.modules.live.webrtc;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class BrowserLiveRegistry {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, SessionMeta> sessionMetas = new ConcurrentHashMap<>();
    private final Map<Integer, String> broadcastersByRoom = new ConcurrentHashMap<>();

    public void register(WebSocketSession session, Integer roomId, String role, Integer userId) {
        sessions.put(session.getId(), session);
        sessionMetas.put(session.getId(), new SessionMeta(session.getId(), roomId, role, userId));
        if (SessionRole.BROADCASTER.equals(role)) {
            broadcastersByRoom.put(roomId, session.getId());
        }
    }

    public SessionMeta remove(String sessionId) {
        sessions.remove(sessionId);
        SessionMeta meta = sessionMetas.remove(sessionId);
        if (meta != null && SessionRole.BROADCASTER.equals(meta.getRole())) {
            broadcastersByRoom.remove(meta.getRoomId(), sessionId);
        }
        return meta;
    }

    public SessionMeta getMeta(String sessionId) {
        return sessionMetas.get(sessionId);
    }

    public String getBroadcasterSessionId(Integer roomId) {
        String sessionId = broadcastersByRoom.get(roomId);
        if (sessionId == null) {
            return null;
        }
        WebSocketSession session = sessions.get(sessionId);
        if (session == null || !session.isOpen()) {
            broadcastersByRoom.remove(roomId, sessionId);
            sessionMetas.remove(sessionId);
            sessions.remove(sessionId);
            return null;
        }
        return sessionId;
    }

    public Set<String> getViewerSessionIds(Integer roomId) {
        return sessionMetas.values().stream()
                .filter(meta -> meta.getRoomId() != null && meta.getRoomId().equals(roomId))
                .filter(meta -> SessionRole.VIEWER.equals(meta.getRole()))
                .map(SessionMeta::getSessionId)
                .collect(Collectors.toSet());
    }

    public boolean isBrowserLive(Integer roomId) {
        return roomId != null && getBroadcasterSessionId(roomId) != null;
    }

    public void send(String sessionId, Object payload) {
        WebSocketSession session = sessions.get(sessionId);
        if (session == null || !session.isOpen()) {
            return;
        }
        synchronized (session) {
            try {
                session.sendMessage(new TextMessage(JSON.toJSONString(payload)));
            } catch (IOException ignored) {
            }
        }
    }

    public interface SessionRole {
        String BROADCASTER = "broadcaster";
        String VIEWER = "viewer";
    }

    @Getter
    @RequiredArgsConstructor
    public static class SessionMeta {
        private final String sessionId;
        private final Integer roomId;
        private final String role;
        private final Integer userId;
    }
}
