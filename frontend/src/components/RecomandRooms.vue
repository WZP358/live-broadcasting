<script setup>
import LiveRoom from "@/components/LiveRoom.vue"
import { useRouter } from "vue-router"
import { FALLBACK_AVATAR } from "@/utils/fallback"

const router = useRouter()

const handleMoreClick = () => {}
const roomClick = () => {
  router.push({ path: "/room/1" })
}
</script>

<template>
  <div class="recommend-content-wrapper">
    <div class="header">
      <img
        :src="FALLBACK_AVATAR"
        alt="" />
      <h1 title>王者荣耀</h1>
      <span @click="handleMoreClick">更多</span>
    </div>
    <div class="content">
      <LiveRoom @click="roomClick"></LiveRoom>
      <LiveRoom @click="roomClick"></LiveRoom>
      <LiveRoom @click="roomClick"></LiveRoom>
      <LiveRoom @click="roomClick"></LiveRoom>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.recommend-content-wrapper {
  width: 1280px;
  height: 310px;
  display: flex;
  flex-direction: column;
  margin: 0 auto;
  .header {
    padding: 10px 10px 0px 10px;
    display: flex;
    align-items: center;
    position: relative;
    img {
      height: 25px;
      width: 25px;
      margin: 0px 5px;
      border-radius: 50%;
    }
    h1[title] {
      display: inline;
      font-size: 20px;
      color: $font-color;
    }
    span {
      position: absolute;
      right: 15px;
      color: $font-color;
      font-size: 14px;
      cursor: pointer;
    }
    span:hover {
      color: $primary-color;
    }
  }
  .content {
    display: grid;
    grid-template-columns: repeat(4, 320px);
  }
}
</style>
