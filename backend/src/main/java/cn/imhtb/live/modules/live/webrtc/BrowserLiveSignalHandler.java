package cn.imhtb.live.modules.live.webrtc;

import cn.imhtb.live.common.utils.JwtUtil;
import cn.imhtb.live.modules.live.service.LiveLifecycleService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class BrowserLiveSignalHandler extends TextWebSocketHandler {

    private final BrowserLiveRegistry registry;
    private final LiveLifecycleService liveLifecycleService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("browser-live ws connected, sessionId={}, uri={}", session.getId(), session.getUri());
        sendToSession(session, message("connected", null, session.getId()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JSONObject body = JSON.parseObject(message.getPayload());
            String type = body.getString("type");
            if (!StringUtils.hasText(type)) {
                return;
            }
            switch (type) {
                case "join":
                    handleJoin(session, body);
                    break;
                case "offer":
                case "answer":
                case "ice-candidate":
                    relay(session, body, type);
                    break;
                case "cohost-request":
                    routeCohostRequest(session, body);
                    break;
                case "cohost-accepted":
                case "cohost-rejected":
                case "cohost-offer":
                case "cohost-answer":
                case "cohost-ice-candidate":
                case "cohost-ended":
                    relayInteraction(session, body, type);
                    break;
                case "pk-invite":
                    routePkInvite(session, body);
                    break;
                case "pk-accepted":
                case "pk-rejected":
                case "pk-offer":
                case "pk-answer":
                case "pk-ice-candidate":
                case "pk-ended":
                    relayInteraction(session, body, type);
                    break;
                case "leave":
                    handleLeave(session);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.warn("browser-live ws message handling failed, sessionId={}", session.getId(), e);
            sendToSession(session, errorMessage(null, "直播信令处理失败"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("browser-live ws closed, sessionId={}, code={}, reason={}", session.getId(), status.getCode(), status.getReason());
        handleLeave(session);
    }

    private void handleJoin(WebSocketSession session, JSONObject body) {
        Integer roomId = body.getInteger("roomId");
        String role = body.getString("role");
        if (roomId == null || !StringUtils.hasText(role)) {
            sendAndClose(session, errorMessage(roomId, "缺少房间号或角色信息"));
            return;
        }

        // 网页开播优先保证本地可用性，不强依赖 websocket token 鉴权。
        Integer userId = parseUserId(session, role);
        registry.register(session, roomId, role, userId);
        if (BrowserLiveRegistry.SessionRole.BROADCASTER.equals(role)) {
            liveLifecycleService.markLiveStarted(roomId, userId);
        }
        log.info("browser-live joined, sessionId={}, roomId={}, role={}, userId={}", session.getId(), roomId, role, userId);
        sendToSession(session, joinedMessage(roomId, role, session.getId()));

        if (BrowserLiveRegistry.SessionRole.VIEWER.equals(role)) {
            String broadcasterSessionId = registry.getBroadcasterSessionId(roomId);
            if (StringUtils.hasText(broadcasterSessionId)) {
                registry.send(session.getId(), message("broadcaster-online", roomId, broadcasterSessionId));
                registry.send(broadcasterSessionId, message("viewer-joined", roomId, session.getId()));
            } else {
                registry.send(session.getId(), message("broadcaster-offline", roomId, null));
            }
            return;
        }

        for (String viewerSessionId : registry.getViewerSessionIds(roomId)) {
            registry.send(viewerSessionId, message("broadcaster-online", roomId, session.getId()));
            registry.send(session.getId(), message("viewer-joined", roomId, viewerSessionId));
        }
    }

    private void relay(WebSocketSession session, JSONObject body, String type) {
        BrowserLiveRegistry.SessionMeta meta = registry.getMeta(session.getId());
        if (meta == null) {
            return;
        }
        String targetSessionId = body.getString("targetSessionId");
        if (!StringUtils.hasText(targetSessionId)) {
            registry.send(session.getId(), errorMessage(meta.getRoomId(), "缺少目标会话"));
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("roomId", meta.getRoomId());
        payload.put("fromSessionId", session.getId());
        payload.put("sdp", body.get("sdp"));
        payload.put("candidate", body.get("candidate"));
        registry.send(targetSessionId, payload);
    }

    private void routeCohostRequest(WebSocketSession session, JSONObject body) {
        BrowserLiveRegistry.SessionMeta meta = registry.getMeta(session.getId());
        if (meta == null || meta.getRoomId() == null) {
            return;
        }

        String targetSessionId = body.getString("targetSessionId");
        if (!StringUtils.hasText(targetSessionId)) {
            targetSessionId = registry.getBroadcasterSessionId(meta.getRoomId());
        }
        if (!StringUtils.hasText(targetSessionId) || targetSessionId.equals(meta.getSessionId())) {
            registry.send(meta.getSessionId(), errorMessage(meta.getRoomId(), "当前房间没有可接收连麦申请的主播"));
            return;
        }

        registry.send(targetSessionId, interactionPayload(session, meta, body, "cohost-request"));
    }

    private void routePkInvite(WebSocketSession session, JSONObject body) {
        BrowserLiveRegistry.SessionMeta meta = registry.getMeta(session.getId());
        if (meta == null || meta.getRoomId() == null) {
            return;
        }

        Integer targetRoomId = body.getInteger("targetRoomId");
        if (targetRoomId == null || targetRoomId.equals(meta.getRoomId())) {
            registry.send(meta.getSessionId(), pkUnavailable(meta.getRoomId(), targetRoomId, "请选择另一个正在开播的房间"));
            return;
        }

        String targetSessionId = registry.getBroadcasterSessionId(targetRoomId);
        if (!StringUtils.hasText(targetSessionId)) {
            registry.send(meta.getSessionId(), pkUnavailable(meta.getRoomId(), targetRoomId, "目标主播未开播或未连接网页开播信令"));
            return;
        }

        Map<String, Object> payload = interactionPayload(session, meta, body, "pk-invite");
        payload.put("roomId", targetRoomId);
        payload.put("targetRoomId", targetRoomId);
        registry.send(targetSessionId, payload);
    }

    private void relayInteraction(WebSocketSession session, JSONObject body, String type) {
        BrowserLiveRegistry.SessionMeta meta = registry.getMeta(session.getId());
        if (meta == null) {
            return;
        }

        String targetSessionId = body.getString("targetSessionId");
        if (!StringUtils.hasText(targetSessionId)) {
            registry.send(meta.getSessionId(), errorMessage(meta.getRoomId(), "互动信令缺少目标会话"));
            return;
        }

        registry.send(targetSessionId, interactionPayload(session, meta, body, type));
    }

    private Map<String, Object> interactionPayload(WebSocketSession session, BrowserLiveRegistry.SessionMeta meta, JSONObject body, String type) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("roomId", meta.getRoomId());
        payload.put("fromRoomId", meta.getRoomId());
        payload.put("fromSessionId", session.getId());
        payload.put("fromUserId", meta.getUserId());
        payload.put("targetRoomId", body.getInteger("targetRoomId"));
        payload.put("targetSessionId", body.getString("targetSessionId"));
        payload.put("sdp", body.get("sdp"));
        payload.put("candidate", body.get("candidate"));
        copyIfPresent(body, payload, "applicantName");
        copyIfPresent(body, payload, "applicantAvatar");
        copyIfPresent(body, payload, "inviterName");
        copyIfPresent(body, payload, "inviterAvatar");
        copyIfPresent(body, payload, "acceptorName");
        copyIfPresent(body, payload, "reason");
        return payload;
    }

    private void copyIfPresent(JSONObject body, Map<String, Object> payload, String key) {
        Object value = body.get(key);
        if (value != null) {
            payload.put(key, value);
        }
    }

    private Map<String, Object> pkUnavailable(Integer roomId, Integer targetRoomId, String message) {
        Map<String, Object> payload = message("pk-unavailable", roomId, null);
        payload.put("targetRoomId", targetRoomId);
        payload.put("message", message);
        return payload;
    }

    private void handleLeave(WebSocketSession session) {
        BrowserLiveRegistry.SessionMeta meta = registry.remove(session.getId());
        if (meta == null || meta.getRoomId() == null) {
            return;
        }
        if (BrowserLiveRegistry.SessionRole.BROADCASTER.equals(meta.getRole())) {
            for (String viewerSessionId : registry.getViewerSessionIds(meta.getRoomId())) {
                registry.send(viewerSessionId, message("broadcaster-offline", meta.getRoomId(), session.getId()));
            }
            liveLifecycleService.markLiveStopped(meta.getRoomId());
            return;
        }
        String broadcasterSessionId = registry.getBroadcasterSessionId(meta.getRoomId());
        if (StringUtils.hasText(broadcasterSessionId)) {
            registry.send(broadcasterSessionId, message("viewer-left", meta.getRoomId(), session.getId()));
        }
    }

    private Integer parseUserId(WebSocketSession session, String role) {
        if (!BrowserLiveRegistry.SessionRole.BROADCASTER.equals(role)) {
            return null;
        }
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (!StringUtils.hasText(query)) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && "token".equals(kv[0])) {
                return JwtUtil.verifyGetUserId(decodeQueryValue(kv[1]));
            }
        }
        return null;
    }

    private String decodeQueryValue(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private Map<String, Object> joinedMessage(Integer roomId, String role, String sessionId) {
        Map<String, Object> payload = message("joined", roomId, sessionId);
        payload.put("role", role);
        return payload;
    }

    private Map<String, Object> errorMessage(Integer roomId, String error) {
        Map<String, Object> payload = message("error", roomId, null);
        payload.put("message", error);
        return payload;
    }

    private void sendAndClose(WebSocketSession session, Map<String, Object> payload) {
        sendToSession(session, payload);
        try {
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException e) {
            log.warn("browser-live ws close failed, sessionId={}", session.getId(), e);
        }
    }

    private void sendToSession(WebSocketSession session, Map<String, Object> payload) {
        if (session == null || !session.isOpen()) {
            return;
        }
        synchronized (session) {
            try {
                session.sendMessage(new TextMessage(JSON.toJSONString(payload)));
            } catch (IOException e) {
                log.warn("browser-live ws send failed, sessionId={}", session.getId(), e);
            }
        }
    }

    private Map<String, Object> message(String type, Integer roomId, Object sessionId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("roomId", roomId);
        payload.put("sessionId", sessionId);
        return payload;
    }
}
