package io.github.lunasaw.voglander.manager.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.assembler.RoleAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.RoleDTO;
import io.github.lunasaw.voglander.manager.service.RoleService;
import io.github.lunasaw.voglander.repository.cache.redis.RedisCache;
import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtil;
import io.github.lunasaw.voglander.repository.entity.MenuDO;
import io.github.lunasaw.voglander.repository.entity.RoleDO;
import io.github.lunasaw.voglander.repository.mapper.MenuMapper;
import io.github.lunasaw.voglander.repository.mapper.RoleMapper;
import lombok.extern.slf4j.Slf4j;
import com.luna.common.check.Assert;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 角色管理器
 * 负责处理角色相关的复杂业务逻辑
 *
 * <p>
 * 架构设计：基于标准模板方法的高度复用设计
 * </p>
 * <ul>
 * <li>核心CRUD模板：{@link #add(RoleDTO)} - 标准新增模板</li>
 * <li>智能更新模板：{@link #update(RoleDTO, RoleDTO)} - 支持条件更新策略</li>
 * <li>类型安全查询：{@link #get(RoleDTO)} - 基于LambdaQueryWrapper的查询模板</li>
 * <li>删除操作模板：{@link #deleteOne(RoleDTO)} - 单条删除模板</li>
 * <li>批量删除模板：{@link #deleteBatch(RoleDTO)} - 批量删除模板</li>
 * <li>分页查询模板：{@link #getPage(RoleDTO, int, int)} - 标准分页模板</li>
 * <li>统一缓存管理：{@link #clearCache(Long, String, String)} - 统一的缓存清理逻辑</li>
 * </ul>
 *
 * <p>
 * 业务扩展模板：
 * </p>
 * <ul>
 * <li>角色权限管理：{@link #createRole(RoleDTO)} - 带权限关联的创建操作</li>
 * <li>角色更新扩展：{@link #updateRole(Long, RoleDTO)} - 带权限更新的业务方法</li>
 * <li>角色删除扩展：{@link #deleteRole(Long)} - 带权限清理的删除操作</li>
 * <li>角色查询扩展：{@link #getRoleById(Long)} - 带权限信息的查询操作</li>
 * <li>角色列表扩展：{@link #getRoleList(RoleDTO)} - 带权限信息的分页查询</li>
 * </ul>
 *
 * <p>
 * 设计优势：
 * </p>
 * <ul>
 * <li>高度复用：所有CRUD操作基于统一模板方法，代码复用率>90%</li>
 * <li>缓存一致性：所有修改和删除操作都通过统一入口，确保缓存清理的一致性</li>
 * <li>类型安全：使用LambdaQueryWrapper和DTO接口，确保编译时类型检查</li>
 * <li>易于扩展：新功能可直接基于模板方法实现，支持权限管理等增强功能</li>
 * <li>维护简便：核心逻辑集中在模板方法中，业务变更影响面小</li>
 * </ul>
 *
 * @author luna
 */
@Slf4j
@Component
public class RoleManager {

    /**
     * 角色缓存Key前缀
     */
    private static final String ROLE_CACHE_PREFIX      = "role:";

    /**
     * 角色列表缓存Key
     */
    private static final String ROLE_LIST_CACHE_KEY    = "role:list";

    /**
     * 用户角色缓存Key前缀
     */
    private static final String USER_ROLE_CACHE_PREFIX = "user:role:";

    /**
     * 角色分布式锁前缀
     */
    private static final String ROLE_LOCK_PREFIX       = "role:lock:";

    @Autowired
    private CacheManager        cacheManager;

    @Autowired
    private MenuMapper          menuMapper;

    @Autowired
    private RoleMapper          roleMapper;

    @Autowired
    private RedisCache          redisCache;

    @Autowired
    private RedisLockUtil       redisLockUtil;

    /**
     * 模板方法：统一缓存清理
     * 每个Manager都需要的基础方法，提供高度复用且易于维护的缓存管理
     * 
     * @param id 主键ID（可为null）
     * @param oldRoleName 旧角色名（可为null）
     * @param newRoleName 新角色名（可为null）
     */
    private void clearCache(Long id, String oldRoleName, String newRoleName) {
        try {
            // 清理单个角色缓存
            if (id != null) {
                Optional.ofNullable(cacheManager.getCache("role"))
                    .ifPresent(cache -> cache.evict(id));
                redisCache.deleteKey(ROLE_CACHE_PREFIX + id);
                log.debug("清理角色ID缓存: {}", id);
            }

            // 清理角色列表缓存
            Optional.ofNullable(cacheManager.getCache("role"))
                .ifPresent(cache -> cache.evict(ROLE_LIST_CACHE_KEY));
            redisCache.deleteKey(ROLE_LIST_CACHE_KEY);

            // 清理用户角色缓存（通过pattern匹配）
            redisCache.deleteKey(redisCache.keys(USER_ROLE_CACHE_PREFIX + "*"));

            log.debug("清理角色相关缓存成功，角色ID：{}", id);
        } catch (Exception e) {
            log.warn("缓存清理异常，但不影响业务流程: {}", e.getMessage());
        }
    }

    // ================================
    // 核心模板方法（标准CRUD操作）
    // ================================

    /**
     * 模板方法：新增数据
     * 标准的数据新增流程：校验参数 -> 转换DO -> 插入数据库 -> 返回ID
     *
     * @param roleDTO 角色数据传输对象
     * @return 新增记录的ID
     * @throws IllegalArgumentException 当必要参数为空时
     * @throws RuntimeException 当数据库操作失败时
     */
    public Long add(RoleDTO roleDTO) {
        // 校验必要参数
        Assert.notNull(roleDTO, "角色信息不能为空");
        Assert.hasText(roleDTO.getRoleName(), "角色名称不能为空");

        try {
            log.info("开始新增角色 - 角色名称: {}", roleDTO.getRoleName());

            // 转为DO
            RoleDO roleDO = RoleAssembler.toDO(roleDTO);
            roleDO.setCreateTime(LocalDateTime.now());
            roleDO.setUpdateTime(LocalDateTime.now());

            // 插入数据库
            int result = roleMapper.insert(roleDO);
            if (result <= 0) {
                throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION, "数据库插入失败");
            }

            // 清理相关缓存
            clearCache(roleDO.getId(), null, roleDO.getRoleName());

            log.info("新增角色成功 - ID: {}, 角色名称: {}", roleDO.getId(), roleDTO.getRoleName());
            return roleDO.getId();

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("新增角色失败 - 角色名称: {}, 错误: {}", roleDTO.getRoleName(), e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION, e.getMessage());
        }
    }

    /**
     * 模板方法：条件更新数据（通用版本）
     * 标准的数据更新流程：校验参数 -> 根据查询条件查找记录 -> 应用更新内容 -> 更新数据库
     * 
     * @param queryDTO 查询条件DTO（用于定位要更新的记录）
     * @param updateDTO 更新内容DTO（要设置的字段值）
     * @return 更新记录的ID
     * @throws IllegalArgumentException 当必要参数为空时
     * @throws RuntimeException 当数据库操作失败时或未找到记录时
     */
    public Long update(RoleDTO queryDTO, RoleDTO updateDTO) {
        // 校验参数
        Assert.notNull(queryDTO, "查询条件不能为空");
        Assert.notNull(updateDTO, "更新内容不能为空");

        try {
            log.info("开始条件更新角色 - 查询条件: ID={}, 角色名称={}",
                queryDTO.getId(), queryDTO.getRoleName());

            // 1. 根据查询条件查找现有记录
            RoleDO queryDO = RoleAssembler.toDO(queryDTO);
            LambdaQueryWrapper<RoleDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(queryDO.getId() != null, RoleDO::getId, queryDO.getId())
                .eq(StringUtils.isNotBlank(queryDO.getRoleName()), RoleDO::getRoleName, queryDO.getRoleName())
                .eq(queryDO.getStatus() != null, RoleDO::getStatus, queryDO.getStatus())
                .last("limit 1");

            RoleDO existingRecord = roleMapper.selectOne(queryWrapper);
            if (existingRecord == null) {
                throw new ServiceException(ServiceExceptionEnum.DATA_NOT_EXISTS, "未找到要更新的角色记录");
            }

            String oldRoleName = existingRecord.getRoleName();

            // 2. 准备更新的DO对象
            RoleDO updateDO = RoleAssembler.toDO(updateDTO);
            updateDO.setId(existingRecord.getId());
            updateDO.setUpdateTime(LocalDateTime.now());

            // 3. 执行更新操作
            int result = roleMapper.updateById(updateDO);
            if (result <= 0) {
                throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION, "数据库更新失败");
            }

            // 4. 清理相关缓存
            clearCache(existingRecord.getId(), oldRoleName, updateDO.getRoleName());

            log.info("条件更新角色成功 - ID: {}", existingRecord.getId());
            return existingRecord.getId();

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("条件更新角色失败 - 错误: {}", e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION, e.getMessage());
        }
    }

    /**
     * 扩展方法：通过ID更新指定字段
     * 
     * @param id 记录ID
     * @param updateDTO 要更新的字段内容
     * @return 更新记录的ID
     */
    public Long updateById(Long id, RoleDTO updateDTO) {
        Assert.notNull(id, "ID不能为空");
        Assert.notNull(updateDTO, "更新内容不能为空");

        RoleDTO queryDTO = new RoleDTO();
        queryDTO.setId(id);

        return update(queryDTO, updateDTO);
    }

    /**
     * 模板方法：单条查询
     * 标准的数据查询流程：校验参数 -> 转换DO条件 -> 查询数据库 -> 转换并返回DTO
     *
     * @param roleDTO 查询条件（支持ID、角色名等条件）
     * @return 查询结果DTO，未找到时返回null
     * @throws IllegalArgumentException 当查询条件为空时
     */
    public RoleDTO get(RoleDTO roleDTO) {
        Assert.notNull(roleDTO, "查询条件不能为空");

        try {
            log.debug("开始查询角色 - ID: {}, 角色名称: {}", roleDTO.getId(), roleDTO.getRoleName());

            // 转为DO条件搜索
            RoleDO queryDO = RoleAssembler.toDO(roleDTO);
            LambdaQueryWrapper<RoleDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(queryDO.getId() != null, RoleDO::getId, queryDO.getId())
                .eq(StringUtils.isNotBlank(queryDO.getRoleName()), RoleDO::getRoleName, queryDO.getRoleName())
                .eq(queryDO.getStatus() != null, RoleDO::getStatus, queryDO.getStatus())
                .last("limit 1");

            RoleDO existingRecord = roleMapper.selectOne(queryWrapper);
            if (existingRecord == null) {
                log.debug("未找到匹配的角色记录");
                return null;
            }

            // 转换并返回DTO
            RoleDTO resultDTO = RoleAssembler.toDTO(existingRecord);
            log.debug("查询角色成功 - ID: {}", existingRecord.getId());

            return resultDTO;

        } catch (Exception e) {
            log.error("查询角色失败 - 错误: {}", e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION, e.getMessage());
        }
    }

    /**
     * 模板方法：删除单条记录
     * 标准的单条删除流程：校验参数 -> 通过条件查找 -> 删除数据库记录 -> 清理缓存
     *
     * @param roleDTO 删除条件
     * @return 删除结果，true表示删除成功，false表示未找到记录
     * @throws IllegalArgumentException 当删除条件为空时
     * @throws RuntimeException 当数据库操作失败时
     */
    public Boolean deleteOne(RoleDTO roleDTO) {
        Assert.notNull(roleDTO, "删除条件不能为空");

        try {
            log.info("开始删除单条角色 - ID: {}, 角色名称: {}", roleDTO.getId(), roleDTO.getRoleName());

            // 构建查询条件
            RoleDO queryDO = RoleAssembler.toDO(roleDTO);
            LambdaQueryWrapper<RoleDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(queryDO.getId() != null, RoleDO::getId, queryDO.getId())
                .eq(StringUtils.isNotBlank(queryDO.getRoleName()), RoleDO::getRoleName, queryDO.getRoleName())
                .eq(queryDO.getStatus() != null, RoleDO::getStatus, queryDO.getStatus())
                .last("limit 1");

            RoleDO existingRecord = roleMapper.selectOne(queryWrapper);
            if (existingRecord == null) {
                log.warn("未找到要删除的角色记录 - ID: {}, 角色名称: {}", roleDTO.getId(), roleDTO.getRoleName());
                return false;
            }

            // 先删除角色权限关联
            roleMapper.deleteRoleMenuByRoleId(existingRecord.getId());

            // 执行删除操作
            int result = roleMapper.deleteById(existingRecord.getId());
            if (result <= 0) {
                throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION, "数据库删除失败");
            }

            // 清理相关缓存
            clearCache(existingRecord.getId(), existingRecord.getRoleName(), null);

            log.info("删除单条角色成功 - ID: {}, 角色名称: {}",
                existingRecord.getId(), existingRecord.getRoleName());

            return true;

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除单条角色失败 - 错误: {}", e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION, e.getMessage());
        }
    }

    /**
     * 模板方法：批量删除记录
     * 标准的批量删除流程：校验参数 -> 转换DO条件 -> 查询匹配记录 -> 批量删除 -> 清理缓存
     *
     * @param roleDTO 删除条件
     * @return 删除结果，true表示删除成功
     * @throws IllegalArgumentException 当删除条件为空时
     * @throws RuntimeException 当数据库操作失败时
     */
    public Boolean deleteBatch(RoleDTO roleDTO) {
        Assert.notNull(roleDTO, "删除条件不能为空");

        try {
            log.info("开始批量删除角色 - 状态: {}", roleDTO.getStatus());

            // 构建查询条件
            RoleDO queryDO = RoleAssembler.toDO(roleDTO);
            LambdaQueryWrapper<RoleDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(queryDO.getStatus() != null, RoleDO::getStatus, queryDO.getStatus())
                .like(StringUtils.isNotBlank(queryDO.getRoleName()), RoleDO::getRoleName, queryDO.getRoleName());

            // 先查询要删除的记录
            List<RoleDO> toDeleteRecords = roleMapper.selectList(queryWrapper);
            if (CollectionUtils.isEmpty(toDeleteRecords)) {
                log.info("未找到匹配的角色记录，删除条件可能无匹配数据");
                return true;
            }

            log.info("找到{}条匹配记录，准备批量删除", toDeleteRecords.size());

            // 批量删除角色权限关联
            for (RoleDO record : toDeleteRecords) {
                roleMapper.deleteRoleMenuByRoleId(record.getId());
            }

            // 执行批量删除操作
            int result = roleMapper.delete(queryWrapper);
            if (result <= 0) {
                throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION, "数据库批量删除失败");
            }

            // 批量清理相关缓存
            for (RoleDO record : toDeleteRecords) {
                clearCache(record.getId(), record.getRoleName(), null);
            }

            log.info("批量删除角色成功 - 删除了{}条记录", toDeleteRecords.size());
            return true;

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量删除角色失败 - 错误: {}", e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION, e.getMessage());
        }
    }

    /**
     * 模板方法：分页查询
     * 标准的分页查询流程：校验参数 -> 转换DO条件 -> 分页查询数据库 -> 转换记录为DTO -> 返回Page<DTO>
     *
     * @param roleDTO 查询条件（可为null表示查询所有）
     * @param page 页码（从1开始）
     * @param size 页大小
     * @return 分页结果Page<RoleDTO>
     * @throws IllegalArgumentException 当分页参数无效时
     * @throws RuntimeException 当数据库操作失败时
     */
    public Page<RoleDTO> getPage(RoleDTO roleDTO, int page, int size) {
        // 校验分页参数
        if (page < 1) {
            throw new IllegalArgumentException("页码必须大于0");
        }
        if (size < 1 || size > 1000) {
            throw new IllegalArgumentException("页大小必须在1-1000之间");
        }

        try {
            log.debug("开始分页查询角色 - page: {}, size: {}, 查询条件: {}", page, size,
                roleDTO != null ? roleDTO.getRoleName() : "无条件查询");

            // 构建查询条件
            LambdaQueryWrapper<RoleDO> queryWrapper = new LambdaQueryWrapper<>();
            if (roleDTO != null) {
                RoleDO queryDO = RoleAssembler.toDO(roleDTO);
                queryWrapper.like(StringUtils.isNotBlank(queryDO.getRoleName()), RoleDO::getRoleName, queryDO.getRoleName())
                    .eq(queryDO.getStatus() != null, RoleDO::getStatus, queryDO.getStatus());
            }

            // 默认按创建时间降序排列
            queryWrapper.orderByDesc(RoleDO::getCreateTime);

            // 创建分页对象
            Page<RoleDO> pageQuery = new Page<>(page, size);

            // 执行分页查询
            Page<RoleDO> doPage = roleMapper.selectPage(pageQuery, queryWrapper);

            // 转换为DTO分页结果
            Page<RoleDTO> dtoPage = new Page<>(page, size);
            dtoPage.setTotal(doPage.getTotal());
            dtoPage.setPages(doPage.getPages());
            dtoPage.setCurrent(doPage.getCurrent());
            dtoPage.setSize(doPage.getSize());

            // 转换记录为DTO
            List<RoleDTO> dtoRecords = RoleAssembler.toDTOList(doPage.getRecords());
            dtoPage.setRecords(dtoRecords);

            log.debug("分页查询角色成功 - 总记录数: {}, 当前页: {}, 页大小: {}, 总页数: {}",
                doPage.getTotal(), doPage.getCurrent(), doPage.getSize(), doPage.getPages());

            return dtoPage;

        } catch (Exception e) {
            log.error("分页查询角色失败 - page: {}, size: {}, 错误: {}", page, size, e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION, e.getMessage());
        }
    }

    /**
     * 通过ID查询角色（便捷方法）
     * 
     * @param id 角色ID
     * @return 角色信息
     */
    public RoleDTO getById(Long id) {
        Assert.notNull(id, "ID不能为空");
        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setId(id);
        return get(roleDTO);
    }

    // ================================
    // 业务扩展方法（兼容现有功能）
    // ================================

    /**
     * 分页查询角色列表（包含权限信息）
     *
     * @param dto 查询条件
     * @return 角色列表分页数据
     */
    public IPage<RoleDTO> getRoleList(RoleDTO dto) {
        Page<RoleDO> page = new Page<>(
            dto.getPageNum() != null ? dto.getPageNum() : 1,
            dto.getPageSize() != null ? dto.getPageSize() : 10);

        // 构建查询条件
        LambdaQueryWrapper<RoleDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(dto.getRoleName()), RoleDO::getRoleName, dto.getRoleName())
            .eq(dto.getStatus() != null, RoleDO::getStatus, dto.getStatus())
            .orderByDesc(RoleDO::getCreateTime);

        // 使用RoleMapper进行分页查询
        IPage<RoleDO> rolePage = roleMapper.selectPage(page, queryWrapper);

        // 转换结果并填充权限信息
        IPage<RoleDTO> result = new Page<>();
        result.setCurrent(rolePage.getCurrent());
        result.setSize(rolePage.getSize());
        result.setTotal(rolePage.getTotal());
        result.setPages(rolePage.getPages());

        List<RoleDTO> roleDTOList = RoleAssembler.toDTOList(rolePage.getRecords());

        // 为每个角色查询并设置权限信息
        if (roleDTOList != null) {
            roleDTOList.forEach(roleDTO -> {
                List<MenuDO> menuList = menuMapper.selectMenusByRoleId(roleDTO.getId());
                roleDTO.setPermissions(RoleAssembler.menuListToPermissionIds(menuList));
            });
        }

        result.setRecords(roleDTOList);
        return result;
    }

    /**
     * 根据ID获取角色信息（包含权限信息）
     *
     * @param id 角色ID
     * @return 角色信息
     */
    public RoleDTO getRoleById(Long id) {
        if (id == null) {
            return null;
        }

        // 使用RoleMapper进行查询
        RoleDO roleDO = roleMapper.selectById(id);
        if (roleDO == null) {
            return null;
        }

        RoleDTO dto = RoleAssembler.toDTO(roleDO);

        // 查询角色权限
        List<MenuDO> menuList = menuMapper.selectMenusByRoleId(id);
        dto.setPermissions(RoleAssembler.menuListToPermissionIds(menuList));

        return dto;
    }

    /**
     * 创建角色（包含权限关联）
     *
     * @param dto 角色信息
     * @return 创建结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean createRole(RoleDTO dto) {
        Assert.notNull(dto, "角色信息不能为空");
        Assert.hasText(dto.getRoleName(), "角色名称不能为空");

        RoleDO roleDO = RoleAssembler.createRoleDO(dto);
        roleDO.setCreateTime(LocalDateTime.now());

        // 调用统一入口方法
        Long roleId = roleInternal(roleDO, "创建角色");

        if (roleId != null) {
            // 创建角色权限关联
            if (dto.getPermissions() != null && !dto.getPermissions().isEmpty()) {
                updateRolePermissions(roleId, dto.getPermissions());
            }
            return true;
        }
        return false;
    }

    /**
     * 更新角色（包含权限关联）
     *
     * @param id 角色ID
     * @param dto 角色信息
     * @return 更新结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(Long id, RoleDTO dto) {
        Assert.notNull(id, "角色ID不能为空");
        Assert.notNull(dto, "角色信息不能为空");

        RoleDO existingRole = roleMapper.selectById(id);
        if (existingRole == null) {
            throw new ServiceException("角色不存在");
        }

        // 更新角色基本信息
        RoleAssembler.updateRoleDO(existingRole, dto);
        existingRole.setUpdateTime(LocalDateTime.now());

        // 调用统一入口方法
        Long roleId = roleInternal(existingRole, "更新角色");

        if (roleId != null) {
            // 更新角色权限关联
            updateRolePermissions(id, dto.getPermissions());
            return true;
        }
        return false;
    }

    /**
     * 删除角色（包含权限关联）
     *
     * @param id 角色ID
     * @return 删除结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRole(Long id) {
        Assert.notNull(id, "角色ID不能为空");

        RoleDO role = roleMapper.selectById(id);
        if (role == null) {
            throw new ServiceException("角色不存在");
        }

        // 检查是否有用户使用该角色
        // 这里可以根据业务需求添加检查逻辑

        // 调用统一删除入口方法
        return deleteRoleInternal(id, "删除角色");
    }

    /**
     * 根据用户ID获取角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    public List<RoleDTO> getRolesByUserId(Long userId) {
        Assert.notNull(userId, "用户ID不能为空");

        List<RoleDO> roleList = roleMapper.selectRolesByUserId(userId);
        return RoleAssembler.toDTOList(roleList);
    }

    // ================================
    // 内部方法（保持与原有实现兼容）
    // ================================

    /**
     * 角色数据操作统一入口 - 负责统一的缓存清理、日志记录和数据校验
     *
     * @param roleDO 角色数据对象
     * @param operationType 操作类型描述
     * @return 角色ID
     */
    private Long roleInternal(RoleDO roleDO, String operationType) {
        Assert.notNull(roleDO, "角色数据不能为空");
        Assert.hasText(operationType, "操作类型不能为空");

        String lockKey = ROLE_LOCK_PREFIX + (roleDO.getId() != null ? roleDO.getId() : "new");

        try {
            // 获取分布式锁
            if (!redisLockUtil.tryLock(lockKey, 5)) {
                throw new ServiceException("系统繁忙，请稍后重试");
            }

            boolean isUpdate = roleDO.getId() != null;
            boolean result;

            if (isUpdate) {
                result = roleMapper.updateById(roleDO) > 0;
            } else {
                result = roleMapper.insert(roleDO) > 0;
            }

            if (result) {
                // 清理相关缓存
                clearRoleCache(roleDO.getId());

                log.info("{}成功，角色ID：{}，角色名称：{}", operationType, roleDO.getId(), roleDO.getRoleName());
                return roleDO.getId();
            } else {
                throw new ServiceException(operationType + "失败");
            }
        } catch (Exception e) {
            log.error("{}失败，角色ID：{}，错误信息：{}", operationType, roleDO.getId(), e.getMessage());
            throw e;
        } finally {
            // 释放分布式锁
            redisLockUtil.unLock(lockKey);
        }
    }

    /**
     * 角色删除统一入口 - 负责统一的缓存清理、日志记录和数据校验
     *
     * @param roleId 角色ID
     * @param operationType 操作类型描述
     * @return 是否删除成功
     */
    private boolean deleteRoleInternal(Long roleId, String operationType) {
        Assert.notNull(roleId, "角色ID不能为空");
        Assert.hasText(operationType, "操作类型不能为空");

        String lockKey = ROLE_LOCK_PREFIX + roleId;

        try {
            // 获取分布式锁
            if (!redisLockUtil.tryLock(lockKey, 5)) {
                throw new ServiceException("系统繁忙，请稍后重试");
            }

            // 先删除角色权限关联
            roleMapper.deleteRoleMenuByRoleId(roleId);

            boolean result = roleMapper.deleteById(roleId) > 0;

            if (result) {
                // 清理相关缓存
                clearRoleCache(roleId);

                log.info("{}成功，角色ID：{}", operationType, roleId);
                return true;
            } else {
                throw new ServiceException(operationType + "失败");
            }
        } catch (Exception e) {
            log.error("{}失败，角色ID：{}，错误信息：{}", operationType, roleId, e.getMessage());
            throw e;
        } finally {
            // 释放分布式锁
            redisLockUtil.unLock(lockKey);
        }
    }

    /**
     * 清理角色相关缓存（兼容原有方法）
     *
     * @param roleId 角色ID（可为null）
     */
    private void clearRoleCache(Long roleId) {
        clearCache(roleId, null, null);
    }

    /**
     * 更新角色权限关联
     *
     * @param roleId 角色ID
     * @param permissions 权限列表（菜单ID列表）
     */
    private void updateRolePermissions(Long roleId, List<Long> permissions) {
        // 先删除该角色的所有权限关联
        roleMapper.deleteRoleMenuByRoleId(roleId);

        // 如果有新的权限，批量插入
        if (permissions != null && !permissions.isEmpty()) {
            roleMapper.batchInsertRoleMenu(roleId, permissions);
            log.info("更新角色权限成功，角色ID：{}，权限菜单ID：{}", roleId, permissions);
        } else {
            log.info("清空角色权限，角色ID：{}", roleId);
        }

        // 权限更新后需要清理相关缓存
        clearRoleCache(roleId);
    }
}