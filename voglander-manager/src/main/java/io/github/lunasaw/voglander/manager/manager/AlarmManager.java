package io.github.lunasaw.voglander.manager.manager;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.assembler.AlarmAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.AlarmDTO;
import io.github.lunasaw.voglander.manager.service.AlarmService;
import io.github.lunasaw.voglander.repository.entity.AlarmDO;
import lombok.extern.slf4j.Slf4j;

/**
 * 告警管理器。
 * <p>
 * 基于标准模板方法（add/update/updateById/get/deleteOne/deleteBatch/getPage）提供 CRUD，
 * 并暴露 {@link #ack(Long)} 告警确认业务方法。对外参数/返回均为 DTO。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Component
public class AlarmManager {

    @Autowired
    private AlarmService   alarmService;

    @Autowired
    private AlarmAssembler alarmAssembler;

    /**
     * 模板方法：新增告警。
     *
     * @param dto 告警DTO，deviceId 不能为空
     * @return 主键ID
     */
    public Long add(AlarmDTO dto) {
        Assert.notNull(dto, "告警信息不能为空");
        Assert.hasText(dto.getDeviceId(), "deviceId不能为空");

        AlarmDO alarmDO = alarmAssembler.dtoToDo(dto);
        LocalDateTime now = LocalDateTime.now();
        alarmDO.setCreateTime(now);
        alarmDO.setUpdateTime(now);
        if (alarmDO.getAlarmTime() == null) {
            alarmDO.setAlarmTime(now);
        }
        if (alarmDO.getAckStatus() == null) {
            alarmDO.setAckStatus(0);
        }

        boolean success = alarmService.save(alarmDO);
        if (!success) {
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION, "告警插入失败");
        }
        return alarmDO.getId();
    }

    /**
     * 扩展方法：通过ID更新指定字段。
     *
     * @param id 主键ID
     * @param updateDTO 更新内容
     * @return 主键ID
     */
    public Long updateById(Long id, AlarmDTO updateDTO) {
        Assert.notNull(id, "告警ID不能为空");
        Assert.notNull(updateDTO, "更新内容不能为空");

        AlarmDO existing = alarmService.getById(id);
        if (existing == null) {
            throw new ServiceException(ServiceExceptionEnum.DATA_NOT_EXISTS, "未找到要更新的告警: " + id);
        }
        return applyUpdate(existing, updateDTO);
    }

    /**
     * 模板方法：单条查询。
     *
     * @param dto 查询条件
     * @return 命中的告警DTO，未命中返回 null
     */
    public AlarmDTO get(AlarmDTO dto) {
        Assert.notNull(dto, "查询条件不能为空");
        AlarmDO existing = alarmService.getOne(buildQuery(dto).last("limit 1"));
        return alarmAssembler.doToDto(existing);
    }

    /**
     * 按主键ID查询。
     *
     * @param id 主键ID
     * @return 告警DTO，未命中返回 null
     */
    public AlarmDTO getById(Long id) {
        Assert.notNull(id, "告警ID不能为空");
        return alarmAssembler.doToDto(alarmService.getById(id));
    }

    /**
     * 模板方法：删除单条记录。
     *
     * @param dto 删除条件
     * @return 是否删除成功
     */
    public Boolean deleteOne(AlarmDTO dto) {
        Assert.notNull(dto, "删除条件不能为空");
        AlarmDO existing = alarmService.getOne(buildQuery(dto).last("limit 1"));
        if (existing == null) {
            return true;
        }
        return alarmService.removeById(existing.getId());
    }

    /**
     * 模板方法：批量删除。
     *
     * @param dto 删除条件
     * @return 是否删除成功
     */
    public Boolean deleteBatch(AlarmDTO dto) {
        Assert.notNull(dto, "删除条件不能为空");
        return alarmService.remove(buildQuery(dto));
    }

    /**
     * 模板方法：分页查询（默认按告警时间降序）。
     *
     * @param dto 查询条件
     * @param page 页码（从1开始）
     * @param size 页大小（1-1000）
     * @return 分页DTO结果
     */
    public Page<AlarmDTO> getPage(AlarmDTO dto, int page, int size) {
        if (page < 1) {
            throw new IllegalArgumentException("页码必须大于0");
        }
        if (size < 1 || size > 1000) {
            throw new IllegalArgumentException("页大小必须在1-1000之间");
        }

        LambdaQueryWrapper<AlarmDO> queryWrapper = buildQuery(dto)
            .orderByDesc(AlarmDO::getAlarmTime)
            .orderByDesc(AlarmDO::getCreateTime);

        Page<AlarmDO> doPage = alarmService.page(new Page<>(page, size), queryWrapper);

        Page<AlarmDTO> dtoPage = new Page<>(page, size);
        dtoPage.setTotal(doPage.getTotal());
        dtoPage.setPages(doPage.getPages());
        dtoPage.setCurrent(doPage.getCurrent());
        dtoPage.setSize(doPage.getSize());
        dtoPage.setRecords(alarmAssembler.doListToDtoList(doPage.getRecords()));
        return dtoPage;
    }

    /**
     * 告警确认：将 ack_status 置为 1。
     *
     * @param id 告警主键ID
     * @return 是否确认成功
     */
    public Boolean ack(Long id) {
        Assert.notNull(id, "告警ID不能为空");
        AlarmDO existing = alarmService.getById(id);
        if (existing == null) {
            throw new ServiceException(ServiceExceptionEnum.DATA_NOT_EXISTS, "未找到要确认的告警: " + id);
        }
        AlarmDTO update = new AlarmDTO();
        update.setAckStatus(1);
        applyUpdate(existing, update);
        return true;
    }

    // ================================
    // 私有辅助
    // ================================

    private Long applyUpdate(AlarmDO existing, AlarmDTO updateDTO) {
        if (updateDTO.getDeviceId() != null) {
            existing.setDeviceId(updateDTO.getDeviceId());
        }
        if (updateDTO.getChannelId() != null) {
            existing.setChannelId(updateDTO.getChannelId());
        }
        if (updateDTO.getAlarmType() != null) {
            existing.setAlarmType(updateDTO.getAlarmType());
        }
        if (updateDTO.getAlarmLevel() != null) {
            existing.setAlarmLevel(updateDTO.getAlarmLevel());
        }
        if (updateDTO.getAlarmTime() != null) {
            existing.setAlarmTime(updateDTO.getAlarmTime());
        }
        if (updateDTO.getDescription() != null) {
            existing.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getAckStatus() != null) {
            existing.setAckStatus(updateDTO.getAckStatus());
        }
        if (updateDTO.getExtend() != null) {
            existing.setExtend(updateDTO.getExtend());
        }
        existing.setUpdateTime(LocalDateTime.now());

        boolean success = alarmService.updateById(existing);
        if (!success) {
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION, "告警更新失败: id=" + existing.getId());
        }
        return existing.getId();
    }

    /**
     * 基于 DTO 构建查询条件（使用 condition 参数避免空值判断）。
     */
    private LambdaQueryWrapper<AlarmDO> buildQuery(AlarmDTO dto) {
        LambdaQueryWrapper<AlarmDO> queryWrapper = new LambdaQueryWrapper<>();
        if (dto == null) {
            return queryWrapper;
        }
        return queryWrapper
            .eq(dto.getId() != null, AlarmDO::getId, dto.getId())
            .eq(dto.getDeviceId() != null, AlarmDO::getDeviceId, dto.getDeviceId())
            .eq(dto.getChannelId() != null, AlarmDO::getChannelId, dto.getChannelId())
            .eq(dto.getAlarmType() != null, AlarmDO::getAlarmType, dto.getAlarmType())
            .eq(dto.getAlarmLevel() != null, AlarmDO::getAlarmLevel, dto.getAlarmLevel())
            .eq(dto.getAckStatus() != null, AlarmDO::getAckStatus, dto.getAckStatus());
    }

    /**
     * 分页查询并支持告警时间区间过滤（告警中心使用）。
     *
     * @param dto 等值查询条件
     * @param startTime 告警时间下界（含），可空
     * @param endTime 告警时间上界（含），可空
     * @param page 页码
     * @param size 页大小
     * @return 分页DTO结果
     */
    public Page<AlarmDTO> getPageWithTimeRange(AlarmDTO dto, LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
        if (page < 1) {
            throw new IllegalArgumentException("页码必须大于0");
        }
        if (size < 1 || size > 1000) {
            throw new IllegalArgumentException("页大小必须在1-1000之间");
        }
        LambdaQueryWrapper<AlarmDO> queryWrapper = buildQuery(dto)
            .ge(startTime != null, AlarmDO::getAlarmTime, startTime)
            .le(endTime != null, AlarmDO::getAlarmTime, endTime)
            .orderByDesc(AlarmDO::getAlarmTime)
            .orderByDesc(AlarmDO::getCreateTime);

        Page<AlarmDO> doPage = alarmService.page(new Page<>(page, size), queryWrapper);

        Page<AlarmDTO> dtoPage = new Page<>(page, size);
        dtoPage.setTotal(doPage.getTotal());
        dtoPage.setPages(doPage.getPages());
        dtoPage.setCurrent(doPage.getCurrent());
        dtoPage.setSize(doPage.getSize());
        dtoPage.setRecords(alarmAssembler.doListToDtoList(doPage.getRecords()));
        return dtoPage;
    }
}
