<template>
  <crud-page module-name="账单" base-url="/api/v1/system/bill" :columns="columns" :search-fields="searchFields" :form-fields="formFields" />
</template>

<script>
import CrudPage from '../common/CrudPage'

export default {
  name: 'LiveBill',
  components: { CrudPage },
  data() {
    return {
      searchFields: [
        { prop: 'userId', label: '用户ID' },
        { prop: 'type', label: '流水类型' }
      ],
      columns: [
        { prop: 'id', label: '账单ID', width: 90 },
        { prop: 'userId', label: '用户ID', width: 90 },
        { prop: 'orderNo', label: '订单号' },
        { prop: 'type', label: '类型' },
        { prop: 'billChange', label: '变动金额' },
        { prop: 'balance', label: '余额' },
        { prop: 'mark', label: '标记' },
        { prop: 'createTime', label: '创建时间', width: 160 }
      ],
      formFields: [
        { prop: 'id', label: '账单ID', required: false },
        { prop: 'userId', label: '用户ID', type: 'number' },
        { prop: 'orderNo', label: '订单号', required: false },
        { prop: 'type', label: '流水类型', type: 'number' },
        { prop: 'billChange', label: '变动金额', type: 'number' },
        { prop: 'balance', label: '余额', type: 'number', required: false },
        { prop: 'mark', label: '标记', required: false }
      ]
    }
  }
}
</script>
