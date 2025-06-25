package io.github.lunasaw.voglander.web.api.user;

import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.common.util.JwtUtils;
import io.github.lunasaw.voglander.manager.assembler.UserAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.domaon.vo.UserInfoVO;
import io.github.lunasaw.voglander.manager.service.AuthService;
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

/**
 * 用户控制器
 *
 * @author luna
 */
@Slf4j
@RestController
@RequestMapping("/user")
@Tag(name = "用户管理", description = "用户信息相关接口")
public class UserController {

    @Autowired
    private AuthService authService;

    /**
     * 获取用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的详细信息")
    @ApiResponse(responseCode = "200", description = "获取成功",
        content = @Content(schema = @Schema(implementation = UserInfoVO.class)))
    public AjaxResult getUserInfo(@Parameter(description = "访问令牌") @RequestHeader(value = "Authorization", required = false) String token) {
        if (StringUtils.isBlank(token) || !token.startsWith("Bearer ")) {
            return AjaxResult.error("请先登录");
        }

        token = token.substring(7);
        UserDTO userDTO = authService.getUserByToken(token);
        if (userDTO == null) {
            return AjaxResult.error("用户不存在或token无效");
        }

        UserInfoVO userInfoVO = UserAssembler.toUserInfoVO(userDTO);
        return AjaxResult.success(userInfoVO);
    }
}