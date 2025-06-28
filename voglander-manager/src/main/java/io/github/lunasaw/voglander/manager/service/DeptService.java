package io.github.lunasaw.voglander.manager.service;

import io.github.lunasaw.voglander.manager.domaon.dto.DeptDTO;

import java.util.List;

/**
 * 部门服务接口
 *
 * @author luna
 */
public interface DeptService {

    /**
     * 获取所有部门列表
     *
     * @return 部门列表
     */
    List<DeptDTO> getAllDepts();

    /**
     * 构建部门树
     *
     * @param deptList 部门列表
     * @return 部门树
     */
    List<DeptDTO> buildDeptTree(List<DeptDTO> deptList);

    /**
     * 根据ID获取部门
     *
     * @param id 部门ID
     * @return 部门信息
     */
    DeptDTO getDeptById(Long id);

    /**
     * 创建部门
     *
     * @param deptDTO 部门信息
     * @return 创建的部门ID
     */
    Long createDept(DeptDTO deptDTO);

    /**
     * 更新部门
     *
     * @param id 部门ID
     * @param deptDTO 部门信息
     * @return 是否更新成功
     */
    boolean updateDept(Long id, DeptDTO deptDTO);

    /**
     * 删除部门
     *
     * @param id 部门ID
     * @return 是否删除成功
     */
    boolean deleteDept(Long id);

    /**
     * 检查部门名称是否存在
     *
     * @param name 部门名称
     * @param excludeId 排除的部门ID
     * @return 是否存在
     */
    boolean isDeptNameExists(String name, Long excludeId);
}