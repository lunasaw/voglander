package io.github.lunasaw.voglander.web.api.export;

import java.io.Serializable;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.service.ExportTaskService;
import io.github.lunasaw.voglander.repository.entity.ExportTaskDO;

/**
 * 报表导出(ExportTaskDO)表控制层
 *
 * @author chenzhangyue
 * @since 2024-01-29 22:49:50
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX + "/export")
public class ExportTaskDOController {
    /**
     * 服务对象
     */
    @Resource
    private ExportTaskService exportTaskService;

    /**
     * 分页查询所有数据
     *
     * @param page 分页对象
     * @param exportTaskDO 查询实体
     * @return 所有数据
     */
    @GetMapping
    public AjaxResult selectAll(Page<ExportTaskDO> page, ExportTaskDO exportTaskDO) {
        return AjaxResult.success(this.exportTaskService.page(page, new QueryWrapper<>(exportTaskDO)));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("{id}")
    public AjaxResult selectOne(@PathVariable Serializable id) {
        return AjaxResult.success(this.exportTaskService.getById(id));
    }

    /**
     * 新增数据
     *
     * @param exportTaskDO 实体对象
     * @return 新增结果
     */
    @PostMapping
    public AjaxResult insert(@RequestBody ExportTaskDO exportTaskDO) {
        return AjaxResult.success(this.exportTaskService.save(exportTaskDO));
    }

    /**
     * 修改数据
     *
     * @param exportTaskDO 实体对象
     * @return 修改结果
     */
    @PutMapping
    public AjaxResult update(@RequestBody ExportTaskDO exportTaskDO) {
        return AjaxResult.success(this.exportTaskService.updateById(exportTaskDO));
    }

    /**
     * 删除数据
     *
     * @param idList 主键结合
     * @return 删除结果
     */
    @DeleteMapping
    public AjaxResult delete(@RequestParam("idList") List<Long> idList) {
        return AjaxResult.success(this.exportTaskService.removeByIds(idList));
    }
}
