package io.github.lunasaw.voglander.web.api.cascade;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.manager.manager.CascadeChannelManager;
import io.github.lunasaw.voglander.web.api.cascade.assembler.CascadeWebAssembler;
import io.github.lunasaw.voglander.web.api.cascade.req.CascadeChannelBatchBindReq;
import io.github.lunasaw.voglander.web.api.cascade.req.CascadeChannelCreateReq;
import io.github.lunasaw.voglander.web.api.cascade.req.CascadeChannelPageReq;
import io.github.lunasaw.voglander.web.api.cascade.req.CascadeChannelUpdateReq;
import io.github.lunasaw.voglander.web.api.cascade.resp.CascadeChannelListResp;
import io.github.lunasaw.voglander.web.api.cascade.vo.CascadeChannelVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 级联通道映射管理 API
 *
 * <p>
 * 遵循 device 模块 Web 范式：分页 POST /getPage + @RequestBody PageReq，返回 VO/ListResp；
 * 时间字段统一 Unix 毫秒；CRUD 入参用 Req + WebAssembler 转 DTO。
 * </p>
 *
 * @author luna
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/cascade/channel")
@Tag(name = "级联通道管理", description = "级联通道映射 CRUD + 分页查询")
public class CascadeChannelController {

    @Autowired
    private CascadeChannelManager cascadeChannelManager;

    @Autowired
    private CascadeWebAssembler   cascadeWebAssembler;

    @PostMapping
    @Operation(summary = "新增级联通道映射")
    public AjaxResult<Long> add(@RequestBody CascadeChannelCreateReq req) {
        CascadeChannelDTO dto = cascadeWebAssembler.toDTO(req);
        Long id = cascadeChannelManager.add(dto);
        return AjaxResult.success(id);
    }

    @PostMapping("/batchBind")
    @Operation(summary = "批量绑定级联通道", description = "同一上级平台下批量绑定本地通道，已存在的跳过，返回新增条数")
    public AjaxResult<Integer> batchBind(@RequestBody CascadeChannelBatchBindReq req) {
        int added = cascadeChannelManager.batchBind(cascadeWebAssembler.toBatchDTOList(req));
        return AjaxResult.success(added);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新级联通道映射")
    public AjaxResult<Boolean> update(@PathVariable Long id, @RequestBody CascadeChannelUpdateReq req) {
        CascadeChannelDTO dto = cascadeWebAssembler.toDTO(req);
        dto.setId(id);
        boolean ok = cascadeChannelManager.update(dto);
        return AjaxResult.success(ok);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除级联通道映射")
    public AjaxResult<Boolean> delete(@PathVariable Long id) {
        boolean ok = cascadeChannelManager.delete(id);
        return AjaxResult.success(ok);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询级联通道详情")
    public AjaxResult<CascadeChannelVO> getById(@PathVariable Long id) {
        CascadeChannelDTO dto = cascadeChannelManager.getById(id);
        return AjaxResult.success(CascadeChannelVO.convertVO(dto));
    }

    @PostMapping("/getPage")
    @Operation(summary = "分页查询级联通道", description = "POST 条件分页，返回 total + items（VO，时间毫秒）")
    public AjaxResult<CascadeChannelListResp> getPage(
        @RequestBody(required = false) CascadeChannelPageReq pageReq,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {
        CascadeChannelDTO query = cascadeWebAssembler.pageReqToQueryDto(pageReq);
        Page<CascadeChannelDTO> dtoPage = cascadeChannelManager.getPage(query, page, size);

        List<CascadeChannelVO> items = dtoPage.getRecords().stream()
            .map(CascadeChannelVO::convertVO)
            .collect(Collectors.toList());

        CascadeChannelListResp resp = new CascadeChannelListResp();
        resp.setTotal(dtoPage.getTotal());
        resp.setItems(items);
        return AjaxResult.success(resp);
    }
}
