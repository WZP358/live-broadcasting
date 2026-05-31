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

test("getAnchorName 应返回主播名称的优先顺序", () => {
  assert.equal(getAnchorName({ userInfo: { name: "张三" } }), "张三");
  assert.equal(getAnchorName({ userInfo: { nickName: "李四" } }), "李四");
  assert.equal(getAnchorName({ userNickname: "王五" }), "王五");
  assert.equal(getAnchorName({}), "主播");
  assert.equal(getAnchorName(), "主播");
});

test("getRoomHeat 应返回人气值或热度", () => {
  assert.equal(getRoomHeat({ popularity: 100 }), 100);
  assert.equal(getRoomHeat({ heat: 50 }), 50);
  assert.equal(getRoomHeat({ popularity: 200, heat: 300 }), 200); // popularity 优先
  assert.equal(getRoomHeat({}), 0);
});

test("formatHeat 格式化数字为中文单位", () => {
  assert.equal(formatHeat(0), "0");
  assert.equal(formatHeat(-1), "0");
  assert.equal(formatHeat(999), "999");
  assert.equal(formatHeat(10000), "1万");
  assert.equal(formatHeat(12345), "1.2万");
  assert.equal(formatHeat(9999999), "1000万");  // 约1000万，toFixed(1) 进位
  assert.equal(formatHeat(9990000), "999万");    // 正好 999万
  assert.equal(formatHeat(100000000), "1亿");
  assert.equal(formatHeat(150000000), "1.5亿");
  assert.equal(formatHeat(null), "0");
  assert.equal(formatHeat(undefined), "0");
});

test("normalizeLivingRooms 应补全 popularity 并保序", () => {
  const rooms = [
    { id: 1, title: "房间A" },
    { id: 2, title: "房间B", popularity: 50 },
  ];
  const result = normalizeLivingRooms(rooms);
  assert.equal(result.length, 2);
  assert.equal(result[1].popularity, 50);
  assert.equal(typeof result[0].popularity, "number");
  assert.ok(result[0].popularity >= 1);
});

test("normalizeLivingRooms 空数组返回空数组", () => {
  assert.deepEqual(normalizeLivingRooms([]), []);
  assert.deepEqual(normalizeLivingRooms(), []);
});

test("filterRoomsByKeyword 按关键字筛选", () => {
  const rooms = [
    { id: 1, title: "前端直播间", introduce: "Vue3直播" },
    { id: 2, title: "后端直播", notice: "SpringBoot教学" },
    { id: 3, title: "游戏娱乐" },
  ];
  assert.equal(filterRoomsByKeyword(rooms, "vue").length, 1);
  assert.equal(filterRoomsByKeyword(rooms, "直播").length, 2);
  assert.equal(filterRoomsByKeyword(rooms, "").length, 3);
  assert.equal(filterRoomsByKeyword([], "test").length, 0);
});

test("sortRoomsByMode hot 模式按热度降序", () => {
  const rooms = [
    { id: 1, popularity: 10 },
    { id: 2, popularity: 100 },
    { id: 3, popularity: 50 },
  ];
  const result = sortRoomsByMode(rooms, "hot");
  assert.equal(result[0].id, 2);
  assert.equal(result[1].id, 3);
  assert.equal(result[2].id, 1);
});

test("sortRoomsByMode history 模式只返回历史房间", () => {
  const rooms = [
    { id: 1, title: "A" },
    { id: 2, title: "B" },
    { id: 3, title: "C" },
  ];
  const history = [{ roomId: 1 }, { roomId: 3 }];
  const result = sortRoomsByMode(rooms, "history", history);
  assert.equal(result.length, 2);
  assert.equal(result[0].id, 1);
  assert.equal(result[1].id, 3);
});

test("sortRoomsByMode recommend 模式加权排序", () => {
  const rooms = [
    { id: 1, popularity: 10, browserLive: false, pullUrl: null },
    { id: 2, popularity: 5, browserLive: true, pullUrl: null },
    { id: 3, popularity: 10, browserLive: false, pullUrl: "rtmp://..." },
  ];
  const result = sortRoomsByMode(rooms, "recommend");
  // browserLive (+2000) > pullUrl (+500) > 纯热度
  assert.equal(result[0].id, 2, "browserLive 应排第一");
  assert.equal(result[1].id, 3, "有pullUrl应排第二");
  assert.equal(result[2].id, 1, "纯热度排最后");
});

test("buildHotRanking 返回热门排行前N", () => {
  const rooms = Array.from({ length: 10 }, (_, i) => ({
    id: i + 1,
    popularity: (i + 1) * 10,
  }));
  const ranking = buildHotRanking(rooms, 5);
  assert.equal(ranking.length, 5);
  assert.equal(ranking[0].popularity, 100);
});

test("buildRelatedRooms 排除当前房间，同分类优先", () => {
  const rooms = [
    { id: 1, popularity: 100, categoryInfo: { id: 10 } },
    { id: 2, popularity: 50, categoryInfo: { id: 10 } },
    { id: 3, popularity: 80, categoryInfo: { id: 20 } },
    { id: 4, popularity: 10, categoryInfo: { id: 30 } },
  ];
  const result = buildRelatedRooms(rooms, { id: 1, categoryInfo: { id: 10 } }, 3);
  assert.equal(result.length, 3);
  assert.ok(!result.find((r) => r.id === 1), "不应包含当前房间");
  assert.equal(result[0].id, 2, "同分类优先");
});
