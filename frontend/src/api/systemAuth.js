import request from "@/utils/request"

export default {
  getPageAuths(params) {
    return request({
      url: "/api/v1/system/auth/page",
      method: "get",
      params: {
        pageNo: params?.page,
        pageSize: params?.limit,
        status: params?.status,
        userId: params?.userId,
      },
    })
  },
  updateStatus(type, ids) {
    const statusMap = {
      pass: 1,
      reset: 0,
      reject: 3,
    }
    return request({
      url: "/api/v1/system/auth/status",
      method: "post",
      data: { ids, type: statusMap[type] },
    })
  },
  deleteAuths(ids) {
    return request({
      url: "/api/v1/system/auth/delete",
      method: "post",
      data: { ids },
    })
  },
}
