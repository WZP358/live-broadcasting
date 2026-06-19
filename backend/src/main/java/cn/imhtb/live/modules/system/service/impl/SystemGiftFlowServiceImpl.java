package cn.imhtb.live.modules.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.enums.PresentRewardTypeEnum;
import cn.imhtb.live.mappers.PresentMapper;
import cn.imhtb.live.mappers.PresentRewardMapper;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.mappers.UserMapper;
import cn.imhtb.live.modules.system.model.SystemGiftFlowQuery;
import cn.imhtb.live.modules.system.model.SystemGiftFlowRecord;
import cn.imhtb.live.modules.system.model.SystemGiftFlowSummary;
import cn.imhtb.live.pojo.database.Present;
import cn.imhtb.live.pojo.database.PresentReward;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.pojo.database.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SystemGiftFlowServiceImpl implements cn.imhtb.live.modules.system.service.ISystemGiftFlowService {

    private final PresentRewardMapper rewardMapper;
    private final PresentMapper presentMapper;
    private final UserMapper userMapper;
    private final RoomMapper roomMapper;

    @Override
    public PageData<SystemGiftFlowRecord> page(SystemGiftFlowQuery query, Integer pageNo, Integer pageSize) {
        Page<PresentReward> page = rewardMapper.selectPage(
                new Page<>(normalizePageNo(pageNo), normalizePageSize(pageSize)),
                buildWrapper(query).orderByDesc(PresentReward::getCreateTime).orderByDesc(PresentReward::getId)
        );
        return new PageData<>(page.getTotal(), enrich(page.getRecords()));
    }

    @Override
    public SystemGiftFlowSummary summary(SystemGiftFlowQuery query) {
        List<PresentReward> rewards = rewardMapper.selectList(buildWrapper(query));
        SystemGiftFlowSummary summary = new SystemGiftFlowSummary();
        LocalDate today = LocalDate.now();

        for (PresentReward reward : rewards) {
            BigDecimal amount = amountOf(reward);
            summary.setTotalCount(summary.getTotalCount() + 1);
            summary.setTotalAmount(summary.getTotalAmount().add(amount));
            if (Objects.equals(reward.getType(), PresentRewardTypeEnum.VIDEO.getCode())) {
                summary.setVideoAmount(summary.getVideoAmount().add(amount));
            } else {
                summary.setLiveAmount(summary.getLiveAmount().add(amount));
            }
            if (reward.getCreateTime() != null && reward.getCreateTime().toLocalDate().equals(today)) {
                summary.setTodayCount(summary.getTodayCount() + 1);
                summary.setTodayAmount(summary.getTodayAmount().add(amount));
            }
        }
        return summary;
    }

    private LambdaQueryWrapper<PresentReward> buildWrapper(SystemGiftFlowQuery query) {
        SystemGiftFlowQuery safeQuery = query == null ? new SystemGiftFlowQuery() : query;
        return new LambdaQueryWrapper<PresentReward>()
                .eq(safeQuery.getRoomId() != null, PresentReward::getRoomId, safeQuery.getRoomId())
                .eq(safeQuery.getFromId() != null, PresentReward::getFromId, safeQuery.getFromId())
                .eq(safeQuery.getToId() != null, PresentReward::getToId, safeQuery.getToId())
                .eq(safeQuery.getPresentId() != null, PresentReward::getPresentId, safeQuery.getPresentId())
                .eq(safeQuery.getType() != null, PresentReward::getType, safeQuery.getType())
                .ge(safeQuery.getStartTime() != null, PresentReward::getCreateTime, safeQuery.getStartTime())
                .le(safeQuery.getEndTime() != null, PresentReward::getCreateTime, safeQuery.getEndTime());
    }

    private List<SystemGiftFlowRecord> enrich(List<PresentReward> rewards) {
        if (CollectionUtils.isEmpty(rewards)) {
            return Collections.emptyList();
        }

        Map<Integer, Present> presents = selectMap(rewards.stream()
                .map(PresentReward::getPresentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()), presentMapper::selectBatchIds, Present::getId);
        Map<Integer, User> users = selectMap(rewards.stream()
                .flatMap(reward -> Stream.of(reward.getFromId(), reward.getToId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()), userMapper::selectBatchIds, User::getId);
        Map<Integer, Room> rooms = selectMap(rewards.stream()
                .map(PresentReward::getRoomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()), roomMapper::selectBatchIds, Room::getId);

        return rewards.stream().map(reward -> {
            SystemGiftFlowRecord record = BeanUtil.toBean(reward, SystemGiftFlowRecord.class);
            Present present = presents.get(reward.getPresentId());
            if (present != null) {
                record.setPresentName(present.getName());
                record.setPresentIcon(present.getIcon());
            }
            User fromUser = users.get(reward.getFromId());
            if (fromUser != null) {
                record.setFromUserNickname(displayName(fromUser));
                record.setFromUserAvatar(fromUser.getAvatar());
            }
            User anchor = users.get(reward.getToId());
            if (anchor != null) {
                record.setAnchorNickname(displayName(anchor));
                record.setAnchorAvatar(anchor.getAvatar());
            }
            Room room = rooms.get(reward.getRoomId());
            if (room != null) {
                record.setRoomTitle(room.getTitle());
            }
            return record;
        }).collect(Collectors.toList());
    }

    private <T> Map<Integer, T> selectMap(Set<Integer> ids, Function<List<Integer>, List<T>> selector, Function<T, Integer> idGetter) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return selector.apply(List.copyOf(ids)).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(idGetter, Function.identity(), (left, right) -> left));
    }

    private String displayName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return "用户" + user.getId();
    }

    private BigDecimal amountOf(PresentReward reward) {
        return reward.getTotalPrice() == null ? BigDecimal.ZERO : reward.getTotalPrice();
    }

    private long normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    private long normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }
}
