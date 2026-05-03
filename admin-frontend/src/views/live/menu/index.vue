<template>
  <crud-page module-name="菜单" base-url="/api/v1/system/menu" :columns="columns" :search-fields="searchFields" :form-fields="formFields" />
</template>

<script>
import CrudPage from '../common/CrudPage'

export default {
  name: 'SystemMenu',
  components: { CrudPage },
  data() {
    return {
      searchFields: [{ prop: 'title', label: '菜单名称' }],
      columns: [
        { prop: 'id', label: '菜单ID', width: 90 },
        { prop: 'title', label: '菜单名称' },
        { prop: 'path', label: '路由地址' },
        { prop: 'icon', label: '图标' },
        { prop: 'sort', label: '排序', width: 80 },
        { prop: 'status', label: '状态', type: 'tag', width: 90, options: [{ label: '正常', value: 0 }, { label: '停用', value: -1 }] }
      ],
      formFields: [
        { prop: 'id', label: '菜单ID', required: false },
        { prop: 'pid', label: '父菜单ID', type: 'number', required: false },
        { prop: 'title', label: '菜单名称' },
        { prop: 'icon', label: '菜单图标', required: false },
        { prop: 'path', label: '路由地址', required: false },
        { prop: 'menuIndex', label: '菜单索引', type: 'number', required: false },
        { prop: 'sort', label: '显示排序', type: 'number', required: false },
        { prop: 'status', label: '状态', type: 'select', defaultValue: 0, options: [{ label: '正常', value: 0 }, { label: '停用', value: -1 }] }
      ]
    }
  }
}
</script>
