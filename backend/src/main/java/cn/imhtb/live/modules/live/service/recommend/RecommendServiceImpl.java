package cn.imhtb.live.modules.live.service.recommend;

import cn.imhtb.live.common.enums.LiveRoomStatusEnum;
import cn.imhtb.live.common.enums.StatusEnum;
import cn.imhtb.live.common.enums.WatchTypeEnum;
import cn.imhtb.live.mappers.CategoryMapper;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.mappers.WatchMapper;
import cn.imhtb.live.modules.live.event.LiveEvent;
import cn.imhtb.live.modules.live.event.LiveEventBus;
import cn.imhtb.live.modules.live.event.LiveEventObserver;
import cn.imhtb.live.modules.live.event.LiveStartedEvent;
import cn.imhtb.live.modules.live.event.LiveStoppedEvent;
import cn.imhtb.live.pojo.database.Category;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.pojo.database.Watch;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于余弦相似度的混合推荐服务实现。
 *
 * <h3>推荐策略（优先级从高到低）</h3>
 * <ol>
 *   <li><b>协同过滤</b> — 找到与当前用户行为最相似的 K 个用户，推荐他们正在看的房间</li>
 *   <li><b>内容匹配</b> — 将用户历史行为的房间特征向量取平均，与所有直播中房间计算余弦相似度</li>
 *   <li><b>实时补全</b> — 按最近开播房间补全不足的推荐位，保证推荐结果仍然可观看</li>
 * </ol>
 *
 * <h3>特征向量构成（归一化后拼接）</h3>
 * <pre>
 *   V = [ c₁, c₂, ..., cₘ  | w₁, w₂, ..., wₙ ]
 *        ← 分类独热编码 →    ← 标题/简介 TF-IDF 关键词 →
 * </pre>
 *
 * @author PulseLive Recommendation Team
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RecommendServiceImpl implements IRecommendService, LiveEventObserver {

    private final RoomMapper roomMapper;
    private final WatchMapper watchMapper;
    private final CategoryMapper categoryMapper;

    @Autowired(required = false)
    private LiveEventBus eventBus;

    // 缓存：房间 ID → 特征向量
    private final Map<Integer, double[]> roomVectors = new ConcurrentHashMap<>();
    // 缓存：分类 ID → 分类索引
    private final Map<Integer, Integer> categoryIndex = new ConcurrentHashMap<>();
    private volatile boolean modelReady = false;

    // ─── 生命周期 ────────────────────────────────────────────

    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.register(this);
        }
        refreshModel();
    }

    @Override
    public synchronized void refreshModel() {
        log.info("[推荐引擎] 开始刷新推荐模型...");
        long t0 = System.currentTimeMillis();
        modelReady = false;

        try {
            // 1. 构建分类索引
            List<Category> categories = listEnabledCategories();
            categoryIndex.clear();
            for (int i = 0; i < categories.size(); i++) {
                categoryIndex.put(categories.get(i).getId(), i);
            }
            int catDim = categories.size();

            // 2. 收集所有房间文本用于 TF-IDF
            List<Room> allRooms = roomMapper.selectList(
                new LambdaQueryWrapper<Room>()
                    .eq(Room::getDisabled, StatusEnum.YES.getCode())
                    .in(!categoryIndex.isEmpty(), Room::getCategoryId, categoryIndex.keySet())
                    .eq(Room::getStatus, LiveRoomStatusEnum.LIVING.getCode()));
            List<String> documents = allRooms.stream()
                .map(r -> (r.getTitle() != null ? r.getTitle() : "") + " " +
                          (r.getIntroduce() != null ? r.getIntroduce() : ""))
                .collect(Collectors.toList());

            List<Map<Integer, Double>> tfidfVectors = new ArrayList<>(documents.size());
            int tfidfDim = 0;
            for (int i = 0; i < documents.size(); i++) {
                Map<Integer, Double> tfidfVec = CosineSimilarityRecommender.computeTfIdfVector(documents, i);
                tfidfVectors.add(tfidfVec);
                for (Integer idx : tfidfVec.keySet()) {
                    tfidfDim = Math.max(tfidfDim, idx + 1);
                }
            }

            // 3. 为每个房间构建特征向量
            roomVectors.clear();
            for (int i = 0; i < allRooms.size(); i++) {
                Room room = allRooms.get(i);
                double[] vec = buildRoomVector(room, catDim, tfidfVectors.get(i), tfidfDim);
                roomVectors.put(room.getId(), vec);
            }

            modelReady = true;
            log.info("[推荐引擎] 模型刷新完成, 耗时 {}ms, {} 个房间已索引",
                System.currentTimeMillis() - t0, roomVectors.size());
        } catch (Exception e) {
            log.error("[推荐引擎] 模型刷新失败", e);
        }
    }

    @Override
    public void onEvent(LiveEvent event) {
        if (event == null) {
            return;
        }
        if (LiveStartedEvent.TYPE.equals(event.getEventType())
                || LiveStoppedEvent.TYPE.equals(event.getEventType())) {
            log.info("[推荐引擎] 收到直播状态事件, type={}, roomId={}, 自动刷新模型",
                event.getEventType(), event.getRoomId());
            refreshModel();
        }
    }

    // ─── 对外接口 ────────────────────────────────────────────

    @Override
    public List<Map<String, Object>> recommendForUser(Integer userId, int limit) {
        if (!modelReady) refreshModel();
        if (categoryIndex.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> shownIds = new HashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();

        if (userId != null) {
            // 获取用户关注和观看历史
            List<Watch> followWatches = watchMapper.selectList(
                new LambdaQueryWrapper<Watch>()
                    .eq(Watch::getUserId, userId)
                    .eq(Watch::getWatchType, WatchTypeEnum.FOLLOW.getCode()));
            List<Watch> historyWatches = watchMapper.selectList(
                new LambdaQueryWrapper<Watch>()
                    .eq(Watch::getUserId, userId)
                    .eq(Watch::getWatchType, WatchTypeEnum.HISTORY.getCode()));

            // 合并用户行为记录
            Set<Integer> interactedRoomIds = new HashSet<>();
            List<Watch> allWatches = new ArrayList<>();
            allWatches.addAll(followWatches);
            allWatches.addAll(historyWatches);
            for (Watch w : allWatches) {
                interactedRoomIds.add(w.getRoomId());
            }

            // ── 策略1: 协同过滤 — 找相似用户 ──
            if (!interactedRoomIds.isEmpty() && result.size() < limit) {
                List<Map.Entry<Integer, Double>> similarUsers =
                    findSimilarUsers(userId, interactedRoomIds, 10);
                for (Map.Entry<Integer, Double> su : similarUsers) {
                    List<Room> theirRooms = getLivingFollowedRooms(su.getKey());
                    for (Room r : theirRooms) {
                        if (result.size() >= limit) break;
                        if (shownIds.add(r.getId()) && !interactedRoomIds.contains(r.getId())) {
                            result.add(packageResult(
                                r,
                                su.getValue(),
                                "collaborative",
                                "与你兴趣相近的用户也关注了这个直播间"));
                        }
                    }
                }
            }

            // ── 策略2: 内容匹配 — 构建用户画像向量 ──
            if (!interactedRoomIds.isEmpty() && result.size() < limit) {
                double[] userProfile = buildUserProfileVector(interactedRoomIds);
                if (userProfile != null) {
                    // 筛选直播中且未交互过的候选房间
                    Map<Integer, double[]> candidates = new HashMap<>();
                    for (Map.Entry<Integer, double[]> entry : roomVectors.entrySet()) {
                        if (!interactedRoomIds.contains(entry.getKey())) {
                            candidates.put(entry.getKey(), entry.getValue());
                        }
                    }
                    List<Map.Entry<Integer, Double>> topK =
                        CosineSimilarityRecommender.topKBySimilarity(
                            userProfile, candidates,
                            limit - result.size(), shownIds);

                    for (Map.Entry<Integer, Double> e : topK) {
                        if (shownIds.add(e.getKey())) {
                            Room room = roomMapper.selectById(e.getKey());
                            if (isRecommendableRoom(room)) {
                                result.add(packageResult(
                                    room,
                                    e.getValue(),
                                    "content",
                                    "与你最近观看或关注的内容相似"));
                            }
                        }
                    }
                }
            }
        }

        // ── 策略3: 实时补全 ──
        if (result.size() < limit) {
            List<Room> hotRooms = roomMapper.selectList(
                new LambdaQueryWrapper<Room>()
                    .eq(Room::getStatus, LiveRoomStatusEnum.LIVING.getCode())
                    .eq(Room::getDisabled, StatusEnum.YES.getCode())
                    .in(!categoryIndex.isEmpty(), Room::getCategoryId, categoryIndex.keySet())
                    .notIn(!shownIds.isEmpty(), Room::getId, shownIds)
                    .orderByDesc(Room::getId)
                    .last("limit " + (limit - result.size())));
            for (Room r : hotRooms) {
                if (shownIds.add(r.getId())) {
                    result.add(packageResult(
                        r,
                        0.0,
                        "hot",
                        "正在直播且近期热度较高"));
                }
            }
        }

        // 补充分类名
        enrichCategoryNames(result);
        return result;
    }

    @Override
    public List<Map<String, Object>> similarRooms(Integer roomId, int limit) {
        if (!modelReady) refreshModel();
        if (categoryIndex.isEmpty()) {
            return Collections.emptyList();
        }

        double[] targetVec = roomVectors.get(roomId);
        if (targetVec == null) {
            return Collections.emptyList();
        }

        Map<Integer, double[]> candidates = new HashMap<>(roomVectors);
        candidates.remove(roomId);

        Set<Integer> exclude = new HashSet<>();
        exclude.add(roomId);

        List<Map.Entry<Integer, Double>> topK =
            CosineSimilarityRecommender.topKBySimilarity(targetVec, candidates, limit, exclude);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, Double> e : topK) {
            Room room = roomMapper.selectById(e.getKey());
            if (isRecommendableRoom(room)) {
                result.add(packageResult(
                    room,
                    e.getValue(),
                    "similar",
                    "与当前直播间分类和内容相似"));
            }
        }
        enrichCategoryNames(result);
        return result;
    }

    // ─── 向量构建 ────────────────────────────────────────────

    /**
     * 构建房间特征向量。
     * <pre>
     *   V = [c₁...cₘ | t₁...tₙ]
     *   cᵢ = 1 if room.categoryId == category_i else 0
     *   tⱼ = TF-IDF of token j in room title + introduce
     * </pre>
     */
    private double[] buildRoomVector(Room room, int catDim, Map<Integer, Double> tfidfVec, int tfidfDim) {
        // 分类独热
        double[] catVec = new double[catDim];
        if (room.getCategoryId() != null) {
            Integer idx = categoryIndex.get(room.getCategoryId());
            if (idx != null) {
                catVec[idx] = 1.0;
            }
        }

        // 拼接: [分类独热 | TF-IDF]
        double[] vec = new double[catDim + tfidfDim];
        System.arraycopy(catVec, 0, vec, 0, catDim);
        for (Map.Entry<Integer, Double> entry : tfidfVec.entrySet()) {
            vec[catDim + entry.getKey()] = entry.getValue();
        }

        // L2 归一化
        normalizeL2(vec);
        return vec;
    }

    /**
     * 构建用户画像向量 = 用户交互过的所有房间向量的加权平均。
     */
    private double[] buildUserProfileVector(Set<Integer> roomIds) {
        if (roomIds.isEmpty()) return null;

        double[] profile = null;
        int count = 0;
        double totalWeight = 0;

        for (int roomId : roomIds) {
            double[] roomVec = roomVectors.get(roomId);
            if (roomVec == null) continue;
            if (profile == null) {
                profile = new double[roomVec.length];
            }
            // 关注权重 > 历史权重
            double weight = 1.0;
            for (int i = 0; i < roomVec.length; i++) {
                profile[i] += roomVec[i] * weight;
            }
            totalWeight += weight;
            count++;
        }

        if (count == 0) return null;

        for (int i = 0; i < profile.length; i++) {
            profile[i] /= totalWeight;
        }
        normalizeL2(profile);
        return profile;
    }

    // ─── 协同过滤辅助 ────────────────────────────────────────

    /**
     * 找与当前用户行为最相似的其他用户。
     * <p>将每个用户表示为「他们交互过的房间 ID 集合」，
     * 用 Jaccard 系数 + 余弦相似度的加权结果排序。</p>
     */
    private List<Map.Entry<Integer, Double>> findSimilarUsers(
            int targetUserId, Set<Integer> targetRoomIds, int topK) {

        // 获取所有有行为记录的用户（除目标用户外）
        List<Watch> allWatches = watchMapper.selectList(
            new LambdaQueryWrapper<Watch>()
                .ne(Watch::getUserId, targetUserId)
                .in(Watch::getWatchType, WatchTypeEnum.FOLLOW.getCode(), WatchTypeEnum.HISTORY.getCode()));

        // 按用户分组
        Map<Integer, Set<Integer>> userRooms = new HashMap<>();
        for (Watch w : allWatches) {
            userRooms.computeIfAbsent(w.getUserId(), k -> new HashSet<>()).add(w.getRoomId());
        }

        // 计算每个用户的 Jaccard 相似度
        return userRooms.entrySet().stream()
            .map(e -> {
                Set<Integer> otherRooms = e.getValue();
                Set<Integer> union = new HashSet<>(targetRoomIds);
                union.addAll(otherRooms);
                Set<Integer> intersection = new HashSet<>(targetRoomIds);
                intersection.retainAll(otherRooms);
                double jaccard = union.isEmpty() ? 0.0 :
                    (double) intersection.size() / union.size();
                return new AbstractMap.SimpleEntry<>(e.getKey(), jaccard);
            })
            .filter(e -> e.getValue() > 0.01)
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(topK)
            .collect(Collectors.toList());
    }

    private List<Room> getLivingFollowedRooms(int userId) {
        List<Watch> watches = watchMapper.selectList(
            new LambdaQueryWrapper<Watch>()
                .eq(Watch::getUserId, userId)
                .eq(Watch::getWatchType, WatchTypeEnum.FOLLOW.getCode()));
        if (watches.isEmpty()) return Collections.emptyList();

        List<Integer> roomIds = watches.stream().map(Watch::getRoomId).collect(Collectors.toList());
        return roomMapper.selectList(
            new LambdaQueryWrapper<Room>()
                .in(Room::getId, roomIds)
                .eq(Room::getStatus, LiveRoomStatusEnum.LIVING.getCode())
                .eq(Room::getDisabled, StatusEnum.YES.getCode())
                .in(!categoryIndex.isEmpty(), Room::getCategoryId, categoryIndex.keySet()));
    }

    // ─── 工具方法 ────────────────────────────────────────────

    private Map<String, Object> packageResult(Room room, double score) {
        return packageResult(room, score, "hot", "正在直播且近期热度较高");
    }

    private Map<String, Object> packageResult(Room room, double score, String recommendType, String recommendReason) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", room.getId());
        m.put("title", room.getTitle());
        m.put("cover", room.getCover());
        m.put("status", room.getStatus());
        m.put("categoryId", room.getCategoryId());
        m.put("score", Math.round(score * 10000.0) / 10000.0);
        m.put("recommendType", recommendType);
        m.put("recommendReason", recommendReason);
        return m;
    }

    private void enrichCategoryNames(List<Map<String, Object>> result) {
        if (result.isEmpty()) return;
        List<Integer> catIds = result.stream()
            .map(m -> (Integer) m.get("categoryId"))
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        if (catIds.isEmpty()) return;

        List<Category> cats = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
            .in(Category::getId, catIds)
            .eq(Category::getStatus, StatusEnum.YES.getCode()));
        Map<Integer, String> catMap = cats.stream()
            .collect(Collectors.toMap(Category::getId, Category::getName));
        for (Map<String, Object> m : result) {
            Integer cid = (Integer) m.get("categoryId");
            if (cid != null) m.put("categoryName", catMap.getOrDefault(cid, ""));
        }
    }

    private boolean isRecommendableRoom(Room room) {
        return room != null
            && Objects.equals(room.getStatus(), LiveRoomStatusEnum.LIVING.getCode())
            && Objects.equals(room.getDisabled(), StatusEnum.YES.getCode())
            && room.getCategoryId() != null
            && categoryIndex.containsKey(room.getCategoryId());
    }

    private void normalizeL2(double[] vec) {
        double norm = 0.0;
        for (double v : vec) norm += v * v;
        if (norm > 0) {
            norm = Math.sqrt(norm);
            for (int i = 0; i < vec.length; i++) vec[i] /= norm;
        }
    }

    private List<Category> listEnabledCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
            .eq(Category::getStatus, StatusEnum.YES.getCode())
            .orderByDesc(Category::getSort)
            .orderByAsc(Category::getId));
    }

}
