<template>
  <crud-page module-name="认证审核" base-url="/api/v1/system/auth" :columns="columns" :search-fields="searchFields" :form-fields="formFields" />
</template>

<script>
import CrudPage from '../common/CrudPage'

export default {
  name: 'LiveAuth',
  components: { CrudPage },
  data() {
    return {
      searchFields: [
        { prop: 'userId', label: '用户ID' },
        { prop: 'status', label: '审核状态', type: 'select', options: [{ label: '待审核', value: 0 }, { label: '已通过', value: 1 }, { label: '已拒绝', value: -1 }] }
      ],
      columns: [
        { prop: 'id', label: '认证ID', width: 90 },
        { prop: 'userId', label: '用户ID', width: 90 },
        { prop: 'realName', label: '真实姓名' },
        { prop: 'cardNo', label: '证件号' },
        { prop: 'status', label: '状态', type: 'tag', width: 90, options: [{ label: '待审核', value: 0 }, { label: '已通过', value: 1 }, { label: '已拒绝', value: -1 }] },
        { prop: 'createTime', label: '提交时间', width: 160 }
      ],
      formFields: [
        { prop: 'id', label: '认证ID', required: false },
        { prop: 'userId', label: '用户ID', type: 'number' },
        { prop: 'realName', label: '真实姓名' },
        { prop: 'cardNo', label: '证件号' },
        { prop: 'status', label: '审核状态', type: 'select', defaultValue: 0, options: [{ label: '待审核', value: 0 }, { label: '已通过', value: 1 }, { label: '已拒绝', value: -1 }] }
      ]
    }
  }
}
</script>
