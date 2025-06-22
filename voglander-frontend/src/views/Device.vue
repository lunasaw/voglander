<template>
  <div class="device-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>设备管理</span>
          <div class="header-buttons">
            <el-button type="info" @click="handleRefreshCount">
              <el-icon><Refresh /></el-icon>
              刷新统计
            </el-button>
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              添加设备
            </el-button>
            <el-button type="success" @click="handleBatchAdd">
              <el-icon><Plus /></el-icon>
              批量添加
            </el-button>
          </div>
        </div>
      </template>

      <!-- 统计信息 -->
      <el-row :gutter="16" class="stats-row">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-item">
              <div class="stat-value">{{ deviceStats.total }}</div>
              <div class="stat-label">设备总数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-item">
              <div class="stat-value">{{ deviceStats.online }}</div>
              <div class="stat-label">在线设备</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-item">
              <div class="stat-value">{{ deviceStats.offline }}</div>
              <div class="stat-label">离线设备</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-item">
              <div class="stat-value">{{ deviceStats.error }}</div>
              <div class="stat-label">异常设备</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 搜索区域 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="设备名称">
          <el-input
            v-model="searchForm.name"
            placeholder="请输入设备名称"
            clearable
          />
        </el-form-item>
        <el-form-item label="设备类型">
          <el-select v-model="searchForm.type" placeholder="请选择类型" clearable>
            <el-option label="摄像头" value="camera" />
            <el-option label="NVR" value="nvr" />
            <el-option label="DVR" value="dvr" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="在线" value="online" />
            <el-option label="离线" value="offline" />
            <el-option label="异常" value="error" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 批量操作区域 -->
      <div class="batch-actions" v-if="selectedDevices.length > 0">
        <el-button type="warning" @click="handleBatchUpdate">
          <el-icon><Edit /></el-icon>
          批量更新 ({{ selectedDevices.length }})
        </el-button>
        <el-button type="danger" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>
          批量删除 ({{ selectedDevices.length }})
        </el-button>
        <el-button @click="handleClearSelection">
          清空选择
        </el-button>
      </div>

      <!-- 设备表格 -->
      <el-table
        :data="deviceList"
        style="width: 100%"
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="deviceId" label="设备ID" width="200" show-overflow-tooltip />
        <el-table-column prop="name" label="设备名称" width="150" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.name || '未命名' }}
          </template>
        </el-table-column>
        <el-table-column prop="type" label="设备类型" width="120">
          <template #default="scope">
            {{ getTypeLabel(scope.row.type) }}
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP地址" width="130" />
        <el-table-column prop="port" label="端口" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag
              :type="scope.row.status === 'online' ? 'success' :
                     scope.row.status === 'offline' ? 'danger' : 'warning'"
            >
              {{ scope.row.status === 'online' ? '在线' :
                 scope.row.status === 'offline' ? '离线' : '异常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="registerTime" label="注册时间" width="180">
          <template #default="scope">
            {{ scope.row.registerTime ? new Date(scope.row.registerTime).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="keepaliveTime" label="心跳时间" width="180">
          <template #default="scope">
            {{ scope.row.keepaliveTime ? new Date(scope.row.keepaliveTime).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="handleEdit(scope.row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button
              size="small"
              type="danger"
              @click="handleDelete(scope.row)"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        :current-page="pagination.currentPage"
        :page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        style="margin-top: 20px; text-align: right;"
      />
    </el-card>

    <!-- 设备详情/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
    >
      <el-form
        :model="deviceForm"
        :rules="deviceRules"
        ref="deviceFormRef"
        label-width="100px"
      >
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="deviceForm.name" />
        </el-form-item>
        <el-form-item label="设备类型" prop="type">
          <el-select v-model="deviceForm.type" placeholder="请选择设备类型">
            <el-option label="摄像头" value="camera" />
            <el-option label="NVR" value="nvr" />
            <el-option label="DVR" value="dvr" />
          </el-select>
        </el-form-item>
        <el-form-item label="IP地址" prop="ip">
          <el-input v-model="deviceForm.ip" />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="deviceForm.port" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="deviceForm.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="deviceForm.password" type="password" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量添加对话框 -->
    <el-dialog
      v-model="batchDialogVisible"
      title="批量添加设备"
      width="800px"
    >
      <el-form :model="batchForm" ref="batchFormRef">
        <el-form-item label="设备列表">
          <el-table :data="batchForm.devices" style="width: 100%">
            <el-table-column label="设备名称">
              <template #default="scope">
                <el-input v-model="scope.row.name" placeholder="请输入设备名称" />
              </template>
            </el-table-column>
            <el-table-column label="设备类型">
              <template #default="scope">
                <el-select v-model="scope.row.type" placeholder="请选择">
                  <el-option label="摄像头" value="camera" />
                  <el-option label="NVR" value="nvr" />
                  <el-option label="DVR" value="dvr" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="IP地址">
              <template #default="scope">
                <el-input v-model="scope.row.ip" placeholder="请输入IP地址" />
              </template>
            </el-table-column>
            <el-table-column label="端口">
              <template #default="scope">
                <el-input-number v-model="scope.row.port" :min="1" :max="65535" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="scope">
                <el-button size="small" type="danger" @click="removeBatchDevice(scope.$index)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
        <el-form-item>
          <el-button @click="addBatchDevice">添加设备</el-button>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBatchSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Edit, Delete } from '@element-plus/icons-vue'
import { deviceApi } from '../api'

export default {
  name: 'DevicePage',
  components: {
    Plus,
    Search,
    Refresh,
    Edit,
    Delete
  },
  setup() {
    // 搜索表单
    const searchForm = reactive({
      name: '',
      type: '',
      status: ''
    })

    // 设备列表
    const deviceList = ref([])
    const loading = ref(false)
    const selectedDevices = ref([])

    // 统计信息
    const deviceStats = reactive({
      total: 0,
      online: 0,
      offline: 0,
      error: 0
    })

    // 分页
    const pagination = reactive({
      currentPage: 1,
      pageSize: 10,
      total: 0
    })

    // 对话框
    const dialogVisible = ref(false)
    const dialogTitle = ref('')
    const deviceFormRef = ref()

    // 批量对话框
    const batchDialogVisible = ref(false)
    const batchFormRef = ref()
    const batchForm = reactive({
      devices: []
    })

    // 设备表单
    const deviceForm = reactive({
      id: null,
      name: '',
      type: '',
      ip: '',
      port: 8000,
      username: '',
      password: ''
    })

    // 表单验证规则
    const deviceRules = {
      name: [
        { required: true, message: '请输入设备名称', trigger: 'blur' }
      ],
      type: [
        { required: true, message: '请选择设备类型', trigger: 'change' }
      ],
      ip: [
        { required: true, message: '请输入IP地址', trigger: 'blur' },
        { pattern: /^(\d{1,3}\.){3}\d{1,3}$/, message: 'IP地址格式不正确', trigger: 'blur' }
      ],
      port: [
        { required: true, message: '请输入端口号', trigger: 'blur' }
      ]
    }

    // 加载设备列表
    const loadDeviceList = async () => {
      loading.value = true
      try {
        const searchCondition = {}
        if (searchForm.name) searchCondition.name = searchForm.name
        if (searchForm.type) searchCondition.type = convertTypeToInteger(searchForm.type)
        if (searchForm.status) searchCondition.status = convertStatusToInteger(searchForm.status)

        const response = await deviceApi.getPageListByEntity(
          pagination.currentPage,
          pagination.pageSize,
          searchCondition
        )

        if (response.code === 0) {
          // 转换后端Integer类型为前端字符串类型
          const devices = (response.data.records || []).map(device => ({
            ...device,
            type: convertIntegerToType(device.type),
            // 处理设备状态：1-在线，2-离线，其他-异常
            status: device.status === 1 ? 'online' : device.status === 2 ? 'offline' : 'error'
          }))
          deviceList.value = devices
          // 使用后端返回的分页信息
          pagination.total = response.data.total || 0
          pagination.currentPage = response.data.current || 1
          pagination.pageSize = response.data.size || 10
        } else {
          ElMessage.error(response.msg || '加载设备列表失败')
        }
      } catch (error) {
        console.error('加载设备列表失败：', error)
        ElMessage.error('加载设备列表失败')
      } finally {
        loading.value = false
      }
    }

    // 加载统计信息
    const loadDeviceStats = async () => {
      try {
        // 总数
        const totalResponse = await deviceApi.getCount()
        if (totalResponse.code === 0) {
          deviceStats.total = totalResponse.data
        }

        // 在线数量 (后端状态：1-在线)
        const onlineResponse = await deviceApi.getCountByEntity({ status: 1 })
        if (onlineResponse.code === 0) {
          deviceStats.online = onlineResponse.data
        }

        // 离线数量 (后端状态：2-离线)
        const offlineResponse = await deviceApi.getCountByEntity({ status: 2 })
        if (offlineResponse.code === 0) {
          deviceStats.offline = offlineResponse.data
        }

        // 异常数量 (后端状态：其他值)
        const errorResponse = await deviceApi.getCountByEntity({ status: 0 })
        if (errorResponse.code === 0) {
          deviceStats.error = errorResponse.data
        }
      } catch (error) {
        console.error('加载统计信息失败：', error)
      }
    }

    // 搜索
    const handleSearch = () => {
      pagination.currentPage = 1
      loadDeviceList()
    }

    // 重置
    const handleReset = () => {
      Object.assign(searchForm, {
        name: '',
        type: '',
        status: ''
      })
      pagination.currentPage = 1
      loadDeviceList()
    }

    // 刷新统计
    const handleRefreshCount = () => {
      loadDeviceStats()
    }

    // 添加设备
    const handleAdd = () => {
      dialogTitle.value = '添加设备'
      Object.assign(deviceForm, {
        id: null,
        name: '',
        type: '',
        ip: '',
        port: 8000,
        username: '',
        password: ''
      })
      dialogVisible.value = true
    }

    // 编辑设备
    const handleEdit = (row) => {
      dialogTitle.value = '编辑设备'
      // 编辑时确保type是字符串格式
      const editData = {
        ...row,
        type: convertIntegerToType(row.type)
      }
      Object.assign(deviceForm, editData)
      dialogVisible.value = true
    }

    // 删除设备
    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除设备 "${row.name}" 吗？`,
          '提示',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        const response = await deviceApi.deleteById(row.id)
        if (response.code === 0) {
          ElMessage.success('删除成功')
          loadDeviceList()
          loadDeviceStats()
        } else {
          ElMessage.error(response.msg || '删除失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除设备失败：', error)
          ElMessage.error('删除设备失败')
        }
      }
    }

    // 提交表单
    const handleSubmit = async () => {
      try {
        await deviceFormRef.value.validate()

        // 转换前端字符串类型为后端Integer类型
        const deviceData = { ...deviceForm }
        deviceData.type = convertTypeToInteger(deviceForm.type)

        const response = deviceForm.id
          ? await deviceApi.update(deviceData)
          : await deviceApi.insert(deviceData)

        if (response.code === 0) {
          ElMessage.success('保存成功')
          dialogVisible.value = false
          loadDeviceList()
          loadDeviceStats()
        } else {
          ElMessage.error(response.msg || '保存失败')
        }
      } catch (error) {
        console.error('保存失败：', error)
        ElMessage.error('保存失败')
      }
    }

    // 批量添加
    const handleBatchAdd = () => {
      batchForm.devices = [
        { name: '', type: '', ip: '', port: 8000 }
      ]
      batchDialogVisible.value = true
    }

    // 添加批量设备行
    const addBatchDevice = () => {
      batchForm.devices.push({ name: '', type: '', ip: '', port: 8000 })
    }

    // 删除批量设备行
    const removeBatchDevice = (index) => {
      batchForm.devices.splice(index, 1)
    }

    // 批量添加提交
    const handleBatchSubmit = async () => {
      try {
        if (batchForm.devices.length === 0) {
          ElMessage.warning('请至少添加一个设备')
          return
        }

        // 转换批量设备数据的类型
        const deviceListData = batchForm.devices.map(device => ({
          ...device,
          type: convertTypeToInteger(device.type)
        }))

        const response = await deviceApi.insertBatch(deviceListData)
        if (response.code === 0) {
          ElMessage.success('批量添加成功')
          batchDialogVisible.value = false
          loadDeviceList()
          loadDeviceStats()
        } else {
          ElMessage.error(response.msg || '批量添加失败')
        }
      } catch (error) {
        console.error('批量添加失败：', error)
        ElMessage.error('批量添加失败')
      }
    }

    // 选择改变
    const handleSelectionChange = (selection) => {
      selectedDevices.value = selection
    }

    // 清空选择
    const handleClearSelection = () => {
      selectedDevices.value = []
    }

    // 批量删除
    const handleBatchDelete = async () => {
      try {
        await ElMessageBox.confirm(
          `确定要删除选中的 ${selectedDevices.value.length} 个设备吗？`,
          '提示',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        const ids = selectedDevices.value.map(device => device.id)
        const response = await deviceApi.deleteBatch(ids)

        if (response.code === 0) {
          ElMessage.success('批量删除成功')
          selectedDevices.value = []
          loadDeviceList()
          loadDeviceStats()
        } else {
          ElMessage.error(response.msg || '批量删除失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量删除失败：', error)
          ElMessage.error('批量删除失败')
        }
      }
    }

    // 批量更新
    const handleBatchUpdate = async () => {
      try {
        const response = await deviceApi.updateBatch(selectedDevices.value)
        if (response.code === 0) {
          ElMessage.success('批量更新成功')
          selectedDevices.value = []
          loadDeviceList()
          loadDeviceStats()
        } else {
          ElMessage.error(response.msg || '批量更新失败')
        }
      } catch (error) {
        console.error('批量更新失败：', error)
        ElMessage.error('批量更新失败')
      }
    }

    // 分页事件
    const handleSizeChange = (val) => {
      pagination.pageSize = val
      pagination.currentPage = 1
      loadDeviceList()
    }

    const handleCurrentChange = (val) => {
      pagination.currentPage = val
      loadDeviceList()
    }

    // 类型转换工具函数
    const convertTypeToInteger = (typeStr) => {
      const typeMap = {
        'camera': 2,
        'nvr': 3,
        'dvr': 4,
        'gb28181': 1
      }
      return typeMap[typeStr] || 2 // 默认为摄像头
    }

    const convertIntegerToType = (typeInt) => {
      const typeMap = {
        1: 'gb28181',
        2: 'camera',
        3: 'nvr',
        4: 'dvr'
      }
      return typeMap[typeInt] || 'camera' // 默认为摄像头
    }

    const getTypeLabel = (typeStr) => {
      const labelMap = {
        'camera': '摄像头',
        'nvr': 'NVR',
        'dvr': 'DVR',
        'gb28181': 'GB28181'
      }
      return labelMap[typeStr] || '摄像头'
    }

    // 状态转换工具函数
    const convertStatusToInteger = (statusStr) => {
      const statusMap = {
        'online': 1,
        'offline': 2,
        'error': 0
      }
      return statusMap[statusStr] || 0
    }

    onMounted(() => {
      loadDeviceList()
      loadDeviceStats()
    })

    return {
      searchForm,
      deviceList,
      loading,
      selectedDevices,
      deviceStats,
      pagination,
      dialogVisible,
      dialogTitle,
      deviceForm,
      deviceRules,
      deviceFormRef,
      batchDialogVisible,
      batchFormRef,
      batchForm,
      handleSearch,
      handleReset,
      handleRefreshCount,
      handleAdd,
      handleEdit,
      handleDelete,
      handleSubmit,
      handleBatchAdd,
      addBatchDevice,
      removeBatchDevice,
      handleBatchSubmit,
      handleSelectionChange,
      handleClearSelection,
      handleBatchDelete,
      handleBatchUpdate,
      handleSizeChange,
      handleCurrentChange,
      getTypeLabel
    }
  }
}
</script>

<style scoped>
.device-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-buttons {
  display: flex;
  gap: 10px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
}

.stat-card:nth-child(2) .stat-card {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-card:nth-child(3) .stat-card {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-card:nth-child(4) .stat-card {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-item {
  text-align: center;
  padding: 10px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
}

.search-form {
  margin-bottom: 20px;
}

.batch-actions {
  margin-bottom: 20px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>