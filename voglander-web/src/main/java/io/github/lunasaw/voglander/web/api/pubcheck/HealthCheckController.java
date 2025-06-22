package io.github.lunasaw.voglander.web.api.pubcheck;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器
 *
 * @author luna
 * @date 2024-12-19
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1)
public class HealthCheckController {

    /**
     * 健康检查接口
     *
     * @return 返回ok表示项目正常启动
     */
    @GetMapping("/check")
    public AjaxResult check() {
        return AjaxResult.success("ok");
    }
}