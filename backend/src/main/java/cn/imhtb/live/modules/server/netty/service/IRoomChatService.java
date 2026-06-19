package cn.imhtb.live.modules.server.netty.service;

import cn.imhtb.live.modules.server.netty.domain.req.ChatMsgReq;
import io.netty.channel.Channel;

/**
 * @author pinteh
 * @date 2023/6/4
 */
public interface IRoomChatService {

    /**
     * 获取房间在线人数
     *
     * @param roomId 房间id
     * @return long
     */
    long getRoomUserSize(Integer roomId);

    /**
     * 进入
     *
     * @param channel 通道
     * @param data    数据
     */
    void enter(Channel channel, String data);

    /**
     * 退出
     *
     * @param channel 通道
     */
    void exit(Channel channel);

    /**
     * 发送消息
     *
     * @param msg    消息
     * @param roomId 房间id
     * @param userId 用户id
     */
    void sendMessage(String msg, Integer roomId, Integer userId);


    /**
     * 发送消息
     *
     * @param chatMsgReq 发送消息
     */
    void sendChatMsg(ChatMsgReq chatMsgReq);

    /**
     * 送礼物
     *
     * @param msg        消息
     * @param roomId     房间id
     * @param userId     当前用户id
     * @param giftId     礼物id
     * @param giftName   礼物名称
     * @param number     礼物数量
     * @param senderName 送礼用户昵称
     */
    void sendGiftMsg(String msg, Integer roomId, Integer userId, Integer giftId, String giftName, Integer number, String senderName);

    /**
     * 广播直播间亲密榜
     *
     * @param roomId 房间id
     */
    void broadcastIntimacyRank(Integer roomId);

    /**
     * 发送违规通知
     *
     * @param roomId 房间id
     * @param data   违规数据
     */
    void sendGuardViolation(Integer roomId, cn.imhtb.live.modules.server.netty.domain.resp.GuardViolationRespDTO data);

    /**
     * 发送消息给指定用户
     *
     * @param userId  用户id
     * @param message 消息
     */
    void sendToUser(Integer userId, cn.imhtb.live.modules.server.netty.domain.resp.WsMsgRespDTO<?> message);

    /**
     * 禁言用户
     *
     * @param roomId          房间id
     * @param targetUserId    被禁言用户id
     * @param durationSeconds 禁言时长(秒)
     */
    void muteUser(Integer roomId, Integer targetUserId, Integer durationSeconds);

    /**
     * 踢出用户
     *
     * @param roomId       房间id
     * @param targetUserId 被踢出用户id
     */
    void kickUser(Integer roomId, Integer targetUserId);

}
