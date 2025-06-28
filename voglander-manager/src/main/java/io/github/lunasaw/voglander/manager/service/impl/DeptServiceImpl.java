package io.github.lunasaw.voglander.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.common.check.Assert;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.manager.assembler.DeptAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.DeptDTO;
import io.github.lunasaw.voglander.manager.service.DeptService;
import io.github.lunasaw.voglander.repository.entity.DeptDO;
import io.github.lunasaw.voglander.repository.mapper.DeptMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门服务实现类
 *
 * @author luna
 */
@Slf4j
@Service
public class DeptServiceImpl implements DeptService {

    @Autowired
    private DeptMapper deptMapper;

    @Override
    public List<DeptDTO> getAllDepts() {
        LambdaQueryWrapper<DeptDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeptDO::getStatus, 1)
            .orderByAsc(DeptDO::getSortOrder)
            .orderByAsc(DeptDO::getId);

        List<DeptDO> deptDOList = deptMapper.selectList(queryWrapper);
        return DeptAssembler.toDTOList(deptDOList);
    }

    @Override
    public List<DeptDTO> buildDeptTree(List<DeptDTO> deptList) {
        return DeptAssembler.buildDeptTree(deptList);
    }

    @Override
    public DeptDTO getDeptById(Long id) {
        Assert.notNull(id, "部门ID不能为空");

        DeptDO deptDO = deptMapper.selectById(id);
        return DeptAssembler.toDTO(deptDO);
    }

    @Override
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

        int result = deptMapper.insert(deptDO);
        if (result > 0) {
            log.info("创建部门成功，部门ID：{}", deptDO.getId());
            return deptDO.getId();
        } else {
            throw new ServiceException("创建部门失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDept(Long id, DeptDTO deptDTO) {
        Assert.notNull(id, "部门ID不能为空");
        Assert.notNull(deptDTO, "部门信息不能为空");

        DeptDO existDept = deptMapper.selectById(id);
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

        int result = deptMapper.updateById(deptDO);
        if (result > 0) {
            log.info("更新部门成功，部门ID：{}", id);
            return true;
        } else {
            throw new ServiceException("更新部门失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDept(Long id) {
        Assert.notNull(id, "部门ID不能为空");

        DeptDO deptDO = deptMapper.selectById(id);
        if (deptDO == null) {
            throw new ServiceException("部门不存在");
        }

        // 检查是否有子部门
        LambdaQueryWrapper<DeptDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeptDO::getParentId, id);
        long childCount = deptMapper.selectCount(queryWrapper);
        if (childCount > 0) {
            throw new ServiceException("该部门下还有子部门，无法删除");
        }

        int result = deptMapper.deleteById(id);
        if (result > 0) {
            log.info("删除部门成功，部门ID：{}", id);
            return true;
        } else {
            throw new ServiceException("删除部门失败");
        }
    }

    @Override
    public boolean isDeptNameExists(String name, Long excludeId) {
        if (StringUtils.isBlank(name)) {
            return false;
        }

        LambdaQueryWrapper<DeptDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeptDO::getDeptName, name);
        if (excludeId != null) {
            queryWrapper.ne(DeptDO::getId, excludeId);
        }

        return deptMapper.selectCount(queryWrapper) > 0;
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