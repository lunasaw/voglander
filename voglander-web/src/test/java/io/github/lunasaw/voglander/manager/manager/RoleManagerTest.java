package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.domaon.dto.RoleDTO;
import io.github.lunasaw.voglander.manager.service.RoleService;
import io.github.lunasaw.voglander.repository.entity.RoleDO;
import lombok.extern.slf4j.Slf4j;

/**
 * RoleManager集成测试类
 * 按照Manager层测试标准规范实现
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
public class RoleManagerTest extends BaseTest {

    // 测试数据常量 - 使用有意义的业务数据
    private static final String  TEST_ROLE_NAME        = "测试角色";
    private static final String  TEST_ROLE_NAME_2      = "测试角色2";
    private static final String  TEST_ROLE_DESCRIPTION = "测试角色描述";
    private static final Integer TEST_STATUS           = 1;
    private static final Integer TEST_STATUS_DISABLED  = 0;
    private static final Long    TEST_MENU_ID          = 100L;

    // 被测试对象 - 使用真实注入
    @Autowired
    private RoleManager          roleManager;

    @Autowired
    private CacheManager         cacheManager;

    // 基础Service层 - 继承IService<DO>的服务使用真实注入
    @Autowired
    private RoleService          roleService;

    // 注意：RoleAssembler使用静态方法，不需要模拟

    // 测试数据对象
    private RoleDO               testRoleDO;
    private RoleDTO              testRoleDTO;

    @BeforeEach
    public void setUp() {
        log.info("开始设置测试数据");

        // 1. 清理数据库中的测试数据
        cleanupTestData();

        // 2. 创建测试用的DO和DTO对象
        testRoleDO = createTestRoleDO();
        testRoleDTO = createTestRoleDTO();

        log.info("测试数据设置完成");
    }

    @AfterEach
    public void tearDown() {
        log.info("开始清理测试数据");
        cleanupTestData();
        log.info("测试数据清理完成");
    }

    /**
     * 清理测试数据 - 必须实现
     */
    private void cleanupTestData() {
        try {
            // 删除测试数据 - 覆盖所有测试数据模式
            QueryWrapper<RoleDO> wrapper = new QueryWrapper<>();
            wrapper.in("role_name", TEST_ROLE_NAME, TEST_ROLE_NAME_2)
                .or().in("description", TEST_ROLE_DESCRIPTION, TEST_ROLE_DESCRIPTION + "2");
            roleService.remove(wrapper);

            // 清理缓存
            if (cacheManager.getCache("role") != null) {
                cacheManager.getCache("role").clear();
            }
        } catch (Exception e) {
            log.warn("清理测试数据时发生异常: {}", e.getMessage());
        }
    }

    /**
     * 创建测试用的RoleDO
     */
    private RoleDO createTestRoleDO() {
        RoleDO role = new RoleDO();
        role.setRoleName(TEST_ROLE_NAME);
        role.setStatus(TEST_STATUS);
        role.setDescription(TEST_ROLE_DESCRIPTION);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        return role;
    }

    /**
     * 创建测试用的RoleDTO
     */
    private RoleDTO createTestRoleDTO() {
        RoleDTO dto = new RoleDTO();
        dto.setRoleName(TEST_ROLE_NAME);
        dto.setStatus(TEST_STATUS);
        dto.setDescription(TEST_ROLE_DESCRIPTION);
        dto.setPermissions(Arrays.asList(TEST_MENU_ID));
        return dto;
    }

    // ================================
    // 核心模板方法测试
    // ================================

    @Test
    public void testAdd_Success() {
        // Given
        RoleDTO dto = createTestRoleDTO();

        // When
        Long id = roleManager.add(dto);

        // Then
        assertNotNull(id);
        assertTrue(id > 0);

        // 验证数据库中的记录
        RoleDO saved = roleService.getById(id);
        assertNotNull(saved);
        assertEquals(TEST_ROLE_NAME, saved.getRoleName());
        assertEquals(TEST_STATUS, saved.getStatus());

        log.info("新增测试通过，ID: {}", id);
    }

    @Test
    public void testAdd_ValidationError() {
        // Test null DTO
        assertThrows(IllegalArgumentException.class, () -> roleManager.add(null));

        // Test empty role name
        RoleDTO dto = createTestRoleDTO();
        dto.setRoleName("");
        assertThrows(IllegalArgumentException.class, () -> roleManager.add(dto));

            // Test null role name
            dto.setRoleName(null);
            assertThrows(IllegalArgumentException.class, () -> roleManager.add(dto));

            log.info("新增参数校验测试通过");
        }

        @Test
        public void testUpdate_Success() {
            // Given - 先创建一个角色
            Long id = roleManager.add(testRoleDTO);
            assertNotNull(id);

            RoleDTO queryDTO = new RoleDTO();
            queryDTO.setId(id);

            RoleDTO updateDTO = new RoleDTO();
            updateDTO.setRoleName(TEST_ROLE_NAME_2);
            updateDTO.setDescription(TEST_ROLE_DESCRIPTION + "2");

        // When
        Long updatedId = roleManager.update(queryDTO, updateDTO);

        // Then
        assertNotNull(updatedId);
        assertEquals(id, updatedId);

        // 验证数据库更新
        RoleDO updated = roleService.getById(id);
        assertNotNull(updated);
        assertEquals(TEST_ROLE_NAME_2, updated.getRoleName());

        log.info("条件更新测试通过，ID: {}", updatedId);
    }

    @Test
    public void testUpdateById_Success() {
        // Given - 先创建一个角色
        Long id = roleManager.add(testRoleDTO);
        assertNotNull(id);

        RoleDTO updateDTO = new RoleDTO();
        updateDTO.setRoleName(TEST_ROLE_NAME_2);

        // When
        Long updatedId = roleManager.updateById(id, updateDTO);

        // Then
        assertNotNull(updatedId);
        assertEquals(id, updatedId);

        // 验证数据库更新
        RoleDO updated = roleService.getById(id);
        assertNotNull(updated);
        assertEquals(TEST_ROLE_NAME_2, updated.getRoleName());

        log.info("ID更新测试通过，ID: {}", updatedId);
    }

    @Test
    public void testUpdate_NotFound() {
        // Given
        RoleDTO queryDTO = new RoleDTO();
        queryDTO.setId(999L);

        RoleDTO updateDTO = new RoleDTO();
        updateDTO.setRoleName(TEST_ROLE_NAME_2);

        // When & Then
        assertThrows(RuntimeException.class, () -> roleManager.update(queryDTO, updateDTO));

        log.info("更新不存在记录测试通过");
    }

    @Test
    public void testGet_Success() {
        // Given - 先创建一个角色
        Long id = roleManager.add(testRoleDTO);
        assertNotNull(id);

        RoleDTO queryDTO = new RoleDTO();
        queryDTO.setId(id);

        // When
        RoleDTO result = roleManager.get(queryDTO);

        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals(TEST_ROLE_NAME, result.getRoleName());

        log.info("查询测试通过，ID: {}", id);
    }

    @Test
    public void testGetById_Success() {
        // Given - 先创建一个角色
        Long id = roleManager.add(testRoleDTO);
        assertNotNull(id);

        // When
        RoleDTO result = roleManager.getById(id);

        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals(TEST_ROLE_NAME, result.getRoleName());

        log.info("ID查询测试通过，ID: {}", id);
    }

    @Test
    public void testGet_NotFound() {
        // Given
        RoleDTO queryDTO = new RoleDTO();
        queryDTO.setId(999L);

        // When
        RoleDTO result = roleManager.get(queryDTO);

        // Then
        assertNull(result);

        log.info("查询不存在记录测试通过");
    }

    @Test
    public void testDeleteOne_Success() {
        // Given - 先创建一个角色
        Long id = roleManager.add(testRoleDTO);
        assertNotNull(id);

        RoleDTO deleteDTO = new RoleDTO();
        deleteDTO.setId(id);

        // When
        Boolean result = roleManager.deleteOne(deleteDTO);

        // Then
        assertTrue(result);

        // 验证数据库中记录已删除
        RoleDO deleted = roleService.getById(id);
        assertNull(deleted);

        log.info("删除测试通过，ID: {}", id);
    }

    @Test
    public void testDeleteOne_NotFound() {
        // Given
        RoleDTO deleteDTO = new RoleDTO();
        deleteDTO.setId(999L);

        // When
        Boolean result = roleManager.deleteOne(deleteDTO);

        // Then
        assertFalse(result);

        log.info("删除不存在记录测试通过");
    }

    @Test
    public void testDeleteBatch_Success() {
        // Given - 先创建多个角色
        RoleDTO dto1 = createTestRoleDTO();
        dto1.setRoleName(TEST_ROLE_NAME + "1");
        Long id1 = roleManager.add(dto1);

        RoleDTO dto2 = createTestRoleDTO();
        dto2.setRoleName(TEST_ROLE_NAME + "2");
        Long id2 = roleManager.add(dto2);

        // 构建删除条件
        RoleDTO deleteDTO = new RoleDTO();
        deleteDTO.setStatus(TEST_STATUS);

        // When
        Boolean result = roleManager.deleteBatch(deleteDTO);

        // Then
        assertTrue(result);

        // 验证数据库中记录已删除
        RoleDO deleted1 = roleService.getById(id1);
        RoleDO deleted2 = roleService.getById(id2);
        assertNull(deleted1);
        assertNull(deleted2);

        log.info("批量删除测试通过");
    }

    @Test
    public void testGetPage_Success() {
        // Given - 先创建多个角色
        for (int i = 1; i <= 3; i++) {
            RoleDTO dto = createTestRoleDTO();
            dto.setRoleName(TEST_ROLE_NAME + i);
            roleManager.add(dto);
        }

        RoleDTO queryDTO = new RoleDTO();
        queryDTO.setStatus(TEST_STATUS);

        // When
        Page<RoleDTO> result = roleManager.getPage(queryDTO, 1, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.getTotal() >= 3);
        assertTrue(result.getRecords().size() >= 3);

        log.info("分页查询测试通过，总记录数: {}", result.getTotal());
    }

    @Test
    public void testGetPage_ValidationError() {
        // Test invalid page
        RoleDTO queryDTO = new RoleDTO();
        assertThrows(IllegalArgumentException.class, () -> roleManager.getPage(queryDTO, 0, 10));

        // Test invalid size
        assertThrows(IllegalArgumentException.class, () -> roleManager.getPage(queryDTO, 1, 0));

        assertThrows(IllegalArgumentException.class, () -> roleManager.getPage(queryDTO, 1, 1001));

        log.info("分页参数校验测试通过");
    }

    @Test
    public void testCompleteLifecycle() {
        // 1. 创建
        Long id = roleManager.add(testRoleDTO);
        assertNotNull(id);
        log.info("1. 创建成功: {}", id);

        // 2. 查询验证
        RoleDTO created = roleManager.getById(id);
        assertNotNull(created);
        assertEquals(TEST_ROLE_NAME, created.getRoleName());
        log.info("2. 查询验证成功");

        // 3. 更新
        RoleDTO updateDTO = new RoleDTO();
        updateDTO.setRoleName(TEST_ROLE_NAME_2);
        Long updatedId = roleManager.updateById(id, updateDTO);
        assertNotNull(updatedId);
        assertEquals(id, updatedId);
        log.info("3. 更新成功");

        // 4. 验证更新
        RoleDTO updated = roleManager.getById(id);
        assertEquals(TEST_ROLE_NAME_2, updated.getRoleName());
        log.info("4. 更新验证成功");

        // 5. 删除
        RoleDTO deleteDTO = new RoleDTO();
        deleteDTO.setId(id);
        Boolean deleted = roleManager.deleteOne(deleteDTO);
        assertTrue(deleted);
        log.info("5. 删除成功");

        // 6. 验证删除
        RoleDTO deletedResult = roleManager.getById(id);
        assertNull(deletedResult);
        log.info("6. 删除验证成功");

        log.info("完整生命周期测试通过");
    }

    // ================================
    // 业务扩展方法测试（兼容性测试）
    // ================================

    @Test
    public void testCreateRole_Success() {
        // Given
        RoleDTO dto = createTestRoleDTO();

        // When
        boolean result = roleManager.createRole(dto);

        // Then
        assertTrue(result);

        // 验证角色已创建
        List<RoleDO> roles = roleService.list();
        assertTrue(roles.stream().anyMatch(r -> TEST_ROLE_NAME.equals(r.getRoleName())));

        log.info("创建角色业务方法测试通过");
    }

    @Test
    public void testUpdateRole_Success() {
        // Given - 先创建角色
        boolean created = roleManager.createRole(testRoleDTO);
        assertTrue(created);

        // 找到创建的角色
        List<RoleDO> roles = roleService.list();
        RoleDO createdRole = roles.stream()
            .filter(r -> TEST_ROLE_NAME.equals(r.getRoleName()))
            .findFirst()
            .orElse(null);
        assertNotNull(createdRole);

        RoleDTO updateDTO = new RoleDTO();
        updateDTO.setRoleName(TEST_ROLE_NAME_2);
        updateDTO.setDescription(TEST_ROLE_DESCRIPTION + "2");

        // When
        boolean result = roleManager.updateRole(createdRole.getId(), updateDTO);

        // Then
        assertTrue(result);

        // 验证更新
        RoleDO updated = roleService.getById(createdRole.getId());
        assertEquals(TEST_ROLE_NAME_2, updated.getRoleName());

        log.info("更新角色业务方法测试通过");
    }

    @Test
    public void testDeleteRole_Success() {
        // Given - 先创建角色
        boolean created = roleManager.createRole(testRoleDTO);
        assertTrue(created);

        // 找到创建的角色
        List<RoleDO> roles = roleService.list();
        RoleDO createdRole = roles.stream()
            .filter(r -> TEST_ROLE_NAME.equals(r.getRoleName()))
            .findFirst()
            .orElse(null);
        assertNotNull(createdRole);

        // When
        boolean result = roleManager.deleteRole(createdRole.getId());

        // Then
        assertTrue(result);

        // 验证删除
        RoleDO deleted = roleService.getById(createdRole.getId());
        assertNull(deleted);

        log.info("删除角色业务方法测试通过");
    }

    @Test
    public void testGetRoleList_Success() {
        // Given - 先创建角色
        roleManager.createRole(testRoleDTO);

        RoleDTO queryDTO = new RoleDTO();
        queryDTO.setPageNum(1);
        queryDTO.setPageSize(10);
        queryDTO.setStatus(TEST_STATUS);

        // When
        var result = roleManager.getRoleList(queryDTO);

        // Then
        assertNotNull(result);
        assertTrue(result.getTotal() >= 1);

        log.info("角色列表查询测试通过，总记录数: {}", result.getTotal());
    }

    @Test
    public void testGetRoleById_Success() {
        // Given - 先创建角色
        roleManager.createRole(testRoleDTO);

        // 找到创建的角色
        List<RoleDO> roles = roleService.list();
        RoleDO createdRole = roles.stream()
            .filter(r -> TEST_ROLE_NAME.equals(r.getRoleName()))
            .findFirst()
            .orElse(null);
        assertNotNull(createdRole);

        // When
        RoleDTO result = roleManager.getRoleById(createdRole.getId());

        // Then
        assertNotNull(result);
        assertEquals(createdRole.getId(), result.getId());
        assertEquals(TEST_ROLE_NAME, result.getRoleName());
        assertNotNull(result.getPermissions());

        log.info("根据ID查询角色测试通过");
    }

    @Test
    public void testGetRolesByUserId_Success() {
        // Given
        Long userId = 1L;

        // When
        List<RoleDTO> result = roleManager.getRolesByUserId(userId);

        // Then
        assertNotNull(result);

        log.info("根据用户ID查询角色测试通过");
    }
}