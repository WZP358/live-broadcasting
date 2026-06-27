import { createRouter, createWebHashHistory } from "vue-router"
import { useUserStore } from "@/stores/modules/user"

import DefaultLayout from "@/layout/DefaultLayout.vue"
import SystemLayout from "@/layout/SystemLayout.vue"

const routes = [
  {
    path: "/login",
    component: () => import("@/views/login.vue"),
    meta: { title: "登录", guestOnly: true },
  },
  {
    path: "/admin-login",
    redirect: "/login",
  },
  {
    path: "/register",
    component: () => import("@/views/register.vue"),
    meta: { title: "注册" },
  },
  {
    path: "/system",
    component: SystemLayout,
    redirect: "/system/dashboard",
    meta: { title: "管理后台", requiresAuth: true, adminOnly: true },
    children: [
      {
        path: "/system/dashboard",
        component: () => import("@/views/system/dashboard/index.vue"),
        meta: { title: "控制台", requiresAuth: true, adminOnly: true },
      },
      {
        path: "/system/user-manage",
        component: () => import("@/views/system/user/index.vue"),
        meta: { title: "用户管理", requiresAuth: true, adminOnly: true },
      },
      {
        path: "/system/message-manage",
        component: () => import("@/views/system/message/index.vue"),
        meta: { title: "消息管理", requiresAuth: true, adminOnly: true },
      },
      {
        path: "/system/content-audit",
        component: () => import("@/views/system/audit/index.vue"),
        meta: { title: "内容审核", requiresAuth: true, adminOnly: true },
      },
      {
        path: "/system/customer-service",
        component: () => import("@/views/system/customer-service/index.vue"),
        meta: { title: "客服处理", requiresAuth: true, adminOnly: true },
      },
      {
        path: "/system/present-manage",
        component: () => import("@/views/system/gift/index.vue"),
        meta: { title: "礼物管理", requiresAuth: true, adminOnly: true },
      },
      {
        path: "/system/gift-flow",
        component: () => import("@/views/system/gift-flow/index.vue"),
        meta: { title: "礼物流水", requiresAuth: true, adminOnly: true },
      },
      {
        path: "/system/system-manage/system-manage-menu",
        component: () => import("@/views/system/menu2/index.vue"),
        meta: { title: "菜单管理", requiresAuth: true, adminOnly: true },
      },
      {
        path: "/system/system-manage/user-role-manage",
        component: () => import("@/views/system/role/index.vue"),
        meta: { title: "角色管理", requiresAuth: true, adminOnly: true },
      },
      {
        path: "/system/system-manage",
        component: () => import("@/views/system/manage/menu.vue"),
        meta: { title: "系统管理", requiresAuth: true, adminOnly: true },
      },
      {
        path: "/system/category-manage",
        component: () => import("@/views/system/category/index.vue"),
        meta: { title: "分类管理", requiresAuth: true, adminOnly: true },
      },
      {
        path: "/system/live-center/live-room-manage",
        component: () => import("@/views/system/room/index.vue"),
        meta: { title: "直播间管理", requiresAuth: true, adminOnly: true },
      },
      {
        path: "/system/bill",
        component: () => import("@/views/system/bill/index.vue"),
        meta: { title: "账单管理", requiresAuth: true, adminOnly: true },
      },
      {
        path: "/system/system-manage/dictionary-manage",
        component: () => import("@/views/system/dict/index.vue"),
        meta: { title: "字典管理", requiresAuth: true, adminOnly: true },
      },
      {
        path: "/system/system-manage/system-config",
        component: () => import("@/views/system/config/index.vue"),
        meta: { title: "系统配置", requiresAuth: true, adminOnly: true },
      },
    ],
  },
  {
    path: "/",
    component: DefaultLayout,
    redirect: "/home",
    meta: { title: "前台" },
    children: [
      {
        path: "/home",
        component: () => import("@/views/home.vue"),
        meta: { title: "首页" },
      },
      {
        path: "/discover",
        redirect: "/home",
        meta: { title: "首页" },
      },
      {
        path: "/search",
        component: () => import("@/views/search.vue"),
        meta: { title: "搜索" },
      },
      {
        path: "/room/:id",
        component: () => import("@/views/room/index.vue"),
        meta: { title: "直播间" },
      },
      {
        path: "/live/studio",
        component: () => import("@/views/live-studio/index.vue"),
        meta: { title: "开播工作台", requiresAuth: true },
      },
      {
        path: "/center",
        component: () => import("@/views/center/index.vue"),
        redirect: "/center/personnel/profile",
        meta: { title: "个人中心", requiresAuth: true },
        children: [
          {
            path: "personnel/profile",
            component: () => import("@/views/center/profile/index.vue"),
            meta: { title: "个人资料", requiresAuth: true },
          },
          {
            path: "personnel/follow",
            component: () => import("@/views/center/follow/index.vue"),
            meta: { title: "我的关注", requiresAuth: true },
          },
          {
            path: "personnel/history",
            component: () => import("@/views/center/view-history/index.vue"),
            meta: { title: "观看历史", requiresAuth: true },
          },
          {
            path: "dollar/wallet",
            component: () => import("@/views/center/wallet/index.vue"),
            meta: { title: "我的钱包", requiresAuth: true },
          },
          {
            path: "statistic/punishment",
            component: () => import("@/views/center/punishment/index.vue"),
            meta: { title: "违规记录", requiresAuth: true },
          },
          {
            path: "statistic/overview",
            component: () => import("@/views/center/overview/index.vue"),
            meta: { title: "数据概览", requiresAuth: true },
          },
          {
            path: "live/live-settings",
            component: () => import("@/views/center/live-settings/index.vue"),
            meta: { title: "开播设置", requiresAuth: true },
          },
          {
            path: "statistic/gift-list",
            component: () => import("@/views/center/gift-list/index.vue"),
            meta: { title: "礼物记录", requiresAuth: true },
          },
          {
            path: "dollar/recharge",
            component: () => import("@/views/center/recharge/index.vue"),
            meta: { title: "充值", requiresAuth: true },
          },
          {
            path: "dollar/bill",
            component: () => import("@/views/center/bill/index.vue"),
            meta: { title: "账单", requiresAuth: true },
          },
          {
            path: "messages",
            component: () => import("@/views/center/messages/index.vue"),
            meta: { title: "消息中心", requiresAuth: true },
          },
          {
            path: "messages/messages",
            redirect: "/center/messages",
          },
          {
            path: "messages/customer-service",
            component: () => import("@/views/center/customer-service/index.vue"),
            meta: { title: "联系客服", requiresAuth: true },
          },
          {
            path: "live/guardian",
            component: () => import("@/views/center/live/guardian.vue"),
            meta: { title: "守护管理", requiresAuth: true },
          },
          {
            path: "live/dashboard",
            component: () => import("@/views/center/live/dashboard.vue"),
            meta: { title: "数据看板", requiresAuth: true },
          },
        ],
      },
    ],
  },
  {
    path: "/403",
    component: () => import("@/views/default/403.vue"),
    meta: { title: "无权限" },
  },
  {
    path: "/:pathMatch(.*)*",
    component: () => import("@/views/default/404.vue"),
    meta: { title: "页面不存在" },
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const loggedIn = userStore.isLogin
  const admin = userStore.isAdmin

  if (to.meta?.guestOnly && loggedIn) {
    next(admin ? "/system/dashboard" : "/")
    return
  }

  if (to.meta?.requiresAuth && !loggedIn) {
    next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
    return
  }

  const isSystemRoute = to.path.startsWith("/system")
  if (loggedIn && admin && !isSystemRoute && to.path !== "/403") {
    next("/system/dashboard")
    return
  }

  if (to.meta?.adminOnly && !admin) {
    next("/403")
    return
  }

  next()
})

export default router
