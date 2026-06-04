# PulseLive 余弦相似度推荐系统 — 技术文档

## 1. 概述

PulseLive 推荐系统采用 **余弦相似度 (Cosine Similarity)** 作为核心度量，实现 **协同过滤 × 内容推荐 × 热度加权** 的混合推荐策略。系统为纯 Java 实现，零外部 ML 依赖，直接在应用启动时完成模型初始化。

### 核心指标

| 指标 | 说明 |
|------|------|
| 相似度度量 | 余弦相似度 \(\cos(A,B) = \frac{A \cdot B}{\|A\| \times \|B\|}\) |
| 推荐延迟 | < 50ms（内存计算） |
| 模型大小 | ~200KB（特征向量缓存） |
| 冷启动 | 未登录用户直接走热度兜底 |

---

## 2. 余弦相似度数学原理

### 2.1 定义

余弦相似度度量两个非零向量在 **方向** 上的接近程度，忽略向量的长度（模）。对于推荐系统而言，"方向一致"比"数值大小相同"更有意义——用户 A 看了 3 个游戏房间、用户 B 看了 30 个游戏房间，虽然量级不同，但他们的兴趣方向是一致的。

\[
\cos(\mathbf{A}, \mathbf{B}) = \frac{\mathbf{A} \cdot \mathbf{B}}{\|\mathbf{A}\| \times \|\mathbf{B}\|} = \frac{\sum_{i=1}^{n} A_i B_i}{\sqrt{\sum_{i=1}^{n} A_i^2} \times \sqrt{\sum_{i=1}^{n} B_i^2}}
\]

**取值范围：** \([-1, 1]\)
- \(1.0\) — 完全相同方向（完美匹配）
- \(0.0\) — 正交（无关）
- \(-1.0\) — 完全相反（负相关，推荐系统中过滤掉）

### 2.2 为什么用余弦相似度而不是欧氏距离？

| 对比维度 | 余弦相似度 | 欧氏距离 |
|----------|-----------|---------|
| 关注点 | 方向（兴趣偏好） | 绝对数值大小 |
| 量级敏感性 | 不敏感（L2 归一化后） | 敏感 |
| 高维稀疏场景 | 非常适合 | 容易失效（维度灾难） |
| 计算复杂度 | O(n) | O(n) |
| 推荐场景适配 | ✅ 用户兴趣方向匹配 | ❌ 行为数量差异干扰大 |

**示例：** 用户 A 看了 3 次游戏直播，用户 B 看了 50 次游戏直播。欧氏距离会认为两者差异巨大（|3-50|=47），而余弦相似度会发现两者的兴趣方向完全一致（都指向"游戏"）。

### 2.3 稀疏向量优化

文本特征（TF-IDF）天然是高维稀疏的——词汇表可能有上千个词，但每个房间标题只含其中几十个。系统针对稀疏向量做了优化：

\[
\cos_{sparse}(A, B) = \frac{\sum_{i \in A \cap B} A_i \times B_i}{\sqrt{\sum A_i^2} \times \sqrt{\sum B_i^2}}
\]

只遍历两个向量 **共有** 的非零维度，将复杂度从 \(O(V)\) 降到 \(O(\min(|A|, |B|))\)（V 为词汇表大小）。

---

## 3. 特征工程设计

### 3.1 房间特征向量

每个直播间被编码为以下结构的稠密向量：

\[
\mathbf{V}_{room} = [\underbrace{c_1, c_2, ..., c_m}_{\text{分类独热}} \;|\; \underbrace{t_1, t_2, ..., t_k}_{\text{TF-IDF 关键词}}]
\]

| 特征段 | 维度 | 编码方式 | 示例 |
|--------|------|----------|------|
| 分类独热 | m = 分类总数 | 1 if 匹配 else 0 | 游戏: `[1,0,0,0,...]` |
| TF-IDF 关键词 | k = 词汇表大小 | TF × IDF 权重 | "前端": 0.43, "Vue3": 0.67 |

向量构建后执行 **L2 归一化**，使所有房间向量落在单位超球面上，保证余弦相似度计算的一致性。

### 3.2 用户画像向量

用户的兴趣向量通过其历史行为房间向量的 **加权平均** 构建：

\[
\mathbf{V}_{user} = \frac{\sum_{r \in history} w_r \cdot \mathbf{V}_r}{\sum w_r}
\]

其中关注行为权重 \(w_{follow} > w_{history}\)（关注比单纯观看更能体现兴趣）。

### 3.3 TF-IDF 文本向量化

对房间标题和简介进行分词后计算 TF-IDF 权重：

\[
\text{TF}(t, d) = \frac{\text{词 } t \text{ 在文档 } d \text{ 中的频次}}{\text{文档 } d \text{ 的总词数}}
\]

\[
\text{IDF}(t) = \log\left(\frac{N - df_t + 0.5}{df_t + 0.5} + 1\right)
\]

\[
\text{TF-IDF}(t, d) = \text{TF}(t, d) \times \text{IDF}(t)
\]

其中 \(N\) 为文档总数，\(df_t\) 为包含词 \(t\) 的文档数。采用 BM25 平滑变体避免 IDF 为负。

---

## 4. 推荐策略管线

```
用户请求推荐
    │
    ├─ 已登录？
    │   ├─ YES → 加载用户行为记录（关注 + 历史）
    │   │
    │   │   ├─ 策略1: 协同过滤（权重 α=0.30）
    │   │   │   └─ 找 Top-K 相似用户 → 推荐他们正在看的房间
    │   │   │
    │   │   ├─ 策略2: 内容匹配（权重 β=0.50）
    │   │   │   └─ 用户画像向量 × 所有房间向量 → 余弦 Top-N
    │   │   │
    │   │   └─ 不足 limit？
    │   │       └─ YES → 策略3: 热度兜底（权重 γ=0.20）
    │   │
    │   └─ NO → 直接热度兜底
    │
    └─ 返回 Top-K 推荐结果
```

### 4.1 混合评分公式

\[
\text{score}(r) = \alpha \cdot \text{sim}_{cf}(r) + \beta \cdot \text{sim}_{content}(r) + \gamma \cdot \sigma(\text{pop}_z(r))
\]

其中：
- \(\text{sim}_{cf}(r)\) — 协同过滤相似度（通过相似用户的交互计算）
- \(\text{sim}_{content}(r)\) — 内容余弦相似度
- \(\sigma(\text{pop}_z(r))\) — 人气值 Z-score 的 Sigmoid 归一化
- 默认权重：\(\alpha=0.30, \beta=0.50, \gamma=0.20\)

### 4.2 物品-物品相似推荐

用于"相关推荐"场景（如直播间下方的推荐列表）：

\[
\text{similar}(room_A) = \text{topK}_{r \neq A} \; \cos(\mathbf{V}_A, \mathbf{V}_r)
\]

直接使用房间特征向量的余弦相似度排序，排除源房间自身。

---

## 5. 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                      前端 (Vue 3)                            │
│  recommend.js ──→ GET /api/v1/recommend/rooms               │
│                ──→ GET /api/v1/recommend/similar-rooms/:id  │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP
┌──────────────────────────▼──────────────────────────────────┐
│                  RecommendController                         │
│  recommendRooms() / similarRooms() / refreshModel()         │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                  RecommendServiceImpl                        │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  CosineSimilarityRecommender (核心引擎)              │    │
│  │  · cosineSimilarity(dense)                          │    │
│  │  · cosineSimilaritySparse(sparse)                   │    │
│  │  · computeTfIdfVector(docs, idx)                    │    │
│  │  · topKBySimilarity(target, candidates, K)          │    │
│  │  · HybridScorer(α, β, γ)                           │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  数据层: RoomMapper / WatchMapper / CategoryMapper           │
└──────────────────────────────────────────────────────────────┘
```

---

## 6. 评估指标

### 6.1 离线评估

| 指标 | 公式 | 说明 |
|------|------|------|
| Precision@K | \(\frac{\|\text{推荐} \cap \text{实际观看}\|}{K}\) | 推荐准确率 |
| Recall@K | \(\frac{\|\text{推荐} \cap \text{实际观看}\|}{\|\text{实际观看}\|}\) | 推荐覆盖率 |
| Diversity | \(1 - \frac{\sum \cos(R_i, R_j)}{K(K-1)/2}\) | 推荐多样性 |
| Coverage | \(\frac{\|\text{推荐过的房间}\|}{\|\text{总房间}\|}\) | 目录覆盖率 |

### 6.2 在线指标

- **CTR (Click-Through Rate)**：推荐房间点击率
- **观看时长**：从推荐进入的用户的平均观看时长
- **转化率**：推荐 → 关注 转化率

---

## 7. API 接口文档

### 7.1 个性化推荐

```
GET /api/v1/recommend/rooms?limit=12

Response:
{
  "code": 0,
  "data": [
    {
      "id": 42,
      "title": "Vue3 实战直播",
      "cover": "https://...",
      "status": 1,
      "categoryId": 5,
      "categoryName": "技术",
      "score": 0.8732        // 推荐分数 (0-1)
    }
  ]
}
```

### 7.2 相似直播间

```
GET /api/v1/recommend/similar-rooms/42?limit=6

Response:
{
  "code": 0,
  "data": [
    {
      "id": 99,
      "title": "React 前端进阶",
      "score": 0.9105
    }
  ]
}
```

### 7.3 刷新模型

```
POST /api/v1/recommend/refresh

Response:
{ "code": 0, "data": "推荐模型已刷新" }
```

---

## 8. 参考文献

1. Sarwar, B., et al. "Item-based collaborative filtering recommendation algorithms." *WWW 2001.*
2. Salton, G., & Buckley, C. "Term-weighting approaches in automatic text retrieval." *Information Processing & Management*, 1988.
3. Koren, Y., et al. "Matrix factorization techniques for recommender systems." *Computer*, 2009.
4. Deerwester, S., et al. "Indexing by latent semantic analysis." *JASIS*, 1990.
