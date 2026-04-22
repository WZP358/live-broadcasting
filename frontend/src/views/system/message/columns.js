export const createMessageColumns = () => [
  { title: "消息ID", dataIndex: "id", key: "id", width: 100 },
  { title: "直播间", dataIndex: "roomId", key: "roomId", width: 220 },
  { title: "发送用户", dataIndex: "fromUserNickname", key: "fromUserNickname", width: 160 },
  { title: "消息内容", dataIndex: "content", key: "content" },
  { title: "类型", dataIndex: "type", key: "type", width: 120 },
  { title: "状态", dataIndex: "status", key: "status", width: 120 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 180 },
]
