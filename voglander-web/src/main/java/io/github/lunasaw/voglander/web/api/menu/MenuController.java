package io.github.lunasaw.voglander.web.api.menu;

import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.common.util.JwtUtils;
import io.github.lunasaw.voglander.manager.assembler.MenuAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.MenuDTO;
import io.github.lunasaw.voglander.manager.domaon.vo.MenuVO;
import io.github.lunasaw.voglander.manager.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单控制器
 *
 * @author luna
 */
@Slf4j
@RestController
@RequestMapping("/menu")
@Tag(name = "菜单管理", description = "菜单权限相关接口")
public class MenuController {

    @Autowired
    private MenuService menuService;

    /**
     * 获取用户所有菜单
     */
    @GetMapping("/all")
    @Operation(summary = "获取用户菜单", description = "获取当前登录用户的所有可访问菜单，返回树形结构")
    @ApiResponse(responseCode = "200", description = "获取成功",
        content = @Content(schema = @Schema(implementation = MenuVO[].class)))
    public AjaxResult getAllMenus(@Parameter(description = "访问令牌") @RequestHeader(value = "Authorization", required = false) String token) {
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

        // 转换为前端格式
        List<MenuVO> menuVOList = MenuAssembler.toVOList(menuTree);

        return AjaxResult.success(menuVOList);
    }
}