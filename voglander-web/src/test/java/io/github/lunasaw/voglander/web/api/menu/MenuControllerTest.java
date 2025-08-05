package io.github.lunasaw.voglander.web.api.menu;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.manager.MenuManager;
import io.github.lunasaw.voglander.web.assembler.MenuWebAssembler;
import lombok.extern.slf4j.Slf4j;

/**
 * MenuController 纯单元测试
 * 不依赖Spring上下文，只关注当前控制器逻辑
 * 
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class MenuControllerTest {

    @Mock
    private MenuManager      menuManager;

    @Mock
    private MenuWebAssembler menuWebAssembler;

    @InjectMocks
    private MenuController menuController;

    @BeforeEach
    public void setUp() {
        // Initialize Mockito annotations to fix null injection issues
        org.mockito.MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testControllerInstantiation() {
        // 验证控制器能够正常实例化，依赖正确注入
        log.info("MenuController 单元测试启动成功");
        assertNotNull(menuController);
        assertNotNull(menuManager);
        assertNotNull(menuWebAssembler);
    }

    /**
     * 测试获取用户权限菜单接口 - 无效Authorization头场景
     */
    @Test
    public void testGetUserPermissions_InvalidToken() {
        // When - 无效Authorization头
        AjaxResult result = menuController.getUserPermissions("invalid_token");

        // Then
        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertEquals("请先登录", result.getMsg());

        // 验证Manager方法没有被调用
        verifyNoInteractions(menuManager);

        log.info("testGetUserPermissions_InvalidToken passed");
    }

    /**
     * 测试获取用户权限菜单接口 - 空Authorization头场景
     */
    @Test
    public void testGetUserPermissions_NullToken() {
        // When - 空Authorization头
        AjaxResult result = menuController.getUserPermissions(null);

        // Then
        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertEquals("请先登录", result.getMsg());

        log.info("testGetUserPermissions_NullToken passed");
    }

    /**
     * 测试获取菜单列表接口 - 验证方法调用
     */
    @Test
    public void testGetMenuList_MethodInvocation() {
        // Given - 不设置具体的返回值，只验证方法是否被调用
        when(menuManager.getAllMenus()).thenReturn(java.util.Collections.emptyList());
        when(menuManager.buildMenuTree(any())).thenReturn(java.util.Collections.emptyList());
        when(menuWebAssembler.toRespList(any())).thenReturn(java.util.Collections.emptyList());

        // When
        AjaxResult result = menuController.getMenuList();

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());

        // 验证Manager方法被调用
        verify(menuManager).getAllMenus();
        verify(menuManager).buildMenuTree(any());
        verify(menuWebAssembler).toRespList(any());

        log.info("testGetMenuList_MethodInvocation passed - 方法调用验证成功");
    }
}