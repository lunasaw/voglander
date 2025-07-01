package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.config.TestConfig;
import io.github.lunasaw.voglander.manager.assembler.RoleAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.RoleDTO;
import io.github.lunasaw.voglander.repository.entity.MenuDO;
import io.github.lunasaw.voglander.repository.entity.RoleDO;
import io.github.lunasaw.voglander.repository.mapper.MenuMapper;
import io.github.lunasaw.voglander.repository.mapper.RoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * RoleManager单元测试类
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
@SpringBootTest(classes = TestConfig.class)
@TestPropertySource(properties = {"spring.cache.type=simple"})
public class RoleManagerTest {

    private final Long   TEST_ROLE_ID   = 1L;
    private final Long   TEST_USER_ID   = 10L;
    private final Long   TEST_MENU_ID   = 100L;
    private final String TEST_ROLE_NAME = "测试角色";

    @Autowired
    private RoleManager  roleManager;

    @MockitoBean
    private MenuMapper   menuMapper;

    @MockitoBean
    private RoleMapper   roleMapper;

    private RoleDTO      testRoleDTO;
    private RoleDO       testRoleDO;
    private MenuDO       testMenuDO;

    @BeforeEach
    public void setUp() {
        // 初始化测试数据
        testRoleDTO = createTestRoleDTO();
        testRoleDO = createTestRoleDO();
        testMenuDO = createTestMenuDO();
    }

    /**
     * 创建测试用的RoleDTO
     */
    private RoleDTO createTestRoleDTO() {
        RoleDTO dto = new RoleDTO();
        dto.setId(TEST_ROLE_ID);
        dto.setRoleName(TEST_ROLE_NAME);
        dto.setStatus(1);
        dto.setDescription("测试角色描述");
        dto.setPageNum(1);
        dto.setPageSize(10);
        dto.setPermissions(Arrays.asList(TEST_MENU_ID));
        return dto;
    }

    /**
     * 创建测试用的RoleDO
     */
    private RoleDO createTestRoleDO() {
        RoleDO role = new RoleDO();
        role.setId(TEST_ROLE_ID);
        role.setRoleName(TEST_ROLE_NAME);
        role.setStatus(1);
        role.setDescription("测试角色描述");
        return role;
    }

    /**
     * 创建测试用的MenuDO
     */
    private MenuDO createTestMenuDO() {
        MenuDO menu = new MenuDO();
        menu.setId(TEST_MENU_ID);
        menu.setMenuName("测试菜单");
        menu.setPermission("test:view");
        return menu;
    }

    @Test
    public void testGetRoleList_Success() {
        // Given
        Page<RoleDO> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(testRoleDO));
        mockPage.setTotal(1L);
        mockPage.setCurrent(1);
        mockPage.setSize(10);
        mockPage.setPages(1L);

        when(roleMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // Mock静态方法调用
        mockStatic(RoleAssembler.class);
        when(RoleAssembler.toDTOList(anyList())).thenReturn(Arrays.asList(testRoleDTO));
        when(RoleAssembler.menuListToPermissionIds(anyList())).thenReturn(Arrays.asList(TEST_MENU_ID));

        when(menuMapper.selectMenusByRoleId(TEST_ROLE_ID)).thenReturn(Arrays.asList(testMenuDO));

        // When
        IPage<RoleDTO> result = roleManager.getRoleList(testRoleDTO);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getTotal());
        verify(roleMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        log.info("testGetRoleList_Success passed");
    }

    @Test
    public void testGetRoleById_Success() {
        // Given
        when(roleMapper.selectById(TEST_ROLE_ID)).thenReturn(testRoleDO);
        when(menuMapper.selectMenusByRoleId(TEST_ROLE_ID)).thenReturn(Arrays.asList(testMenuDO));

        // Mock静态方法调用
        mockStatic(RoleAssembler.class);
        when(RoleAssembler.toDTO(testRoleDO)).thenReturn(testRoleDTO);
        when(RoleAssembler.menuListToPermissionIds(anyList())).thenReturn(Arrays.asList(TEST_MENU_ID));

        // When
        RoleDTO result = roleManager.getRoleById(TEST_ROLE_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_ROLE_ID, result.getId());
        assertEquals(1, result.getPermissions().size());
        verify(roleMapper).selectById(TEST_ROLE_ID);
        verify(menuMapper).selectMenusByRoleId(TEST_ROLE_ID);
        log.info("testGetRoleById_Success passed");
    }

    @Test
    public void testGetRoleById_NullId() {
        // When
        RoleDTO result = roleManager.getRoleById(null);

        // Then
        assertNull(result);
        verify(roleMapper, never()).selectById(any());
        log.info("testGetRoleById_NullId passed");
    }

    @Test
    public void testGetRoleById_RoleNotExists() {
        // Given
        when(roleMapper.selectById(TEST_ROLE_ID)).thenReturn(null);

        // When
        RoleDTO result = roleManager.getRoleById(TEST_ROLE_ID);

        // Then
        assertNull(result);
        verify(roleMapper).selectById(TEST_ROLE_ID);
        verify(menuMapper, never()).selectMenusByRoleId(any());
        log.info("testGetRoleById_RoleNotExists passed");
    }

    @Test
    public void testCreateRole_Success() {
        // Given
        when(roleMapper.insert(any(RoleDO.class))).thenReturn(1);
        doNothing().when(roleMapper).batchInsertRoleMenu(anyLong(), anyList());

        // Mock静态方法调用
        mockStatic(RoleAssembler.class);
        when(RoleAssembler.createRoleDO(testRoleDTO)).thenReturn(testRoleDO);

        // When
        boolean result = roleManager.createRole(testRoleDTO);

        // Then
        assertTrue(result);
        verify(roleMapper).insert(any(RoleDO.class));
        verify(roleMapper).batchInsertRoleMenu(anyLong(), anyList());
        log.info("testCreateRole_Success passed");
    }

    @Test
    public void testCreateRole_WithoutPermissions() {
        // Given
        testRoleDTO.setPermissions(null);
        when(roleMapper.insert(any(RoleDO.class))).thenReturn(1);

        // Mock静态方法调用
        mockStatic(RoleAssembler.class);
        when(RoleAssembler.createRoleDO(testRoleDTO)).thenReturn(testRoleDO);

        // When
        boolean result = roleManager.createRole(testRoleDTO);

        // Then
        assertTrue(result);
        verify(roleMapper).insert(any(RoleDO.class));
        verify(roleMapper, never()).batchInsertRoleMenu(anyLong(), anyList());
        log.info("testCreateRole_WithoutPermissions passed");
    }

    @Test
    public void testCreateRole_Failed() {
        // Given
        when(roleMapper.insert(any(RoleDO.class))).thenReturn(0);

        // Mock静态方法调用
        mockStatic(RoleAssembler.class);
        when(RoleAssembler.createRoleDO(testRoleDTO)).thenReturn(testRoleDO);

        // When
        boolean result = roleManager.createRole(testRoleDTO);

        // Then
        assertFalse(result);
        verify(roleMapper).insert(any(RoleDO.class));
        verify(roleMapper, never()).batchInsertRoleMenu(anyLong(), anyList());
        log.info("testCreateRole_Failed passed");
    }

    @Test
    public void testUpdateRole_Success() {
        // Given
        when(roleMapper.selectById(TEST_ROLE_ID)).thenReturn(testRoleDO);
        when(roleMapper.updateById(any(RoleDO.class))).thenReturn(1);
        doNothing().when(roleMapper).deleteRoleMenuByRoleId(TEST_ROLE_ID);
        doNothing().when(roleMapper).batchInsertRoleMenu(anyLong(), anyList());

        // Mock静态方法调用
        mockStatic(RoleAssembler.class);
        doNothing().when(RoleAssembler.class);
        RoleAssembler.updateRoleDO(any(RoleDO.class), any(RoleDTO.class));

        // When
        boolean result = roleManager.updateRole(TEST_ROLE_ID, testRoleDTO);

        // Then
        assertTrue(result);
        verify(roleMapper).updateById(any(RoleDO.class));
        verify(roleMapper).deleteRoleMenuByRoleId(TEST_ROLE_ID);
        verify(roleMapper).batchInsertRoleMenu(anyLong(), anyList());
        log.info("testUpdateRole_Success passed");
    }

    @Test
    public void testUpdateRole_RoleNotExists() {
        // Given
        when(roleMapper.selectById(TEST_ROLE_ID)).thenReturn(null);

        // When
        boolean result = roleManager.updateRole(TEST_ROLE_ID, testRoleDTO);

        // Then
        assertFalse(result);
        verify(roleMapper).selectById(TEST_ROLE_ID);
        verify(roleMapper, never()).updateById(any(RoleDO.class));
        log.info("testUpdateRole_RoleNotExists passed");
    }

    @Test
    public void testUpdateRole_Failed() {
        // Given
        when(roleMapper.selectById(TEST_ROLE_ID)).thenReturn(testRoleDO);
        when(roleMapper.updateById(any(RoleDO.class))).thenReturn(0);

        // Mock静态方法调用
        mockStatic(RoleAssembler.class);
        doNothing().when(RoleAssembler.class);
        RoleAssembler.updateRoleDO(any(RoleDO.class), any(RoleDTO.class));

        // When
        boolean result = roleManager.updateRole(TEST_ROLE_ID, testRoleDTO);

        // Then
        assertFalse(result);
        verify(roleMapper).updateById(any(RoleDO.class));
        verify(roleMapper, never()).deleteRoleMenuByRoleId(anyLong());
        log.info("testUpdateRole_Failed passed");
    }

    @Test
    public void testDeleteRole_Success() {
        // Given
        when(roleMapper.selectById(TEST_ROLE_ID)).thenReturn(testRoleDO);
        doNothing().when(roleMapper).deleteRoleMenuByRoleId(TEST_ROLE_ID);
        when(roleMapper.deleteById(TEST_ROLE_ID)).thenReturn(1);

        // When
        boolean result = roleManager.deleteRole(TEST_ROLE_ID);

        // Then
        assertTrue(result);
        verify(roleMapper).deleteRoleMenuByRoleId(TEST_ROLE_ID);
        verify(roleMapper).deleteById(TEST_ROLE_ID);
        log.info("testDeleteRole_Success passed");
    }

    @Test
    public void testDeleteRole_RoleNotExists() {
        // Given
        when(roleMapper.selectById(TEST_ROLE_ID)).thenReturn(null);

        // When
        boolean result = roleManager.deleteRole(TEST_ROLE_ID);

        // Then
        assertFalse(result);
        verify(roleMapper).selectById(TEST_ROLE_ID);
        verify(roleMapper, never()).deleteById(anyLong());
        log.info("testDeleteRole_RoleNotExists passed");
    }

    @Test
    public void testDeleteRole_Failed() {
        // Given
        when(roleMapper.selectById(TEST_ROLE_ID)).thenReturn(testRoleDO);
        doNothing().when(roleMapper).deleteRoleMenuByRoleId(TEST_ROLE_ID);
        when(roleMapper.deleteById(TEST_ROLE_ID)).thenReturn(0);

        // When
        boolean result = roleManager.deleteRole(TEST_ROLE_ID);

        // Then
        assertFalse(result);
        verify(roleMapper).deleteRoleMenuByRoleId(TEST_ROLE_ID);
        verify(roleMapper).deleteById(TEST_ROLE_ID);
        log.info("testDeleteRole_Failed passed");
    }

    @Test
    public void testGetRolesByUserId_Success() {
        // Given
        List<RoleDO> roleList = Arrays.asList(testRoleDO);
        when(roleMapper.selectRolesByUserId(TEST_USER_ID)).thenReturn(roleList);

        // Mock静态方法调用
        mockStatic(RoleAssembler.class);
        when(RoleAssembler.toDTOList(roleList)).thenReturn(Arrays.asList(testRoleDTO));

        // When
        List<RoleDTO> result = roleManager.getRolesByUserId(TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(roleMapper).selectRolesByUserId(TEST_USER_ID);
        log.info("testGetRolesByUserId_Success passed");
    }
}