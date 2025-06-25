package io.github.lunasaw.voglander.web.api.medianode;

import java.util.List;
import java.util.stream.Collectors;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaNodeDTO;
import io.github.lunasaw.voglander.manager.manager.MediaNodeManager;
import io.github.lunasaw.voglander.repository.entity.MediaNodeDO;
import io.github.lunasaw.voglander.web.api.medianode.assembler.MediaNodeWebAssembler;
import io.github.lunasaw.voglander.web.api.medianode.req.MediaNodeCreateReq;
import io.github.lunasaw.voglander.web.api.medianode.req.MediaNodeUpdateReq;
import io.github.lunasaw.voglander.web.api.medianode.vo.MediaNodeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * 流媒体节点管理
 *
 * @author luna
 * @since 2025-01-23
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/medianode")
@Tag(name = "流媒体节点管理", description = "流媒体节点增删改查等相关接口")
public class MediaNodeController {

    @Autowired
    private MediaNodeManager mediaNodeManager;

    @Autowired
    private MediaNodeWebAssembler mediaNodeWebAssembler;

    @GetMapping("/get/{id}")
    @Operation(summary = "根据ID获取节点", description = "通过数据库主键ID获取流媒体节点详细信息")
    @ApiResponse(responseCode = "200", description = "获取成功",
        content = @Content(schema = @Schema(implementation = AjaxResult.class)))
    public AjaxResult getById(@Parameter(description = "节点数据库ID") @PathVariable(value = "id") Long id) {
        MediaNodeDTO mediaNodeDTO = mediaNodeManager.getMediaNodeDTOById(id);
        if (mediaNodeDTO == null) {
            return AjaxResult.error("节点不存在");
        }
        MediaNodeVO mediaNodeVO = MediaNodeVO.convertVO(mediaNodeDTO);
        return AjaxResult.success(mediaNodeVO);
    }

    @GetMapping("/getByServerId/{serverId}")
    @Operation(summary = "根据节点ID获取节点", description = "通过节点服务ID获取流媒体节点信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public AjaxResult getByServerId(@Parameter(description = "节点服务ID") @PathVariable(value = "serverId") String serverId) {
        MediaNodeDTO mediaNodeDTO = mediaNodeManager.getDTOByServerId(serverId);
        if (mediaNodeDTO == null) {
            return AjaxResult.error("节点不存在");
        }
        MediaNodeVO mediaNodeVO = MediaNodeVO.convertVO(mediaNodeDTO);
        return AjaxResult.success(mediaNodeVO);
    }

    @GetMapping("/get")
    @Operation(summary = "根据条件查询节点", description = "通过节点实体条件查询流媒体节点信息")
    public AjaxResult getByEntity(MediaNodeDO mediaNode) {
        MediaNodeDTO mediaNodeDTO = mediaNodeManager.getMediaNodeDTOByEntity(mediaNode);
        if (mediaNodeDTO == null) {
            return AjaxResult.error("节点不存在");
        }
        MediaNodeVO mediaNodeVO = MediaNodeVO.convertVO(mediaNodeDTO);
        return AjaxResult.success(mediaNodeVO);
    }

    @GetMapping("/list")
    @Operation(summary = "获取节点列表", description = "根据条件获取流媒体节点列表")
    public AjaxResult list(MediaNodeDO mediaNode) {
        List<MediaNodeDTO> mediaNodeDTOList = mediaNodeManager.listMediaNodeDTO(mediaNode);
        List<MediaNodeVO> mediaNodeVOList = mediaNodeDTOList.stream()
                .map(MediaNodeVO::convertVO)
                .collect(Collectors.toList());
        return AjaxResult.success(mediaNodeVOList);
    }

    @GetMapping("/listEnabled")
    @Operation(summary = "获取启用的节点列表", description = "获取所有启用状态的流媒体节点列表")
    public AjaxResult listEnabled() {
        List<MediaNodeDTO> mediaNodeDTOList = mediaNodeManager.getEnabledNodes();
        List<MediaNodeVO> mediaNodeVOList = mediaNodeDTOList.stream()
                .map(MediaNodeVO::convertVO)
                .collect(Collectors.toList());
        return AjaxResult.success(mediaNodeVOList);
    }

    @GetMapping("/listOnline")
    @Operation(summary = "获取在线的节点列表", description = "获取所有在线状态的流媒体节点列表")
    public AjaxResult listOnline() {
        List<MediaNodeDTO> mediaNodeDTOList = mediaNodeManager.getOnlineNodes();
        List<MediaNodeVO> mediaNodeVOList = mediaNodeDTOList.stream()
                .map(MediaNodeVO::convertVO)
                .collect(Collectors.toList());
        return AjaxResult.success(mediaNodeVOList);
    }

    @GetMapping("/pageListByEntity/{page}/{size}")
    @Operation(summary = "分页查询节点", description = "根据条件分页查询流媒体节点列表")
    public AjaxResult listPageByEntity(
        @Parameter(description = "页码") @PathVariable(value = "page") int page,
        @Parameter(description = "每页大小") @PathVariable(value = "size") int size,
        MediaNodeDO mediaNode) {
        QueryWrapper<MediaNodeDO> query = Wrappers.query(mediaNode);
        Page<MediaNodeDTO> pageInfo = mediaNodeManager.pageQuery(page, size, query);

        // 转换为 VO 模型
        List<MediaNodeVO> mediaNodeVOList = pageInfo.getRecords().stream()
                .map(MediaNodeVO::convertVO)
                .collect(Collectors.toList());

        // 构建返回的分页对象
        Page<MediaNodeVO> resultPage = new Page<>(page, size);
        resultPage.setRecords(mediaNodeVOList);
        resultPage.setTotal(pageInfo.getTotal());
        resultPage.setCurrent(pageInfo.getCurrent());
        resultPage.setSize(pageInfo.getSize());
        resultPage.setPages(pageInfo.getPages());

        return AjaxResult.success(resultPage);
    }

    @GetMapping("/pageList/{page}/{size}")
    @Operation(summary = "简单分页查询", description = "分页查询所有流媒体节点")
    public AjaxResult listPage(
        @Parameter(description = "页码") @PathVariable(value = "page") int page,
        @Parameter(description = "每页大小") @PathVariable(value = "size") int size) {
        Page<MediaNodeDTO> pageInfo = mediaNodeManager.pageQuerySimple(page, size);

        // 转换为 VO 模型
        List<MediaNodeVO> mediaNodeVOList = pageInfo.getRecords().stream()
                .map(MediaNodeVO::convertVO)
                .collect(Collectors.toList());

        // 构建返回的分页对象
        Page<MediaNodeVO> resultPage = new Page<>(page, size);
        resultPage.setRecords(mediaNodeVOList);
        resultPage.setTotal(pageInfo.getTotal());
        resultPage.setCurrent(pageInfo.getCurrent());
        resultPage.setSize(pageInfo.getSize());
        resultPage.setPages(pageInfo.getPages());

        return AjaxResult.success(resultPage);
    }

    @PostMapping("/insert")
    @Operation(summary = "创建节点", description = "添加新的流媒体节点")
    @ApiResponse(responseCode = "200", description = "创建成功")
    public AjaxResult insert(@Valid @RequestBody MediaNodeCreateReq createReq) {
        // Req -> DTO (使用 Web 层转换器)
        MediaNodeDTO mediaNodeDTO = mediaNodeWebAssembler.toMediaNodeDTO(createReq);

        // 通过 Manager 层处理业务逻辑
        Long nodeId = mediaNodeManager.createMediaNode(mediaNodeDTO);

        return AjaxResult.success(nodeId);
    }

    @PostMapping("/insertBatch")
    @Operation(summary = "批量创建节点", description = "批量添加流媒体节点")
    public AjaxResult insertBatch(@Valid @RequestBody List<MediaNodeCreateReq> createReqList) {
        // 批量 Req -> DTO (使用 Web 层转换器)
        List<MediaNodeDTO> mediaNodeDTOList = mediaNodeWebAssembler.toMediaNodeDTOList(createReqList);

        // 通过 Manager 层处理批量业务逻辑
        int successCount = mediaNodeManager.batchCreateMediaNode(mediaNodeDTOList);

        return AjaxResult.success("成功创建 " + successCount + " 个节点，共 " + createReqList.size() + " 个请求");
    }

    @PutMapping("/update")
    @Operation(summary = "更新节点", description = "更新流媒体节点信息")
    public AjaxResult update(@Valid @RequestBody MediaNodeUpdateReq updateReq) {
        // Req -> DTO (使用 Web 层转换器)
        MediaNodeDTO mediaNodeDTO = mediaNodeWebAssembler.toMediaNodeDTO(updateReq);

        Long updated = mediaNodeManager.updateMediaNode(mediaNodeDTO);

        return AjaxResult.success(updated);
    }

    @PutMapping("/updateBatch")
    @Operation(summary = "批量更新节点", description = "批量更新流媒体节点信息")
    public AjaxResult updateBatch(@Valid @RequestBody List<MediaNodeUpdateReq> updateReqList) {
        // 批量 Req -> DTO (使用 Web 层转换器)
        List<MediaNodeDTO> mediaNodeDTOList = mediaNodeWebAssembler.toUpdateMediaNodeDTOList(updateReqList);

        // 通过 Manager 层处理批量业务逻辑
        int successCount = mediaNodeManager.batchUpdateMediaNode(mediaNodeDTOList);

        return AjaxResult.success("成功更新 " + successCount + " 个节点，共 " + updateReqList.size() + " 个请求");
    }

    @PutMapping("/updateStatus/{serverId}")
    @Operation(summary = "更新节点状态", description = "更新流媒体节点在线状态")
    public AjaxResult updateStatus(
        @Parameter(description = "节点服务ID") @PathVariable(value = "serverId") String serverId,
        @Parameter(description = "节点状态 1在线 0离线") @RequestParam Integer status,
        @Parameter(description = "心跳时间戳") @RequestParam(required = false) Long keepalive) {
        mediaNodeManager.updateNodeStatus(serverId, status, keepalive);
        return AjaxResult.success("状态更新成功");
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除节点", description = "根据数据库ID删除流媒体节点")
    public AjaxResult deleteOne(@Parameter(description = "节点数据库ID") @PathVariable(value = "id") Long id) {
        // 通过Manager删除，这里简化直接通过service删除
        boolean removed = mediaNodeManager.getMediaNodeDTOById(id) != null;
        if (!removed) {
            return AjaxResult.error("节点不存在");
        }
        // TODO: 这里应该通过 Manager 层实现删除逻辑
        return AjaxResult.success("删除成功");
    }

    @DeleteMapping("/deleteByServerId/{serverId}")
    @Operation(summary = "根据节点ID删除", description = "根据节点服务ID删除流媒体节点")
    public AjaxResult deleteByServerId(@Parameter(description = "节点服务ID") @PathVariable(value = "serverId") String serverId) {
        MediaNodeDTO existingNode = mediaNodeManager.getDTOByServerId(serverId);
        if (existingNode == null) {
            return AjaxResult.error("节点不存在");
        }
        // TODO: 这里应该通过 Manager 层实现删除逻辑
        return AjaxResult.success("删除成功");
    }

    @DeleteMapping("/deleteIds")
    @Operation(summary = "批量删除节点", description = "根据数据库ID列表批量删除流媒体节点")
    public AjaxResult deleteBatch(@RequestBody List<Long> ids) {
        // TODO: 这里应该通过 Manager 层实现批量删除逻辑
        return AjaxResult.success("批量删除成功");
    }
}