import test from "node:test";
import assert from "node:assert/strict";

import { normalizeCategories, normalizeSearchRooms } from "../../src/utils/categoryPresenter.js";

test("normalizeCategories 只保留有效启用分类并按 sort desc/id asc 排序", () => {
  const result = normalizeCategories([
    { id: 2, name: "娱乐", sort: 20, status: 0 },
    { id: 1, name: "游戏", sort: 20, status: 0 },
    { id: 3, name: "禁用", sort: 99, status: -1 },
    { id: 2, name: "重复娱乐", sort: 30, status: 0 },
    { id: null, name: "无效", sort: 1, status: 0 },
    { id: 4, name: "", sort: 1, status: 0 },
  ]);

  assert.deepEqual(result.map((item) => item.id), [1, 2]);
  assert.equal(result[0].name, "游戏");
});

test("normalizeSearchRooms 将搜索结果转换成直播卡片可消费结构", () => {
  const [room] = normalizeSearchRooms([
    {
      roomId: 10,
      roomTitle: "前端开发直播",
      anchorName: "小明",
      anchorAvatar: "/avatar.png",
      categoryId: 3,
      categoryName: "技术",
    },
  ]);

  assert.equal(room.id, 10);
  assert.equal(room.title, "前端开发直播");
  assert.deepEqual(room.userInfo, { id: undefined, name: "小明", avatar: "/avatar.png" });
  assert.deepEqual(room.categoryInfo, { id: 3, name: "技术" });
});
