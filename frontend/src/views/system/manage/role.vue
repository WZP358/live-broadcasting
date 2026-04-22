<template>
  <div class="search-wrapper">
    <a-form ref="formRef" class="ant-advanced-search-form" :model="formState" @finish="onFinish">
      <a-row :gutter="24">
        <a-col :span="6">
          <a-form-item name="name" label="角色名称">
            <a-input v-model:value="formState.name" placeholder="请输入角色名称" autocomplete="off"></a-input>
          </a-form-item>
        </a-col>
        <a-col :span="6"></a-col>
        <a-col :span="6"></a-col>
        <a-col :span="6" style="text-align: right">
          <a-button type="primary" html-type="submit">查询</a-button>
          <a-button style="margin: 0 8px" @click="handleReset">重置</a-button>
          <a v-if="showExpand" style="font-size: 12px" @click="expand = !expand">
            <template v-if="expand">
              <UpOutlined />
            </template>
            <template v-else>
              <DownOutlined />
            </template>
            展开
          </a>
        </a-col>
      </a-row>
    </a-form>
  </div>
  <div class="content-wrapper" ref="containerRef">
    <a-table :scroll="{ y: tableScrollY }" :dataSource="dataSource" :columns="columns" :pagination="false" size="small">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'disabled'">
          <a-tag v-if="record.disabled" color="red">禁用</a-tag>
          <a-tag v-else color="green">启用</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <span>
            <a>详情</a>
            <a-divider type="vertical" />
            <a>修改</a>
          </span>
        </template>
      </template>
    </a-table>
    <a-pagination
      :total="total"
      :current="current"
      :page-size="pageSize"
      size="middle"
      :show-size-changer="true"
      :show-quick-jumper="true"
      :page-size-options="['10', '20', '50', '100']"
      @change="handlePageChange"
    />
  </div>
</template>

<script setup>
import { UpOutlined, DownOutlined } from "@ant-design/icons-vue"
import { onMounted, ref, reactive } from "vue"
import systemApi from "@/api/system"
import { useTableScroll } from "@/composables/useTableScroll"
import { useSearchExpand } from "@/composables/useSearchExpand"

const formRef = ref()
const { expand, showExpand } = useSearchExpand(formRef)
const formState = reactive({})
const { containerRef, tableScrollY } = useTableScroll()
const onFinish = (values) => {
  console.log("Received values of form: ", values)
  console.log("formState: ", formState)
  getData()
}

const total = ref(0)
const current = ref(1)
const pageSize = ref(10)

onMounted(() => {
  getData()
})

const getData = () => {
  systemApi
    .page("role", {
      pageNo: current.value,
      pageSize: pageSize.value,
      ...formState,
    })
    .then((res) => {
      total.value = res.data.total
      dataSource.value = res.data.list
    })
}

const handlePageChange = (page, size) => {
  if (size && size !== pageSize.value) {
    pageSize.value = size
  }
  current.value = page
  getData()
}

const handleReset = () => {
  formRef.value.resetFields()
  getData()
}

const dataSource = ref([])
const columns = ref([
  {
    title: "序号",
    dataIndex: "index",
    align: "center",
    customRender: ({ text, record, index, column }) => index + 1,
    width: 80,
    fixed: true,
  },

  {
    title: "名称",
    dataIndex: "name",
    key: "name",
    width: 150,
    fixed: true,
  },
  {
    title: "禁用状态",
    dataIndex: "disabled",
    key: "disabled",
  },
  {
    title: "创建时间",
    dataIndex: "createTime",
    key: "createTime",
  },
  {
    title: "操作",
    key: "action",
    align: "center",
    width: 120,
    fixed: "right",
  },
])
</script>

<style lang="scss" scoped>
.search-wrapper {
  .ant-advanced-search-form {
    .ant-form-item {
      margin-bottom: 0px;
    }

    :deep(.ant-form-item .ant-row) {
      flex-wrap: nowrap;
    }

    :deep(.ant-form-item-label) {
      width: 84px;
      text-align: right;
    }

    :deep(.ant-form-item-control) {
      flex: 1;
      min-width: 0;
    }
  }
}
.content-wrapper {
  margin-top: 20px;
}
</style>
