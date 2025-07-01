package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import io.github.lunasaw.voglander.common.enums.export.ExportTaskTypeEnums;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.config.TestConfig;
import io.github.lunasaw.voglander.manager.assembler.ExportTaskAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.ExportTaskDTO;
import io.github.lunasaw.voglander.manager.service.ExportTaskService;
import io.github.lunasaw.voglander.repository.entity.ExportTaskDO;
import lombok.extern.slf4j.Slf4j;

/**
 * ExportTaskManager单元测试类
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
@SpringBootTest(classes = TestConfig.class)
@TestPropertySource(properties = {"spring.cache.type=simple"})
public class ExportTaskManagerTest {

    private final Long          TEST_ID         = 1L;
    private final Long          TEST_BIZ_ID     = 100L;
    private final String        TEST_NAME       = "测试导出任务";
    private final Integer       TEST_TYPE       = ExportTaskTypeEnums.DEVICE_LIST.getValue();
    private final String        TEST_APPLY_USER = "testuser";
    private final String        TEST_URL        = "http://example.com/export.xlsx";

    @Autowired
    private ExportTaskManager   exportTaskManager;

    @MockitoBean
    private ExportTaskService   exportTaskService;

    @MockitoBean
    private ExportTaskAssembler exportTaskAssembler;

    private ExportTaskDTO       testExportTaskDTO;
    private ExportTaskDO        testExportTaskDO;

    @BeforeEach
    public void setUp() {
        // 初始化测试数据
        testExportTaskDTO = createTestExportTaskDTO();
        testExportTaskDO = createTestExportTaskDO();
    }

    /**
     * 创建测试用的ExportTaskDTO
     */
    private ExportTaskDTO createTestExportTaskDTO() {
        ExportTaskDTO dto = new ExportTaskDTO();
        dto.setId(TEST_ID);
        dto.setBizId(TEST_BIZ_ID);
        dto.setName(TEST_NAME);
        dto.setType(TEST_TYPE);
        dto.setApplyUser(TEST_APPLY_USER);
        dto.setStatus(0); // 处理中
        dto.setDeleted(0);
        dto.setExpired(0);
        dto.setGmtCreate(new Date());
        dto.setGmtUpdate(new Date());
        return dto;
    }

    /**
     * 创建测试用的ExportTaskDO
     */
    private ExportTaskDO createTestExportTaskDO() {
        ExportTaskDO task = new ExportTaskDO();
        task.setId(TEST_ID);
        task.setBizId(TEST_BIZ_ID);
        task.setName(TEST_NAME);
        task.setType(TEST_TYPE);
        task.setApplyUser(TEST_APPLY_USER);
        task.setStatus(0); // 处理中
        task.setDeleted(0);
        task.setExpired(0);
        task.setGmtCreate(new Date());
        task.setGmtUpdate(new Date());
        return task;
    }

    @Test
    public void testCreateExportTask_Success() {
        // Given
        when(exportTaskService.getOne(any(QueryWrapper.class))).thenReturn(null); // 业务ID不存在
        when(exportTaskAssembler.toCreateExportTaskDO(testExportTaskDTO)).thenReturn(testExportTaskDO);
        when(exportTaskService.save(any(ExportTaskDO.class))).thenReturn(true);

        // When
        Long result = exportTaskManager.createExportTask(testExportTaskDTO);

        // Then
        assertNotNull(result);
        assertEquals(TEST_ID, result);
        verify(exportTaskService).save(any(ExportTaskDO.class));
        log.info("testCreateExportTask_Success passed");
    }

    @Test
    public void testCreateExportTask_NullDTO() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            exportTaskManager.createExportTask(null);
        });
        log.info("testCreateExportTask_NullDTO passed");
    }

    @Test
    public void testCreateExportTask_NullBizId() {
        // Given
        testExportTaskDTO.setBizId(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            exportTaskManager.createExportTask(testExportTaskDTO);
        });
        log.info("testCreateExportTask_NullBizId passed");
    }

    @Test
    public void testCreateExportTask_BizIdExists() {
        // Given
        when(exportTaskService.getOne(any(QueryWrapper.class))).thenReturn(testExportTaskDO); // 业务ID已存在

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            exportTaskManager.createExportTask(testExportTaskDTO);
        });

        assertTrue(exception.getMessage().contains("业务ID已存在"));
        log.info("testCreateExportTask_BizIdExists passed");
    }

    @Test
    public void testBatchCreateExportTask_Success() {
        // Given
        ExportTaskDTO dto2 = createTestExportTaskDTO();
        dto2.setBizId(101L);
        List<ExportTaskDTO> dtoList = Arrays.asList(testExportTaskDTO, dto2);

        when(exportTaskService.getOne(any(QueryWrapper.class))).thenReturn(null); // 业务ID不存在
        when(exportTaskAssembler.toCreateExportTaskDO(any(ExportTaskDTO.class))).thenReturn(testExportTaskDO);
        when(exportTaskService.save(any(ExportTaskDO.class))).thenReturn(true);

        // When
        int result = exportTaskManager.batchCreateExportTask(dtoList);

        // Then
        assertEquals(2, result);
        verify(exportTaskService, times(2)).save(any(ExportTaskDO.class));
        log.info("testBatchCreateExportTask_Success passed");
    }

    @Test
    public void testBatchCreateExportTask_EmptyList() {
        // When
        int result = exportTaskManager.batchCreateExportTask(Collections.emptyList());

        // Then
        assertEquals(0, result);
        log.info("testBatchCreateExportTask_EmptyList passed");
    }

    @Test
    public void testBatchCreateExportTask_NullList() {
        // When
        int result = exportTaskManager.batchCreateExportTask(null);

        // Then
        assertEquals(0, result);
        log.info("testBatchCreateExportTask_NullList passed");
    }

    @Test
    public void testUpdateExportTask_Success() {
        // Given
        when(exportTaskService.getById(TEST_ID)).thenReturn(testExportTaskDO);
        when(exportTaskAssembler.toExportTaskDTO(testExportTaskDO)).thenReturn(testExportTaskDTO);
        when(exportTaskAssembler.toUpdateExportTaskDO(testExportTaskDTO)).thenReturn(testExportTaskDO);
        when(exportTaskService.updateById(any(ExportTaskDO.class))).thenReturn(true);

        // When
        Long result = exportTaskManager.updateExportTask(testExportTaskDTO);

        // Then
        assertNotNull(result);
        assertEquals(TEST_ID, result);
        verify(exportTaskService).updateById(any(ExportTaskDO.class));
        log.info("testUpdateExportTask_Success passed");
    }

    @Test
    public void testUpdateExportTask_TaskNotExists() {
        // Given
        when(exportTaskService.getById(TEST_ID)).thenReturn(null);
        when(exportTaskAssembler.toExportTaskDTO(null)).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            exportTaskManager.updateExportTask(testExportTaskDTO);
        });

        assertTrue(exception.getMessage().contains("导出任务不存在"));
        log.info("testUpdateExportTask_TaskNotExists passed");
    }

    @Test
    public void testBatchUpdateExportTask_Success() {
        // Given
        List<ExportTaskDTO> dtoList = Arrays.asList(testExportTaskDTO);

        when(exportTaskService.getById(TEST_ID)).thenReturn(testExportTaskDO);
        when(exportTaskAssembler.toExportTaskDTO(testExportTaskDO)).thenReturn(testExportTaskDTO);
        when(exportTaskAssembler.toUpdateExportTaskDO(any(ExportTaskDTO.class))).thenReturn(testExportTaskDO);
        when(exportTaskService.updateById(any(ExportTaskDO.class))).thenReturn(true);

        // When
        int result = exportTaskManager.batchUpdateExportTask(dtoList);

        // Then
        assertEquals(1, result);
        verify(exportTaskService).updateById(any(ExportTaskDO.class));
        log.info("testBatchUpdateExportTask_Success passed");
    }

    @Test
    public void testGetByBizId_Success() {
        // Given
        when(exportTaskService.getOne(any(QueryWrapper.class))).thenReturn(testExportTaskDO);

        // When
        ExportTaskDO result = exportTaskManager.getByBizId(TEST_BIZ_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_BIZ_ID, result.getBizId());
        verify(exportTaskService).getOne(any(QueryWrapper.class));
        log.info("testGetByBizId_Success passed");
    }

    @Test
    public void testGetByBizId_NullBizId() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            exportTaskManager.getByBizId(null);
        });
        log.info("testGetByBizId_NullBizId passed");
    }

    @Test
    public void testGetDTOByBizId_Success() {
        // Given
        when(exportTaskService.getOne(any(QueryWrapper.class))).thenReturn(testExportTaskDO);
        when(exportTaskAssembler.toExportTaskDTO(testExportTaskDO)).thenReturn(testExportTaskDTO);

        // When
        ExportTaskDTO result = exportTaskManager.getDTOByBizId(TEST_BIZ_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_BIZ_ID, result.getBizId());
        log.info("testGetDTOByBizId_Success passed");
    }

    @Test
    public void testGetExportTaskDTOById_Success() {
        // Given
        when(exportTaskService.getById(TEST_ID)).thenReturn(testExportTaskDO);
        when(exportTaskAssembler.toExportTaskDTO(testExportTaskDO)).thenReturn(testExportTaskDTO);

        // When
        ExportTaskDTO result = exportTaskManager.getExportTaskDTOById(TEST_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_ID, result.getId());
        log.info("testGetExportTaskDTOById_Success passed");
    }

    @Test
    public void testGetExportTaskDTOByEntity_Success() {
        // Given
        ExportTaskDO queryTask = new ExportTaskDO();
        queryTask.setBizId(TEST_BIZ_ID);

        when(exportTaskService.getOne(any(QueryWrapper.class))).thenReturn(testExportTaskDO);
        when(exportTaskAssembler.toExportTaskDTO(testExportTaskDO)).thenReturn(testExportTaskDTO);

        // When
        ExportTaskDTO result = exportTaskManager.getExportTaskDTOByEntity(queryTask);

        // Then
        assertNotNull(result);
        assertEquals(TEST_BIZ_ID, result.getBizId());
        log.info("testGetExportTaskDTOByEntity_Success passed");
    }

    @Test
    public void testListExportTaskDTO_Success() {
        // Given
        ExportTaskDO queryTask = new ExportTaskDO();
        queryTask.setStatus(0);

        List<ExportTaskDO> taskList = Arrays.asList(testExportTaskDO);
        when(exportTaskService.list(any(QueryWrapper.class))).thenReturn(taskList);
        when(exportTaskAssembler.toExportTaskDTOList(taskList)).thenReturn(Arrays.asList(testExportTaskDTO));

        // When
        List<ExportTaskDTO> result = exportTaskManager.listExportTaskDTO(queryTask);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        log.info("testListExportTaskDTO_Success passed");
    }

    @Test
    public void testPageQuerySimple_Success() {
        // Given
        int page = 1;
        int size = 10;

        Page<ExportTaskDO> mockPage = new Page<>(page, size);
        mockPage.setRecords(Arrays.asList(testExportTaskDO));
        mockPage.setTotal(1L);
        mockPage.setCurrent(page);
        mockPage.setSize(size);
        mockPage.setPages(1L);

        when(exportTaskService.page(any(Page.class))).thenReturn(mockPage);
        when(exportTaskAssembler.toExportTaskDTOList(anyList())).thenReturn(Arrays.asList(testExportTaskDTO));

        // When
        Page<ExportTaskDTO> result = exportTaskManager.pageQuerySimple(page, size);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getTotal());
        log.info("testPageQuerySimple_Success passed");
    }

    @Test
    public void testPageQuery_Success() {
        // Given
        int page = 1;
        int size = 10;
        QueryWrapper<ExportTaskDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 0);

        Page<ExportTaskDO> mockPage = new Page<>(page, size);
        mockPage.setRecords(Arrays.asList(testExportTaskDO));
        mockPage.setTotal(1L);
        mockPage.setCurrent(page);
        mockPage.setSize(size);
        mockPage.setPages(1L);

        when(exportTaskService.page(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);
        when(exportTaskAssembler.toExportTaskDTOList(anyList())).thenReturn(Arrays.asList(testExportTaskDTO));

        // When
        Page<ExportTaskDTO> result = exportTaskManager.pageQuery(page, size, queryWrapper);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getTotal());
        log.info("testPageQuery_Success passed");
    }

    @Test
    public void testUpdateStatus_Success() {
        // Given
        Integer newStatus = 1;
        when(exportTaskService.getOne(any(QueryWrapper.class))).thenReturn(testExportTaskDO);
        when(exportTaskService.updateById(any(ExportTaskDO.class))).thenReturn(true);

        // When
        exportTaskManager.updateStatus(TEST_BIZ_ID, newStatus);

        // Then
        verify(exportTaskService).updateById(any(ExportTaskDO.class));
        log.info("testUpdateStatus_Success passed");
    }

    @Test
    public void testUpdateStatus_TaskNotExists() {
        // Given
        when(exportTaskService.getOne(any(QueryWrapper.class))).thenReturn(null);

        // When
        exportTaskManager.updateStatus(TEST_BIZ_ID, 1);

        // Then
        verify(exportTaskService, never()).updateById(any(ExportTaskDO.class));
        log.info("testUpdateStatus_TaskNotExists passed");
    }

    @Test
    public void testMarkAsCompleted_Success() {
        // Given
        when(exportTaskService.getOne(any(QueryWrapper.class))).thenReturn(testExportTaskDO);
        when(exportTaskService.updateById(any(ExportTaskDO.class))).thenReturn(true);

        // When
        exportTaskManager.markAsCompleted(TEST_BIZ_ID, TEST_URL);

        // Then
        verify(exportTaskService).updateById(any(ExportTaskDO.class));
        log.info("testMarkAsCompleted_Success passed");
    }

    @Test
    public void testMarkAsError_Success() {
        // Given
        when(exportTaskService.getOne(any(QueryWrapper.class))).thenReturn(testExportTaskDO);
        when(exportTaskService.updateById(any(ExportTaskDO.class))).thenReturn(true);

        // When
        exportTaskManager.markAsError(TEST_BIZ_ID);

        // Then
        verify(exportTaskService).updateById(any(ExportTaskDO.class));
        log.info("testMarkAsError_Success passed");
    }

    @Test
    public void testDeleteExportTask_Success() {
        // Given
        when(exportTaskService.remove(any(QueryWrapper.class))).thenReturn(true);

        // When
        Boolean result = exportTaskManager.deleteExportTask(TEST_BIZ_ID);

        // Then
        assertTrue(result);
        verify(exportTaskService).remove(any(QueryWrapper.class));
        log.info("testDeleteExportTask_Success passed");
    }

    @Test
    public void testDeleteExportTask_NullBizId() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            exportTaskManager.deleteExportTask(null);
        });
        log.info("testDeleteExportTask_NullBizId passed");
    }
}