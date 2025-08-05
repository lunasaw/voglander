package io.github.lunasaw.voglander.web.api.role;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
     * 测试获取角色列表 - 验证方法调用
     */
    @Test
    public void testGetRoleList_MethodInvocation() {
        // Given
        RoleQueryReq req = new RoleQueryReq();
        req.setPageNum(1);
        req.setPageSize(10);

        // Mock the static method call and service call
        try (var mockedRoleWebAssembler = mockStatic(io.github.lunasaw.voglander.web.assembler.RoleWebAssembler.class)) {
            // Setup basic mocking
            mockedRoleWebAssembler.when(() -> io.github.lunasaw.voglander.web.assembler.RoleWebAssembler.toDTO(any(RoleQueryReq.class)))
                .thenReturn(null);
            mockedRoleWebAssembler.when(() -> io.github.lunasaw.voglander.web.assembler.RoleWebAssembler.toVOList(any()))
                .thenReturn(java.util.Collections.emptyList());

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

            // 验证Service方法被调用
            verify(roleService).getRoleList(any());

            log.info("testGetRoleList_MethodInvocation passed - 方法调用验证成功");
        }
    }
}