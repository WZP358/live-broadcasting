<script setup>
import LiveRoom from "@/components/LiveRoom.vue"
import { useRouter } from "vue-router"

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
        src="http://localhost:9000/live.file.bucket/%E3%80%90%E6%8A%A5%E7%BA%B8%E5%A5%B3%E5%AD%A9%E3%80%912024-09-03%2007_35_35_1725495031.png"
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
