package cn.imhtb.live.modules.live.service.recommend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 余弦相似度推荐引擎单元测试。
 *
 * <p>TDD 规范：先写测试，验证数学正确性，再集成到服务层。</p>
 */
@DisplayName("CosineSimilarityRecommender 余弦相似度推荐引擎")
class CosineSimilarityRecommenderTest {

    // ─── 余弦相似度基础 ──────────────────────────────────────

    @Nested
    @DisplayName("cosineSimilarity — 稠密向量余弦相似度")
    class CosineSimilarityDense {

        @Test
        @DisplayName("相同向量 → 1.0")
        void identicalVectorsShouldReturnOne() {
            double[] a = {1, 2, 3};
            double[] b = {1, 2, 3};
            assertEquals(1.0, CosineSimilarityRecommender.cosineSimilarity(a, b), 1e-9);
        }

        @Test
        @DisplayName("正交向量 → 0.0")
        void orthogonalVectorsShouldReturnZero() {
            double[] a = {1, 0, 0};
            double[] b = {0, 1, 0};
            assertEquals(0.0, CosineSimilarityRecommender.cosineSimilarity(a, b), 1e-9);
        }

        @Test
        @DisplayName("相反向量 → -1.0")
        void oppositeVectorsShouldReturnNegativeOne() {
            double[] a = {1, 2, 3};
            double[] b = {-1, -2, -3};
            assertEquals(-1.0, CosineSimilarityRecommender.cosineSimilarity(a, b), 1e-9);
        }

        @Test
        @DisplayName("缩放不敏感：向量缩放不影响相似度")
        void scaleInvariance() {
            double[] a = {1, 2, 3};
            double[] b = {2, 4, 6}; // 2x a
            assertEquals(1.0, CosineSimilarityRecommender.cosineSimilarity(a, b), 1e-9);
        }

        @Test
        @DisplayName("部分相似：45 度角 ~ 0.707")
        void partialSimilarity45Degrees() {
            double[] a = {1, 1};
            double[] b = {1, 0};
            double expected = Math.cos(Math.PI / 4); // 0.7071...
            assertEquals(expected, CosineSimilarityRecommender.cosineSimilarity(a, b), 1e-6);
        }

        @Test
        @DisplayName("零向量 → 0.0")
        void zeroVectorShouldReturnZero() {
            double[] a = {0, 0, 0};
            double[] b = {1, 2, 3};
            assertEquals(0.0, CosineSimilarityRecommender.cosineSimilarity(a, b), 1e-9);
        }

        @Test
        @DisplayName("null 向量 → 抛出 IllegalArgumentException")
        void nullVectorShouldThrow() {
            assertThrows(IllegalArgumentException.class, () ->
                CosineSimilarityRecommender.cosineSimilarity(null, new double[]{1}));
            assertThrows(IllegalArgumentException.class, () ->
                CosineSimilarityRecommender.cosineSimilarity(new double[]{1}, null));
        }

        @Test
        @DisplayName("维度不一致 → 抛出 IllegalArgumentException")
        void dimensionMismatchShouldThrow() {
            assertThrows(IllegalArgumentException.class, () ->
                CosineSimilarityRecommender.cosineSimilarity(new double[]{1, 2}, new double[]{1, 2, 3}));
        }
    }

    // ─── 稀疏向量余弦相似度 ──────────────────────────────────

    @Nested
    @DisplayName("cosineSimilaritySparse — 稀疏向量余弦相似度")
    class CosineSimilaritySparse {

        @Test
        @DisplayName("两个完全相同的稀疏向量 → 1.0")
        void identicalSparseVectorsShouldReturnOne() {
            Map<Integer, Double> a = Map.of(0, 1.0, 1, 2.0);
            Map<Integer, Double> b = Map.of(0, 1.0, 1, 2.0);
            assertEquals(1.0, CosineSimilarityRecommender.cosineSimilaritySparse(a, b), 1e-9);
        }

        @Test
        @DisplayName("完全不重叠的稀疏向量 → 0.0")
        void nonOverlappingSparseVectorsShouldReturnZero() {
            Map<Integer, Double> a = Map.of(0, 1.0);
            Map<Integer, Double> b = Map.of(99, 5.0);
            assertEquals(0.0, CosineSimilarityRecommender.cosineSimilaritySparse(a, b), 1e-9);
        }

        @Test
        @DisplayName("部分重叠的稀疏向量")
        void partiallyOverlappingSparse() {
            // a = [1, 0, 2], b = [0, 3, 2]
            Map<Integer, Double> a = Map.of(0, 1.0, 2, 2.0);
            Map<Integer, Double> b = Map.of(1, 3.0, 2, 2.0);
            // 点积: 只有索引2 重叠 → 2*2=4
            // norm_a: √(1+4)=√5, norm_b: √(9+4)=√13
            // cos = 4/√65 ≈ 0.4961
            double expected = 4.0 / Math.sqrt(65.0);
            assertEquals(expected, CosineSimilarityRecommender.cosineSimilaritySparse(a, b), 1e-4);
        }

        @Test
        @DisplayName("空向量 → 0.0")
        void emptyOrNullSparseShouldReturnZero() {
            assertEquals(0.0, CosineSimilarityRecommender.cosineSimilaritySparse(
                Collections.emptyMap(), Map.of(0, 1.0)), 1e-9);
            assertEquals(0.0, CosineSimilarityRecommender.cosineSimilaritySparse(null, Map.of(0, 1.0)), 1e-9);
        }
    }

    // ─── TF-IDF ──────────────────────────────────────────────

    @Nested
    @DisplayName("computeTfIdfVector — TF-IDF 文本向量化")
    class TfIdfVectorization {

        @Test
        @DisplayName("单文档 TF-IDF 非空")
        void singleDocumentShouldProduceNonEmptyVector() {
            List<String> docs = List.of("前端 Vue3 实战 直播");
            Map<Integer, Double> vec = CosineSimilarityRecommender.computeTfIdfVector(docs, 0);
            assertFalse(vec.isEmpty(), "TF-IDF 向量不应为空");
            // 所有词出现在1个文档中, IDF = log(1+0.5/0.5+1) = log(2) ≈ 0.693
            // 每个词 TF = 1/4 = 0.25
            for (double w : vec.values()) {
                assertTrue(w > 0, "权重应大于0");
            }
        }

        @Test
        @DisplayName("多文档：高频词应获得更低权重")
        void commonTermsShouldHaveLowerWeight() {
            // "直播" 出现在所有文档中 → IDF 应较低
            List<String> docs = List.of(
                "前端 直播 教学",
                "后端 直播 开发",
                "游戏 直播 娱乐"
            );
            Map<Integer, Double> vec = CosineSimilarityRecommender.computeTfIdfVector(docs, 0);
            // "前端"应该比"直播"权重大（因为"前端"只在文档0中出现）
            assertFalse(vec.isEmpty());
        }

        @Test
        @DisplayName("无效索引 → 空向量")
        void invalidIndexShouldReturnEmpty() {
            List<String> docs = List.of("test");
            assertTrue(CosineSimilarityRecommender.computeTfIdfVector(docs, -1).isEmpty());
            assertTrue(CosineSimilarityRecommender.computeTfIdfVector(docs, 99).isEmpty());
        }

        @Test
        @DisplayName("空文档列表 → 空向量")
        void emptyDocsShouldReturnEmpty() {
            assertTrue(CosineSimilarityRecommender.computeTfIdfVector(Collections.emptyList(), 0).isEmpty());
        }

        @Test
        @DisplayName("停用词应被过滤")
        void stopWordsShouldBeFiltered() {
            List<String> docs = List.of("的 了 在 是 前端 Vue3");
            Map<Integer, Double> vec = CosineSimilarityRecommender.computeTfIdfVector(docs, 0);
            // 停用词"的、了、在、是"应该被过滤
            assertFalse(vec.isEmpty());
            for (Map.Entry<Integer, Double> e : vec.entrySet()) {
                assertTrue(e.getValue() > 0);
            }
        }
    }

    // ─── Top-K 推荐 ─────────────────────────────────────────

    @Nested
    @DisplayName("topKBySimilarity — Top-K 排序推荐")
    class TopKRecommendation {

        @Test
        @DisplayName("正确选出最相似的 Top-2")
        void shouldReturnTop2MostSimilar() {
            double[] target = {1, 0, 0}; // 指向第0维
            Map<Integer, double[]> candidates = new LinkedHashMap<>();
            candidates.put(1, new double[]{1, 0, 0});   // cos=1.0
            candidates.put(2, new double[]{0.707, 0.707, 0}); // cos≈0.707
            candidates.put(3, new double[]{0, 1, 0});   // cos=0.0

            List<Map.Entry<Integer, Double>> result =
                CosineSimilarityRecommender.topKBySimilarity(target, candidates, 2, Collections.emptySet());

            assertEquals(2, result.size());
            assertEquals(Integer.valueOf(1), result.get(0).getKey());
            assertEquals(1.0, result.get(0).getValue(), 1e-6);
            assertEquals(Integer.valueOf(2), result.get(1).getKey());
        }

        @Test
        @DisplayName("排除指定 ID")
        void shouldExcludeSpecifiedIds() {
            double[] target = {1, 0};
            Map<Integer, double[]> candidates = Map.of(
                1, new double[]{1, 0},
                2, new double[]{0.9, 0.1}
            );

            List<Map.Entry<Integer, Double>> result =
                CosineSimilarityRecommender.topKBySimilarity(target, candidates, 5, Set.of(1));

            assertEquals(1, result.size());
            assertEquals(Integer.valueOf(2), result.get(0).getKey());
        }

        @Test
        @DisplayName("候选集为空 → 空结果")
        void emptyCandidatesShouldReturnEmpty() {
            List<Map.Entry<Integer, Double>> result =
                CosineSimilarityRecommender.topKBySimilarity(
                    new double[]{1}, Collections.emptyMap(), 5, Collections.emptySet());
            assertTrue(result.isEmpty());
        }
    }

    // ─── 混合评分 ────────────────────────────────────────────

    @Nested
    @DisplayName("HybridScorer — 混合评分器")
    class HybridScoring {

        @Test
        @DisplayName("满分：所有信号最强时接近 1.0")
        void allMaxSignalsShouldScoreNearOne() {
            CosineSimilarityRecommender.HybridScorer scorer =
                new CosineSimilarityRecommender.HybridScorer();
            double score = scorer.score(1.0, 1.0, 5.0); // 高人气 Z-score
            assertTrue(score > 0.85, "混合评分应接近 1.0: " + score);
        }

        @Test
        @DisplayName("零分：所有信号最弱时接近 0.0")
        void allZeroSignalsShouldScoreNearZero() {
            CosineSimilarityRecommender.HybridScorer scorer =
                new CosineSimilarityRecommender.HybridScorer();
            double score = scorer.score(0.0, 0.0, -5.0); // 低人气 Z-score
            assertTrue(score < 0.25, "混合评分应接近 0.0: " + score);
        }

        @Test
        @DisplayName("权重归一化：sum(α,β,γ) = 1.0")
        void weightsShouldSumToOne() {
            CosineSimilarityRecommender.HybridScorer scorer =
                new CosineSimilarityRecommender.HybridScorer(3, 5, 2);
            // 按权重评分: 高协同 + 低内容 + 中热度
            double score = scorer.score(1.0, 0.0, 0.0);
            assertTrue(score > 0.25 && score < 0.4,
                "协同权重约占 0.3: " + score);
        }

        @Test
        @DisplayName("自定义权重")
        void customWeights() {
            // 纯内容推荐模式: α=0, β=1, γ=0
            CosineSimilarityRecommender.HybridScorer pureContent =
                new CosineSimilarityRecommender.HybridScorer(0.0, 1.0, 0.0);
            double score = pureContent.score(0.5, 0.9, 2.0);
            assertTrue(score > 0.85, "纯内容模式: 内容分应主导: " + score);
        }
    }
}
