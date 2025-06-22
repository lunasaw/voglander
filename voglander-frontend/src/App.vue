<template>
  <div id="app">
    <el-container>
      <el-header>
        <div class="header-content">
          <div class="logo">
            <h2>Voglander 设备管理系统</h2>
          </div>
          <el-menu
            mode="horizontal"
            :default-active="activeIndex"
            class="el-menu-demo"
            @select="handleSelect"
          >
            <el-menu-item index="1" @click="$router.push('/')">
              <el-icon><House /></el-icon>
              <span>首页</span>
            </el-menu-item>
            <el-menu-item index="2" @click="$router.push('/device')">
              <el-icon><Monitor /></el-icon>
              <span>设备管理</span>
            </el-menu-item>
            <el-menu-item index="3" @click="$router.push('/api-test')">
              <el-icon><Tools /></el-icon>
              <span>API测试</span>
            </el-menu-item>
            <el-menu-item index="4" @click="$router.push('/about')">
              <el-icon><InfoFilled /></el-icon>
              <span>关于</span>
            </el-menu-item>
          </el-menu>
        </div>
      </el-header>

      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { House, Monitor, Tools, InfoFilled } from '@element-plus/icons-vue'

export default {
  name: 'App',
  components: {
    House,
    Monitor,
    Tools,
    InfoFilled
  },
  setup() {
    const route = useRoute()

    const activeIndex = computed(() => {
      const path = route.path
      switch (path) {
        case '/':
          return '1'
        case '/device':
          return '2'
        case '/api-test':
          return '3'
        case '/about':
          return '4'
        default:
          return '1'
      }
    })

    const handleSelect = (key, keyPath) => {
      console.log(key, keyPath)
    }

    return {
      activeIndex,
      handleSelect
    }
  }
}
</script>

<style>
#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  margin: 0;
  padding: 0;
  height: 100vh;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  background-color: #f5f5f5;
}

.el-header {
  background-color: #fff;
  color: #333;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  padding: 0;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.logo h2 {
  margin: 0;
  color: #409eff;
  font-weight: 600;
}

.el-menu-demo {
  border-bottom: none;
}

.el-main {
  padding: 0;
  background-color: #f5f5f5;
}
</style>
