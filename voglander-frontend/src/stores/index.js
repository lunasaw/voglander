import { defineStore } from 'pinia'

// 用户状态管理
export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    isAuthenticated: false
  }),

  getters: {
    getUserInfo: (state) => state.user,
    isLoggedIn: (state) => state.isAuthenticated
  },

  actions: {
    setUser(user) {
      this.user = user
      this.isAuthenticated = true
    },

    logout() {
      this.user = null
      this.isAuthenticated = false
    }
  }
})

// 设备状态管理
export const useDeviceStore = defineStore('device', {
  state: () => ({
    devices: [],
    selectedDevice: null,
    loading: false
  }),

  getters: {
    getDevices: (state) => state.devices,
    getSelectedDevice: (state) => state.selectedDevice
  },

  actions: {
    setDevices(devices) {
      this.devices = devices
    },

    selectDevice(device) {
      this.selectedDevice = device
    },

    setLoading(loading) {
      this.loading = loading
    }
  }
})