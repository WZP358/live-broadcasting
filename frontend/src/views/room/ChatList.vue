<template>
  <div class="chat-wrapper">
    <div class="chat-top-section" @mouseleave="showRankDropdown = false">
      <div class="chat-header">
        <div class="rank-toolbar">
          <span class="rank-title">本月亲密榜</span>
          <div class="rank-dropdown-wrapper" @mouseenter="showRankDropdown = true">
            <a-button type="link" size="small">TOP 10</a-button>
          </div>
        </div>
        <div class="rank-board">
          <div class="rank-item" :class="`rank-item-${item.rankNo}`" v-for="item in displayRanks" :key="item.key">
            <div class="rank-badge">NO.{{ item.rankNo }}</div>
            <img class="avatar" :src="item.avatar || fallbackAvatar" />
            <span class="name">{{ item.nickName }}</span>
            <span class="charm">{{ formatIntimacy(item.intimacyValue) }}</span>
          </div>
        </div>
      </div>
      <div class="rank-dropdown-list" :class="{ 'is-open': showRankDropdown }" @mouseenter="showRankDropdown = true">
        <div class="rank-dropdown-item" v-for="item in rankList" :key="item.userId">
          <div class="rank-dropdown-left">
            <span class="rank-dropdown-no" :class="`rank-no-${item.rankNo}`">{{ item.rankNo }}</span>
            <img class="rank-dropdown-avatar" :src="item.avatar || fallbackAvatar" />
            <span class="rank-dropdown-name">{{ item.nickName || `观众${item.userId}` }}</span>
          </div>
          <span class="rank-dropdown-value">{{ formatIntimacy(item.intimacyValue) }}</span>
        </div>
        <a-empty v-if="rankList.length === 0" description="本月还没有亲密值记录" />
      </div>
    </div>
    <div class="chat-main" ref="scrollContainer" @scroll="handleScroll">
      <a-list size="small" :data-source="data">
        <template #renderItem="{ item }">
          <a-list-item>
            <MessageItem :data="item" />
          </a-list-item>
        </template>
      </a-list>
    </div>
    <div class="chat-footer">
      <a-flex vertical>
        <a-textarea class="chat-box" v-model:value="messageText" :placeholder="isLogin ? '发个弹幕呗～' : '需要登陆才能发送弹幕哦～'"
          show-count :maxlength="20" :disabled="!isLogin" :auto-size="{ minRows: 2, maxRows: 2 }" />
        <div class="chat-btn-wrapper">
          <span class="popularity">{{ popularity || 0 }}人在线</span>
          <a-button type="primary" size="small" @click="handleMessageSend" :disabled="!isLogin">发送</a-button>
        </div>
      </a-flex>
    </div>
  </div>

</template>

<script setup>
import MessageItem from "./MessageItem.vue"
import { onBeforeMount, onMounted, ref, computed, nextTick, watch, onBeforeUnmount } from "vue"
import { useStore } from "@/stores"
import ChatApi from "@/api/chat"
import roomApi from "@/api/room"

const props = defineProps({
  roomId: {
    type: Number,
    default: null,
  },
})

const emits = defineEmits(["sendGift"])

const maxReconnectCount = ref(50)
const lockReconnect = ref(false)
const scrollContainer = ref(null)
const isUserScrolling = ref(false)
const roomId = computed(() => props.roomId)
const store = useStore()
const isLogin = useStore().user().isLogin
const popularity = ref(1)
const rankList = ref([])
const showRankDropdown = ref(false)
const fallbackAvatar = "https://dummyimage.com/96x96/f5f5f5/999999.png&text=TOP"

let websocket = null
const reconnectTimer = ref()
const heartBeatTimer = ref()
const popularityInterval = ref()

const messageText = ref("")

const data = ref([])
const createPlaceholderRank = (rankNo) => ({
  key: `placeholder-${rankNo}`,
  rankNo,
  nickName: ["待上榜", "虚位以待", "冲榜中"][rankNo - 1] || "待上榜",
  avatar: "",
  intimacyValue: 0,
})

const displayRanks = computed(() => {
  const topThree = [1, 2, 3].map((rankNo) => {
    const item = rankList.value[rankNo - 1]
    return item ? { ...item, key: item.userId } : createPlaceholderRank(rankNo)
  })
  return [topThree[1], topThree[0], topThree[2]]
})

onMounted(async () => {
  initWebSocket()
  getPopularity()
  getIntimacyRank()
})

onBeforeMount(() => {
  websocket && websocket.close()
})

onBeforeUnmount(() => {
  close()
})

watch(data, () => {
  if (!isUserScrolling.value) {
    scrollToBottom()
  }
})

const scrollToBottom = () => {
  nextTick(() => {
    if (scrollContainer.value) {
      scrollContainer.value.scrollTop = scrollContainer.value.scrollHeight
    }
  })
}

const handleScroll = () => { }

/**
 * 发送消息
 */
const handleMessageSend = () => {
  if (messageText.value.trim() === "") {
    return
  }
  let text = messageText.value.trim()
  messageText.value = ""
  ChatApi.sendChatMsg({
    roomId: roomId.value,
    text,
  })
}

const getIntimacyRank = async () => {
  const res = await roomApi.getIntimacyRank({ roomId: roomId.value })
  rankList.value = (res.data || []).map((item, index) => ({
    ...item,
    rankNo: item.rankNo || index + 1,
  }))
}

const formatIntimacy = (value) => {
  const num = Number(value || 0)
  if (Number.isInteger(num)) {
    return `${num}`
  }
  return `${num.toFixed(2)}`
}

/**
 * 获取人气
 */
const getPopularity = () => {
  popularityInterval.value = setInterval(async () => {
    let res = await ChatApi.getPopularity({ roomId: roomId.value })
    popularity.value = res.data
  }, 10000)
}

/**
 * 初始化 WebSocket连接
 */
const initWebSocket = () => {
  let wsUrl = "ws://" + location.hostname + ":10022?token=" + encodeURIComponent(store.user().userToken)
  websocket = new WebSocket(wsUrl)
  websocket.onopen = () => {
    console.log("WebSocket连接成功!")
    connetRoom()
    // 开启跳包
    heartBeat()
  }
  websocket.onclose = (e) => {
    if (e.code === 1000) {
      // 服务端主动断开，不需要重连
      return
    }
    if (e.code === 1005) {
      // 手动关闭，不需要重连
      return
    }
    reconnectWebSocket()
  }
  websocket.onerror = () => {
    // WebSocket连接时发生错误
  }
  websocket.onmessage = (event) => {
    console.log("Message from server:", event.data)
    let message = JSON.parse(event.data)
    if (message.method === "intimacyRank") {
      rankList.value = (message.data || []).map((item, index) => ({
        ...item,
        rankNo: item.rankNo || index + 1,
      }))
      return
    }
    // 处理礼物
    if (message.method === "giftMessage") {
      emits("sendGift", message.data)
      return
    }
    data.value = data.value.concat(message.data)
    // 裁剪消息列表长度
    if (data.value.length > 40) {
      data.value = data.value.slice(-40)
    }
  }
}

/**
 * 连接房间
 */
const connetRoom = () => {
  let reqBody = {
    msgType: 0,
    data: {
      roomId: roomId.value,
    },
  }
  if (websocket) {
    websocket.send(JSON.stringify(reqBody))
  }
}

/**
 * 发送心跳包
 */
const heartBeat = () => {
  heartBeatTimer.value = setInterval(() => {
    if (websocket && websocket.readyState === 1) {
      websocket.send(JSON.stringify({ msgType: 2 }))
    }
  }, 9500)
}

/**
 * 重新连接websocket
 */
const reconnectWebSocket = () => {
  // 关闭心跳定时器
  heartBeatTimer.value && clearInterval(heartBeatTimer.value)
  heartBeatTimer.value = null
  // 关闭原来的重连定时器
  reconnectTimer.value && clearTimeout(reconnectTimer.value)
  reconnectTimer.value = null

  if (lockReconnect.value) {
    return
  }
  lockReconnect.value = true
  // 重连次数上限
  if (maxReconnectCount.value == 0) {
    return
  }
  // 重连
  reconnectTimer.value = setTimeout(() => {
    initWebSocket()
    maxReconnectCount.value--
    lockReconnect.value = false
  }, 5000)
}

const close = () => {
  websocket && websocket.close()
  heartBeatTimer.value && clearTimeout(heartBeatTimer.value)
  popularityInterval.value && clearInterval(popularityInterval.value)
  reconnectTimer.value && clearTimeout(reconnectTimer.value)
}
</script>

<style lang="scss" scoped>
.chat-wrapper {
  height: 100%;
  background-color: #fff;
  display: flex;
  flex-direction: column;
  position: relative;
  flex: 1;
  height: 710px;
  min-height: 0;
  overflow: hidden;

  .chat-top-section {
    position: relative;
    z-index: 2;
    flex: 0 0 auto;
    height: 180px;
  }

  .chat-header {
    height: 158px;
    background-color: #fff;
    padding: 12px 10px 8px;
    box-sizing: border-box;

    .rank-toolbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 8px;
      position: relative;

      .rank-title {
        font-size: 14px;
        font-weight: 600;
      }

      .rank-dropdown-wrapper {
        position: relative;
      }
    }
  }

  .rank-dropdown-list {
    position: absolute;
    top: calc(100% - 4px);
    left: 10px;
    right: 10px;
    max-height: 320px;
    overflow-y: auto;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    z-index: 100;
    opacity: 0;
    visibility: hidden;
    pointer-events: none;
    transform: translateY(-8px);
    transition: opacity 0.2s ease, transform 0.2s ease, visibility 0.2s ease;

    &.is-open {
      opacity: 1;
      visibility: visible;
      pointer-events: auto;
      transform: translateY(0);
    }

    .rank-dropdown-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 8px 12px;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      &:hover {
        background-color: #fafafa;
      }
    }

    .rank-dropdown-left {
      display: flex;
      align-items: center;
      min-width: 0;
      flex: 1;
      overflow: hidden;
    }

    .rank-dropdown-no {
      width: 22px;
      font-size: 12px;
      color: #999;
      text-align: center;

      &.rank-no-1 {
        color: #f3d26b;
        font-weight: 600;
      }

      &.rank-no-2 {
        color: #c8d0da;
        font-weight: 600;
      }

      &.rank-no-3 {
        color: #d8a478;
        font-weight: 600;
      }
    }

    .rank-dropdown-avatar {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      object-fit: cover;
      margin-right: 8px;
      margin-left: 4px;
    }

    .rank-dropdown-name {
      font-size: 13px;
      color: #333;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .rank-dropdown-value {
      font-size: 12px;
      color: #ec8303;
      font-weight: 600;
      margin-left: 8px;
    }

    ::v-deep(.ant-empty) {
      margin: 16px 0;

      .ant-empty-description {
        font-size: 12px;
        color: #999;
      }
    }
  }
}

.rank-board {
  display: flex;
  gap: 8px;
}

.rank-item {
  flex: 1;
  min-width: 0;
  text-align: center;
  padding: 10px 6px 8px;
  border-radius: 4px;
  background: linear-gradient(180deg, #fff8ef 0%, #fff 100%);
  border: 1px solid #f6e3c7;

  .rank-badge {
    font-size: 11px;
    color: #8c6b35;
    margin-bottom: 6px;
  }

  .avatar {
    width: 42px;
    height: 42px;
    border: 2px solid #d7d7d7;
    border-radius: 50%;
    object-fit: cover;
  }

  .name {
    display: block;
    font-size: 12px;
    margin-top: 8px;
    color: #333;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .charm {
    display: block;
    margin-top: 4px;
    color: #ec8303;
    font-size: 12px;
  }
}

.rank-item-1 {
  background: linear-gradient(180deg, #fff4cf 0%, #fff 100%);
  border-color: #f3d26b;

  .avatar {
    border: gold 2px solid;
  }
}

.rank-item-2 .avatar {
  border-color: #c8d0da;
}

.rank-item-3 .avatar {
  border-color: #d8a478;
}

.chat-main {
  flex: 1;
  min-height: 0;
  overflow-y: auto;

  &::-webkit-scrollbar {
    display: none;
  }

  ::v-deep(.ant-list-item) {
    padding: 5px 8px;
    border: none;
  }
}

.chat-footer {
  flex: 0 0 auto;
  padding: 10px;

  .chat-box {
    width: 100%;
  }

  .chat-btn-wrapper {
    margin-top: 10px;
    text-align: right;

    ::v-deep(.ant-btn) {
      width: 75px;
      // color: $font-color-light;
    }

    .popularity {
      float: left;
      color: $font-color-light;
      font-size: 12px;
      margin: 5px 0px 0px 0px;
    }
  }

  ::v-deep(.ant-input-textarea-show-count::after) {
    position: absolute;
    top: 35px;
    right: 6px;
    font-size: 12px;
    color: $font-color-light;
  }
}

.rank-modal {
  .rank-modal-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 0;
    border-bottom: 1px solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }
  }

  .left {
    display: flex;
    align-items: center;
    min-width: 0;
  }

  .rank-no {
    width: 38px;
    font-size: 13px;
    color: #999;
  }

  .avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    object-fit: cover;
    margin-right: 10px;
  }

  .name {
    color: #333;
  }

  .value {
    color: #ec8303;
    font-weight: 600;
  }
}
</style>
