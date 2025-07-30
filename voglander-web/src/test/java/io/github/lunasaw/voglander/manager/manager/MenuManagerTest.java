package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.domaon.dto.MenuDTO;
import io.github.lunasaw.voglander.manager.service.MenuService;
import io.github.lunasaw.voglander.repository.entity.MenuDO;
import lombok.extern.slf4j.Slf4j;

/**
 * MenuManager集成测试类
 * 继承BaseTest使用真实的Spring容器和数据库进行集成测试
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
public class MenuManagerTest extends BaseTest {

    private final Long   TEST_MENU_ID   = 1L;
    private final String TEST_MENU_NAME = "测试菜单";
    private final String TEST_MENU_CODE = "test_menu";
    private final String TEST_PATH      = "/test";

    @Autowired
    private MenuManager  menuManager;

    @Autowired
    private MenuService  menuService;

    private MenuDTO      testMenuDTO;
    private MenuDO       testMenuDO;

    @BeforeEach
    public void setUp() {
        // 调用父类的setUp方法
        super.baseSetUp();
        
        // 清理测试数据
        menuService.remove(null); // 清空所有菜单数据
        
        // 初始化测试数据
        testMenuDTO = createTestMenuDTO();
        testMenuDO = createTestMenuDO();
    }

    /**
     * 创建测试用的MenuDTO
     */
    private MenuDTO createTestMenuDTO() {
        MenuDTO dto = new MenuDTO();
        dto.setId(null); // 创建时ID为null
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
        menu.setId(null); // 创建时ID为null，由数据库自动生成
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
    public void testGetAllMenus_Success() {
        // Given - 创建测试菜单数据
        menuService.save(testMenuDO);

        // When
        List<MenuDTO> result = menuManager.getAllMenus();

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        log.info("testGetAllMenus_Success passed");
    }

    @Test
    public void testGetMenuById_Success() {
        // Given - 保存测试菜单
        menuService.save(testMenuDO);

        // When
        MenuDTO result = menuManager.getMenuById(testMenuDO.getId());

        // Then
        assertNotNull(result);
        assertEquals(testMenuDO.getId(), result.getId());
        assertEquals(TEST_MENU_NAME, result.getMenuName());
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
        // When
        Long result = menuManager.createMenu(testMenuDTO);

        // Then
        assertNotNull(result);
        
        // 验证数据库中确实有数据
        MenuDO savedMenu = menuService.getById(result);
        assertNotNull(savedMenu);
        assertEquals(TEST_MENU_NAME, savedMenu.getMenuName());
        assertEquals(TEST_MENU_CODE, savedMenu.getMenuCode());
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
        // Given - 先创建一个菜单
        menuService.save(testMenuDO);
        
        // 创建另一个同名菜单
        MenuDTO duplicateMenu = createTestMenuDTO();
        duplicateMenu.setMenuCode("different_code");

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            menuManager.createMenu(duplicateMenu);
        });
        log.info("testCreateMenu_MenuNameExists passed");
    }

    @Test
    public void testUpdateMenu_Success() {
        // Given - 先保存菜单
        menuService.save(testMenuDO);
        
        // 修改菜单信息
        testMenuDTO.setId(testMenuDO.getId());
        testMenuDTO.setMenuName("更新后的菜单名");
        testMenuDTO.setPath("/updated-path");

        // When
        boolean result = menuManager.updateMenu(testMenuDO.getId(), testMenuDTO);

        // Then
        assertTrue(result);
        
        // 验证更新结果
        MenuDO updatedMenu = menuService.getById(testMenuDO.getId());
        assertEquals("更新后的菜单名", updatedMenu.getMenuName());
        assertEquals("/updated-path", updatedMenu.getPath());
        log.info("testUpdateMenu_Success passed");
    }

    @Test
    public void testUpdateMenu_MenuNotExists() {
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            menuManager.updateMenu(999L, testMenuDTO);
        });
        log.info("testUpdateMenu_MenuNotExists passed");
    }

    @Test
    public void testDeleteMenu_Success() {
        // Given - 保存菜单
        menuService.save(testMenuDO);
        Long menuId = testMenuDO.getId();

        // When
        boolean result = menuManager.deleteMenu(menuId);

        // Then
        assertTrue(result);
        
        // 验证菜单已被删除
        MenuDO deletedMenu = menuService.getById(menuId);
        assertNull(deletedMenu);
        log.info("testDeleteMenu_Success passed");
    }

    @Test
    public void testDeleteMenu_MenuNotExists() {
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            menuManager.deleteMenu(999L);
        });
        log.info("testDeleteMenu_MenuNotExists passed");
    }

    @Test
    public void testIsMenuNameExists_True() {
        // Given - 保存菜单
        menuService.save(testMenuDO);

        // When
        boolean result = menuManager.isMenuNameExists(TEST_MENU_NAME, null);

        // Then
        assertTrue(result);
        log.info("testIsMenuNameExists_True passed");
    }

    @Test
    public void testIsMenuNameExists_False() {
        // When
        boolean result = menuManager.isMenuNameExists("不存在的菜单名", null);

        // Then
        assertFalse(result);
        log.info("testIsMenuNameExists_False passed");
    }

    @Test
    public void testIsMenuNameExists_BlankName() {
        // When
        boolean result = menuManager.isMenuNameExists("", null);

        // Then
        assertFalse(result);
        log.info("testIsMenuNameExists_BlankName passed");
    }

    @Test
    public void testIsMenuPathExists_True() {
        // Given - 保存菜单
        menuService.save(testMenuDO);

        // When
        boolean result = menuManager.isMenuPathExists(TEST_PATH, null);

        // Then
        assertTrue(result);
        log.info("testIsMenuPathExists_True passed");
    }

    @Test
    public void testIsMenuPathExists_False() {
        // When
        boolean result = menuManager.isMenuPathExists("/non-existent-path", null);

        // Then
        assertFalse(result);
        log.info("testIsMenuPathExists_False passed");
    }

    @Test
    public void testIsMenuPathExists_BlankPath() {
        // When
        boolean result = menuManager.isMenuPathExists("", null);

        // Then
        assertFalse(result);
        log.info("testIsMenuPathExists_BlankPath passed");
    }

    @Test
    public void testIsMenuPathExists_WithExcludeId() {
        // Given - 保存菜单
        menuService.save(testMenuDO);

        // When - 排除当前菜单ID，应该返回false
        boolean result = menuManager.isMenuPathExists(TEST_PATH, testMenuDO.getId());

        // Then
        assertFalse(result);
        log.info("testIsMenuPathExists_WithExcludeId passed");
    }
}