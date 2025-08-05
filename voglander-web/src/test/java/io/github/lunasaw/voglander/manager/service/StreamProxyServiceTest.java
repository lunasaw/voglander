package io.github.lunasaw.voglander.manager.service;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.BaseMockTest;
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;
import io.github.lunasaw.voglander.repository.mapper.StreamProxyMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * StreamProxyService单元测试类
 * 
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
public class StreamProxyServiceTest extends BaseMockTest {

    private final String       TEST_APP       = "live";
    private final String       TEST_STREAM    = "test";
    private final String       TEST_URL       = "rtmp://live.example.com/live/test";
    private final String       TEST_PROXY_KEY = "test-proxy-key-123";
    private final Long         TEST_ID        = 1L;

    @Autowired
    private StreamProxyService streamProxyService;

    @MockitoBean
    private StreamProxyMapper  streamProxyMapper;

    private StreamProxyDO      testStreamProxyDO;

    @BeforeEach
    public void setUp() {
        testStreamProxyDO = createTestStreamProxyDO();
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

    @Test
    public void testSave_Success() {
        // Arrange
        when(streamProxyMapper.insert(any(StreamProxyDO.class))).thenReturn(1);

        // Act
        boolean result = streamProxyService.save(testStreamProxyDO);

        // Assert
        assertTrue(result);
        verify(streamProxyMapper).insert(testStreamProxyDO);
    }

    @Test
    public void testSave_Failed() {
        // Arrange
        when(streamProxyMapper.insert(any(StreamProxyDO.class))).thenReturn(0);

        // Act
        boolean result = streamProxyService.save(testStreamProxyDO);

        // Assert
        assertFalse(result);
        verify(streamProxyMapper).insert(testStreamProxyDO);
    }

    @Test
    public void testGetById_Success() {
        // Arrange
        when(streamProxyMapper.selectById(TEST_ID)).thenReturn(testStreamProxyDO);

        // Act
        StreamProxyDO result = streamProxyService.getById(TEST_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_ID, result.getId());
        assertEquals(TEST_PROXY_KEY, result.getProxyKey());
        verify(streamProxyMapper).selectById(TEST_ID);
    }

    @Test
    public void testGetById_NotFound() {
        // Arrange
        when(streamProxyMapper.selectById(TEST_ID)).thenReturn(null);

        // Act
        StreamProxyDO result = streamProxyService.getById(TEST_ID);

        // Assert
        assertNull(result);
        verify(streamProxyMapper).selectById(TEST_ID);
    }

    @Test
    public void testGetById_NullId() {
        // Act
        StreamProxyDO result = streamProxyService.getById(null);

        // Assert
        assertNull(result);
        verify(streamProxyMapper, never()).selectById(any());
    }

    @Test
    public void testUpdateById_Success() {
        // Arrange
        when(streamProxyMapper.updateById(any(StreamProxyDO.class))).thenReturn(1);

        // Act
        boolean result = streamProxyService.updateById(testStreamProxyDO);

        // Assert
        assertTrue(result);
        verify(streamProxyMapper).updateById(testStreamProxyDO);
    }

    @Test
    public void testUpdateById_Failed() {
        // Arrange
        when(streamProxyMapper.updateById(any(StreamProxyDO.class))).thenReturn(0);

        // Act
        boolean result = streamProxyService.updateById(testStreamProxyDO);

        // Assert
        assertFalse(result);
        verify(streamProxyMapper).updateById(testStreamProxyDO);
    }

    @Test
    public void testRemoveById_Success() {
        // Arrange
        when(streamProxyMapper.deleteById(TEST_ID)).thenReturn(1);

        // Act
        boolean result = streamProxyService.removeById(TEST_ID);

        // Assert
        assertTrue(result);
        verify(streamProxyMapper).deleteById(TEST_ID);
    }

    @Test
    public void testRemoveById_Failed() {
        // Arrange
        when(streamProxyMapper.deleteById(TEST_ID)).thenReturn(0);

        // Act
        boolean result = streamProxyService.removeById(TEST_ID);

        // Assert
        assertFalse(result);
        verify(streamProxyMapper).deleteById(TEST_ID);
    }

    @Test
    public void testRemoveById_NullId() {
        // Act
        boolean result = streamProxyService.removeById(null);

        // Assert
        assertFalse(result);
        verify(streamProxyMapper, never()).deleteById(any());
    }

    @Test
    public void testSaveOrUpdate_NewEntity_Save() {
        // Arrange
        testStreamProxyDO.setId(null); // 新实体没有ID
        when(streamProxyMapper.insert(any(StreamProxyDO.class))).thenReturn(1);

        // Act
        boolean result = streamProxyService.saveOrUpdate(testStreamProxyDO);

        // Assert
        assertTrue(result);
        verify(streamProxyMapper).insert(testStreamProxyDO);
        verify(streamProxyMapper, never()).updateById(any());
    }

    @Test
    public void testSaveOrUpdate_ExistingEntity_Update() {
        // Arrange
        when(streamProxyMapper.updateById(any(StreamProxyDO.class))).thenReturn(1);

        // Act
        boolean result = streamProxyService.saveOrUpdate(testStreamProxyDO);

        // Assert
        assertTrue(result);
        verify(streamProxyMapper).updateById(testStreamProxyDO);
        verify(streamProxyMapper, never()).insert(any());
    }

    @Test
    public void testGetOne_Success() {
        // Arrange
        QueryWrapper<StreamProxyDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("proxy_key", TEST_PROXY_KEY);
        when(streamProxyMapper.selectOne(any(QueryWrapper.class))).thenReturn(testStreamProxyDO);

        // Act
        StreamProxyDO result = streamProxyService.getOne(queryWrapper);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_PROXY_KEY, result.getProxyKey());
        verify(streamProxyMapper).selectOne(queryWrapper);
    }

    @Test
    public void testGetOne_NotFound() {
        // Arrange
        QueryWrapper<StreamProxyDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("proxy_key", "non-existent-key");
        when(streamProxyMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        // Act
        StreamProxyDO result = streamProxyService.getOne(queryWrapper);

        // Assert
        assertNull(result);
        verify(streamProxyMapper).selectOne(queryWrapper);
    }

    @Test
    public void testList_Success() {
        // Arrange
        QueryWrapper<StreamProxyDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app", TEST_APP);
        List<StreamProxyDO> expectedList = Arrays.asList(testStreamProxyDO);
        when(streamProxyMapper.selectList(any(QueryWrapper.class))).thenReturn(expectedList);

        // Act
        List<StreamProxyDO> result = streamProxyService.list(queryWrapper);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_PROXY_KEY, result.get(0).getProxyKey());
        verify(streamProxyMapper).selectList(queryWrapper);
    }

    @Test
    public void testList_EmptyResult() {
        // Arrange
        QueryWrapper<StreamProxyDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app", "non-existent-app");
        when(streamProxyMapper.selectList(any(QueryWrapper.class))).thenReturn(Arrays.asList());

        // Act
        List<StreamProxyDO> result = streamProxyService.list(queryWrapper);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(streamProxyMapper).selectList(queryWrapper);
    }

    @Test
    public void testPage_Success() {
        // Arrange
        Page<StreamProxyDO> page = new Page<>(1, 10);
        Page<StreamProxyDO> mockResult = new Page<>(1, 10);
        mockResult.setRecords(Arrays.asList(testStreamProxyDO));
        mockResult.setTotal(1);
        when(streamProxyMapper.selectPage(any(Page.class), any())).thenReturn(mockResult);

        // Act
        Page<StreamProxyDO> result = streamProxyService.page(page);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(TEST_PROXY_KEY, result.getRecords().get(0).getProxyKey());
        verify(streamProxyMapper).selectPage(eq(page), any());
    }

    @Test
    public void testPage_WithQueryWrapper_Success() {
        // Arrange
        Page<StreamProxyDO> page = new Page<>(1, 10);
        QueryWrapper<StreamProxyDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app", TEST_APP);

        Page<StreamProxyDO> mockResult = new Page<>(1, 10);
        mockResult.setRecords(Arrays.asList(testStreamProxyDO));
        mockResult.setTotal(1);
        when(streamProxyMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockResult);

        // Act
        Page<StreamProxyDO> result = streamProxyService.page(page, queryWrapper);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(TEST_PROXY_KEY, result.getRecords().get(0).getProxyKey());
        verify(streamProxyMapper).selectPage(page, queryWrapper);
    }

    @Test
    public void testPage_EmptyPage() {
        // Arrange
        Page<StreamProxyDO> page = new Page<>(1, 10);
        Page<StreamProxyDO> mockResult = new Page<>(1, 10);
        mockResult.setRecords(Arrays.asList());
        mockResult.setTotal(0);
        when(streamProxyMapper.selectPage(any(Page.class), any())).thenReturn(mockResult);

        // Act
        Page<StreamProxyDO> result = streamProxyService.page(page);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
        verify(streamProxyMapper).selectPage(eq(page), any());
    }

    @Test
    public void testCount_Success() {
        // Arrange
        QueryWrapper<StreamProxyDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("enabled", true);
        when(streamProxyMapper.selectCount(any(QueryWrapper.class))).thenReturn(5L);

        // Act
        long result = streamProxyService.count(queryWrapper);

        // Assert
        assertEquals(5L, result);
        verify(streamProxyMapper).selectCount(queryWrapper);
    }

    @Test
    public void testCount_ZeroResult() {
        // Arrange
        QueryWrapper<StreamProxyDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("enabled", false);
        when(streamProxyMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);

        // Act
        long result = streamProxyService.count(queryWrapper);

        // Assert
        assertEquals(0L, result);
        verify(streamProxyMapper).selectCount(queryWrapper);
    }

    @Test
    public void testCount_NoQuery() {
        // Arrange
        when(streamProxyMapper.selectCount(any())).thenReturn(10L);

        // Act
        long result = streamProxyService.count();

        // Assert
        assertEquals(10L, result);
        verify(streamProxyMapper).selectCount(any());
    }
}