# ZLM集成模块使用说明

## 新架构概述

为了解决集群环境中节点同步问题，我们重构了ZLM节点管理架构，采用NodeSupplier机制：

### 原有架构问题

- 手动维护节点状态复杂
- Spring事件在集群环境中无法跨节点同步
- 节点变更需要主动推送到LoadBalancer

### 新架构优势

- LoadBalancer主动从NodeSupplier获取最新节点列表
- 支持集群环境，每个节点都可以从数据库获取最新状态
- 简化了节点维护逻辑
- 支持动态刷新，无需重启

## 核心组件

### 1. NodeSupplier接口

```java
public interface NodeSupplier {
    List<ZlmNode> getNodes();

    default String getName() {
        return this.getClass().getSimpleName();
    }
}
```

### 2. VoglanderNodeSupplier实现

- 从数据库动态获取启用的媒体节点
- 自动转换MediaNodeDTO为ZlmNode
- 支持异常处理和日志记录

### 3. LoadBalancer增强

- 支持setNodeSupplier()设置节点提供器
- 支持refreshNodes()主动刷新节点列表
- selectNode()时自动刷新确保使用最新节点

## 使用方式

### 1. 自动配置（推荐）

在application.yml中启用ZLM：

```yaml
zlm:
  enable: true
  balance: RANDOM  # 或其他负载均衡策略
```

系统会自动：

- 创建LoadBalancer bean
- 创建VoglanderNodeSupplier bean
- 将NodeSupplier注入到LoadBalancer中

### 2. 手动配置

如果需要自定义NodeSupplier：

```java

@Component
public class CustomNodeSupplier implements NodeSupplier {
    @Override
    public List<ZlmNode> getNodes() {
        // 自定义获取节点逻辑
        return customNodes;
    }
}
```

### 3. 节点管理

现在不需要手动调用addNode/removeNode，LoadBalancer会：

- 在selectNode时自动刷新节点列表
- 确保始终使用数据库中的最新节点状态

## 集群支持

新架构完美支持集群环境：

- 每个节点的LoadBalancer都从同一个数据库获取节点列表
- 节点状态变更会在下次请求时自动同步
- 无需复杂的集群间通信机制

## 性能考虑

- refreshNodes()调用频率：每次selectNode时调用
- 数据库查询优化：建议在MediaNodeManager中添加缓存
- 异常处理：NodeSupplier异常不会影响系统运行

## 迁移指南

从旧架构迁移到新架构：

1. 删除手动的addNode/removeNode调用
2. 移除复杂的事件监听逻辑
3. 启用zlm.enable=true配置
4. 验证VoglanderNodeSupplier正常工作

## 注意事项

1. 确保数据库中的media_node表数据准确
2. 建议为MediaNodeManager.getEnabledNodes()添加缓存
3. 监控LoadBalancer的refreshNodes日志确保正常工作
4. 对于高并发场景，考虑增加节点查询缓存时间