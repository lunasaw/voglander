<template>
  <div class="device-page">
    <!-- 主内容区域 - 统一的圆角模块 -->
    <div class="main-content">
      <!-- 统计卡片 -->
      <div class="stats-section">
        <el-row :gutter="20">
          <el-col :xs="12" :sm="6">
            <div class="stat-card total">
              <div class="stat-icon">
                <el-icon size="32">
                  <Monitor/>
                </el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-number">{{ deviceStats.total }}</div>
                <div class="stat-label">设备总数</div>
              </div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="6">
            <div class="stat-card online">
              <div class="stat-icon">
                <el-icon size="32">
                  <CircleCheck/>
                </el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-number">{{ deviceStats.online }}</div>
                <div class="stat-label">在线设备</div>
              </div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="6">
            <div class="stat-card offline">
              <div class="stat-icon">
                <el-icon size="32">
                  <CircleClose/>
                </el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-number">{{ deviceStats.offline }}</div>
                <div class="stat-label">离线设备</div>
              </div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="6">
            <div class="stat-card warning">
              <div class="stat-icon">
                <el-icon size="32">
                  <Warning/>
                </el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-number">{{ deviceStats.error }}</div>
                <div class="stat-label">异常设备</div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 搜索和筛选区域 -->
      <div class="search-section">
        <div class="section-header">
          <span class="section-title">搜索筛选</span>
          <el-button text @click="toggleSearchCollapse">
            <el-icon>
              <component :is="searchCollapsed ? 'ArrowDown' : 'ArrowUp'"/>
            </el-icon>
          </el-button>
        </div>

        <el-collapse-transition>
          <div v-show="!searchCollapsed" class="search-content">
            <el-form :inline="true" :model="searchForm" class="search-form">
              <el-form-item label="设备ID">
                <el-input
                    v-model="searchForm.deviceId"
                    placeholder="请输入设备ID"
                    clearable
                    style="width: 200px"
                />
              </el-form-item>
              <el-form-item label="设备名称">
                <el-input
                    v-model="searchForm.name"
                    placeholder="请输入设备名称"
                    clearable
                    style="width: 200px"
                />
              </el-form-item>
              <el-form-item label="IP地址">
                <el-input
                    v-model="searchForm.ip"
                    placeholder="请输入IP地址"
                    clearable
                    style="width: 200px"
                />
              </el-form-item>
              <el-form-item label="设备状态">
                <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 120px">
                  <el-option label="在线" :value="1"/>
                  <el-option label="离线" :value="0"/>
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleSearch">
                  <el-icon>
                    <Search/>
                  </el-icon>
                  搜索
                </el-button>
                <el-button @click="handleReset">
                  <el-icon>
                    <Refresh/>
                  </el-icon>
                  重置
                </el-button>
                <el-button type="primary" @click="handleAdd">
                  <el-icon>
                    <Plus/>
                  </el-icon>
                  添加设备
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-collapse-transition>
      </div>

      <!-- 设备列表区域 -->
      <div class="table-section">
        <div class="section-header">
          <div class="table-title">
            <span class="section-title">设备列表</span>
            <el-tag v-if="selectedDevices.length > 0" type="primary" size="small">
              已选择 {{ selectedDevices.length }} 项
            </el-tag>
          </div>
          <div class="table-actions">
            <!-- 批量操作 -->
            <div class="batch-actions" v-if="selectedDevices.length > 0">
              <el-button type="danger" size="small" @click="handleBatchDelete">
                <el-icon>
                  <Delete/>
                </el-icon>
                批量删除
              </el-button>
              <el-button size="small" @click="handleClearSelection">
                清空选择
              </el-button>
            </div>

            <!-- 字段显示控制 -->
            <el-dropdown @command="handleColumnCommand">
              <el-button size="small">
                <el-icon>
                  <Setting/>
                </el-icon>
                显示字段
                <el-icon>
                  <ArrowDown/>
                </el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                      v-for="column in columnConfig"
                      :key="column.key"
                      :command="column.key"
                  >
                    <el-checkbox
                        v-model="column.visible"
                        @change="handleColumnVisibleChange"
                    >
                      {{ column.label }}
                    </el-checkbox>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>

            <!-- 刷新按钮 -->
            <el-button size="small" @click="loadDeviceList">
              <el-icon>
                <Refresh/>
              </el-icon>
              刷新
            </el-button>
          </div>
        </div>

        <!-- 设备表格 -->
        <div class="table-content">
          <el-table
        :data="deviceList"
        style="width: 100%"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        row-key="id"
        table-layout="auto"
      >
            <el-table-column type="selection" width="50" fixed="left"/>

            <el-table-column
                v-if="getColumnVisible('id')"
                prop="id"
                label="ID"
                width="80"
                fixed="left"
            />

            <el-table-column
                v-if="getColumnVisible('deviceId')"
                prop="deviceId"
                label="设备ID"
                width="150"
                show-overflow-tooltip
                fixed="left"
            />

            <el-table-column
                v-if="getColumnVisible('name')"
                prop="name"
                label="设备名称"
                width="120"
                show-overflow-tooltip
            >
          <template #default="scope">
            {{ scope.row.name || '未命名' }}
          </template>
        </el-table-column>

            <el-table-column
                v-if="getColumnVisible('typeName')"
                prop="typeName"
                label="设备类型"
                width="120"
            />

            <el-table-column
                v-if="getColumnVisible('ip')"
                prop="ip"
                label="IP地址"
                width="140"
            />

            <el-table-column
                v-if="getColumnVisible('port')"
                prop="port"
                label="端口"
                width="80"
            />

            <el-table-column
                v-if="getColumnVisible('status')"
                prop="statusName"
                label="状态"
                width="100"
            >
          <template #default="scope">
            <el-tag
                :type="scope.row.status === 1 ? 'success' : scope.row.status === 0 ? 'danger' : 'warning'"
                size="small"
            >
              {{ scope.row.statusName }}
            </el-tag>
          </template>
        </el-table-column>

            <el-table-column
                v-if="getColumnVisible('serverIp')"
                prop="serverIp"
                label="注册节点"
                width="140"
            />

            <el-table-column
                v-if="getColumnVisible('registerTime')"
                prop="registerTime"
                label="注册时间"
                width="160"
            >
          <template #default="scope">
            {{ formatDateTime(scope.row.registerTime) }}
          </template>
        </el-table-column>

            <el-table-column
                v-if="getColumnVisible('keepaliveTime')"
                prop="keepaliveTime"
                label="心跳时间"
                width="160"
            >
          <template #default="scope">
            {{ formatDateTime(scope.row.keepaliveTime) }}
          </template>
        </el-table-column>

            <!-- 操作列 -->
            <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button size="small" type="primary" link @click="handleView(scope.row)">
                <el-icon>
                  <View/>
                </el-icon>
                详情
              </el-button>
              <el-button size="small" type="success" link @click="handleEdit(scope.row)">
                <el-icon>
                  <Edit/>
                </el-icon>
                编辑
              </el-button>
              <el-button size="small" type="danger" link @click="handleDelete(scope.row)">
                <el-icon>
                  <Delete/>
                </el-icon>
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

          <!-- 分页 -->
          <div class="pagination-wrapper">
            <el-pagination
                :current-page="pagination.currentPage"
                :page-size="pagination.pageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="pagination.total"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 设备详情对话框 -->
    <el-dialog
        v-model="detailDialogVisible"
        title="设备详情"
        width="800px"
        destroy-on-close
    >
      <el-descriptions :column="2" border v-if="selectedDevice">
        <el-descriptions-item label="设备ID">{{ selectedDevice.deviceId }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ selectedDevice.name || '未命名' }}</el-descriptions-item>
        <el-descriptions-item label="设备类型">{{ selectedDevice.typeName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="selectedDevice.status === 1 ? 'success' : selectedDevice.status === 0 ? 'danger' : 'warning'">
            {{ selectedDevice.statusName }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ selectedDevice.ip }}</el-descriptions-item>
        <el-descriptions-item label="端口">{{ selectedDevice.port }}</el-descriptions-item>
        <el-descriptions-item label="注册节点">{{ selectedDevice.serverIp }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formatDateTime(selectedDevice.registerTime) }}</el-descriptions-item>
        <el-descriptions-item label="心跳时间">{{ formatDateTime(selectedDevice.keepaliveTime) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(selectedDevice.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(selectedDevice.updateTime) }}</el-descriptions-item>
        <el-descriptions-item label="扩展信息" :span="2">
          <div v-if="selectedDevice.extendInfo">
            <el-descriptions :column="2" size="small" border>
              <el-descriptions-item label="序列号">{{
                  selectedDevice.extendInfo.serialNumber || '-'
                }}
              </el-descriptions-item>
              <el-descriptions-item label="传输协议">{{
                  selectedDevice.extendInfo.transport || '-'
                }}
              </el-descriptions-item>
              <el-descriptions-item label="注册有效期">{{
                  selectedDevice.extendInfo.expires || '-'
                }}s
              </el-descriptions-item>
              <el-descriptions-item label="数据流模式">{{
                  selectedDevice.extendInfo.streamMode || '-'
                }}
              </el-descriptions-item>
              <el-descriptions-item label="编码">{{ selectedDevice.extendInfo.charset || '-' }}</el-descriptions-item>
              <el-descriptions-item label="设备信息">{{
                  selectedDevice.extendInfo.deviceInfo || '-'
                }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
          <el-empty v-else description="无扩展信息" :image-size="60"/>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 设备添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      destroy-on-close
    >
      <el-form
        :model="deviceForm"
        :rules="deviceRules"
        ref="deviceFormRef"
        label-width="120px"
        label-position="left"
        class="device-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备ID" prop="deviceId">
              <el-input v-model="deviceForm.deviceId" placeholder="请输入设备ID"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备名称" prop="name">
              <el-input v-model="deviceForm.name" placeholder="请输入设备名称"/>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="IP地址" prop="ip">
              <el-input v-model="deviceForm.ip" placeholder="请输入IP地址"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="端口" prop="port">
              <el-input-number v-model="deviceForm.port" :min="1" :max="65535" style="width: 100%"/>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备种类" prop="subType">
              <el-select v-model="deviceForm.subType" placeholder="请选择设备种类" @change="handleSubTypeChange"
                         style="width: 100%">
                <el-option
                    v-for="item in enumData.deviceSubTypes"
                    :key="item.value"
                    :label="item.desc"
                    :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备协议" prop="protocol">
              <el-select v-model="deviceForm.protocol" placeholder="请选择设备协议" @change="handleProtocolChange"
                         style="width: 100%">
                <el-option
                    v-for="item in enumData.deviceProtocols"
                    :key="item.value"
                    :label="item.desc"
                    :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="protocol-type-field">
          <el-form-item label="协议类型" prop="type">
            <el-input v-model="deviceForm.typeName" placeholder="根据设备种类和协议自动计算" readonly/>
            <div class="form-tip">根据设备种类和协议自动计算</div>
          </el-form-item>
        </div>

        <!-- 扩展信息 -->
        <el-divider content-position="left">
          <span class="divider-text">扩展信息 (可选)</span>
        </el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备序列号">
              <el-input v-model="deviceForm.extendInfo.serialNumber" placeholder="请输入设备序列号"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="传输协议">
              <el-select v-model="deviceForm.extendInfo.transport" placeholder="请选择传输协议" style="width: 100%">
                <el-option
                    v-for="item in enumData.transports"
                    :key="item.value"
                    :label="item.desc"
                    :value="item.code"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="注册有效期(秒)">
              <el-input-number v-model="deviceForm.extendInfo.expires" :min="60" :max="86400" placeholder="3600"
                               style="width: 100%"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="认证密码">
              <el-input v-model="deviceForm.extendInfo.password" type="password" placeholder="请输入认证密码"
                        show-password/>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="数据流模式">
              <el-input v-model="deviceForm.extendInfo.streamMode" placeholder="请输入数据流模式"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字符编码">
              <el-select v-model="deviceForm.extendInfo.charset" placeholder="请选择字符编码" style="width: 100%">
                <el-option
                    v-for="item in enumData.charsets"
                    :key="item.value"
                    :label="item.desc"
                    :value="item.code"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
            {{ submitLoading ? '保存中...' : '确定' }}
          </el-button>
        </div>
      </template>
    </el-dialog>


  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Search, Refresh, Edit, Delete, View, Setting, ArrowDown, ArrowUp,
  Monitor, CircleCheck, CircleClose, Warning, Close, InfoFilled
} from '@element-plus/icons-vue'
import {deviceApi, enumApi} from '../api'

export default {
  name: 'DevicePage',
  components: {
    Plus, Search, Refresh, Edit, Delete, View, Setting, ArrowDown, ArrowUp,
    Monitor, CircleCheck, CircleClose, Warning, Close, InfoFilled
  },
  setup() {
    // 搜索表单 - 严格按照后端DeviceDO字段
    const searchForm = reactive({
      deviceId: '',
      name: '',
      ip: '',
      status: null // 后端：1-在线，0-离线
    })

    // UI控制
    const searchCollapsed = ref(false)
    const submitLoading = ref(false)

    // 字段显示控制
    const columnConfig = ref([
      {key: 'id', label: 'ID', visible: true},
      {key: 'deviceId', label: '设备ID', visible: true},
      {key: 'name', label: '设备名称', visible: true},
      {key: 'typeName', label: '设备类型', visible: true},
      {key: 'ip', label: 'IP地址', visible: true},
      {key: 'port', label: '端口', visible: true},
      {key: 'status', label: '状态', visible: true},
      {key: 'serverIp', label: '注册节点', visible: true},
      {key: 'registerTime', label: '注册时间', visible: true},
      {key: 'keepaliveTime', label: '心跳时间', visible: false}
    ])

    // 设备列表
    const deviceList = ref([])
    const loading = ref(false)
    const selectedDevices = ref([])

    // 枚举数据
    const enumData = reactive({
      deviceSubTypes: [],
      deviceProtocols: [],
      deviceAgreementTypes: [],
      transports: [],
      charsets: [],
      deviceStatus: []
    })

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

    // 详情对话框
    const detailDialogVisible = ref(false)
    const selectedDevice = ref(null)

    // 编辑对话框
    const dialogVisible = ref(false)
    const dialogTitle = ref('')
    const deviceFormRef = ref()



    // 设备表单 - 按照后端DeviceCreateReq字段
    const deviceForm = reactive({
      id: null,
      deviceId: '',
      name: '',
      ip: '',
      port: 5060,
      type: null, // 根据subType和protocol自动计算
      subType: null, // 设备种类
      protocol: null, // 设备协议
      typeName: '', // 协议类型名称，用于显示
      serverIp: '',
      extendInfo: {
        serialNumber: '',
        transport: 'UDP',
        expires: 3600,
        password: '',
        streamMode: '',
        charset: 'UTF-8',
        deviceInfo: ''
      }
    })

    // 表单验证规则
    const deviceRules = {
      deviceId: [
        {required: true, message: '请输入设备ID', trigger: 'blur'}
      ],
      name: [
        { required: true, message: '请输入设备名称', trigger: 'blur' }
      ],
      ip: [
        { required: true, message: '请输入IP地址', trigger: 'blur' },
        { pattern: /^(\d{1,3}\.){3}\d{1,3}$/, message: 'IP地址格式不正确', trigger: 'blur' }
      ],
      port: [
        { required: true, message: '请输入端口号', trigger: 'blur' }
      ],
      subType: [
        {required: true, message: '请选择设备种类', trigger: 'change'}
      ],
      protocol: [
        {required: true, message: '请选择设备协议', trigger: 'change'}
      ]
    }

    // 加载枚举数据
    const loadEnumData = async () => {
      try {
        const response = await enumApi.getAllEnums()
        if (response.code === 0) {
          Object.assign(enumData, response.data)
        } else {
          ElMessage.error('加载枚举数据失败：' + response.msg)
        }
      } catch (error) {
        console.error('加载枚举数据失败：', error)
        ElMessage.error('加载枚举数据失败')
      }
    }

    // 根据设备种类和协议计算协议类型
    const calculateAgreementType = async (subType, protocol) => {
      if (!subType || !protocol) {
        deviceForm.type = null
        deviceForm.typeName = ''
        return
      }

      try {
        const response = await enumApi.getDeviceAgreementType(subType, protocol)
        if (response.code === 0) {
          deviceForm.type = response.data.value
          deviceForm.typeName = response.data.desc
        } else {
          deviceForm.type = null
          deviceForm.typeName = '不支持的组合'
          ElMessage.warning(response.msg || '不支持的设备种类和协议组合')
        }
      } catch (error) {
        console.error('计算协议类型失败：', error)
        deviceForm.type = null
        deviceForm.typeName = '计算失败'
      }
    }

    // 处理设备种类变化
    const handleSubTypeChange = (value) => {
      calculateAgreementType(value, deviceForm.protocol)
    }

    // 处理协议变化
    const handleProtocolChange = (value) => {
      calculateAgreementType(deviceForm.subType, value)
    }



    // 加载设备列表 - 使用后端分页接口
    const loadDeviceList = async () => {
      loading.value = true
      try {
        // 构建搜索条件，只传递非空值
        const searchCondition = {}
        if (searchForm.deviceId) searchCondition.deviceId = searchForm.deviceId
        if (searchForm.name) searchCondition.name = searchForm.name
        if (searchForm.ip) searchCondition.ip = searchForm.ip
        if (searchForm.status !== null) searchCondition.status = searchForm.status

        const response = await deviceApi.getPageListByEntity(
          pagination.currentPage,
          pagination.pageSize,
          searchCondition
        )

        if (response.code === 0) {
          // 直接使用后端返回的DeviceVO数据，不做任何转换
          deviceList.value = response.data.records || []
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

        // 离线数量 (后端状态：0-离线)
        const offlineResponse = await deviceApi.getCountByEntity({status: 0})
        if (offlineResponse.code === 0) {
          deviceStats.offline = offlineResponse.data
        }

        // 异常数量计算
        deviceStats.error = Math.max(0, deviceStats.total - deviceStats.online - deviceStats.offline)
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
        deviceId: '',
        name: '',
        ip: '',
        status: null
      })
      pagination.currentPage = 1
      loadDeviceList()
    }

    // 查看详情
    const handleView = (row) => {
      selectedDevice.value = row
      detailDialogVisible.value = true
    }

    // 添加设备
    const handleAdd = () => {
      dialogTitle.value = '添加设备'
      Object.assign(deviceForm, {
        id: null,
        deviceId: '',
        name: '',
        ip: '',
        port: 5060,
        type: 1
      })
      dialogVisible.value = true
    }

    // 编辑设备
    const handleEdit = (row) => {
      dialogTitle.value = '编辑设备'
      Object.assign(deviceForm, {
        id: row.id,
        deviceId: row.deviceId,
        name: row.name,
        ip: row.ip,
        port: row.port,
        type: row.type
      })
      dialogVisible.value = true
    }

    // 删除设备
    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(
            `确定要删除设备 "${row.name || row.deviceId}" 吗？`,
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

        const response = deviceForm.id
            ? await deviceApi.update(deviceForm)
            : await deviceApi.insert(deviceForm)

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

    // 时间格式化
    const formatDateTime = (dateTime) => {
      if (!dateTime) return '-'
      return new Date(dateTime).toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    }

    // 搜索折叠控制
    const toggleSearchCollapse = () => {
      searchCollapsed.value = !searchCollapsed.value
    }

    // 字段显示控制
    const getColumnVisible = (key) => {
      const column = columnConfig.value.find(c => c.key === key)
      return column ? column.visible : true
    }

    const handleColumnCommand = (command) => {
      const column = columnConfig.value.find(c => c.key === command)
      if (column) {
        column.visible = !column.visible
      }
    }

    const handleColumnVisibleChange = () => {
      // 表格字段显示变化时无需重新加载数据
    }


    // 其他逻辑保持不变
    onMounted(() => {
      // 优先加载枚举数据，然后加载设备数据
      loadEnumData()
      loadDeviceList()
      loadDeviceStats()
    })

    return {
      searchForm,
      deviceList,
      loading,
      selectedDevices,
      enumData,
      deviceStats,
      pagination,
      detailDialogVisible,
      selectedDevice,
      dialogVisible,
      dialogTitle,
      deviceForm,
      deviceRules,
      deviceFormRef,
      searchCollapsed,
      submitLoading,
      columnConfig,
      handleSearch,
      handleReset,
      handleView,
      handleAdd,
      handleEdit,
      handleDelete,
      handleSubmit,
      handleSelectionChange,
      handleClearSelection,
      handleBatchDelete,
      handleSizeChange,
      handleCurrentChange,
      formatDateTime,
      handleSubTypeChange,
      handleProtocolChange,
      toggleSearchCollapse,
      getColumnVisible,
      handleColumnCommand,
      handleColumnVisibleChange,
      loadDeviceList
    }
  }
}
</script>

<style scoped>
.device-page {
  padding: 24px;
  min-height: 100%;
  background: #f0f2f5;
}

/* 主内容区域 - 统一的圆角模块 */
.main-content {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  border: 1px solid #ebeef5;
}

/* 统计卡片区域 */
.stats-section {
  padding: 24px 24px 0 24px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid #ebeef5;
  transition: all 0.3s ease;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-card:hover {
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.stat-card.total {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.stat-card.online {
  background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
  color: white;
}

.stat-card.offline {
  background: linear-gradient(135deg, #ff4d4f 0%, #cf1322 100%);
  color: white;
}

.stat-card.warning {
  background: linear-gradient(135deg, #faad14 0%, #d48806 100%);
  color: white;
}

.stat-icon {
  flex-shrink: 0;
  opacity: 0.9;
}

.stat-content {
  flex: 1;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 4px;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
  font-weight: 500;
}

/* 搜索区域 */
.search-section {
  padding: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-weight: 600;
  color: #303133;
  font-size: 16px;
}

.search-content {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
}

.search-form {
  margin: 0;
}

.search-form :deep(.el-form-item) {
  margin-right: 24px;
  margin-bottom: 16px;
}

.search-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
  text-align: left;
  display: flex;
  align-items: center;
}

/* 搜索表单输入框优化 */
.search-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  transition: all 0.2s;
}

.search-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c0c4cc inset;
}

.search-form :deep(.el-select .el-input__wrapper) {
  border-radius: 8px;
}

/* 表格区域 */
.table-section {
  padding: 0;
}

.table-section .section-header {
  padding: 24px 24px 16px 24px;
  margin-bottom: 0;
  border-bottom: 1px solid #f0f0f0;
}

.table-content {
  padding: 0;
}

.table-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.table-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.batch-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 8px 16px;
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 6px;
  margin-right: 12px;
}

/* 表格样式 */
.table-content :deep(.el-table) {
  border-radius: 0;
}

.table-content :deep(.el-table__header) {
  background: #fafafa;
}

.table-content :deep(.el-table th) {
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;
  font-weight: 600;
  color: #303133;
}

.table-content :deep(.el-table td) {
  border-bottom: 1px solid #f5f7fa;
}

.table-content :deep(.el-table__row:hover) {
  background: #f5f7fa;
}

.action-buttons {
  display: flex;
  gap: 4px;
  align-items: center;
  flex-wrap: nowrap;
}

.action-buttons .el-button {
  margin: 0;
  padding: 4px 8px;
  font-size: 12px;
}

/* 分页 */
.pagination-wrapper {
  padding: 16px 24px 24px 24px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

/* 对话框样式 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}

.divider-text {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

/* 设备添加/编辑表单优化 */
.device-form {
  max-height: 70vh;
  overflow-y: auto;
}

/* 表单项优化 */
.device-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.device-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #303133;
  line-height: 1.6;
  text-align: left;
  display: flex;
  align-items: center;
}

.device-form :deep(.el-form-item__content) {
  line-height: 1.6;
}

/* 输入框优化 */
.device-form :deep(.el-input) {
  border-radius: 8px;
}

.device-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  transition: all 0.2s;
}

.device-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c0c4cc inset;
}

.device-form :deep(.el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 1px #409eff inset;
}

/* 选择器优化 */
.device-form :deep(.el-select) {
  width: 100%;
}

.device-form :deep(.el-select .el-input__wrapper) {
  border-radius: 8px;
}

/* 数字输入框优化 */
.device-form :deep(.el-input-number) {
  width: 100%;
}

.device-form :deep(.el-input-number .el-input__wrapper) {
  border-radius: 8px;
}

/* 分割线优化 */
.device-form :deep(.el-divider) {
  margin: 28px 0 20px 0;
}

.device-form :deep(.el-divider__text) {
  background: #fafafa;
  padding: 0 16px;
}

/* 表单行间距优化 */
.device-form .el-row {
  margin-bottom: 0;
}

.device-form .el-row + .el-row {
  margin-top: 0;
}

/* 只读输入框样式 */
.device-form :deep(.el-input.is-disabled .el-input__wrapper) {
  background-color: #f5f7fa;
  border-color: #e4e7ed;
  color: #606266;
}

/* 协议类型字段特殊样式 */
.protocol-type-field {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
  margin: 8px 0;
}

.protocol-type-field :deep(.el-form-item) {
  margin-bottom: 8px;
}

.protocol-type-field :deep(.el-input.is-disabled .el-input__wrapper) {
  background-color: #ffffff;
  border: 1px dashed #d9d9d9;
}


/* 字段显示控制 */
.table-section :deep(.el-dropdown-menu__item) {
  padding: 8px 16px;
}

.table-section :deep(.el-checkbox) {
  margin-right: 8px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  /* 移动端适配 */
}

@media (max-width: 768px) {
  .device-page {
    padding: 16px;
  }

  .main-content {
    border-radius: 12px;
  }

  .stats-section {
    padding: 16px 16px 0 16px;
  }

  .search-section {
    padding: 16px;
  }

  .table-section .section-header {
    padding: 16px 16px 12px 16px;
  }

  .pagination-wrapper {
    padding: 12px 16px 16px 16px;
  }

  .stat-card {
    padding: 16px;
    flex-direction: column;
    text-align: center;
    gap: 8px;
  }

  .search-form :deep(.el-form-item) {
    margin-right: 0;
    margin-bottom: 12px;
  }

  .search-content {
    padding: 12px;
  }

  /* 设备表单移动端优化 */
  .device-form :deep(.el-form-item__label) {
    line-height: 1.4;
    margin-bottom: 4px;
  }

  .device-form .el-row {
    margin-left: 0 !important;
    margin-right: 0 !important;
  }

  .device-form .el-col {
    padding-left: 0 !important;
    padding-right: 0 !important;
    margin-bottom: 12px;
  }

  .protocol-type-field {
    padding: 12px;
    margin: 4px 0;
  }

  .section-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }

  .table-actions {
    justify-content: space-between;
  }

  .action-buttons {
    flex-direction: column;
    gap: 8px;
  }

  .action-buttons .el-button {
    width: 100%;
    justify-content: center;
  }
}

/* 动画效果 */
.el-card {
  transition: all 0.3s ease;
}

.el-button {
  transition: all 0.2s ease;
}

.stat-card {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 自定义滚动条 */
:deep(.el-table__body-wrapper) {
  scrollbar-width: thin;
  scrollbar-color: #c1c1c1 #f1f1f1;
}

:deep(.el-table__body-wrapper::-webkit-scrollbar) {
  height: 8px;
}

:deep(.el-table__body-wrapper::-webkit-scrollbar-track) {
  background: #f1f1f1;
  border-radius: 4px;
}

:deep(.el-table__body-wrapper::-webkit-scrollbar-thumb) {
  background: #c1c1c1;
  border-radius: 4px;
}

:deep(.el-table__body-wrapper::-webkit-scrollbar-thumb:hover) {
  background: #a8a8a8;
}
</style>