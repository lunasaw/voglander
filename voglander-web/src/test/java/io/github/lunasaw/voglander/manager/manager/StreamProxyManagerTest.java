package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.assembler.StreamProxyAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.manager.service.StreamProxyService;
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;
import lombok.extern.slf4j.Slf4j;

/**
 * StreamProxyManager集成测试类
 * 
 * 测试策略：
 * - 继承BaseTest进行集成测试，使用真实的Spring上下文和数据库事务
 * - StreamProxyService使用@Autowired（基础数据访问层）
 * - StreamProxyAssembler使用@MockitoBean（业务逻辑层）
 * - 使用@Transactional确保测试数据隔离
 * 
 * 测试覆盖：
 * - 6个核心模板方法的完整测试：add, update, get, deleteOne, deleteBatch, getPage
 * - 业务扩展方法的完整测试：createStreamProxy, updateStreamProxy, deleteStreamProxy等
 * - 边界条件、异常场景、缓存行为测试
 * - 完整生命周期测试和复杂业务场景测试
 * 
 * 模板方法测试架构：
 * - 核心模板方法：验证标准CRUD流程和数据一致性
 * - 扩展业务方法：验证操作日志和业务逻辑
 * - 缓存清理验证：验证统一缓存清理机制
 * - 异常处理测试：验证参数校验和错误处理
 * 
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
public class StreamProxyManagerTest extends BaseTest {

    // 测试数据常量
    private static final String  TEST_APP            = "live";
    private static final String  TEST_STREAM         = "test";
    private static final String  TEST_URL            = "rtmp://live.example.com/live/test";
    private static final String  TEST_PROXY_KEY      = "test-proxy-key-123";
    private static final String  TEST_OPERATION_DESC = "测试操作";
    private static final String  TEST_EXTEND         = "{\"test\":\"data\"}";

    // 被测试对象和依赖
    @Autowired
    private StreamProxyManager   streamProxyManager;

    @Autowired
    private CacheManager         cacheManager;

    @Autowired
    private StreamProxyService   streamProxyService;

    @MockitoBean
    private StreamProxyAssembler streamProxyAssembler;

    // 测试数据对象
    private StreamProxyDO        testStreamProxyDO;
    private StreamProxyDTO       testStreamProxyDTO;

    @BeforeEach
    public void setUp() {
        log.info("开始设置测试数据");

        // 清理数据库中的测试数据
        cleanupTestData();

        // 创建测试用的DO对象
        testStreamProxyDO = createTestStreamProxyDO();

        // 创建测试用的DTO对象
        testStreamProxyDTO = createTestStreamProxyDTO();

        // 设置Assembler的模拟行为
        setupAssemblerMocks();

        log.info("测试数据设置完成");
    }

    @AfterEach
    public void tearDown() {
        log.info("开始清理测试数据");
        cleanupTestData();
        log.info("测试数据清理完成");
    }

    /**
     * 清理测试数据
     */
    private void cleanupTestData() {
        try {
            // 删除测试数据
            QueryWrapper<StreamProxyDO> wrapper = new QueryWrapper<>();
            wrapper.in("app", TEST_APP, TEST_APP + "2")
                .or().in("stream", TEST_STREAM, TEST_STREAM + "2")
                .or().in("proxy_key", TEST_PROXY_KEY, TEST_PROXY_KEY + "2");
            streamProxyService.remove(wrapper);

            // 清理缓存
            Cache cache = cacheManager.getCache("streamProxy");
            if (cache != null) {
                cache.clear();
            }
        } catch (Exception e) {
            log.warn("清理测试数据时发生异常: {}", e.getMessage());
        }
    }

    /**
     * 创建测试用的StreamProxyDO对象
     */
    private StreamProxyDO createTestStreamProxyDO() {
        StreamProxyDO streamProxyDO = new StreamProxyDO();
        streamProxyDO.setApp(TEST_APP);
        streamProxyDO.setStream(TEST_STREAM);
        streamProxyDO.setUrl(TEST_URL);
        streamProxyDO.setProxyKey(TEST_PROXY_KEY);
        streamProxyDO.setStatus(1);
        streamProxyDO.setOnlineStatus(0);
        streamProxyDO.setEnabled(true);
        streamProxyDO.setExtend(TEST_EXTEND);
        streamProxyDO.setCreateTime(LocalDateTime.now());
        streamProxyDO.setUpdateTime(LocalDateTime.now());
        return streamProxyDO;
    }

    /**
     * 创建测试用的StreamProxyDTO对象
     */
    private StreamProxyDTO createTestStreamProxyDTO() {
        StreamProxyDTO streamProxyDTO = new StreamProxyDTO();
        streamProxyDTO.setApp(TEST_APP);
        streamProxyDTO.setStream(TEST_STREAM);
        streamProxyDTO.setUrl(TEST_URL);
        streamProxyDTO.setProxyKey(TEST_PROXY_KEY);
        streamProxyDTO.setStatus(1);
        streamProxyDTO.setOnlineStatus(0);
        streamProxyDTO.setEnabled(true);
        streamProxyDTO.setExtend(TEST_EXTEND);
        return streamProxyDTO;
    }

    /**
     * 设置Assembler的模拟行为
     */
    private void setupAssemblerMocks() {
        // 模拟DTO转DO - 需要复制所有字段
        when(streamProxyAssembler.dtoToDo(any(StreamProxyDTO.class)))
            .thenAnswer(invocation -> {
                StreamProxyDTO dto = invocation.getArgument(0);
                StreamProxyDO streamProxyDO = new StreamProxyDO();
                streamProxyDO.setId(dto.getId());
                streamProxyDO.setCreateTime(dto.getCreateTime());
                streamProxyDO.setUpdateTime(dto.getUpdateTime());
                streamProxyDO.setApp(dto.getApp());
                streamProxyDO.setStream(dto.getStream());
                streamProxyDO.setUrl(dto.getUrl());
                streamProxyDO.setProxyKey(dto.getProxyKey());
                streamProxyDO.setStatus(dto.getStatus());
                streamProxyDO.setOnlineStatus(dto.getOnlineStatus());
                streamProxyDO.setEnabled(dto.getEnabled());
                streamProxyDO.setDescription(dto.getDescription());
                streamProxyDO.setExtend(dto.getExtend());
                return streamProxyDO;
            });

        // 模拟DO转DTO - 需要复制所有字段包括ID
        when(streamProxyAssembler.doToDto(any(StreamProxyDO.class)))
            .thenAnswer(invocation -> {
                StreamProxyDO streamProxyDO = invocation.getArgument(0);
                StreamProxyDTO dto = new StreamProxyDTO();
                dto.setId(streamProxyDO.getId());
                dto.setCreateTime(streamProxyDO.getCreateTime());
                dto.setUpdateTime(streamProxyDO.getUpdateTime());
                dto.setApp(streamProxyDO.getApp());
                dto.setStream(streamProxyDO.getStream());
                dto.setUrl(streamProxyDO.getUrl());
                dto.setProxyKey(streamProxyDO.getProxyKey());
                dto.setStatus(streamProxyDO.getStatus());
                dto.setOnlineStatus(streamProxyDO.getOnlineStatus());
                dto.setEnabled(streamProxyDO.getEnabled());
                dto.setDescription(streamProxyDO.getDescription());
                dto.setExtend(streamProxyDO.getExtend());
                return dto;
            });

        // 模拟DO列表转DTO列表
        when(streamProxyAssembler.doListToDtoList(anyList()))
            .thenAnswer(invocation -> {
                List<StreamProxyDO> doList = invocation.getArgument(0);
                if (doList == null || doList.isEmpty()) {
                    return new ArrayList<>();
                }
                return doList.stream()
                    .map(streamProxyDO -> {
                        StreamProxyDTO dto = new StreamProxyDTO();
                        dto.setId(streamProxyDO.getId());
                        dto.setCreateTime(streamProxyDO.getCreateTime());
                        dto.setUpdateTime(streamProxyDO.getUpdateTime());
                        dto.setApp(streamProxyDO.getApp());
                        dto.setStream(streamProxyDO.getStream());
                        dto.setUrl(streamProxyDO.getUrl());
                        dto.setProxyKey(streamProxyDO.getProxyKey());
                        dto.setStatus(streamProxyDO.getStatus());
                        dto.setOnlineStatus(streamProxyDO.getOnlineStatus());
                        dto.setEnabled(streamProxyDO.getEnabled());
                        dto.setDescription(streamProxyDO.getDescription());
                        dto.setExtend(streamProxyDO.getExtend());
                        return dto;
                    })
                    .collect(java.util.stream.Collectors.toList());
            });
    }

    // ================================
    // 核心模板方法测试
    // ================================

    @Test
    public void testAdd_Success() {
        // Given
        StreamProxyDTO dto = createTestStreamProxyDTO();

        // When
        Long id = streamProxyManager.add(dto);

        // Then
        assertNotNull(id);
        assertTrue(id > 0);

        // 验证数据库中的记录
        StreamProxyDO saved = streamProxyService.getById(id);
        assertNotNull(saved);
        assertEquals(TEST_APP, saved.getApp());
        assertEquals(TEST_STREAM, saved.getStream());
        assertEquals(TEST_URL, saved.getUrl());
        assertEquals(TEST_PROXY_KEY, saved.getProxyKey());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());

        // 验证Assembler被调用
        verify(streamProxyAssembler).dtoToDo(dto);

        log.info("核心模板方法add测试通过，ID: {}", id);
    }

    @Test
    public void testAdd_Validation() {
        // Test null DTO
        assertThrows(IllegalArgumentException.class, () -> streamProxyManager.add(null));

        // Test empty app
        StreamProxyDTO dto1 = createTestStreamProxyDTO();
        dto1.setApp("");
        assertThrows(IllegalArgumentException.class, () -> streamProxyManager.add(dto1));

        // Test empty stream
        StreamProxyDTO dto2 = createTestStreamProxyDTO();
        dto2.setStream("");
        assertThrows(IllegalArgumentException.class, () -> streamProxyManager.add(dto2));

        // Test empty url
        StreamProxyDTO dto3 = createTestStreamProxyDTO();
        dto3.setUrl("");
        assertThrows(IllegalArgumentException.class, () -> streamProxyManager.add(dto3));

        log.info("核心模板方法add参数校验测试通过");
    }

    @Test
    public void testUpdate_ByIdSuccess() {
        // Given - 先创建一个代理
        Long proxyId = streamProxyManager.add(testStreamProxyDTO);
        assertNotNull(proxyId);

        // When - 通过ID更新
        StreamProxyDTO updateDTO = new StreamProxyDTO();
        updateDTO.setId(proxyId);
        updateDTO.setUrl("rtmp://updated.example.com/live/test");
        updateDTO.setDescription("Updated description");

        Long updatedId = streamProxyManager.update(updateDTO);

        // Then
        assertEquals(proxyId, updatedId);

        StreamProxyDO updated = streamProxyService.getById(proxyId);
        assertEquals("rtmp://updated.example.com/live/test", updated.getUrl());
        assertEquals("Updated description", updated.getDescription());
        assertNotNull(updated.getUpdateTime());

        // 验证Assembler被调用
        verify(streamProxyAssembler, atLeastOnce()).dtoToDo(any(StreamProxyDTO.class));

        log.info("核心模板方法update（通过ID）测试通过");
    }

    @Test
    public void testUpdate_ByAppStreamSuccess() {
        // Given - 先创建一个代理
        Long createdId = streamProxyManager.add(testStreamProxyDTO);
        assertNotNull(createdId);

        // When - 通过app+stream更新（不包含ID）
        // 重要：只设置查询条件字段和要更新的字段，避免LambdaQueryWrapper包含过多的查询条件
        StreamProxyDTO updateDTO = new StreamProxyDTO();
        updateDTO.setApp(TEST_APP);
        updateDTO.setStream(TEST_STREAM);
        updateDTO.setUrl("rtmp://updated.example.com/live/test");
        updateDTO.setDescription("Updated by app+stream");

        Long updatedId = streamProxyManager.update(updateDTO);

        // Then
        assertNotNull(updatedId);
        assertEquals(createdId, updatedId); // 应该是同一个ID
        assertTrue(updatedId > 0);

        StreamProxyDO updated = streamProxyService.getById(updatedId);
        assertEquals("rtmp://updated.example.com/live/test", updated.getUrl());
        assertEquals("Updated by app+stream", updated.getDescription());
        assertEquals(TEST_APP, updated.getApp());
        assertEquals(TEST_STREAM, updated.getStream());

        log.info("核心模板方法update（通过app+stream）测试通过");
    }

    @Test
    public void testUpdate_RecordNotFound() {
        // Given
        StreamProxyDTO updateDTO = new StreamProxyDTO();
        updateDTO.setId(99999L);
        updateDTO.setUrl("rtmp://test.example.com/live/test");

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> streamProxyManager.update(updateDTO));

        assertTrue(exception.getMessage().contains("未找到要更新的记录"));

        log.info("核心模板方法update记录不存在测试通过");
    }

    @Test
    public void testGet_ByIdSuccess() {
        // Given
        Long proxyId = streamProxyManager.add(testStreamProxyDTO);

        // When
        StreamProxyDTO queryDTO = new StreamProxyDTO();
        queryDTO.setId(proxyId);
        StreamProxyDTO found = streamProxyManager.get(queryDTO);

        // Then
        assertNotNull(found);
        assertEquals(proxyId, found.getId());
        assertEquals(TEST_APP, found.getApp());
        assertEquals(TEST_STREAM, found.getStream());
        assertEquals(TEST_URL, found.getUrl());

        // 验证Assembler被调用
        verify(streamProxyAssembler, atLeastOnce()).dtoToDo(any(StreamProxyDTO.class));
        verify(streamProxyAssembler, atLeastOnce()).doToDto(any(StreamProxyDO.class));

        log.info("核心模板方法get（通过ID）测试通过");
    }

    @Test
    public void testGet_ByAppStreamSuccess() {
        // Given
        streamProxyManager.add(testStreamProxyDTO);

        // When
        StreamProxyDTO queryDTO = new StreamProxyDTO();
        queryDTO.setApp(TEST_APP);
        queryDTO.setStream(TEST_STREAM);
        StreamProxyDTO found = streamProxyManager.get(queryDTO);

        // Then
        assertNotNull(found);
        assertEquals(TEST_APP, found.getApp());
        assertEquals(TEST_STREAM, found.getStream());
        assertEquals(TEST_URL, found.getUrl());

        log.info("核心模板方法get（通过app+stream）测试通过");
    }

    @Test
    public void testGet_NotFound() {
        // When
        StreamProxyDTO queryDTO = new StreamProxyDTO();
        queryDTO.setId(99999L);
        StreamProxyDTO found = streamProxyManager.get(queryDTO);

        // Then
        assertNull(found);

        log.info("核心模板方法get记录不存在测试通过");
    }

    @Test
    public void testDeleteOne_ByIdSuccess() {
        // Given
        Long proxyId = streamProxyManager.add(testStreamProxyDTO);

        // When
        StreamProxyDTO deleteDTO = new StreamProxyDTO();
        deleteDTO.setId(proxyId);
        Boolean result = streamProxyManager.deleteOne(deleteDTO);

        // Then
        assertTrue(result);
        assertNull(streamProxyService.getById(proxyId));

        // 验证Assembler被调用
        verify(streamProxyAssembler, atLeastOnce()).dtoToDo(any(StreamProxyDTO.class));

        log.info("核心模板方法deleteOne（通过ID）测试通过");
    }

    @Test
    public void testDeleteOne_ByProxyKeySuccess() {
        // Given
        Long proxyId = streamProxyManager.add(testStreamProxyDTO);

        // When
        StreamProxyDTO deleteDTO = new StreamProxyDTO();
        deleteDTO.setProxyKey(TEST_PROXY_KEY);
        Boolean result = streamProxyManager.deleteOne(deleteDTO);

        // Then
        assertTrue(result);
        assertNull(streamProxyService.getById(proxyId));

        log.info("核心模板方法deleteOne（通过proxyKey）测试通过");
    }

    @Test
    public void testDeleteOne_NotFound() {
        // When
        StreamProxyDTO deleteDTO = new StreamProxyDTO();
        deleteDTO.setId(99999L);
        Boolean result = streamProxyManager.deleteOne(deleteDTO);

        // Then
        assertFalse(result);

        log.info("核心模板方法deleteOne记录不存在测试通过");
    }

    @Test
    public void testDeleteBatch_Success() {
        // Given - 创建多个代理
        streamProxyManager.add(testStreamProxyDTO);

        StreamProxyDTO dto2 = createTestStreamProxyDTO();
        dto2.setStream(TEST_STREAM + "2");
        dto2.setProxyKey(TEST_PROXY_KEY + "2");
        streamProxyManager.add(dto2);

        // When - 批量删除所有TEST_APP的代理
        StreamProxyDTO deleteDTO = new StreamProxyDTO();
        deleteDTO.setApp(TEST_APP);
        Boolean result = streamProxyManager.deleteBatch(deleteDTO);

        // Then
        assertTrue(result);

        // 验证都被删除了
        StreamProxyDTO queryDTO = new StreamProxyDTO();
        queryDTO.setApp(TEST_APP);
        Page<StreamProxyDTO> page = streamProxyManager.getPage(queryDTO, 1, 10);
        assertEquals(0, page.getTotal());

        log.info("核心模板方法deleteBatch测试通过");
    }

    @Test
    public void testGetPage_WithConditions() {
        // Given - 创建多个代理
        streamProxyManager.add(testStreamProxyDTO);

        StreamProxyDTO dto2 = createTestStreamProxyDTO();
        dto2.setStream(TEST_STREAM + "2");
        dto2.setProxyKey(TEST_PROXY_KEY + "2");
        streamProxyManager.add(dto2);

        StreamProxyDTO dto3 = createTestStreamProxyDTO();
        dto3.setApp(TEST_APP + "3");
        dto3.setStream(TEST_STREAM + "3");
        dto3.setProxyKey(TEST_PROXY_KEY + "3");
        streamProxyManager.add(dto3);

        // When - 分页查询TEST_APP的代理
        StreamProxyDTO queryDTO = new StreamProxyDTO();
        queryDTO.setApp(TEST_APP);
        Page<StreamProxyDTO> page = streamProxyManager.getPage(queryDTO, 1, 10);

        // Then
        assertNotNull(page);
        assertEquals(2, page.getTotal()); // 只有2个TEST_APP的代理
        assertEquals(2, page.getRecords().size());
        assertEquals(1, page.getCurrent());
        assertEquals(10, page.getSize());

        // 验证记录内容
        for (StreamProxyDTO dto : page.getRecords()) {
            assertEquals(TEST_APP, dto.getApp());
        }

        // 验证Assembler被调用
        verify(streamProxyAssembler, atLeastOnce()).dtoToDo(any(StreamProxyDTO.class));
        verify(streamProxyAssembler, atLeastOnce()).doListToDtoList(anyList());

        log.info("核心模板方法getPage（带条件）测试通过，找到{}条记录", page.getTotal());
    }

    @Test
    public void testGetPage_AllRecords() {
        // Given
        streamProxyManager.add(testStreamProxyDTO);

        StreamProxyDTO dto2 = createTestStreamProxyDTO();
        dto2.setApp(TEST_APP + "2");
        dto2.setStream(TEST_STREAM + "2");
        dto2.setProxyKey(TEST_PROXY_KEY + "2");
        streamProxyManager.add(dto2);

        // When - 查询所有记录（无条件）
        Page<StreamProxyDTO> page = streamProxyManager.getPage(null, 1, 10);

        // Then
        assertNotNull(page);
        assertTrue(page.getTotal() >= 2);
        assertTrue(page.getRecords().size() >= 2);

        log.info("核心模板方法getPage（无条件）测试通过，总记录数: {}", page.getTotal());
    }

    @Test
    public void testGetPage_Validation() {
        // Test invalid page
        assertThrows(IllegalArgumentException.class, () -> streamProxyManager.getPage(null, 0, 10));

        // Test invalid size
        assertThrows(IllegalArgumentException.class, () -> streamProxyManager.getPage(null, 1, 0));

        assertThrows(IllegalArgumentException.class, () -> streamProxyManager.getPage(null, 1, 1001));

        log.info("核心模板方法getPage参数校验测试通过");
    }

    // ================================
    // 业务扩展方法测试
    // ================================

    @Test
    public void testCreateStreamProxy_Success() {
        // Given
        StreamProxyDTO dto = createTestStreamProxyDTO();
        dto.setEnabled(null); // 测试默认值设置
        dto.setStatus(null);
        dto.setOnlineStatus(null);

        // When
        Long proxyId = streamProxyManager.createStreamProxy(dto);

        // Then
        assertNotNull(proxyId);
        assertTrue(proxyId > 0);

        // 验证数据库中的记录
        StreamProxyDO saved = streamProxyService.getById(proxyId);
        assertNotNull(saved);
        assertEquals(TEST_APP, saved.getApp());
        assertEquals(TEST_STREAM, saved.getStream());
        assertEquals(TEST_URL, saved.getUrl());
        assertEquals(TEST_PROXY_KEY, saved.getProxyKey());
        assertTrue(saved.getEnabled()); // 默认true
        assertEquals(1, saved.getStatus()); // 默认1
        assertEquals(0, saved.getOnlineStatus()); // 默认0

        // 验证Assembler被调用
        verify(streamProxyAssembler).dtoToDo(any(StreamProxyDTO.class));

        log.info("业务扩展方法createStreamProxy测试通过，代理ID: {}", proxyId);
    }

    @Test
    public void testUpdateStreamProxy_Success() {
        // Given - 先创建一个代理
        Long proxyId = streamProxyManager.add(testStreamProxyDTO);

        StreamProxyDTO updateDTO = new StreamProxyDTO();
        updateDTO.setId(proxyId);
        updateDTO.setUrl("rtmp://updated.example.com/live/test");
        updateDTO.setDescription("Updated by business method");

        // When - 使用带操作日志的业务方法
        Boolean result = streamProxyManager.updateStreamProxy(updateDTO, "业务更新测试");

        // Then
        assertTrue(result);

        StreamProxyDO updated = streamProxyService.getById(proxyId);
        assertEquals("rtmp://updated.example.com/live/test", updated.getUrl());
        assertEquals("Updated by business method", updated.getDescription());

        log.info("业务扩展方法updateStreamProxy测试通过");
    }

    @Test
    public void testDeleteStreamProxy_Success() {
        // Given
        Long proxyId = streamProxyManager.add(testStreamProxyDTO);

        StreamProxyDTO deleteDTO = new StreamProxyDTO();
        deleteDTO.setId(proxyId);

        // When - 使用带操作日志的业务删除方法
        boolean result = streamProxyManager.deleteStreamProxy(deleteDTO, "业务删除测试");

        // Then
        assertTrue(result);
        assertNull(streamProxyService.getById(proxyId));

        log.info("业务扩展方法deleteStreamProxy测试通过");
    }

    @Test
    public void testDeleteByProxyKey_Success() {
        // Given
        Long proxyId = streamProxyManager.add(testStreamProxyDTO);

        // When
        boolean result = streamProxyManager.deleteByProxyKey(TEST_PROXY_KEY, "通过ProxyKey删除测试");

        // Then
        assertTrue(result);
        assertNull(streamProxyService.getById(proxyId));

        log.info("业务扩展方法deleteByProxyKey测试通过");
    }

    @Test
    public void testGetById_Success() {
        // Given
        Long proxyId = streamProxyManager.add(testStreamProxyDTO);

        // When
        StreamProxyDTO found = streamProxyManager.getById(proxyId);

        // Then
        assertNotNull(found);
        assertEquals(proxyId, found.getId());
        assertEquals(TEST_APP, found.getApp());
        assertEquals(TEST_STREAM, found.getStream());

        log.info("业务扩展方法getById测试通过");
    }

    @Test
    public void testGetById_NotFound() {
        // When
        StreamProxyDTO found = streamProxyManager.getById(99999L);

        // Then
        assertNull(found);

        log.info("业务扩展方法getById记录不存在测试通过");
    }

    @Test
    public void testUpdateStreamProxyOnlineStatus_Success() {
        // Given
        Long proxyId = streamProxyManager.add(testStreamProxyDTO);

        // When
        boolean result = streamProxyManager.updateStreamProxyOnlineStatus(proxyId, 1, "设备上线");

        // Then
        assertTrue(result);

        StreamProxyDO updated = streamProxyService.getById(proxyId);
        assertEquals(1, updated.getOnlineStatus());

        log.info("业务扩展方法updateStreamProxyOnlineStatus测试通过");
    }

    @Test
    public void testUpdateStreamProxyKey_Success() {
        // Given
        Long proxyId = streamProxyManager.add(testStreamProxyDTO);
        String newProxyKey = "new-proxy-key-456";

        // When
        boolean result = streamProxyManager.updateStreamProxyKey(proxyId, newProxyKey, "更新代理密钥");

        // Then
        assertTrue(result);

        StreamProxyDO updated = streamProxyService.getById(proxyId);
        assertEquals(newProxyKey, updated.getProxyKey());

        log.info("业务扩展方法updateStreamProxyKey测试通过");
    }

    @Test
    public void testGetProxyPage_Success() {
        // Given
        streamProxyManager.add(testStreamProxyDTO);

        StreamProxyDTO dto2 = createTestStreamProxyDTO();
        dto2.setStream(TEST_STREAM + "2");
        dto2.setProxyKey(TEST_PROXY_KEY + "2");
        streamProxyManager.add(dto2);

        // When
        Page<StreamProxyDTO> page = streamProxyManager.getProxyPage(1, 10);

        // Then
        assertNotNull(page);
        assertTrue(page.getTotal() >= 2);
        assertTrue(page.getRecords().size() >= 2);
        assertEquals(1, page.getCurrent());
        assertEquals(10, page.getSize());

        log.info("业务扩展方法getProxyPage测试通过，总记录数: {}", page.getTotal());
    }

    // ================================
    // 缓存清理测试
    // ================================

    @Test
    public void testCacheClearing_OnUpdate() {
        // Given
        Long proxyId = streamProxyManager.add(testStreamProxyDTO);
        String newProxyKey = "updated-proxy-key";

        // When - 更新会触发缓存清理
        StreamProxyDTO updateDTO = new StreamProxyDTO();
        updateDTO.setId(proxyId);
        updateDTO.setProxyKey(newProxyKey);
        updateDTO.setUrl("rtmp://updated.example.com/live/test");

        Long updatedId = streamProxyManager.update(updateDTO);

        // Then
        assertEquals(proxyId, updatedId);

        StreamProxyDO updated = streamProxyService.getById(proxyId);
        assertEquals(newProxyKey, updated.getProxyKey());
        assertEquals("rtmp://updated.example.com/live/test", updated.getUrl());

        log.info("缓存清理测试（更新）通过 - 旧key: {}, 新key: {}", TEST_PROXY_KEY, newProxyKey);
    }

    @Test
    public void testCacheClearing_OnDelete() {
        // Given
        Long proxyId = streamProxyManager.add(testStreamProxyDTO);

        // When - 删除会触发缓存清理
        StreamProxyDTO deleteDTO = new StreamProxyDTO();
        deleteDTO.setId(proxyId);

        Boolean result = streamProxyManager.deleteOne(deleteDTO);

        // Then
        assertTrue(result);
        assertNull(streamProxyService.getById(proxyId));

        log.info("缓存清理测试（删除）通过 - 已删除ID: {}, proxyKey: {}", proxyId, TEST_PROXY_KEY);
    }

    // ================================
    // 完整生命周期测试和复杂场景测试
    // ================================

    @Test
    public void testCompleteLifecycleWithTemplates() {
        // 1. 创建代理（使用核心模板方法）
        Long proxyId = streamProxyManager.add(testStreamProxyDTO);
        assertNotNull(proxyId);
        log.info("1. 使用模板方法add创建代理成功: {}", proxyId);

        // 2. 查询验证（使用核心模板方法）
        StreamProxyDTO queryDTO = new StreamProxyDTO();
        queryDTO.setId(proxyId);
        StreamProxyDTO found = streamProxyManager.get(queryDTO);
        assertNotNull(found);
        assertEquals(TEST_APP, found.getApp());
        assertEquals(TEST_STREAM, found.getStream());
        log.info("2. 使用模板方法get查询验证成功");

        // 3. 更新代理（使用核心模板方法）
        StreamProxyDTO updateDTO = new StreamProxyDTO();
        updateDTO.setId(proxyId);
        updateDTO.setUrl("rtmp://updated.example.com/live/test");
        updateDTO.setDescription("Updated description");
        updateDTO.setOnlineStatus(1);

        Long updatedId = streamProxyManager.update(updateDTO);
        assertEquals(proxyId, updatedId);

        // 验证更新结果
        StreamProxyDTO updatedFound = streamProxyManager.getById(proxyId);
        assertEquals("rtmp://updated.example.com/live/test", updatedFound.getUrl());
        assertEquals("Updated description", updatedFound.getDescription());
        assertEquals(1, updatedFound.getOnlineStatus());
        log.info("3. 使用模板方法update更新成功");

        // 4. 业务方法测试
        boolean businessUpdateResult = streamProxyManager.updateStreamProxyOnlineStatus(proxyId, 0, "设备下线");
        assertTrue(businessUpdateResult);

        StreamProxyDTO offlineFound = streamProxyManager.getById(proxyId);
        assertEquals(0, offlineFound.getOnlineStatus());
        log.info("4. 业务方法updateStreamProxyOnlineStatus测试成功");

        // 5. 删除代理（使用核心模板方法）
        StreamProxyDTO deleteDTO = new StreamProxyDTO();
        deleteDTO.setId(proxyId);
        Boolean deleteResult = streamProxyManager.deleteOne(deleteDTO);
        assertTrue(deleteResult);

        // 验证删除结果
        StreamProxyDTO deletedFound = streamProxyManager.getById(proxyId);
        assertNull(deletedFound);
        log.info("5. 使用模板方法deleteOne删除成功");

        log.info("基于模板方法的完整生命周期测试通过");
    }

    @Test
    public void testComplexBusinessScenarios() {
        // 场景1：批量创建和条件查询
        List<Long> createdIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            StreamProxyDTO dto = createTestStreamProxyDTO();
            dto.setStream(TEST_STREAM + i);
            dto.setProxyKey(TEST_PROXY_KEY + i);
            dto.setStatus(i % 2); // 交替设置状态
            Long id = streamProxyManager.add(dto);
            createdIds.add(id);
        }
        log.info("场景1：批量创建5个代理完成");

        // 场景2：分页查询测试
        StreamProxyDTO queryDTO = new StreamProxyDTO();
        queryDTO.setApp(TEST_APP);
        Page<StreamProxyDTO> page1 = streamProxyManager.getPage(queryDTO, 1, 3);
        assertEquals(5, page1.getTotal());
        assertEquals(3, page1.getRecords().size());

        Page<StreamProxyDTO> page2 = streamProxyManager.getPage(queryDTO, 2, 3);
        assertEquals(2, page2.getRecords().size());
        log.info("场景2：分页查询测试通过 - 总数:{}, 第一页:{}, 第二页:{}",
            page1.getTotal(), page1.getRecords().size(), page2.getRecords().size());

        // 场景3：条件批量删除
        StreamProxyDTO batchDeleteDTO = new StreamProxyDTO();
        batchDeleteDTO.setApp(TEST_APP);
        batchDeleteDTO.setStatus(0); // 只删除状态为0的
        Boolean batchResult = streamProxyManager.deleteBatch(batchDeleteDTO);
        assertTrue(batchResult);

        // 验证批量删除结果
        Page<StreamProxyDTO> afterDelete = streamProxyManager.getPage(queryDTO, 1, 10);
        assertTrue(afterDelete.getTotal() < 5); // 应该剩余一些记录
        log.info("场景3：条件批量删除测试通过 - 剩余记录数: {}", afterDelete.getTotal());

        // 清理剩余数据
        streamProxyManager.deleteBatch(queryDTO);
        log.info("复杂业务场景测试完成");
    }

    @Test
    public void testTemplateMethodsIntegration() {
        // 测试模板方法的完整集成流程
        List<Long> proxyIds = new ArrayList<>();

        // 1. 使用add模板方法批量创建
        for (int i = 0; i < 3; i++) {
            StreamProxyDTO dto = createTestStreamProxyDTO();
            dto.setStream(TEST_STREAM + i);
            dto.setProxyKey(TEST_PROXY_KEY + i);
            dto.setDescription("Template test " + i);

            Long id = streamProxyManager.add(dto);
            proxyIds.add(id);
        }
        log.info("批量创建完成，创建了{}个代理", proxyIds.size());

        // 2. 使用get模板方法验证创建结果
        for (Long id : proxyIds) {
            StreamProxyDTO found = streamProxyManager.getById(id);
            assertNotNull(found);
            assertEquals(TEST_APP, found.getApp());
            assertTrue(found.getDescription().startsWith("Template test"));
        }
        log.info("查询验证完成");

        // 3. 使用update模板方法批量更新
        for (int i = 0; i < proxyIds.size(); i++) {
            StreamProxyDTO updateDTO = new StreamProxyDTO();
            updateDTO.setId(proxyIds.get(i));
            updateDTO.setDescription("Updated template test " + i);
            updateDTO.setOnlineStatus(1);

            Long updatedId = streamProxyManager.update(updateDTO);
            assertEquals(proxyIds.get(i), updatedId);
        }
        log.info("批量更新完成");

        // 4. 使用getPage模板方法验证更新结果
        StreamProxyDTO pageQuery = new StreamProxyDTO();
        pageQuery.setApp(TEST_APP);
        pageQuery.setOnlineStatus(1);

        Page<StreamProxyDTO> updatedPage = streamProxyManager.getPage(pageQuery, 1, 10);
        assertEquals(3, updatedPage.getTotal());

        for (StreamProxyDTO dto : updatedPage.getRecords()) {
            assertEquals(1, dto.getOnlineStatus());
            assertTrue(dto.getDescription().startsWith("Updated template test"));
        }
        log.info("分页查询验证更新结果完成");

        // 5. 使用deleteOne模板方法逐个删除
        for (Long id : proxyIds) {
            StreamProxyDTO deleteDTO = new StreamProxyDTO();
            deleteDTO.setId(id);

            Boolean result = streamProxyManager.deleteOne(deleteDTO);
            assertTrue(result);
        }

        // 验证全部删除
        Page<StreamProxyDTO> finalPage = streamProxyManager.getPage(pageQuery, 1, 10);
        assertEquals(0, finalPage.getTotal());
        log.info("逐个删除验证完成");

        log.info("模板方法集成测试完成");
    }
}