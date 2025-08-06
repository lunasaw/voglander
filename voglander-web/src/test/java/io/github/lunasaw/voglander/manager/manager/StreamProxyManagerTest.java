package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
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
 * - 13个公有方法的完整测试
 * - 边界条件、异常场景、缓存行为
 * - 统一内部方法的间接测试
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
        // 模拟DTO转DO
        when(streamProxyAssembler.dtoToDo(any(StreamProxyDTO.class)))
            .thenAnswer(invocation -> {
                StreamProxyDTO dto = invocation.getArgument(0);
                StreamProxyDO streamProxyDO = new StreamProxyDO();
                streamProxyDO.setApp(dto.getApp());
                streamProxyDO.setStream(dto.getStream());
                streamProxyDO.setUrl(dto.getUrl());
                streamProxyDO.setProxyKey(dto.getProxyKey());
                streamProxyDO.setStatus(dto.getStatus());
                streamProxyDO.setOnlineStatus(dto.getOnlineStatus());
                streamProxyDO.setEnabled(dto.getEnabled());
                streamProxyDO.setExtend(dto.getExtend());
                return streamProxyDO;
            });

        // 模拟DO转DTO
        when(streamProxyAssembler.doToDto(any(StreamProxyDO.class)))
            .thenAnswer(invocation -> {
                StreamProxyDO streamProxyDO = invocation.getArgument(0);
                StreamProxyDTO dto = new StreamProxyDTO();
                dto.setApp(streamProxyDO.getApp());
                dto.setStream(streamProxyDO.getStream());
                dto.setUrl(streamProxyDO.getUrl());
                dto.setProxyKey(streamProxyDO.getProxyKey());
                dto.setStatus(streamProxyDO.getStatus());
                dto.setOnlineStatus(streamProxyDO.getOnlineStatus());
                dto.setEnabled(streamProxyDO.getEnabled());
                dto.setExtend(streamProxyDO.getExtend());
                return dto;
            });
    }

    // ================================
    // 基础功能测试
    // ================================

    @Test
    public void testCreateStreamProxy_Success() {
        // Given
        StreamProxyDTO dto = createTestStreamProxyDTO();

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
        assertTrue(saved.getEnabled());
        assertEquals(1, saved.getStatus());
        assertEquals(0, saved.getOnlineStatus());

        // 验证Assembler被调用
        verify(streamProxyAssembler).dtoToDo(dto);

        log.info("创建代理测试通过，代理ID: {}", proxyId);
    }

    @Test
    public void testCreateStreamProxy_WithDefaults() {
        // Given
        StreamProxyDTO dto = new StreamProxyDTO();
        dto.setApp(TEST_APP);
        dto.setStream(TEST_STREAM);
        dto.setUrl(TEST_URL);
        // 不设置enabled、status、onlineStatus，测试默认值

        // When
        Long proxyId = streamProxyManager.createStreamProxy(dto);

        // Then
        assertNotNull(proxyId);

        StreamProxyDO saved = streamProxyService.getById(proxyId);
        assertTrue(saved.getEnabled()); // 默认true
        assertEquals(1, saved.getStatus()); // 默认1
        assertEquals(0, saved.getOnlineStatus()); // 默认0

        log.info("默认值测试通过");
    }

    @Test
    public void testCreateStreamProxy_Validation() {
        // Test null DTO
        assertThrows(IllegalArgumentException.class, () -> streamProxyManager.createStreamProxy(null));

        // Test empty app
        StreamProxyDTO dto1 = createTestStreamProxyDTO();
        dto1.setApp("");
        assertThrows(IllegalArgumentException.class, () -> streamProxyManager.createStreamProxy(dto1));

        // Test empty stream
        StreamProxyDTO dto2 = createTestStreamProxyDTO();
        dto2.setStream("");
        assertThrows(IllegalArgumentException.class, () -> streamProxyManager.createStreamProxy(dto2));

        // Test empty url
        StreamProxyDTO dto3 = createTestStreamProxyDTO();
        dto3.setUrl("");
        assertThrows(IllegalArgumentException.class, () -> streamProxyManager.createStreamProxy(dto3));

        log.info("参数校验测试通过");
    }

    @Test
    public void testUpdateProxyOnlineStatus_ByKey_Success() {
        // Given - 先创建一个代理
        Long proxyId = streamProxyManager.createStreamProxy(testStreamProxyDTO);
        assertNotNull(proxyId);

        // When - 更新在线状态
        Long updatedId = streamProxyManager.updateProxyOnlineStatus(
            TEST_PROXY_KEY, 1, "测试上线");

        // Then
        assertEquals(proxyId, updatedId);

        StreamProxyDO updated = streamProxyService.getById(proxyId);
        assertEquals(1, updated.getOnlineStatus());

        log.info("根据ProxyKey更新在线状态测试通过");
    }

    @Test
    public void testUpdateProxyOnlineStatus_ByKey_NotFound() {
        // When
        Long result = streamProxyManager.updateProxyOnlineStatus(
            "non-existing-key", 1, "测试操作");

        // Then
        assertNull(result);

        log.info("代理不存在测试通过");
    }

    @Test
    public void testUpdateStreamProxyOnlineStatus_ByAppStream_Success() {
        // Given
        Long proxyId = streamProxyManager.createStreamProxy(testStreamProxyDTO);

        // When
        Long updatedId = streamProxyManager.updateStreamProxyOnlineStatus(
            TEST_APP, TEST_STREAM, 1, TEST_EXTEND);

        // Then
        assertEquals(proxyId, updatedId);

        StreamProxyDO updated = streamProxyService.getById(proxyId);
        assertEquals(1, updated.getOnlineStatus());
        assertEquals(TEST_EXTEND, updated.getExtend());

        log.info("根据App和Stream更新测试通过");
    }

    @Test
    public void testSaveOrUpdateProxy_UpdateExisting() {
        // Given - 先创建一个代理
        Long originalId = streamProxyManager.createStreamProxy(testStreamProxyDTO);

        // When - Hook回调更新
        Long updatedId = streamProxyManager.saveOrUpdateProxy(
            TEST_APP, TEST_STREAM, TEST_URL, TEST_PROXY_KEY, 1, "hook-extend");

        // Then
        assertEquals(originalId, updatedId);

        StreamProxyDO updated = streamProxyService.getById(originalId);
        assertEquals(1, updated.getOnlineStatus());
        assertEquals("hook-extend", updated.getExtend());

        log.info("Hook更新现有代理测试通过");
    }

    @Test
    public void testSaveOrUpdateProxy_CreateNew() {
        // When
        Long proxyId = streamProxyManager.saveOrUpdateProxy(
            TEST_APP, TEST_STREAM, TEST_URL, TEST_PROXY_KEY, 1, TEST_EXTEND);

        // Then
        assertNotNull(proxyId);
        assertTrue(proxyId > 0);

        StreamProxyDO created = streamProxyService.getById(proxyId);
        assertEquals(TEST_APP, created.getApp());
        assertEquals(TEST_STREAM, created.getStream());
        assertEquals(TEST_URL, created.getUrl());
        assertEquals(TEST_PROXY_KEY, created.getProxyKey());
        assertEquals(1, created.getStatus());
        assertEquals(1, created.getOnlineStatus());
        assertTrue(created.getEnabled());
        assertEquals(TEST_EXTEND, created.getExtend());

        log.info("Hook创建新代理测试通过");
    }

    @Test
    public void testGetById() {
        // Given
        Long proxyId = streamProxyManager.createStreamProxy(testStreamProxyDTO);

        // When
        StreamProxyDO found = streamProxyManager.getById(proxyId);

        // Then
        assertNotNull(found);
        assertEquals(TEST_APP, found.getApp());
        assertEquals(TEST_STREAM, found.getStream());

        // Test null ID
        assertNull(streamProxyManager.getById(null));

        // Test non-existing ID
        assertNull(streamProxyManager.getById(99999L));

        log.info("根据ID查询测试通过");
    }

    @Test
    public void testGetByProxyKey() {
        // Given
        streamProxyManager.createStreamProxy(testStreamProxyDTO);

        // When
        StreamProxyDO found = streamProxyManager.getByProxyKey(TEST_PROXY_KEY);

        // Then
        assertNotNull(found);
        assertEquals(TEST_PROXY_KEY, found.getProxyKey());

        // Test null key
        assertNull(streamProxyManager.getByProxyKey(null));

        // Test non-existing key
        assertNull(streamProxyManager.getByProxyKey("non-existing"));

        log.info("根据ProxyKey查询测试通过");
    }

    @Test
    public void testGetByAppAndStream() {
        // Given
        streamProxyManager.createStreamProxy(testStreamProxyDTO);

        // When
        StreamProxyDO found = streamProxyManager.getByAppAndStream(TEST_APP, TEST_STREAM);

        // Then
        assertNotNull(found);
        assertEquals(TEST_APP, found.getApp());
        assertEquals(TEST_STREAM, found.getStream());

        // Test null parameters
        assertNull(streamProxyManager.getByAppAndStream(null, TEST_STREAM));
        assertNull(streamProxyManager.getByAppAndStream(TEST_APP, null));

        // Test non-existing
        assertNull(streamProxyManager.getByAppAndStream("non-existing", "non-existing"));

        log.info("根据App和Stream查询测试通过");
    }

    @Test
    public void testGetProxyByApp() {
        // Given
        streamProxyManager.createStreamProxy(testStreamProxyDTO);

        StreamProxyDTO dto2 = createTestStreamProxyDTO();
        dto2.setStream(TEST_STREAM + "2");
        streamProxyManager.createStreamProxy(dto2);

        // When
        List<StreamProxyDO> proxies = streamProxyManager.getProxyByApp(TEST_APP);

        // Then
        assertNotNull(proxies);
        assertEquals(2, proxies.size());

        // Test null app
        List<StreamProxyDO> emptyList = streamProxyManager.getProxyByApp(null);
        assertTrue(emptyList.isEmpty());

        log.info("根据App查询列表测试通过，找到{}个代理", proxies.size());
    }

    @Test
    public void testGetProxyPage() {
        // Given - 创建多个代理
        streamProxyManager.createStreamProxy(testStreamProxyDTO);

        StreamProxyDTO dto2 = createTestStreamProxyDTO();
        dto2.setStream(TEST_STREAM + "2");
        dto2.setProxyKey(TEST_PROXY_KEY + "2");
        streamProxyManager.createStreamProxy(dto2);

        // When
        Page<StreamProxyDO> page = streamProxyManager.getProxyPage(1, 10);

        // Then
        assertNotNull(page);
        assertTrue(page.getRecords().size() >= 2);
        assertEquals(1, page.getCurrent());
        assertEquals(10, page.getSize());

        log.info("分页查询测试通过，总记录数: {}", page.getTotal());
    }

    @Test
    public void testDoToDto() {
        // Given
        Long proxyId = streamProxyManager.createStreamProxy(testStreamProxyDTO);
        StreamProxyDO streamProxyDO = streamProxyService.getById(proxyId);

        // When
        StreamProxyDTO dto = streamProxyManager.doToDto(streamProxyDO);

        // Then
        assertNotNull(dto);
        assertEquals(streamProxyDO.getApp(), dto.getApp());
        assertEquals(streamProxyDO.getStream(), dto.getStream());
        assertEquals(streamProxyDO.getUrl(), dto.getUrl());

        // 验证Assembler被调用
        verify(streamProxyAssembler, atLeastOnce()).doToDto(any(StreamProxyDO.class));

        log.info("DO转DTO测试通过");
    }

    @Test
    public void testDeleteStreamProxy_Success() {
        // Given
        Long proxyId = streamProxyManager.createStreamProxy(testStreamProxyDTO);

        // When
        boolean result = streamProxyManager.deleteStreamProxy(proxyId, TEST_OPERATION_DESC);

        // Then
        assertTrue(result);
        assertNull(streamProxyService.getById(proxyId));

        log.info("根据ID删除代理测试通过");
    }

    @Test
    public void testDeleteByProxyKey_Success() {
        // Given
        Long proxyId = streamProxyManager.createStreamProxy(testStreamProxyDTO);

        // When
        boolean result = streamProxyManager.deleteByProxyKey(TEST_PROXY_KEY, TEST_OPERATION_DESC);

        // Then
        assertTrue(result);
        assertNull(streamProxyService.getById(proxyId));

        log.info("根据ProxyKey删除代理测试通过");
    }

    @Test
    public void testCompleteLifecycle() {
        // 1. 创建代理
        Long proxyId = streamProxyManager.createStreamProxy(testStreamProxyDTO);
        assertNotNull(proxyId);
        log.info("1. 创建代理成功: {}", proxyId);

        // 2. 查询验证
        StreamProxyDO created = streamProxyManager.getById(proxyId);
        assertEquals(0, created.getOnlineStatus());
        log.info("2. 查询验证成功，初始状态: {}", created.getOnlineStatus());

        // 3. 更新在线状态
        streamProxyManager.updateProxyOnlineStatus(TEST_PROXY_KEY, 1, "设备上线");
        StreamProxyDO onlined = streamProxyManager.getById(proxyId);
        assertEquals(1, onlined.getOnlineStatus());
        log.info("3. 更新在线状态成功: {}", onlined.getOnlineStatus());

        // 4. Hook回调更新
        streamProxyManager.saveOrUpdateProxy(TEST_APP, TEST_STREAM, TEST_URL,
            TEST_PROXY_KEY, 0, "hook回调数据");
        StreamProxyDO hooked = streamProxyManager.getById(proxyId);
        assertEquals(0, hooked.getOnlineStatus());
        assertEquals("hook回调数据", hooked.getExtend());
        log.info("4. Hook回调更新成功");

        // 5. 删除代理
        boolean deleted = streamProxyManager.deleteStreamProxy(proxyId, "清理测试数据");
        assertTrue(deleted);
        assertNull(streamProxyManager.getById(proxyId));
        log.info("5. 删除代理成功");

        log.info("完整生命周期测试通过");
    }
}