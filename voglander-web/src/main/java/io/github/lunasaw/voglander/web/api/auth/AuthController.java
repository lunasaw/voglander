package io.github.lunasaw.voglander.web.api.auth;

import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.common.util.JwtUtils;
import io.github.lunasaw.voglander.manager.domaon.dto.LoginDTO;
import io.github.lunasaw.voglander.web.api.auth.vo.LoginVO;
import io.github.lunasaw.voglander.manager.service.AuthService;
import io.github.lunasaw.voglander.web.api.auth.assembler.AuthWebAssembler;
import io.github.lunasaw.voglander.web.api.auth.vo.LoginReq;
import io.github.lunasaw.voglander.web.api.auth.vo.LoginResp;
import io.github.lunasaw.voglander.web.api.auth.vo.RefreshTokenResp;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 认证控制器
 *
 * @author luna
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "认证管理", description = "用户认证相关接口")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户通过用户名和密码进行登录认证")
    @ApiResponse(responseCode = "200", description = "登录成功",
        content = @Content(schema = @Schema(implementation = LoginResp.class)))
    public AjaxResult<LoginResp> login(@Valid @RequestBody LoginReq loginReq) {
        LoginDTO loginDTO = AuthWebAssembler.toLoginDTO(loginReq);

        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken(authService.login(loginDTO));
        LoginResp loginResp = AuthWebAssembler.toLoginResp(loginVO);
        return AjaxResult.success(loginResp);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出系统")
    @ApiResponse(responseCode = "200", description = "登出成功")
    public AjaxResult logout(@Parameter(description = "访问令牌") @RequestHeader(value = "Authorization", required = false) String token) {
        if (StringUtils.isNotBlank(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
            Long userId = JwtUtils.getUserId(token);
            if (userId != null) {
                authService.logout(userId);
            }
        }
        return AjaxResult.success();
    }

    /**
     * 刷新token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新访问令牌", description = "使用当前令牌刷新获取新的访问令牌")
    @ApiResponse(responseCode = "200", description = "刷新成功",
        content = @Content(schema = @Schema(implementation = RefreshTokenResp.class)))
    public AjaxResult refreshToken(@Parameter(description = "访问令牌") @RequestHeader(value = "Authorization", required = false) String token) {
        if (StringUtils.isBlank(token) || !token.startsWith("Bearer ")) {
            return AjaxResult.error("无效的token");
        }

        token = token.substring(7);
        Long userId = JwtUtils.getUserId(token);
        if (userId == null) {
            return AjaxResult.error("无效的token");
        }

        String newToken = authService.refreshToken(userId);
        RefreshTokenResp resp = AuthWebAssembler.toRefreshTokenResp(newToken);
        return AjaxResult.success(resp);
    }

    /**
     * 获取用户权限码
     */
    @GetMapping("/codes")
    @Operation(summary = "获取用户权限码", description = "获取当前登录用户的所有权限码")
    @ApiResponse(responseCode = "200", description = "获取成功",
        content = @Content(schema = @Schema(implementation = String[].class)))
    public AjaxResult getAccessCodes(@Parameter(description = "访问令牌") @RequestHeader(value = "Authorization", required = false) String token) {
        if (StringUtils.isBlank(token) || !token.startsWith("Bearer ")) {
            return AjaxResult.error("请先登录");
        }

        token = token.substring(7);
        Long userId = JwtUtils.getUserId(token);
        if (userId == null) {
            return AjaxResult.error("无效的token");
        }

        List<String> permissions = authService.getUserPermissions(userId);
        return AjaxResult.success(permissions);
    }
}