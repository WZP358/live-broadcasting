export const getAnchorName = (room = {}) =>
  room?.userInfo?.name || room?.userInfo?.nickName || room?.userNickname || "主播";

export const getRoomHeat = (room = {}) => Number(room?.popularity || room?.heat || 0);

export const formatHeat = (value) => {
  const count = Number(value || 0);
  if (!Number.isFinite(count) || count <= 0) return "0";
  if (count >= 100000000) return `${(count / 100000000).toFixed(1).replace(/\.0$/, "")}亿`;
  if (count >= 10000) return `${(count / 10000).toFixed(1).replace(/\.0$/, "")}万`;
  return `${count}`;
};

export const normalizeLivingRooms = (rooms = []) =>
  rooms.map((item, index) => ({
    ...item,
    popularity: Number(item?.popularity || item?.heat || 0) || Math.max(1, rooms.length - index),
    _order: index,
  }));

export const filterRoomsByKeyword = (rooms = [], keyword = "") => {
  const value = keyword.trim().toLowerCase();
  if (!value) return [...rooms];

  return rooms.filter((room) =>
    [room.title, room.introduce, room.notice, getAnchorName(room), room.categoryInfo?.name]
      .filter(Boolean)
      .join(" ")
      .toLowerCase()
      .includes(value),
  );
};

const sortStrategies = {
  hot: (rooms) => [...rooms].sort((a, b) => getRoomHeat(b) - getRoomHeat(a)),
  new: (rooms) => [...rooms].sort((a, b) => Number(b.id || b._order) - Number(a.id || a._order)),
  recommend: (rooms) =>
    [...rooms].sort((a, b) => {
      const aScore = getRoomHeat(a) + (a.browserLive ? 2000 : 0) + (a.pullUrl ? 500 : 0);
      const bScore = getRoomHeat(b) + (b.browserLive ? 2000 : 0) + (b.pullUrl ? 500 : 0);
      return bScore - aScore;
    }),
};

export const sortRoomsByMode = (rooms = [], mode = "recommend", historyRooms = []) => {
  if (mode === "history") {
    const historyIds = new Set(historyRooms.map((item) => Number(item.roomId || item.id)));
    return rooms.filter((room) => historyIds.has(Number(room.id)));
  }

  const strategy = sortStrategies[mode] || sortStrategies.recommend;
  return strategy(rooms);
};

export const buildHotRanking = (rooms = [], limit = 8) => sortStrategies.hot(rooms).slice(0, limit);

export const buildRelatedRooms = (rooms = [], currentRoom = {}, limit = 4) => {
  const roomId = Number(currentRoom?.id || 0);
  const categoryId = currentRoom?.categoryInfo?.id || currentRoom?.categoryId;

  return [...rooms]
    .filter((item) => Number(item.id) !== roomId)
    .sort((a, b) => {
      const aSameCategory = (a.categoryInfo?.id || a.categoryId) === categoryId ? 1 : 0;
      const bSameCategory = (b.categoryInfo?.id || b.categoryId) === categoryId ? 1 : 0;
      return bSameCategory - aSameCategory || getRoomHeat(b) - getRoomHeat(a);
    })
    .slice(0, limit);
};
