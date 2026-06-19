export const normalizeCategories = (list = []) => {
  const seen = new Set();
  return list
    .filter((item) => item && item.id != null && item.name && item.status !== -1)
    .filter((item) => {
      const id = Number(item.id);
      if (!Number.isFinite(id) || seen.has(id)) return false;
      seen.add(id);
      return true;
    })
    .map((item) => ({
      ...item,
      id: Number(item.id),
      sort: Number(item.sort || 0),
    }))
    .sort((a, b) => (Number(b.sort || 0) - Number(a.sort || 0)) || (Number(a.id || 0) - Number(b.id || 0)));
};

export const normalizeSearchRooms = (rooms = []) =>
  rooms.map((item) => {
    const id = Number(item.id || item.roomId || 0);
    const title = item.title || item.roomTitle || "直播间";
    const categoryId = item.categoryInfo?.id || item.categoryId;
    const categoryName = item.categoryInfo?.name || item.categoryName;
    const userInfo = item.userInfo || {
      id: item.anchorId,
      name: item.anchorName,
      avatar: item.anchorAvatar,
    };

    return {
      ...item,
      id,
      title,
      categoryId,
      categoryInfo: categoryId || categoryName ? { id: categoryId, name: categoryName } : item.categoryInfo,
      userInfo,
    };
  });
