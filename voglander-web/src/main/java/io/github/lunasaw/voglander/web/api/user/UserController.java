package io.github.lunasaw.voglander.web.api.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.assembler.UserAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.web.api.user.vo.UserInfoVO;
import io.github.lunasaw.voglander.manager.service.AuthService;
import io.github.lunasaw.voglander.manager.service.UserService;
import io.github.lunasaw.voglander.web.api.user.assembler.UserWebAssembler;
import io.github.lunasaw.voglander.web.api.user.req.UserCreateReq;
import io.github.lunasaw.voglander.web.api.user.req.UserQueryReq;
import io.github.lunasaw.voglander.web.api.user.req.UserUpdateReq;
import io.github.lunasaw.voglander.web.api.user.vo.UserListResp;
import io.github.lunasaw.voglander.web.api.user.vo.UserVO;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @Autowired
    private UserService userService;

    /**
     * 获取用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的详细信息")
    @ApiResponse(responseCode = "200", description = "获取成功",
        content = @Content(schema = @Schema(implementation = UserInfoVO.class)))
    public AjaxResult<UserInfoVO>
        getUserInfo(@Parameter(description = "访问令牌") @RequestHeader(value = "Authorization", required = false) String token) {
        if (StringUtils.isBlank(token) || !token.startsWith("Bearer ")) {
            throw new ServiceException(ServiceExceptionEnum.LOGIN_REQUIRED);
        }

        token = token.substring(7);
        UserDTO userDTO = authService.getUserByToken(token);
        if (userDTO == null) {
            throw new ServiceException(ServiceExceptionEnum.TOKEN_INVALID);
        }

        UserInfoVO userInfoVO = UserAssembler.toUserInfoVO(userDTO);
        return AjaxResult.success(userInfoVO);
    }

    /**
     * 分页查询用户列表
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询用户列表", description = "根据查询条件分页获取用户列表")
    @ApiResponse(responseCode = "200", description = "查询成功",
        content = @Content(schema = @Schema(implementation = UserListResp.class)))
    public AjaxResult<UserListResp> getUserList(@Parameter(description = "查询条件") UserQueryReq req) {
        UserDTO dto = UserWebAssembler.toDTO(req);
        IPage<UserDTO> page = userService.getUserList(dto);

        List<UserVO> userVOList = UserWebAssembler.toVOList(page.getRecords());
        UserListResp response = UserWebAssembler.toListResp(userVOList);

        return AjaxResult.success(response);
    }

    /**
     * 根据ID获取用户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户详细信息")
    @ApiResponse(responseCode = "200", description = "获取成功",
        content = @Content(schema = @Schema(implementation = UserVO.class)))
    public AjaxResult<UserVO> getUserById(@Parameter(description = "用户ID") @PathVariable Long id) {
        UserDTO userDTO = userService.getUserById(id);
        if (userDTO == null) {
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION.getCode(), "用户不存在");
        }

        UserVO userVO = UserWebAssembler.toVO(userDTO);
        return AjaxResult.success(userVO);
    }

    /**
     * 创建用户
     */
    @PostMapping
    @Operation(summary = "创建用户", description = "创建新的用户")
    @ApiResponse(responseCode = "200", description = "创建成功")
    public AjaxResult<Long> createUser(@Parameter(description = "用户创建信息") @Valid @RequestBody UserCreateReq req) {
        UserDTO dto = UserWebAssembler.toDTO(req);
        Long userId = userService.createUser(dto);
        return AjaxResult.success(userId);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新用户", description = "根据用户ID更新用户信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public AjaxResult<Boolean> updateUser(
        @Parameter(description = "用户ID") @PathVariable Long id,
        @Parameter(description = "用户更新信息") @Valid @RequestBody UserUpdateReq req) {

        // 确保请求中的ID与路径参数一致
        req.setId(id);

        UserDTO dto = UserWebAssembler.toDTO(req);
        boolean result = userService.updateUser(id, dto);
        return AjaxResult.success(result);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "根据用户ID删除用户")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public AjaxResult<Boolean> deleteUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        boolean result = userService.deleteUser(id);
        return AjaxResult.success(result);
    }

    /**
     * 检查用户名是否存在
     */
    @GetMapping("/check-username/{username}")
    @Operation(summary = "检查用户名", description = "检查用户名是否已存在")
    @ApiResponse(responseCode = "200", description = "检查完成")
    public AjaxResult<Boolean> checkUsername(
        @Parameter(description = "用户名") @PathVariable String username,
        @Parameter(description = "排除的用户ID") Long excludeId) {

        boolean exists = userService.isUsernameExists(username, excludeId);
        return AjaxResult.success(exists);
    }

    /**
     * 检查邮箱是否存在
     */
    @GetMapping("/check-email/{email}")
    @Operation(summary = "检查邮箱", description = "检查邮箱是否已存在")
    @ApiResponse(responseCode = "200", description = "检查完成")
    public AjaxResult<Boolean> checkEmail(
        @Parameter(description = "邮箱") @PathVariable String email,
        @Parameter(description = "排除的用户ID") Long excludeId) {

        boolean exists = userService.isEmailExists(email, excludeId);
        return AjaxResult.success(exists);
    }

    /**
     * 检查手机号是否存在
     */
    @GetMapping("/check-phone/{phone}")
    @Operation(summary = "检查手机号", description = "检查手机号是否已存在")
    @ApiResponse(responseCode = "200", description = "检查完成")
    public AjaxResult<Boolean> checkPhone(
        @Parameter(description = "手机号") @PathVariable String phone,
        @Parameter(description = "排除的用户ID") Long excludeId) {

        boolean exists = userService.isPhoneExists(phone, excludeId);
        return AjaxResult.success(exists);
    }
}