package cn.imhtb.live.modules.live.service.impl;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.mappers.UserMapper;
import cn.imhtb.live.modules.live.service.ISearchService;
import cn.imhtb.live.modules.live.vo.SearchResultVO;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.pojo.database.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class SearchServiceImpl implements ISearchService {

    private final RoomMapper roomMapper;
    private final UserMapper userMapper;

    @Override
    public PageData<SearchResultVO> searchRooms(String keyword, Integer page, Integer limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            PageData<SearchResultVO> empty = new PageData<>();
            empty.setTotal(0L);
            empty.setList(Collections.emptyList());
            return empty;
        }

        String kw = keyword.trim();
        String likeKw = "%" + kw + "%";

        // 搜索匹配的房间（标题模糊匹配，只查启用且直播中的房间优先）
        LambdaQueryWrapper<Room> roomWrapper = new LambdaQueryWrapper<Room>()
                .like(Room::getTitle, kw)
                .eq(Room::getDisabled, 0)
                .orderByDesc(Room::getStatus)
                .orderByDesc(Room::getId);
        Page<Room> roomPage = roomMapper.selectPage(new Page<>(page, limit), roomWrapper);

        // 如果房间结果不够一页，用主播昵称补搜
        if (roomPage.getRecords().size() < limit) {
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<User>()
                    .like(User::getNickname, kw)
                    .eq(User::getDisabled, 0);
            List<User> matchedUsers = userMapper.selectList(userWrapper);
            Set<Integer> existingRoomIds = roomPage.getRecords().stream()
                    .map(Room::getId).collect(Collectors.toSet());

            if (!matchedUsers.isEmpty()) {
                List<Integer> userIds = matchedUsers.stream()
                        .map(User::getId).collect(Collectors.toList());
                LambdaQueryWrapper<Room> byUserWrapper = new LambdaQueryWrapper<Room>()
                        .in(Room::getUserId, userIds)
                        .eq(Room::getDisabled, 0)
                        .notIn(!existingRoomIds.isEmpty(), Room::getId, existingRoomIds)
                        .orderByDesc(Room::getStatus)
                        .orderByDesc(Room::getId);
                List<Room> userRooms = roomMapper.selectList(byUserWrapper);
                roomPage.getRecords().addAll(userRooms);
                roomPage.setTotal(roomPage.getTotal() + userRooms.size());
            }
        }

        // 组装结果
        Set<Integer> anchorIds = roomPage.getRecords().stream()
                .map(Room::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        final Map<Integer, User> userMap;
        if (!anchorIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(anchorIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        } else {
            userMap = Collections.emptyMap();
        }

        List<SearchResultVO> list = roomPage.getRecords().stream()
                .map(room -> {
                    SearchResultVO vo = new SearchResultVO();
                    vo.setRoomId(room.getId());
                    vo.setRoomTitle(room.getTitle());
                    vo.setCover(room.getCover());
                    vo.setStatus(room.getStatus());
                    vo.setBrowserLive(false);
                    User anchor = userMap.get(room.getUserId());
                    vo.setAnchorName(anchor != null ? anchor.getNickname() : "主播");
                    vo.setAnchorAvatar(anchor != null ? anchor.getAvatar() : null);
                    return vo;
                })
                .collect(Collectors.toList());

        PageData<SearchResultVO> result = new PageData<>();
        result.setTotal(roomPage.getTotal());
        result.setList(list);
        return result;
    }
}
