export const createMenuColumns = () => [
  { title: "菜单ID", dataIndex: "id", key: "id", width: 100 },
  { title: "菜单名称", dataIndex: "title", key: "title", width: 220 },
  { title: "图标", dataIndex: "icon", key: "icon", width: 180 },
  { title: "路由路径", dataIndex: "path", key: "path", width: 200 },
  { title: "父级ID", dataIndex: "pid", key: "pid", width: 100 },
  { title: "排序", dataIndex: "sort", key: "sort", width: 100 },
  { title: "状态", dataIndex: "status", key: "status", width: 120 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 180 },
  { title: "操作", key: "action", width: 150, align: "center", fixed: "right" },
]
