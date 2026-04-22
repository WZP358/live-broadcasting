<template>
  <div class="search-wrapper">
    <a-form ref="formRef" class="ant-advanced-search-form" :model="formState" @finish="onFinish">
      <a-row :gutter="24">
        <a-col :span="8">
          <a-form-item name="name" label="名称">
            <a-input v-model:value="formState.name" placeholder="请输入名称" autocomplete="off"></a-input>
          </a-form-item>
        </a-col>
        <a-col :span="16" style="text-align: right">
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
    <div class="operation-wrapper">
      <a-tooltip title="search">
        <a-button type="primary" @click="handleAdd" :icon="h(PlusOutlined)">新增</a-button>
      </a-tooltip>
    </div>
    <a-table :scroll="{ y: tableScrollY }" :dataSource="dataSource" :columns="columns" :pagination="false" size="small">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'icon'">
          <a-flex align="center">
            <SvgIcon :="{ icon: record.icon, size: '15px' }" />
            <section style="width: 10px"></section>
            <span>{{ record.icon }}</span>
          </a-flex>
        </template>
        <template v-if="column.key === 'status'">
          <CellStatus :val="record.status" />
        </template>
        <template v-else-if="column.key === 'action'">
          <span>
            <!-- <a @click="handleView(record)">详情</a> -->
            <a @click="handleEdit(record)">修改</a>
            <a-divider type="vertical" />
            <a style="color: red;" @click="handleDelte(record)">删除</a>
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

  <!-- 新增/编辑弹窗 -->
  <a-modal v-model:open="modalVisible" :title="modalTitle" @ok="handleModalOk" @cancel="handleModalCancel"
    :confirmLoading="confirmLoading" width="600px">
    <a-form ref="modalFormRef" :model="modalFormState" :rules="modalFormRules" :label-col="{ span: 4 }"
      :wrapper-col="{ span: 18 }">
      <a-form-item label="名称" name="title">
        <a-input v-model:value="modalFormState.title" placeholder="请输入菜单名称" />
      </a-form-item>
      <a-form-item label="父级标识" name="pid">
        <a-input-number v-model:value="modalFormState.pid" placeholder="请输入父级ID" :min="0" style="width: 100%" />
      </a-form-item>
      <a-form-item label="图标" name="icon">
        <a-input v-model:value="modalFormState.icon" placeholder="请输入图标名称" />
      </a-form-item>
      <a-form-item label="路径" name="path">
        <a-input v-model:value="modalFormState.path" placeholder="请输入路径" />
      </a-form-item>
      <a-form-item label="排序" name="sort">
        <a-input-number v-model:value="modalFormState.sort" placeholder="请输入排序" :min="0" style="width: 100%" />
      </a-form-item>
      <a-form-item label="状态" name="status">
        <a-select v-model:value="modalFormState.status" placeholder="请选择状态">
          <a-select-option :value="0">启用</a-select-option>
          <a-select-option :value="-1">禁用</a-select-option>
        </a-select>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { UpOutlined, DownOutlined, PlusOutlined, ExclamationCircleOutlined } from "@ant-design/icons-vue"
import { onMounted, ref, reactive, h, createVNode } from "vue"
import { message, Modal } from 'ant-design-vue'
import systemMenuApi from "@/api/systemMenu"
import systemApi from "@/api/system"
import SvgIcon from "@/components/SvgIcon/index.vue"
import CellStatus from "@/components/Common/CellStatus.vue"
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
  systemMenuApi
    .getPage({
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

// 弹窗相关
const modalVisible = ref(false)
const modalTitle = ref('')
const confirmLoading = ref(false)
const modalFormRef = ref()
const isEdit = ref(false)
const editId = ref(null)

const modalFormState = reactive({
  title: '',
  pid: 0,
  icon: '',
  path: '',
  sort: 0,
  status: 0
})

const modalFormRules = {
  title: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  pid: [{ required: true, message: '请输入父级标识', trigger: 'blur' }],
  icon: [{ required: true, message: '请输入图标名称', trigger: 'blur' }],
  path: [{ required: true, message: '请输入路径', trigger: 'blur' }],
  sort: [{ required: true, message: '请输入排序', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

// 新增
const handleAdd = () => {
  modalTitle.value = '新增菜单'
  isEdit.value = false
  editId.value = null
  resetModalForm()
  modalVisible.value = true
}

// 编辑
const handleEdit = (record) => {
  modalTitle.value = '编辑菜单'
  isEdit.value = true
  editId.value = record.id

  // 填充表单数据
  Object.assign(modalFormState, {
    id: record.id,
    title: record.title,
    pid: record.pid,
    icon: record.icon,
    path: record.path,
    sort: record.sort,
    status: record.status
  })

  modalVisible.value = true
}

// 查看详情
const handleView = (record) => {
  modalTitle.value = '查看菜单详情'
  isEdit.value = false
  editId.value = null

  // 填充表单数据
  Object.assign(modalFormState, {
    id: record.id,
    title: record.title,
    pid: record.pid,
    icon: record.icon,
    path: record.path,
    sort: record.sort,
    status: record.status
  })

  modalVisible.value = true
}

// 重置弹窗表单
const resetModalForm = () => {
  Object.assign(modalFormState, {
    title: '',
    pid: 0,
    icon: '',
    path: '',
    sort: 0,
    status: 0
  })
  if (modalFormRef.value) {
    modalFormRef.value.resetFields()
  }
}

// 弹窗确定
const handleModalOk = () => {
  modalFormRef.value.validate().then(() => {
    confirmLoading.value = true

    // const apiCall = isEdit.value
    //   ? systemMenuApi.update(editId.value, modalFormState)
    //   : systemMenuApi.save('menu', modalFormState)
    const apiCall = systemApi.save('menu', modalFormState)

    apiCall.then(() => {
      message.success(isEdit.value ? '编辑成功' : '新增成功')
      modalVisible.value = false
      getData()
    }).catch((error) => {
      message.error(isEdit.value ? '编辑失败' : '新增失败')
      console.error(error)
    }).finally(() => {
      confirmLoading.value = false
    })
  }).catch(() => {
    message.error('请检查表单输入')
  })
}

// 弹窗取消
const handleModalCancel = () => {
  modalVisible.value = false
  resetModalForm()
}

// 删除
const handleDelte = (record) => {
  Modal.confirm({
    title: '确认删除吗？',
    icon: createVNode(ExclamationCircleOutlined),
    content: `删除后将无法恢复，确定删除 ${record.title} 吗？`,
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    onOk() {
      systemApi.delete('menu', [record.id]).then(() => {
        message.success('删除成功')
        getData()
      }).catch((error) => {
        message.error('删除失败')
        console.error(error)
      })
    },
    onCancel() {
      console.log('Cancel');
    }
  })
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
    dataIndex: "title",
    key: "title",
    width: 150,
    fixed: true,
  },

  {
    title: "图标",
    dataIndex: "icon",
    key: "icon",
  },
  {
    title: "路径",
    dataIndex: "path",
    key: "path",
  },
  {
    title: "父级标识",
    dataIndex: "pid",
    key: "pid",
    width: 100,
  },
  {
    title: "排序",
    dataIndex: "sort",
    key: "sort",
    sorter: (a, b) => a.sort - b.sort,
  },
  {
    title: "状态",
    dataIndex: "status",
    key: "status",
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

  .operation-wrapper {
    margin-bottom: 20px;
    display: flex;
  }
}
</style>
