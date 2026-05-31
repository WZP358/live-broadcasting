<template>
  <crud-page module-name="直播间" base-url="/api/v1/system/room" :columns="columns" :search-fields="searchFields" :form-fields="formFields" />
</template>

<script>
import CrudPage from '../common/CrudPage'

export default {
  name: 'LiveRoom',
  components: { CrudPage },
  data() {
    return {
      searchFields: [
        { prop: 'id', label: '房间ID' },
        { prop: 'title', label: '直播标题' },
        { prop: 'status', label: '直播状态', type: 'select', options: [{ label: '未开播', value: 0 }, { label: '直播中', value: 1 }, { label: '已停播', value: -1 }] }
      ],
      columns: [
        { prop: 'id', label: '房间ID', width: 90 },
        { prop: 'cover', label: '封面', type: 'image', width: 80 },
        { prop: 'title', label: '直播标题' },
        { prop: 'userNickname', label: '主播' },
        { prop: 'categoryName', label: '分类' },
        { prop: 'status', label: '状态', type: 'dict', width: 90, options: [{ label: '未开播', value: 0, listClass: 'info' }, { label: '直播中', value: 1, listClass: 'success' }, { label: '已停播', value: -1, listClass: 'danger' }] },
        { prop: 'createTime', label: '创建时间', width: 160 }
      ],
      formFields: [
        { prop: 'id', label: '房间ID', required: false },
        { prop: 'title', label: '直播标题' },
        { prop: 'userId', label: '主播ID', type: 'number' },
        { prop: 'categoryId', label: '分类ID', type: 'number', required: false },
        { prop: 'cover', label: '直播封面', type: 'imageUpload', required: false },
        { prop: 'notice', label: '公告', type: 'textarea', required: false },
        { prop: 'disabled', label: '禁用状态', type: 'select', defaultValue: 0, options: [{ label: '正常', value: 0 }, { label: '禁用', value: -1 }] }
      ]
    }
  }
}
</script>
