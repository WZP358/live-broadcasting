import request from "@/utils/request"

export default {
  getPage(params) {
    return request({
      url: "/api/v1/system/gift-flow/page",
      method: "get",
      params,
    })
  },
  getSummary(params) {
    return request({
      url: "/api/v1/system/gift-flow/summary",
      method: "get",
      params,
    })
  },
}
