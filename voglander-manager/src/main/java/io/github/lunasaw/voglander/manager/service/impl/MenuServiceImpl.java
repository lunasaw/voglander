package io.github.lunasaw.voglander.manager.service.impl;

import io.github.lunasaw.voglander.manager.assembler.MenuAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.MenuDTO;
import io.github.lunasaw.voglander.manager.service.MenuService;
import io.github.lunasaw.voglander.repository.entity.MenuDO;
import io.github.lunasaw.voglander.repository.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 菜单服务实现类
 *
 * @author luna
 */
@Slf4j
@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<MenuDTO> getUserMenus(Long userId) {
        List<MenuDO> menuDOList = userMapper.selectUserMenus(userId);
        return MenuAssembler.toDTOList(menuDOList);
    }

    @Override
    public List<MenuDTO> buildMenuTree(List<MenuDTO> menuList) {
        return MenuAssembler.buildMenuTree(menuList);
    }
}