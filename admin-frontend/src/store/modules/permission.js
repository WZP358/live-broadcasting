import { constantRoutes } from '@/router'

const permission = {
  state: {
    routes: [],
    addRoutes: [],
    defaultRoutes: [],
    topbarRouters: [],
    sidebarRouters: []
  },
  mutations: {
    SET_ROUTES: (state, routes) => {
      state.addRoutes = []
      state.routes = routes
    },
    SET_DEFAULT_ROUTES: (state, routes) => {
      state.defaultRoutes = routes
    },
    SET_TOPBAR_ROUTES: (state, routes) => {
      state.topbarRouters = routes
    },
    SET_SIDEBAR_ROUTERS: (state, routes) => {
      state.sidebarRouters = routes
    }
  },
  actions: {
    GenerateRoutes({ commit }) {
      return new Promise(resolve => {
        commit('SET_ROUTES', constantRoutes)
        commit('SET_SIDEBAR_ROUTERS', constantRoutes)
        commit('SET_DEFAULT_ROUTES', constantRoutes)
        commit('SET_TOPBAR_ROUTES', constantRoutes)
        resolve([])
      })
    }
  }
}

export default permission
