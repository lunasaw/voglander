package io.github.lunasaw.voglander.web.api.cascade;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
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
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import io.github.lunasaw.voglander.manager.manager.CascadePlatformManager;
import io.github.lunasaw.voglander.web.api.cascade.assembler.CascadeWebAssembler;
import io.github.lunasaw.voglander.web.api.cascade.req.CascadePlatformCreateReq;
import io.github.lunasaw.voglander.web.api.cascade.req.CascadePlatformPageReq;
import io.github.lunasaw.voglander.web.api.cascade.req.CascadePlatformUpdateReq;
import io.github.lunasaw.voglander.web.api.cascade.resp.CascadePlatformListResp;
import io.github.lunasaw.voglander.web.api.cascade.vo.CascadePlatformVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 级联上级平台管理 API
 *
 * <p>
 * 遵循 device 模块 Web 范式���分页 POST /getPage + @RequestBody PageReq，返回 VO/ListResp；
 * 时间字段统一 Unix 毫秒；CRUD 入参用 Req + WebAssembler 转 DTO。
 * </p>
 *
 * @author luna
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/cascade/platform")
@Tag(name = "级联平台管理", description = "级联上级平台 CRUD + 注册调度启停")
public class CascadePlatformController {

    @Autowired
    private CascadePlatformManager    cascadePlatformManager;

    @Autowired
    private CascadeClientScheduler    cascadeClientScheduler;

    @Autowired
    private CascadeWebAssembler       cascadeWebAssembler;

    @Autowired
    private VoglanderSipClientProperties sipClientProperties;

    @PostMapping
    @Operation(summary = "新增上级平台")
    public AjaxResult<Long> add(@RequestBody CascadePlatformCreateReq req) {
        CascadePlatformDTO dto = cascadeWebAssembler.toDTO(req);
        fillLocalIpIfAbsent(dto);
        Long id = cascadePlatformManager.add(dto);
        return AjaxResult.success(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新上级平台")
    public AjaxResult<Boolean> update(@PathVariable Long id, @RequestBody CascadePlatformUpdateReq req) {
        CascadePlatformDTO dto = cascadeWebAssembler.toDTO(req);
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
    public AjaxResult<CascadePlatformVO> getById(@PathVariable Long id) {
        CascadePlatformDTO dto = cascadePlatformManager.getById(id);
        return AjaxResult.success(CascadePlatformVO.convertVO(dto));
    }

    @PostMapping("/getPage")
    @Operation(summary = "分页查询上级平台", description = "POST 条件分页，返回 total + items（VO，时间毫秒）")
    public AjaxResult<CascadePlatformListResp> getPage(
        @RequestBody(required = false) CascadePlatformPageReq pageReq,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {
        CascadePlatformDTO query = cascadeWebAssembler.pageReqToQueryDto(pageReq);
        Page<CascadePlatformDTO> dtoPage = cascadePlatformManager.getPage(query, page, size);

        List<CascadePlatformVO> items = dtoPage.getRecords().stream()
            .map(CascadePlatformVO::convertVO)
            .collect(Collectors.toList());

        CascadePlatformListResp resp = new CascadePlatformListResp();
        resp.setTotal(dtoPage.getTotal());
        resp.setItems(items);
        return AjaxResult.success(resp);
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "启用上级平台（enabled=1 + 启动注册保活调度）")
    public AjaxResult<Boolean> enable(@PathVariable Long id) {
        /* 启用前检查 localIp：前端未填则用当前 SIP 客户端绑定 IP 回填并持久化 */
        CascadePlatformDTO platform = cascadePlatformManager.getById(id);
        if (platform != null && StringUtils.isBlank(platform.getLocalIp())) {
            CascadePlatformDTO patch = new CascadePlatformDTO();
            patch.setId(id);
            patch.setLocalIp(sipClientProperties.getDomain());
            patch.setLocalPort(sipClientProperties.getPort());
            cascadePlatformManager.update(patch);
            platform = cascadePlatformManager.getById(id);
        }
        boolean ok = cascadePlatformManager.enablePlatform(id);
        if (ok && platform != null) {
            cascadeClientScheduler.startPlatform(platform);
        }
        return AjaxResult.success(ok);
    }

    /** 新增时若前端未填 localIp/localPort，回填 SIP 客户端实际绑定地址 */
    private void fillLocalIpIfAbsent(CascadePlatformDTO dto) {
        if (StringUtils.isBlank(dto.getLocalIp())) {
            dto.setLocalIp(sipClientProperties.getDomain());
        }
        if (dto.getLocalPort() == null) {
            dto.setLocalPort(sipClientProperties.getPort());
        }
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
