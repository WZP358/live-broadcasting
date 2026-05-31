/**
 * BDD Specification: 用户核心流程
 *
 * 采用 Given / When / Then 风格描述关键业务行为，
 * 每个 spec 对应一条端到端用户故事。
 */
import test from "node:test";
import assert from "node:assert/strict";

import {
  normalizeLivingRooms,
  filterRoomsByKeyword,
  sortRoomsByMode,
  buildHotRanking,
} from "../../src/utils/liveRoomPresenter.js";

// ─── Story: 游客浏览首页 ───────────────────────────────

test("Story: 游客打开首页看到热门直播间", async (t) => {
  // Given: 后端返回若干开播中的房间
  const rawRooms = [
    { id: 1, title: "前端技术直播", popularity: 5200 },
    { id: 2, title: "音乐电台", popularity: 31000 },
    { id: 3, title: "游戏实况", popularity: 8700 },
  ];

  await t.test("When: 页面加载完成", () => {
    // Then: 房间列表被标准化处理
    const rooms = normalizeLivingRooms(rawRooms);
    assert.equal(rooms.length, 3);
  });

  await t.test("Then: 房间按热度排序展示", () => {
    const sorted = sortRoomsByMode(normalizeLivingRooms(rawRooms), "hot");
    assert.equal(sorted[0].title, "音乐电台");
    assert.equal(sorted[1].title, "游戏实况");
  });
});

// ─── Story: 分类筛选 ───────────────────────────────

test("Story: 用户按分类筛选直播间", async (t) => {
  const rooms = normalizeLivingRooms([
    { id: 1, title: "Vue3教学", categoryInfo: { name: "技术" }, popularity: 500 },
    { id: 2, title: "React直播", categoryInfo: { name: "技术" }, popularity: 300 },
    { id: 3, title: "唱歌", categoryInfo: { name: "娱乐" }, popularity: 800 },
  ]);

  await t.test("When: 搜索 '技术'", () => {
    const filtered = filterRoomsByKeyword(rooms, "技术");
    assert.equal(filtered.length, 2, "categoryInfo.name 也会被搜索，两个技术房间匹配");
  });

  await t.test("When: 搜索 'Vue'", () => {
    const filtered = filterRoomsByKeyword(rooms, "vue");
    assert.equal(filtered.length, 1);
    assert.equal(filtered[0].id, 1);
  });
});

// ─── Story: 主播下播后房间从首页消失 ────────────────────

test("Story: 主播下播后房间应立即从首页列表消失", async (t) => {
  // Given: 首页展示 3 个直播中房间
  const beforeStop = [
    { id: 1, title: "主播A", popularity: 1000 },
    { id: 2, title: "主播B", popularity: 800 },
    { id: 3, title: "主播C", popularity: 600 },
  ];

  await t.test("When: 房间2的主播下播（从API返回的列表中移除）", () => {
    // 模拟 API 不再返回房间2
    const afterStop = beforeStop.filter((r) => r.id !== 2);

    // Then: 首页只展示2个房间
    assert.equal(afterStop.length, 2);
    assert.ok(!afterStop.find((r) => r.id === 2), "下播房间不应出现在列表中");
  });

  await t.test("Then: 热门排行也应排除下播房间", () => {
    const afterStop = beforeStop.filter((r) => r.id !== 2);
    const ranking = buildHotRanking(normalizeLivingRooms(afterStop), 5);
    assert.equal(ranking.length, 2);
    assert.ok(!ranking.find((r) => r.id === 2));
  });
});

// ─── Story: 搜索直播间 ───────────────────────────────

test("Story: 用户搜索直播间", async (t) => {
  // Given: 有多个直播间
  const rooms = normalizeLivingRooms([
    { id: 1, title: "Python入门", introduce: "零基础教学", categoryInfo: { name: "教育" } },
    { id: 2, title: "JavaScript进阶", introduce: "高级教程" },
    { id: 3, title: "Go语言实战" },
  ]);

  await t.test("When: 搜索 'python'", () => {
    const result = filterRoomsByKeyword(rooms, "python");
    assert.equal(result.length, 1);
    assert.equal(result[0].id, 1);
  });

  await t.test("When: 搜索不存在的关键词", () => {
    const result = filterRoomsByKeyword(rooms, "rust");
    assert.equal(result.length, 0);
  });
});

// ─── Story: 低延迟直播间优先展示 ────────────────────────

test("Story: recommend模式下浏览器直播优先展示", () => {
  // Given: 有普通直播和低延迟直播
  const rooms = normalizeLivingRooms([
    { id: 1, title: "普通直播A", popularity: 100, browserLive: false },
    { id: 2, title: "低延迟直播", popularity: 50, browserLive: true },
    { id: 3, title: "普通直播B", popularity: 90, browserLive: false, pullUrl: "rtmp://..." },
  ]);

  // When: 按 recommend 模式排序
  const sorted = sortRoomsByMode(rooms, "recommend");

  // Then: 低延迟直播排第一（browserLive +2000 权重），有回退源的次之（pullUrl +500）
  assert.equal(sorted[0].id, 2, "浏览器低延迟直播应排第一");
  assert.equal(sorted[1].id, 3, "有回退源的排第二");
  assert.equal(sorted[2].id, 1, "普通直播排最后");
});
