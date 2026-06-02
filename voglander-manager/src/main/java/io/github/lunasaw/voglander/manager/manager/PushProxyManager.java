package io.github.lunasaw.voglander.manager.manager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.manager.assembler.PushProxyAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.PushProxyDTO;
import io.github.lunasaw.voglander.manager.service.PushProxyService;
import io.github.lunasaw.voglander.repository.entity.PushProxyDO;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * 推流代理管理器
 * 负责处理推流代理相关的复杂业务逻辑
 *
 * <p>
 * 架构设计：基于标准模板方法的高度复用设计
 * </p>
 * <ul>
 * <li>核心CRUD模板：{@link #add(PushProxyDTO)} - 标准新增模板</li>
 * <li>智能更新模板：{@link #update(PushProxyDTO)} - 支持ID和业务键的更新策略</li>
 * <li>智能更新模板：{@link #update(PushProxyDTO,PushProxyDTO)} - 支持ID和业务键的更新策略</li>
 * <li>类型安全查询：{@link #get(PushProxyDTO)} - 基于LambdaQueryWrapper的查询模板</li>
 * <li>删除操作模板：{@link #deleteOne(PushProxyDTO)} - 单条删除和批量删除模板</li>
 * <li>分页查询模板：{@link #getPage(PushProxyDTO, int, int)} - 标准分页模板</li>
 * <li>统一缓存管理：{@link #clearCache(Long, String, String)} - 统一的缓存清理逻辑</li>
 * </ul>
 *
 * <p>
 * 模板扩展示例：
 * </p>
 * <ul>
 * <li>操作日志模板：{@link #updatePushProxy(PushProxyDTO, String)} - 带操作日志的业务方法模板</li>
 * <li>通用日志包装器：{@link #executeWithOperationLogging} - 统一操作日志记录模板</li>
 * </ul>
 *
 * <p>
 * 设计优势：
 * </p>
 * <ul>
 * <li>高度复用：所有CRUD操作基于统一模板方法，代码复用率>90%</li>
 * <li>缓存一致性：所有修改和删除操作都通过统一入口，确保缓存清理的一致性</li>
 * <li>类型安全：使用LambdaQueryWrapper和DTO接口，确保编译时类型检查</li>
 * <li>易于扩展：新功能可直接基于模板方法实现，支持操作日志等增强功能</li>
 * <li>维护简便：核心逻辑集中在模板方法中，业务变更影响面小</li>
 * </ul>
 *
 * <p>
 * Manager层设计规范：
 * </p>
 * <ul>
 * <li>对外接口：所有公开方法参数和返回值均使用DTO类型</li>
 * <li>内部实现：通过Assembler进行DTO和DO之间的转换</li>
 * <li>查询策略：智能查询策略（ID优先，业务键备用）</li>
 * <li>异常处理：统一异常处理和日志记录格式</li>
 * </ul>
 *
 * @author luna
 * @since 2025-01-23
 */
@Slf4j
@Component
public class PushProxyManager {

    @Autowired
    private PushProxyService   pushProxyService;

    @Autowired
    private PushProxyAssembler pushProxyAssembler;

    @Autowired
    private CacheManager       cacheManager;

    /**
     * 模板方法：统一缓存清理
     * 每个Manager都需要的基础方法，提供高度复用且易于维护的缓存管理
     * 
     * @param id 主键ID
     * @param oldKey 旧的业务键（可能为空）
     * @param newKey 新的业务键（可能为空）
     */
    private void clearCache(Long id, String oldKey, String newKey) {
        try {
            // 根据ID清理缓存
            if (id != null) {
                Optional.ofNullable(cacheManager.getCache("pushProxy"))
                    .ifPresent(cache -> cache.evict(id));
                log.debug("清理ID缓存: {}", id);
            }

            // 根据旧业务键清理缓存
            if (oldKey != null) {
                Optional.ofNullable(cacheManager.getCache("pushProxy"))
                    .ifPresent(cache -> cache.evict("key:" + oldKey));
                log.debug("清理旧业务键缓存: {}", oldKey);
            }

            // 根据新业务键清理缓存（如果与旧键不同）
            if (newKey != null && !newKey.equals(oldKey)) {
                Optional.ofNullable(cacheManager.getCache("pushProxy"))
                    .ifPresent(cache -> cache.evict("key:" + newKey));
                log.debug("清理新业务键缓存: {}", newKey);
            }
        } catch (Exception e) {
            log.warn("缓存清理异常，但不影响业务流程: {}", e.getMessage());
        }
    }

    /**
     * 模板方法：通用操作日志记录包装器
     * 统一的操作日志记录模板：记录操作开始 -> 执行业务逻辑 -> 记录操作结果
     * 所有需要记录用户操作日志的方法都应使用此模板实现
     *
     * @param pushProxyDTO 操作的代理信息
     * @param operationDesc 操作描述
     * @param operation 具体的业务操作函数
     * @param <T> 操作返回值类型
     * @return 操作执行结果
     */
    private <T> T executeWithOperationLogging(PushProxyDTO pushProxyDTO, String operationDesc,
        java.util.function.Supplier<T> operation) {
        try {
            // 操作日志：记录操作开始
            log.info("开始{} - 代理信息: ID={}, app={}, stream={}, dstUrl={}, proxyKey={}",
                operationDesc,
                pushProxyDTO.getId(),
                pushProxyDTO.getApp(),
                pushProxyDTO.getStream(),
                pushProxyDTO.getDstUrl(),
                pushProxyDTO.getProxyKey());

            // 执行核心业务逻辑
            T result = operation.get();

            // 操作日志：记录操作成功结果
            log.info("完成{} - 操作成功，代理信息: ID={}, app={}, stream={}, dstUrl={}, proxyKey={}",
                operationDesc,
                pushProxyDTO.getId(),
                pushProxyDTO.getApp(),
                pushProxyDTO.getStream(),
                pushProxyDTO.getDstUrl(),
                pushProxyDTO.getProxyKey());

            return result;

        } catch (Exception e) {
            // 操作日志：记录操作失败
            log.error("{}失败 - 代理信息: ID={}, app={}, stream={}, dstUrl={}, proxyKey={}, 错误: {}",
                operationDesc,
                pushProxyDTO.getId(),
                pushProxyDTO.getApp(),
                pushProxyDTO.getStream(),
                pushProxyDTO.getDstUrl(),
                pushProxyDTO.getProxyKey(),
                e.getMessage(), e);

            // 重新抛出异常，保持原有的异常处理逻辑
            throw e;
        }
    }

    /**
     * 模板方法：新增数据
     * 标准的数据新增流程：校验参数 -> 转换DO -> 插入数据库 -> 返回ID
     *
     * @param pushProxyDTO 数据传输对象
     * @return 新增记录的ID
     * @throws IllegalArgumentException 当必要参数为空时
     * @throws RuntimeException 当数据库操作失败时
     */
    public Long add(PushProxyDTO pushProxyDTO) {
        // 校验DB not null 参数
        Assert.notNull(pushProxyDTO, "代理信息不能为空");
        Assert.hasText(pushProxyDTO.getApp(), "应用名称不能为空");
        Assert.hasText(pushProxyDTO.getStream(), "流ID不能为空");
        Assert.hasText(pushProxyDTO.getDstUrl(), "推流目标地址不能为空");

        try {
            log.info("开始新增推流代理 - app: {}, stream: {}, dstUrl: {}",
                pushProxyDTO.getApp(), pushProxyDTO.getStream(), pushProxyDTO.getDstUrl());

            // 转为DO
            PushProxyDO pushProxyDO = pushProxyAssembler.dtoToDo(pushProxyDTO);
            pushProxyDO.setCreateTime(LocalDateTime.now());
            pushProxyDO.setUpdateTime(LocalDateTime.now());

            // 插入DB
            boolean success = pushProxyService.save(pushProxyDO);
            if (!success) {
                throw new ServiceException(ServiceExceptionEnum.PUSH_PROXY_OPERATION_FAILED, "数据库插入失败");
            }

            // 清理相关缓存
            clearCache(pushProxyDO.getId(), null, pushProxyDO.getProxyKey());

            log.info("新增推流代理成功 - ID: {}, app: {}, stream: {}, dstUrl: {}",
                pushProxyDO.getId(), pushProxyDTO.getApp(), pushProxyDTO.getStream(), pushProxyDTO.getDstUrl());

            // 返回ID
            return pushProxyDO.getId();

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("新增推流代理失败 - app: {}, stream: {}, dstUrl: {}, 错误: {}",
                pushProxyDTO.getApp(), pushProxyDTO.getStream(), pushProxyDTO.getDstUrl(), e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.PUSH_PROXY_OPERATION_FAILED, e.getMessage());
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
    public Long update(PushProxyDTO queryDTO, PushProxyDTO updateDTO) {
        // 校验参数
        Assert.notNull(queryDTO, "查询条件不能为空");
        Assert.notNull(updateDTO, "更新内容不能为空");

        try {
            log.info("开始条件更新推流代理 - 查询条件: ID={}, app={}, stream={}",
                queryDTO.getId(), queryDTO.getApp(), queryDTO.getStream());

            // 1. 先根据查询条件找到现有记录
            // 使用通用查询条件
            PushProxyDO queryDO = pushProxyAssembler.dtoToDo(queryDTO);
            LambdaQueryWrapper<PushProxyDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(queryDO.getId() != null, PushProxyDO::getId, queryDO.getId())
                .eq(queryDO.getApp() != null, PushProxyDO::getApp, queryDO.getApp())
                .eq(queryDO.getStream() != null, PushProxyDO::getStream, queryDO.getStream())
                .eq(queryDO.getProxyKey() != null, PushProxyDO::getProxyKey, queryDO.getProxyKey())
                .eq(queryDO.getServerId() != null, PushProxyDO::getServerId, queryDO.getServerId())
                .eq(queryDO.getStatus() != null, PushProxyDO::getStatus, queryDO.getStatus())
                .eq(queryDO.getOnlineStatus() != null, PushProxyDO::getOnlineStatus, queryDO.getOnlineStatus())
                .eq(queryDO.getEnabled() != null, PushProxyDO::getEnabled, queryDO.getEnabled())
                .last("limit 1");
            PushProxyDO existingRecord = pushProxyService.getOne(queryWrapper);

            if (existingRecord == null) {
                throw new ServiceException(ServiceExceptionEnum.PUSH_PROXY_NOT_FOUND, "未找到要更新的记录");
            }

            // 记录旧的proxyKey用于缓存清理
            String oldProxyKey = existingRecord.getProxyKey();

            // 2. 准备更新的DO对象
            PushProxyDO updateDO = pushProxyAssembler.dtoToDo(updateDTO);
            updateDO.setId(existingRecord.getId()); // 确保使用正确的ID
            updateDO.setUpdateTime(LocalDateTime.now());

            // 3. 执行更新操作
            boolean success = pushProxyService.updateById(updateDO);
            if (!success) {
                throw new ServiceException(ServiceExceptionEnum.PUSH_PROXY_OPERATION_FAILED, "数据库更新失败");
            }

            // 4. 清理相关缓存
            clearCache(existingRecord.getId(), oldProxyKey, updateDO.getProxyKey());

            log.info("条件更新推流代理成功 - ID: {}", existingRecord.getId());
            return existingRecord.getId();

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("条件更新推流代理失败 - 错误: {}", e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.PUSH_PROXY_OPERATION_FAILED, e.getMessage());
        }
    }

    /**
     * 模板方法：更新数据（便捷版本，兼容旧接口）
     * 当传入对象包含ID时，使用ID查询；否则使用其他字段组合查询
     * 
     * @param pushProxyDTO 包含查询条件和更新内容的DTO
     * @return 更新记录的ID
     * @throws IllegalArgumentException 当必要参数为空时
     * @throws RuntimeException 当数据库操作失败时
     */
    public Long update(PushProxyDTO pushProxyDTO) {
        Assert.notNull(pushProxyDTO, "代理信息不能为空");

        // 构建查询条件DTO
        PushProxyDTO queryDTO = new PushProxyDTO();
        if (pushProxyDTO.getId() != null) {
            queryDTO.setId(pushProxyDTO.getId());
        } else if (pushProxyDTO.getApp() != null && pushProxyDTO.getStream() != null) {
            queryDTO.setApp(pushProxyDTO.getApp());
            queryDTO.setStream(pushProxyDTO.getStream());
        } else if (pushProxyDTO.getProxyKey() != null) {
            queryDTO.setProxyKey(pushProxyDTO.getProxyKey());
        } else {
            // 如果没有明确的查询条件，使用原对象作为查询条件
            queryDTO = pushProxyDTO;
        }

        // 调用通用的条件更新方法
        return update(queryDTO, pushProxyDTO);
    }

    /**
     * 扩展方法：通过ID更新指定字段
     * 
     * @param id 记录ID
     * @param updateDTO 要更新的字段内容
     * @return 更新记录的ID
     */
    public Long updateById(Long id, PushProxyDTO updateDTO) {
        Assert.notNull(id, "ID不能为空");
        Assert.notNull(updateDTO, "更新内容不能为空");

        PushProxyDTO queryDTO = new PushProxyDTO();
        queryDTO.setId(id);

        return update(queryDTO, updateDTO);
    }

    /**
     * 模板方法：单条查询
     * 标准的数据查询流程：校验参数 -> 转换DO条件 -> 查询数据库 -> 转换并返回DTO
     *
     * @param pushProxyDTO 查询条件（支持ID、app+stream、proxyKey等条件）
     * @return 查询结果DTO，未找到时返回null
     * @throws IllegalArgumentException 当查询条件为空时
     */
    public PushProxyDTO get(PushProxyDTO pushProxyDTO) {
        // 校验DB not null 参数
        Assert.notNull(pushProxyDTO, "查询条件不能为空");

        try {
            log.debug("开始查询推流代理 - ID: {}, app: {}, stream: {}, proxyKey: {}",
                pushProxyDTO.getId(), pushProxyDTO.getApp(),
                pushProxyDTO.getStream(), pushProxyDTO.getProxyKey());

            // 转为DO条件搜索，按优先级查询
            PushProxyDO pushProxyDO = pushProxyAssembler.dtoToDo(pushProxyDTO);
            LambdaQueryWrapper<PushProxyDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(pushProxyDO.getId() != null, PushProxyDO::getId, pushProxyDO.getId())
                .eq(pushProxyDO.getApp() != null, PushProxyDO::getApp, pushProxyDO.getApp())
                .eq(pushProxyDO.getStream() != null, PushProxyDO::getStream, pushProxyDO.getStream())
                .eq(pushProxyDO.getProxyKey() != null, PushProxyDO::getProxyKey, pushProxyDO.getProxyKey())
                .eq(pushProxyDO.getServerId() != null, PushProxyDO::getServerId, pushProxyDO.getServerId())
                .eq(pushProxyDO.getStatus() != null, PushProxyDO::getStatus, pushProxyDO.getStatus())
                .eq(pushProxyDO.getOnlineStatus() != null, PushProxyDO::getOnlineStatus, pushProxyDO.getOnlineStatus())
                .eq(pushProxyDO.getEnabled() != null, PushProxyDO::getEnabled, pushProxyDO.getEnabled())
                .last("limit 1");
            PushProxyDO existingRecord = pushProxyService.getOne(queryWrapper);
            if (existingRecord == null) {
                log.debug("未找到匹配的推流代理记录");
                return null;
            }

            // 转换并返回DTO
            PushProxyDTO resultDTO = pushProxyAssembler.doToDto(existingRecord);
            log.debug("查询推流代理成功 - ID: {}", existingRecord.getId());

            return resultDTO;

        } catch (Exception e) {
            log.error("查询推流代理失败 - 错误: {}", e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.PUSH_PROXY_OPERATION_FAILED, e.getMessage());
        }
    }

    /**
     * 模板方法：删除单条记录
     * 标准的单条删除流程：校验参数 -> 通过唯一索引查找 -> 删除数据库记录 -> 清理缓存
     * 删除策略：优先使用ID，其次使用proxyKey，最后使用app+stream
     *
     * @param pushProxyDTO 删除条件（必须包含唯一索引字段）
     * @return 删除结果，true表示删除成功，false表示未找到记录
     * @throws IllegalArgumentException 当删除条件为空或无效时
     * @throws RuntimeException 当数据库操作失败时
     */
    public Boolean deleteOne(PushProxyDTO pushProxyDTO) {
        // 校验DB not null 参数
        Assert.notNull(pushProxyDTO, "删除条件不能为空");

        try {
            log.info("开始删除单条推流代理 - ID: {}, proxyKey: {}, app: {}, stream: {}",
                pushProxyDTO.getId(), pushProxyDTO.getProxyKey(),
                pushProxyDTO.getApp(), pushProxyDTO.getStream());

            PushProxyDO pushProxyDO = pushProxyAssembler.dtoToDo(pushProxyDTO);
            LambdaQueryWrapper<PushProxyDO> queryWrapper = new LambdaQueryWrapper<>();

            // 构建删除查询条件，优先使用ID，其次proxyKey，最后app+stream
            if (pushProxyDO.getId() != null) {
                queryWrapper.eq(PushProxyDO::getId, pushProxyDO.getId());
            } else if (pushProxyDO.getProxyKey() != null) {
                queryWrapper.eq(PushProxyDO::getProxyKey, pushProxyDO.getProxyKey());
            } else if (pushProxyDO.getApp() != null && pushProxyDO.getStream() != null) {
                queryWrapper.eq(PushProxyDO::getApp, pushProxyDO.getApp())
                    .eq(PushProxyDO::getStream, pushProxyDO.getStream());
            } else {
                // 如果没有足够的查询条件，使用所有非null字段
                queryWrapper.eq(pushProxyDO.getId() != null, PushProxyDO::getId, pushProxyDO.getId())
                    .eq(pushProxyDO.getApp() != null, PushProxyDO::getApp, pushProxyDO.getApp())
                    .eq(pushProxyDO.getStream() != null, PushProxyDO::getStream, pushProxyDO.getStream())
                    .eq(pushProxyDO.getProxyKey() != null, PushProxyDO::getProxyKey, pushProxyDO.getProxyKey())
                    .eq(pushProxyDO.getServerId() != null, PushProxyDO::getServerId, pushProxyDO.getServerId())
                    .eq(pushProxyDO.getStatus() != null, PushProxyDO::getStatus, pushProxyDO.getStatus())
                    .eq(pushProxyDO.getOnlineStatus() != null, PushProxyDO::getOnlineStatus, pushProxyDO.getOnlineStatus())
                    .eq(pushProxyDO.getEnabled() != null, PushProxyDO::getEnabled, pushProxyDO.getEnabled());
            }

            queryWrapper.last("limit 1");
            PushProxyDO existingRecord = pushProxyService.getOne(queryWrapper);
            if (existingRecord == null) {
                log.warn("未找到要删除的推流代理记录 - ID: {}, proxyKey: {}, app: {}, stream: {}",
                    pushProxyDTO.getId(), pushProxyDTO.getProxyKey(),
                    pushProxyDTO.getApp(), pushProxyDTO.getStream());
                return false;
            }
            // 执行删除操作
            boolean success = pushProxyService.removeById(existingRecord);
            if (!success) {
                throw new ServiceException(ServiceExceptionEnum.PUSH_PROXY_OPERATION_FAILED, "数据库删除失败");
            }

            // 清理相关缓存
            clearCache(existingRecord.getId(), existingRecord.getProxyKey(), null);

            log.info("删除单条推流代理成功 - ID: {}, app: {}, stream: {}, proxyKey: {}",
                existingRecord.getId(), existingRecord.getApp(),
                existingRecord.getStream(), existingRecord.getProxyKey());

            return true;

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除单条推流代理失败 - 错误: {}", e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.PUSH_PROXY_OPERATION_FAILED, e.getMessage());
        }
    }

    /**
     * 模板方法：批量删除记录
     * 标准的批量删除流程：校验参数 -> 转换DO条件 -> 查询匹配记录 -> 批量删除 -> 清理缓存
     * 注意：批量删除可能删除多条记录，不校验必须使用唯一索引
     * 上游需要确认是使用 deleteOne 还是此方法
     *
     * @param pushProxyDTO 删除条件（支持多种条件组合）
     * @return 删除结果，true表示删除成功（包括删除0条记录），false表示操作失败
     * @throws IllegalArgumentException 当删除条件为空时
     * @throws RuntimeException 当数据库操作失败时
     */
    public Boolean deleteBatch(PushProxyDTO pushProxyDTO) {
        // 校验DB not null 参数
        Assert.notNull(pushProxyDTO, "删除条件不能为空");

        try {
            log.info("开始批量删除推流代理 - app: {}, stream: {}, status: {}, onlineStatus: {}",
                pushProxyDTO.getApp(), pushProxyDTO.getStream(), pushProxyDTO.getStatus(),
                pushProxyDTO.getOnlineStatus());

            // 转为DO 批量删除 - 构建查询条件
            PushProxyDO pushProxyDO = pushProxyAssembler.dtoToDo(pushProxyDTO);
            LambdaQueryWrapper<PushProxyDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(pushProxyDO.getId() != null, PushProxyDO::getId, pushProxyDO.getId())
                .eq(pushProxyDO.getApp() != null, PushProxyDO::getApp, pushProxyDO.getApp())
                .eq(pushProxyDO.getStream() != null, PushProxyDO::getStream, pushProxyDO.getStream())
                .eq(pushProxyDO.getProxyKey() != null, PushProxyDO::getProxyKey, pushProxyDO.getProxyKey())
                .eq(pushProxyDO.getServerId() != null, PushProxyDO::getServerId, pushProxyDO.getServerId())
                .eq(pushProxyDO.getStatus() != null, PushProxyDO::getStatus, pushProxyDO.getStatus())
                .eq(pushProxyDO.getOnlineStatus() != null, PushProxyDO::getOnlineStatus, pushProxyDO.getOnlineStatus())
                .eq(pushProxyDO.getEnabled() != null, PushProxyDO::getEnabled, pushProxyDO.getEnabled());

            // 先查询要删除的记录，用于清理缓存
            List<PushProxyDO> toDeleteRecords = pushProxyService.list(queryWrapper);
            if (toDeleteRecords.isEmpty()) {
                log.info("未找到匹配的推流代理记录，删除条件可能无匹配数据");
                return true;
            }

            log.info("找到{}条匹配记录，准备批量删除", toDeleteRecords.size());

            // 执行批量删除操作
            boolean success = pushProxyService.remove(queryWrapper);
            if (!success) {
                throw new ServiceException(ServiceExceptionEnum.PUSH_PROXY_OPERATION_FAILED, "数据库批量删除失败");
            }

            // 批量清理相关缓存
            for (PushProxyDO record : toDeleteRecords) {
                clearCache(record.getId(), record.getProxyKey(), null);
            }

            log.info("批量删除推流代理成功 - 删除了{}条记录", toDeleteRecords.size());

            return true;

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量删除推流代理失败 - 错误: {}", e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.PUSH_PROXY_OPERATION_FAILED, e.getMessage());
        }
    }

    /**
     * 模板方法：分页查询
     * 标准的分页查询流程：校验参数 -> 转换DO条件 -> 分页查询数据库 -> 转换记录为DTO -> 返回Page<DTO>
     *
     * @param pushProxyDTO 查询条件（可为null表示查询所有）
     * @param page 页码（从1开始）
     * @param size 页大小
     * @return 分页结果Page<PushProxyDTO>
     * @throws IllegalArgumentException 当分页参数无效时
     * @throws RuntimeException 当数据库操作失败时
     */
    public Page<PushProxyDTO> getPage(PushProxyDTO pushProxyDTO, int page, int size) {
        // 校验DB not null 参数
        if (page < 1) {
            throw new IllegalArgumentException("页码必须大于0");
        }
        if (size < 1 || size > 1000) {
            throw new IllegalArgumentException("页大小必须在1-1000之间");
        }

        try {
            log.debug("开始分页查询推流代理 - page: {}, size: {}, 查询条件: {}", page, size,
                pushProxyDTO != null ? pushProxyDTO.toString() : "无条件查询");

            // 转为DO 条件分页查询 - 构建查询条件
            LambdaQueryWrapper<PushProxyDO> queryWrapper;
            if (pushProxyDTO != null) {
                PushProxyDO pushProxyDO = pushProxyAssembler.dtoToDo(pushProxyDTO);
                queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(pushProxyDO.getId() != null, PushProxyDO::getId, pushProxyDO.getId())
                    .eq(pushProxyDO.getApp() != null, PushProxyDO::getApp, pushProxyDO.getApp())
                    .eq(pushProxyDO.getStream() != null, PushProxyDO::getStream, pushProxyDO.getStream())
                    .eq(pushProxyDO.getProxyKey() != null, PushProxyDO::getProxyKey, pushProxyDO.getProxyKey())
                    .eq(pushProxyDO.getServerId() != null, PushProxyDO::getServerId, pushProxyDO.getServerId())
                    .eq(pushProxyDO.getStatus() != null, PushProxyDO::getStatus, pushProxyDO.getStatus())
                    .eq(pushProxyDO.getOnlineStatus() != null, PushProxyDO::getOnlineStatus, pushProxyDO.getOnlineStatus())
                    .eq(pushProxyDO.getEnabled() != null, PushProxyDO::getEnabled, pushProxyDO.getEnabled());
            } else {
                queryWrapper = new LambdaQueryWrapper<>();
            }

            // 默认按创建时间降序排列
            queryWrapper.orderByDesc(PushProxyDO::getCreateTime);

            // 创建分页对象
            Page<PushProxyDO> pageQuery = new Page<>(page, size);

            // 执行分页查询
            Page<PushProxyDO> doPage = pushProxyService.page(pageQuery, queryWrapper);

            // 转换为DTO分页结果
            Page<PushProxyDTO> dtoPage = new Page<>(page, size);
            dtoPage.setTotal(doPage.getTotal());
            dtoPage.setPages(doPage.getPages());
            dtoPage.setCurrent(doPage.getCurrent());
            dtoPage.setSize(doPage.getSize());

            // 获取record 转为 dto返回
            List<PushProxyDTO> dtoRecords = pushProxyAssembler.doListToDtoList(doPage.getRecords());
            dtoPage.setRecords(dtoRecords);

            log.debug("分页查询推流代理成功 - 总记录数: {}, 当前页: {}, 页大小: {}, 总页数: {}",
                doPage.getTotal(), doPage.getCurrent(), doPage.getSize(), doPage.getPages());

            return dtoPage;

        } catch (Exception e) {
            log.error("分页查询推流代理失败 - page: {}, size: {}, 错误: {}", page, size, e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.PUSH_PROXY_OPERATION_FAILED, e.getMessage());
        }
    }

    /**
     * 模版方法必要的扩展，通过Id，或者一位索引查询
     * 
     * @param id
     * @return
     */
    public PushProxyDTO getById(Long id) {
        Assert.notNull(id, "ID不能为空");
        PushProxyDTO pushProxyDTO = new PushProxyDTO();
        pushProxyDTO.setId(id);
        return get(pushProxyDTO);
    }

    /**
     * 创建推流代理
     * 使用标准模板方法实现
     *
     * @param pushProxyDTO 代理信息
     * @return 代理ID
     */
    public Long createPushProxy(PushProxyDTO pushProxyDTO) {
        if (pushProxyDTO.getStatus() == null) {
            pushProxyDTO.setStatus(1); // 默认启用
        }
        if (pushProxyDTO.getOnlineStatus() == null) {
            pushProxyDTO.setOnlineStatus(0); // 默认离线
        }
        if (pushProxyDTO.getEnabled() == null) {
            pushProxyDTO.setEnabled(1); // 默认启用
        }
        if (pushProxyDTO.getSchema() == null) {
            pushProxyDTO.setSchema("rtmp"); // 默认RTMP协议
        }

        // 设置ExtendObj默认值
        if (pushProxyDTO.getExtendObj() == null) {
            pushProxyDTO.setExtendObj(new PushProxyDTO.ExtendObj());
        }
        PushProxyDTO.ExtendObj extendObj = pushProxyDTO.getExtendObj();

        if (extendObj.getVhost() == null) {
            extendObj.setVhost("__defaultVhost__"); // 默认虚拟主机
        }
        if (extendObj.getRetryCount() == null) {
            extendObj.setRetryCount(-1); // 默认无限重试
        }
        if (extendObj.getRtpType() == null) {
            extendObj.setRtpType(0); // 默认TCP
        }
        if (extendObj.getTimeoutSec() == null) {
            extendObj.setTimeoutSec(10); // 默认10秒超时
        }

        return add(pushProxyDTO);
    }

    /**
     * 模板扩展方法：带操作日志的更新推流代理
     * 使用通用操作日志记录模板，简化重复代码
     *
     * @param pushProxyDTO 代理信息
     * @param operationDesc 操作描述
     * @return 是否更新成功
     */
    public Boolean updatePushProxy(PushProxyDTO pushProxyDTO, String operationDesc) {
        return executeWithOperationLogging(pushProxyDTO, operationDesc, () -> {
            Long result = update(pushProxyDTO);
            return result != null;
        });
    }

    /**
     * 分页查询推流代理
     * 使用标准模板方法实现
     *
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    public Page<PushProxyDTO> getProxyPage(int page, int size) {
        return getPage(null, page, size);
    }

    /**
     * 模板扩展方法：带操作日志的删除推流代理
     * 使用通用操作日志记录模板，简化重复代码
     *
     * @param pushProxyDTO 删除条件（支持ID、app+stream、proxyKey等多种条件）
     * @param operationDesc 操作描述
     * @return 是否成功删除
     */
    public boolean deletePushProxy(PushProxyDTO pushProxyDTO, String operationDesc) {
        return executeWithOperationLogging(pushProxyDTO, operationDesc, () -> deleteOne(pushProxyDTO));
    }

    /**
     * 根据ID删除推流代理（兼容旧接口）
     * 使用标准带日志的删除方法实现
     *
     * @param id 代理ID
     * @param operationDesc 操作描述
     * @return 是否成功
     */
    public boolean deletePushProxyById(Long id, String operationDesc) {
        Assert.notNull(id, "代理ID不能为空");

        PushProxyDTO deleteDTO = new PushProxyDTO();
        deleteDTO.setId(id);
        return deletePushProxy(deleteDTO, operationDesc);
    }

    /**
     * 模板扩展方法：根据代理key删除推流代理
     * 使用通用操作日志记录模板，简化重复代码
     *
     * @param proxyKey 代理key（DB唯一键）
     * @param operationDesc 操作描述
     * @return 是否成功删除
     */
    public boolean deleteByProxyKey(String proxyKey, String operationDesc) {
        Assert.hasText(proxyKey, "代理key不能为空");

        // 构建删除条件（仅支持DB唯一键proxyKey）
        PushProxyDTO deleteDTO = new PushProxyDTO();
        deleteDTO.setProxyKey(proxyKey);

        return executeWithOperationLogging(deleteDTO, operationDesc, () -> deleteOne(deleteDTO));
    }

    /**
     * 模板扩展方法：批量删除推流代理
     * 使用通用批量操作日志记录模板，简化重复代码
     *
     * @param pushProxyDTO 删除条件（支持多种条件组合）
     * @param operationDesc 操作描述
     * @return 是否成功删除
     */
    public boolean deleteBatchPushProxy(PushProxyDTO pushProxyDTO, String operationDesc) {
        return executeWithOperationLogging(pushProxyDTO, operationDesc, () -> deleteBatch(pushProxyDTO));
    }

    /**
     * 模板扩展方法：添加推流代理
     * 使用通用批量操作日志记录模板，简化重复代码
     *
     * @param pushProxyDTO 删除条件（支持多种条件组合）
     * @param operationDesc 操作描述
     * @return 是否成功删除
     */
    public Long addPushProxy(PushProxyDTO pushProxyDTO, String operationDesc) {
        return executeWithOperationLogging(pushProxyDTO, operationDesc, () -> add(pushProxyDTO));
    }

    /**
     * 更新推流代理在线状态
     * 使用标准模板方法实现，底层已包含存在性检查
     * 
     * @param id 推流代理ID
     * @param onlineStatus 在线状态 1在线 0离线
     * @param operationDesc 操作描述
     * @return 更新结果
     */
    public boolean updatePushProxyOnlineStatus(Long id, Integer onlineStatus, String operationDesc) {
        Assert.notNull(id, "代理ID不能为空");
        Assert.notNull(onlineStatus, "在线状态不能为空");

        try {
            log.info("开始{} - 代理ID: {}, 在线状态: {}", operationDesc, id, onlineStatus);

            // 构建更新条件：仅包含ID和要更新的字段
            PushProxyDTO updateDTO = new PushProxyDTO();
            updateDTO.setId(id);
            updateDTO.setOnlineStatus(onlineStatus);

            // 使用带操作日志的更新模板方法，底层已包含存在性检查
            return updatePushProxy(updateDTO, operationDesc);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("{}失败 - 代理ID: {}, 在线状态: {}, 错误: {}", operationDesc, id, onlineStatus, e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.PUSH_PROXY_OPERATION_FAILED, e.getMessage());
        }
    }

    /**
     * 更新推流代理密钥
     * 使用标准模板方法实现，底层已包含存在性检查
     * 
     * @param id 推流代理ID
     * @param proxyKey 代理密钥
     * @param operationDesc 操作描述
     * @return 更新结果
     */
    public boolean updatePushProxyKey(Long id, String proxyKey, String operationDesc) {
        Assert.notNull(id, "代理ID不能为空");
        Assert.hasText(proxyKey, "代理密钥不能为空");

        try {
            log.info("开始{} - 代理ID: {}, 代理密钥: {}", operationDesc, id, proxyKey);

            // 构建更新条件：仅包含ID和要更新的字段
            PushProxyDTO updateDTO = new PushProxyDTO();
            updateDTO.setId(id);
            updateDTO.setProxyKey(proxyKey);

            // 使用带操作日志的更新模板方法，底层已包含存在性检查
            return updatePushProxy(updateDTO, operationDesc);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("{}失败 - 代理ID: {}, 代理密钥: {}, 错误: {}", operationDesc, id, proxyKey, e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.PUSH_PROXY_OPERATION_FAILED, e.getMessage());
        }
    }

}