export const createRoomColumns = () => [
  { title: "房间ID", dataIndex: "id", key: "id", width: 100, fixed: "left" },
  { title: "直播间信息", dataIndex: "title", key: "title", width: 260, fixed: "left" },
  { title: "主播", dataIndex: "userInfo", key: "userInfo", width: 220 },
  { title: "分类", dataIndex: "categoryInfo", key: "categoryInfo", width: 130 },
  { title: "直播状态", dataIndex: "status", key: "status", width: 120 },
  { title: "封禁状态", dataIndex: "disabled", key: "disabled", width: 120 },
  { title: "公告", dataIndex: "notice", key: "notice", ellipsis: true, width: 220 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 180 },
  { title: "操作", key: "action", width: 180, fixed: "right", align: "center" },
]
