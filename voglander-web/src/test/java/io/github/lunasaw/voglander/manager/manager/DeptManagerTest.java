package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.manager.domaon.dto.DeptDTO;
import io.github.lunasaw.voglander.manager.service.DeptService;
import io.github.lunasaw.voglander.repository.entity.DeptDO;
import lombok.extern.slf4j.Slf4j;

/**
 * DeptManager集成测试类
 * 使用真实的数据库和完整的Spring容器
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
public class DeptManagerTest extends BaseTest {

    private final String TEST_DEPT_NAME = "测试部门_" + System.currentTimeMillis();
    private final String TEST_DEPT_CODE = "TEST_DEPT_" + System.currentTimeMillis();

    @Autowired
    private DeptManager  deptManager;

    @Autowired
    private DeptService  deptService;

    @BeforeEach
    public void setUp() {
        // 清理测试数据
        cleanUpTestData();
        log.debug("Test setup completed - data cleaned up");
    }

    @AfterEach
    public void tearDown() {
        // 测试结束后清理数据
        cleanUpTestData();
        log.debug("Test teardown completed - data cleaned up");
    }

    /**
     * 清理测试数据
     */
    private void cleanUpTestData() {
        try {
            // 清理所有测试部门数据
            LambdaQueryWrapper<DeptDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.like(DeptDO::getDeptName, "测试部门_")
                .or()
                .like(DeptDO::getDeptCode, "TEST_DEPT_");
            deptService.remove(queryWrapper);
        } catch (Exception e) {
            log.debug("Failed to clean up test data: {}", e.getMessage());
        }
    }

    /**
     * 创建并插入测试部门
     */
    private DeptDTO createAndInsertTestDept() {
        DeptDTO deptDTO = new DeptDTO();
        deptDTO.setDeptName(TEST_DEPT_NAME);
        deptDTO.setDeptCode(TEST_DEPT_CODE);
        deptDTO.setParentId(0L);
        deptDTO.setStatus(1);
        deptDTO.setSortOrder(1);
        deptDTO.setRemark("测试部门描述");

        // 通过Manager创建部门
        Long deptId = deptManager.createDept(deptDTO);
        assertNotNull(deptId);

        // 返回创建的部门信息
        return deptManager.getDeptById(deptId);
    }

    @Test
    public void testGetAllDepts_Success() {
        // Given - 创建测试部门
        DeptDTO createdDept = createAndInsertTestDept();

        // When
        List<DeptDTO> result = deptManager.getAllDepts();

        // Then
        assertNotNull(result);
        assertTrue(result.size() > 0);

        // 验证我们创建的部门在列表中
        boolean found = result.stream()
            .anyMatch(dept -> dept.getDeptName().equals(TEST_DEPT_NAME));
        assertTrue(found, "Created department should be in the list");

        log.info("testGetAllDepts_Success passed");
    }

    @Test
    public void testBuildDeptTree_Success() {
        // Given - 创建测试部门
        DeptDTO createdDept = createAndInsertTestDept();
        List<DeptDTO> deptList = List.of(createdDept);

        // When
        List<DeptDTO> result = deptManager.buildDeptTree(deptList);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(createdDept.getDeptName(), result.get(0).getDeptName());

        log.info("testBuildDeptTree_Success passed");
    }

    @Test
    public void testGetDeptById_Success() {
        // Given - 创建测试部门
        DeptDTO createdDept = createAndInsertTestDept();

        // When
        DeptDTO result = deptManager.getDeptById(createdDept.getId());

        // Then
        assertNotNull(result);
        assertEquals(createdDept.getId(), result.getId());
        assertEquals(TEST_DEPT_NAME, result.getDeptName());
        assertEquals(TEST_DEPT_CODE, result.getDeptCode());

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
        DeptDTO deptDTO = new DeptDTO();
        deptDTO.setDeptName(TEST_DEPT_NAME);
        deptDTO.setDeptCode(TEST_DEPT_CODE);
        deptDTO.setParentId(0L);
        deptDTO.setStatus(1);
        deptDTO.setSortOrder(1);
        deptDTO.setRemark("测试部门描述");

        // When
        Long result = deptManager.createDept(deptDTO);

        // Then
        assertNotNull(result);
        assertTrue(result > 0);

        // 验证部门是否创建成功
        DeptDTO createdDept = deptManager.getDeptById(result);
        assertNotNull(createdDept);
        assertEquals(TEST_DEPT_NAME, createdDept.getDeptName());
        assertEquals(TEST_DEPT_CODE, createdDept.getDeptCode());

        log.info("testCreateDept_Success passed - Department created with ID: {}", result);
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
        DeptDTO deptDTO = new DeptDTO();
        deptDTO.setDeptName("");
        deptDTO.setDeptCode(TEST_DEPT_CODE);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            deptManager.createDept(deptDTO);
        });
        log.info("testCreateDept_BlankDeptName passed");
    }

    @Test
    public void testCreateDept_DeptNameExists() {
        // Given - 先创建一个部门
        DeptDTO firstDept = createAndInsertTestDept();

        // 尝试创建同名部门
        DeptDTO duplicateDept = new DeptDTO();
        duplicateDept.setDeptName(TEST_DEPT_NAME); // 同样的名称
        duplicateDept.setDeptCode("DIFFERENT_CODE");
        duplicateDept.setParentId(0L);
        duplicateDept.setStatus(1);

        // When & Then
        assertThrows(ServiceException.class, () -> {
            deptManager.createDept(duplicateDept);
        });
        log.info("testCreateDept_DeptNameExists passed");
    }

    @Test
    public void testUpdateDept_Success() {
        // Given - 创建测试部门
        DeptDTO createdDept = createAndInsertTestDept();

        // 修改部门信息
        String newName = "更新后的部门名称_" + System.currentTimeMillis();
        createdDept.setDeptName(newName);
        createdDept.setRemark("更新后的描述");

        // When
        boolean result = deptManager.updateDept(createdDept.getId(), createdDept);

        // Then
        assertTrue(result);

        // 验证更新是否成功
        DeptDTO updatedDept = deptManager.getDeptById(createdDept.getId());
        assertEquals(newName, updatedDept.getDeptName());
        assertEquals("更新后的描述", updatedDept.getRemark());

        log.info("testUpdateDept_Success passed");
    }

    @Test
    public void testUpdateDept_DeptNotExists() {
        // Given
        DeptDTO deptDTO = new DeptDTO();
        deptDTO.setDeptName(TEST_DEPT_NAME);
        Long nonExistentId = 99999L;

        // When & Then
        assertThrows(ServiceException.class, () -> {
            deptManager.updateDept(nonExistentId, deptDTO);
        });
        log.info("testUpdateDept_DeptNotExists passed");
    }

    @Test
    public void testDeleteDept_Success() {
        // Given - 创建测试部门
        DeptDTO createdDept = createAndInsertTestDept();

        // When
        boolean result = deptManager.deleteDept(createdDept.getId());

        // Then
        assertTrue(result);

        // 验证部门是否被删除
        assertNull(deptManager.getDeptById(createdDept.getId()));

        log.info("testDeleteDept_Success passed");
    }

    @Test
    public void testDeleteDept_DeptNotExists() {
        // Given
        Long nonExistentId = 99999L;

        // When & Then
        assertThrows(ServiceException.class, () -> {
            deptManager.deleteDept(nonExistentId);
        });
        log.info("testDeleteDept_DeptNotExists passed");
    }

    @Test
    public void testIsDeptNameExists_True() {
        // Given - 创建测试部门
        DeptDTO createdDept = createAndInsertTestDept();

        // When
        boolean result = deptManager.isDeptNameExists(TEST_DEPT_NAME, null);

        // Then
        assertTrue(result);
        log.info("testIsDeptNameExists_True passed");
    }

    @Test
    public void testIsDeptNameExists_False() {
        // Given
        String nonExistentName = "不存在的部门名称_" + System.currentTimeMillis();

        // When
        boolean result = deptManager.isDeptNameExists(nonExistentName, null);

        // Then
        assertFalse(result);
        log.info("testIsDeptNameExists_False passed");
    }

    @Test
    public void testIsDeptNameExists_BlankName() {
        // When
        boolean result = deptManager.isDeptNameExists("", null);

        // Then
        assertFalse(result);
        log.info("testIsDeptNameExists_BlankName passed");
    }

    @Test
    public void testIsDeptNameExists_WithExcludeId() {
        // Given - 创建测试部门
        DeptDTO createdDept = createAndInsertTestDept();

        // When - 检查同名但排除自身ID
        boolean result = deptManager.isDeptNameExists(TEST_DEPT_NAME, createdDept.getId());

        // Then - 应该返回false，因为排除了自身
        assertFalse(result);
        log.info("testIsDeptNameExists_WithExcludeId passed");
    }
}