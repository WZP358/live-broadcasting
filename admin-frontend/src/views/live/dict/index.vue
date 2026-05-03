<template>
  <crud-page module-name="字典" base-url="/api/v1/system/dict" :columns="columns" :search-fields="searchFields" :form-fields="formFields" />
</template>

<script>
import CrudPage from '../common/CrudPage'

export default {
  name: 'SystemDict',
  components: { CrudPage },
  data() {
    return {
      searchFields: [
        { prop: 'type', label: '字典类型' },
        { prop: 'label', label: '字典标签' }
      ],
      columns: [
        { prop: 'id', label: '字典ID', width: 90 },
        { prop: 'type', label: '字典类型' },
        { prop: 'typeName', label: '类型名称' },
        { prop: 'label', label: '字典标签' },
        { prop: 'value', label: '字典键值' },
        { prop: 'sort', label: '排序', width: 80 },
        { prop: 'status', label: '状态', type: 'tag', width: 90, options: [{ label: '正常', value: 0 }, { label: '停用', value: -1 }] }
      ],
      formFields: [
        { prop: 'id', label: '字典ID', required: false },
        { prop: 'type', label: '字典类型' },
        { prop: 'typeName', label: '类型名称', required: false },
        { prop: 'label', label: '字典标签' },
        { prop: 'value', label: '字典键值' },
        { prop: 'sort', label: '排序', type: 'number', required: false },
        { prop: 'status', label: '状态', type: 'select', defaultValue: 0, options: [{ label: '正常', value: 0 }, { label: '停用', value: -1 }] }
      ]
    }
  }
}
</script>
