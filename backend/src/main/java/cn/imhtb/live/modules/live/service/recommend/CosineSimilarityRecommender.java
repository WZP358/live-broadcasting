package cn.imhtb.live.modules.live.service.recommend;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 余弦相似度推荐引擎核心 — 零外部依赖，纯数学实现。
 *
 * <h3>算法原理</h3>
 * <p>余弦相似度度量两个向量在方向上的接近程度，取值 [-1, 1]，
 * 1 表示完全相同方向，0 表示正交，-1 表示完全相反。</p>
 *
 * <pre>
 *                               A · B        Σ(Aᵢ × Bᵢ)
 *   cos(A, B) = ──────────────── = ────────────────────────
 *                ‖A‖ × ‖B‖       √Σ(Aᵢ²) × √Σ(Bᵢ²)
 * </pre>
 *
 * <h3>适用场景</h3>
 * <ul>
 *   <li><b>协同过滤 (Collaborative Filtering)</b>：用余弦相似度找行为相似的用户</li>
 *   <li><b>内容推荐 (Content-Based)</b>：用余弦相似度匹配房间特征向量</li>
 *   <li><b>物品相似 (Item-Item)</b>：计算两两房间之间的相似度矩阵</li>
 * </ul>
 *
 * <h3>特征向量设计</h3>
 * <p>每个直播间被编码为一个稠密特征向量，维度构成如下：</p>
 * <pre>
 *   V_room = [category_onehot | tfidf_keywords | popularity_norm | browser_flag]
 *            ↑                 ↑                 ↑                ↑
 *           分类独热编码      标题TF-IDF词向量    归一化人气值      浏览器直播标记
 * </pre>
 *
 * @author PulseLive Recommendation Team
 * @since 2026-05
 */
public class CosineSimilarityRecommender {

    // ─── 余弦相似度核心 ──────────────────────────────────────

    /**
     * 计算两个稠密向量的余弦相似度。
     *
     * @param a 向量 A（非空、非零）
     * @param b 向量 B（非空、非零，必须与 A 等长）
     * @return 余弦相似度值，范围 [-1, 1]
     * @throws IllegalArgumentException 如果向量为 null、长度不等或任意一个为零向量
     */
    public static double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("向量不能为 null");
        }
        if (a.length != b.length) {
            throw new IllegalArgumentException(
                String.format("向量维度不一致: A.length=%d, B.length=%d", a.length, b.length));
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0; // 零向量无方向，相似度定义为 0
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 计算两个稀疏向量（用 Map 表示）的余弦相似度。
     * <p>适用于高维稀疏场景（如 TF-IDF 词向量），只遍历非零项。</p>
     *
     * @param sparseA 稀疏向量 A（索引 → 值）
     * @param sparseB 稀疏向量 B（索引 → 值）
     * @return 余弦相似度值
     */
    public static double cosineSimilaritySparse(
            Map<Integer, Double> sparseA,
            Map<Integer, Double> sparseB) {

        if (sparseA == null || sparseB == null || sparseA.isEmpty() || sparseB.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        // 只计算两个向量共有的非零维度
        for (Map.Entry<Integer, Double> entry : sparseA.entrySet()) {
            int idx = entry.getKey();
            double valA = entry.getValue();
            normA += valA * valA;

            Double valB = sparseB.get(idx);
            if (valB != null) {
                dotProduct += valA * valB;
            }
        }

        for (double valB : sparseB.values()) {
            normB += valB * valB;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // ─── TF-IDF 文本向量化 ────────────────────────────────────

    /**
     * 对标题/简介文本做简单 TF-IDF 向量化。
     *
     * <p>使用 BM25 变体：</p>
     * <pre>
     *   TF(t,d) = 词 t 在文档 d 中的出现次数 / 文档总词数
     *   IDF(t)  = log((文档总数 - 含 t 的文档数 + 0.5) / (含 t 的文档数 + 0.5) + 1)
     *   TF-IDF   = TF × IDF
     * </pre>
     *
     * @param documents    所有文档的文本列表（每项是拼接好的 title + introduce）
     * @param targetIndex  要向量化的目标文档索引
     * @return 稀疏 TF-IDF 向量（词索引 → 权重）
     */
    public static Map<Integer, Double> computeTfIdfVector(
            List<String> documents, int targetIndex) {

        if (documents == null || targetIndex < 0 || targetIndex >= documents.size()) {
            return Collections.emptyMap();
        }

        // 1. 分词（简单按 Unicode 非字母数字切分，保留中文字符）
        List<List<String>> tokenizedDocs = new ArrayList<>();
        Set<String> vocabulary = new LinkedHashSet<>();

        for (String doc : documents) {
            List<String> tokens = tokenize(doc);
            tokenizedDocs.add(tokens);
            vocabulary.addAll(tokens);
        }

        List<String> vocabList = new ArrayList<>(vocabulary);
        int N = documents.size();

        // 2. 计算每个词的文档频率 (DF)
        Map<String, Integer> docFrequency = new HashMap<>();
        for (List<String> tokens : tokenizedDocs) {
            Set<String> uniqueTokens = new HashSet<>(tokens);
            for (String token : uniqueTokens) {
                docFrequency.merge(token, 1, Integer::sum);
            }
        }

        // 3. 为目标文档计算 TF-IDF
        List<String> targetTokens = tokenizedDocs.get(targetIndex);
        Map<String, Double> tfMap = new HashMap<>();
        for (String token : targetTokens) {
            tfMap.merge(token, 1.0, Double::sum);
        }
        // 归一化 TF
        int totalTerms = targetTokens.size();
        if (totalTerms > 0) {
            for (String key : tfMap.keySet()) {
                tfMap.put(key, tfMap.get(key) / totalTerms);
            }
        }

        // 4. TF-IDF = TF × IDF
        Map<Integer, Double> vector = new HashMap<>();
        for (int i = 0; i < vocabList.size(); i++) {
            String term = vocabList.get(i);
            double tf = tfMap.getOrDefault(term, 0.0);
            if (tf == 0.0) continue;

            int df = docFrequency.getOrDefault(term, 0);
            double idf = Math.log((N - df + 0.5) / (df + 0.5) + 1.0);
            vector.put(i, tf * idf);
        }

        return vector;
    }

    /**
     * 简单分词：按非字母数字字符切分，过滤单字和停用词。
     */
    private static List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) return Collections.emptyList();

        // 按非字母数字 + 非中文字符切分
        String[] raw = text.toLowerCase().split("[^a-z0-9\\u4e00-\\u9fff]+");
        Set<String> stopWords = Set.of(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一",
            "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着",
            "没有", "看", "好", "自己", "这", "the", "a", "an", "is", "are", "was",
            "were", "be", "been", "being", "have", "has", "had", "do", "does", "did",
            "will", "would", "could", "should", "may", "might", "can", "shall",
            "to", "of", "in", "for", "on", "with", "at", "by", "from", "as", "into",
            "through", "during", "before", "after", "above", "below", "between",
            "and", "but", "or", "nor", "not", "so", "yet", "both", "either", "neither"
        );

        return Arrays.stream(raw)
            .filter(t -> t.length() >= 2)
            .filter(t -> !stopWords.contains(t))
            .collect(Collectors.toList());
    }

    // ─── 推荐 Top-K ──────────────────────────────────────────

    /**
     * 从候选集中选出与目标向量余弦相似度最高的 topK 项。
     *
     * @param targetVector 目标向量
     * @param candidates   候选集（ID → 特征向量）
     * @param topK         返回数量
     * @param excludeIds   要排除的 ID 集合
     * @return 按相似度降序排列的 [(id, similarity)]
     */
    public static List<Map.Entry<Integer, Double>> topKBySimilarity(
            double[] targetVector,
            Map<Integer, double[]> candidates,
            int topK,
            Set<Integer> excludeIds) {

        if (targetVector == null || candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> exclude = excludeIds != null ? excludeIds : Collections.emptySet();

        return candidates.entrySet().stream()
            .filter(e -> !exclude.contains(e.getKey()))
            .map(e -> new AbstractMap.SimpleEntry<>(
                e.getKey(),
                cosineSimilarity(targetVector, e.getValue())))
            .filter(e -> e.getValue() > 0.001) // 只保留正相关
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(topK)
            .collect(Collectors.toList());
    }

    // ─── 混合评分 ────────────────────────────────────────────

    /**
     * 混合评分：融合协同过滤、内容推荐和热度加成。
     *
     * <pre>
     *   score(r) = α · sim_cf(r) + β · sim_content(r) + γ · popularity_zscore(r)
     *
     *   默认权重：
     *   α = 0.35  协同过滤（用户行为相似度）
     *   β = 0.45  内容匹配（房间特征相似度）
     *   γ = 0.20  热度加成（归一化人气值）
     * </pre>
     */
    public static class HybridScorer {

        private final double alpha; // 协同过滤权重
        private final double beta;  // 内容匹配权重
        private final double gamma; // 热度权重

        public HybridScorer() {
            this(0.35, 0.45, 0.20);
        }

        public HybridScorer(double alpha, double beta, double gamma) {
            double total = alpha + beta + gamma;
            this.alpha = alpha / total;
            this.beta = beta / total;
            this.gamma = gamma / total;
        }

        /**
         * @param cfScore      协同过滤相似度 [0, 1]
         * @param contentScore 内容相似度 [0, 1]
         * @param popularityZ  归一化人气 Z-score
         * @return 混合评分
         */
        public double score(double cfScore, double contentScore, double popularityZ) {
            // sigmoid 映射到 [0,1]
            double popScore = 1.0 / (1.0 + Math.exp(-popularityZ));
            return alpha * cfScore + beta * contentScore + gamma * popScore;
        }
    }
}
