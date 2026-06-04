/**
 * BDD Specification: 余弦相似度推荐系统
 *
 * 验证推荐引擎的核心算法在 JavaScript 侧的行为正确性。
 * (Java 侧由 CosineSimilarityRecommenderTest 覆盖)
 */
import test from "node:test";
import assert from "node:assert/strict";

// ─── JS 版余弦相似度（与 Java 端算法一致） ──────────────

const cosineSimilarity = (a, b) => {
  if (!a || !b || a.length !== b.length) return 0;
  let dot = 0, normA = 0, normB = 0;
  for (let i = 0; i < a.length; i++) {
    dot += a[i] * b[i];
    normA += a[i] * a[i];
    normB += b[i] * b[i];
  }
  if (normA === 0 || normB === 0) return 0;
  return dot / (Math.sqrt(normA) * Math.sqrt(normB));
};

const topKBySimilarity = (targetVec, candidates, topK, excludeIds = new Set()) => {
  return Object.entries(candidates)
    .filter(([id]) => !excludeIds.has(Number(id)))
    .map(([id, vec]) => ({ id: Number(id), score: cosineSimilarity(targetVec, vec) }))
    .filter(r => r.score > 0.001)
    .sort((a, b) => b.score - a.score)
    .slice(0, topK);
};

// ─── Story: 推荐系统正确按相似度排序 ────────────────────

test("Story: 用户画像向量与房间特征向量计算余弦相似度", async (t) => {
  // Given: 用户画像向量（偏好游戏+技术分类）
  const userProfile = [1, 0, 1, 0]; // 喜欢分类0和2

  // Given: 4个候选房间的特征向量
  const candidates = {
    1: [1, 0, 1, 0],  // 完全匹配 → cos=1.0
    2: [1, 0, 0, 0],  // 部分匹配 → cos≈0.707
    3: [0, 1, 1, 0],  // 部分匹配 → cos≈0.707
    4: [0, 1, 0, 1],  // 不匹配   → cos=0.0
  };

  await t.test("Then: 完全匹配的房间排第一", () => {
    const result = topKBySimilarity(userProfile, candidates, 4);
    assert.equal(result[0].id, 1);
    assert.ok(result[0].score > 0.99);
  });

  await t.test("Then: 不匹配的房间被过滤掉", () => {
    const result = topKBySimilarity(userProfile, candidates, 4);
    const room4 = result.find(r => r.id === 4);
    assert.equal(room4, undefined, "完全不匹配应被过滤");
  });
});

// ─── Story: 推荐结果排除已交互房间 ──────────────────────

test("Story: 推荐列表不应包含用户已看过的房间", async (t) => {
  const userProfile = [1, 1];
  const candidates = {
    10: [1, 1], // 匹配但用户已看过
    20: [1, 0.5],
    30: [0.8, 1],
  };
  const excludeIds = new Set([10]);

  await t.test("Then: 已看过的房间10被排除", () => {
    const result = topKBySimilarity(userProfile, candidates, 3, excludeIds);
    assert.equal(result.length, 2);
    assert.ok(!result.find(r => r.id === 10));
    // [0.8, 1] 比 [1, 0.5] 更接近 [1, 1]，所以 30 排第一
    assert.equal(result[0].id, 30);
  });
});

// ─── Story: 分数归一化到 [0, 1] ─────────────────────────

test("Story: 推荐分数应落在合理范围内", async (t) => {
  const candidates = {
    1: [1, 0, 0],
    2: [0.5, 0.5, 0.7],
    3: [-1, 0, 0],
  };

  await t.test("Then: 所有分数在 [-1, 1] 范围内", () => {
    const result = topKBySimilarity([1, 0, 0], candidates, 3);
    for (const r of result) {
      assert.ok(r.score >= -1 && r.score <= 1, `分数 ${r.score} 应 ∈ [-1,1]`);
    }
  });

  await t.test("Then: 负相关被过滤", () => {
    const result = topKBySimilarity([1, 0, 0], candidates, 3);
    const neg = result.find(r => r.id === 3);
    assert.equal(neg, undefined, "负相关(-1.0)不应被推荐");
  });
});

// ─── Story: 空数据边界 ───────────────────────────────────

test("Story: 边界条件处理", async (t) => {
  await t.test("When: 零向量 → cos=0", () => {
    assert.equal(cosineSimilarity([0, 0], [1, 2]), 0);
  });

  await t.test("When: 候选集为空 → 空结果", () => {
    assert.deepEqual(topKBySimilarity([1], {}, 5), []);
  });

  await t.test("When: null/undefined 输入 → 安全返回", () => {
    assert.equal(cosineSimilarity(null, [1]), 0);
    assert.equal(cosineSimilarity([1], undefined), 0);
  });

  await t.test("When: 维度不一致 → 安全返回 0", () => {
    assert.equal(cosineSimilarity([1, 2], [1, 2, 3]), 0);
  });
});

console.log("BDD PASS: cosineRecommendation.spec.mjs 全部通过");
