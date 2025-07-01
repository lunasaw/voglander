package io.github.lunasaw.voglander.manager.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import io.github.lunasaw.voglander.manager.domaon.dto.RoleDTO;
import io.github.lunasaw.voglander.repository.entity.RoleDO;

import java.util.List;

/**
 * 角色服务接口
 *
 * @author luna
 */
public interface RoleService extends IService<RoleDO> {

    /**
     * 分页查询角色列表
     *
     * @param dto 查询条件
     * @return 角色列表分页数据
     */
    IPage<RoleDTO> getRoleList(RoleDTO dto);

    /**
     * 根据ID获取角色信息
     *
     * @param id 角色ID
     * @return 角色信息
     */
    RoleDTO getRoleById(Long id);

    /**
     * 创建角色
     *
     * @param dto 角色信息
     * @return 创建结果
     */
    boolean createRole(RoleDTO dto);

    /**
     * 更新角色
     *
     * @param id 角色ID
     * @param dto 角色信息
     * @return 更新结果
     */
    boolean updateRole(Long id, RoleDTO dto);

    /**
     * 删除角色
     *
     * @param id 角色ID
     * @return 删除结果
     */
    boolean deleteRole(Long id);

    /**
     * 根据用户ID获取角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<RoleDTO> getRolesByUserId(Long userId);
}