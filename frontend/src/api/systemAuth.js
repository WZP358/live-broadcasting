import request from "@/utils/request"

export default {
  getPageAuths(params) {
    return request({
      url: "/admin/auth/list",
      method: "get",
      params,
    })
  },
  updateStatus(type, ids) {
    return request({
      url: `/admin/auth/pass/${type}`,
      method: "post",
      data: { ids },
    })
  },
  deleteAuths(ids) {
    return request({
      url: "/admin/auth/del",
      method: "post",
      data: { ids },
    })
  },
}
