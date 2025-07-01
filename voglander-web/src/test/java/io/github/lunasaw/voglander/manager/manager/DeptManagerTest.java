package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.config.TestConfig;
import io.github.lunasaw.voglander.manager.assembler.DeptAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.DeptDTO;
import io.github.lunasaw.voglander.manager.service.DeptService;
import io.github.lunasaw.voglander.repository.entity.DeptDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * DeptManager单元测试类
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
@SpringBootTest(classes = TestConfig.class)
@TestPropertySource(properties = {"spring.cache.type=simple"})
public class DeptManagerTest {

    private final Long   TEST_DEPT_ID   = 1L;
    private final String TEST_DEPT_NAME = "测试部门";
    private final String TEST_DEPT_CODE = "TEST_DEPT";

    @Autowired
    private DeptManager  deptManager;

    @MockitoBean
    private DeptService  deptService;

    private DeptDTO      testDeptDTO;
    private DeptDO       testDeptDO;

    @BeforeEach
    public void setUp() {
        // 初始化测试数据
        testDeptDTO = createTestDeptDTO();
        testDeptDO = createTestDeptDO();
    }

    /**
     * 创建测试用的DeptDTO
     */
    private DeptDTO createTestDeptDTO() {
        DeptDTO dto = new DeptDTO();
        dto.setId(TEST_DEPT_ID);
        dto.setDeptName(TEST_DEPT_NAME);
        dto.setDeptCode(TEST_DEPT_CODE);
        dto.setParentId(0L);
        dto.setStatus(1);
        dto.setSortOrder(1);
        dto.setRemark("测试部门描述");
        dto.setCreateTime(LocalDateTime.now());
        return dto;
    }

    /**
     * 创建测试用的DeptDO
     */
    private DeptDO createTestDeptDO() {
        DeptDO dept = new DeptDO();
        dept.setId(TEST_DEPT_ID);
        dept.setDeptName(TEST_DEPT_NAME);
        dept.setDeptCode(TEST_DEPT_CODE);
        dept.setParentId(0L);
        dept.setStatus(1);
        dept.setSortOrder(1);
        dept.setRemark("测试部门描述");
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());
        return dept;
    }

    @Test
    public void testGetAllDepts_Success() {
        // Given
        List<DeptDO> deptList = Arrays.asList(testDeptDO);
        when(deptService.list(any(LambdaQueryWrapper.class))).thenReturn(deptList);

        // Mock静态方法调用
        try (var mockedDeptAssembler = mockStatic(DeptAssembler.class)) {
            mockedDeptAssembler.when(() -> DeptAssembler.toDTOList(deptList)).thenReturn(Arrays.asList(testDeptDTO));
        }

        // When
        List<DeptDTO> result = deptManager.getAllDepts();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(deptService).list(any(LambdaQueryWrapper.class));
        log.info("testGetAllDepts_Success passed");
    }

    @Test
    public void testBuildDeptTree_Success() {
        // Given
        List<DeptDTO> deptList = Arrays.asList(testDeptDTO);

        // Mock静态方法调用
        try (var mockedDeptAssembler = mockStatic(DeptAssembler.class)) {
            mockedDeptAssembler.when(() -> DeptAssembler.buildDeptTree(deptList)).thenReturn(deptList);
        }

        // When
        List<DeptDTO> result = deptManager.buildDeptTree(deptList);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        log.info("testBuildDeptTree_Success passed");
    }

    @Test
    public void testGetDeptById_Success() {
        // Given
        when(deptService.getById(TEST_DEPT_ID)).thenReturn(testDeptDO);

        // Mock静态方法调用
        try (var mockedDeptAssembler = mockStatic(DeptAssembler.class)) {
            mockedDeptAssembler.when(() -> DeptAssembler.toDTO(testDeptDO)).thenReturn(testDeptDTO);
        }

        // When
        DeptDTO result = deptManager.getDeptById(TEST_DEPT_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_DEPT_ID, result.getId());
        verify(deptService).getById(TEST_DEPT_ID);
        log.info("testGetDeptById_Success passed");
    }

    @Test
    public void testGetDeptById_NullId() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            deptManager.getDeptById(null);
        });
        log.info("testGetDeptById_NullId passed");
    }

    @Test
    public void testCreateDept_Success() {
        // Given
        when(deptService.count(any(LambdaQueryWrapper.class))).thenReturn(0L); // 部门名称不存在
        when(deptService.save(any(DeptDO.class))).thenReturn(true);

        // Mock静态方法调用
        try (var mockedDeptAssembler = mockStatic(DeptAssembler.class)) {
            mockedDeptAssembler.when(() -> DeptAssembler.toDO(testDeptDTO)).thenReturn(testDeptDO);

            // When
            Long result = deptManager.createDept(testDeptDTO);

            // Then
            assertNotNull(result);
            assertEquals(TEST_DEPT_ID, result);
            verify(deptService).save(any(DeptDO.class));
            log.info("testCreateDept_Success passed");
        }
    }

    @Test
    public void testCreateDept_NullDTO() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            deptManager.createDept(null);
        });
        log.info("testCreateDept_NullDTO passed");
    }

    @Test
    public void testCreateDept_BlankDeptName() {
        // Given
        testDeptDTO.setDeptName("");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            deptManager.createDept(testDeptDTO);
        });
        log.info("testCreateDept_BlankDeptName passed");
    }

    @Test
    public void testCreateDept_DeptNameExists() {
        // Given
        when(deptService.count(any(LambdaQueryWrapper.class))).thenReturn(1L); // 部门名称已存在

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            deptManager.createDept(testDeptDTO);
        });
        log.info("testCreateDept_DeptNameExists passed");
    }

    @Test
    public void testUpdateDept_Success() {
        // Given
        when(deptService.getById(TEST_DEPT_ID)).thenReturn(testDeptDO);
        when(deptService.count(any(LambdaQueryWrapper.class))).thenReturn(0L); // 部门名称不重复
        when(deptService.updateById(any(DeptDO.class))).thenReturn(true);

        // Mock静态方法调用
        try (var mockedDeptAssembler = mockStatic(DeptAssembler.class)) {
            mockedDeptAssembler.when(() -> DeptAssembler.toDO(testDeptDTO)).thenReturn(testDeptDO);

            // When
            boolean result = deptManager.updateDept(TEST_DEPT_ID, testDeptDTO);

            // Then
            assertTrue(result);
            verify(deptService).updateById(any(DeptDO.class));
            log.info("testUpdateDept_Success passed");
        }
    }

    @Test
    public void testUpdateDept_DeptNotExists() {
        // Given
        when(deptService.getById(TEST_DEPT_ID)).thenReturn(null);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            deptManager.updateDept(TEST_DEPT_ID, testDeptDTO);
        });
        log.info("testUpdateDept_DeptNotExists passed");
    }

    @Test
    public void testUpdateDept_DeptNameExists() {
        // Given
        when(deptService.getById(TEST_DEPT_ID)).thenReturn(testDeptDO);
        when(deptService.count(any(LambdaQueryWrapper.class))).thenReturn(1L); // 部门名称已存在

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            deptManager.updateDept(TEST_DEPT_ID, testDeptDTO);
        });
        log.info("testUpdateDept_DeptNameExists passed");
    }

    @Test
    public void testDeleteDept_Success() {
        // Given
        when(deptService.getById(TEST_DEPT_ID)).thenReturn(testDeptDO);
        when(deptService.count(any(LambdaQueryWrapper.class))).thenReturn(0L); // 没有子部门
        when(deptService.removeById(TEST_DEPT_ID)).thenReturn(true);

        // When
        boolean result = deptManager.deleteDept(TEST_DEPT_ID);

        // Then
        assertTrue(result);
        verify(deptService).removeById(TEST_DEPT_ID);
        log.info("testDeleteDept_Success passed");
    }

    @Test
    public void testDeleteDept_DeptNotExists() {
        // Given
        when(deptService.getById(TEST_DEPT_ID)).thenReturn(null);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            deptManager.deleteDept(TEST_DEPT_ID);
        });
        log.info("testDeleteDept_DeptNotExists passed");
    }

    @Test
    public void testDeleteDept_HasChildren() {
        // Given
        when(deptService.getById(TEST_DEPT_ID)).thenReturn(testDeptDO);
        when(deptService.count(any(LambdaQueryWrapper.class))).thenReturn(1L); // 有子部门

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            deptManager.deleteDept(TEST_DEPT_ID);
        });
        log.info("testDeleteDept_HasChildren passed");
    }

    @Test
    public void testIsDeptNameExists_True() {
        // Given
        when(deptService.count(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // When
        boolean result = deptManager.isDeptNameExists(TEST_DEPT_NAME, null);

        // Then
        assertTrue(result);
        verify(deptService).count(any(LambdaQueryWrapper.class));
        log.info("testIsDeptNameExists_True passed");
    }

    @Test
    public void testIsDeptNameExists_False() {
        // Given
        when(deptService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // When
        boolean result = deptManager.isDeptNameExists(TEST_DEPT_NAME, null);

        // Then
        assertFalse(result);
        verify(deptService).count(any(LambdaQueryWrapper.class));
        log.info("testIsDeptNameExists_False passed");
    }

    @Test
    public void testIsDeptNameExists_BlankName() {
        // When
        boolean result = deptManager.isDeptNameExists("", null);

        // Then
        assertFalse(result);
        verify(deptService, never()).count(any(LambdaQueryWrapper.class));
        log.info("testIsDeptNameExists_BlankName passed");
    }

    @Test
    public void testIsDeptNameExists_WithExcludeId() {
        // Given
        when(deptService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // When
        boolean result = deptManager.isDeptNameExists(TEST_DEPT_NAME, TEST_DEPT_ID);

        // Then
        assertFalse(result);
        verify(deptService).count(any(LambdaQueryWrapper.class));
        log.info("testIsDeptNameExists_WithExcludeId passed");
    }
}