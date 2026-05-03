<template>
  <crud-page module-name="分类" base-url="/api/v1/system/category" :columns="columns" :search-fields="searchFields" :form-fields="formFields" />
</template>

<script>
import CrudPage from '../common/CrudPage'

export default {
  name: 'LiveCategory',
  components: { CrudPage },
  data() {
    return {
      searchFields: [{ prop: 'name', label: '分类名称' }],
      columns: [
        { prop: 'id', label: '分类ID', width: 90 },
        { prop: 'name', label: '分类名称' },
        { prop: 'icon', label: '图标' },
        { prop: 'sort', label: '排序', width: 80 },
        { prop: 'status', label: '状态', type: 'tag', width: 90, options: [{ label: '正常', value: 0 }, { label: '停用', value: -1 }] },
        { prop: 'createTime', label: '创建时间', width: 160 }
      ],
      formFields: [
        { prop: 'id', label: '分类ID', required: false },
        { prop: 'parentId', label: '父级ID', type: 'number', required: false },
        { prop: 'name', label: '分类名称' },
        { prop: 'icon', label: '图标', required: false },
        { prop: 'sort', label: '排序', type: 'number', required: false },
        { prop: 'status', label: '状态', type: 'select', defaultValue: 0, options: [{ label: '正常', value: 0 }, { label: '停用', value: -1 }] }
      ]
    }
  }
}
</script>
