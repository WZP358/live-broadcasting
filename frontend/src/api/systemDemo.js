import request from "@/utils/request"

export default {
  getStatus() {
    return request({
      url: "/api/v1/system/demo/status",
      method: "get",
    })
  },
  enable() {
    return request({
      url: "/api/v1/system/demo/enable",
      method: "post",
    })
  },
  disable() {
    return request({
      url: "/api/v1/system/demo/disable",
      method: "post",
    })
  },
}
