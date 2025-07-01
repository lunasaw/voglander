package io.github.lunasaw.voglander.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.lunasaw.voglander.repository.entity.MenuDO;

/**
 * 菜单服务接口
 * 基于IService提供基础CRUD操作
 *
 * @author luna
 */
public interface MenuService extends IService<MenuDO> {

    // 继承IService的基础方法即可，无需额外的复杂业务方法
    // 复杂业务逻辑在Manager层实现
}
