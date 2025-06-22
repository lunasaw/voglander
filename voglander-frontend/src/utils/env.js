// 环境变量配置
export const ENV = {
  // API基础地址
  API_BASE_URL: process.env.VUE_APP_API_BASE_URL || 'http://localhost:8087',

  // 应用标题
  APP_TITLE: process.env.VUE_APP_TITLE || 'Voglander 设备管理系统',

  // 是否为开发环境
  IS_DEV: process.env.NODE_ENV === 'development',

  // 是否为生产环境
  IS_PROD: process.env.NODE_ENV === 'production'
}