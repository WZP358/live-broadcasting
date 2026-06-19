package cn.imhtb.live.modules.live.service.impl;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.enums.LiveRoomStatusEnum;
import cn.imhtb.live.common.enums.StatusEnum;
import cn.imhtb.live.mappers.CategoryMapper;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.mappers.UserMapper;
import cn.imhtb.live.modules.live.service.ISearchService;
import cn.imhtb.live.modules.live.vo.SearchResultVO;
import cn.imhtb.live.pojo.database.Category;
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
    private final CategoryMapper categoryMapper;

    @Override
    public PageData<SearchResultVO> searchRooms(String keyword, Integer categoryId, Integer page, Integer limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return emptyResult();
        }

        int currentPage = page == null || page < 1 ? 1 : page;
        int pageSize = limit == null || limit < 1 ? 12 : Math.min(limit, 50);
        String kw = keyword.trim();

        if (categoryId != null && !isEnabledCategory(categoryId)) {
            return emptyResult();
        }
        Set<Integer> enabledCategoryIds = resolveEnabledCategoryIds(categoryId);
        if (enabledCategoryIds.isEmpty()) {
            return emptyResult();
        }

        LambdaQueryWrapper<Room> roomWrapper = new LambdaQueryWrapper<Room>()
                .like(Room::getTitle, kw)
                .in(Room::getCategoryId, enabledCategoryIds)
                .eq(Room::getDisabled, StatusEnum.YES.getCode())
                .eq(Room::getStatus, LiveRoomStatusEnum.LIVING.getCode())
                .orderByDesc(Room::getStatus)
                .orderByDesc(Room::getId);
        Page<Room> roomPage = roomMapper.selectPage(new Page<>(currentPage, pageSize), roomWrapper);

        if (roomPage.getRecords().size() < pageSize) {
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<User>()
                    .like(User::getNickname, kw)
                    .eq(User::getDisabled, StatusEnum.YES.getCode());
            List<User> matchedUsers = userMapper.selectList(userWrapper);
            Set<Integer> existingRoomIds = roomPage.getRecords().stream()
                    .map(Room::getId).collect(Collectors.toSet());

            if (!matchedUsers.isEmpty()) {
                List<Integer> userIds = matchedUsers.stream()
                        .map(User::getId).collect(Collectors.toList());
                LambdaQueryWrapper<Room> byUserWrapper = new LambdaQueryWrapper<Room>()
                        .in(Room::getUserId, userIds)
                        .in(Room::getCategoryId, enabledCategoryIds)
                        .eq(Room::getDisabled, StatusEnum.YES.getCode())
                        .eq(Room::getStatus, LiveRoomStatusEnum.LIVING.getCode())
                        .notIn(!existingRoomIds.isEmpty(), Room::getId, existingRoomIds)
                        .orderByDesc(Room::getStatus)
                        .orderByDesc(Room::getId);
                List<Room> userRooms = roomMapper.selectList(byUserWrapper);
                int remaining = pageSize - roomPage.getRecords().size();
                List<Room> limitedUserRooms = userRooms.stream()
                        .limit(remaining)
                        .collect(Collectors.toList());
                roomPage.getRecords().addAll(limitedUserRooms);
                roomPage.setTotal(roomPage.getTotal() + userRooms.size());
            }
        }

        Set<Integer> anchorIds = roomPage.getRecords().stream()
                .map(Room::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        final Map<Integer, User> userMap;
        if (!anchorIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(anchorIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        } else {
            userMap = Collections.emptyMap();
        }

        Set<Integer> categoryIds = roomPage.getRecords().stream()
                .map(Room::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        final Map<Integer, Category> categoryMap;
        if (!categoryIds.isEmpty()) {
            categoryMap = categoryMapper.selectBatchIds(categoryIds).stream()
                    .filter(category -> Objects.equals(category.getStatus(), StatusEnum.YES.getCode()))
                    .collect(Collectors.toMap(Category::getId, category -> category, (a, b) -> a));
        } else {
            categoryMap = Collections.emptyMap();
        }

        List<SearchResultVO> list = roomPage.getRecords().stream()
                .filter(room -> categoryMap.containsKey(room.getCategoryId()))
                .map(room -> {
                    SearchResultVO vo = new SearchResultVO();
                    Category category = categoryMap.get(room.getCategoryId());
                    User anchor = userMap.get(room.getUserId());
                    String anchorName = anchor != null ? anchor.getNickname() : "主播";

                    vo.setId(room.getId());
                    vo.setTitle(room.getTitle());
                    vo.setRoomId(room.getId());
                    vo.setRoomTitle(room.getTitle());
                    vo.setCover(room.getCover());
                    vo.setStatus(room.getStatus());
                    vo.setBrowserLive(false);
                    vo.setAnchorName(anchorName);
                    vo.setAnchorAvatar(anchor != null ? anchor.getAvatar() : null);
                    vo.setCategoryId(room.getCategoryId());
                    vo.setCategoryName(category != null ? category.getName() : null);

                    SearchResultVO.UserInfoVO userInfo = new SearchResultVO.UserInfoVO();
                    userInfo.setId(room.getUserId());
                    userInfo.setName(anchorName);
                    userInfo.setAvatar(anchor != null ? anchor.getAvatar() : null);
                    vo.setUserInfo(userInfo);

                    if (category != null) {
                        SearchResultVO.CategoryInfoVO categoryInfo = new SearchResultVO.CategoryInfoVO();
                        categoryInfo.setId(category.getId());
                        categoryInfo.setName(category.getName());
                        vo.setCategoryInfo(categoryInfo);
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        PageData<SearchResultVO> result = new PageData<>();
        result.setTotal(roomPage.getTotal());
        result.setList(list);
        return result;
    }

    private boolean isEnabledCategory(Integer categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        return category != null && Objects.equals(category.getStatus(), StatusEnum.YES.getCode());
    }

    private Set<Integer> resolveEnabledCategoryIds(Integer categoryId) {
        if (categoryId != null) {
            return Set.of(categoryId);
        }
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, StatusEnum.YES.getCode()))
                .stream()
                .map(Category::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private PageData<SearchResultVO> emptyResult() {
        PageData<SearchResultVO> empty = new PageData<>();
        empty.setTotal(0L);
        empty.setList(Collections.emptyList());
        return empty;
    }
}
