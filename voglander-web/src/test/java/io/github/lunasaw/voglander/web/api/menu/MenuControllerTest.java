package io.github.lunasaw.voglander.web.api.menu;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.domaon.vo.MenuVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 菜单控制器测试
 *
 * @author luna
 */
@Slf4j
@SpringBootTest
public class MenuControllerTest extends BaseTest {

    @Autowired
    private MenuController menuController;

    /**
     * 测试获取用户权限菜单接口
     */
    @Test
    public void testGetUserPermissions() {
        // 使用模拟的token
        String mockToken = "Bearer mock_token_for_test";

        try {
            AjaxResult<List<MenuVO>> result = menuController.getUserPermissions(mockToken);

            log.info("用户权限菜单接口测试结果：{}", result);

            if (result.getCode() == 0) {
                List<MenuVO> menuList = result.getData();
                log.info("获取到菜单数量：{}", menuList != null ? menuList.size() : 0);

                if (menuList != null && !menuList.isEmpty()) {
                    for (MenuVO menu : menuList) {
                        log.info("菜单信息 - Name: {}, Path: {}, Component: {}",
                            menu.getName(), menu.getPath(), menu.getComponent());

                        if (menu.getMeta() != null) {
                            log.info("菜单元数据 - Title: {}, Order: {}, AffixTab: {}, NoBasicLayout: {}",
                                menu.getMeta().getTitle(),
                                menu.getMeta().getOrder(),
                                menu.getMeta().getAffixTab(),
                                menu.getMeta().getNoBasicLayout());
                        }

                        if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
                            log.info("子菜单数量：{}", menu.getChildren().size());
                        }
                    }
                }
            } else {
                log.warn("接口调用失败：{}", result.getMsg());
            }

        } catch (Exception e) {
            log.error("测试用户权限菜单接口时发生异常", e);
        }
    }

    /**
     * 测试获取菜单列表接口
     */
    @Test
    public void testGetMenuList() {
        try {
            AjaxResult result = menuController.getMenuList();

            log.info("菜单列表接口测试结果：{}", result);

        } catch (Exception e) {
            log.error("测试菜单列表接口时发生异常", e);
        }
    }
}