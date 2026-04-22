export const createAuthColumns = () => [
  { title: "记录ID", dataIndex: "id", key: "id", width: 100, fixed: "left" },
  { title: "用户ID", dataIndex: "userId", key: "userId", width: 100 },
  { title: "真实姓名", dataIndex: "realName", key: "realName", width: 140 },
  { title: "身份证号", dataIndex: "cardNo", key: "cardNo", width: 220 },
  { title: "审核状态", dataIndex: "status", key: "status", width: 120 },
  { title: "证件图片", key: "images", width: 180 },
  { title: "驳回原因", dataIndex: "rejectReason", key: "rejectReason", width: 220 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 180 },
  { title: "操作", key: "action", width: 220, fixed: "right", align: "center" },
]
