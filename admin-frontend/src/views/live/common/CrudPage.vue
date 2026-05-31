<template>
  <div class="app-container pulse-crud-page">
    <el-form
      v-show="showSearch"
      ref="queryForm"
      :model="queryParams"
      size="small"
      :inline="true"
      label-width="88px"
    >
      <el-form-item v-for="field in searchFields" :key="field.prop" :label="field.label" :prop="field.prop">
        <el-select
          v-if="field.type === 'select'"
          v-model="queryParams[field.prop]"
          clearable
          filterable
          :placeholder="'请选择' + field.label"
        >
          <el-option v-for="option in field.options" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <el-input
          v-else
          v-model="queryParams[field.prop]"
          clearable
          :placeholder="'请输入' + field.label"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="rows" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column
        v-for="column in columns"
        :key="column.prop"
        :label="column.label"
        :prop="column.prop"
        :width="column.width"
        :show-overflow-tooltip="column.type !== 'image'"
        align="center"
      >
        <template slot-scope="scope">
          <dict-tag
            v-if="column.type === 'tag' || column.type === 'dict'"
            :options="toDictOptions(column.options)"
            :value="scope.row[column.prop]"
          />
          <image-preview
            v-else-if="column.type === 'image' && scope.row[column.prop]"
            :src="scope.row[column.prop]"
            :width="52"
            :height="52"
          />
          <span v-else>{{ formatColumn(column, scope.row[column.prop]) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNo"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="title" :visible.sync="open" width="640px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item v-for="field in formFields" :key="field.prop" :label="field.label" :prop="field.prop">
          <el-select
            v-if="field.type === 'select'"
            v-model="form[field.prop]"
            clearable
            filterable
            :placeholder="'请选择' + field.label"
          >
            <el-option v-for="option in field.options" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
          <el-input-number v-else-if="field.type === 'number'" v-model="form[field.prop]" controls-position="right" :min="0" />
          <image-upload
            v-else-if="field.type === 'imageUpload'"
            v-model="form[field.prop]"
            :limit="field.limit || 1"
            :file-size="field.fileSize || 5"
          />
          <el-input
            v-else-if="field.type === 'textarea'"
            v-model="form[field.prop]"
            type="textarea"
            :rows="3"
            :placeholder="'请输入' + field.label"
          />
          <el-input v-else v-model="form[field.prop]" :placeholder="'请输入' + field.label" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { pageResource, saveResource, delResource } from '@/api/pulselive'

export default {
  name: 'PulseLiveCrudPage',
  props: {
    moduleName: { type: String, required: true },
    baseUrl: { type: String, required: true },
    columns: { type: Array, required: true },
    searchFields: { type: Array, default: () => [] },
    formFields: { type: Array, required: true }
  },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      rows: [],
      title: '',
      open: false,
      queryParams: {
        pageNo: 1,
        pageSize: 10
      },
      form: {},
      rules: {}
    }
  },
  created() {
    this.searchFields.forEach(field => {
      this.$set(this.queryParams, field.prop, undefined)
    })
    this.formFields.forEach(field => {
      if (field.required !== false) {
        this.$set(this.rules, field.prop, [{ required: true, message: `${field.label}不能为空`, trigger: 'blur' }])
      }
    })
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      pageResource(this.baseUrl, this.queryParams).then(response => {
        const data = response.data || {}
        this.rows = data.list || []
        this.total = Number(data.total || 0)
      }).finally(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNo = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    reset() {
      this.form = {}
      this.formFields.forEach(field => {
        this.$set(this.form, field.prop, field.defaultValue === undefined ? undefined : field.defaultValue)
      })
      this.resetForm('form')
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = `新增${this.moduleName}`
    },
    handleUpdate(row) {
      this.reset()
      const record = row.id ? row : this.rows.find(item => item.id === this.ids[0])
      this.form = { ...record }
      this.open = true
      this.title = `修改${this.moduleName}`
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        saveResource(this.baseUrl, this.form).then(() => {
          this.$modal.msgSuccess('保存成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const ids = row.id ? [row.id] : this.ids
      this.$modal.confirm(`是否确认删除${this.moduleName}编号为 "${ids.join(',')}" 的数据项？`).then(() => {
        return delResource(this.baseUrl, ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleExport() {
      this.$modal.msgWarning('当前模块尚未接入导出接口')
    },
    cancel() {
      this.open = false
      this.reset()
    },
    formatColumn(column, value) {
      if (value === undefined || value === null || value === '') return '-'
      if (!column.options) return value
      const option = column.options.find(item => item.value === value)
      return option ? option.label : value
    },
    toDictOptions(options = []) {
      return options.map(item => ({
        label: item.label,
        value: item.value,
        raw: {
          listClass: item.listClass || this.resolveListClass(item.value),
          cssClass: item.cssClass || ''
        }
      }))
    },
    resolveListClass(value) {
      if (value === 0 || value === '0') return 'success'
      if (value === 1 || value === '1') return 'primary'
      if (value === -1 || value === '-1') return 'danger'
      return 'default'
    }
  }
}
</script>

<style scoped>
.pulse-crud-page ::v-deep .el-dialog__body {
  padding-bottom: 8px;
}
</style>
