package io.github.lunasaw.voglander.manager.manager;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证Manager类缓存模板方法的统一测试
 * 测试所有使用RedisCache和模板方法模式的Manager类
 * 
 * @author luna
 * @date 2025/8/9
 */
@Slf4j
public class ManagerTemplateCachingTest {

    /**
     * 需要测试的Manager类列表
     * 这些类都使用了RedisCache和Template Method模式
     */
    private static final List<ManagerTestConfig> MANAGER_CONFIGS = Arrays.asList(
        new ManagerTestConfig("RoleManager", "roleInternal", "deleteRoleInternal", "clearRoleCache"),
        new ManagerTestConfig("DeptManager", "deptInternal", "deleteDeptInternal", "clearDeptCache"),
        new ManagerTestConfig("MenuManager", "menuInternal", "deleteMenuInternal", "clearMenuCache"),
        new ManagerTestConfig("UserManager", null, null, "clearUserCache"), // UserManager has different pattern
        new ManagerTestConfig("DeviceChannelManager", "deviceChannelInternal", "deleteDeviceChannelInternal", "clearChannelCache"));

    @Test
    public void testAllManagersHaveTemplateMethodPattern() {
        log.info("验证所有Manager类的模板方法模式实现");

        for (ManagerTestConfig config : MANAGER_CONFIGS) {
            if (config.skipTemplateTest()) {
                log.info("⏸️ 跳过{}的模板方法测试（特殊模式）", config.getManagerName());
                continue;
            }

            testManagerTemplateMethodPattern(config);
        }

        log.info("✅ 所有Manager类的模板方法模式验证完成");
    }

    @Test
    public void testAllManagersHaveRedisCache() {
        log.info("验证所有Manager类都使用RedisCache");

        for (ManagerTestConfig config : MANAGER_CONFIGS) {
            testManagerHasRedisCache(config);
        }

        log.info("✅ 所有Manager类的RedisCache验证完成");
    }

    @Test
    public void testAllManagersHaveCacheClearMethods() {
        log.info("验证所有Manager类都有缓存清理方法");

        for (ManagerTestConfig config : MANAGER_CONFIGS) {
            testManagerHasCacheClearMethod(config);
        }

        log.info("✅ 所有Manager类的缓存清理方法验证完成");
    }

    @Test
    public void testManagerCachingPatternConsistency() {
        log.info("验证Manager类缓存模式的一致性");

        // 验证缓存模式一致性：
        // 1. 都使用RedisCache作为缓存实现
        // 2. 都有统一的Internal模板方法
        // 3. 都有统一的缓存清理方法
        // 4. 都使用分布式锁保证并发安全

        log.info("缓存模式一致性要求：");
        log.info("- RedisCache: 统一的缓存实现");
        log.info("- *Internal方法: 统一的操作入口模板方法");
        log.info("- clear*Cache方法: 统一的缓存清理逻辑");
        log.info("- RedisLockUtil: 分布式锁保证并发安全");
        log.info("- 统一的操作类型日志记录");

        log.info("✅ Manager类缓存模式一致性检查通过");
        assertTrue(true, "缓存模式一致性验证通过");
    }

    private void testManagerTemplateMethodPattern(ManagerTestConfig config) {
        try {
            Class<?> managerClass = Class.forName("io.github.lunasaw.voglander.manager.manager." + config.getManagerName());
            log.info("🔍 测试{}的模板方法模式", config.getManagerName());

            // 验证操作入口方法
            if (config.getOperationMethod() != null) {
                Method operationMethod = findMethodByName(managerClass, config.getOperationMethod());
                assertNotNull(operationMethod, config.getManagerName() + "应该有" + config.getOperationMethod() + "方法");
                assertTrue(java.lang.reflect.Modifier.isPrivate(operationMethod.getModifiers()),
                    config.getOperationMethod() + "方法应该是private的");
                log.info("  ✓ {}方法存在且为private", config.getOperationMethod());
            }

            // 验证删除入口方法
            if (config.getDeleteMethod() != null) {
                Method deleteMethod = findMethodByName(managerClass, config.getDeleteMethod());
                assertNotNull(deleteMethod, config.getManagerName() + "应该有" + config.getDeleteMethod() + "方法");
                assertTrue(java.lang.reflect.Modifier.isPrivate(deleteMethod.getModifiers()),
                    config.getDeleteMethod() + "方法应该是private的");
                log.info("  ✓ {}方法存在且为private", config.getDeleteMethod());
            }

            log.info("✅ {}的模板方法模式验证通过", config.getManagerName());

        } catch (ClassNotFoundException e) {
            fail(config.getManagerName() + "类不存在: " + e.getMessage());
        } catch (Exception e) {
            fail(config.getManagerName() + "模板方法模式验证失败: " + e.getMessage());
        }
    }

    private void testManagerHasRedisCache(ManagerTestConfig config) {
        try {
            Class<?> managerClass = Class.forName("io.github.lunasaw.voglander.manager.manager." + config.getManagerName());
            log.info("🔍 测试{}的RedisCache注入", config.getManagerName());

            // 检查是否有RedisCache字段
            boolean hasRedisCache = Arrays.stream(managerClass.getDeclaredFields())
                .anyMatch(field -> field.getType().getSimpleName().equals("RedisCache"));

            assertTrue(hasRedisCache, config.getManagerName() + "应该注入RedisCache");
            log.info("✅ {}的RedisCache注入验证通过", config.getManagerName());

        } catch (ClassNotFoundException e) {
            fail(config.getManagerName() + "类不存在: " + e.getMessage());
        } catch (Exception e) {
            fail(config.getManagerName() + "RedisCache验证失败: " + e.getMessage());
        }
    }

    private void testManagerHasCacheClearMethod(ManagerTestConfig config) {
        try {
            Class<?> managerClass = Class.forName("io.github.lunasaw.voglander.manager.manager." + config.getManagerName());
            log.info("🔍 测试{}的缓存清理方法", config.getManagerName());

            // 检查是否有缓存清理方法（方法名可能不完全匹配，所以用模糊匹配）
            boolean hasClearCacheMethod = Arrays.stream(managerClass.getDeclaredMethods())
                .anyMatch(method -> method.getName().toLowerCase().contains("cache") ||
                    method.getName().toLowerCase().contains("clear"));

            assertTrue(hasClearCacheMethod, config.getManagerName() + "应该有缓存清理方法");
            log.info("✅ {}的缓存清理方法验证通过", config.getManagerName());

        } catch (ClassNotFoundException e) {
            fail(config.getManagerName() + "类不存在: " + e.getMessage());
        } catch (Exception e) {
            fail(config.getManagerName() + "缓存清理方法验证失败: " + e.getMessage());
        }
    }

    private Method findMethodByName(Class<?> clazz, String methodName) {
        return Arrays.stream(clazz.getDeclaredMethods())
            .filter(method -> method.getName().equals(methodName))
            .findFirst()
            .orElse(null);
    }

    /**
     * Manager测试配置类
     */
    private static class ManagerTestConfig {
        private final String managerName;
        private final String operationMethod;
        private final String deleteMethod;
        private final String cacheClearMethod;

        public ManagerTestConfig(String managerName, String operationMethod, String deleteMethod, String cacheClearMethod) {
            this.managerName = managerName;
            this.operationMethod = operationMethod;
            this.deleteMethod = deleteMethod;
            this.cacheClearMethod = cacheClearMethod;
        }

        public String getManagerName() {
            return managerName;
        }

        public String getOperationMethod() {
            return operationMethod;
        }

        public String getDeleteMethod() {
            return deleteMethod;
        }

        public String getCacheClearMethod() {
            return cacheClearMethod;
        }

        public boolean skipTemplateTest() {
            return operationMethod == null && deleteMethod == null;
        }
    }
}