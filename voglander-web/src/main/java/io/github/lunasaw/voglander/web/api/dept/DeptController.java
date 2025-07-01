package io.github.lunasaw.voglander.web.api.dept;

import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.domaon.dto.DeptDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeptReq;
import io.github.lunasaw.voglander.manager.manager.DeptManager;
import io.github.lunasaw.voglander.manager.service.DeptService;
import io.github.lunasaw.voglander.web.api.dept.vo.DeptResp;
import io.github.lunasaw.voglander.web.assembler.DeptWebAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门控制器
 *
 * @author luna
 */
@Slf4j
@RestController
@RequestMapping("/system/dept")
@Tag(name = "系统部门管理", description = "系统部门管理相关接口")
public class DeptController {

    @Autowired
    private DeptService      deptService;

    @Autowired
    private DeptManager      deptManager;

    @Autowired
    private DeptWebAssembler deptWebAssembler;

    /**
     * 获取部门列表数据
     * 复杂业务场景：需要查询和树形构建，调用Manager层
     */
    @GetMapping("/list")
    @Operation(summary = "获取部门列表", description = "获取所有部门数据，返回树形结构")
    @ApiResponse(responseCode = "200", description = "获取成功",
        content = @Content(schema = @Schema(implementation = DeptResp[].class)))
    public AjaxResult<List<DeptResp>> getDeptList() {
        try {
            // 复杂业务：获取所有部门并构建树形结构，调用Manager
            List<DeptDTO> deptList = deptManager.getAllDepts();
            List<DeptDTO> deptTree = deptManager.buildDeptTree(deptList);

            // 转换为响应格式
            List<DeptResp> deptRespList = deptWebAssembler.toRespList(deptTree);

            return AjaxResult.success(deptRespList);
        } catch (Exception e) {
            log.error("获取部门列表失败", e);
            return AjaxResult.error("获取部门列表失败：" + e.getMessage());
        }
    }

    /**
     * 创建部门
     * 复杂业务场景：需要业务逻辑验证和事务处理，调用Manager层
     */
    @PostMapping
    @Operation(summary = "创建部门", description = "创建新的部门")
    @ApiResponse(responseCode = "200", description = "创建成功")
    public AjaxResult createDept(@Parameter(description = "部门信息") @Valid @RequestBody DeptReq deptReq) {
        try {
            // 转换为DTO
            DeptDTO deptDTO = deptWebAssembler.toDTO(deptReq);

            // 复杂业务：创建部门（包含验证和事务），调用Manager
            Long deptId = deptManager.createDept(deptDTO);

            return AjaxResult.success("创建成功", deptId);
        } catch (Exception e) {
            log.error("创建部门失败", e);
            return AjaxResult.error("创建失败：" + e.getMessage());
        }
    }

    /**
     * 更新部门
     * 复杂业务场景：需要业务逻辑验证和事务处理，调用Manager层
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新部门", description = "更新指定ID的部门")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public AjaxResult updateDept(
        @Parameter(description = "部门ID") @PathVariable String id,
        @Parameter(description = "部门信息") @Valid @RequestBody DeptReq deptReq) {
        try {
            Long deptId = Long.valueOf(id);

            // 转换为DTO
            DeptDTO deptDTO = deptWebAssembler.toDTO(deptReq);

            // 复杂业务：更新部门（包含验证和事务），调用Manager
            boolean success = deptManager.updateDept(deptId, deptDTO);

            if (success) {
                return AjaxResult.success("更新成功");
            } else {
                return AjaxResult.error("更新失败");
            }
        } catch (Exception e) {
            log.error("更新部门失败", e);
            return AjaxResult.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 删除部门
     * 复杂业务场景：需要业务逻辑验证和事务处理，调用Manager层
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除部门", description = "删除指定ID的部门")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public AjaxResult deleteDept(@Parameter(description = "部门ID") @PathVariable String id) {
        try {
            Long deptId = Long.valueOf(id);

            // 复杂业务：删除部门（包含验证和事务），调用Manager
            boolean success = deptManager.deleteDept(deptId);

            if (success) {
                return AjaxResult.success("删除成功");
            } else {
                return AjaxResult.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除部门失败", e);
            return AjaxResult.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 获取部门详情
     * 简单业务场景：获取单个实体，但需要DTO转换，调用Manager层
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取部门详情", description = "根据ID获取部门详细信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public AjaxResult getDeptById(@Parameter(description = "部门ID") @PathVariable String id) {
        try {
            Long deptId = Long.valueOf(id);

            // 业务场景：需要DTO转换，调用Manager
            DeptDTO deptDTO = deptManager.getDeptById(deptId);

            if (deptDTO == null) {
                return AjaxResult.error("部门不存在");
            }

            // 转换为响应格式
            DeptResp deptResp = deptWebAssembler.toResp(deptDTO);

            return AjaxResult.success(deptResp);
        } catch (Exception e) {
            log.error("获取部门详情失败", e);
            return AjaxResult.error("获取失败：" + e.getMessage());
        }
    }
}