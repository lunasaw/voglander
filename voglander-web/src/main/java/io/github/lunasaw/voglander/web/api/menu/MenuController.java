package io.github.lunasaw.voglander.web.api.menu;

import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.common.util.JwtUtils;
import io.github.lunasaw.voglander.manager.assembler.MenuAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.MenuDTO;
import io.github.lunasaw.voglander.web.api.menu.req.MenuReq;
import io.github.lunasaw.voglander.web.api.menu.vo.MenuResp;
import io.github.lunasaw.voglander.web.api.menu.vo.MenuVO;
import io.github.lunasaw.voglander.manager.service.MenuService;
import io.github.lunasaw.voglander.web.assembler.MenuWebAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private MenuService menuService;

    @Autowired
    private MenuWebAssembler menuWebAssembler;

    /**
     * 获取用户所有菜单
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

        // 获取用户菜单
        List<MenuDTO> userMenus = menuService.getUserMenus(userId);

        // 构建菜单树
        List<MenuDTO> menuTree = menuService.buildMenuTree(userMenus);

        // 转换为响应格式
        List<MenuResp> menuRespList = menuWebAssembler.toRespList(menuTree);

        return AjaxResult.success(menuRespList);
    }

    /**
     * 获取用户权限菜单（前端路由格式）
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

        // 获取用户菜单
        List<MenuDTO> userMenus = menuService.getUserMenus(userId);

        // 构建菜单树
        List<MenuDTO> menuTree = menuService.buildMenuTree(userMenus);

        // 转换为前端路由格式
        List<MenuVO> menuVOList = MenuAssembler.toVOList(menuTree);

        return AjaxResult.success(menuVOList);
    }

    /**
     * 获取菜单数据列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取菜单列表", description = "获取所有菜单数据，返回树形结构")
    @ApiResponse(responseCode = "200", description = "获取成功",
        content = @Content(schema = @Schema(implementation = MenuResp[].class)))
    public AjaxResult<List<MenuResp>> getMenuList() {
        try {
            // 获取所有菜单
            List<MenuDTO> menuList = menuService.getAllMenus();

            // 构建菜单树
            List<MenuDTO> menuTree = menuService.buildMenuTree(menuList);

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
     */
    @GetMapping("/name-exists")
    @Operation(summary = "检查菜单名称", description = "检查菜单名称是否已存在")
    @ApiResponse(responseCode = "200", description = "检查成功")
    public AjaxResult isMenuNameExists(
        @Parameter(description = "菜单名称") @RequestParam String name,
        @Parameter(description = "排除的菜单ID") @RequestParam(required = false) String id) {
        try {
            Long excludeId = StringUtils.isNotBlank(id) ? Long.valueOf(id) : null;
            boolean exists = menuService.isMenuNameExists(name, excludeId);
            return AjaxResult.success(exists);
        } catch (Exception e) {
            log.error("检查菜单名称失败", e);
            return AjaxResult.error("检查失败：" + e.getMessage());
        }
    }

    /**
     * 检查菜单路径是否存在
     */
    @GetMapping("/path-exists")
    @Operation(summary = "检查菜单路径", description = "检查菜单路径是否已存在")
    @ApiResponse(responseCode = "200", description = "检查成功")
    public AjaxResult isMenuPathExists(
        @Parameter(description = "菜单路径") @RequestParam String path,
        @Parameter(description = "排除的菜单ID") @RequestParam(required = false) String id) {
        try {
            Long excludeId = StringUtils.isNotBlank(id) ? Long.valueOf(id) : null;
            boolean exists = menuService.isMenuPathExists(path, excludeId);
            return AjaxResult.success(exists);
        } catch (Exception e) {
            log.error("检查菜单路径失败", e);
            return AjaxResult.error("检查失败：" + e.getMessage());
        }
    }

    /**
     * 创建菜单
     */
    @PostMapping
    @Operation(summary = "创建菜单", description = "创建新的菜单")
    @ApiResponse(responseCode = "200", description = "创建成功")
    public AjaxResult createMenu(@Parameter(description = "菜单信息") @Valid @RequestBody MenuReq menuReq) {
        try {
            // 转换为DTO
            MenuDTO menuDTO = menuWebAssembler.toDTO(menuReq);

            // 创建菜单
            Long menuId = menuService.createMenu(menuDTO);

            return AjaxResult.success("创建成功", menuId);
        } catch (Exception e) {
            log.error("创建菜单失败", e);
            return AjaxResult.error("创建失败：" + e.getMessage());
        }
    }

    /**
     * 更新菜单
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

            // 更新菜单
            boolean success = menuService.updateMenu(menuId, menuDTO);

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
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜单", description = "删除指定ID的菜单")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public AjaxResult deleteMenu(@Parameter(description = "菜单ID") @PathVariable String id) {
        try {
            Long menuId = Long.valueOf(id);

            // 删除菜单
            boolean success = menuService.deleteMenu(menuId);

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
}