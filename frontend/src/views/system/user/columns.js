export const createUserColumns = () => [
  { title: "用户ID", dataIndex: "id", key: "id", width: 100, fixed: "left" },
  { title: "用户信息", dataIndex: "nickname", key: "nickname", width: 240, fixed: "left" },
  { title: "账号", dataIndex: "username", key: "username", width: 160 },
  { title: "性别", dataIndex: "sex", key: "sex", width: 100 },
  { title: "手机", dataIndex: "mobile", key: "mobile", width: 150 },
  { title: "邮箱", dataIndex: "email", key: "email", width: 220 },
  { title: "状态", dataIndex: "disabled", key: "disabled", width: 110 },
  { title: "个性签名", dataIndex: "signature", key: "signature", ellipsis: true, width: 220 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 180 },
  { title: "操作", key: "action", width: 180, fixed: "right", align: "center" },
]
