package io.github.lunasaw.voglander.manager.service;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.repository.entity.PushProxyDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 推流代理基础服务集成测试
 * 
 * @author luna
 * @since 2025-01-23
 */
@Slf4j
public class PushProxyServiceTest extends BaseTest {

    // 测试数据常量
    private static final String TEST_APP     = "live";
    private static final String TEST_STREAM  = "test";
    private static final String TEST_DST_URL = "rtmp://push.example.com/live/test";

    @Autowired
    private PushProxyService    pushProxyService;

    private PushProxyDO         testDO;

    @BeforeEach
    public void setUp() {
        log.info("开始设置推流代理基础服务测试数据");

        // 清理测试数据
        cleanupTestData();

        // 创建测试DO对象
        testDO = createTestDO();

        log.info("推流代理基础服务测试数据设置完成");
    }

    @AfterEach
    public void tearDown() {
        log.info("开始清理推流代理基础服务测试数据");
        cleanupTestData();
        log.info("推流代理基础服务测试数据清理完成");
    }

    /**
     * 清理测试数据
     */
    private void cleanupTestData() {
        try {
            QueryWrapper<PushProxyDO> wrapper = new QueryWrapper<>();
            wrapper.in("app", TEST_APP, TEST_APP + "2")
                .or().in("stream", TEST_STREAM, TEST_STREAM + "2")
                .or().like("dst_url", "example.com");
            pushProxyService.remove(wrapper);
        } catch (Exception e) {
            log.warn("清理测试数据时发生异常: {}", e.getMessage());
        }
    }

    /**
     * 创建测试DO对象
     */
    private PushProxyDO createTestDO() {
        PushProxyDO pushProxyDO = new PushProxyDO();
        pushProxyDO.setApp(TEST_APP);
        pushProxyDO.setStream(TEST_STREAM);
        pushProxyDO.setDstUrl(TEST_DST_URL);
        pushProxyDO.setSchema("rtmp");
        pushProxyDO.setDescription("测试推流代理");
        pushProxyDO.setStatus(1);
        pushProxyDO.setOnlineStatus(1);
        pushProxyDO.setEnabled(1);
        pushProxyDO.setServerId("zlm-node-1");
        pushProxyDO.setProxyKey("test_proxy_key");
        pushProxyDO.setCreateTime(LocalDateTime.now());
        pushProxyDO.setUpdateTime(LocalDateTime.now());
        return pushProxyDO;
    }

    // ================================
    // 基础CRUD操作测试
    // ================================

    @Test
    public void testSave_Success() {
        // When
        boolean result = pushProxyService.save(testDO);

        // Then
        assertTrue(result);
        assertNotNull(testDO.getId());
        assertTrue(testDO.getId() > 0);

        log.info("保存推流代理测试通过，ID: {}", testDO.getId());
    }

    @Test
    public void testSave_Failed() {
        // Given - 创建一个无效的DO对象（如必填字段为null）
        PushProxyDO invalidDO = new PushProxyDO();
        // 不设置必填字段，预期保存失败

        // When & Then
        assertThrows(Exception.class, () -> pushProxyService.save(invalidDO));

        log.info("保存推流代理失败测试通过");
    }

    @Test
    public void testGetById_Success() {
        // Given
        pushProxyService.save(testDO);
        Long id = testDO.getId();

        // When
        PushProxyDO result = pushProxyService.getById(id);

        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals(TEST_APP, result.getApp());
        assertEquals(TEST_STREAM, result.getStream());

        log.info("根据ID获取推流代理测试通过");
    }

    @Test
    public void testGetById_NotFound() {
        // When
        PushProxyDO result = pushProxyService.getById(999L);

        // Then
        assertNull(result);

        log.info("根据ID获取不存在的推流代理测试通过");
    }

    @Test
    public void testUpdateById_Success() {
        // Given
        pushProxyService.save(testDO);
        testDO.setDescription("更新后的描述");
        testDO.setUpdateTime(LocalDateTime.now());

        // When
        boolean result = pushProxyService.updateById(testDO);

        // Then
        assertTrue(result);

        // 验证更新的数据
        PushProxyDO updated = pushProxyService.getById(testDO.getId());
        assertNotNull(updated);
        assertEquals("更新后的描述", updated.getDescription());

        log.info("根据ID更新推流代理测试通过");
    }

    @Test
    public void testUpdateById_NotFound() {
        // Given
        testDO.setId(999L);

        // When
        boolean result = pushProxyService.updateById(testDO);

        // Then
        assertFalse(result);

        log.info("根据ID更新不存在的推流代理测试通过");
    }

    @Test
    public void testRemoveById_Success() {
        // Given
        pushProxyService.save(testDO);
        Long id = testDO.getId();

        // When
        boolean result = pushProxyService.removeById(id);

        // Then
        assertTrue(result);

        // 验证删除的数据
        PushProxyDO deleted = pushProxyService.getById(id);
        assertNull(deleted);

        log.info("根据ID删除推流代理测试通过");
    }

    @Test
    public void testRemoveById_NotFound() {
        // When
        boolean result = pushProxyService.removeById(999L);

        // Then
        assertFalse(result);

        log.info("根据ID删除不存在的推流代理测试通过");
    }

    @Test
    public void testRemove_Success() {
        // Given
        pushProxyService.save(testDO);

        PushProxyDO testDO2 = createTestDO();
        testDO2.setStream(TEST_STREAM + "2");
        pushProxyService.save(testDO2);

        // When
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getApp, TEST_APP);
        boolean result = pushProxyService.remove(wrapper);

        // Then
        assertTrue(result);

        // 验证删除结果
        long count = pushProxyService.count(wrapper);
        assertEquals(0L, count);

        log.info("条件删除推流代理测试通过");
    }

    // ================================
    // 查询操作测试
    // ================================

    @Test
    public void testGetOne_Success() {
        // Given
        pushProxyService.save(testDO);

        // When
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getApp, TEST_APP)
            .eq(PushProxyDO::getStream, TEST_STREAM);
        PushProxyDO result = pushProxyService.getOne(wrapper);

        // Then
        assertNotNull(result);
        assertEquals(TEST_APP, result.getApp());
        assertEquals(TEST_STREAM, result.getStream());

        log.info("条件查询单个推流代理测试通过");
    }

    @Test
    public void testGetOne_NotFound() {
        // When
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getApp, "nonexistent");
        PushProxyDO result = pushProxyService.getOne(wrapper);

        // Then
        assertNull(result);

        log.info("条件查询不存在的推流代理测试通过");
    }

    @Test
    public void testList_Success() {
        // Given
        pushProxyService.save(testDO);

        PushProxyDO testDO2 = createTestDO();
        testDO2.setStream(TEST_STREAM + "2");
        pushProxyService.save(testDO2);

        // When
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getStatus, 1);
        List<PushProxyDO> result = pushProxyService.list(wrapper);

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= 2);
        assertTrue(result.stream().allMatch(proxy -> proxy.getStatus().equals(1)));

        log.info("条件查询推流代理列表测试通过，记录数: {}", result.size());
    }

    @Test
    public void testList_Empty() {
        // When
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getStatus, 999);
        List<PushProxyDO> result = pushProxyService.list(wrapper);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        log.info("条件查询空推流代理列表测试通过");
    }

    @Test
    public void testPage_Success() {
        // Given
        pushProxyService.save(testDO);

        PushProxyDO testDO2 = createTestDO();
        testDO2.setStream(TEST_STREAM + "2");
        pushProxyService.save(testDO2);

        // When
        Page<PushProxyDO> pageQuery = new Page<>(1, 10);
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getEnabled, 1);
        Page<PushProxyDO> result = pushProxyService.page(pageQuery, wrapper);

        // Then
        assertNotNull(result);
        assertTrue(result.getTotal() >= 2);
        assertTrue(result.getRecords().size() >= 2);

        log.info("分页查询推流代理测试通过，总记录数: {}", result.getTotal());
    }

    @Test
    public void testPage_Empty() {
        // When
        Page<PushProxyDO> pageQuery = new Page<>(1, 10);
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getEnabled, 999);
        Page<PushProxyDO> result = pushProxyService.page(pageQuery, wrapper);

        // Then
        assertNotNull(result);
        assertEquals(0L, result.getTotal());
        assertTrue(result.getRecords().isEmpty());

        log.info("分页查询空推流代理测试通过");
    }

    // ================================
    // 统计操作测试
    // ================================

    @Test
    public void testCount_Success() {
        // Given
        pushProxyService.save(testDO);

        PushProxyDO testDO2 = createTestDO();
        testDO2.setStream(TEST_STREAM + "2");
        pushProxyService.save(testDO2);

        // When
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getStatus, 1);
        long result = pushProxyService.count(wrapper);

        // Then
        assertTrue(result >= 2);

        log.info("统计推流代理数量测试通过，记录数: {}", result);
    }

    @Test
    public void testCount_Zero() {
        // When
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getStatus, 999);
        long result = pushProxyService.count(wrapper);

        // Then
        assertEquals(0L, result);

        log.info("统计零推流代理数量测试通过");
    }

    // ================================
    // 批量操作测试
    // ================================

    @Test
    public void testSaveBatch_Success() {
        // Given
        PushProxyDO testDO2 = createTestDO();
        testDO2.setStream(TEST_STREAM + "2");
        List<PushProxyDO> doList = Arrays.asList(testDO, testDO2);

        log.info("Before save - testDO.getId(): {}, testDO2.getId(): {}", testDO.getId(), testDO2.getId());

        // When
        boolean result = pushProxyService.saveBatch(doList);

        log.info("Save result: {}", result);
        log.info("After save - testDO.getId(): {}, testDO2.getId(): {}", testDO.getId(), testDO2.getId());

        // Then
        assertTrue(result);

        // 验证数据已保存到数据库 - 查询数据库确认
        PushProxyDO savedDO1 = pushProxyService.lambdaQuery()
            .eq(PushProxyDO::getApp, TEST_APP)
            .eq(PushProxyDO::getStream, TEST_STREAM)
            .one();
        PushProxyDO savedDO2 = pushProxyService.lambdaQuery()
            .eq(PushProxyDO::getApp, TEST_APP)
            .eq(PushProxyDO::getStream, TEST_STREAM + "2")
            .one();

        assertNotNull(savedDO1, "第一个对象应该已保存到数据库");
        assertNotNull(savedDO2, "第二个对象应该已保存到数据库");
        assertNotNull(savedDO1.getId(), "第一个对象的ID应该被设置");
        assertNotNull(savedDO2.getId(), "第二个对象的ID应该被设置");

        log.info("批量保存推流代理测试通过");
    }

    @Test
    public void testSaveBatch_Empty() {
        // When
        boolean result = pushProxyService.saveBatch(Arrays.asList());

        // Then
        assertFalse(result); // MyBatis Plus空列表保存返回false

        log.info("批量保存空推流代理列表测试通过");
    }

    @Test
    public void testUpdateBatchById_Success() {
        // Given
        pushProxyService.save(testDO);
        testDO.setDescription("批量更新的描述");
        List<PushProxyDO> doList = Arrays.asList(testDO);

        // When
        boolean result = pushProxyService.updateBatchById(doList);

        // Then
        assertTrue(result);

        // 验证更新
        PushProxyDO updated = pushProxyService.getById(testDO.getId());
        assertEquals("批量更新的描述", updated.getDescription());

        log.info("批量更新推流代理测试通过");
    }

    @Test
    public void testRemoveByIds_Success() {
        // Given
        pushProxyService.save(testDO);

        PushProxyDO testDO2 = createTestDO();
        testDO2.setStream(TEST_STREAM + "2");
        pushProxyService.save(testDO2);

        List<Long> ids = Arrays.asList(testDO.getId(), testDO2.getId());

        // When
        boolean result = pushProxyService.removeByIds(ids);

        // Then
        assertTrue(result);

        // 验证删除结果
        assertNull(pushProxyService.getById(testDO.getId()));
        assertNull(pushProxyService.getById(testDO2.getId()));

        log.info("批量删除推流代理测试通过");
    }

    // ================================
    // 异常场景测试
    // ================================

    @Test
    public void testNullParameterValidation() {
        // 测试null参数处理 - MyBatis Plus可能不对所有null参数抛异常

        // save(null) 应该抛异常
        assertThrows(Exception.class, () -> pushProxyService.save(null));

        // updateById(null) 可能返回false而不是抛异常
        try {
            boolean result = pushProxyService.updateById(null);
            assertFalse(result);
        } catch (Exception e) {
            // 也接受抛异常的情况
            log.info("updateById(null)抛出异常: {}", e.getMessage());
        }

        // getById(null) 可能返回null而不是抛异常
        try {
            PushProxyDO result = pushProxyService.getById(null);
            assertNull(result);
        } catch (Exception e) {
            // 也接受抛异常的情况
            log.info("getById(null)抛出异常: {}", e.getMessage());
        }

        // removeById(null) 可能返回false而不是抛异常
        try {
            boolean result = pushProxyService.removeById(null);
            assertFalse(result);
        } catch (Exception e) {
            // 也接受抛异常的情况
            log.info("removeById(null)抛出异常: {}", e.getMessage());
        }

        log.info("null参数校验测试通过");
    }
}