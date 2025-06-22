package io.github.lunasaw.voglander.web.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 首页控制器
 *
 * @author luna
 * @version 1.0
 * @date 2024/01/01
 * @description: 处理首页相关请求
 */
@Controller
public class HomeController {

    /**
     * 首页重定向到logo展示页面
     *
     * @return 重定向路径
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/index.html";
    }

    /**
     * 首页直接访问
     *
     * @return 静态页面
     */
    @GetMapping("/home")
    public String home() {
        return "redirect:/index.html";
    }
}