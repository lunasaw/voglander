package io.github.lunasaw.voglander.manager.manager;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.common.check.Assert;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.manager.assembler.DeptAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.DeptDTO;
import io.github.lunasaw.voglander.manager.service.DeptService;
import io.github.lunasaw.voglander.repository.cache.redis.RedisCache;
import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtil;
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

    @Autowired
    private RedisCache          redisCache;

    @Autowired
    private RedisLockUtil       redisLockUtil;

    // 缓存key常量
    private static final String DEPT_CACHE_PREFIX   = "dept:";
    private static final String DEPT_LIST_CACHE_KEY = "dept_list:all";
    private static final String DEPT_TREE_CACHE_KEY = "dept_tree:all";
    private static final String DEPT_LOCK_PREFIX    = "dept_lock:";

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

        // 调用统一入口方法
        return deptInternal(deptDO, "创建部门");
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

        // 调用统一入口方法
        Long result = deptInternal(deptDO, "更新部门");
        return result != null;
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

        // 调用统一删除入口方法
        return deleteDeptInternal(id, "删除部门");
    }

    /**
     * 部门数据操作统一入口 - 负责统一的缓存清理、日志记录和数据校验
     *
     * @param deptDO 部门数据对象
     * @param operationType 操作类型描述
     * @return 部门ID
     */
    private Long deptInternal(DeptDO deptDO, String operationType) {
        Assert.notNull(deptDO, "部门数据不能为空");
        Assert.hasText(operationType, "操作类型不能为空");

        String lockKey = DEPT_LOCK_PREFIX + (deptDO.getId() != null ? deptDO.getId() : "new");

        try {
            // 获取分布式锁
            if (!redisLockUtil.tryLock(lockKey, 5)) {
                throw new ServiceException("系统繁忙，请稍后重试");
            }

            boolean isUpdate = deptDO.getId() != null;
            boolean result;

            if (isUpdate) {
                result = deptService.updateById(deptDO);
            } else {
                result = deptService.save(deptDO);
            }

            if (result) {
                // 清理相关缓存
                clearDeptCache(deptDO.getId());

                log.info("{}成功，部门ID：{}，部门名称：{}", operationType, deptDO.getId(), deptDO.getDeptName());
                return deptDO.getId();
            } else {
                throw new ServiceException(operationType + "失败");
            }
        } catch (Exception e) {
            log.error("{}失败，部门ID：{}，错误信息：{}", operationType, deptDO.getId(), e.getMessage());
            throw e;
        } finally {
            // 释放分布式锁
            redisLockUtil.unLock(lockKey);
        }
    }

    /**
     * 部门删除统一入口 - 负责统一的缓存清理、日志记录和数据校验
     *
     * @param deptId 部门ID
     * @param operationType 操作类型描述
     * @return 是否删除成功
     */
    private boolean deleteDeptInternal(Long deptId, String operationType) {
        Assert.notNull(deptId, "部门ID不能为空");
        Assert.hasText(operationType, "操作类型不能为空");

        String lockKey = DEPT_LOCK_PREFIX + deptId;

        try {
            // 获取分布式锁
            if (!redisLockUtil.tryLock(lockKey, 5)) {
                throw new ServiceException("系统繁忙，请稍后重试");
            }

            boolean result = deptService.removeById(deptId);

            if (result) {
                // 清理相关缓存
                clearDeptCache(deptId);

                log.info("{}成功，部门ID：{}", operationType, deptId);
                return true;
            } else {
                throw new ServiceException(operationType + "失败");
            }
        } catch (Exception e) {
            log.error("{}失败，部门ID：{}，错误信息：{}", operationType, deptId, e.getMessage());
            throw e;
        } finally {
            // 释放分布式锁
            redisLockUtil.unLock(lockKey);
        }
    }

    /**
     * 清理部门相关缓存
     *
     * @param deptId 部门ID（可为null）
     */
    private void clearDeptCache(Long deptId) {
        try {
            // 清理单个部门缓存
            if (deptId != null) {
                redisCache.deleteKey(DEPT_CACHE_PREFIX + deptId);
            }

            // 清理部门列表缓存
            redisCache.deleteKey(DEPT_LIST_CACHE_KEY);

            // 清理部门树缓存
            redisCache.deleteKey(DEPT_TREE_CACHE_KEY);

            log.debug("清理部门缓存成功，部门ID：{}", deptId);
        } catch (Exception e) {
            log.error("清理部门缓存失败，部门ID：{}，错误信息：{}", deptId, e.getMessage());
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