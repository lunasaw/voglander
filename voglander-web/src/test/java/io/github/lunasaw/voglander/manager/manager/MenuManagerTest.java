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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.config.TestConfig;
import io.github.lunasaw.voglander.manager.assembler.MenuAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.MenuDTO;
import io.github.lunasaw.voglander.manager.service.MenuService;
import io.github.lunasaw.voglander.repository.entity.MenuDO;
import lombok.extern.slf4j.Slf4j;

/**
 * MenuManager单元测试类
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
@SpringBootTest(classes = TestConfig.class)
@TestPropertySource(properties = {"spring.cache.type=simple"})
public class MenuManagerTest {

    private final Long   TEST_MENU_ID   = 1L;
    private final Long   TEST_USER_ID   = 10L;
    private final String TEST_MENU_NAME = "测试菜单";
    private final String TEST_MENU_CODE = "test_menu";
    private final String TEST_PATH      = "/test";

    @Autowired
    private MenuManager  menuManager;

    @MockitoBean
    private UserManager  userManager;

    @MockitoBean
    private MenuService  menuService;

    private MenuDTO      testMenuDTO;
    private MenuDO       testMenuDO;

    @BeforeEach
    public void setUp() {
        // 初始化测试数据
        testMenuDTO = createTestMenuDTO();
        testMenuDO = createTestMenuDO();
    }

    /**
     * 创建测试用的MenuDTO
     */
    private MenuDTO createTestMenuDTO() {
        MenuDTO dto = new MenuDTO();
        dto.setId(TEST_MENU_ID);
        dto.setMenuName(TEST_MENU_NAME);
        dto.setMenuCode(TEST_MENU_CODE);
        dto.setPath(TEST_PATH);
        dto.setParentId(0L);
        dto.setStatus(1);
        dto.setSortOrder(1);
        dto.setPermission("test:view");
        dto.setIcon("test-icon");
        return dto;
    }

    /**
     * 创建测试用的MenuDO
     */
    private MenuDO createTestMenuDO() {
        MenuDO menu = new MenuDO();
        menu.setId(TEST_MENU_ID);
        menu.setMenuName(TEST_MENU_NAME);
        menu.setMenuCode(TEST_MENU_CODE);
        menu.setPath(TEST_PATH);
        menu.setParentId(0L);
        menu.setStatus(1);
        menu.setSortOrder(1);
        menu.setPermission("test:view");
        menu.setIcon("test-icon");
        menu.setCreateTime(LocalDateTime.now());
        menu.setUpdateTime(LocalDateTime.now());
        return menu;
    }

    @Test
    public void testGetUserMenus_Success() {
        // Given
        List<MenuDO> menuDOList = Arrays.asList(testMenuDO);
        when(userManager.getUserMenus(TEST_USER_ID)).thenReturn(menuDOList);

        // Mock静态方法调用
        mockStatic(MenuAssembler.class);
        when(MenuAssembler.toDTOList(menuDOList)).thenReturn(Arrays.asList(testMenuDTO));

        // When
        List<MenuDTO> result = menuManager.getUserMenus(TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userManager).getUserMenus(TEST_USER_ID);
        log.info("testGetUserMenus_Success passed");
    }

    @Test
    public void testBuildMenuTree_Success() {
        // Given
        List<MenuDTO> menuList = Arrays.asList(testMenuDTO);

        // Mock静态方法调用
        mockStatic(MenuAssembler.class);
        when(MenuAssembler.buildMenuTree(menuList)).thenReturn(menuList);

        // When
        List<MenuDTO> result = menuManager.buildMenuTree(menuList);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        log.info("testBuildMenuTree_Success passed");
    }

    @Test
    public void testGetAllMenus_Success() {
        // Given
        List<MenuDO> menuList = Arrays.asList(testMenuDO);
        when(menuService.list(any(LambdaQueryWrapper.class))).thenReturn(menuList);

        // Mock静态方法调用
        mockStatic(MenuAssembler.class);
        when(MenuAssembler.toDTOList(menuList)).thenReturn(Arrays.asList(testMenuDTO));

        // When
        List<MenuDTO> result = menuManager.getAllMenus();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(menuService).list(any(LambdaQueryWrapper.class));
        log.info("testGetAllMenus_Success passed");
    }

    @Test
    public void testGetMenuById_Success() {
        // Given
        when(menuService.getById(TEST_MENU_ID)).thenReturn(testMenuDO);

        // Mock静态方法调用
        mockStatic(MenuAssembler.class);
        when(MenuAssembler.toDTO(testMenuDO)).thenReturn(testMenuDTO);

        // When
        MenuDTO result = menuManager.getMenuById(TEST_MENU_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_MENU_ID, result.getId());
        verify(menuService).getById(TEST_MENU_ID);
        log.info("testGetMenuById_Success passed");
    }

    @Test
    public void testGetMenuById_NullId() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            menuManager.getMenuById(null);
        });
        log.info("testGetMenuById_NullId passed");
    }

    @Test
    public void testCreateMenu_Success() {
        // Given
        when(menuService.count(any(LambdaQueryWrapper.class))).thenReturn(0L); // 菜单名称和路径不存在
        when(menuService.save(any(MenuDO.class))).thenReturn(true);

        // Mock静态方法调用
        mockStatic(MenuAssembler.class);
        when(MenuAssembler.toDO(testMenuDTO)).thenReturn(testMenuDO);

        // When
        Long result = menuManager.createMenu(testMenuDTO);

        // Then
        assertNotNull(result);
        assertEquals(TEST_MENU_ID, result);
        verify(menuService).save(any(MenuDO.class));
        log.info("testCreateMenu_Success passed");
    }

    @Test
    public void testCreateMenu_NullDTO() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            menuManager.createMenu(null);
        });
        log.info("testCreateMenu_NullDTO passed");
    }

    @Test
    public void testCreateMenu_BlankMenuName() {
        // Given
        testMenuDTO.setMenuName("");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            menuManager.createMenu(testMenuDTO);
        });
        log.info("testCreateMenu_BlankMenuName passed");
    }

    @Test
    public void testCreateMenu_BlankMenuCode() {
        // Given
        testMenuDTO.setMenuCode("");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            menuManager.createMenu(testMenuDTO);
        });
        log.info("testCreateMenu_BlankMenuCode passed");
    }

    @Test
    public void testCreateMenu_MenuNameExists() {
        // Given
        when(menuService.count(any(LambdaQueryWrapper.class))).thenReturn(1L); // 菜单名称已存在

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            menuManager.createMenu(testMenuDTO);
        });
        log.info("testCreateMenu_MenuNameExists passed");
    }

    @Test
    public void testUpdateMenu_Success() {
        // Given
        when(menuService.getById(TEST_MENU_ID)).thenReturn(testMenuDO);
        when(menuService.count(any(LambdaQueryWrapper.class))).thenReturn(0L); // 菜单名称和路径不重复
        when(menuService.updateById(any(MenuDO.class))).thenReturn(true);

        // Mock静态方法调用
        mockStatic(MenuAssembler.class);
        when(MenuAssembler.toDO(testMenuDTO)).thenReturn(testMenuDO);

        // When
        boolean result = menuManager.updateMenu(TEST_MENU_ID, testMenuDTO);

        // Then
        assertTrue(result);
        verify(menuService).updateById(any(MenuDO.class));
        log.info("testUpdateMenu_Success passed");
    }

    @Test
    public void testUpdateMenu_MenuNotExists() {
        // Given
        when(menuService.getById(TEST_MENU_ID)).thenReturn(null);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            menuManager.updateMenu(TEST_MENU_ID, testMenuDTO);
        });
        log.info("testUpdateMenu_MenuNotExists passed");
    }

    @Test
    public void testUpdateMenu_MenuNameExists() {
        // Given
        when(menuService.getById(TEST_MENU_ID)).thenReturn(testMenuDO);
        when(menuService.count(any(LambdaQueryWrapper.class))).thenReturn(1L); // 菜单名称已存在

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            menuManager.updateMenu(TEST_MENU_ID, testMenuDTO);
        });
        log.info("testUpdateMenu_MenuNameExists passed");
    }

    @Test
    public void testDeleteMenu_Success() {
        // Given
        when(menuService.getById(TEST_MENU_ID)).thenReturn(testMenuDO);
        when(menuService.count(any(LambdaQueryWrapper.class))).thenReturn(0L); // 没有子菜单
        when(menuService.removeById(TEST_MENU_ID)).thenReturn(true);

        // When
        boolean result = menuManager.deleteMenu(TEST_MENU_ID);

        // Then
        assertTrue(result);
        verify(menuService).removeById(TEST_MENU_ID);
        log.info("testDeleteMenu_Success passed");
    }

    @Test
    public void testDeleteMenu_MenuNotExists() {
        // Given
        when(menuService.getById(TEST_MENU_ID)).thenReturn(null);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            menuManager.deleteMenu(TEST_MENU_ID);
        });
        log.info("testDeleteMenu_MenuNotExists passed");
    }

    @Test
    public void testDeleteMenu_HasChildren() {
        // Given
        when(menuService.getById(TEST_MENU_ID)).thenReturn(testMenuDO);
        when(menuService.count(any(LambdaQueryWrapper.class))).thenReturn(1L); // 有子菜单

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            menuManager.deleteMenu(TEST_MENU_ID);
        });
        log.info("testDeleteMenu_HasChildren passed");
    }

    @Test
    public void testIsMenuNameExists_True() {
        // Given
        when(menuService.count(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // When
        boolean result = menuManager.isMenuNameExists(TEST_MENU_NAME, null);

        // Then
        assertTrue(result);
        verify(menuService).count(any(LambdaQueryWrapper.class));
        log.info("testIsMenuNameExists_True passed");
    }

    @Test
    public void testIsMenuNameExists_False() {
        // Given
        when(menuService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // When
        boolean result = menuManager.isMenuNameExists(TEST_MENU_NAME, null);

        // Then
        assertFalse(result);
        verify(menuService).count(any(LambdaQueryWrapper.class));
        log.info("testIsMenuNameExists_False passed");
    }

    @Test
    public void testIsMenuNameExists_BlankName() {
        // When
        boolean result = menuManager.isMenuNameExists("", null);

        // Then
        assertFalse(result);
        verify(menuService, never()).count(any(LambdaQueryWrapper.class));
        log.info("testIsMenuNameExists_BlankName passed");
    }

    @Test
    public void testIsMenuPathExists_True() {
        // Given
        when(menuService.count(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // When
        boolean result = menuManager.isMenuPathExists(TEST_PATH, null);

        // Then
        assertTrue(result);
        verify(menuService).count(any(LambdaQueryWrapper.class));
        log.info("testIsMenuPathExists_True passed");
    }

    @Test
    public void testIsMenuPathExists_False() {
        // Given
        when(menuService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // When
        boolean result = menuManager.isMenuPathExists(TEST_PATH, null);

        // Then
        assertFalse(result);
        verify(menuService).count(any(LambdaQueryWrapper.class));
        log.info("testIsMenuPathExists_False passed");
    }

    @Test
    public void testIsMenuPathExists_BlankPath() {
        // When
        boolean result = menuManager.isMenuPathExists("", null);

        // Then
        assertFalse(result);
        verify(menuService, never()).count(any(LambdaQueryWrapper.class));
        log.info("testIsMenuPathExists_BlankPath passed");
    }

    @Test
    public void testIsMenuPathExists_WithExcludeId() {
        // Given
        when(menuService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // When
        boolean result = menuManager.isMenuPathExists(TEST_PATH, TEST_MENU_ID);

        // Then
        assertFalse(result);
        verify(menuService).count(any(LambdaQueryWrapper.class));
        log.info("testIsMenuPathExists_WithExcludeId passed");
    }
}