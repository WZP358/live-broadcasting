<template>
  <crud-page module-name="礼物" base-url="/api/v1/system/gift" :columns="columns" :search-fields="searchFields" :form-fields="formFields" />
</template>

<script>
import CrudPage from '../common/CrudPage'

export default {
  name: 'LiveGift',
  components: { CrudPage },
  data() {
    return {
      searchFields: [{ prop: 'name', label: '礼物名称' }],
      columns: [
        { prop: 'id', label: '礼物ID', width: 90 },
        { prop: 'icon', label: '图标', type: 'image', width: 80 },
        { prop: 'name', label: '礼物名称' },
        { prop: 'price', label: '价格' },
        { prop: 'sort', label: '排序', width: 80 },
        { prop: 'disabled', label: '状态', type: 'tag', width: 90, options: [{ label: '正常', value: 0 }, { label: '停用', value: -1 }] }
      ],
      formFields: [
        { prop: 'id', label: '礼物ID', required: false },
        { prop: 'name', label: '礼物名称' },
        { prop: 'icon', label: '图标地址', required: false },
        { prop: 'price', label: '价格', type: 'number' },
        { prop: 'sort', label: '排序', type: 'number', required: false },
        { prop: 'description', label: '描述', type: 'textarea', required: false },
        { prop: 'disabled', label: '状态', type: 'select', defaultValue: 0, options: [{ label: '正常', value: 0 }, { label: '停用', value: -1 }] }
      ]
    }
  }
}
</script>
