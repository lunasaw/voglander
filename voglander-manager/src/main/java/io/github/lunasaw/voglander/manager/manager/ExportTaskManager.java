package io.github.lunasaw.voglander.manager.manager;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.manager.assembler.ExportTaskAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.ExportTaskDTO;
import io.github.lunasaw.voglander.manager.service.ExportTaskService;
import io.github.lunasaw.voglander.repository.entity.ExportTaskDO;

import lombok.extern.slf4j.Slf4j;

/**
 * 导出任务管理器
 *
 * @author chenzhangyue
 * @since 2024-01-30
 */
@Slf4j
@Component
public class ExportTaskManager {

    @Autowired
    private ExportTaskService exportTaskService;

    @Autowired
    private ExportTaskAssembler exportTaskAssembler;

    /**
     * 创建导出任务
     *
     * @param exportTaskDTO 导出任务DTO对象
     * @return 导出任务ID
     */
    public Long createExportTask(ExportTaskDTO exportTaskDTO) {
        Assert.notNull(exportTaskDTO, "exportTaskDTO can not be null");
        Assert.notNull(exportTaskDTO.getBizId(), "bizId can not be null");
        Assert.notNull(exportTaskDTO.getName(), "name can not be null");
        Assert.notNull(exportTaskDTO.getType(), "type can not be null");

        // 检查业务ID是否已存在
        ExportTaskDO existingTask = getByBizId(exportTaskDTO.getBizId());
        if (existingTask != null) {
            throw new RuntimeException("业务ID已存在: " + exportTaskDTO.getBizId());
        }

        // 使用 Assembler 转换并设置基础字段
        ExportTaskDO exportTaskDO = exportTaskAssembler.toCreateExportTaskDO(exportTaskDTO);

        // 保存到数据库
        exportTaskService.save(exportTaskDO);
        return exportTaskDO.getId();
    }

    /**
     * 批量创建导出任务
     *
     * @param exportTaskDTOList 导出任务DTO列表
     * @return 成功创建的数量
     */
    public int batchCreateExportTask(List<ExportTaskDTO> exportTaskDTOList) {
        if (exportTaskDTOList == null || exportTaskDTOList.isEmpty()) {
            return 0;
        }

        int successCount = 0;

        for (ExportTaskDTO exportTaskDTO : exportTaskDTOList) {
            try {
                // 检查必要字段
                Assert.notNull(exportTaskDTO.getBizId(), "bizId can not be null");
                Assert.notNull(exportTaskDTO.getName(), "name can not be null");
                Assert.notNull(exportTaskDTO.getType(), "type can not be null");

                // 检查业务ID是否已存在
                ExportTaskDO existingTask = getByBizId(exportTaskDTO.getBizId());
                if (existingTask != null) {
                    log.warn("业务ID已存在，跳过创建: {}", exportTaskDTO.getBizId());
                    continue;
                }

                // 使用 Assembler 转换并设置基础字段
                ExportTaskDO exportTaskDO = exportTaskAssembler.toCreateExportTaskDO(exportTaskDTO);

                // 保存到数据库
                if (exportTaskService.save(exportTaskDO)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("批量创建导出任务失败，bizId: {}, error: {}", exportTaskDTO.getBizId(), e.getMessage());
            }
        }

        return successCount;
    }

    /**
     * 更新导出任务
     *
     * @param exportTaskDTO 导出任务DTO对象
     * @return 更新后的导出任务ID
     */
    public Long updateExportTask(ExportTaskDTO exportTaskDTO) {
        Assert.notNull(exportTaskDTO, "exportTaskDTO can not be null");
        Assert.notNull(exportTaskDTO.getId(), "id can not be null");

        // 检查导出任务是否存在
        ExportTaskDTO existingTask = getExportTaskDTOById(exportTaskDTO.getId());
        if (existingTask == null) {
            throw new RuntimeException("导出任务不存在，ID: " + exportTaskDTO.getId());
        }

        // 使用 Assembler 转换并设置基础字段
        ExportTaskDO exportTaskDO = exportTaskAssembler.toUpdateExportTaskDO(exportTaskDTO);

        // 更新到数据库
        exportTaskService.updateById(exportTaskDO);
        return exportTaskDO.getId();
    }

    /**
     * 批量更新导出任务
     *
     * @param exportTaskDTOList 导出任务DTO列表
     * @return 成功更新的数量
     */
    public int batchUpdateExportTask(List<ExportTaskDTO> exportTaskDTOList) {
        if (exportTaskDTOList == null || exportTaskDTOList.isEmpty()) {
            return 0;
        }

        int successCount = 0;

        for (ExportTaskDTO exportTaskDTO : exportTaskDTOList) {
            try {
                // 检查必要字段
                Assert.notNull(exportTaskDTO.getId(), "id can not be null");

                // 检查导出任务是否存在
                ExportTaskDTO existingTask = getExportTaskDTOById(exportTaskDTO.getId());
                if (existingTask == null) {
                    log.warn("导出任务不存在，跳过更新: ID {}", exportTaskDTO.getId());
                    continue;
                }

                // 使用 Assembler 转换并设置基础字段
                ExportTaskDO exportTaskDO = exportTaskAssembler.toUpdateExportTaskDO(exportTaskDTO);

                // 更新到数据库
                if (exportTaskService.updateById(exportTaskDO)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("批量更新导出任务失败，ID: {}, error: {}", exportTaskDTO.getId(), e.getMessage());
            }
        }

        return successCount;
    }

    /**
     * 根据业务ID获取导出任务DO
     *
     * @param bizId 业务ID
     * @return ExportTaskDO
     */
    public ExportTaskDO getByBizId(Long bizId) {
        Assert.notNull(bizId, "bizId can not be null");
        QueryWrapper<ExportTaskDO> queryWrapper = new QueryWrapper<ExportTaskDO>().eq("biz_id", bizId);
        return exportTaskService.getOne(queryWrapper);
    }

    /**
     * 根据业务ID获取导出任务DTO
     *
     * @param bizId 业务ID
     * @return ExportTaskDTO
     */
    public ExportTaskDTO getDTOByBizId(Long bizId) {
        ExportTaskDO exportTaskDO = getByBizId(bizId);
        return exportTaskAssembler.toExportTaskDTO(exportTaskDO);
    }

    /**
     * 根据ID获取导出任务DTO
     *
     * @param id 导出任务主键ID
     * @return ExportTaskDTO
     */
    public ExportTaskDTO getExportTaskDTOById(Long id) {
        ExportTaskDO exportTaskDO = exportTaskService.getById(id);
        return exportTaskAssembler.toExportTaskDTO(exportTaskDO);
    }

    /**
     * 根据实体条件获取单个导出任务DTO
     *
     * @param exportTask 查询条件
     * @return ExportTaskDTO
     */
    public ExportTaskDTO getExportTaskDTOByEntity(ExportTaskDO exportTask) {
        QueryWrapper<ExportTaskDO> query = new QueryWrapper<>();
        if (exportTask.getBizId() != null) {
            query.eq("biz_id", exportTask.getBizId());
        }
        if (exportTask.getName() != null) {
            query.eq("name", exportTask.getName());
        }
        if (exportTask.getStatus() != null) {
            query.eq("status", exportTask.getStatus());
        }
        if (exportTask.getType() != null) {
            query.eq("type", exportTask.getType());
        }
        if (exportTask.getApplyUser() != null) {
            query.eq("apply_user", exportTask.getApplyUser());
        }

        ExportTaskDO exportTaskDO = exportTaskService.getOne(query);
        return exportTaskAssembler.toExportTaskDTO(exportTaskDO);
    }

    /**
     * 根据条件查询导出任务DTO列表
     *
     * @param exportTask 查询条件
     * @return ExportTaskDTO列表
     */
    public List<ExportTaskDTO> listExportTaskDTO(ExportTaskDO exportTask) {
        QueryWrapper<ExportTaskDO> query = new QueryWrapper<>();
        if (exportTask != null) {
            if (exportTask.getBizId() != null) {
                query.eq("biz_id", exportTask.getBizId());
            }
            if (exportTask.getName() != null) {
                query.like("name", exportTask.getName());
            }
            if (exportTask.getStatus() != null) {
                query.eq("status", exportTask.getStatus());
            }
            if (exportTask.getType() != null) {
                query.eq("type", exportTask.getType());
            }
            if (exportTask.getApplyUser() != null) {
                query.like("apply_user", exportTask.getApplyUser());
            }
            if (exportTask.getDeleted() != null) {
                query.eq("deleted", exportTask.getDeleted());
            }
            if (exportTask.getExpired() != null) {
                query.eq("expired", exportTask.getExpired());
            }
        }

        List<ExportTaskDO> exportTaskDOList = exportTaskService.list(query);
        return exportTaskAssembler.toExportTaskDTOList(exportTaskDOList);
    }

    /**
     * 简单分页查询导出任务DTO列表
     *
     * @param page 当前页
     * @param size 页大小
     * @return 分页结果
     */
    public Page<ExportTaskDTO> pageQuerySimple(int page, int size) {
        Page<ExportTaskDO> queryPage = new Page<>(page, size);
        Page<ExportTaskDO> pageInfo = exportTaskService.page(queryPage);

        // 使用 Assembler 进行数据转换
        Page<ExportTaskDTO> resultPage = new Page<>(page, size);
        resultPage.setRecords(exportTaskAssembler.toExportTaskDTOList(pageInfo.getRecords()));
        resultPage.setTotal(pageInfo.getTotal());
        resultPage.setCurrent(pageInfo.getCurrent());
        resultPage.setSize(pageInfo.getSize());
        resultPage.setPages(pageInfo.getPages());

        return resultPage;
    }

    /**
     * 分页查询导出任务列表，返回DTO模型
     *
     * @param page 当前页
     * @param size 页大小
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    public Page<ExportTaskDTO> pageQuery(int page, int size, QueryWrapper<ExportTaskDO> queryWrapper) {
        Page<ExportTaskDO> queryPage = new Page<>(page, size);
        Page<ExportTaskDO> pageInfo = exportTaskService.page(queryPage, queryWrapper);

        // 使用 Assembler 进行数据转换
        Page<ExportTaskDTO> resultPage = new Page<>(page, size);
        resultPage.setRecords(exportTaskAssembler.toExportTaskDTOList(pageInfo.getRecords()));
        resultPage.setTotal(pageInfo.getTotal());
        resultPage.setCurrent(pageInfo.getCurrent());
        resultPage.setSize(pageInfo.getSize());
        resultPage.setPages(pageInfo.getPages());

        return resultPage;
    }

    /**
     * 更新导出任务状态
     *
     * @param bizId 业务ID
     * @param status 状态
     */
    public void updateStatus(Long bizId, Integer status) {
        ExportTaskDO exportTaskDO = getByBizId(bizId);
        if (exportTaskDO == null) {
            return;
        }
        exportTaskDO.setStatus(status);
        exportTaskDO.setGmtUpdate(new Date());
        exportTaskService.updateById(exportTaskDO);
    }

    /**
     * 标记导出任务为已完成
     *
     * @param bizId 业务ID
     * @param url 下载地址
     */
    public void markAsCompleted(Long bizId, String url) {
        ExportTaskDO exportTaskDO = getByBizId(bizId);
        if (exportTaskDO == null) {
            return;
        }
        exportTaskDO.setStatus(1); // 已完成
        exportTaskDO.setUrl(url);
        exportTaskDO.setExportTime(new Date());
        exportTaskDO.setGmtUpdate(new Date());
        exportTaskService.updateById(exportTaskDO);
    }

    /**
     * 标记导出任务为失败
     *
     * @param bizId 业务ID
     */
    public void markAsError(Long bizId) {
        ExportTaskDO exportTaskDO = getByBizId(bizId);
        if (exportTaskDO == null) {
            return;
        }
        exportTaskDO.setStatus(-1); // 失败
        exportTaskDO.setGmtUpdate(new Date());
        exportTaskService.updateById(exportTaskDO);
    }

    /**
     * 删除导出任务
     *
     * @param bizId 业务ID
     * @return 删除结果
     */
    public Boolean deleteExportTask(Long bizId) {
        Assert.notNull(bizId, "bizId can not be null");
        QueryWrapper<ExportTaskDO> queryWrapper = new QueryWrapper<ExportTaskDO>().eq("biz_id", bizId);
        return exportTaskService.remove(queryWrapper);
    }
}
