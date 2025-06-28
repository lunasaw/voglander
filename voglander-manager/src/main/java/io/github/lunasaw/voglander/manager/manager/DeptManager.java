package io.github.lunasaw.voglander.manager.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.common.check.Assert;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.manager.assembler.DeptAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.DeptDTO;
import io.github.lunasaw.voglander.manager.service.DeptService;
import io.github.lunasaw.voglander.repository.entity.DeptDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门管理器
 * 处理部门相关的复杂业务逻辑
 *
 * @author luna
 */
@Slf4j
@Component
public class DeptManager {

    @Autowired
    private DeptService deptService;

    /**
     * 获取所有部门列表
     *
     * @return 部门列表
     */
    public List<DeptDTO> getAllDepts() {
        LambdaQueryWrapper<DeptDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeptDO::getStatus, 1)
            .orderByAsc(DeptDO::getSortOrder)
            .orderByAsc(DeptDO::getId);

        List<DeptDO> deptDOList = deptService.list(queryWrapper);
        return DeptAssembler.toDTOList(deptDOList);
    }

    /**
     * 构建部门树
     *
     * @param deptList 部门列表
     * @return 部门树
     */
    public List<DeptDTO> buildDeptTree(List<DeptDTO> deptList) {
        return DeptAssembler.buildDeptTree(deptList);
    }

    /**
     * 根据ID获取部门
     *
     * @param id 部门ID
     * @return 部门信息
     */
    public DeptDTO getDeptById(Long id) {
        Assert.notNull(id, "部门ID不能为空");

        DeptDO deptDO = deptService.getById(id);
        return DeptAssembler.toDTO(deptDO);
    }

    /**
     * 创建部门
     *
     * @param deptDTO 部门信息
     * @return 创建的部门ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createDept(DeptDTO deptDTO) {
        Assert.notNull(deptDTO, "部门信息不能为空");
        Assert.hasText(deptDTO.getDeptName(), "部门名称不能为空");

        // 检查部门名称是否重复
        if (isDeptNameExists(deptDTO.getDeptName(), null)) {
            throw new ServiceException("部门名称已存在");
        }

        DeptDO deptDO = DeptAssembler.toDO(deptDTO);

        // 生成部门编码
        if (StringUtils.isBlank(deptDO.getDeptCode())) {
            deptDO.setDeptCode(generateDeptCode(deptDTO.getDeptName()));
        }

        deptDO.setCreateTime(LocalDateTime.now());
        deptDO.setUpdateTime(LocalDateTime.now());

        boolean result = deptService.save(deptDO);
        if (result) {
            log.info("创建部门成功，部门ID：{}", deptDO.getId());
            return deptDO.getId();
        } else {
            throw new ServiceException("创建部门失败");
        }
    }

    /**
     * 更新部门
     *
     * @param id 部门ID
     * @param deptDTO 部门信息
     * @return 是否更新成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDept(Long id, DeptDTO deptDTO) {
        Assert.notNull(id, "部门ID不能为空");
        Assert.notNull(deptDTO, "部门信息不能为空");

        DeptDO existDept = deptService.getById(id);
        if (existDept == null) {
            throw new ServiceException("部门不存在");
        }

        // 检查部门名称是否重复
        if (StringUtils.isNotBlank(deptDTO.getDeptName()) &&
            isDeptNameExists(deptDTO.getDeptName(), id)) {
            throw new ServiceException("部门名称已存在");
        }

        DeptDO deptDO = DeptAssembler.toDO(deptDTO);
        deptDO.setId(id);
        deptDO.setUpdateTime(LocalDateTime.now());

        boolean result = deptService.updateById(deptDO);
        if (result) {
            log.info("更新部门成功，部门ID：{}", id);
            return true;
        } else {
            throw new ServiceException("更新部门失败");
        }
    }

    /**
     * 删除部门
     *
     * @param id 部门ID
     * @return 是否删除成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDept(Long id) {
        Assert.notNull(id, "部门ID不能为空");

        DeptDO deptDO = deptService.getById(id);
        if (deptDO == null) {
            throw new ServiceException("部门不存在");
        }

        // 检查是否有子部门
        LambdaQueryWrapper<DeptDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeptDO::getParentId, id);
        long childCount = deptService.count(queryWrapper);
        if (childCount > 0) {
            throw new ServiceException("该部门下还有子部门，无法删除");
        }

        boolean result = deptService.removeById(id);
        if (result) {
            log.info("删除部门成功，部门ID：{}", id);
            return true;
        } else {
            throw new ServiceException("删除部门失败");
        }
    }

    /**
     * 检查部门名称是否存在
     *
     * @param name 部门名称
     * @param excludeId 排除的部门ID
     * @return 是否存在
     */
    public boolean isDeptNameExists(String name, Long excludeId) {
        if (StringUtils.isBlank(name)) {
            return false;
        }

        LambdaQueryWrapper<DeptDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeptDO::getDeptName, name);
        if (excludeId != null) {
            queryWrapper.ne(DeptDO::getId, excludeId);
        }

        return deptService.count(queryWrapper) > 0;
    }

    /**
     * 生成部门编码
     *
     * @param deptName 部门名称
     * @return 部门编码
     */
    private String generateDeptCode(String deptName) {
        // 简单生成规则：部门名称首字母+时间戳后6位
        String firstLetter = deptName.substring(0, 1).toLowerCase();
        String timestamp = String.valueOf(System.currentTimeMillis());
        return firstLetter + timestamp.substring(timestamp.length() - 6);
    }
}