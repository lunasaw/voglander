package io.github.lunasaw.voglander.web.api.role;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.domaon.dto.RoleDTO;
import io.github.lunasaw.voglander.manager.domaon.vo.RoleVO;
import io.github.lunasaw.voglander.manager.service.RoleService;
import io.github.lunasaw.voglander.web.api.role.req.RoleCreateReq;
import io.github.lunasaw.voglander.web.api.role.req.RoleQueryReq;
import io.github.lunasaw.voglander.web.api.role.req.RoleUpdateReq;
import io.github.lunasaw.voglander.web.assembler.RoleWebAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 *
 * @author luna
 */
@Slf4j
@RestController
@RequestMapping("/system/role")
@Tag(name = "角色管理", description = "系统角色管理相关接口")
public class RoleController {

    @Autowired
    private RoleService roleService;

    /**
     * 获取角色列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取角色列表", description = "分页查询角色列表数据")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public AjaxResult<List<RoleVO>> getRoleList(RoleQueryReq req) {
        log.info("获取角色列表，查询条件：{}", req);

        RoleDTO dto = RoleWebAssembler.toDTO(req);
        dto.setPageNum(req.getPageNum());
        dto.setPageSize(req.getPageSize());

        IPage<RoleDTO> page = roleService.getRoleList(dto);
        List<RoleVO> voList = RoleWebAssembler.toVOList(page.getRecords());

        return AjaxResult.success(voList).put("total", page.getTotal());
    }

    /**
     * 根据ID获取角色详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取角色详情", description = "根据角色ID获取角色详细信息")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public AjaxResult<RoleVO> getRoleById(@PathVariable @Parameter(description = "角色ID") String id) {
        log.info("获取角色详情，角色ID：{}", id);

        RoleDTO dto = roleService.getRoleById(Long.valueOf(id));
        if (dto == null) {
            return AjaxResult.error("角色不存在");
        }

        RoleVO vo = RoleWebAssembler.toVO(dto);
        return AjaxResult.success(vo);
    }

    /**
     * 创建角色
     */
    @PostMapping
    @Operation(summary = "创建角色", description = "创建新的系统角色")
    @ApiResponse(responseCode = "200", description = "创建成功")
    public AjaxResult<Void> createRole(@Valid @RequestBody RoleCreateReq req) {
        log.info("创建角色，请求参数：{}", req);

        RoleDTO dto = RoleWebAssembler.toDTO(req);
        boolean result = roleService.createRole(dto);

        if (result) {
            return AjaxResult.success("角色创建成功");
        } else {
            return AjaxResult.error("角色创建失败");
        }
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新角色", description = "更新指定角色的信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public AjaxResult<Void> updateRole(@PathVariable @Parameter(description = "角色ID") String id,
        @Valid @RequestBody RoleUpdateReq req) {
        log.info("更新角色，角色ID：{}，请求参数：{}", id, req);

        RoleDTO dto = RoleWebAssembler.toDTO(req);
        boolean result = roleService.updateRole(Long.valueOf(id), dto);

        if (result) {
            return AjaxResult.success("角色更新成功");
        } else {
            return AjaxResult.error("角色更新失败");
        }
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色", description = "删除指定的系统角色")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public AjaxResult<Void> deleteRole(@PathVariable @Parameter(description = "角色ID") String id) {
        log.info("删除角色，角色ID：{}", id);

        boolean result = roleService.deleteRole(Long.valueOf(id));

        if (result) {
            return AjaxResult.success("角色删除成功");
        } else {
            return AjaxResult.error("角色删除失败");
        }
    }
}