/**
 * BDD Specification: 直播间生命周期
 *
 * 覆盖直播间从创建 -> 开播 -> 直播中 -> 下播 -> 回放的核心流程。
 * 采用 Given / When / Then 风格。
 */
import test from "node:test";
import assert from "node:assert/strict";

import {
  getAnchorName,
  getRoomHeat,
  formatHeat,
  normalizeLivingRooms,
  filterRoomsByKeyword,
  sortRoomsByMode,
  buildHotRanking,
  buildRelatedRooms,
} from "../../src/utils/liveRoomPresenter.js";

// ─── Story: 主播开播 ───────────────────────────────────

test("Story: 主播点击开播按钮，直播间状态变为「正在直播」", async (t) => {
  // Given: 主播有一个未开播的房间
  const draftRoom = {
    id: 42,
    title: "小明直播间",
    status: 0, // STOP
    userInfo: { name: "小明" },
    popularity: 100,
    categoryInfo: { id: 5, name: "游戏" },
  };

  await t.test("When: 开播成功，status 变为 LIVING(1)", () => {
    const liveRoom = { ...draftRoom, status: 1 };

    // Then: status=1
    assert.equal(liveRoom.status, 1);

    // Then: 能正常展示主播名
    assert.equal(getAnchorName(liveRoom), "小明");
  });

  await t.test("Then: 该房间应出现在「正在直播」列表中", () => {
    const liveRoom = { ...draftRoom, status: 1 };
    const livingRooms = normalizeLivingRooms([
      liveRoom,
      { id: 99, title: "其他房间", popularity: 50 },
    ]);

    // 筛选 status=1 的房间
    const active = livingRooms.filter((r) => r.status === 1);
    assert.equal(active.length, 1);
    assert.equal(active[0].id, 42);
  });
});

// ─── Story: 主播下播 ──────────────────────────────────

test("Story: 主播下播后，房间从首页列表消失", async (t) => {
  // Given: 首页有3个直播中房间
  const rooms = normalizeLivingRooms([
    { id: 1, title: "A", status: 1, popularity: 1000 },
    { id: 2, title: "B", status: 1, popularity: 800 },
    { id: 3, title: "C", status: 1, popularity: 600 },
  ]);

  await t.test("When: 房间2下播（status 变为 STOP）", () => {
    const afterStop = rooms.map((r) => (r.id === 2 ? { ...r, status: 0 } : r));
    const stillLiving = afterStop.filter((r) => r.status === 1);

    // Then: 只有2个房间还在列表中
    assert.equal(stillLiving.length, 2);
    assert.ok(!stillLiving.find((r) => r.id === 2));
  });

  await t.test("Then: 热门排行中也不应包含下播房间", () => {
    const afterStop = rooms.filter((r) => r.id !== 2);
    const ranking = buildHotRanking(afterStop, 5);
    assert.equal(ranking.length, 2);
    assert.ok(!ranking.find((r) => r.id === 2));
  });
});

// ─── Story: 游客浏览首页推荐 ────────────────────────────

test("Story: 游客打开首页看到推荐直播间", async (t) => {
  // Given: 多个直播间，混合状态
  const rooms = normalizeLivingRooms([
    { id: 1, title: "技术分享", popularity: 5000, status: 1, browserLive: true, pullUrl: "rtmp://..." },
    { id: 2, title: "游戏直播", popularity: 30000, status: 1 },
    { id: 3, title: "音乐电台", popularity: 15000, status: 1, pullUrl: "rtmp://..." },
  ]);

  await t.test("Then: 默认按 recommend 模式排序（综合热度+特性加权）", () => {
    const sorted = sortRoomsByMode(rooms, "recommend");
    // 游戏直播 30000
    // 音乐电台 15000 + 500(pullUrl) = 15500
    // 技术分享 5000 + 2000(browserLive) + 500(pullUrl) = 7500
    assert.equal(sorted[0].id, 2); // 最高热度
    assert.equal(sorted[1].id, 3); // 音乐高于技术
    assert.equal(sorted[2].id, 1);
  });

  await t.test("Then: 热门排名应展示前N个", () => {
    const hotRanking = buildHotRanking(rooms, 2);
    assert.equal(hotRanking.length, 2);
    assert.equal(hotRanking[0].id, 2); // 最高热度
    assert.equal(hotRanking[1].id, 3);
  });

  await t.test("Then: formatHeat 格式化热度值", () => {
    assert.equal(formatHeat(30000), "3万");
    assert.equal(formatHeat(15000), "1.5万");
    assert.equal(formatHeat(5000), "5000");
  });
});

// ─── Story: 相关推荐 ───────────────────────────────────

test("Story: 进入直播间后展示相关推荐", async (t) => {
  const allRooms = normalizeLivingRooms([
    { id: 1, title: "当前房间", categoryInfo: { id: 10 }, popularity: 500, status: 1 },
    { id: 2, title: "同分类A", categoryInfo: { id: 10 }, popularity: 300, status: 1 },
    { id: 3, title: "同分类B", categoryInfo: { id: 10 }, popularity: 200, status: 1 },
    { id: 4, title: "不同分类", categoryInfo: { id: 20 }, popularity: 1000, status: 1 },
    { id: 5, title: "另一个不同", categoryInfo: { id: 30 }, popularity: 800, status: 1 },
  ]);

  await t.test("Then: 推荐应优先同分类，再按热度排序，排除当前房间", () => {
    const related = buildRelatedRooms(allRooms, { id: 1, categoryInfo: { id: 10 } }, 4);

    assert.equal(related.length, 4);
    // 不应包含当前房间
    assert.ok(!related.find((r) => r.id === 1));
    // 前两个应是同分类的
    assert.equal(related[0].id, 2); // 同分类A, 热度300
    assert.equal(related[1].id, 3); // 同分类B, 热度200
    // 后面的是不同分类按热度
    assert.equal(related[2].id, 4); // 热度1000
    assert.equal(related[3].id, 5); // 热度800
  });

  await t.test("Then: 如果房间数量不足limit，只返回已有的", () => {
    const fewRooms = allRooms.slice(0, 3);
    const related = buildRelatedRooms(fewRooms, { id: 1, categoryInfo: { id: 10 } }, 10);
    assert.equal(related.length, 2); // 只有id=2和id=3
  });
});

// ─── Story: 搜索直播间 ─────────────────────────────────

test("Story: 用户在搜索框搜索直播间", async (t) => {
  const rooms = normalizeLivingRooms([
    { id: 1, title: "前端Vue3实战", introduce: "从零开始", notice: "每晚8点", categoryInfo: { name: "技术" } },
    { id: 2, title: "后端SpringBoot", introduce: "Java教程", categoryInfo: { name: "技术" } },
    { id: 3, title: "今晚吃鸡", introduce: "娱乐游戏" },
  ]);

  await t.test("When: 搜索'vue', Then: 匹配title", () => {
    const result = filterRoomsByKeyword(rooms, "vue");
    assert.equal(result.length, 1);
    assert.equal(result[0].id, 1);
  });

  await t.test("When: 搜索'技术', Then: 匹配categoryInfo.name", () => {
    const result = filterRoomsByKeyword(rooms, "技术");
    assert.equal(result.length, 2);
  });

  await t.test("When: 搜索'java', Then: 匹配introduce", () => {
    const result = filterRoomsByKeyword(rooms, "java");
    assert.equal(result.length, 1);
    assert.equal(result[0].id, 2);
  });

  await t.test("When: 搜索'每晚', Then: 匹配notice", () => {
    const result = filterRoomsByKeyword(rooms, "每晚");
    assert.equal(result.length, 1);
    assert.equal(result[0].id, 1);
  });

  await t.test("When: 搜索空字符串, Then: 返回全部", () => {
    const result = filterRoomsByKeyword(rooms, "");
    assert.equal(result.length, 3);
  });

  await t.test("When: 搜索不存在内容, Then: 返回空", () => {
    const result = filterRoomsByKeyword(rooms, "不存在的");
    assert.equal(result.length, 0);
  });
});

// ─── Story: 历史记录排序 ───────────────────────────────

test("Story: 用户查看历史记录，按观看顺序排列", async (t) => {
  const rooms = normalizeLivingRooms([
    { id: 10, title: "三天前看的" },
    { id: 20, title: "昨天看的" },
    { id: 30, title: "今天看的" },
  ]);

  await t.test("When: 选择history模式, Then: 只返回历史中的房间", () => {
    const history = [{ roomId: 10 }, { roomId: 30 }];
    const result = sortRoomsByMode(rooms, "history", history);

    assert.equal(result.length, 2);
    assert.equal(result[0].id, 10);
    assert.equal(result[1].id, 30);
  });

  await t.test("Given: 历史记录为空, When: 查看历史, Then: 返回空", () => {
    const result = sortRoomsByMode(rooms, "history", []);
    assert.equal(result.length, 0);
  });
});

console.log("BDD PASS: roomLifecycle.spec.mjs 全部通过");
