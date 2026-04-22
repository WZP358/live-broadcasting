export const createGiftColumns = () => [
  { title: "礼物ID", dataIndex: "id", key: "id", width: 100 },
  { title: "礼物信息", dataIndex: "name", key: "name", width: 240 },
  { title: "价格", dataIndex: "price", key: "price", width: 120 },
  { title: "排序", dataIndex: "sort", key: "sort", width: 100 },
  { title: "状态", dataIndex: "disabled", key: "disabled", width: 120 },
  { title: "描述", dataIndex: "description", key: "description", ellipsis: true },
  { title: "操作", key: "action", width: 160, align: "center" },
]
