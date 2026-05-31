import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { useStore } from "@/stores";
import $modal from "@/utils/message";
import roomApi from "@/api/room";
import watchApi from "@/api/watch";
import liveApi from "@/api/live";
import { buildRelatedRooms, getAnchorName } from "@/utils/liveRoomPresenter";

export function useRoomPage() {
  const router = useRouter();
  const store = useStore();
  const roomId = computed(() => Number(router.currentRoute.value.params.id));
  const isLogin = computed(() => store.user().isLogin);
  const roomInfo = ref({});
  const roomExtraInfo = ref({});
  const recommendRooms = ref([]);
  const anchorName = computed(() => getAnchorName(roomInfo.value));

  const loadRoomInfo = async () => {
    try {
      const res = await roomApi.getRoomInfo({ roomId: roomId.value });
      roomInfo.value = res.data || {};
    } catch (error) {
      roomInfo.value = {};
    }
  };

  const loadRoomExtraInfo = async () => {
    try {
      const res = await roomApi.getRoomExtraInfo({ roomId: roomId.value });
      roomExtraInfo.value = res.data || {};
    } catch (error) {
      roomExtraInfo.value = {};
    }
  };

  const saveHistory = () => watchApi.saveHistory({ roomId: roomId.value });

  const loadRecommendRooms = async () => {
    try {
      const res = await liveApi.listLivingRooms({});
      recommendRooms.value = buildRelatedRooms(res?.data?.list || [], roomInfo.value, 4);
    } catch (error) {
      recommendRooms.value = [];
    }
  };

  const refreshRoom = async () => {
    await loadRoomInfo();
    await loadRecommendRooms();
    if (isLogin.value) {
      await Promise.all([loadRoomExtraInfo(), saveHistory()]);
    } else {
      roomExtraInfo.value = {};
    }
  };

  const toggleFollow = async () => {
    if (!isLogin.value) {
      router.push("/login");
      return;
    }

    try {
      if (roomExtraInfo.value.follow) {
        await watchApi.unFollow({ roomId: roomId.value });
        $modal.msgSuccess("已取消关注");
      } else {
        await watchApi.follow({ roomId: roomId.value });
        $modal.msgSuccess("关注成功");
      }
      await loadRoomExtraInfo();
    } catch (error) {
      $modal.msgError("操作失败，请稍后重试");
    }
  };

  const copyRoomLink = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href);
      $modal.msgSuccess("直播间链接已复制");
    } catch (error) {
      $modal.msgWarning("复制失败，请手动复制浏览器地址");
    }
  };

  const goLogin = () => {
    router.push("/login");
  };

  onMounted(refreshRoom);
  watch(roomId, refreshRoom);

  return {
    anchorName,
    copyRoomLink,
    goLogin,
    isLogin,
    recommendRooms,
    refreshRoom,
    roomExtraInfo,
    roomId,
    roomInfo,
    toggleFollow,
  };
}
