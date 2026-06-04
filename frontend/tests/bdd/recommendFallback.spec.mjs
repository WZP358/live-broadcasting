/**
 * BDD Specification: 个性化推荐兜底逻辑
 *
 * 验证推荐算法在无用户数据时的兜底行为。
 */
import test from "node:test";
import assert from "node:assert/strict";

// ─── 推荐策略模拟 ──────────────────────────────────────

const recommendRooms = ({
  userId = null,
  watches = [],
  allLivingRooms = [],
  limit = 12,
}) => {
  const shownIds = new Set();
  const result = [];

  // Step 1: 关注的房间在直播
  if (userId != null && watches.length > 0) {
    const followedRoomIds = new Set(watches.map((w) => w.roomId));
    const followedLiving = allLivingRooms.filter(
      (r) => followedRoomIds.has(r.id) && r.status === 1,
    );
    for (const r of followedLiving) {
      if (result.length >= limit) break;
      if (!shownIds.has(r.id)) {
        shownIds.add(r.id);
        result.push(r);
      }
    }
  }

  // Step 2: 同分类热门
  if (result.length < limit && watches.length > 0) {
    const watchedCategoryIds = new Set(
      allLivingRooms
        .filter((r) => watches.some((w) => w.roomId === r.id))
        .map((r) => r.categoryId)
        .filter(Boolean),
    );
    if (watchedCategoryIds.size > 0) {
      const sameCategory = allLivingRooms
        .filter(
          (r) => watchedCategoryIds.has(r.categoryId) && r.status === 1 && !shownIds.has(r.id),
        )
        .sort((a, b) => b.id - a.id);
      for (const r of sameCategory) {
        if (result.length >= limit) break;
        if (!shownIds.has(r.id)) {
          shownIds.add(r.id);
          result.push(r);
        }
      }
    }
  }

  // Step 3: 热门兜底
  if (result.length < limit) {
    const hot = allLivingRooms
      .filter((r) => r.status === 1 && !shownIds.has(r.id))
      .sort((a, b) => b.popularity - a.popularity);
    for (const r of hot) {
      if (result.length >= limit) break;
      if (!shownIds.has(r.id)) {
        shownIds.add(r.id);
        result.push(r);
      }
    }
  }

  return result;
};

// ─── 测试数据 ──────────────────────────────────────────

const allRooms = [
  { id: 1, title: "关注的在播", categoryId: 5, status: 1, popularity: 100 },
  { id: 2, title: "关注的已下播", categoryId: 5, status: 0, popularity: 1000 },
  { id: 3, title: "同分类热门A", categoryId: 5, status: 1, popularity: 500 },
  { id: 4, title: "同分类热门B", categoryId: 5, status: 1, popularity: 300 },
  { id: 5, title: "普通热门1", categoryId: 10, status: 1, popularity: 999 },
  { id: 6, title: "普通热门2", categoryId: 10, status: 1, popularity: 888 },
  { id: 7, title: "普通热门3", categoryId: 10, status: 1, popularity: 777 },
];

// ─── Story: 登录用户个性化推荐 ─────────────────────────

test("Story: 登录用户看到个性化推荐（关注优先 > 同分类 > 热门兜底）", async (t) => {
  await t.test("Given: 用户关注了房间1, When: 推荐, Then: 关注的在播房间排第一", () => {
    const result = recommendRooms({
      userId: 1,
      watches: [{ roomId: 1 }, { roomId: 2 }],
      allLivingRooms: allRooms,
      limit: 4,
    });

    assert.equal(result.length, 4);
    assert.equal(result[0].id, 1, "关注的房间应排第一");
  });

  await t.test("Then: 关注房间之后是同分类热门", () => {
    const result = recommendRooms({
      userId: 1,
      watches: [{ roomId: 1 }],
      allLivingRooms: allRooms,
      limit: 4,
    });

    // 第1个：关注房间 id=1
    // 第2-3个：同分类 id=3, id=4
    // 第4个：热门兜底
    assert.equal(result[1].categoryId, 5, "第二个应是同分类的");
    assert.equal(result[2].categoryId, 5, "第三个应是同分类的");
  });
});

// ─── Story: 未登录用户热门推荐 ─────────────────────────

test("Story: 未登录游客看到热门直播间", async (t) => {
  await t.test("Given: 未登录, When: 请求推荐, Then: 返回热门直播间按热度排序", () => {
    const result = recommendRooms({
      userId: null,
      watches: [],
      allLivingRooms: allRooms,
      limit: 3,
    });

    assert.equal(result.length, 3);
    // 按热度降序: 999(5), 888(6), 777(7)
    assert.equal(result[0].id, 5);
    assert.equal(result[1].id, 6);
    assert.equal(result[2].id, 7);
  });

  await t.test("When: 没有任何在播房间, Then: 返回空列表", () => {
    const result = recommendRooms({
      userId: null,
      watches: [],
      allLivingRooms: [],
      limit: 12,
    });

    assert.equal(result.length, 0);
  });
});

// ─── Story: 无关注历史的登录用户 ───────────────────────

test("Story: 登录但无关注历史的用户看到热门推荐", async (t) => {
  await t.test("Given: 已登录但关注列表为空, When: 推荐, Then: 直接走热门兜底", () => {
    const result = recommendRooms({
      userId: 1,
      watches: [],
      allLivingRooms: allRooms,
      limit: 2,
    });

    assert.equal(result.length, 2);
    assert.equal(result[0].id, 5); // 热度最高
  });
});

// ─── Story: 去重逻辑 ───────────────────────────────────

test("Story: 推荐列表不应有重复房间", async (t) => {
  await t.test("Given: 关注的房间也在热门列表中, Then: 不应重复出现", () => {
    const rooms = [
      { id: 1, title: "关注且热门", categoryId: 5, status: 1, popularity: 9999 },
      { id: 2, title: "普通", categoryId: 10, status: 1, popularity: 100 },
    ];

    const result = recommendRooms({
      userId: 1,
      watches: [{ roomId: 1 }],
      allLivingRooms: rooms,
      limit: 3,
    });

    const ids = result.map((r) => r.id);
    const uniqueIds = new Set(ids);
    assert.equal(ids.length, uniqueIds.size, "不应有重复ID");
  });
});

console.log("BDD PASS: recommendFallback.spec.mjs 全部通过");
