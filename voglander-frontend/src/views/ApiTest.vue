<template>
  <div class="api-test-page">
    <el-card>
      <template #header>
        <span>设备API接口测试</span>
      </template>

      <el-tabs v-model="activeTab" type="card">
        <!-- 查询接口 -->
        <el-tab-pane label="查询接口" name="query">
          <el-space direction="vertical" size="large" style="width: 100%">

            <!-- 根据ID获取设备 -->
            <el-card>
              <template #header>根据ID获取设备</template>
              <el-form :inline="true">
                <el-form-item label="设备ID">
                  <el-input-number v-model="queryForms.getById.id" :min="1" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="testGetById">测试</el-button>
                </el-form-item>
              </el-form>
              <el-input
                v-model="results.getById"
                type="textarea"
                :rows="4"
                placeholder="结果将显示在这里..."
                readonly
              />
            </el-card>

            <!-- 根据条件获取设备 -->
            <el-card>
              <template #header>根据条件获取设备</template>
              <el-form :inline="true">
                <el-form-item label="设备名称">
                  <el-input v-model="queryForms.getByEntity.name" />
                </el-form-item>
                <el-form-item label="设备类型">
                  <el-select v-model="queryForms.getByEntity.type" clearable>
                    <el-option label="摄像头" value="camera" />
                    <el-option label="NVR" value="nvr" />
                    <el-option label="DVR" value="dvr" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="testGetByEntity">测试</el-button>
                </el-form-item>
              </el-form>
              <el-input
                v-model="results.getByEntity"
                type="textarea"
                :rows="4"
                placeholder="结果将显示在这里..."
                readonly
              />
            </el-card>

            <!-- 获取设备列表 -->
            <el-card>
              <template #header>获取设备列表</template>
              <el-form :inline="true">
                <el-form-item label="设备名称">
                  <el-input v-model="queryForms.getList.name" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="testGetList">测试</el-button>
                </el-form-item>
              </el-form>
              <el-input
                v-model="results.getList"
                type="textarea"
                :rows="4"
                placeholder="结果将显示在这里..."
                readonly
              />
            </el-card>

            <!-- 分页查询设备 -->
            <el-card>
              <template #header>分页查询设备</template>
              <el-form :inline="true">
                <el-form-item label="页码">
                  <el-input-number v-model="queryForms.getPageList.page" :min="1" />
                </el-form-item>
                <el-form-item label="每页数量">
                  <el-input-number v-model="queryForms.getPageList.size" :min="1" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="testGetPageList">测试</el-button>
                </el-form-item>
              </el-form>
              <el-input
                v-model="results.getPageList"
                type="textarea"
                :rows="4"
                placeholder="结果将显示在这里..."
                readonly
              />
            </el-card>

            <!-- 分页查询设备（带条件） -->
            <el-card>
              <template #header>分页查询设备（带条件）</template>
              <el-form :inline="true">
                <el-form-item label="页码">
                  <el-input-number v-model="queryForms.getPageListByEntity.page" :min="1" />
                </el-form-item>
                <el-form-item label="每页数量">
                  <el-input-number v-model="queryForms.getPageListByEntity.size" :min="1" />
                </el-form-item>
                <el-form-item label="设备名称">
                  <el-input v-model="queryForms.getPageListByEntity.name" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="testGetPageListByEntity">测试</el-button>
                </el-form-item>
              </el-form>
              <el-input
                v-model="results.getPageListByEntity"
                type="textarea"
                :rows="4"
                placeholder="结果将显示在这里..."
                readonly
              />
            </el-card>

          </el-space>
        </el-tab-pane>

        <!-- 统计接口 -->
        <el-tab-pane label="统计接口" name="count">
          <el-space direction="vertical" size="large" style="width: 100%">

            <!-- 获取设备总数 -->
            <el-card>
              <template #header>获取设备总数</template>
              <el-button type="primary" @click="testGetCount">测试</el-button>
              <el-input
                v-model="results.getCount"
                type="textarea"
                :rows="4"
                placeholder="结果将显示在这里..."
                readonly
                style="margin-top: 10px;"
              />
            </el-card>

            <!-- 根据条件获取设备数量 -->
            <el-card>
              <template #header>根据条件获取设备数量</template>
              <el-form :inline="true">
                <el-form-item label="设备状态">
                  <el-select v-model="queryForms.getCountByEntity.status" clearable>
                    <el-option label="在线" value="online" />
                    <el-option label="离线" value="offline" />
                    <el-option label="异常" value="error" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="testGetCountByEntity">测试</el-button>
                </el-form-item>
              </el-form>
              <el-input
                v-model="results.getCountByEntity"
                type="textarea"
                :rows="4"
                placeholder="结果将显示在这里..."
                readonly
              />
            </el-card>

          </el-space>
        </el-tab-pane>

        <!-- 添加接口 -->
        <el-tab-pane label="添加接口" name="insert">
          <el-space direction="vertical" size="large" style="width: 100%">

            <!-- 添加单个设备 -->
            <el-card>
              <template #header>添加单个设备</template>
              <el-form :model="insertForms.single" label-width="100px">
                <el-form-item label="设备名称">
                  <el-input v-model="insertForms.single.name" />
                </el-form-item>
                <el-form-item label="设备类型">
                  <el-input-number v-model="insertForms.single.type" />
                </el-form-item>
                <el-form-item label="IP地址">
                  <el-input v-model="insertForms.single.ip" />
                </el-form-item>
                <el-form-item label="端口">
                  <el-input-number v-model="insertForms.single.port" :min="1" :max="65535" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="testInsertSingle">测试添加</el-button>
                </el-form-item>
              </el-form>
              <el-input
                v-model="results.insert"
                type="textarea"
                :rows="4"
                placeholder="结果将显示在这里..."
                readonly
              />
            </el-card>

            <!-- 批量添加设备 -->
            <el-card>
              <template #header>批量添加设备</template>
              <el-button type="primary" @click="testInsertBatch">测试批量添加</el-button>
              <el-input
                v-model="results.insertBatch"
                type="textarea"
                :rows="4"
                placeholder="结果将显示在这里..."
                readonly
                style="margin-top: 10px;"
              />
            </el-card>

          </el-space>
        </el-tab-pane>

        <!-- 更新接口 -->
        <el-tab-pane label="更新接口" name="update">
          <el-space direction="vertical" size="large" style="width: 100%">

            <!-- 更新设备 -->
            <el-card>
              <template #header>更新设备</template>
              <el-form :model="updateForms.single" label-width="100px">
                <el-form-item label="设备ID">
                  <el-input-number v-model="updateForms.single.id" :min="1" />
                </el-form-item>
                <el-form-item label="设备名称">
                  <el-input v-model="updateForms.single.name" />
                </el-form-item>
                <el-form-item label="设备类型">
                  <el-select v-model="updateForms.single.type">
                    <el-option label="摄像头" value="camera" />
                    <el-option label="NVR" value="nvr" />
                    <el-option label="DVR" value="dvr" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="testUpdateSingle">测试更新</el-button>
                </el-form-item>
              </el-form>
              <el-input
                v-model="results.update"
                type="textarea"
                :rows="4"
                placeholder="结果将显示在这里..."
                readonly
              />
            </el-card>

            <!-- 批量更新设备 -->
            <el-card>
              <template #header>批量更新设备</template>
              <el-button type="primary" @click="testUpdateBatch">测试批量更新</el-button>
              <el-input
                v-model="results.updateBatch"
                type="textarea"
                :rows="4"
                placeholder="结果将显示在这里..."
                readonly
                style="margin-top: 10px;"
              />
            </el-card>

          </el-space>
        </el-tab-pane>

        <!-- 删除接口 -->
        <el-tab-pane label="删除接口" name="delete">
          <el-space direction="vertical" size="large" style="width: 100%">

            <!-- 根据ID删除设备 -->
            <el-card>
              <template #header>根据ID删除设备</template>
              <el-form :inline="true">
                <el-form-item label="设备ID">
                  <el-input-number v-model="deleteForms.single.id" :min="1" />
                </el-form-item>
                <el-form-item>
                  <el-button type="danger" @click="testDeleteById">测试删除</el-button>
                </el-form-item>
              </el-form>
              <el-input
                v-model="results.deleteById"
                type="textarea"
                :rows="4"
                placeholder="结果将显示在这里..."
                readonly
              />
            </el-card>

            <!-- 根据条件删除设备 -->
            <el-card>
              <template #header>根据条件删除设备</template>
              <el-form :inline="true">
                <el-form-item label="设备名称">
                  <el-input v-model="deleteForms.byEntity.name" />
                </el-form-item>
                <el-form-item>
                  <el-button type="danger" @click="testDeleteByEntity">测试删除</el-button>
                </el-form-item>
              </el-form>
              <el-input
                v-model="results.deleteByEntity"
                type="textarea"
                :rows="4"
                placeholder="结果将显示在这里..."
                readonly
              />
            </el-card>

            <!-- 批量删除设备 -->
            <el-card>
              <template #header>批量删除设备</template>
              <el-button type="danger" @click="testDeleteBatch">测试批量删除</el-button>
              <el-input
                v-model="results.deleteBatch"
                type="textarea"
                :rows="4"
                placeholder="结果将显示在这里..."
                readonly
                style="margin-top: 10px;"
              />
            </el-card>

          </el-space>
        </el-tab-pane>

      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { deviceApi } from '../api'

export default {
  name: 'ApiTestPage',
  setup() {
    const activeTab = ref('query')

    // 查询表单数据
    const queryForms = reactive({
      getById: { id: 1 },
      getByEntity: { name: '', type: '' },
      getList: { name: '' },
      getPageList: { page: 1, size: 10 },
      getPageListByEntity: { page: 1, size: 10, name: '' },
      getCountByEntity: { status: '' }
    })

    // 插入表单数据
    const insertForms = reactive({
      single: {
        name: '测试设备',
        type: 1,
        ip: '192.168.1.100',
        port: 8000
      }
    })

    // 更新表单数据
    const updateForms = reactive({
      single: {
        id: 1,
        name: '更新后的设备',
        type: 'nvr'
      }
    })

    // 删除表单数据
    const deleteForms = reactive({
      single: { id: 1 },
      byEntity: { name: '' }
    })

    // 结果显示
    const results = reactive({
      getById: '',
      getByEntity: '',
      getList: '',
      getPageList: '',
      getPageListByEntity: '',
      getCount: '',
      getCountByEntity: '',
      insert: '',
      insertBatch: '',
      update: '',
      updateBatch: '',
      deleteById: '',
      deleteByEntity: '',
      deleteBatch: ''
    })

    // 测试方法
    const testGetById = async () => {
      try {
        const response = await deviceApi.getById(queryForms.getById.id)
        results.getById = JSON.stringify(response, null, 2)
        ElMessage.success('调用成功')
      } catch (error) {
        results.getById = `错误: ${error.message}`
        ElMessage.error('调用失败')
      }
    }

    const testGetByEntity = async () => {
      try {
        const response = await deviceApi.getByEntity(queryForms.getByEntity)
        results.getByEntity = JSON.stringify(response, null, 2)
        ElMessage.success('调用成功')
      } catch (error) {
        results.getByEntity = `错误: ${error.message}`
        ElMessage.error('调用失败')
      }
    }

    const testGetList = async () => {
      try {
        const response = await deviceApi.getList(queryForms.getList)
        results.getList = JSON.stringify(response, null, 2)
        ElMessage.success('调用成功')
      } catch (error) {
        results.getList = `错误: ${error.message}`
        ElMessage.error('调用失败')
      }
    }

    const testGetPageList = async () => {
      try {
        const response = await deviceApi.getPageList(
          queryForms.getPageList.page,
          queryForms.getPageList.size
        )
        results.getPageList = JSON.stringify(response, null, 2)
        ElMessage.success('调用成功')
      } catch (error) {
        results.getPageList = `错误: ${error.message}`
        ElMessage.error('调用失败')
      }
    }

    const testGetPageListByEntity = async () => {
      try {
        const { page, size, ...condition } = queryForms.getPageListByEntity
        const response = await deviceApi.getPageListByEntity(page, size, condition)
        results.getPageListByEntity = JSON.stringify(response, null, 2)
        ElMessage.success('调用成功')
      } catch (error) {
        results.getPageListByEntity = `错误: ${error.message}`
        ElMessage.error('调用失败')
      }
    }

    const testGetCount = async () => {
      try {
        const response = await deviceApi.getCount()
        results.getCount = JSON.stringify(response, null, 2)
        ElMessage.success('调用成功')
      } catch (error) {
        results.getCount = `错误: ${error.message}`
        ElMessage.error('调用失败')
      }
    }

    const testGetCountByEntity = async () => {
      try {
        const response = await deviceApi.getCountByEntity(queryForms.getCountByEntity)
        results.getCountByEntity = JSON.stringify(response, null, 2)
        ElMessage.success('调用成功')
      } catch (error) {
        results.getCountByEntity = `错误: ${error.message}`
        ElMessage.error('调用失败')
      }
    }

    const testInsertSingle = async () => {
      try {
        const response = await deviceApi.insert(insertForms.single)
        results.insert = JSON.stringify(response, null, 2)
        ElMessage.success('调用成功')
      } catch (error) {
        results.insert = `错误: ${error.message}`
        ElMessage.error('调用失败')
      }
    }

    const testInsertBatch = async () => {
      try {
        const testData = [
          { name: '批量设备1', type: 'camera', ip: '192.168.1.101', port: 8000 },
          { name: '批量设备2', type: 'nvr', ip: '192.168.1.102', port: 8000 }
        ]
        const response = await deviceApi.insertBatch(testData)
        results.insertBatch = JSON.stringify(response, null, 2)
        ElMessage.success('调用成功')
      } catch (error) {
        results.insertBatch = `错误: ${error.message}`
        ElMessage.error('调用失败')
      }
    }

    const testUpdateSingle = async () => {
      try {
        const response = await deviceApi.update(updateForms.single)
        results.update = JSON.stringify(response, null, 2)
        ElMessage.success('调用成功')
      } catch (error) {
        results.update = `错误: ${error.message}`
        ElMessage.error('调用失败')
      }
    }

    const testUpdateBatch = async () => {
      try {
        const testData = [
          { id: 1, name: '批量更新设备1', type: 'camera' },
          { id: 2, name: '批量更新设备2', type: 'nvr' }
        ]
        const response = await deviceApi.updateBatch(testData)
        results.updateBatch = JSON.stringify(response, null, 2)
        ElMessage.success('调用成功')
      } catch (error) {
        results.updateBatch = `错误: ${error.message}`
        ElMessage.error('调用失败')
      }
    }

    const testDeleteById = async () => {
      try {
        const response = await deviceApi.deleteById(deleteForms.single.id)
        results.deleteById = JSON.stringify(response, null, 2)
        ElMessage.success('调用成功')
      } catch (error) {
        results.deleteById = `错误: ${error.message}`
        ElMessage.error('调用失败')
      }
    }

    const testDeleteByEntity = async () => {
      try {
        const response = await deviceApi.deleteByEntity(deleteForms.byEntity)
        results.deleteByEntity = JSON.stringify(response, null, 2)
        ElMessage.success('调用成功')
      } catch (error) {
        results.deleteByEntity = `错误: ${error.message}`
        ElMessage.error('调用失败')
      }
    }

    const testDeleteBatch = async () => {
      try {
        const testIds = [1, 2, 3]
        const response = await deviceApi.deleteBatch(testIds)
        results.deleteBatch = JSON.stringify(response, null, 2)
        ElMessage.success('调用成功')
      } catch (error) {
        results.deleteBatch = `错误: ${error.message}`
        ElMessage.error('调用失败')
      }
    }

    return {
      activeTab,
      queryForms,
      insertForms,
      updateForms,
      deleteForms,
      results,
      testGetById,
      testGetByEntity,
      testGetList,
      testGetPageList,
      testGetPageListByEntity,
      testGetCount,
      testGetCountByEntity,
      testInsertSingle,
      testInsertBatch,
      testUpdateSingle,
      testUpdateBatch,
      testDeleteById,
      testDeleteByEntity,
      testDeleteBatch
    }
  }
}
</script>

<style scoped>
.api-test-page {
  padding: 20px;
}

.el-card {
  margin-bottom: 20px;
}

.el-form-item {
  margin-right: 16px;
}
</style>