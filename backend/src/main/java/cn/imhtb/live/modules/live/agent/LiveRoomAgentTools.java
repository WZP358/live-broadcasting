package cn.imhtb.live.modules.live.agent;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.enums.LiveRoomStatusEnum;
import cn.imhtb.live.modules.live.vo.RoomRespVo;
import cn.imhtb.live.modules.live.webrtc.BrowserLiveRegistry;
import cn.imhtb.live.modules.server.netty.live.NettyBrowserLiveRegistry;
import cn.imhtb.live.modules.server.netty.service.IRoomChatService;
import cn.imhtb.live.pojo.vo.RoomExtraInfoResp;
import cn.imhtb.live.service.IRoomService;
import com.alibaba.fastjson.JSON;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LiveRoomAgentTools {

    private final IRoomService roomService;
    private final IRoomChatService roomChatService;
    private final BrowserLiveRegistry browserLiveRegistry;
    private final NettyBrowserLiveRegistry nettyBrowserLiveRegistry;

    @Tool(name = "get_live_room_audience_stats", value = {"查询指定直播间当前实时在线人数、观众数、主播音视频连接状态和关注数。"})
    public String getLiveRoomAudienceStats(@P(value = "直播间ID", required = true) Integer roomId) {
        Map<String, Object> result = baseResult("get_live_room_audience_stats", roomId);
        if (!isValidRoomId(roomId)) {
            result.put("success", false);
            result.put("message", "缺少有效的直播间ID。");
            return JSON.toJSONString(result);
        }

        RoomRespVo room = roomService.getRoomInfo(roomId);
        if (room == null) {
            result.put("success", false);
            result.put("message", "直播间不存在。");
            return JSON.toJSONString(result);
        }

        long chatOnlineCount = roomChatService.getRoomUserSize(roomId);
        int springSignalViewers = browserLiveRegistry.getViewerCount(roomId);
        int nettySignalViewers = nettyBrowserLiveRegistry.getViewerCount(roomId);
        boolean springAnchorOnline = browserLiveRegistry.hasBroadcaster(roomId);
        boolean nettyAnchorOnline = nettyBrowserLiveRegistry.hasBroadcaster(roomId);
        int signalViewerCount = springSignalViewers + nettySignalViewers;
        boolean anchorOnline = springAnchorOnline || nettyAnchorOnline;
        int signalParticipantCount = signalViewerCount + (anchorOnline ? 1 : 0);
        long displayOnlineCount = chatOnlineCount > 0 ? chatOnlineCount : signalParticipantCount;

        result.put("success", true);
        result.put("room", roomSummary(room));
        result.put("onlineCount", displayOnlineCount);
        result.put("chatOnlineCount", chatOnlineCount);
        result.put("signalViewerCount", signalViewerCount);
        result.put("signalParticipantCount", signalParticipantCount);
        result.put("anchorOnline", anchorOnline);
        result.put("source", "IRoomChatService.getRoomUserSize + BrowserLiveRegistry + NettyBrowserLiveRegistry");
        result.put("followCount", readFollowCount(roomId));
        return JSON.toJSONString(result);
    }

    @Tool(name = "get_live_room_detail", value = {"查询指定直播间的标题、主播、分类、状态、简介、是否有可播放直播流和关注数。"})
    public String getLiveRoomDetail(@P(value = "直播间ID", required = true) Integer roomId) {
        Map<String, Object> result = baseResult("get_live_room_detail", roomId);
        if (!isValidRoomId(roomId)) {
            result.put("success", false);
            result.put("message", "缺少有效的直播间ID。");
            return JSON.toJSONString(result);
        }

        RoomRespVo room = roomService.getRoomInfo(roomId);
        if (room == null) {
            result.put("success", false);
            result.put("message", "直播间不存在。");
            return JSON.toJSONString(result);
        }

        result.put("success", true);
        result.put("room", roomSummary(room));
        result.put("introduce", room.getIntroduce());
        result.put("hasPlayableStream", Boolean.TRUE.equals(room.getBrowserLive()) || room.getPullUrl() != null);
        result.put("followCount", readFollowCount(roomId));
        return JSON.toJSONString(result);
    }

    @Tool(name = "list_living_rooms", value = {"查询当前正在直播的直播间列表，可用于回答现在有哪些直播间在开播。"})
    public String listLivingRooms(@P(value = "返回数量，默认5，最大10", required = false) Integer limit) {
        int size = limit == null ? 5 : Math.max(1, Math.min(limit, 10));
        PageData<RoomRespVo> page = roomService.getLivingRooms(null, 1, size);
        List<Map<String, Object>> rooms = new ArrayList<>();
        if (page.getList() != null) {
            for (RoomRespVo room : page.getList()) {
                Map<String, Object> summary = roomSummary(room);
                summary.put("onlineCount", roomChatService.getRoomUserSize(room.getId()));
                rooms.add(summary);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tool", "list_living_rooms");
        result.put("success", true);
        result.put("total", page.getTotal());
        result.put("returned", rooms.size());
        result.put("rooms", rooms);
        result.put("source", "IRoomService.getLivingRooms");
        return JSON.toJSONString(result);
    }

    private Map<String, Object> baseResult(String tool, Integer roomId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tool", tool);
        result.put("roomId", roomId);
        return result;
    }

    private Map<String, Object> roomSummary(RoomRespVo room) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("roomId", room.getId());
        summary.put("title", room.getTitle());
        summary.put("status", room.getStatus());
        summary.put("statusText", statusText(room.getStatus()));
        summary.put("browserLive", Boolean.TRUE.equals(room.getBrowserLive()));
        if (room.getUserInfo() != null) {
            summary.put("anchorUserId", room.getUserInfo().getId());
            summary.put("anchorName", room.getUserInfo().getName());
            summary.put("anchorAvatar", room.getUserInfo().getAvatar());
        }
        if (room.getCategoryInfo() != null) {
            summary.put("categoryId", room.getCategoryInfo().getId());
            summary.put("categoryName", room.getCategoryInfo().getName());
        }
        return summary;
    }

    private Long readFollowCount(Integer roomId) {
        RoomExtraInfoResp extraInfo = roomService.getExtraInfo(null, roomId);
        return extraInfo == null || extraInfo.getFollowCount() == null ? 0L : extraInfo.getFollowCount();
    }

    private boolean isValidRoomId(Integer roomId) {
        return roomId != null && roomId > 0;
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        if (status == LiveRoomStatusEnum.LIVING.getCode()) {
            return "正在直播";
        }
        if (status == LiveRoomStatusEnum.STOP.getCode()) {
            return "未开播";
        }
        if (status == LiveRoomStatusEnum.BANNING.getCode()) {
            return "已封禁";
        }
        if (status == LiveRoomStatusEnum.UN_AUTH.getCode()) {
            return "未认证";
        }
        return "未知";
    }
}
