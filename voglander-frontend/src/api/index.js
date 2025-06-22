import axios from 'axios'

// 创建axios实例
const api = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL || 'http://localhost:8081',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 可以在这里添加认证token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    // 处理错误响应
    if (error.response) {
      switch (error.response.status) {
        case 401:
          // 未授权，跳转到登录页
          localStorage.removeItem('token')
          window.location.href = '/login'
          break
        case 403:
          console.error('禁止访问')
          break
        case 500:
          console.error('服务器错误')
          break
        default:
          console.error('请求错误', error.response.data)
      }
    }
    return Promise.reject(error)
  }
)

// 设备管理API接口定义 - 对应DeviceController的所有接口
export const deviceApi = {
  // 根据ID获取设备详情
  getById: (id) => api.get(`/api/v1/device/get/${id}`),

  // 根据设备实体获取单个设备
  getByEntity: (device) => api.get('/api/v1/device/get', { params: device }),

  // 获取设备列表
  getList: (device = {}) => api.get('/api/v1/device/list', { params: device }),

  // 分页查询设备（带条件）
  getPageListByEntity: (page, size, device = {}) =>
    api.get(`/api/v1/device/pageListByEntity/${page}/${size}`, { params: device }),

  // 分页查询设备
  getPageList: (page, size) => api.get(`/api/v1/device/pageList/${page}/${size}`),

  // 插入单个设备
  insert: (device) => api.post('/api/v1/device/insert', device),

  // 批量插入设备
  insertBatch: (deviceList) => api.post('/api/v1/device/insertBatch', deviceList),

  // 更新设备
  update: (device) => api.put('/api/v1/device/update', device),

  // 批量更新设备
  updateBatch: (deviceList) => api.put('/api/v1/device/updateBatch', deviceList),

  // 根据ID删除单个设备
  deleteById: (id) => api.delete(`/api/v1/device/delete/${id}`),

  // 根据设备实体删除设备
  deleteByEntity: (device) => api.delete('/api/v1/device/deleteByEntity', { data: device }),

  // 批量删除设备
  deleteBatch: (ids) => api.delete('/api/v1/device/deleteIds', {data: ids}),

  // 获取设备总数
  getCount: () => api.get('/api/v1/device/count'),

  // 根据条件获取设备总数
  getCountByEntity: (device) => api.get('/api/v1/device/countByEntity', { params: device })
}

export const userApi = {
  // 用户登录
  login: (data) => api.post('/api/login', data),

  // 用户注册
  register: (data) => api.post('/api/register', data),

  // 获取用户信息
  getUserInfo: () => api.get('/api/user/info')
}

export default api