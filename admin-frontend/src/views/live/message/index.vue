<template>
  <crud-page module-name="消息" base-url="/api/v1/system/message" :columns="columns" :search-fields="searchFields" :form-fields="formFields" />
</template>

<script>
import CrudPage from '../common/CrudPage'

export default {
  name: 'LiveMessage',
  components: { CrudPage },
  data() {
    return {
      searchFields: [
        { prop: 'roomId', label: '房间ID' },
        { prop: 'userId', label: '用户ID' }
      ],
      columns: [
        { prop: 'id', label: '消息ID', width: 90 },
        { prop: 'roomId', label: '房间ID', width: 90 },
        { prop: 'userId', label: '用户ID', width: 90 },
        { prop: 'content', label: '消息内容' },
        { prop: 'createTime', label: '发送时间', width: 160 }
      ],
      formFields: [
        { prop: 'id', label: '消息ID', required: false },
        { prop: 'roomId', label: '房间ID', type: 'number' },
        { prop: 'userId', label: '用户ID', type: 'number' },
        { prop: 'content', label: '消息内容', type: 'textarea' }
      ]
    }
  }
}
</script>
