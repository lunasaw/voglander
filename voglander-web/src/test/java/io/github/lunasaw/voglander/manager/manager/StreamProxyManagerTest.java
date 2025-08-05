package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.BaseMockTest;
import io.github.lunasaw.voglander.manager.assembler.StreamProxyAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.manager.service.StreamProxyService;
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;
import lombok.extern.slf4j.Slf4j;

/**
 * StreamProxyManager单元测试类
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
public class StreamProxyManagerTest extends BaseMockTest {

    private final String         TEST_APP       = "live";
    private final String         TEST_STREAM    = "test";
    private final String         TEST_URL       = "rtmp://live.example.com/live/test";
    private final String         TEST_PROXY_KEY = "test-proxy-key-123";
    private final Long           TEST_ID        = 1L;

    @Autowired
    private StreamProxyManager   streamProxyManager;

    @Autowired
    private CacheManager         cacheManager;

    @MockitoBean
    private StreamProxyService   streamProxyService;

    @MockitoBean
    private StreamProxyAssembler streamProxyAssembler;

    private StreamProxyDO        testStreamProxyDO;
    private StreamProxyDTO       testStreamProxyDTO;

    @BeforeEach
    public void setUp() {
        // 准备测试数据
        testStreamProxyDO = createTestStreamProxyDO();
        testStreamProxyDTO = createTestStreamProxyDTO();

        // 清除缓存
        if (cacheManager.getCache("streamProxy") != null) {
            cacheManager.getCache("streamProxy").clear();
        }
    }

    private StreamProxyDO createTestStreamProxyDO() {
        StreamProxyDO streamProxy = new StreamProxyDO();
        streamProxy.setId(TEST_ID);
        streamProxy.setApp(TEST_APP);
        streamProxy.setStream(TEST_STREAM);
        streamProxy.setUrl(TEST_URL);
        streamProxy.setProxyKey(TEST_PROXY_KEY);
        streamProxy.setStatus(1);
        streamProxy.setOnlineStatus(1);
        streamProxy.setEnabled(true);
        streamProxy.setDescription("Test proxy");
        streamProxy.setExtend("{\"vhost\":\"__defaultVhost__\"}");
        streamProxy.setCreateTime(LocalDateTime.now());
        streamProxy.setUpdateTime(LocalDateTime.now());
        return streamProxy;
    }

    private StreamProxyDTO createTestStreamProxyDTO() {
        StreamProxyDTO dto = new StreamProxyDTO();
        dto.setApp(TEST_APP);
        dto.setStream(TEST_STREAM);
        dto.setUrl(TEST_URL);
        dto.setEnabled(true);
        dto.setDescription("Test proxy");
        return dto;
    }

    @Test
    public void testCreateStreamProxy_Success() {
        // Arrange
        when(streamProxyAssembler.dtoToDo(any(StreamProxyDTO.class))).thenReturn(testStreamProxyDO);
        when(streamProxyService.saveOrUpdate(any(StreamProxyDO.class))).thenReturn(true);

        // Act
        Long result = streamProxyManager.createStreamProxy(testStreamProxyDTO);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_ID, result);
        verify(streamProxyAssembler).dtoToDo(any(StreamProxyDTO.class));
        verify(streamProxyService).saveOrUpdate(any(StreamProxyDO.class));
    }

    @Test
    public void testCreateStreamProxy_NullDTO_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            streamProxyManager.createStreamProxy(null);
        });
    }

    @Test
    public void testCreateStreamProxy_EmptyApp_ThrowsException() {
        // Arrange
        testStreamProxyDTO.setApp("");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            streamProxyManager.createStreamProxy(testStreamProxyDTO);
        });
    }

    @Test
    public void testCreateStreamProxy_EmptyStream_ThrowsException() {
        // Arrange
        testStreamProxyDTO.setStream("");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            streamProxyManager.createStreamProxy(testStreamProxyDTO);
        });
    }

    @Test
    public void testCreateStreamProxy_EmptyUrl_ThrowsException() {
        // Arrange
        testStreamProxyDTO.setUrl("");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            streamProxyManager.createStreamProxy(testStreamProxyDTO);
        });
    }

    @Test
    public void testGetByProxyKey_Success() {
        // Arrange
        when(streamProxyService.getOne(any(QueryWrapper.class))).thenReturn(testStreamProxyDO);

        // Act
        StreamProxyDO result = streamProxyManager.getByProxyKey(TEST_PROXY_KEY);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_PROXY_KEY, result.getProxyKey());
        assertEquals(TEST_APP, result.getApp());
        assertEquals(TEST_STREAM, result.getStream());
        verify(streamProxyService).getOne(any(QueryWrapper.class));
    }

    @Test
    public void testGetByProxyKey_NotFound() {
        // Arrange
        when(streamProxyService.getOne(any(QueryWrapper.class))).thenReturn(null);

        // Act
        StreamProxyDO result = streamProxyManager.getByProxyKey(TEST_PROXY_KEY);

        // Assert
        assertNull(result);
        verify(streamProxyService).getOne(any(QueryWrapper.class));
    }

    @Test
    public void testGetByProxyKey_NullKey() {
        // Act
        StreamProxyDO result = streamProxyManager.getByProxyKey(null);

        // Assert
        assertNull(result);
        verify(streamProxyService, never()).getOne(any(QueryWrapper.class));
    }

    @Test
    public void testGetByAppAndStream_Success() {
        // Arrange
        when(streamProxyService.getOne(any(QueryWrapper.class))).thenReturn(testStreamProxyDO);

        // Act
        StreamProxyDO result = streamProxyManager.getByAppAndStream(TEST_APP, TEST_STREAM);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_APP, result.getApp());
        assertEquals(TEST_STREAM, result.getStream());
        verify(streamProxyService).getOne(any(QueryWrapper.class));
    }

    @Test
    public void testGetByAppAndStream_NullParams() {
        // Act
        StreamProxyDO result1 = streamProxyManager.getByAppAndStream(null, TEST_STREAM);
        StreamProxyDO result2 = streamProxyManager.getByAppAndStream(TEST_APP, null);

        // Assert
        assertNull(result1);
        assertNull(result2);
        verify(streamProxyService, never()).getOne(any(QueryWrapper.class));
    }

    @Test
    public void testSaveOrUpdateProxy_CreateNew() {
        // Arrange
        when(streamProxyService.getOne(any(QueryWrapper.class))).thenReturn(null); // 不存在
        when(streamProxyService.saveOrUpdate(any(StreamProxyDO.class))).thenReturn(true);

        // Act
        Long result = streamProxyManager.saveOrUpdateProxy(TEST_APP, TEST_STREAM, TEST_URL, TEST_PROXY_KEY, 1, null);

        // Assert
        assertNotNull(result);
        verify(streamProxyService).getOne(any(QueryWrapper.class));
        verify(streamProxyService).saveOrUpdate(any(StreamProxyDO.class));
    }

    @Test
    public void testSaveOrUpdateProxy_UpdateExisting() {
        // Arrange
        StreamProxyDO existing = createTestStreamProxyDO();
        existing.setOnlineStatus(0); // 原来是离线
        when(streamProxyService.getOne(any(QueryWrapper.class))).thenReturn(existing);
        when(streamProxyService.saveOrUpdate(any(StreamProxyDO.class))).thenReturn(true);

        // Act
        Long result = streamProxyManager.saveOrUpdateProxy(TEST_APP, TEST_STREAM, TEST_URL, TEST_PROXY_KEY, 1, null);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_ID, result);
        verify(streamProxyService).getOne(any(QueryWrapper.class));
        verify(streamProxyService).saveOrUpdate(any(StreamProxyDO.class));
    }

    @Test
    public void testSaveOrUpdateProxy_NullApp_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            streamProxyManager.saveOrUpdateProxy(null, TEST_STREAM, TEST_URL, TEST_PROXY_KEY, 1, null);
        });
    }

    @Test
    public void testSaveOrUpdateProxy_NullStream_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            streamProxyManager.saveOrUpdateProxy(TEST_APP, null, TEST_URL, TEST_PROXY_KEY, 1, null);
        });
    }

    @Test
    public void testSaveOrUpdateProxy_NullUrl_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            streamProxyManager.saveOrUpdateProxy(TEST_APP, TEST_STREAM, null, TEST_PROXY_KEY, 1, null);
        });
    }

    @Test
    public void testSaveOrUpdateProxy_NullProxyKey_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            streamProxyManager.saveOrUpdateProxy(TEST_APP, TEST_STREAM, TEST_URL, null, 1, null);
        });
    }

    @Test
    public void testUpdateProxyOnlineStatus_Success() {
        // Arrange
        when(streamProxyService.getOne(any(QueryWrapper.class))).thenReturn(testStreamProxyDO);
        when(streamProxyService.saveOrUpdate(any(StreamProxyDO.class))).thenReturn(true);

        // Act
        Long result = streamProxyManager.updateProxyOnlineStatus(TEST_PROXY_KEY, 0, "测试更新状态");

        // Assert
        assertNotNull(result);
        assertEquals(TEST_ID, result);
        verify(streamProxyService).getOne(any(QueryWrapper.class));
        verify(streamProxyService).saveOrUpdate(any(StreamProxyDO.class));
    }

    @Test
    public void testUpdateProxyOnlineStatus_ProxyNotFound() {
        // Arrange
        when(streamProxyService.getOne(any(QueryWrapper.class))).thenReturn(null);

        // Act
        Long result = streamProxyManager.updateProxyOnlineStatus(TEST_PROXY_KEY, 0, "测试更新状态");

        // Assert
        assertNull(result);
        verify(streamProxyService).getOne(any(QueryWrapper.class));
        verify(streamProxyService, never()).saveOrUpdate(any(StreamProxyDO.class));
    }

    @Test
    public void testUpdateProxyOnlineStatus_NullProxyKey_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            streamProxyManager.updateProxyOnlineStatus(null, 0, "测试更新状态");
        });
    }

    @Test
    public void testUpdateProxyOnlineStatus_NullStatus_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            streamProxyManager.updateProxyOnlineStatus(TEST_PROXY_KEY, null, "测试更新状态");
        });
    }

    @Test
    public void testGetProxyPage_Success() {
        // Arrange
        Page<StreamProxyDO> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(testStreamProxyDO));
        mockPage.setTotal(1);
        when(streamProxyService.page(any(Page.class))).thenReturn(mockPage);

        // Act
        Page<StreamProxyDO> result = streamProxyManager.getProxyPage(1, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(TEST_PROXY_KEY, result.getRecords().get(0).getProxyKey());
        verify(streamProxyService).page(any(Page.class));
    }

    @Test
    public void testDeleteStreamProxy_Success() {
        // Arrange
        when(streamProxyService.getById(TEST_ID)).thenReturn(testStreamProxyDO);
        when(streamProxyService.removeById(TEST_ID)).thenReturn(true);

        // Act
        boolean result = streamProxyManager.deleteStreamProxy(TEST_ID, "测试删除");

        // Assert
        assertTrue(result);
        verify(streamProxyService).getById(TEST_ID);
        verify(streamProxyService).removeById(TEST_ID);
    }

    @Test
    public void testDeleteStreamProxy_NotFound() {
        // Arrange
        when(streamProxyService.getById(TEST_ID)).thenReturn(null);

        // Act
        boolean result = streamProxyManager.deleteStreamProxy(TEST_ID, "测试删除");

        // Assert
        assertFalse(result);
        verify(streamProxyService).getById(TEST_ID);
        verify(streamProxyService, never()).removeById(TEST_ID);
    }

    @Test
    public void testDeleteStreamProxy_NullId_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            streamProxyManager.deleteStreamProxy(null, "测试删除");
        });
    }

    @Test
    public void testDeleteByProxyKey_Success() {
        // Arrange
        when(streamProxyService.getOne(any(QueryWrapper.class))).thenReturn(testStreamProxyDO);
        when(streamProxyService.removeById(TEST_ID)).thenReturn(true);

        // Act
        boolean result = streamProxyManager.deleteByProxyKey(TEST_PROXY_KEY, "根据key删除");

        // Assert
        assertTrue(result);
        verify(streamProxyService).getOne(any(QueryWrapper.class));
        verify(streamProxyService).removeById(TEST_ID);
    }

    @Test
    public void testDeleteByProxyKey_NotFound() {
        // Arrange
        when(streamProxyService.getOne(any(QueryWrapper.class))).thenReturn(null);

        // Act
        boolean result = streamProxyManager.deleteByProxyKey(TEST_PROXY_KEY, "根据key删除");

        // Assert
        assertFalse(result);
        verify(streamProxyService).getOne(any(QueryWrapper.class));
        verify(streamProxyService, never()).removeById(any());
    }

    @Test
    public void testDeleteByProxyKey_NullKey_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            streamProxyManager.deleteByProxyKey(null, "根据key删除");
        });
    }

    @Test
    public void testGetById_Success() {
        // Arrange
        when(streamProxyService.getById(TEST_ID)).thenReturn(testStreamProxyDO);

        // Act
        StreamProxyDO result = streamProxyManager.getById(TEST_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_ID, result.getId());
        assertEquals(TEST_PROXY_KEY, result.getProxyKey());
        verify(streamProxyService).getById(TEST_ID);
    }

    @Test
    public void testGetById_NullId() {
        // Act
        StreamProxyDO result = streamProxyManager.getById(null);

        // Assert
        assertNull(result);
        verify(streamProxyService, never()).getById(any());
    }

    @Test
    public void testDoToDto_Success() {
        // Arrange
        when(streamProxyAssembler.doToDto(testStreamProxyDO)).thenReturn(testStreamProxyDTO);

        // Act
        StreamProxyDTO result = streamProxyManager.doToDto(testStreamProxyDO);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_APP, result.getApp());
        assertEquals(TEST_STREAM, result.getStream());
        verify(streamProxyAssembler).doToDto(testStreamProxyDO);
    }

    @Test
    public void testUpdateStreamProxy_Success() {
        // Arrange
        when(streamProxyService.saveOrUpdate(any(StreamProxyDO.class))).thenReturn(true);

        // Act
        Long result = streamProxyManager.updateStreamProxy(testStreamProxyDO, "测试更新");

        // Assert
        assertNotNull(result);
        assertEquals(TEST_ID, result);
        verify(streamProxyService).saveOrUpdate(testStreamProxyDO);
    }

    @Test
    public void testCacheClearingOnUpdate() {
        // Arrange
        Cache cache = cacheManager.getCache("streamProxy");
        if (cache != null) {
            cache.put(TEST_ID, testStreamProxyDO);
            cache.put("key:" + TEST_PROXY_KEY, testStreamProxyDO);
        }
        when(streamProxyService.saveOrUpdate(any(StreamProxyDO.class))).thenReturn(true);

        // Act
        streamProxyManager.updateStreamProxy(testStreamProxyDO, "测试缓存清理");

        // Assert
        if (cache != null) {
            assertNull(cache.get(TEST_ID));
        }
        verify(streamProxyService).saveOrUpdate(testStreamProxyDO);
    }
}