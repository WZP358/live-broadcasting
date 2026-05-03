<template>
  <crud-page module-name="角色" base-url="/api/v1/system/role" :columns="columns" :search-fields="searchFields" :form-fields="formFields" />
</template>

<script>
import CrudPage from '../common/CrudPage'

export default {
  name: 'SystemRole',
  components: { CrudPage },
  data() {
    return {
      searchFields: [{ prop: 'name', label: '角色名称' }],
      columns: [
        { prop: 'id', label: '角色ID', width: 90 },
        { prop: 'name', label: '角色名称' },
        { prop: 'permission', label: '权限字符' },
        { prop: 'level', label: '角色等级', width: 90 },
        { prop: 'createTime', label: '创建时间', width: 160 }
      ],
      formFields: [
        { prop: 'id', label: '角色ID', required: false },
        { prop: 'name', label: '角色名称' },
        { prop: 'permission', label: '权限字符', required: false },
        { prop: 'level', label: '角色等级', type: 'number', required: false }
      ]
    }
  }
}
</script>
