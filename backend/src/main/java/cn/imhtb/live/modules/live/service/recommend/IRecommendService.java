package cn.imhtb.live.modules.live.service.recommend;

import java.util.List;
import java.util.Map;

/**
 * 推荐服务接口 — 基于余弦相似度的混合推荐。
 *
 * <h3>推荐管线</h3>
 * <ol>
 *   <li>构建用户行为画像向量</li>
 *   <li>构建房间内容特征向量</li>
 *   <li>计算余弦相似度矩阵</li>
 *   <li>协同过滤 + 内容匹配 + 热度加成 → 混合排序</li>
 *   <li>返回 Top-K 推荐结果</li>
 * </ol>
 *
 * @author PulseLive Recommendation Team
 */
public interface IRecommendService {

    /**
     * 为用户生成个性化直播间推荐。
     *
     * @param userId 用户 ID（null 表示未登录，走热门兜底）
     * @param limit  返回数量上限
     * @return 推荐房间列表，每项包含 id, title, cover, categoryId, score 等
     */
    List<Map<String, Object>> recommendForUser(Integer userId, int limit);

    /**
     * 查找与指定房间最相似的直播间（物品-物品协同过滤）。
     *
     * @param roomId 源房间 ID
     * @param limit  返回数量上限
     * @return 相似房间列表（不包含源房间自身）
     */
    List<Map<String, Object>> similarRooms(Integer roomId, int limit);

    /**
     * 手动触发推荐模型刷新（重新计算特征向量和相似度矩阵）。
     * 适用于：有新房间上线、分类变更、模型参数调整后。
     */
    void refreshModel();
}
