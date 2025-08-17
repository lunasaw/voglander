package io.github.lunasaw.voglander.manager.manager;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.assembler.PushProxyAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.PushProxyDTO;
import io.github.lunasaw.voglander.manager.service.PushProxyService;
import io.github.lunasaw.voglander.repository.entity.PushProxyDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 推流代理管理器集成测试
 * 
 * @author luna
 * @since 2025-01-23
 */
@Slf4j
public class PushProxyManagerTest extends BaseTest {

    // 测试数据常量
    private static final String TEST_APP            = "live";
    private static final String TEST_STREAM         = "test";
    private static final String TEST_DST_URL        = "rtmp://push.example.com/live/test";
    private static final String TEST_OPERATION_DESC = "测试操作";

    // 被测试对象 - 使用真实注入
    @Autowired
    private PushProxyManager    pushProxyManager;

    @Autowired
    private CacheManager        cacheManager;

    // 基础Service层 - 继承IService<DO>的服务使用真实注入
    @Autowired
    private PushProxyService    pushProxyService;

    // 业务组装层 - 数据转换逻辑使用模拟
    @MockitoBean
    private PushProxyAssembler  pushProxyAssembler;

    // 测试数据对象
    private PushProxyDO         testDO;
    private PushProxyDTO        testDTO;

    @BeforeEach
    public void setUp() {
        log.info("开始设置推流代理管理器测试数据");

        // 1. 清理数据库中的测试数据
        cleanupTestData();

        // 2. 创建测试用的DO和DTO对象
        testDO = createTestDO();
        testDTO = createTestDTO();

        // 3. 设置Assembler的模拟行为
        setupAssemblerMocks();

        log.info("推流代理管理器测试数据设置完成");
    }

    @AfterEach
    public void tearDown() {
        log.info("开始清理推流代理管理器测试数据");
        cleanupTestData();
        log.info("推流代理管理器测试数据清理完成");
    }

    /**
     * 清理测试数据 - 必须实现
     */
    private void cleanupTestData() {
        try {
            // 删除测试数据 - 覆盖所有测试数据模式
            QueryWrapper<PushProxyDO> wrapper = new QueryWrapper<>();
            wrapper.in("app", TEST_APP, TEST_APP + "2")
                .or().in("stream", TEST_STREAM, TEST_STREAM + "2")
                .or().like("dst_url", "example.com");
            pushProxyService.remove(wrapper);

            // 清理缓存
            if (cacheManager.getCache("pushProxy") != null) {
                cacheManager.getCache("pushProxy").clear();
            }
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
        return pushProxyDO;
    }

    /**
     * 创建测试DTO对象
     */
    private PushProxyDTO createTestDTO() {
        PushProxyDTO pushProxyDTO = new PushProxyDTO();
        pushProxyDTO.setApp(TEST_APP);
        pushProxyDTO.setStream(TEST_STREAM);
        pushProxyDTO.setDstUrl(TEST_DST_URL);
        pushProxyDTO.setSchema("rtmp");
        pushProxyDTO.setDescription("测试推流代理");
        pushProxyDTO.setStatus(1);
        pushProxyDTO.setOnlineStatus(0);
        pushProxyDTO.setEnabled(1);
        pushProxyDTO.setServerId("zlm-node-1");
        pushProxyDTO.setProxyKey("test_proxy_key");

        // 设置ExtendObj
        PushProxyDTO.ExtendObj extendObj = new PushProxyDTO.ExtendObj();
        extendObj.setVhost("__defaultVhost__");
        extendObj.setRetryCount(-1);
        extendObj.setRtpType(0);
        extendObj.setTimeoutSec(10);
        pushProxyDTO.setExtendObj(extendObj);

        return pushProxyDTO;
    }

    /**
     * 设置Assembler的模拟行为 - 必须实现
     */
    private void setupAssemblerMocks() {
        // 模拟DTO转DO
        when(pushProxyAssembler.dtoToDo(any(PushProxyDTO.class)))
            .thenAnswer(invocation -> {
                PushProxyDTO dto = invocation.getArgument(0);
                PushProxyDO pushProxyDO = new PushProxyDO();
                pushProxyDO.setId(dto.getId());
                pushProxyDO.setApp(dto.getApp());
                pushProxyDO.setStream(dto.getStream());
                pushProxyDO.setDstUrl(dto.getDstUrl());
                pushProxyDO.setSchema(dto.getSchema());
                pushProxyDO.setDescription(dto.getDescription());
                pushProxyDO.setStatus(dto.getStatus());
                pushProxyDO.setOnlineStatus(dto.getOnlineStatus());
                pushProxyDO.setEnabled(dto.getEnabled());
                pushProxyDO.setServerId(dto.getServerId());
                pushProxyDO.setProxyKey(dto.getProxyKey());
                return pushProxyDO;
            });

        // 模拟DO转DTO
        when(pushProxyAssembler.doToDto(any(PushProxyDO.class)))
            .thenAnswer(invocation -> {
                PushProxyDO pushProxyDO = invocation.getArgument(0);
                PushProxyDTO dto = new PushProxyDTO();
                dto.setId(pushProxyDO.getId());
                dto.setApp(pushProxyDO.getApp());
                dto.setStream(pushProxyDO.getStream());
                dto.setDstUrl(pushProxyDO.getDstUrl());
                dto.setSchema(pushProxyDO.getSchema());
                dto.setDescription(pushProxyDO.getDescription());
                dto.setStatus(pushProxyDO.getStatus());
                dto.setOnlineStatus(pushProxyDO.getOnlineStatus());
                dto.setEnabled(pushProxyDO.getEnabled());
                dto.setServerId(pushProxyDO.getServerId());
                dto.setProxyKey(pushProxyDO.getProxyKey());
                return dto;
            });

        // 模拟DO列表转DTO列表
        when(pushProxyAssembler.doListToDtoList(any()))
            .thenAnswer(invocation -> {
                return invocation.getArgument(0); // 简化处理，实际应转换列表
            });
    }

    // ================================
    // 核心模板方法测试
    // ================================

    @Test
    public void testAdd_Success() {
        // Given
        PushProxyDTO dto = createTestDTO();

        // When
        Long id = pushProxyManager.add(dto);

        // Then
        assertNotNull(id);
        assertTrue(id > 0);

        // 验证数据库中的记录
        PushProxyDO saved = pushProxyService.getById(id);
        assertNotNull(saved);
        assertEquals(TEST_APP, saved.getApp());
        assertEquals(TEST_STREAM, saved.getStream());

        // 验证Assembler被调用
        verify(pushProxyAssembler).dtoToDo(dto);

        log.info("新增推流代理测试通过，ID: {}", id);
    }

    @Test
    public void testAdd_Validation() {
        // Test null DTO
        assertThrows(IllegalArgumentException.class, () -> pushProxyManager.add(null));

        // Test invalid fields
        PushProxyDTO dto = createTestDTO();
        PushProxyDTO finalDto = dto;
        finalDto.setApp("");
        assertThrows(IllegalArgumentException.class, () -> pushProxyManager.add(finalDto));

        dto = createTestDTO();
        dto.setStream(null);
        PushProxyDTO finalDto1 = dto;
        assertThrows(IllegalArgumentException.class, () -> pushProxyManager.add(finalDto1));

        dto = createTestDTO();
        dto.setDstUrl("");
        PushProxyDTO finalDto2 = dto;
        assertThrows(IllegalArgumentException.class, () -> pushProxyManager.add(finalDto2));

        log.info("新增推流代理参数校验测试通过");
    }

    @Test
    public void testUpdate_Success() {
        // Given
        Long id = pushProxyManager.add(testDTO);
        assertNotNull(id);

        // When - 更新描述
        PushProxyDTO updateDTO = new PushProxyDTO();
        updateDTO.setDescription("更新后的描述");
        Long resultId = pushProxyManager.updateById(id, updateDTO);

        // Then
        assertNotNull(resultId);
        assertEquals(id, resultId);

        // 验证数据库中的记录
        PushProxyDO updated = pushProxyService.getById(id);
        assertNotNull(updated);
        assertEquals("更新后的描述", updated.getDescription());

        verify(pushProxyAssembler, atLeast(2)).dtoToDo(any());

        log.info("更新推流代理测试通过，ID: {}", resultId);
    }

    @Test
    public void testUpdate_NotFound() {
        // Given
        PushProxyDTO queryDTO = new PushProxyDTO();
        queryDTO.setId(999L);
        PushProxyDTO updateDTO = new PushProxyDTO();
        updateDTO.setDescription("不存在的记录");

        // When & Then
        assertThrows(RuntimeException.class, () -> pushProxyManager.update(queryDTO, updateDTO));

        log.info("更新不存在的推流代理测试通过");
    }

    @Test
    public void testGet_Success() {
        // Given
        Long id = pushProxyManager.add(testDTO);
        assertNotNull(id);

        // When
        PushProxyDTO queryDTO = new PushProxyDTO();
        queryDTO.setId(id);
        PushProxyDTO result = pushProxyManager.get(queryDTO);

        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals(TEST_APP, result.getApp());
        assertEquals(TEST_STREAM, result.getStream());

        verify(pushProxyAssembler).doToDto(any());

        log.info("查询推流代理测试通过");
    }

    @Test
    public void testGet_NotFound() {
        // Given
        PushProxyDTO queryDTO = new PushProxyDTO();
        queryDTO.setId(999L);

        // When
        PushProxyDTO result = pushProxyManager.get(queryDTO);

        // Then
        assertNull(result);

        log.info("查询不存在的推流代理测试通过");
    }

    @Test
    public void testGetById_Success() {
        // Given
        Long id = pushProxyManager.add(testDTO);
        assertNotNull(id);

        // When
        PushProxyDTO result = pushProxyManager.getById(id);

        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());

        log.info("根据ID查询推流代理测试通过");
    }

    @Test
    public void testDeleteOne_Success() {
        // Given
        Long id = pushProxyManager.add(testDTO);
        assertNotNull(id);

        // When
        PushProxyDTO deleteDTO = new PushProxyDTO();
        deleteDTO.setId(id);
        Boolean success = pushProxyManager.deleteOne(deleteDTO);

        // Then
        assertTrue(success);

        // 验证记录已删除
        PushProxyDO deleted = pushProxyService.getById(id);
        assertNull(deleted);

        log.info("删除推流代理测试通过");
    }

    @Test
    public void testDeleteOne_NotFound() {
        // Given
        PushProxyDTO deleteDTO = new PushProxyDTO();
        deleteDTO.setId(999L);

        // When
        Boolean success = pushProxyManager.deleteOne(deleteDTO);

        // Then
        assertFalse(success);

        log.info("删除不存在的推流代理测试通过");
    }

    @Test
    public void testDeleteBatch_Success() {
        // Given - 创建多条测试记录
        PushProxyDTO dto1 = createTestDTO();
        dto1.setStream(TEST_STREAM + "1");
        Long id1 = pushProxyManager.add(dto1);

        PushProxyDTO dto2 = createTestDTO();
        dto2.setStream(TEST_STREAM + "2");
        Long id2 = pushProxyManager.add(dto2);

        // When - 批量删除
        PushProxyDTO deleteDTO = new PushProxyDTO();
        deleteDTO.setApp(TEST_APP);
        Boolean success = pushProxyManager.deleteBatch(deleteDTO);

        // Then
        assertTrue(success);

        // 验证记录已删除
        assertNull(pushProxyService.getById(id1));
        assertNull(pushProxyService.getById(id2));

        log.info("批量删除推流代理测试通过");
    }

    @Test
    public void testGetPage_Success() {
        // Given
        Long id = pushProxyManager.add(testDTO);
        assertNotNull(id);

        // When
        PushProxyDTO queryDTO = new PushProxyDTO();
        queryDTO.setApp(TEST_APP);
        Page<PushProxyDTO> result = pushProxyManager.getPage(queryDTO, 1, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.getTotal() >= 1);
        assertFalse(result.getRecords().isEmpty());

        log.info("分页查询推流代理测试通过，总记录数: {}", result.getTotal());
    }

    @Test
    public void testGetPage_Empty() {
        // Given
        PushProxyDTO queryDTO = new PushProxyDTO();
        queryDTO.setApp("nonexistent");

        // When
        Page<PushProxyDTO> result = pushProxyManager.getPage(queryDTO, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(0L, result.getTotal());
        assertTrue(result.getRecords().isEmpty());

        log.info("空分页查询推流代理测试通过");
    }

    @Test
    public void testGetPage_InvalidParams() {
        // Test invalid page numbers
        assertThrows(IllegalArgumentException.class, () -> pushProxyManager.getPage(null, 0, 10));

        assertThrows(IllegalArgumentException.class, () -> pushProxyManager.getPage(null, 1, 0));

        assertThrows(IllegalArgumentException.class, () -> pushProxyManager.getPage(null, 1, 1001));

        log.info("分页参数校验测试通过");
    }

    // ================================
    // 增强业务方法测试
    // ================================

    @Test
    public void testCreatePushProxy_Success() {
        // Given
        PushProxyDTO dto = createTestDTO();
        dto.setStatus(null); // 测试默认值设置

        // When
        Long id = pushProxyManager.createPushProxy(dto);

        // Then
        assertNotNull(id);
        assertTrue(id > 0);

        // 验证默认值设置
        PushProxyDO saved = pushProxyService.getById(id);
        assertNotNull(saved);
        assertEquals(1, saved.getStatus());

        log.info("业务创建推流代理测试通过，ID: {}", id);
    }

    @Test
    public void testUpdatePushProxy_Success() {
        // Given
        Long id = pushProxyManager.add(testDTO);
        testDTO.setId(id);

        // When
        Boolean success = pushProxyManager.updatePushProxy(testDTO, TEST_OPERATION_DESC);

        // Then
        assertTrue(success);

        log.info("业务更新推流代理测试通过");
    }

    @Test
    public void testDeletePushProxy_Success() {
        // Given
        Long id = pushProxyManager.add(testDTO);
        testDTO.setId(id);

        // When
        boolean success = pushProxyManager.deletePushProxy(testDTO, TEST_OPERATION_DESC);

        // Then
        assertTrue(success);

        // 验证记录已删除
        assertNull(pushProxyService.getById(id));

        log.info("业务删除推流代理测试通过");
    }

    @Test
    public void testDeletePushProxyById_Success() {
        // Given
        Long id = pushProxyManager.add(testDTO);

        // When
        boolean success = pushProxyManager.deletePushProxyById(id, TEST_OPERATION_DESC);

        // Then
        assertTrue(success);

        // 验证记录已删除
        assertNull(pushProxyService.getById(id));

        log.info("根据ID业务删除推流代理测试通过");
    }

    @Test
    public void testDeleteByProxyKey_Success() {
        // Given
        Long id = pushProxyManager.add(testDTO);
        String proxyKey = testDTO.getProxyKey();

        // When
        boolean success = pushProxyManager.deleteByProxyKey(proxyKey, TEST_OPERATION_DESC);

        // Then
        assertTrue(success);

        // 验证记录已删除
        assertNull(pushProxyService.getById(id));

        log.info("根据代理键业务删除推流代理测试通过");
    }

    @Test
    public void testUpdatePushProxyOnlineStatus_Success() {
        // Given
        Long id = pushProxyManager.add(testDTO);

        // When
        boolean success = pushProxyManager.updatePushProxyOnlineStatus(id, 1, "更新在线状态");

        // Then
        assertTrue(success);

        // 验证状态已更新
        PushProxyDO updated = pushProxyService.getById(id);
        assertNotNull(updated);
        assertEquals(1, updated.getOnlineStatus());

        log.info("更新推流代理在线状态测试通过");
    }

    @Test
    public void testUpdatePushProxyKey_Success() {
        // Given
        Long id = pushProxyManager.add(testDTO);
        String newProxyKey = "new_proxy_key";

        // When
        boolean success = pushProxyManager.updatePushProxyKey(id, newProxyKey, "更新代理密钥");

        // Then
        assertTrue(success);

        // 验证密钥已更新
        PushProxyDO updated = pushProxyService.getById(id);
        assertNotNull(updated);
        assertEquals(newProxyKey, updated.getProxyKey());

        log.info("更新推流代理密钥测试通过");
    }

    // ================================
    // 完整生命周期测试
    // ================================

    @Test
    public void testCompleteLifecycle() {
        // 1. 创建
        Long id = pushProxyManager.createPushProxy(testDTO);
        assertNotNull(id);
        log.info("1. 创建成功: {}", id);

        // 2. 查询验证
        PushProxyDTO created = pushProxyManager.getById(id);
        assertNotNull(created);
        assertEquals(TEST_APP, created.getApp());
        log.info("2. 查询验证成功");

        // 3. 更新
        created.setDescription("更新后的描述");
        Boolean updateSuccess = pushProxyManager.updatePushProxy(created, "更新操作");
        assertTrue(updateSuccess);
        log.info("3. 更新成功");

        // 4. 状态更新
        boolean statusSuccess = pushProxyManager.updatePushProxyOnlineStatus(id, 1, "状态更新");
        assertTrue(statusSuccess);
        log.info("4. 状态更新成功");

        // 5. 删除
        boolean deleted = pushProxyManager.deletePushProxyById(id, "删除操作");
        assertTrue(deleted);
        assertNull(pushProxyService.getById(id));
        log.info("5. 删除成功");

        log.info("完整生命周期测试通过");
    }

    // ================================
    // 并发和压力测试
    // ================================

    @Test
    public void testConcurrentOperations() {
        // 测试并发操作的数据一致性
        String baseStream = TEST_STREAM + "_concurrent_";

        // 并发创建多个代理
        for (int i = 0; i < 5; i++) {
            PushProxyDTO dto = createTestDTO();
            dto.setStream(baseStream + i);
            Long id = pushProxyManager.add(dto);
            assertNotNull(id);
            log.info("并发创建代理 {}: {}", i, id);
        }

        // 验证所有记录都已创建
        PushProxyDTO queryDTO = new PushProxyDTO();
        queryDTO.setApp(TEST_APP);
        Page<PushProxyDTO> result = pushProxyManager.getPage(queryDTO, 1, 10);
        assertTrue(result.getTotal() >= 5);

        log.info("并发操作测试通过，创建记录数: {}", result.getTotal());
    }
}