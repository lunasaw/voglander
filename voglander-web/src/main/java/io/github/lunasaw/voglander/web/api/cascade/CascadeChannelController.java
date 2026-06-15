package io.github.lunasaw.voglander.web.api.cascade;

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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 级联通道映射管理 API
 *
 * @author luna
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/cascade/channel")
@Tag(name = "级联通道管理", description = "级联通道映射 CRUD + 分页查询")
public class CascadeChannelController {

    @Autowired
    private CascadeChannelManager cascadeChannelManager;

    @PostMapping
    @Operation(summary = "新增级联通道映射")
    public AjaxResult<Long> add(@RequestBody CascadeChannelDTO dto) {
        Long id = cascadeChannelManager.add(dto);
        return AjaxResult.success(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新级联通道映射")
    public AjaxResult<Boolean> update(@PathVariable Long id, @RequestBody CascadeChannelDTO dto) {
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
    public AjaxResult<CascadeChannelDTO> getById(@PathVariable Long id) {
        CascadeChannelDTO dto = cascadeChannelManager.getById(id);
        return AjaxResult.success(dto);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询级联通道")
    public AjaxResult<Page<CascadeChannelDTO>> page(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String platformId) {
        CascadeChannelDTO query = new CascadeChannelDTO();
        query.setPlatformId(platformId);
        Page<CascadeChannelDTO> result = cascadeChannelManager.getPage(query, page, size);
        return AjaxResult.success(result);
    }
}
