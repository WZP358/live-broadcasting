<template>
  <crud-page module-name="用户" base-url="/api/v1/system/user" :columns="columns" :search-fields="searchFields" :form-fields="formFields" />
</template>

<script>
import CrudPage from '../common/CrudPage'

export default {
  name: 'LiveUser',
  components: { CrudPage },
  data() {
    return {
      searchFields: [
        { prop: 'id', label: '用户ID' },
        { prop: 'username', label: '账号' },
        { prop: 'nickname', label: '昵称' }
      ],
      columns: [
        { prop: 'id', label: '用户ID', width: 90 },
        { prop: 'username', label: '账号' },
        { prop: 'nickname', label: '昵称' },
        { prop: 'mobile', label: '手机号' },
        { prop: 'email', label: '邮箱' },
        { prop: 'disabled', label: '状态', type: 'tag', width: 90, options: [{ label: '正常', value: 0 }, { label: '停用', value: -1 }] },
        { prop: 'createTime', label: '创建时间', width: 160 }
      ],
      formFields: [
        { prop: 'id', label: '用户ID', required: false },
        { prop: 'username', label: '账号' },
        { prop: 'nickname', label: '昵称' },
        { prop: 'mobile', label: '手机号', required: false },
        { prop: 'email', label: '邮箱', required: false },
        { prop: 'disabled', label: '状态', type: 'select', defaultValue: 0, options: [{ label: '正常', value: 0 }, { label: '停用', value: -1 }] }
      ]
    }
  }
}
</script>
