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
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeClientScheduler;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import io.github.lunasaw.voglander.manager.manager.CascadePlatformManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 级联上级平台管理 API
 *
 * @author luna
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/cascade/platform")
@Tag(name = "级联平台管理", description = "级联上级平台 CRUD + 注册调度启停")
public class CascadePlatformController {

    @Autowired
    private CascadePlatformManager cascadePlatformManager;

    @Autowired
    private CascadeClientScheduler cascadeClientScheduler;

    @PostMapping
    @Operation(summary = "新增上级平台")
    public AjaxResult<Long> add(@RequestBody CascadePlatformDTO dto) {
        Long id = cascadePlatformManager.add(dto);
        return AjaxResult.success(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新上级平台")
    public AjaxResult<Boolean> update(@PathVariable Long id, @RequestBody CascadePlatformDTO dto) {
        dto.setId(id);
        boolean ok = cascadePlatformManager.update(dto);
        return AjaxResult.success(ok);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除上级平台")
    public AjaxResult<Boolean> delete(@PathVariable Long id) {
        boolean ok = cascadePlatformManager.delete(id);
        return AjaxResult.success(ok);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询上级平台详情")
    public AjaxResult<CascadePlatformDTO> getById(@PathVariable Long id) {
        CascadePlatformDTO dto = cascadePlatformManager.getById(id);
        return AjaxResult.success(dto);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询上级平台")
    public AjaxResult<Page<CascadePlatformDTO>> page(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) Integer enabled) {
        CascadePlatformDTO query = new CascadePlatformDTO();
        query.setEnabled(enabled);
        Page<CascadePlatformDTO> result = cascadePlatformManager.getPage(query, page, size);
        return AjaxResult.success(result);
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "启用上级平台（enabled=1 + 启动注册保活调度）")
    public AjaxResult<Boolean> enable(@PathVariable Long id) {
        boolean ok = cascadePlatformManager.enablePlatform(id);
        if (ok) {
            CascadePlatformDTO platform = cascadePlatformManager.getById(id);
            if (platform != null) {
                cascadeClientScheduler.startPlatform(platform);
            }
        }
        return AjaxResult.success(ok);
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "停用上级平台（enabled=0 + 停止注册保活调度）")
    public AjaxResult<Boolean> disable(@PathVariable Long id) {
        cascadeClientScheduler.stopPlatform(id);
        boolean ok = cascadePlatformManager.disablePlatform(id);
        return AjaxResult.success(ok);
    }

    @PostMapping("/refresh")
    @Operation(summary = "批量刷新注册调度（加载 enabled=1 平台启动任务）")
    public AjaxResult<Void> refresh() {
        cascadeClientScheduler.refreshRegistrations();
        return AjaxResult.success();
    }
}
