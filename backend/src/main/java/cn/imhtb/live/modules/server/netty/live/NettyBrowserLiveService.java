package cn.imhtb.live.modules.server.netty.live;

import cn.imhtb.live.modules.live.service.LiveLifecycleService;
import cn.imhtb.live.modules.server.netty.AttrUtil;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NettyBrowserLiveService {

    private final NettyBrowserLiveRegistry registry;
    private final LiveLifecycleService liveLifecycleService;

    public boolean supports(JSONObject body) {
        String type = body.getString("type");
        return "join".equals(type)
                || "offer".equals(type)
                || "answer".equals(type)
                || "ice-candidate".equals(type)
                || "cohost-request".equals(type)
                || "cohost-accepted".equals(type)
                || "cohost-rejected".equals(type)
                || "cohost-offer".equals(type)
                || "cohost-answer".equals(type)
                || "cohost-ice-candidate".equals(type)
                || "cohost-ended".equals(type)
                || "pk-invite".equals(type)
                || "pk-accepted".equals(type)
                || "pk-rejected".equals(type)
                || "pk-offer".equals(type)
                || "pk-answer".equals(type)
                || "pk-ice-candidate".equals(type)
                || "pk-ended".equals(type)
                || "leave".equals(type)
                || "heartbeat".equals(type);
    }

    public void handle(Channel channel, JSONObject body) {
        String type = body.getString("type");
        if (!StringUtils.hasText(type)) {
            log.warn("BrowserLive 消息缺少 type 字段");
            return;
        }

        try {
            switch (type) {
                case "join":
                    handleJoin(channel, body);
                    break;
                case "offer":
                case "answer":
                case "ice-candidate":
                    relay(channel, body, type);
                    break;
                case "cohost-request":
                    routeCohostRequest(channel, body);
                    break;
                case "cohost-accepted":
                case "cohost-rejected":
                case "cohost-offer":
                case "cohost-answer":
                case "cohost-ice-candidate":
                case "cohost-ended":
                    relayInteraction(channel, body, type);
                    break;
                case "pk-invite":
                    routePkInvite(channel, body);
                    break;
                case "pk-accepted":
                case "pk-rejected":
                case "pk-offer":
                case "pk-answer":
                case "pk-ice-candidate":
                case "pk-ended":
                    relayInteraction(channel, body, type);
                    break;
                case "leave":
                    handleLeave(channel);
                    break;
                case "heartbeat":
                    handleHeartbeat(channel);
                    break;
                default:
                    log.warn("未知 BrowserLive 消息类型: {}", type);
                    break;
            }
        } catch (Exception e) {
            log.error("处理 BrowserLive 消息失败, type={}, channel={}", type, channel.id(), e);
            throw e;
        }
    }

    public void handleDisconnect(Channel channel) {
        NettyBrowserLiveRegistry.SessionMeta meta = registry.remove(channel);
        if (meta == null || meta.getRoomId() == null) {
            return;
        }

        if (NettyBrowserLiveRegistry.SessionRole.BROADCASTER.equals(meta.getRole())) {
            for (String viewerSessionId : registry.getViewerSessionIds(meta.getRoomId())) {
                registry.send(viewerSessionId, message("broadcaster-offline", meta.getRoomId(), meta.getSessionId()));
            }
            liveLifecycleService.markLiveStopped(meta.getRoomId());
            return;
        }

        String broadcasterSessionId = registry.getBroadcasterSessionId(meta.getRoomId());
        if (StringUtils.hasText(broadcasterSessionId)) {
            registry.send(broadcasterSessionId, message("viewer-left", meta.getRoomId(), meta.getSessionId()));
        }
    }

    public boolean isBrowserLive(Integer roomId) {
        return roomId != null && registry.isBrowserLive(roomId);
    }

    private void handleJoin(Channel channel, JSONObject body) {
        Integer roomId = body.getInteger("roomId");
        String role = body.getString("role");
        if (roomId == null || !StringUtils.hasText(role)) {
            registry.send(channel.id().asLongText(), errorMessage(roomId, "加入直播失败，缺少房间号或角色信息"));
            return;
        }

        Integer userId = AttrUtil.getAttr(channel, AttrUtil.USER_ID);
        registry.register(channel, roomId, role, userId);
        if (NettyBrowserLiveRegistry.SessionRole.BROADCASTER.equals(role)) {
            liveLifecycleService.markLiveStarted(roomId, userId);
        }
        String sessionId = channel.id().asLongText();
        registry.send(sessionId, joinedMessage(roomId, role, sessionId));

        if (NettyBrowserLiveRegistry.SessionRole.VIEWER.equals(role)) {
            String broadcasterSessionId = registry.getBroadcasterSessionId(roomId);
            if (StringUtils.hasText(broadcasterSessionId)) {
                registry.send(sessionId, message("broadcaster-online", roomId, broadcasterSessionId));
                registry.send(broadcasterSessionId, message("viewer-joined", roomId, sessionId));
            } else {
                registry.send(sessionId, message("broadcaster-offline", roomId, null));
            }
            return;
        }

        for (String viewerSessionId : registry.getViewerSessionIds(roomId)) {
            registry.send(viewerSessionId, message("broadcaster-online", roomId, sessionId));
            registry.send(sessionId, message("viewer-joined", roomId, viewerSessionId));
        }
    }

    private void handleLeave(Channel channel) {
        NettyBrowserLiveRegistry.SessionMeta meta = registry.getMeta(channel.id().asLongText());
        if (meta == null) {
            return;
        }
        handleDisconnect(channel);
    }

    private void handleHeartbeat(Channel channel) {
        NettyBrowserLiveRegistry.SessionMeta meta = registry.getMeta(channel.id().asLongText());
        Integer roomId = meta == null ? null : meta.getRoomId();
        registry.send(channel.id().asLongText(), message("heartbeat-ack", roomId, channel.id().asLongText()));
    }

    private void relay(Channel channel, JSONObject body, String type) {
        NettyBrowserLiveRegistry.SessionMeta meta = registry.getMeta(channel.id().asLongText());
        if (meta == null) {
            return;
        }

        String targetSessionId = body.getString("targetSessionId");
        if (!StringUtils.hasText(targetSessionId)) {
            registry.send(channel.id().asLongText(), errorMessage(meta.getRoomId(), "信令缺少目标会话"));
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("roomId", meta.getRoomId());
        payload.put("fromSessionId", channel.id().asLongText());
        payload.put("sdp", body.get("sdp"));
        payload.put("candidate", body.get("candidate"));
        registry.send(targetSessionId, payload);
    }

    private void routeCohostRequest(Channel channel, JSONObject body) {
        NettyBrowserLiveRegistry.SessionMeta meta = registry.getMeta(channel.id().asLongText());
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

        Map<String, Object> payload = interactionPayload(channel, meta, body, "cohost-request");
        registry.send(targetSessionId, payload);
    }

    private void routePkInvite(Channel channel, JSONObject body) {
        NettyBrowserLiveRegistry.SessionMeta meta = registry.getMeta(channel.id().asLongText());
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

        Map<String, Object> payload = interactionPayload(channel, meta, body, "pk-invite");
        payload.put("roomId", targetRoomId);
        payload.put("targetRoomId", targetRoomId);
        registry.send(targetSessionId, payload);
    }

    private void relayInteraction(Channel channel, JSONObject body, String type) {
        NettyBrowserLiveRegistry.SessionMeta meta = registry.getMeta(channel.id().asLongText());
        if (meta == null) {
            return;
        }

        String targetSessionId = body.getString("targetSessionId");
        if (!StringUtils.hasText(targetSessionId)) {
            registry.send(meta.getSessionId(), errorMessage(meta.getRoomId(), "互动信令缺少目标会话"));
            return;
        }

        Map<String, Object> payload = interactionPayload(channel, meta, body, type);
        registry.send(targetSessionId, payload);
    }

    private Map<String, Object> interactionPayload(Channel channel, NettyBrowserLiveRegistry.SessionMeta meta, JSONObject body, String type) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("roomId", meta.getRoomId());
        payload.put("fromRoomId", meta.getRoomId());
        payload.put("fromSessionId", channel.id().asLongText());
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

    private Map<String, Object> message(String type, Integer roomId, Object sessionId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("roomId", roomId);
        payload.put("sessionId", sessionId);
        return payload;
    }
}
