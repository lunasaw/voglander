# Voglander Frontend

这是 Voglander 设备管理系统的前端项目，基于 Vue 3 + Element Plus 构建。

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Element Plus** - 基于 Vue 3 的组件库
- **Vue Router 4** - Vue.js 官方路由管理器
- **Pinia** - Vue 的状态管理库
- **Axios** - HTTP 客户端
- **Vite** - 下一代前端构建工具

## 项目结构

```
src/
├── api/              # API接口定义
│   └── index.js      # axios配置和API接口
├── assets/           # 静态资源
├── components/       # 通用组件
├── router/           # 路由配置
│   └── index.js      # 路由定义
├── stores/           # 状态管理
│   └── index.js      # Pinia stores
├── utils/            # 工具函数
│   └── env.js        # 环境配置
├── views/            # 页面组件
│   ├── Home.vue      # 首页
│   ├── Device.vue    # 设备管理
│   └── About.vue     # 关于页面
├── App.vue           # 根组件
└── main.js           # 入口文件
```

## 功能特性

### 📊 仪表盘
- 设备统计概览
- 实时状态监控
- 快速操作入口

### 🔧 设备管理
- 设备列表查看
- 设备信息编辑
- 设备状态管理
- 分页和搜索功能

### 🎨 界面特性
- 响应式设计
- 现代化UI界面
- 完整的表单验证
- 友好的用户交互

## 开发指南

### 环境要求
- Node.js >= 12.0.0
- npm >= 6.0.0

### 安装依赖
```bash
npm install
```

### 启动开发服务器
```bash
npm run serve
```

### 构建生产版本
```bash
npm run build
```

### 代码检查
```bash
npm run lint
```

## 环境配置

项目支持多环境配置，可以通过环境变量文件进行配置：

- `.env.development` - 开发环境配置
- `.env.production` - 生产环境配置

### 环境变量说明

```bash
# API基础地址
VUE_APP_API_BASE_URL=http://localhost:8080

# 应用标题
VUE_APP_TITLE=Voglander 设备管理系统
```

## API 接口

项目中的 API 接口定义在 `src/api/index.js` 文件中，主要包括：

### 设备相关接口
- `GET /api/device` - 获取设备列表
- `GET /api/device/:id` - 获取设备详情
- `POST /api/device` - 创建设备
- `PUT /api/device/:id` - 更新设备
- `DELETE /api/device/:id` - 删除设备

### 用户相关接口
- `POST /api/login` - 用户登录
- `POST /api/register` - 用户注册
- `GET /api/user/info` - 获取用户信息

## 状态管理

使用 Pinia 进行状态管理，主要包括：

- `useUserStore` - 用户状态管理
- `useDeviceStore` - 设备状态管理

## 路由配置

项目使用 Vue Router 4 进行路由管理：

- `/` - 首页仪表盘
- `/device` - 设备管理页面
- `/about` - 关于页面

## 开发规范

### 代码风格
- 使用 ES6+ 语法
- 组件使用 Composition API
- 遵循 Vue 3 最佳实践

### 组件命名
- 组件文件使用 PascalCase 命名
- 组件名与文件名保持一致
- Props 使用 camelCase

### 提交规范
- feat: 新功能
- fix: 修复问题
- docs: 文档更新
- style: 代码格式调整
- refactor: 代码重构
- test: 测试相关
- chore: 构建或工具相关

## 部署说明

### 开发环境部署
```bash
npm run serve
```
访问地址：http://localhost:8086

### 生产环境构建
```bash
npm run build
```
构建产物在 `dist` 目录下

### Docker 部署
```dockerfile
# 使用 nginx 作为基础镜像
FROM nginx:alpine

# 复制构建产物
COPY dist/ /usr/share/nginx/html/

# 复制 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
```

## 常见问题

### Q: 开发服务器启动失败？
A: 请检查 Node.js 版本是否符合要求，并确保依赖安装完整。

### Q: API 请求失败？
A: 请检查后端服务是否启动，以及 API 基础地址配置是否正确。

### Q: 页面样式异常？
A: 请确保 Element Plus 样式文件正确引入。

## 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 许可证

本项目采用 MIT 许可证，详见 [LICENSE](LICENSE) 文件。