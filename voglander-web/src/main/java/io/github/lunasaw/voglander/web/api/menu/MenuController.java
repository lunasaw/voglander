package io.github.lunasaw.voglander.web.api.menu;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.common.util.JwtUtils;
import io.github.lunasaw.voglander.manager.domaon.dto.MenuDTO;
import io.github.lunasaw.voglander.manager.manager.MenuManager;
import io.github.lunasaw.voglander.web.api.menu.req.MenuReq;
import io.github.lunasaw.voglander.web.api.menu.vo.MenuResp;
import io.github.lunasaw.voglander.web.api.menu.vo.MenuVO;
import io.github.lunasaw.voglander.web.assembler.MenuWebAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * 菜单控制器
 *
 * @author luna
 */
@Slf4j
@RestController
@RequestMapping("/system/menu")
@Tag(name = "系统菜单管理", description = "系统菜单管理相关接口")
public class MenuController {
    @Autowired
    private MenuManager      menuManager;

    @Autowired
    private MenuWebAssembler menuWebAssembler;

    /**
     * 获取用户所有菜单
     * 复杂业务场景：需要多表查询和树形构建，调用Manager层
     */
    @GetMapping("/all")
    @Operation(summary = "获取用户菜单", description = "获取当前登录用户的所有可访问菜单，返回树形结构")
    @ApiResponse(responseCode = "200", description = "获取成功",
        content = @Content(schema = @Schema(implementation = MenuResp[].class)))
    public AjaxResult<List<MenuResp>>
        getAllMenus(@Parameter(description = "访问令牌") @RequestHeader(value = "Authorization", required = false) String token) {
        if (StringUtils.isBlank(token) || !token.startsWith("Bearer ")) {
            return AjaxResult.error("请先登录");
        }

        token = token.substring(7);
        Long userId = JwtUtils.getUserId(token);
        if (userId == null) {
            return AjaxResult.error("无效的token");
        }

        // 复杂业务：获取用户菜单并构建树形结构，调用Manager
        List<MenuDTO> userMenus = menuManager.getUserMenus(userId);
        List<MenuDTO> menuTree = menuManager.buildMenuTree(userMenus);

        // 转换为响应格式
        List<MenuResp> menuRespList = menuWebAssembler.toRespList(menuTree);

        return AjaxResult.success(menuRespList);
    }

    /**
     * 获取用户权限菜单（前端路由格式）
     * 复杂业务场景：需要多表查询和树形构建，调用Manager层
     */
    @GetMapping("/permissions")
    @Operation(summary = "获取用户权限菜单", description = "获取当前登录用户的权限菜单，返回前端路由格式")
    @ApiResponse(responseCode = "200", description = "获取成功",
        content = @Content(schema = @Schema(implementation = MenuVO[].class)))
    public AjaxResult<List<MenuVO>>
        getUserPermissions(@Parameter(description = "访问令牌") @RequestHeader(value = "Authorization", required = false) String token) {
        if (StringUtils.isBlank(token) || !token.startsWith("Bearer ")) {
            return AjaxResult.error("请先登录");
        }

        token = token.substring(7);
        Long userId = JwtUtils.getUserId(token);
        if (userId == null) {
            return AjaxResult.error("无效的token");
        }

        // 复杂业务：获取用户菜单并构建树形结构，调用Manager
        List<MenuDTO> userMenus = menuManager.getUserMenus(userId);
        List<MenuDTO> menuTree = menuManager.buildMenuTree(userMenus);

        // 转换为前端路由格式
        List<MenuVO> menuVOList = MenuWebAssembler.toVOList(menuTree);
        return AjaxResult.success(menuVOList);
    }

    /**
     * 获取菜单数据列表
     * 复杂业务场景：需要查询和树形构建，调用Manager层
     */
    @GetMapping("/list")
    @Operation(summary = "获取菜单列表", description = "获取所有菜单数据，返回树形结构")
    @ApiResponse(responseCode = "200", description = "获取成功",
        content = @Content(schema = @Schema(implementation = MenuResp[].class)))
    public AjaxResult<List<MenuResp>> getMenuList() {
        try {
            // 复杂业务：获取所有菜单并构建树形结构，调用Manager
            List<MenuDTO> menuList = menuManager.getAllMenus();
            List<MenuDTO> menuTree = menuManager.buildMenuTree(menuList);

            // 转换为响应格式
            List<MenuResp> menuRespList = menuWebAssembler.toRespList(menuTree);

            return AjaxResult.success(menuRespList);
        } catch (Exception e) {
            log.error("获取菜单列表失败", e);
            return AjaxResult.error("获取菜单列表失败：" + e.getMessage());
        }
    }

    /**
     * 检查菜单名称是否存在
     * 复杂业务场景：需要业务逻辑验证，调用Manager层
     */
    @GetMapping("/name-exists")
    @Operation(summary = "检查菜单名称", description = "检查菜单名称是否已存在")
    @ApiResponse(responseCode = "200", description = "检查成功")
    public AjaxResult isMenuNameExists(
        @Parameter(description = "菜单名称") @RequestParam String name,
        @Parameter(description = "排除的菜单ID") @RequestParam(required = false) String id) {
        try {
            Long excludeId = StringUtils.isNotBlank(id) ? Long.valueOf(id) : null;
            boolean exists = menuManager.isMenuNameExists(name, excludeId);
            return AjaxResult.success(exists);
        } catch (Exception e) {
            log.error("检查菜单名称失败", e);
            return AjaxResult.error("检查失败：" + e.getMessage());
        }
    }

    /**
     * 检查菜单路径是否存在
     * 复杂业务场景：需要业务逻辑验证，调用Manager层
     */
    @GetMapping("/path-exists")
    @Operation(summary = "检查菜单路径", description = "检查菜单路径是否已存在")
    @ApiResponse(responseCode = "200", description = "检查成功")
    public AjaxResult isMenuPathExists(
        @Parameter(description = "菜单路径") @RequestParam String path,
        @Parameter(description = "排除的菜单ID") @RequestParam(required = false) String id) {
        try {
            Long excludeId = StringUtils.isNotBlank(id) ? Long.valueOf(id) : null;
            boolean exists = menuManager.isMenuPathExists(path, excludeId);
            return AjaxResult.success(exists);
        } catch (Exception e) {
            log.error("检查菜单路径失败", e);
            return AjaxResult.error("检查失败：" + e.getMessage());
        }
    }

    /**
     * 创建菜单
     * 复杂业务场景：需要业务逻辑验证和事务处理，调用Manager层
     */
    @PostMapping
    @Operation(summary = "创建菜单", description = "创建新的菜单")
    @ApiResponse(responseCode = "200", description = "创建成功")
    public AjaxResult createMenu(@Parameter(description = "菜单信息") @Valid @RequestBody MenuReq menuReq) {
        try {
            // 转换为DTO
            MenuDTO menuDTO = menuWebAssembler.toDTO(menuReq);

            // 复杂业务：创建菜单（包含验证和事务），调用Manager
            Long menuId = menuManager.createMenu(menuDTO);

            return AjaxResult.success("创建成功", menuId);
        } catch (Exception e) {
            log.error("创建菜单失败", e);
            return AjaxResult.error("创建失败：" + e.getMessage());
        }
    }

    /**
     * 更新菜单
     * 复杂业务场景：需要业务逻辑验证和事务处理，调用Manager层
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新菜单", description = "更新指定ID的菜单")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public AjaxResult updateMenu(
        @Parameter(description = "菜单ID") @PathVariable String id,
        @Parameter(description = "菜单信息") @Valid @RequestBody MenuReq menuReq) {
        try {
            Long menuId = Long.valueOf(id);

            // 转换为DTO
            MenuDTO menuDTO = menuWebAssembler.toDTO(menuReq);

            // 复杂业务：更新菜单（包含验证和事务），调用Manager
            boolean success = menuManager.updateMenu(menuId, menuDTO);

            if (success) {
                return AjaxResult.success("更新成功");
            } else {
                return AjaxResult.error("更新失败");
            }
        } catch (Exception e) {
            log.error("更新菜单失败", e);
            return AjaxResult.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 删除菜单
     * 复杂业务场景：需要业务逻辑验证和事务处理，调用Manager层
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜单", description = "删除指定ID的菜单")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public AjaxResult deleteMenu(@Parameter(description = "菜单ID") @PathVariable String id) {
        try {
            Long menuId = Long.valueOf(id);

            // 复杂业务：删除菜单（包含验证和事务），调用Manager
            boolean success = menuManager.deleteMenu(menuId);

            if (success) {
                return AjaxResult.success("删除成功");
            } else {
                return AjaxResult.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除菜单失败", e);
            return AjaxResult.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取菜单详情
     * 简单业务场景：单表查询，直接调用Service层
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取菜单详情", description = "根据ID获取菜单详细信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public AjaxResult getMenuById(@Parameter(description = "菜单ID") @PathVariable String id) {
        try {
            Long menuId = Long.valueOf(id);

            // 简单业务：单表查询，直接调用Service
            MenuDTO menuDTO = menuManager.getMenuById(menuId);

            if (menuDTO == null) {
                return AjaxResult.error("菜单不存在");
            }

            return AjaxResult.success(menuDTO);
        } catch (Exception e) {
            log.error("获取菜单详情失败", e);
            return AjaxResult.error("获取失败：" + e.getMessage());
        }
    }
}