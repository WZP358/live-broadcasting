import { defineStore } from "pinia"
import { computed, ref } from "vue"
import userApi from "@/api/user"

const ADMIN_ROLE_IDS = [1, 2]

export const useUserStore = defineStore(
  "user",
  () => {
    const isLogin = ref(false)
    const userInfo = ref({})
    const userToken = ref("")

    const roleIds = computed(() => userInfo.value?.roleIds || [])
    const isAdmin = computed(() => roleIds.value.some((roleId) => ADMIN_ROLE_IDS.includes(roleId)))

    const login = async (loginParam) => {
      try {
        const res = await userApi.login({
          username: loginParam.username.trim(),
          password: loginParam.password.trim(),
        })

        if (!res) {
          return null
        }

        const { code, data } = res
        if (code !== 0) {
          return null
        }

        userInfo.value = data.user || {}
        userToken.value = data.token || ""
        isLogin.value = true
        return data
      } catch (error) {
        // API 业务错误或网络异常，向上抛出让调用方处理
        throw error
      }
    }

    const logout = () => {
      isLogin.value = false
      userInfo.value = {}
      userToken.value = ""
      window.localStorage.clear()
      window.sessionStorage.clear()
      location.reload()
    }

    const updateAvatar = (avatar) => {
      userInfo.value = {
        ...userInfo.value,
        avatar,
      }
    }

    const updateSecurityInfo = (patch) => {
      userInfo.value = {
        ...userInfo.value,
        ...patch,
      }
    }

    return {
      isLogin,
      userInfo,
      userToken,
      roleIds,
      isAdmin,
      login,
      logout,
      updateAvatar,
      updateSecurityInfo,
    }
  },
  {
    persist: true,
  }
)

export default useUserStore
