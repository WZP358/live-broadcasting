export const createRoleColumns = () => [
  { title: "角色ID", dataIndex: "id", key: "id", width: 90 },
  { title: "角色名称", dataIndex: "name", key: "name", width: 160 },
  { title: "级别", dataIndex: "level", key: "level", width: 100 },
  { title: "权限描述", dataIndex: "permission", key: "permission", ellipsis: true },
  { title: "操作", key: "action", width: 150, align: "center" },
]

export const assignedMenuColumns = [
  { title: "菜单名称", dataIndex: "title", key: "title" },
  { title: "路径", dataIndex: "path", key: "path", width: 200 },
  { title: "类型", dataIndex: "menuType", key: "menuType", width: 110 },
  { title: "操作", key: "action", width: 110, align: "center" },
]
