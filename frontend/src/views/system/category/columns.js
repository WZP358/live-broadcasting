export const createCategoryColumns = () => [
  { title: "分类ID", dataIndex: "id", key: "id", width: 100 },
  { title: "分类信息", dataIndex: "name", key: "name", width: 260 },
  { title: "排序", dataIndex: "sort", key: "sort", width: 100 },
  { title: "状态", dataIndex: "status", key: "status", width: 120 },
  { title: "更新时间", dataIndex: "updateTime", key: "updateTime", width: 180 },
  { title: "操作", key: "action", width: 160, align: "center" },
]
