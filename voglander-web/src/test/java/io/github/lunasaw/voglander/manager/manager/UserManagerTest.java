package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.config.TestConfig;
import io.github.lunasaw.voglander.repository.entity.MenuDO;
import io.github.lunasaw.voglander.repository.entity.RoleMenuDO;
import io.github.lunasaw.voglander.repository.entity.UserDO;
import io.github.lunasaw.voglander.repository.entity.UserRoleDO;
import io.github.lunasaw.voglander.repository.mapper.MenuMapper;
import io.github.lunasaw.voglander.repository.mapper.RoleMenuMapper;
import io.github.lunasaw.voglander.repository.mapper.UserMapper;
import io.github.lunasaw.voglander.repository.mapper.UserRoleMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * UserManager单元测试类
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
@SpringBootTest(classes = TestConfig.class)
@TestPropertySource(properties = {"spring.cache.type=simple"})
public class UserManagerTest {

    private final String   TEST_USERNAME = "testuser";
    private final Long     TEST_USER_ID  = 1L;
    private final Long     TEST_ROLE_ID  = 10L;
    private final Long     TEST_MENU_ID  = 100L;

    @Autowired
    private UserManager    userManager;

    @MockitoBean
    private UserMapper     userMapper;

    @MockitoBean
    private UserRoleMapper userRoleMapper;

    @MockitoBean
    private RoleMenuMapper roleMenuMapper;

    @MockitoBean
    private MenuMapper     menuMapper;

    private UserDO         testUserDO;
    private UserRoleDO     testUserRoleDO;
    private RoleMenuDO     testRoleMenuDO;
    private MenuDO         testMenuDO;

    @BeforeEach
    public void setUp() {
        // 初始化测试数据
        testUserDO = createTestUserDO();
        testUserRoleDO = createTestUserRoleDO();
        testRoleMenuDO = createTestRoleMenuDO();
        testMenuDO = createTestMenuDO();
    }

    /**
     * 创建测试用的UserDO
     */
    private UserDO createTestUserDO() {
        UserDO user = new UserDO();
        user.setId(TEST_USER_ID);
        user.setUsername(TEST_USERNAME);
        user.setNickname("测试用户");
        user.setEmail("test@example.com");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        return user;
    }

    /**
     * 创建测试用的UserRoleDO
     */
    private UserRoleDO createTestUserRoleDO() {
        UserRoleDO userRole = new UserRoleDO();
        userRole.setUserId(TEST_USER_ID);
        userRole.setRoleId(TEST_ROLE_ID);
        userRole.setCreateTime(LocalDateTime.now());
        return userRole;
    }

    /**
     * 创建测试用的RoleMenuDO
     */
    private RoleMenuDO createTestRoleMenuDO() {
        RoleMenuDO roleMenu = new RoleMenuDO();
        roleMenu.setRoleId(TEST_ROLE_ID);
        roleMenu.setMenuId(TEST_MENU_ID);
        roleMenu.setCreateTime(LocalDateTime.now());
        return roleMenu;
    }

    /**
     * 创建测试用的MenuDO
     */
    private MenuDO createTestMenuDO() {
        MenuDO menu = new MenuDO();
        menu.setId(TEST_MENU_ID);
        menu.setMenuName("测试菜单");
        menu.setMenuCode("test_menu");
        menu.setPermission("test:view");
        menu.setStatus(1);
        menu.setSortOrder(1);
        menu.setCreateTime(LocalDateTime.now());
        menu.setUpdateTime(LocalDateTime.now());
        return menu;
    }

    @Test
    public void testGetUserByUsername_Success() {
        // Given
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUserDO);

        // When
        UserDO result = userManager.getUserByUsername(TEST_USERNAME);

        // Then
        assertNotNull(result);
        assertEquals(TEST_USERNAME, result.getUsername());
        verify(userMapper).selectOne(any(LambdaQueryWrapper.class));
        log.info("testGetUserByUsername_Success passed");
    }

    @Test
    public void testGetUserByUsername_BlankUsername() {
        // When
        UserDO result = userManager.getUserByUsername("");

        // Then
        assertNull(result);
        verify(userMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        log.info("testGetUserByUsername_BlankUsername passed");
    }

    @Test
    public void testGetUserByUsername_NullUsername() {
        // When
        UserDO result = userManager.getUserByUsername(null);

        // Then
        assertNull(result);
        verify(userMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        log.info("testGetUserByUsername_NullUsername passed");
    }

    @Test
    public void testGetUserRoleIds_Success() {
        // Given
        List<UserRoleDO> userRoleList = Arrays.asList(testUserRoleDO);
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(userRoleList);

        // When
        List<Long> result = userManager.getUserRoleIds(TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_ROLE_ID, result.get(0));
        verify(userRoleMapper).selectList(any(LambdaQueryWrapper.class));
        log.info("testGetUserRoleIds_Success passed");
    }

    @Test
    public void testGetUserRoleIds_NullUserId() {
        // When
        List<Long> result = userManager.getUserRoleIds(null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRoleMapper, never()).selectList(any(LambdaQueryWrapper.class));
        log.info("testGetUserRoleIds_NullUserId passed");
    }

    @Test
    public void testGetUserRoleIds_EmptyResult() {
        // Given
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        // When
        List<Long> result = userManager.getUserRoleIds(TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRoleMapper).selectList(any(LambdaQueryWrapper.class));
        log.info("testGetUserRoleIds_EmptyResult passed");
    }

    @Test
    public void testGetUserPermissions_Success() {
        // Given
        List<UserRoleDO> userRoleList = Arrays.asList(testUserRoleDO);
        List<RoleMenuDO> roleMenuList = Arrays.asList(testRoleMenuDO);
        List<MenuDO> menuList = Arrays.asList(testMenuDO);

        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(userRoleList);
        when(roleMenuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(roleMenuList);
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(menuList);

        // When
        List<String> result = userManager.getUserPermissions(TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test:view", result.get(0));
        log.info("testGetUserPermissions_Success passed");
    }

    @Test
    public void testGetUserPermissions_NullUserId() {
        // When
        List<String> result = userManager.getUserPermissions(null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        log.info("testGetUserPermissions_NullUserId passed");
    }

    @Test
    public void testGetUserPermissions_NoRoles() {
        // Given
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        // When
        List<String> result = userManager.getUserPermissions(TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        log.info("testGetUserPermissions_NoRoles passed");
    }

    @Test
    public void testGetUserMenus_Success() {
        // Given
        List<UserRoleDO> userRoleList = Arrays.asList(testUserRoleDO);
        List<RoleMenuDO> roleMenuList = Arrays.asList(testRoleMenuDO);
        List<MenuDO> menuList = Arrays.asList(testMenuDO);

        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(userRoleList);
        when(roleMenuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(roleMenuList);
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(menuList);

        // When
        List<MenuDO> result = userManager.getUserMenus(TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_MENU_ID, result.get(0).getId());
        log.info("testGetUserMenus_Success passed");
    }

    @Test
    public void testGetUserMenus_NullUserId() {
        // When
        List<MenuDO> result = userManager.getUserMenus(null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        log.info("testGetUserMenus_NullUserId passed");
    }

    @Test
    public void testDeleteUserRolesByUserId_Success() {
        // Given
        when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        // When
        int result = userManager.deleteUserRolesByUserId(TEST_USER_ID);

        // Then
        assertEquals(1, result);
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        log.info("testDeleteUserRolesByUserId_Success passed");
    }

    @Test
    public void testDeleteUserRolesByUserId_NullUserId() {
        // When
        int result = userManager.deleteUserRolesByUserId(null);

        // Then
        assertEquals(0, result);
        verify(userRoleMapper, never()).delete(any(LambdaQueryWrapper.class));
        log.info("testDeleteUserRolesByUserId_NullUserId passed");
    }

    @Test
    public void testBatchInsertUserRoles_Success() {
        // Given
        List<Long> roleIds = Arrays.asList(TEST_ROLE_ID, 11L, 12L);
        when(userRoleMapper.insert(any(UserRoleDO.class))).thenReturn(1);

        // When
        int result = userManager.batchInsertUserRoles(TEST_USER_ID, roleIds);

        // Then
        assertEquals(3, result);
        verify(userRoleMapper, times(3)).insert(any(UserRoleDO.class));
        log.info("testBatchInsertUserRoles_Success passed");
    }

    @Test
    public void testBatchInsertUserRoles_NullUserId() {
        // Given
        List<Long> roleIds = Arrays.asList(TEST_ROLE_ID);

        // When
        int result = userManager.batchInsertUserRoles(null, roleIds);

        // Then
        assertEquals(0, result);
        verify(userRoleMapper, never()).insert(any(UserRoleDO.class));
        log.info("testBatchInsertUserRoles_NullUserId passed");
    }

    @Test
    public void testBatchInsertUserRoles_EmptyRoleIds() {
        // When
        int result = userManager.batchInsertUserRoles(TEST_USER_ID, Collections.emptyList());

        // Then
        assertEquals(0, result);
        verify(userRoleMapper, never()).insert(any(UserRoleDO.class));
        log.info("testBatchInsertUserRoles_EmptyRoleIds passed");
    }

    @Test
    public void testBatchInsertUserRoles_NullRoleIds() {
        // When
        int result = userManager.batchInsertUserRoles(TEST_USER_ID, null);

        // Then
        assertEquals(0, result);
        verify(userRoleMapper, never()).insert(any(UserRoleDO.class));
        log.info("testBatchInsertUserRoles_NullRoleIds passed");
    }

    @Test
    public void testUpdateUserRoles_Success() {
        // Given
        List<Long> roleIds = Arrays.asList(TEST_ROLE_ID, 11L);
        when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(userRoleMapper.insert(any(UserRoleDO.class))).thenReturn(1);

        // When
        boolean result = userManager.updateUserRoles(TEST_USER_ID, roleIds);

        // Then
        assertTrue(result);
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(userRoleMapper, times(2)).insert(any(UserRoleDO.class));
        log.info("testUpdateUserRoles_Success passed");
    }

    @Test
    public void testUpdateUserRoles_NullUserId() {
        // Given
        List<Long> roleIds = Arrays.asList(TEST_ROLE_ID);

        // When
        boolean result = userManager.updateUserRoles(null, roleIds);

        // Then
        assertFalse(result);
        verify(userRoleMapper, never()).delete(any(LambdaQueryWrapper.class));
        verify(userRoleMapper, never()).insert(any(UserRoleDO.class));
        log.info("testUpdateUserRoles_NullUserId passed");
    }

    @Test
    public void testUpdateUserRoles_EmptyRoleIds() {
        // Given
        when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        // When
        boolean result = userManager.updateUserRoles(TEST_USER_ID, Collections.emptyList());

        // Then
        assertTrue(result);
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(userRoleMapper, never()).insert(any(UserRoleDO.class));
        log.info("testUpdateUserRoles_EmptyRoleIds passed");
    }

    @Test
    public void testUpdateUserRoles_NullRoleIds() {
        // Given
        when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        // When
        boolean result = userManager.updateUserRoles(TEST_USER_ID, null);

        // Then
        assertTrue(result);
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(userRoleMapper, never()).insert(any(UserRoleDO.class));
        log.info("testUpdateUserRoles_NullRoleIds passed");
    }
}