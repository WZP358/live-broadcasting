import store from '@/store'
import router from '@/router'
import { MessageBox } from 'element-ui'
import { login, logout, getInfo } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { isHttp, isEmpty } from '@/utils/validate'
import defAva from '@/assets/images/profile.jpg'

const user = {
  state: {
    token: getToken(),
    id: '',
    name: '',
    nickName: '',
    avatar: '',
    roles: [],
    permissions: []
  },

  mutations: {
    SET_TOKEN: (state, token) => { state.token = token },
    SET_ID: (state, id) => { state.id = id },
    SET_NAME: (state, name) => { state.name = name },
    SET_NICK_NAME: (state, nickName) => { state.nickName = nickName },
    SET_AVATAR: (state, avatar) => { state.avatar = avatar },
    SET_ROLES: (state, roles) => { state.roles = roles },
    SET_PERMISSIONS: (state, permissions) => { state.permissions = permissions }
  },

  actions: {
    Login({ commit }, userInfo) {
      const username = userInfo.username.trim()
      const password = userInfo.password
      return new Promise((resolve, reject) => {
        login(username, password).then(res => {
          const payload = res.data || {}
          const token = payload.token || ''
          const info = payload.user || payload.userInfo || {}
          setToken(token)
          localStorage.setItem('PulseLive-Admin-User', JSON.stringify(info))
          commit('SET_TOKEN', token)
          store.dispatch('lock/unlockScreen')
          resolve()
        }).catch(error => reject(error))
      })
    },

    GetInfo({ commit }) {
      return new Promise((resolve, reject) => {
        getInfo().then(res => {
          const user = res.user || {}
          let avatar = user.avatar || ''
          if (!isHttp(avatar)) {
            avatar = isEmpty(avatar) ? defAva : process.env.VUE_APP_BASE_API + avatar
          }
          commit('SET_ROLES', res.roles && res.roles.length ? res.roles : ['admin'])
          commit('SET_PERMISSIONS', res.permissions || ['*:*:*'])
          commit('SET_ID', user.userId)
          commit('SET_NAME', user.userName)
          commit('SET_NICK_NAME', user.nickName)
          commit('SET_AVATAR', avatar)
          resolve(res)
        }).catch(error => reject(error))
      })
    },

    LogOut({ commit }) {
      return new Promise((resolve, reject) => {
        logout().then(() => {
          commit('SET_TOKEN', '')
          commit('SET_ROLES', [])
          commit('SET_PERMISSIONS', [])
          localStorage.removeItem('PulseLive-Admin-User')
          removeToken()
          resolve()
        }).catch(error => reject(error))
      })
    },

    FedLogOut({ commit }) {
      return new Promise(resolve => {
        commit('SET_TOKEN', '')
        removeToken()
        resolve()
      })
    }
  }
}

export default user
