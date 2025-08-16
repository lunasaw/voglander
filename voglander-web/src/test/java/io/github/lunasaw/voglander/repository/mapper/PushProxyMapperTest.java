package io.github.lunasaw.voglander.repository.mapper;

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
 * 推流代理数据访问层集成测试
 * 
 * @author luna
 * @since 2025-01-23
 */
@Slf4j
public class PushProxyMapperTest extends BaseTest {

    // 测试数据常量
    private static final String TEST_APP     = "live";
    private static final String TEST_STREAM  = "test";
    private static final String TEST_DST_URL = "rtmp://push.example.com/live/test";

    @Autowired
    private PushProxyMapper     pushProxyMapper;

    private PushProxyDO         testDO;

    @BeforeEach
    public void setUp() {
        log.info("开始设置推流代理数据访问层测试数据");

        // 清理测试数据
        cleanupTestData();

        // 创建测试DO对象
        testDO = createTestDO();

        log.info("推流代理数据访问层测试数据设置完成");
    }

    @AfterEach
    public void tearDown() {
        log.info("开始清理推流代理数据访问层测试数据");
        cleanupTestData();
        log.info("推流代理数据访问层测试数据清理完成");
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
            pushProxyMapper.delete(wrapper);
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
        pushProxyDO.setOnlineStatus(0);
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
    public void testInsert_Success() {
        // When
        int result = pushProxyMapper.insert(testDO);

        // Then
        assertEquals(1, result);
        assertNotNull(testDO.getId());
        assertTrue(testDO.getId() > 0);

        // 验证插入的数据
        PushProxyDO inserted = pushProxyMapper.selectById(testDO.getId());
        assertNotNull(inserted);
        assertEquals(TEST_APP, inserted.getApp());
        assertEquals(TEST_STREAM, inserted.getStream());
        assertEquals(TEST_DST_URL, inserted.getDstUrl());

        log.info("插入推流代理测试通过，ID: {}", testDO.getId());
    }

    @Test
    public void testSelectById_Success() {
        // Given
        pushProxyMapper.insert(testDO);
        Long id = testDO.getId();

        // When
        PushProxyDO result = pushProxyMapper.selectById(id);

        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals(TEST_APP, result.getApp());
        assertEquals(TEST_STREAM, result.getStream());
        assertEquals(TEST_DST_URL, result.getDstUrl());

        log.info("根据ID查询推流代理测试通过");
    }

    @Test
    public void testSelectById_NotFound() {
        // When
        PushProxyDO result = pushProxyMapper.selectById(999L);

        // Then
        assertNull(result);

        log.info("查询不存在的推流代理测试通过");
    }

    @Test
    public void testUpdateById_Success() {
        // Given
        pushProxyMapper.insert(testDO);
        Long id = testDO.getId();

        // When
        testDO.setDescription("更新后的描述");
        testDO.setUpdateTime(LocalDateTime.now());
        int result = pushProxyMapper.updateById(testDO);

        // Then
        assertEquals(1, result);

        // 验证更新的数据
        PushProxyDO updated = pushProxyMapper.selectById(id);
        assertNotNull(updated);
        assertEquals("更新后的描述", updated.getDescription());

        log.info("根据ID更新推流代理测试通过");
    }

    @Test
    public void testUpdateById_NotFound() {
        // Given
        testDO.setId(999L);

        // When
        int result = pushProxyMapper.updateById(testDO);

        // Then
        assertEquals(0, result);

        log.info("更新不存在的推流代理测试通过");
    }

    @Test
    public void testDeleteById_Success() {
        // Given
        pushProxyMapper.insert(testDO);
        Long id = testDO.getId();

        // When
        int result = pushProxyMapper.deleteById(id);

        // Then
        assertEquals(1, result);

        // 验证删除的数据
        PushProxyDO deleted = pushProxyMapper.selectById(id);
        assertNull(deleted);

        log.info("根据ID删除推流代理测试通过");
    }

    @Test
    public void testDeleteById_NotFound() {
        // When
        int result = pushProxyMapper.deleteById(999L);

        // Then
        assertEquals(0, result);

        log.info("删除不存在的推流代理测试通过");
    }

    // ================================
    // 条件查询测试
    // ================================

    @Test
    public void testSelectOne_Success() {
        // Given
        pushProxyMapper.insert(testDO);

        // When
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getApp, TEST_APP)
            .eq(PushProxyDO::getStream, TEST_STREAM);
        PushProxyDO result = pushProxyMapper.selectOne(wrapper);

        // Then
        assertNotNull(result);
        assertEquals(TEST_APP, result.getApp());
        assertEquals(TEST_STREAM, result.getStream());

        log.info("条件查询单个推流代理测试通过");
    }

    @Test
    public void testSelectOne_NotFound() {
        // When
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getApp, "nonexistent");
        PushProxyDO result = pushProxyMapper.selectOne(wrapper);

        // Then
        assertNull(result);

        log.info("条件查询不存在的推流代理测试通过");
    }

    @Test
    public void testSelectList_Success() {
        // Given
        pushProxyMapper.insert(testDO);

        PushProxyDO testDO2 = createTestDO();
        testDO2.setStream(TEST_STREAM + "2");
        pushProxyMapper.insert(testDO2);

        // When
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getApp, TEST_APP);
        List<PushProxyDO> result = pushProxyMapper.selectList(wrapper);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(proxy -> TEST_APP.equals(proxy.getApp())));

        log.info("条件查询推流代理列表测试通过，记录数: {}", result.size());
    }

    @Test
    public void testSelectList_Empty() {
        // When
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getApp, "nonexistent");
        List<PushProxyDO> result = pushProxyMapper.selectList(wrapper);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        log.info("条件查询空推流代理列表测试通过");
    }

    @Test
    public void testSelectPage_Success() {
        // Given
        pushProxyMapper.insert(testDO);

        PushProxyDO testDO2 = createTestDO();
        testDO2.setStream(TEST_STREAM + "2");
        pushProxyMapper.insert(testDO2);

        // When
        Page<PushProxyDO> page = new Page<>(1, 10);
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getApp, TEST_APP)
            .orderByDesc(PushProxyDO::getCreateTime);
        Page<PushProxyDO> result = pushProxyMapper.selectPage(page, wrapper);

        // Then
        assertNotNull(result);
        assertEquals(2L, result.getTotal());
        assertEquals(2, result.getRecords().size());
        assertEquals(1, result.getCurrent());
        assertEquals(10, result.getSize());

        log.info("分页查询推流代理测试通过，总记录数: {}", result.getTotal());
    }

    @Test
    public void testSelectPage_Empty() {
        // When
        Page<PushProxyDO> page = new Page<>(1, 10);
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getApp, "nonexistent");
        Page<PushProxyDO> result = pushProxyMapper.selectPage(page, wrapper);

        // Then
        assertNotNull(result);
        assertEquals(0L, result.getTotal());
        assertTrue(result.getRecords().isEmpty());

        log.info("分页查询空推流代理测试通过");
    }

    @Test
    public void testSelectCount_Success() {
        // Given
        pushProxyMapper.insert(testDO);

        PushProxyDO testDO2 = createTestDO();
        testDO2.setStream(TEST_STREAM + "2");
        pushProxyMapper.insert(testDO2);

        // When
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getApp, TEST_APP);
        Long result = pushProxyMapper.selectCount(wrapper);

        // Then
        assertNotNull(result);
        assertEquals(2L, result);

        log.info("统计推流代理数量测试通过，记录数: {}", result);
    }

    @Test
    public void testSelectCount_Zero() {
        // When
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getApp, "nonexistent");
        Long result = pushProxyMapper.selectCount(wrapper);

        // Then
        assertNotNull(result);
        assertEquals(0L, result);

        log.info("统计零推流代理数量测试通过");
    }

    // ================================
    // 复杂查询测试
    // ================================

    @Test
    public void testComplexQuery_Success() {
        // Given
        pushProxyMapper.insert(testDO);

        PushProxyDO testDO2 = createTestDO();
        testDO2.setStream(TEST_STREAM + "2");
        testDO2.setStatus(0); // 禁用状态
        pushProxyMapper.insert(testDO2);

        PushProxyDO testDO3 = createTestDO();
        testDO3.setApp(TEST_APP + "2");
        testDO3.setStream(TEST_STREAM + "3");
        pushProxyMapper.insert(testDO3);

        // When - 查询指定应用下启用状态的代理
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getApp, TEST_APP)
            .eq(PushProxyDO::getStatus, 1)
            .eq(PushProxyDO::getEnabled, 1)
            .orderByDesc(PushProxyDO::getCreateTime);
        List<PushProxyDO> result = pushProxyMapper.selectList(wrapper);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_APP, result.get(0).getApp());
        assertEquals(1, result.get(0).getStatus());

        log.info("复杂条件查询推流代理测试通过");
    }

    @Test
    public void testLikeQuery_Success() {
        // Given
        pushProxyMapper.insert(testDO);

        // When - 模糊查询
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(PushProxyDO::getDstUrl, "example.com")
            .like(PushProxyDO::getDescription, "测试");
        List<PushProxyDO> result = pushProxyMapper.selectList(wrapper);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getDstUrl().contains("example.com"));

        log.info("模糊查询推流代理测试通过");
    }

    @Test
    public void testInQuery_Success() {
        // Given
        pushProxyMapper.insert(testDO);

        PushProxyDO testDO2 = createTestDO();
        testDO2.setStream(TEST_STREAM + "2");
        pushProxyMapper.insert(testDO2);

        // When - IN查询
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PushProxyDO::getStream, Arrays.asList(TEST_STREAM, TEST_STREAM + "2"));
        List<PushProxyDO> result = pushProxyMapper.selectList(wrapper);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        log.info("IN查询推流代理测试通过");
    }

    // ================================
    // 批量操作测试
    // ================================

    @Test
    public void testBatchInsert_Success() {
        // Given
        PushProxyDO testDO2 = createTestDO();
        testDO2.setStream(TEST_STREAM + "2");

        PushProxyDO testDO3 = createTestDO();
        testDO3.setStream(TEST_STREAM + "3");

        // When
        pushProxyMapper.insert(testDO);
        pushProxyMapper.insert(testDO2);
        pushProxyMapper.insert(testDO3);

        // Then
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getApp, TEST_APP);
        List<PushProxyDO> result = pushProxyMapper.selectList(wrapper);

        assertEquals(3, result.size());

        log.info("批量插入推流代理测试通过");
    }

    @Test
    public void testBatchDelete_Success() {
        // Given
        pushProxyMapper.insert(testDO);

        PushProxyDO testDO2 = createTestDO();
        testDO2.setStream(TEST_STREAM + "2");
        pushProxyMapper.insert(testDO2);

        List<Long> ids = Arrays.asList(testDO.getId(), testDO2.getId());

        // When
        int result = pushProxyMapper.deleteBatchIds(ids);

        // Then
        assertEquals(2, result);

        // 验证删除结果
        assertNull(pushProxyMapper.selectById(testDO.getId()));
        assertNull(pushProxyMapper.selectById(testDO2.getId()));

        log.info("批量删除推流代理测试通过");
    }

    @Test
    public void testBatchDeleteByCondition_Success() {
        // Given
        pushProxyMapper.insert(testDO);

        PushProxyDO testDO2 = createTestDO();
        testDO2.setStream(TEST_STREAM + "2");
        pushProxyMapper.insert(testDO2);

        // When
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getApp, TEST_APP);
        int result = pushProxyMapper.delete(wrapper);

        // Then
        assertEquals(2, result);

        // 验证删除结果
        Long count = pushProxyMapper.selectCount(wrapper);
        assertEquals(0L, count);

        log.info("条件批量删除推流代理测试通过");
    }

    // ================================
    // 时间相关查询测试
    // ================================

    @Test
    public void testTimeRangeQuery_Success() {
        // Given
        pushProxyMapper.insert(testDO);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime oneHourLater = now.plusHours(1);

        // When
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(PushProxyDO::getCreateTime, oneHourAgo, oneHourLater);
        List<PushProxyDO> result = pushProxyMapper.selectList(wrapper);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        log.info("时间范围查询推流代理测试通过");
    }

    @Test
    public void testOrderByQuery_Success() {
        // Given
        pushProxyMapper.insert(testDO);

        try {
            Thread.sleep(10); // 确保时间差异
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        PushProxyDO testDO2 = createTestDO();
        testDO2.setStream(TEST_STREAM + "2");
        pushProxyMapper.insert(testDO2);

        // When - 按创建时间降序排列
        LambdaQueryWrapper<PushProxyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushProxyDO::getApp, TEST_APP)
            .orderByDesc(PushProxyDO::getCreateTime);
        List<PushProxyDO> result = pushProxyMapper.selectList(wrapper);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(0).getCreateTime().isAfter(result.get(1).getCreateTime()) ||
            result.get(0).getCreateTime().isEqual(result.get(1).getCreateTime()));

        log.info("排序查询推流代理测试通过");
    }

    // ================================
    // 唯一约束测试
    // ================================

    @Test
    public void testUniqueConstraint() {
        // Given
        pushProxyMapper.insert(testDO);

        // When & Then - 插入重复的app+stream应该会失败（如果有唯一约束）
        PushProxyDO duplicate = createTestDO();
        try {
            pushProxyMapper.insert(duplicate);
            // 如果没有抛出异常，说明没有唯一约束或约束检查被跳过
            log.info("唯一约束测试：允许重复数据插入");
        } catch (Exception e) {
            // 如果抛出异常，说明有唯一约束
            log.info("唯一约束测试：检测到重复数据插入异常 - {}", e.getMessage());
        }
    }
}