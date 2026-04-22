import request from "@/utils/request"

export default {
  getSummary() {
    return request({
      url: "/api/v1/system/dashboard/summary",
      method: "get",
    })
  },
}
