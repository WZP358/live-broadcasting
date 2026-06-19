package cn.imhtb.live.modules.server.netty.live;

import com.alibaba.fastjson.JSON;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class NettyBrowserLiveRegistry {

    private final Map<String, Channel> channels = new ConcurrentHashMap<>();
    private final Map<String, SessionMeta> sessionMetas = new ConcurrentHashMap<>();
    private final Map<Integer, String> broadcastersByRoom = new ConcurrentHashMap<>();

    public void register(Channel channel, Integer roomId, String role, Integer userId) {
        String sessionId = channel.id().asLongText();
        channels.put(sessionId, channel);
        sessionMetas.put(sessionId, new SessionMeta(sessionId, roomId, role, userId));
        if (SessionRole.BROADCASTER.equals(role) && roomId != null) {
            broadcastersByRoom.put(roomId, sessionId);
        }
    }

    public SessionMeta remove(Channel channel) {
        if (channel == null) {
            return null;
        }
        return remove(channel.id().asLongText());
    }

    public SessionMeta remove(String sessionId) {
        channels.remove(sessionId);
        SessionMeta meta = sessionMetas.remove(sessionId);
        if (meta != null && SessionRole.BROADCASTER.equals(meta.getRole()) && meta.getRoomId() != null) {
            broadcastersByRoom.remove(meta.getRoomId(), sessionId);
        }
        return meta;
    }

    public SessionMeta getMeta(String sessionId) {
        return sessionMetas.get(sessionId);
    }

    public String getBroadcasterSessionId(Integer roomId) {
        if (roomId == null) {
            return null;
        }
        String sessionId = broadcastersByRoom.get(roomId);
        if (sessionId == null) {
            return null;
        }
        Channel channel = channels.get(sessionId);
        if (channel == null || !channel.isActive()) {
            broadcastersByRoom.remove(roomId, sessionId);
            sessionMetas.remove(sessionId);
            channels.remove(sessionId);
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
        Channel channel = channels.get(sessionId);
        if (channel == null || !channel.isActive()) {
            return;
        }
        channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(payload)));
    }

    public void sendToRoom(Integer roomId, Object payload) {
        if (roomId == null) {
            return;
        }
        sessionMetas.values().stream()
                .filter(meta -> roomId.equals(meta.getRoomId()))
                .map(SessionMeta::getSessionId)
                .forEach(sessionId -> send(sessionId, payload));
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
