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

import io.github.lunasaw.voglander.manager.assembler.StreamProxyAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.manager.service.StreamProxyService;
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;
import lombok.extern.slf4j.Slf4j;

/**
 * 拉流代理管理器
 * 负责处理拉流代理相关的复杂业务逻辑
 *
 * <p>
 * 架构设计：基于标准模板方法的高度复用设计
 * </p>
 * <ul>
 * <li>核心CRUD模板：{@link #add(StreamProxyDTO)} - 标准新增模板</li>
 * <li>智能更新模板：{@link #update(StreamProxyDTO)} - 支持ID和业务键的更新策略</li>
 * <li>类型安全查询：{@link #get(StreamProxyDTO)} - 基于LambdaQueryWrapper的查询模板</li>
 * <li>删除操作模板：{@link #deleteOne(StreamProxyDTO)} - 单条删除和批量删除模板</li>
 * <li>分页查询模板：{@link #getPage(StreamProxyDTO, int, int)} - 标准分页模板</li>
 * <li>统一缓存管理：{@link #clearCache(Long, String, String)} - 统一的缓存清理逻辑</li>
 * </ul>
 *
 * <p>
 * 模板扩展示例：
 * </p>
 * <ul>
 * <li>操作日志模板：{@link #updateStreamProxy(StreamProxyDTO, String)} - 带操作日志的业务方法模板</li>
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
public class StreamProxyManager {

    @Autowired
    private StreamProxyService   streamProxyService;

    @Autowired
    private StreamProxyAssembler streamProxyAssembler;

    @Autowired
    private CacheManager         cacheManager;

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
                Optional.ofNullable(cacheManager.getCache("streamProxy"))
                    .ifPresent(cache -> cache.evict(id));
                log.debug("清理ID缓存: {}", id);
            }

            // 根据旧业务键清理缓存
            if (oldKey != null) {
                Optional.ofNullable(cacheManager.getCache("streamProxy"))
                    .ifPresent(cache -> cache.evict("key:" + oldKey));
                log.debug("清理旧业务键缓存: {}", oldKey);
            }

            // 根据新业务键清理缓存（如果与旧键不同）
            if (newKey != null && !newKey.equals(oldKey)) {
                Optional.ofNullable(cacheManager.getCache("streamProxy"))
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
     * @param streamProxyDTO 操作的代理信息
     * @param operationDesc 操作描述
     * @param operation 具体的业务操作函数
     * @param <T> 操作返回值类型
     * @return 操作执行结果
     */
    private <T> T executeWithOperationLogging(StreamProxyDTO streamProxyDTO, String operationDesc,
        java.util.function.Supplier<T> operation) {
        try {
            // 操作日志：记录操作开始
            log.info("开始{} - 代理信息: ID={}, app={}, stream={}, proxyKey={}",
                operationDesc,
                streamProxyDTO.getId(),
                streamProxyDTO.getApp(),
                streamProxyDTO.getStream(),
                streamProxyDTO.getProxyKey());

            // 执行核心业务逻辑
            T result = operation.get();

            // 操作日志：记录操作成功结果
            log.info("完成{} - 操作成功，代理信息: ID={}, app={}, stream={}, proxyKey={}",
                operationDesc,
                streamProxyDTO.getId(),
                streamProxyDTO.getApp(),
                streamProxyDTO.getStream(),
                streamProxyDTO.getProxyKey());

            return result;

        } catch (Exception e) {
            // 操作日志：记录操作失败
            log.error("{}失败 - 代理信息: ID={}, app={}, stream={}, proxyKey={}, 错误: {}",
                operationDesc,
                streamProxyDTO.getId(),
                streamProxyDTO.getApp(),
                streamProxyDTO.getStream(),
                streamProxyDTO.getProxyKey(),
                e.getMessage(), e);

            // 重新抛出异常，保持原有的异常处理逻辑
            throw e;
        }
    }

    /**
     * 模板方法：新增数据
     * 标准的数据新增流程：校验参数 -> 转换DO -> 插入数据库 -> 返回ID
     *
     * @param streamProxyDTO 数据传输对象
     * @return 新增记录的ID
     * @throws IllegalArgumentException 当必要参数为空时
     * @throws RuntimeException 当数据库操作失败时
     */
    public Long add(StreamProxyDTO streamProxyDTO) {
        // 校验DB not null 参数
        Assert.notNull(streamProxyDTO, "代理信息不能为空");
        Assert.hasText(streamProxyDTO.getApp(), "应用名称不能为空");
        Assert.hasText(streamProxyDTO.getStream(), "流ID不能为空");
        Assert.hasText(streamProxyDTO.getUrl(), "拉流地址不能为空");

        try {
            log.info("开始新增流代理 - app: {}, stream: {}",
                streamProxyDTO.getApp(), streamProxyDTO.getStream());

            // 转为DO
            StreamProxyDO streamProxyDO = streamProxyAssembler.dtoToDo(streamProxyDTO);
            streamProxyDO.setCreateTime(LocalDateTime.now());
            streamProxyDO.setUpdateTime(LocalDateTime.now());

            // 插入DB
            boolean success = streamProxyService.save(streamProxyDO);
            if (!success) {
                throw new RuntimeException("数据库插入失败");
            }

            // 清理相关缓存
            clearCache(streamProxyDO.getId(), null, streamProxyDO.getProxyKey());

            log.info("新增流代理成功 - ID: {}, app: {}, stream: {}",
                streamProxyDO.getId(), streamProxyDTO.getApp(), streamProxyDTO.getStream());

            // 返回ID
            return streamProxyDO.getId();

        } catch (Exception e) {
            log.error("新增流代理失败 - app: {}, stream: {}, 错误: {}",
                streamProxyDTO.getApp(), streamProxyDTO.getStream(), e.getMessage(), e);
            throw new RuntimeException("新增流代理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 模板方法：更新数据
     * 标准的数据更新流程：校验参数 -> 转换DO -> 通过唯一索引更新DB -> 返回ID
     * 更新策略：优先使用ID，ID为null时使用唯一索引(app+stream)
     *
     * @param streamProxyDTO 数据传输对象
     * @return 更新记录的ID
     * @throws IllegalArgumentException 当必要参数为空时
     * @throws RuntimeException 当数据库操作失败时
     */
    public Long update(StreamProxyDTO streamProxyDTO) {
        // 校验DB not null 参数
        Assert.notNull(streamProxyDTO, "代理信息不能为空");

        try {
            log.info("开始更新流代理 - ID: {}, app: {}, stream: {}",
                streamProxyDTO.getId(), streamProxyDTO.getApp(), streamProxyDTO.getStream());

            // 转为DO
            StreamProxyDO streamProxyDO = streamProxyAssembler.dtoToDo(streamProxyDTO);
            streamProxyDO.setUpdateTime(LocalDateTime.now());

            String oldProxyKey = null;
            StreamProxyDO existingRecord = null;

            // 更新DB 通过唯一索引，优先使用ID，id为null时使用唯一索引
            if (streamProxyDO.getId() != null) {
                // 根据ID查询现有记录
                existingRecord = streamProxyService.getById(streamProxyDO.getId());
                if (existingRecord != null) {
                    oldProxyKey = existingRecord.getProxyKey();
                }
            } else if (streamProxyDTO.getApp() != null && streamProxyDTO.getStream() != null) {
                // 根据业务唯一键(app+stream)查询现有记录
                LambdaQueryWrapper<StreamProxyDO> queryWrapper = new LambdaQueryWrapper<>(streamProxyDO);
                queryWrapper.eq(StreamProxyDO::getApp, streamProxyDTO.getApp())
                    .eq(StreamProxyDO::getStream, streamProxyDTO.getStream());
                queryWrapper.last("limit 1");
                // 使用业务唯一键(app+stream)查询
                existingRecord = streamProxyService.getOne(queryWrapper);
                if (existingRecord != null) {
                    oldProxyKey = existingRecord.getProxyKey();
                    // 设置ID确保更新操作
                    streamProxyDO.setId(existingRecord.getId());
                    log.info("根据app+stream找到现有记录，设置ID: {} 进行更新", existingRecord.getId());
                }
            }

            if (existingRecord == null) {
                throw new RuntimeException("未找到要更新的记录");
            }

            // 执行更新操作
            boolean success = streamProxyService.updateById(streamProxyDO);
            if (!success) {
                throw new RuntimeException("数据库更新失败");
            }

            // 清理相关缓存
            clearCache(streamProxyDO.getId(), oldProxyKey, streamProxyDO.getProxyKey());

            log.info("更新流代理成功 - ID: {}, app: {}, stream: {}",
                streamProxyDO.getId(), streamProxyDTO.getApp(), streamProxyDTO.getStream());

            // 返回ID
            return streamProxyDO.getId();

        } catch (Exception e) {
            log.error("更新流代理失败 - ID: {}, app: {}, stream: {}, 错误: {}",
                streamProxyDTO.getId(), streamProxyDTO.getApp(), streamProxyDTO.getStream(), e.getMessage(), e);
            throw new RuntimeException("更新流代理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 模板方法：单条查询
     * 标准的数据查询流程：校验参数 -> 转换DO条件 -> 查询数据库 -> 转换并返回DTO
     *
     * @param streamProxyDTO 查询条件（支持ID、app+stream、proxyKey等条件）
     * @return 查询结果DTO，未找到时返回null
     * @throws IllegalArgumentException 当查询条件为空时
     */
    public StreamProxyDTO get(StreamProxyDTO streamProxyDTO) {
        // 校验DB not null 参数
        Assert.notNull(streamProxyDTO, "查询条件不能为空");

        try {
            log.debug("开始查询流代理 - ID: {}, app: {}, stream: {}, proxyKey: {}",
                streamProxyDTO.getId(), streamProxyDTO.getApp(),
                streamProxyDTO.getStream(), streamProxyDTO.getProxyKey());

            // 转为DO条件搜索，按优先级查询
            StreamProxyDO streamProxyDO = streamProxyAssembler.dtoToDo(streamProxyDTO);
            LambdaQueryWrapper<StreamProxyDO> queryWrapper = new LambdaQueryWrapper<>(streamProxyDO);
            queryWrapper.last("limit 1");
            StreamProxyDO existingRecord = streamProxyService.getOne(queryWrapper);
            if (existingRecord == null) {
                log.debug("未找到匹配的流代理记录");
                return null;
            }

            // 转换并返回DTO
            StreamProxyDTO resultDTO = streamProxyAssembler.doToDto(existingRecord);
            log.debug("查询流代理成功 - ID: {}", existingRecord.getId());

            return resultDTO;

        } catch (Exception e) {
            log.error("查询流代理失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("查询流代理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 模板方法：删除单条记录
     * 标准的单条删除流程：校验参数 -> 通过唯一索引查找 -> 删除数据库记录 -> 清理缓存
     * 删除策略：优先使用ID，其次使用proxyKey，最后使用app+stream
     *
     * @param streamProxyDTO 删除条件（必须包含唯一索引字段）
     * @return 删除结果，true表示删除成功，false表示未找到记录
     * @throws IllegalArgumentException 当删除条件为空或无效时
     * @throws RuntimeException 当数据库操作失败时
     */
    public Boolean deleteOne(StreamProxyDTO streamProxyDTO) {
        // 校验DB not null 参数
        Assert.notNull(streamProxyDTO, "删除条件不能为空");

        try {
            log.info("开始删除单条流代理 - ID: {}, proxyKey: {}, app: {}, stream: {}",
                streamProxyDTO.getId(), streamProxyDTO.getProxyKey(),
                streamProxyDTO.getApp(), streamProxyDTO.getStream());

            StreamProxyDO streamProxyDO = streamProxyAssembler.dtoToDo(streamProxyDTO);
            LambdaQueryWrapper<StreamProxyDO> queryWrapper = new LambdaQueryWrapper<>(streamProxyDO);
            queryWrapper.last("limit 1");
            StreamProxyDO existingRecord = streamProxyService.getOne(queryWrapper);
            if (existingRecord == null) {
                log.warn("未找到要删除的流代理记录 - ID: {}, proxyKey: {}, app: {}, stream: {}",
                    streamProxyDTO.getId(), streamProxyDTO.getProxyKey(),
                    streamProxyDTO.getApp(), streamProxyDTO.getStream());
                return false;
            }
            // 执行删除操作
            boolean success = streamProxyService.removeById(existingRecord);
            if (!success) {
                throw new RuntimeException("数据库删除失败");
            }

            // 清理相关缓存
            clearCache(existingRecord.getId(), existingRecord.getProxyKey(), null);

            log.info("删除单条流代理成功 - ID: {}, app: {}, stream: {}, proxyKey: {}",
                existingRecord.getId(), existingRecord.getApp(),
                existingRecord.getStream(), existingRecord.getProxyKey());

            return true;

        } catch (Exception e) {
            log.error("删除单条流代理失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("删除单条流代理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 模板方法：批量删除记录
     * 标准的批量删除流程：校验参数 -> 转换DO条件 -> 查询匹配记录 -> 批量删除 -> 清理缓存
     * 注意：批量删除可能删除多条记录，不校验必须使用唯一索引
     * 上游需要确认是使用 deleteOne 还是此方法
     *
     * @param streamProxyDTO 删除条件（支持多种条件组合）
     * @return 删除结果，true表示删除成功（包括删除0条记录），false表示操作失败
     * @throws IllegalArgumentException 当删除条件为空时
     * @throws RuntimeException 当数据库操作失败时
     */
    public Boolean deleteBatch(StreamProxyDTO streamProxyDTO) {
        // 校验DB not null 参数
        Assert.notNull(streamProxyDTO, "删除条件不能为空");

        try {
            log.info("开始批量删除流代理 - app: {}, stream: {}, status: {}, onlineStatus: {}, enabled: {}",
                streamProxyDTO.getApp(), streamProxyDTO.getStream(), streamProxyDTO.getStatus(),
                streamProxyDTO.getOnlineStatus(), streamProxyDTO.getEnabled());

            // 转为DO 批量删除 - 构建查询条件
            StreamProxyDO streamProxyDO = streamProxyAssembler.dtoToDo(streamProxyDTO);
            LambdaQueryWrapper<StreamProxyDO> queryWrapper = new LambdaQueryWrapper<>(streamProxyDO);

            // 先查询要删除的记录，用于清理缓存
            List<StreamProxyDO> toDeleteRecords = streamProxyService.list(queryWrapper);
            if (toDeleteRecords.isEmpty()) {
                log.info("未找到匹配的流代理记录，删除条件可能无匹配数据");
                return true;
            }

            log.info("找到{}条匹配记录，准备批量删除", toDeleteRecords.size());

            // 执行批量删除操作
            boolean success = streamProxyService.remove(queryWrapper);
            if (!success) {
                throw new RuntimeException("数据库批量删除失败");
            }

            // 批量清理相关缓存
            for (StreamProxyDO record : toDeleteRecords) {
                clearCache(record.getId(), record.getProxyKey(), null);
            }

            log.info("批量删除流代理成功 - 删除了{}条记录", toDeleteRecords.size());

            return true;

        } catch (Exception e) {
            log.error("批量删除流代理失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("批量删除流代理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 模板方法：分页查询
     * 标准的分页查询流程：校验参数 -> 转换DO条件 -> 分页查询数据库 -> 转换记录为DTO -> 返回Page<DTO>
     *
     * @param streamProxyDTO 查询条件（可为null表示查询所有）
     * @param page 页码（从1开始）
     * @param size 页大小
     * @return 分页结果Page<StreamProxyDTO>
     * @throws IllegalArgumentException 当分页参数无效时
     * @throws RuntimeException 当数据库操作失败时
     */
    public Page<StreamProxyDTO> getPage(StreamProxyDTO streamProxyDTO, int page, int size) {
        // 校验DB not null 参数
        if (page < 1) {
            throw new IllegalArgumentException("页码必须大于0");
        }
        if (size < 1 || size > 1000) {
            throw new IllegalArgumentException("页大小必须在1-1000之间");
        }

        try {
            log.debug("开始分页查询流代理 - page: {}, size: {}, 查询条件: {}", page, size,
                streamProxyDTO != null ? streamProxyDTO.toString() : "无条件查询");

            // 转为DO 条件分页查询 - 构建查询条件
            StreamProxyDO streamProxyDO = streamProxyAssembler.dtoToDo(streamProxyDTO);
            LambdaQueryWrapper<StreamProxyDO> queryWrapper = new LambdaQueryWrapper<>(streamProxyDO);

            // 默认按创建时间降序排列
            queryWrapper.orderByDesc(StreamProxyDO::getCreateTime);

            // 创建分页对象
            Page<StreamProxyDO> pageQuery = new Page<>(page, size);

            // 执行分页查询
            Page<StreamProxyDO> doPage = streamProxyService.page(pageQuery, queryWrapper);

            // 转换为DTO分页结果
            Page<StreamProxyDTO> dtoPage = new Page<>(page, size);
            dtoPage.setTotal(doPage.getTotal());
            dtoPage.setPages(doPage.getPages());
            dtoPage.setCurrent(doPage.getCurrent());
            dtoPage.setSize(doPage.getSize());

            // 获取record 转为 dto返回
            List<StreamProxyDTO> dtoRecords = streamProxyAssembler.doListToDtoList(doPage.getRecords());
            dtoPage.setRecords(dtoRecords);

            log.debug("分页查询流代理成功 - 总记录数: {}, 当前页: {}, 页大小: {}, 总页数: {}",
                doPage.getTotal(), doPage.getCurrent(), doPage.getSize(), doPage.getPages());

            return dtoPage;

        } catch (Exception e) {
            log.error("分页查询流代理失败 - page: {}, size: {}, 错误: {}", page, size, e.getMessage(), e);
            throw new RuntimeException("分页查询流代理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 模版方法必要的扩展，通过Id，或者一位索引查询
     * 
     * @param id
     * @return
     */
    public StreamProxyDTO getById(Long id) {
        Assert.notNull(id, "ID不能为空");
        StreamProxyDTO streamProxyDTO = new StreamProxyDTO();
        streamProxyDTO.setId(id);
        return get(streamProxyDTO);
    }

    /**
     * 创建拉流代理
     * 使用标准模板方法实现
     *
     * @param streamProxyDTO 代理信息
     * @return 代理ID
     */
    public Long createStreamProxy(StreamProxyDTO streamProxyDTO) {
        // 设置默认值
        if (streamProxyDTO.getEnabled() == null) {
            streamProxyDTO.setEnabled(true);
        }
        if (streamProxyDTO.getStatus() == null) {
            streamProxyDTO.setStatus(1); // 默认启用
        }
        if (streamProxyDTO.getOnlineStatus() == null) {
            streamProxyDTO.setOnlineStatus(0); // 默认离线
        }

        return add(streamProxyDTO);
    }

    /**
     * 模板扩展方法：带操作日志的更新拉流代理
     * 使用通用操作日志记录模板，简化重复代码
     *
     * @param streamProxyDTO 代理信息
     * @param operationDesc 操作描述
     * @return 是否更新成功
     */
    public Boolean updateStreamProxy(StreamProxyDTO streamProxyDTO, String operationDesc) {
        return executeWithOperationLogging(streamProxyDTO, operationDesc, () -> {
            Long result = update(streamProxyDTO);
            return result != null;
        });
    }

    /**
     * 分页查询拉流代理
     * 使用标准模板方法实现
     *
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    public Page<StreamProxyDTO> getProxyPage(int page, int size) {
        return getPage(null, page, size);
    }

    /**
     * 模板扩展方法：带操作日志的删除拉流代理
     * 使用通用操作日志记录模板，简化重复代码
     *
     * @param streamProxyDTO 删除条件（支持ID、app+stream、proxyKey等多种条件）
     * @param operationDesc 操作描述
     * @return 是否成功删除
     */
    public boolean deleteStreamProxy(StreamProxyDTO streamProxyDTO, String operationDesc) {
        return executeWithOperationLogging(streamProxyDTO, operationDesc, () -> deleteOne(streamProxyDTO));
    }

    /**
     * 根据ID删除拉流代理（兼容旧接口）
     * 使用标准带日志的删除方法实现
     *
     * @param id 代理ID
     * @param operationDesc 操作描述
     * @return 是否成功
     */
    public boolean deleteStreamProxyById(Long id, String operationDesc) {
        Assert.notNull(id, "代理ID不能为空");

        StreamProxyDTO deleteDTO = new StreamProxyDTO();
        deleteDTO.setId(id);
        return deleteStreamProxy(deleteDTO, operationDesc);
    }

    /**
     * 模板扩展方法：根据代理key删除拉流代理
     * 使用通用操作日志记录模板，简化重复代码
     *
     * @param proxyKey 代理key（DB唯一键）
     * @param operationDesc 操作描述
     * @return 是否成功删除
     */
    public boolean deleteByProxyKey(String proxyKey, String operationDesc) {
        Assert.hasText(proxyKey, "代理key不能为空");

        // 构建删除条件（仅支持DB唯一键proxyKey）
        StreamProxyDTO deleteDTO = new StreamProxyDTO();
        deleteDTO.setProxyKey(proxyKey);

        return executeWithOperationLogging(deleteDTO, operationDesc, () -> deleteOne(deleteDTO));
    }

    /**
     * 模板扩展方法：批量删除拉流代理
     * 使用通用批量操作日志记录模板，简化重复代码
     *
     * @param streamProxyDTO 删除条件（支持多种条件组合）
     * @param operationDesc 操作描述
     * @return 是否成功删除
     */
    public boolean deleteBatchStreamProxy(StreamProxyDTO streamProxyDTO, String operationDesc) {
        return executeWithOperationLogging(streamProxyDTO, operationDesc, () -> deleteBatch(streamProxyDTO));
    }

    /**
     * 模板扩展方法：添加拉流代理
     * 使用通用批量操作日志记录模板，简化重复代码
     *
     * @param streamProxyDTO 删除条件（支持多种条件组合）
     * @param operationDesc 操作描述
     * @return 是否成功删除
     */
    public Long addStreamProxy(StreamProxyDTO streamProxyDTO, String operationDesc) {
        return executeWithOperationLogging(streamProxyDTO, operationDesc, () -> add(streamProxyDTO));
    }

    /**
     * 更新流代理在线状态
     * 使用标准模板方法实现，底层已包含存在性检查
     * 
     * @param id 流代理ID
     * @param onlineStatus 在线状态 1在线 0离线
     * @param operationDesc 操作描述
     * @return 更新结果
     */
    public boolean updateStreamProxyOnlineStatus(Long id, Integer onlineStatus, String operationDesc) {
        Assert.notNull(id, "代理ID不能为空");
        Assert.notNull(onlineStatus, "在线状态不能为空");

        try {
            log.info("开始{} - 代理ID: {}, 在线状态: {}", operationDesc, id, onlineStatus);

            // 构建更新条件：仅包含ID和要更新的字段
            StreamProxyDTO updateDTO = new StreamProxyDTO();
            updateDTO.setId(id);
            updateDTO.setOnlineStatus(onlineStatus);

            // 使用带操作日志的更新模板方法，底层已包含存在性检查
            return updateStreamProxy(updateDTO, operationDesc);
        } catch (Exception e) {
            log.error("{}失败 - 代理ID: {}, 在线状态: {}, 错误: {}", operationDesc, id, onlineStatus, e.getMessage(), e);
            throw new RuntimeException(operationDesc + "失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新流代理密钥
     * 使用标准模板方法实现，底层已包含存在性检查
     * 
     * @param id 流代理ID
     * @param proxyKey 代理密钥
     * @param operationDesc 操作描述
     * @return 更新结果
     */
    public boolean updateStreamProxyKey(Long id, String proxyKey, String operationDesc) {
        Assert.notNull(id, "代理ID不能为空");
        Assert.hasText(proxyKey, "代理密钥不能为空");

        try {
            log.info("开始{} - 代理ID: {}, 代理密钥: {}", operationDesc, id, proxyKey);

            // 构建更新条件：仅包含ID和要更新的字段
            StreamProxyDTO updateDTO = new StreamProxyDTO();
            updateDTO.setId(id);
            updateDTO.setProxyKey(proxyKey);

            // 使用带操作日志的更新模板方法，底层已包含存在性检查
            return updateStreamProxy(updateDTO, operationDesc);
        } catch (Exception e) {
            log.error("{}失败 - 代理ID: {}, 代理密钥: {}, 错误: {}", operationDesc, id, proxyKey, e.getMessage(), e);
            throw new RuntimeException(operationDesc + "失败: " + e.getMessage(), e);
        }
    }

}