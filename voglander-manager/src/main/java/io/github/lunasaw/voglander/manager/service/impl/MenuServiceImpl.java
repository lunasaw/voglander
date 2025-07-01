package io.github.lunasaw.voglander.manager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lunasaw.voglander.manager.service.MenuService;
import io.github.lunasaw.voglander.repository.entity.MenuDO;
import io.github.lunasaw.voglander.repository.mapper.MenuMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 菜单服务实现类
 * 基于ServiceImpl提供基础CRUD操作
 *
 * @author luna
 */
@Slf4j
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, MenuDO> implements MenuService {

    // 只提供基础的IService功能，复杂业务逻辑在Manager层实现
}