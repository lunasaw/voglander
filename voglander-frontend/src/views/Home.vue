<template>
  <div class="home">
    <el-container>
      <el-header>
        <h1>Voglander 设备管理系统</h1>
        <el-menu
          :default-active="activeIndex"
          class="el-menu-demo"
          mode="horizontal"
          @select="handleSelect"
        >
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/device">设备管理</el-menu-item>
          <el-menu-item index="/about">关于</el-menu-item>
        </el-menu>
      </el-header>

      <el-main>
        <el-row :gutter="20">
          <el-col :span="6">
            <el-card class="box-card">
              <template #header>
                <div class="card-header">设备总数</div>
              </template>
              <div class="text item">{{ deviceCount }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="box-card">
              <template #header>
                <div class="card-header">在线设备</div>
              </template>
              <div class="text item">{{ onlineDeviceCount }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="box-card">
              <template #header>
                <div class="card-header">离线设备</div>
              </template>
              <div class="text item">{{ offlineDeviceCount }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="box-card">
              <template #header>
                <div class="card-header">异常设备</div>
              </template>
              <div class="text item">{{ errorDeviceCount }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="24">
            <el-card class="box-card">
              <template #header>
                <div class="card-header">快速操作</div>
              </template>
              <el-space wrap>
                <el-button type="primary" @click="$router.push('/device')">
                  <el-icon><Plus /></el-icon>
                  添加设备
                </el-button>
                <el-button type="success">
                  <el-icon><Refresh /></el-icon>
                  刷新状态
                </el-button>
                <el-button type="warning">
                  <el-icon><Setting /></el-icon>
                  系统设置
                </el-button>
              </el-space>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Refresh, Setting } from '@element-plus/icons-vue'

export default {
  name: 'HomePage',
  components: {
    Plus,
    Refresh,
    Setting
  },
    setup() {
    const router = useRouter()

    const activeIndex = ref('/')
    const deviceCount = ref(0)
    const onlineDeviceCount = ref(0)
    const offlineDeviceCount = ref(0)
    const errorDeviceCount = ref(0)

    const handleSelect = (key) => {
      router.push(key)
    }

    const loadStatistics = async () => {
      // 这里可以调用API获取统计数据
      deviceCount.value = 125
      onlineDeviceCount.value = 98
      offlineDeviceCount.value = 20
      errorDeviceCount.value = 7
    }

    onMounted(() => {
      loadStatistics()
    })

    return {
      activeIndex,
      deviceCount,
      onlineDeviceCount,
      offlineDeviceCount,
      errorDeviceCount,
      handleSelect
    }
  }
}
</script>

<style scoped>
.home {
  height: 100vh;
}

.el-header {
  background-color: #545c64;
  color: #fff;
  text-align: center;
  line-height: 60px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.el-header h1 {
  margin: 0;
  font-size: 24px;
}

.el-main {
  background-color: #f5f5f5;
  color: #333;
  padding: 20px;
}

.card-header {
  font-weight: bold;
}

.text {
  font-size: 32px;
  font-weight: bold;
  text-align: center;
  color: #409eff;
}

.item {
  margin-bottom: 18px;
}

.box-card {
  height: 120px;
}
</style>