package cn.imhtb.live.modules.server.netty.service.impl;

import cn.imhtb.live.common.enums.LiveRoomStatusEnum;
import cn.imhtb.live.common.enums.StatusEnum;
import cn.imhtb.live.common.exception.BusinessException;
import cn.imhtb.live.common.utils.RedisUtil;
import cn.imhtb.live.mappers.MessageMapper;
import cn.imhtb.live.modules.live.service.IRoomIntimacyRankService;
import cn.imhtb.live.modules.server.RedisPrefix;
import cn.imhtb.live.modules.live.vo.RoomIntimacyRankRespVo;
import cn.imhtb.live.modules.server.netty.AttrUtil;
import cn.imhtb.live.modules.server.netty.assembly.WsMsgAssembly;
import cn.imhtb.live.modules.server.netty.domain.WsChannelExtraInfoDTO;
import cn.imhtb.live.modules.server.netty.domain.req.ChatMsgReq;
import cn.imhtb.live.modules.server.netty.domain.req.WsEnterReqDTO;
import cn.imhtb.live.modules.server.netty.domain.resp.ChatMsgRespDTO;
import cn.imhtb.live.modules.server.netty.domain.resp.GiftMsgRespDTO;
import cn.imhtb.live.modules.server.netty.domain.resp.GuardViolationRespDTO;
import cn.imhtb.live.modules.server.netty.domain.resp.WsMsgRespDTO;
import cn.imhtb.live.modules.server.netty.service.IRoomChatService;
import cn.imhtb.live.modules.user.service.IUserService;
import cn.imhtb.live.pojo.database.Message;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.pojo.database.User;
import cn.imhtb.live.service.IRoomService;
import com.alibaba.fastjson.JSON;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author pinteh
 * @date 2023/6/4
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RoomChatServiceImpl implements IRoomChatService {

    private final IUserService userService;

    private final MessageMapper messageMapper;

    private final IRoomIntimacyRankService roomIntimacyRankService;

    private final IRoomService roomService;

    private final RedisUtil redisUtil;

    private final JdbcTemplate jdbcTemplate;

    private final cn.imhtb.live.modules.live.service.IUserLevelService userLevelService;

    /**
     * 所有的会话信息维护
     */
    private static final ConcurrentHashMap<Channel, WsChannelExtraInfoDTO> ONLINE_ALL = new ConcurrentHashMap<>();
    /**
     * 房间会话维护
     */
    private static final ConcurrentHashMap<Integer, CopyOnWriteArraySet<Channel>> ONLINE_ROOM = new ConcurrentHashMap<>();
    /**
     * 用户会话维护
     */
    private static final ConcurrentHashMap<Integer, CopyOnWriteArraySet<Channel>> ONLINE_USER = new ConcurrentHashMap<>();
    /**
     * 禁言记录: key = "roomId:userId", value = 禁言到期时间戳 (System.currentTimeMillis() + duration)
     */
    private static final ConcurrentHashMap<String, Long> MUTED_USERS = new ConcurrentHashMap<>();


    @Override
    public long getRoomUserSize(Integer roomId) {
        CopyOnWriteArraySet<Channel> channels = ONLINE_ROOM.get(roomId);
        if (channels == null){
            return 0L;
        }
        return channels.stream()
                .filter(channel -> {
                    WsChannelExtraInfoDTO extra = ONLINE_ALL.get(channel);
                    return Objects.isNull(extra) || !extra.isAnchorMonitor(roomId);
                })
                .count();
    }

    @Override
    public void enter(Channel channel, String data) {
//        log.info("chanel = {} , 进入房间:{}", channel.id(), data);
        WsEnterReqDTO wsEnterReqDTO = JSON.parseObject(data, WsEnterReqDTO.class);
        Integer roomId = wsEnterReqDTO.getRoomId();
        if (Objects.isNull(roomId)) {
            return;
        }
        Room room = roomService.getById(roomId);
        if (!isRoomAvailable(room)) {
            ChatMsgRespDTO chatMsgRespDTO = new ChatMsgRespDTO();
            chatMsgRespDTO.setNickname("系统消息");
            chatMsgRespDTO.setText("直播间不存在或已不可用");
            this.sendMessage(channel, WsMsgAssembly.buildChat(chatMsgRespDTO));
            return;
        }

        // 游客登录处理
        WsChannelExtraInfoDTO channelExtraInfoDto = getChannelExtraInfo(channel).addRoomId(roomId);
        channelExtraInfoDto.markAnchorMonitor(roomId, Boolean.TRUE.equals(wsEnterReqDTO.getAnchorMonitor()));
        ONLINE_ROOM.putIfAbsent(roomId, new CopyOnWriteArraySet<>());
        ONLINE_ROOM.get(roomId).add(channel);

        // 会员登录处理
        Integer userId = AttrUtil.getAttr(channel, AttrUtil.USER_ID);
        if (Objects.nonNull(userId)) {
            ONLINE_USER.putIfAbsent(userId, new CopyOnWriteArraySet<>());
            ONLINE_USER.get(userId).add(channel);
            channelExtraInfoDto.setUserId(userId);
//            AttrUtil.setAttr(channel, AttrUtil.USER_ID, userId);
        }

        if (!Boolean.TRUE.equals(wsEnterReqDTO.getAnchorMonitor())
                && Objects.nonNull(room)
                && Objects.equals(room.getStatus(), LiveRoomStatusEnum.LIVING.getCode())) {
            String viewerKey = Objects.nonNull(userId) ? "u:" + userId : "c:" + channel.id().asLongText();
            redisUtil.add(String.format(RedisPrefix.LIVE_VIEWER_SET_KEY, roomId), viewerKey);
        }

//        this.sendMessage(channel, WsMsgAssembly.buildWelcome("欢迎进入直播间~"));
        ChatMsgRespDTO chatMsgRespDTO = new ChatMsgRespDTO();
        chatMsgRespDTO.setNickname("系统消息");
        chatMsgRespDTO.setText("欢迎进入直播间~");
        this.sendMessage(channel, WsMsgAssembly.buildChat(chatMsgRespDTO));
    }

    private WsChannelExtraInfoDTO getChannelExtraInfo(Channel channel) {
        WsChannelExtraInfoDTO extraInfo = ONLINE_ALL.getOrDefault(channel, WsChannelExtraInfoDTO.init());
        WsChannelExtraInfoDTO old = ONLINE_ALL.putIfAbsent(channel, extraInfo);
        return Objects.isNull(old) ? extraInfo : old;
    }

    @Override
    public void exit(Channel channel) {
//        log.info("chanel = {}，关闭", channel.id());

        channel.close();
        WsChannelExtraInfoDTO channelExtraInfoDto = ONLINE_ALL.get(channel);
        if (Objects.nonNull(channelExtraInfoDto)) {
            Set<Integer> roomIds = channelExtraInfoDto.getRoomIds();
            for (Integer roomId : roomIds) {
                CopyOnWriteArraySet<Channel> channels = ONLINE_ROOM.get(roomId);
                if (channels != null) {
                    channels.remove(channel);
                }
                if (!channelExtraInfoDto.isAnchorMonitor(roomId)) {
                    Integer userIdForViewer = channelExtraInfoDto.getUserId();
                    String viewerKey = Objects.nonNull(userIdForViewer) ? "u:" + userIdForViewer : "c:" + channel.id().asLongText();
                    redisUtil.removeSetMember(String.format(RedisPrefix.LIVE_VIEWER_SET_KEY, roomId), viewerKey);
                }
            }
            Integer userId = channelExtraInfoDto.getUserId();
            if (Objects.nonNull(userId)) {
                CopyOnWriteArraySet<Channel> channels = ONLINE_USER.get(userId);
                if (channels != null) {
                    channels.remove(channel);
                }
            }
            ONLINE_ALL.remove(channel);
        }
    }

    @Override
    public void sendMessage(String msg, Integer roomId, Integer userId) {
        CopyOnWriteArraySet<Channel> channels = ONLINE_ROOM.get(roomId);
        if (Objects.nonNull(channels)) {
            for (Channel channel : channels) {
                WsChannelExtraInfoDTO extra = ONLINE_ALL.get(channel);
                if (Objects.nonNull(extra) && userId.equals(extra.getUserId())) {
                    continue;
                }
                sendMessage(channel, WsMsgAssembly.buildWelcome(msg));
            }
        }
    }

    @Override
    public void sendChatMsg(ChatMsgReq chatMsgReq) {
        if (chatMsgReq == null || chatMsgReq.getRoomId() == null || StringUtils.isBlank(chatMsgReq.getText())) {
            throw new BusinessException("弹幕内容不能为空");
        }
        User userInfo = userService.getUserInfo();
        if (userInfo == null || userInfo.getId() == null) {
            throw new BusinessException("请先登录后再发送弹幕");
        }
        Integer userId = userInfo.getId();
        Integer roomId = chatMsgReq.getRoomId();
        Room room = roomService.getById(roomId);
        if (!isRoomAvailable(room)) {
            throw new BusinessException("直播间不存在或已不可用");
        }
        String text = chatMsgReq.getText().trim();

        // 检查是否被禁言
        String muteKey = roomId + ":" + userId;
        Long muteExpiry = MUTED_USERS.get(muteKey);
        if (muteExpiry != null) {
            if (System.currentTimeMillis() < muteExpiry) {
                long remaining = (muteExpiry - System.currentTimeMillis()) / 1000;
                sendToUserInRoom(roomId, userId, WsMsgAssembly.buildMuteNotify("你已被禁言，剩余 " + remaining + " 秒"));
                return;
            } else {
                MUTED_USERS.remove(muteKey);
            }
        }

        CopyOnWriteArraySet<Channel> channels = ONLINE_ROOM.get(roomId);
        if (Objects.nonNull(channels)){
            for (Channel channel : channels) {
                ChatMsgRespDTO chatMsgRespDTO = new ChatMsgRespDTO();
                chatMsgRespDTO.setFromUserId(userId);
                chatMsgRespDTO.setNickname(userInfo.getNickname());
                chatMsgRespDTO.setText(text);
                sendMessage(channel, WsMsgAssembly.buildChat(chatMsgRespDTO));
            }
        }
        // 保存消息
        if (messageTableExists()) {
            Message message = new Message();
            message.setRoomId(roomId);
            message.setFromUid(userId);
            message.setContent(text);
            message.setStatus(0);
            message.setType(1);
            messageMapper.insert(message);
        }
        roomIntimacyRankService.addChatIntimacy(roomId, userId);
        broadcastIntimacyRank(roomId);
        // 发言加经验
        try { userLevelService.addExp(userId, 5); } catch (Exception ignored) {}
    }

    @Override
    public void sendGiftMsg(String msg, Integer roomId, Integer userId, Integer giftId, String giftName, Integer number, String senderName) {
        CopyOnWriteArraySet<Channel> channels = ONLINE_ROOM.get(roomId);
        if (Objects.nonNull(channels)) {
            for (Channel channel : channels) {
                ChatMsgRespDTO chatMsgRespDTO = new ChatMsgRespDTO();
//                chatMsgRespDTO.setFromUserId(userId);
                chatMsgRespDTO.setNickname("系统消息");
                chatMsgRespDTO.setText(msg);
                sendMessage(channel, WsMsgAssembly.buildChat(chatMsgRespDTO));
                // 不发送给当前赠送的用户
//                WsChannelExtraInfoDTO extra = ALL_ONLINE.get(channel);
//                if (Objects.nonNull(extra) && userId.equals(extra.getUserId())) {
//                    continue;
//                }
                sendMessage(channel, WsMsgAssembly.buildGift(GiftMsgRespDTO.builder()
                                .giftName(giftName)
                                .giftId(giftId)
                                .number(number)
                                .senderId(userId)
                                .senderName(senderName)
                                .text(msg)
                                .build()));
            }
        }
        broadcastIntimacyRank(roomId);
    }

    @Override
    public void broadcastIntimacyRank(Integer roomId) {
        CopyOnWriteArraySet<Channel> channels = ONLINE_ROOM.get(roomId);
        if (Objects.isNull(channels) || channels.isEmpty()) {
            return;
        }
       List<RoomIntimacyRankRespVo> ranks = roomIntimacyRankService.getTopRanks(roomId);
        for (Channel channel : channels) {
            sendMessage(channel, WsMsgAssembly.buildIntimacyRank(ranks));
        }
    }

    private void sendMessage(Channel channel, WsMsgRespDTO<?> wsMsgRespDTO) {
        if (channel != null && channel.isActive() && channel.isWritable()) {
            channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(wsMsgRespDTO)));
        }
    }

    @Override
    public void sendGuardViolation(Integer roomId, GuardViolationRespDTO data) {
        CopyOnWriteArraySet<Channel> channels = ONLINE_ROOM.get(roomId);
        if (Objects.nonNull(channels)) {
            for (Channel channel : channels) {
                sendMessage(channel, WsMsgAssembly.buildGuardViolation(data));
            }
        }
    }

    @Override
    public void sendToUser(Integer userId, cn.imhtb.live.modules.server.netty.domain.resp.WsMsgRespDTO<?> message) {
        CopyOnWriteArraySet<Channel> channels = ONLINE_USER.get(userId);
        if (Objects.nonNull(channels)) {
            for (Channel channel : channels) {
                sendMessage(channel, message);
            }
        }
    }

    @Override
    public void muteUser(Integer roomId, Integer targetUserId, Integer durationSeconds) {
        String muteKey = roomId + ":" + targetUserId;
        long expiry = System.currentTimeMillis() + (long) durationSeconds * 1000;
        MUTED_USERS.put(muteKey, expiry);
        sendToUserInRoom(roomId, targetUserId, WsMsgAssembly.buildMuteNotify("你已被房管禁言 " + durationSeconds + " 秒"));

        // 广播禁言消息到房间
        CopyOnWriteArraySet<Channel> channels = ONLINE_ROOM.get(roomId);
        if (channels != null) {
            for (Channel ch : channels) {
                sendMessage(ch, WsMsgAssembly.buildChat(
                        new cn.imhtb.live.modules.server.netty.domain.resp.ChatMsgRespDTO() {{
                            setNickname("系统消息");
                            setText("用户 " + targetUserId + " 已被禁言 " + durationSeconds + " 秒");
                        }}
                ));
            }
        }
    }

    @Override
    public void kickUser(Integer roomId, Integer targetUserId) {
        CopyOnWriteArraySet<Channel> userChannels = ONLINE_USER.get(targetUserId);
        if (userChannels != null) {
            for (Channel ch : userChannels) {
                // 检查这个 channel 是否在目标房间中
                WsChannelExtraInfoDTO extra = ONLINE_ALL.get(ch);
                if (extra != null && extra.getRoomIds().contains(roomId)) {
                    sendMessage(ch, WsMsgAssembly.buildKickNotify("你已被房管踢出直播间"));
                    // 从房间中移除但不关闭连接
                    CopyOnWriteArraySet<Channel> roomChannels = ONLINE_ROOM.get(roomId);
                    if (roomChannels != null) {
                        roomChannels.remove(ch);
                    }
                    extra.getRoomIds().remove(roomId);
                }
            }
        }
        // 广播踢出消息
        CopyOnWriteArraySet<Channel> channels = ONLINE_ROOM.get(roomId);
        if (channels != null) {
            for (Channel ch : channels) {
                sendMessage(ch, WsMsgAssembly.buildChat(
                        new cn.imhtb.live.modules.server.netty.domain.resp.ChatMsgRespDTO() {{
                            setNickname("系统消息");
                            setText("用户 " + targetUserId + " 已被踢出直播间");
                        }}
                ));
            }
        }
    }

    private boolean messageTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                "message"
        );
        return count != null && count > 0;
    }

    private boolean isRoomAvailable(Room room) {
        return room != null && !Objects.equals(room.getDisabled(), StatusEnum.NO.getCode());
    }

    private void sendToUserInRoom(Integer roomId, Integer userId, WsMsgRespDTO<?> message) {
        CopyOnWriteArraySet<Channel> channels = ONLINE_USER.get(userId);
        if (Objects.isNull(channels)) {
            return;
        }
        for (Channel channel : channels) {
            WsChannelExtraInfoDTO extra = ONLINE_ALL.get(channel);
            if (extra != null && extra.getRoomIds().contains(roomId)) {
                sendMessage(channel, message);
            }
        }
    }

}
