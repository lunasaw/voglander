package io.github.lunasaw.voglander.web.api.cascade;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeSubscribeDTO;
import io.github.lunasaw.voglander.manager.manager.CascadeSubscribeManager;
import io.github.lunasaw.voglander.web.api.cascade.vo.CascadeSubscribeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 级联上级订阅查询 API（只读，用于前端订阅状态可视化）。
 *
 * <p>展示某个上级平台对本平台订阅了哪些信息（目录/告警/位置），是主动推送的目标清单。</p>
 *
 * @author luna
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/cascade/subscribe")
@Tag(name = "级联订阅查询", description = "上级订阅清单查询（只读）")
public class CascadeSubscribeController {

    @Autowired
    private CascadeSubscribeManager cascadeSubscribeManager;

    @GetMapping("/list")
    @Operation(summary = "按平台查询活跃订阅清单")
    public AjaxResult<List<CascadeSubscribeVO>> listByPlatform(@RequestParam String platformId) {
        List<CascadeSubscribeDTO> list = cascadeSubscribeManager.listActiveByPlatform(platformId);
        List<CascadeSubscribeVO> vos = list.stream()
            .map(CascadeSubscribeVO::convertVO)
            .collect(Collectors.toList());
        return AjaxResult.success(vos);
    }
}
