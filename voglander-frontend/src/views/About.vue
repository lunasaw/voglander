<template>
  <div class="about">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>关于系统</span>
        </div>
      </template>

      <el-descriptions title="系统信息" :column="2">
        <el-descriptions-item label="系统名称">
          Voglander 设备管理系统
        </el-descriptions-item>
        <el-descriptions-item label="版本号">
          v1.0.0
        </el-descriptions-item>
        <el-descriptions-item label="开发框架">
          Vue 3 + Element Plus
        </el-descriptions-item>
        <el-descriptions-item label="后端框架">
          Spring Boot
        </el-descriptions-item>
        <el-descriptions-item label="数据库">
          MySQL + Redis
        </el-descriptions-item>
        <el-descriptions-item label="开发者">
          Voglander Team
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <h3>功能特性</h3>
      <ul>
        <li>设备管理：支持各类视频监控设备的接入和管理</li>
        <li>实时监控：实时查看设备状态和运行情况</li>
        <li>设备控制：支持远程控制设备参数和功能</li>
        <li>报警管理：设备异常和故障报警处理</li>
        <li>用户管理：多用户权限管理系统</li>
        <li>日志管理：完整的操作日志和审计功能</li>
      </ul>

      <el-divider />

      <h3>技术栈</h3>
      <el-row :gutter="20">
        <el-col :span="12">
          <h4>前端技术</h4>
          <el-tag v-for="tech in frontendTech" :key="tech" class="tech-tag">
            {{ tech }}
          </el-tag>
        </el-col>
        <el-col :span="12">
          <h4>后端技术</h4>
          <el-tag v-for="tech in backendTech" :key="tech" class="tech-tag" type="success">
            {{ tech }}
          </el-tag>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'AboutPage',
  setup() {
    const frontendTech = [
      'Vue 3',
      'Element Plus',
      'Vue Router',
      'Pinia',
      'Axios',
      'Vite'
    ]

    const backendTech = [
      'Spring Boot',
      'MyBatis Plus',
      'Redis',
      'MySQL',
      'Docker',
      'Maven'
    ]

    return {
      frontendTech,
      backendTech
    }
  }
}
</script>

<style scoped>
.about {
  padding: 20px;
}

.card-header {
  font-weight: bold;
  font-size: 18px;
}

.tech-tag {
  margin: 5px;
}

ul {
  padding-left: 20px;
}

li {
  margin: 10px 0;
  line-height: 1.6;
}
</style>