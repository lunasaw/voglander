<template>
  <div id="app">
    <el-container class="layout-container">
      <!-- 侧边栏 -->
      <el-aside :width="sidebarCollapsed ? '64px' : '240px'" class="sidebar">
        <div class="sidebar-header">
          <div class="logo" @click="$router.push('/')">
            <img src="@/assets/logo.svg" alt="Voglander" class="logo-img"/>
            <span v-show="!sidebarCollapsed" class="logo-text">Voglander</span>
          </div>
        </div>

        <el-menu
            :default-active="activeIndex"
            class="sidebar-menu"
            :collapse="sidebarCollapsed"
            unique-opened
            router
            background-color="#ffffff"
            text-color="#606266"
            active-text-color="#667eea"
            @select="handleMenuSelect"
        >
          <el-menu-item index="/">
            <el-icon>
              <House/>
            </el-icon>
            <template #title>首页概览</template>
          </el-menu-item>

          <el-sub-menu index="device">
            <template #title>
              <el-icon><Monitor /></el-icon>
              <span>设备管理</span>
            </template>
            <el-menu-item index="/device">
              <el-icon>
                <List/>
              </el-icon>
              <template #title>设备列表</template>
            </el-menu-item>
            <el-menu-item index="/device/channel">
              <el-icon>
                <VideoCamera/>
              </el-icon>
              <template #title>通道管理</template>
            </el-menu-item>
            <el-menu-item index="/device/config">
              <el-icon>
                <Setting/>
              </el-icon>
              <template #title>设备配置</template>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="monitor">
            <template #title>
              <el-icon>
                <View/>
              </el-icon>
              <span>监控中心</span>
            </template>
            <el-menu-item index="/monitor/live">
              <el-icon>
                <VideoPlay/>
              </el-icon>
              <template #title>实时监控</template>
            </el-menu-item>
            <el-menu-item index="/monitor/playback">
              <el-icon>
                <VideoCamera/>
              </el-icon>
              <template #title>录像回放</template>
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item index="/api-test">
            <el-icon>
              <Tools/>
            </el-icon>
            <template #title>API测试</template>
          </el-menu-item>

          <el-menu-item index="/about">
            <el-icon>
              <InfoFilled/>
            </el-icon>
            <template #title>关于系统</template>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 主内容区域 -->
      <el-container class="main-container">
        <!-- 顶部导航 -->
        <el-header class="header">
          <div class="header-left">
            <el-button
                type="text"
                @click="toggleSidebar"
                class="sidebar-toggle"
            >
              <el-icon size="18">
                <Menu/>
              </el-icon>
            </el-button>

            <!-- 面包屑导航 -->
            <el-breadcrumb separator="/" class="breadcrumb">
              <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item v-for="breadcrumb in breadcrumbs" :key="breadcrumb.path"
                                  :to="{ path: breadcrumb.path }">
                {{ breadcrumb.name }}
              </el-breadcrumb-item>
            </el-breadcrumb>
          </div>

          <div class="header-right">
            <!-- 全屏按钮 -->
            <el-tooltip content="全屏显示">
              <el-button type="text" @click="toggleFullscreen" class="header-action">
                <el-icon size="18">
                  <FullScreen/>
                </el-icon>
              </el-button>
            </el-tooltip>

            <!-- 刷新按钮 -->
            <el-tooltip content="刷新页面">
              <el-button type="text" @click="refreshPage" class="header-action">
                <el-icon size="18">
                  <Refresh/>
                </el-icon>
              </el-button>
            </el-tooltip>

            <!-- 用户信息 -->
            <el-dropdown>
              <span class="user-info">
                <el-avatar size="small" :src="userAvatar">{{ userName }}</el-avatar>
                <span class="user-name">{{ userName }}</span>
                <el-icon><ArrowDown/></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item>个人中心</el-dropdown-item>
                  <el-dropdown-item>系统设置</el-dropdown-item>
                  <el-dropdown-item divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <!-- 主内容 -->
        <el-main class="main-content">
          <div class="content-wrapper">
            <router-view/>
          </div>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import {ref, computed} from 'vue'
import { useRoute } from 'vue-router'
import {
  House, Monitor, Tools, InfoFilled, Menu, FullScreen, Refresh, ArrowDown,
  List, VideoCamera, Setting, View, VideoPlay
} from '@element-plus/icons-vue'

export default {
  name: 'App',
  components: {
    House, Monitor, Tools, InfoFilled, Menu, FullScreen, Refresh, ArrowDown,
    List, VideoCamera, Setting, View, VideoPlay
  },
  setup() {
    const route = useRoute()

    const sidebarCollapsed = ref(false)
    const userName = ref('管理员')
    const userAvatar = ref('')

    const activeIndex = computed(() => {
      return route.path
    })

    // 面包屑导航
    const breadcrumbs = computed(() => {
      const pathArray = route.path.split('/').filter(path => path)
      const breadcrumbMap = {
        'device': {name: '设备管理'},
        'channel': {name: '通道管理'},
        'config': {name: '设备配置'},
        'monitor': {name: '监控中心'},
        'live': {name: '实时监控'},
        'playback': {name: '录像回放'},
        'api-test': {name: 'API测试'},
        'about': {name: '关于系统'}
      }

      return pathArray.map((path, index) => {
        const fullPath = '/' + pathArray.slice(0, index + 1).join('/')
        return {
          path: fullPath,
          name: breadcrumbMap[path]?.name || path
        }
      }).filter(item => item.name !== route.path.slice(1))
    })

    const toggleSidebar = () => {
      sidebarCollapsed.value = !sidebarCollapsed.value
    }

    const handleMenuSelect = (index) => {
      console.log('Selected menu:', index)
    }

    const toggleFullscreen = () => {
      if (!document.fullscreenElement) {
        document.documentElement.requestFullscreen()
      } else {
        if (document.exitFullscreen) {
          document.exitFullscreen()
        }
      }
    }

    const refreshPage = () => {
      window.location.reload()
    }

    return {
      sidebarCollapsed,
      userName,
      userAvatar,
      activeIndex,
      breadcrumbs,
      toggleSidebar,
      handleMenuSelect,
      toggleFullscreen,
      refreshPage
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
  overflow: hidden;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  background-color: #f0f2f5;
}

.layout-container {
  height: 100vh;
}

/* 侧边栏样式 */
.sidebar {
  background: #ffffff;
  transition: width 0.2s ease;
  overflow: hidden;
  border-right: 1px solid #ebeef5;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.06);
}

.sidebar-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #ebeef5;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.logo {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: white;
  text-decoration: none;
  padding: 0 16px;
}

.logo-img {
  height: 32px;
  width: 32px;
  margin-right: 12px;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: white;
}

.sidebar-menu {
  border: none;
  height: calc(100vh - 64px);
  overflow-y: auto;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 240px;
}

/* 菜单项悬停效果 */
.sidebar-menu :deep(.el-menu-item:hover) {
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  color: #667eea;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.15) 0%, rgba(118, 75, 162, 0.15) 100%);
  border-right: 3px solid #667eea;
}

.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  color: #667eea;
}

/* 顶部导航样式 */
.header {
  background: white;
  border-bottom: 1px solid #e8eaec;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 64px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
}

.sidebar-toggle {
  margin-right: 16px;
  color: #666;
}

.sidebar-toggle:hover {
  color: #1890ff;
}

.breadcrumb {
  font-size: 14px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-action {
  color: #666;
  padding: 8px;
}

.header-action:hover {
  color: #1890ff;
  background: #f0f8ff;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 6px;
  transition: background-color 0.2s;
}

.user-info:hover {
  background: #f0f8ff;
}

.user-name {
  font-size: 14px;
  color: #666;
}

/* 主内容区域 */
.main-container {
  overflow: hidden;
}

.main-content {
  background: #f0f2f5;
  padding: 0;
  height: calc(100vh - 64px);
  overflow-y: auto;
}

.content-wrapper {
  padding: 24px;
  height: 100%;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header {
    padding: 0 16px;
  }

  .content-wrapper {
    padding: 16px;
  }

  .user-name {
    display: none;
  }
}
</style>
