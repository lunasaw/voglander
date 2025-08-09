package io.github.lunasaw.voglander.web.api.role;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.lunasaw.voglander.web.assembler.RoleWebAssembler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.service.RoleService;
import io.github.lunasaw.voglander.web.api.role.req.RoleQueryReq;
import lombok.extern.slf4j.Slf4j;

/**
 * RoleController 纯单元测试
 * 不依赖Spring上下文，只关注当前控制器逻辑
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class RoleControllerTest {

    @Mock
    private RoleService    roleService;

    @InjectMocks
    private RoleController roleController;

    @BeforeEach
    public void setUp() {
        // Initialize Mockito annotations to fix null injection issues
        org.mockito.MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testControllerInstantiation() {
        // 验证控制器能够正常实例化，依赖正确注入
        log.info("RoleController 单元测试启动成功");
        assertNotNull(roleController);
        assertNotNull(roleService);
    }

    /**
     * 测试根据ID获取角色详情 - 角色不存在场景
     */
    @Test
    public void testGetRoleById_NotFound() {
        // Given
        String testRoleId = "999";
        when(roleService.getRoleById(999L)).thenReturn(null);

        // When
        AjaxResult result = roleController.getRoleById(testRoleId);

        // Then
        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertEquals("角色不存在", result.getMsg());

        // 验证Service方法调用
        verify(roleService).getRoleById(999L);

        log.info("testGetRoleById_NotFound passed");
    }

    /**
     * 测试获取角色列表 - 正常请求
     */
    @Test
    public void testGetRoleList_NormalRequest() {
        // Given
        RoleQueryReq req = new RoleQueryReq();
        req.setPageNum(2);
        req.setPageSize(20);

        // Mock service call to return empty page
        com.baomidou.mybatisplus.extension.plugins.pagination.Page mockPage =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page();
        mockPage.setRecords(java.util.Collections.emptyList());
        when(roleService.getRoleList(any())).thenReturn(mockPage);

        // When
        AjaxResult result = roleController.getRoleList(req);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());

        // 验证Service方法被调用，并检查传入的DTO参数
        verify(roleService).getRoleList(argThat(dto -> {
            // 验证分页参数正确传递
            return dto.getPageNum().equals(2) && dto.getPageSize().equals(20);
        }));

        log.info("testGetRoleList_NormalRequest passed");
    }

    /**
     * 测试获取角色列表 - null请求处理
     */
    @Test
    public void testGetRoleList_NullRequest() {
        // Mock service call to return empty page
        com.baomidou.mybatisplus.extension.plugins.pagination.Page mockPage =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page();
        mockPage.setRecords(java.util.Collections.emptyList());
        when(roleService.getRoleList(any())).thenReturn(mockPage);

        // When - 传入null请求
        AjaxResult result = roleController.getRoleList(null);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());

        // 验证Service方法被调用，并检查默认分页参数
        verify(roleService).getRoleList(argThat(dto -> {
            // 验证使用了默认的分页参数
            return dto.getPageNum().equals(1) && dto.getPageSize().equals(10);
        }));

        log.info("testGetRoleList_NullRequest passed - 默认分页参数处理正确");
    }

    /**
     * 测试获取角色列表 - 缺少分页参数
     */
    @Test
    public void testGetRoleList_MissingPageParams() {
        // Given - 创建没有分页参数的请求
        RoleQueryReq req = new RoleQueryReq();
        // pageNum 和 pageSize 保持为null

        // Mock service call
        com.baomidou.mybatisplus.extension.plugins.pagination.Page mockPage =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page();
        mockPage.setRecords(java.util.Collections.emptyList());
        when(roleService.getRoleList(any())).thenReturn(mockPage);

        // When
        AjaxResult result = roleController.getRoleList(req);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());

        // 验证使用了默认分页参数
        verify(roleService).getRoleList(argThat(dto -> {
            return dto.getPageNum().equals(1) && dto.getPageSize().equals(10);
        }));

        log.info("testGetRoleList_MissingPageParams passed - 默认分页参数处理正确");
    }
}