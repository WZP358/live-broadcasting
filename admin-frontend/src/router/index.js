import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

import Layout from '@/layout'

export const constantRoutes = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect')
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/login'),
    hidden: true
  },
  {
    path: '/404',
    component: () => import('@/views/error/404'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error/401'),
    hidden: true
  },
  {
    path: '',
    component: Layout,
    redirect: 'index',
    children: [
      {
        path: 'index',
        component: () => import('@/views/live/dashboard'),
        name: 'Index',
        meta: { title: '首页', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: '/live',
    component: Layout,
    redirect: '/live/room',
    alwaysShow: true,
    name: 'Live',
    meta: { title: '直播管理', icon: 'monitor' },
    children: [
      {
        path: 'room',
        component: () => import('@/views/live/room'),
        name: 'LiveRoom',
        meta: { title: '直播间管理', icon: 'tree-table' }
      },
      {
        path: 'category',
        component: () => import('@/views/live/category'),
        name: 'LiveCategory',
        meta: { title: '分类管理', icon: 'tree' }
      },
      {
        path: 'gift',
        component: () => import('@/views/live/gift'),
        name: 'LiveGift',
        meta: { title: '礼物管理', icon: 'example' }
      }
    ]
  },
  {
    path: '/user-center',
    component: Layout,
    redirect: '/user-center/user',
    alwaysShow: true,
    name: 'UserCenter',
    meta: { title: '用户中心', icon: 'user' },
    children: [
      {
        path: 'user',
        component: () => import('@/views/live/user'),
        name: 'LiveUser',
        meta: { title: '用户管理', icon: 'peoples' }
      },
      {
        path: 'auth',
        component: () => import('@/views/live/auth'),
        name: 'LiveAuth',
        meta: { title: '认证审核', icon: 'validCode' }
      },
      {
        path: 'message',
        component: () => import('@/views/live/message'),
        name: 'LiveMessage',
        meta: { title: '消息管理', icon: 'message' }
      }
    ]
  },
  {
    path: '/finance',
    component: Layout,
    redirect: '/finance/bill',
    alwaysShow: true,
    name: 'Finance',
    meta: { title: '财务管理', icon: 'money' },
    children: [
      {
        path: 'bill',
        component: () => import('@/views/live/bill'),
        name: 'LiveBill',
        meta: { title: '账单流水', icon: 'form' }
      }
    ]
  },
  {
    path: '/system',
    component: Layout,
    redirect: '/system/role',
    alwaysShow: true,
    name: 'System',
    meta: { title: '系统管理', icon: 'system' },
    children: [
      {
        path: 'role',
        component: () => import('@/views/live/role'),
        name: 'SystemRole',
        meta: { title: '角色管理', icon: 'peoples' }
      },
      {
        path: 'menu',
        component: () => import('@/views/live/menu'),
        name: 'SystemMenu',
        meta: { title: '菜单管理', icon: 'tree-table' }
      },
      {
        path: 'dict',
        component: () => import('@/views/live/dict'),
        name: 'SystemDict',
        meta: { title: '字典管理', icon: 'dict' }
      }
    ]
  },
  {
    path: '*',
    redirect: '/404',
    hidden: true
  }
]

export const dynamicRoutes = []

let routerPush = Router.prototype.push
let routerReplace = Router.prototype.replace
Router.prototype.push = function push(location) {
  return routerPush.call(this, location).catch(err => err)
}
Router.prototype.replace = function replace(location) {
  return routerReplace.call(this, location).catch(err => err)
}

export default new Router({
  mode: 'history',
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes
})
