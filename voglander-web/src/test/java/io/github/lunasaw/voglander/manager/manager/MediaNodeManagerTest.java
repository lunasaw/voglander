package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.config.TestConfig;
import io.github.lunasaw.voglander.manager.assembler.MediaNodeAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaNodeDTO;
import io.github.lunasaw.voglander.manager.service.MediaNodeService;
import io.github.lunasaw.voglander.repository.entity.MediaNodeDO;
import lombok.extern.slf4j.Slf4j;

/**
 * MediaNodeManager单元测试类
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
@SpringBootTest(classes = TestConfig.class)
@TestPropertySource(properties = {"spring.cache.type=simple"})
public class MediaNodeManagerTest {

    private final String       TEST_SERVER_ID = "TEST_SERVER_001";
    private final Long         TEST_ID        = 1L;

    @Autowired
    private MediaNodeManager   mediaNodeManager;

    @Autowired
    private CacheManager       cacheManager;

    @MockitoBean
    private MediaNodeService   mediaNodeService;

    @MockitoBean
    private MediaNodeAssembler mediaNodeAssembler;

    private MediaNodeDTO       testMediaNodeDTO;
    private MediaNodeDO        testMediaNodeDO;

    @BeforeEach
    public void setUp() {
        // 初始化测试数据
        testMediaNodeDTO = createTestMediaNodeDTO();
        testMediaNodeDO = createTestMediaNodeDO();
    }

    /**
     * 创建测试用的MediaNodeDTO
     */
    private MediaNodeDTO createTestMediaNodeDTO() {
        MediaNodeDTO dto = new MediaNodeDTO();
        dto.setId(TEST_ID);
        dto.setServerId(TEST_SERVER_ID);
        dto.setName("测试流媒体节点");
        dto.setHost("192.168.1.100");
        dto.setSecret("test_secret");
        dto.setEnabled(true);
        dto.setHookEnabled(true);
        dto.setWeight(0);
        dto.setStatus(1);
        dto.setKeepalive(System.currentTimeMillis());
        dto.setDescription("测试节点");
        dto.setCreateTime(LocalDateTime.now());
        dto.setUpdateTime(LocalDateTime.now());
        return dto;
    }

    /**
     * 创建测试用的MediaNodeDO
     */
    private MediaNodeDO createTestMediaNodeDO() {
        MediaNodeDO node = new MediaNodeDO();
        node.setId(TEST_ID);
        node.setServerId(TEST_SERVER_ID);
        node.setName("测试流媒体节点");
        node.setHost("192.168.1.100");
        node.setSecret("test_secret");
        node.setEnabled(true);
        node.setHookEnabled(true);
        node.setWeight(0);
        node.setStatus(1);
        node.setKeepalive(System.currentTimeMillis());
        node.setDescription("测试节点");
        node.setCreateTime(LocalDateTime.now());
        node.setUpdateTime(LocalDateTime.now());
        return node;
    }

    @Test
    public void testCreateMediaNode_Success() {
        // Given
        when(mediaNodeService.getOne(any(QueryWrapper.class))).thenReturn(null);
        when(mediaNodeAssembler.toMediaNodeDO(testMediaNodeDTO)).thenReturn(testMediaNodeDO);
        when(mediaNodeService.save(any(MediaNodeDO.class))).thenAnswer(invocation -> {
            MediaNodeDO savedNode = invocation.getArgument(0);
            savedNode.setId(TEST_ID); // 模拟数据库自动生成主键的行为
            return true;
        });

        // When
        Long result = mediaNodeManager.createMediaNode(testMediaNodeDTO);

        // Then
        assertNotNull(result);
        assertEquals(TEST_ID, result);
        verify(mediaNodeService).save(any(MediaNodeDO.class));
        log.info("testCreateMediaNode_Success passed");
    }

    @Test
    public void testCreateMediaNode_ServerIdAlreadyExists() {
        // Given
        when(mediaNodeService.getOne(any(QueryWrapper.class))).thenReturn(testMediaNodeDO);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            mediaNodeManager.createMediaNode(testMediaNodeDTO);
        });

        assertTrue(exception.getMessage().contains("节点ID已存在"));
        log.info("testCreateMediaNode_ServerIdAlreadyExists passed");
    }

    @Test
    public void testCreateMediaNode_NullDTO() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            mediaNodeManager.createMediaNode(null);
        });
        log.info("testCreateMediaNode_NullDTO passed");
    }

    @Test
    public void testCreateMediaNode_NullServerId() {
        // Given
        testMediaNodeDTO.setServerId(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            mediaNodeManager.createMediaNode(testMediaNodeDTO);
        });
        log.info("testCreateMediaNode_NullServerId passed");
    }

    @Test
    public void testBatchCreateMediaNode_Success() {
        // Given
        MediaNodeDTO dto2 = createTestMediaNodeDTO();
        dto2.setServerId("TEST_SERVER_002");
        List<MediaNodeDTO> dtoList = Arrays.asList(testMediaNodeDTO, dto2);

        when(mediaNodeService.getOne(any(QueryWrapper.class))).thenReturn(null);
        when(mediaNodeAssembler.toMediaNodeDO(any(MediaNodeDTO.class))).thenReturn(testMediaNodeDO);
        when(mediaNodeService.save(any(MediaNodeDO.class))).thenAnswer(invocation -> {
            MediaNodeDO savedNode = invocation.getArgument(0);
            savedNode.setId(TEST_ID); // 模拟数据库自动生成主键的行为
            return true;
        });

        // When
        int result = mediaNodeManager.batchCreateMediaNode(dtoList);

        // Then
        assertEquals(2, result);
        verify(mediaNodeService, times(2)).save(any(MediaNodeDO.class));
        log.info("testBatchCreateMediaNode_Success passed");
    }

    @Test
    public void testBatchCreateMediaNode_EmptyList() {
        // When
        int result = mediaNodeManager.batchCreateMediaNode(Collections.emptyList());

        // Then
        assertEquals(0, result);
        log.info("testBatchCreateMediaNode_EmptyList passed");
    }

    @Test
    public void testUpdateMediaNode_Success() {
        // Given
        when(mediaNodeService.getById(TEST_ID)).thenReturn(testMediaNodeDO);
        when(mediaNodeAssembler.toMediaNodeDO(testMediaNodeDTO)).thenReturn(testMediaNodeDO);
        when(mediaNodeService.updateById(any(MediaNodeDO.class))).thenReturn(true);

        // When
        Long result = mediaNodeManager.updateMediaNode(testMediaNodeDTO);

        // Then
        assertNotNull(result);
        assertEquals(TEST_ID, result);
        verify(mediaNodeService).updateById(any(MediaNodeDO.class));
        log.info("testUpdateMediaNode_Success passed");
    }

    @Test
    public void testUpdateMediaNode_NodeNotExists() {
        // Given
        when(mediaNodeAssembler.toMediaNodeDO(testMediaNodeDTO)).thenReturn(testMediaNodeDO);
        when(mediaNodeService.getById(TEST_ID)).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            mediaNodeManager.updateMediaNode(testMediaNodeDTO);
        });

        assertTrue(exception.getMessage().contains("节点不存在"));
        log.info("testUpdateMediaNode_NodeNotExists passed");
    }

    @Test
    public void testGetByServerId_Success() {
        // Given
        when(mediaNodeService.getOne(any(QueryWrapper.class))).thenReturn(testMediaNodeDO);

        // When
        MediaNodeDO result = mediaNodeManager.getByServerId(TEST_SERVER_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_SERVER_ID, result.getServerId());
        log.info("testGetByServerId_Success passed");
    }

    @Test
    public void testGetByServerId_NullServerId() {
        // When
        MediaNodeDO result = mediaNodeManager.getByServerId(null);

        // Then
        assertNull(result);
        log.info("testGetByServerId_NullServerId passed");
    }

    @Test
    public void testPageQuerySimple_Success() {
        // Given
        int page = 1;
        int size = 10;

        Page<MediaNodeDO> mockPage = new Page<>(page, size);
        mockPage.setRecords(Arrays.asList(testMediaNodeDO));
        mockPage.setTotal(1L);
        mockPage.setCurrent(page);
        mockPage.setSize(size);
        mockPage.setPages(1L);

        when(mediaNodeService.page(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);
        when(mediaNodeAssembler.toMediaNodeDTOList(anyList())).thenReturn(Arrays.asList(testMediaNodeDTO));

        // When
        Page<MediaNodeDTO> result = mediaNodeManager.pageQuerySimple(page, size);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getTotal());
        log.info("testPageQuerySimple_Success passed");
    }

    @Test
    public void testUpdateNodeStatus_Success() {
        // Given
        Integer newStatus = 1;
        Long keepalive = System.currentTimeMillis();
        when(mediaNodeService.getOne(any(QueryWrapper.class))).thenReturn(testMediaNodeDO);
        when(mediaNodeService.getById(TEST_ID)).thenReturn(testMediaNodeDO); // 添加这个 mock，因为 updateMediaNodeInternal 会调用
                                                                             // getById 验证节点存在
        when(mediaNodeService.updateById(any(MediaNodeDO.class))).thenReturn(true);

        // When
        mediaNodeManager.updateNodeStatus(TEST_SERVER_ID, newStatus, keepalive);

        // Then
        verify(mediaNodeService).updateById(any(MediaNodeDO.class));
        log.info("testUpdateNodeStatus_Success passed");
    }

    @Test
    public void testGetEnabledNodes_Success() {
        // Given
        List<MediaNodeDO> nodeList = Arrays.asList(testMediaNodeDO);
        when(mediaNodeService.list(any(QueryWrapper.class))).thenReturn(nodeList);
        when(mediaNodeAssembler.toMediaNodeDTOList(nodeList)).thenReturn(Arrays.asList(testMediaNodeDTO));

        // When
        List<MediaNodeDTO> result = mediaNodeManager.getEnabledNodes();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        log.info("testGetEnabledNodes_Success passed");
    }

    @Test
    public void testGetOnlineNodes_Success() {
        // Given
        List<MediaNodeDO> nodeList = Arrays.asList(testMediaNodeDO);
        when(mediaNodeService.list(any(QueryWrapper.class))).thenReturn(nodeList);
        when(mediaNodeAssembler.toMediaNodeDTOList(nodeList)).thenReturn(Arrays.asList(testMediaNodeDTO));

        // When
        List<MediaNodeDTO> result = mediaNodeManager.getOnlineNodes();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        log.info("testGetOnlineNodes_Success passed");
    }

    @Test
    public void testDeleteMediaNodeById_Success() {
        // Given
        when(mediaNodeService.getById(TEST_ID)).thenReturn(testMediaNodeDO);
        when(mediaNodeService.removeById(TEST_ID)).thenReturn(true);

        // 使用真实的缓存管理器，无需Mock
        // Cache cache = mock(Cache.class);
        // when(cacheManager.getCache("mediaNode")).thenReturn(cache);

        // When
        boolean result = mediaNodeManager.deleteMediaNodeById(TEST_ID);

        // Then
        assertTrue(result);
        verify(mediaNodeService).removeById(TEST_ID);
        log.info("testDeleteMediaNodeById_Success passed");
    }

    @Test
    public void testDeleteMediaNodeByServerId_Success() {
        // Given
        when(mediaNodeService.getOne(any(QueryWrapper.class))).thenReturn(testMediaNodeDO);
        when(mediaNodeService.getById(TEST_ID)).thenReturn(testMediaNodeDO); // 添加这个 mock，因为 deleteMediaNodeInternal 会调用
                                                                             // getById 验证节点存在
        when(mediaNodeService.removeById(TEST_ID)).thenReturn(true);

        // When
        boolean result = mediaNodeManager.deleteMediaNodeByServerId(TEST_SERVER_ID);

        // Then
        assertTrue(result);
        verify(mediaNodeService).removeById(TEST_ID);
        log.info("testDeleteMediaNodeByServerId_Success passed");
    }

    @Test
    public void testSaveOrUpdateNodeStatus_NewNode() {
        // Given
        String apiSecret = "test_secret";
        Long keepalive = System.currentTimeMillis();
        when(mediaNodeService.getOne(any(QueryWrapper.class))).thenReturn(null);
        when(mediaNodeService.save(any(MediaNodeDO.class))).thenAnswer(invocation -> {
            MediaNodeDO savedNode = invocation.getArgument(0);
            savedNode.setId(TEST_ID); // 模拟数据库自动生成主键的行为
            return true;
        });

        // When
        Long result = mediaNodeManager.saveOrUpdateNodeStatus(TEST_SERVER_ID, apiSecret, keepalive, "192.168.1.100", "测试节点");

        // Then
        assertNotNull(result);
        assertEquals(TEST_ID, result);
        verify(mediaNodeService).save(any(MediaNodeDO.class));
        log.info("testSaveOrUpdateNodeStatus_NewNode passed");
    }

    @Test
    public void testSaveOrUpdateNodeStatus_ExistingNode() {
        // Given
        String apiSecret = "test_secret";
        Long keepalive = System.currentTimeMillis();
        when(mediaNodeService.getOne(any(QueryWrapper.class))).thenReturn(testMediaNodeDO);
        when(mediaNodeService.getById(TEST_ID)).thenReturn(testMediaNodeDO); // 添加这个 mock，因为 updateMediaNodeInternal 会调用
                                                                             // getById 验证节点存在
        when(mediaNodeService.updateById(any(MediaNodeDO.class))).thenReturn(true);

        // When
        Long result = mediaNodeManager.saveOrUpdateNodeStatus(TEST_SERVER_ID, apiSecret, keepalive, "192.168.1.100", "测试节点");

        // Then
        assertEquals(TEST_ID, result);
        verify(mediaNodeService).updateById(any(MediaNodeDO.class));
        log.info("testSaveOrUpdateNodeStatus_ExistingNode passed");
    }

    @Test
    public void testGetDTOByServerId_Success() {
        // Given
        when(mediaNodeService.getOne(any(QueryWrapper.class))).thenReturn(testMediaNodeDO);
        when(mediaNodeAssembler.toMediaNodeDTO(testMediaNodeDO)).thenReturn(testMediaNodeDTO);

        // When
        MediaNodeDTO result = mediaNodeManager.getDTOByServerId(TEST_SERVER_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_SERVER_ID, result.getServerId());
        log.info("testGetDTOByServerId_Success passed");
    }

    @Test
    public void testListMediaNodeDTO_Success() {
        // Given
        MediaNodeDO queryNode = new MediaNodeDO();
        queryNode.setStatus(1);

        List<MediaNodeDO> nodeList = Arrays.asList(testMediaNodeDO);
        when(mediaNodeService.list(any(QueryWrapper.class))).thenReturn(nodeList);
        when(mediaNodeAssembler.toMediaNodeDTOList(nodeList)).thenReturn(Arrays.asList(testMediaNodeDTO));

        // When
        List<MediaNodeDTO> result = mediaNodeManager.listMediaNodeDTO(queryNode);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        log.info("testListMediaNodeDTO_Success passed");
    }

    @Test
    public void testBatchDeleteMediaNode_Success() {
        // Given
        List<Long> ids = Arrays.asList(TEST_ID);

        // Mock getById方法返回存在的节点（batchDeleteMediaNode内部会通过deleteMediaNodeInternal调用getById检查节点是否存在）
        when(mediaNodeService.getById(TEST_ID)).thenReturn(testMediaNodeDO);
        when(mediaNodeService.removeById(TEST_ID)).thenReturn(true);

        // When
        int result = mediaNodeManager.batchDeleteMediaNode(ids);

        // Then
        assertEquals(1, result);
        verify(mediaNodeService).removeById(TEST_ID);
        log.info("testBatchDeleteMediaNode_Success passed");
    }

    @Test
    public void testBatchDeleteMediaNode_NodeNotExists() {
        // Given
        Long nonExistentId = 999L;
        List<Long> ids = Arrays.asList(nonExistentId);

        // Mock getById方法返回null（节点不存在）
        when(mediaNodeService.getById(nonExistentId)).thenReturn(null);

        // When
        int result = mediaNodeManager.batchDeleteMediaNode(ids);

        // Then
        assertEquals(0, result); // 期望删除0个节点，因为节点不存在
        verify(mediaNodeService, never()).removeById(nonExistentId); // 验证没有调用删除方法
        log.info("testBatchDeleteMediaNode_NodeNotExists passed");
    }

    @Test
    public void testBatchDeleteMediaNode_EmptyList() {
        // When
        int result = mediaNodeManager.batchDeleteMediaNode(Collections.emptyList());

        // Then
        assertEquals(0, result);
        log.info("testBatchDeleteMediaNode_EmptyList passed");
    }

    @Test
    public void testBatchDeleteMediaNode_NullList() {
        // When
        int result = mediaNodeManager.batchDeleteMediaNode(null);

        // Then
        assertEquals(0, result);
        log.info("testBatchDeleteMediaNode_NullList passed");
    }

    @Test
    public void testUpdateNodeOffline_Success() {
        // Given
        when(mediaNodeService.getOne(any(QueryWrapper.class))).thenReturn(testMediaNodeDO);
        when(mediaNodeService.getById(TEST_ID)).thenReturn(testMediaNodeDO); // 添加这个 mock，因为 updateMediaNodeInternal 会调用
                                                                             // getById 验证节点存在
        when(mediaNodeService.updateById(any(MediaNodeDO.class))).thenReturn(true);

        // When
        mediaNodeManager.updateNodeOffline(TEST_SERVER_ID);

        // Then
        verify(mediaNodeService).updateById(any(MediaNodeDO.class));
        log.info("testUpdateNodeOffline_Success passed");
    }
}