package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.mappers.PrivateMessageMapper;
import cn.imhtb.live.mappers.UserMapper;
import cn.imhtb.live.pojo.database.PrivateMessage;
import cn.imhtb.live.pojo.database.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Api(tags = "私信接口")
@RestController
@RequestMapping("/api/v1/pm")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PrivateMessageController {

    private final PrivateMessageMapper pmMapper;
    private final UserMapper userMapper;

    @ApiOperation("获取与某用户的对话")
    @GetMapping("/conversation")
    public ApiResponse<PageData<PrivateMessage>> conversation(@RequestParam Integer withUserId,
                                                               @RequestParam(defaultValue = "1") Integer page,
                                                               @RequestParam(defaultValue = "20") Integer limit) {
        Integer myId = UserHolder.getUserId();
        Page<PrivateMessage> pg = pmMapper.selectPage(new Page<>(page, limit),
                new LambdaQueryWrapper<PrivateMessage>()
                        .and(w -> w.eq(PrivateMessage::getFromUserId, myId).eq(PrivateMessage::getToUserId, withUserId)
                                .or(w2 -> w2.eq(PrivateMessage::getFromUserId, withUserId).eq(PrivateMessage::getToUserId, myId)))
                        .orderByDesc(PrivateMessage::getCreateTime));
        PageData<PrivateMessage> result = new PageData<>();
        result.setTotal(pg.getTotal());
        result.setList(pg.getRecords());
        return ApiResponse.ofSuccess(result);
    }

    @ApiOperation("发送私信")
    @PostMapping("/send")
    public ApiResponse<Boolean> send(@RequestBody SendReq req) {
        PrivateMessage pm = new PrivateMessage();
        pm.setFromUserId(UserHolder.getUserId());
        pm.setToUserId(req.getToUserId());
        pm.setContent(req.getContent());
        pm.setIsRead(0);
        pm.setCreateTime(LocalDateTime.now());
        return pmMapper.insert(pm) > 0 ? ApiResponse.ofSuccess(true) : ApiResponse.ofError("发送失败");
    }

    @ApiOperation("获取对话联系人列表")
    @GetMapping("/contacts")
    public ApiResponse<List<Map<String, Object>>> contacts() {
        Integer myId = UserHolder.getUserId();
        // 查询所有与我相关的私信，按发送者分组
        List<PrivateMessage> allMsgs = pmMapper.selectList(new LambdaQueryWrapper<PrivateMessage>()
                .and(w -> w.eq(PrivateMessage::getFromUserId, myId).or().eq(PrivateMessage::getToUserId, myId))
                .orderByDesc(PrivateMessage::getCreateTime));
        // 按对话伙伴去重，取最新消息
        Map<Integer, PrivateMessage> latestByPartner = new LinkedHashMap<>();
        for (PrivateMessage pm : allMsgs) {
            Integer partnerId = pm.getFromUserId().equals(myId) ? pm.getToUserId() : pm.getFromUserId();
            if (!latestByPartner.containsKey(partnerId)) {
                latestByPartner.put(partnerId, pm);
            }
        }
        // 批量获取用户信息
        List<Map<String, Object>> result = new ArrayList<>();
        if (!latestByPartner.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(latestByPartner.keySet());
            Map<Integer, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
            for (Map.Entry<Integer, PrivateMessage> entry : latestByPartner.entrySet()) {
                Integer partnerId = entry.getKey();
                PrivateMessage lastMsg = entry.getValue();
                User user = userMap.get(partnerId);
                Map<String, Object> contact = new HashMap<>();
                contact.put("userId", partnerId);
                contact.put("nickname", user != null ? user.getNickname() : "用户" + partnerId);
                contact.put("avatar", user != null ? user.getAvatar() : null);
                contact.put("lastMsg", lastMsg.getContent());
                contact.put("lastTime", lastMsg.getCreateTime());
                // 统计未读数
                Long unread = pmMapper.selectCount(new LambdaQueryWrapper<PrivateMessage>()
                        .eq(PrivateMessage::getFromUserId, partnerId)
                        .eq(PrivateMessage::getToUserId, myId)
                        .eq(PrivateMessage::getIsRead, 0));
                contact.put("unread", unread);
                result.add(contact);
            }
        }
        return ApiResponse.ofSuccess(result);
    }

    @ApiOperation("标记已读")
    @PostMapping("/read")
    public ApiResponse<Boolean> markRead(@RequestBody ReadReq req) {
        pmMapper.update(null, new LambdaUpdateWrapper<PrivateMessage>()
                .eq(PrivateMessage::getToUserId, UserHolder.getUserId())
                .eq(PrivateMessage::getFromUserId, req.getFromUserId())
                .eq(PrivateMessage::getIsRead, 0)
                .set(PrivateMessage::getIsRead, 1));
        return ApiResponse.ofSuccess(true);
    }

    @Data public static class SendReq { private Integer toUserId; private String content; }
    @Data public static class ReadReq { private Integer fromUserId; }
}
