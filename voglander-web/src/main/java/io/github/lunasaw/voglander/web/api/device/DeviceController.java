package io.github.lunasaw.voglander.web.api.device;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.repository.domain.entity.DeviceDO;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.springframework.web.bind.annotation.*;
import io.github.lunasaw.voglander.manager.service.DeviceService;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.List;

import static io.github.lunasaw.voglander.common.domain.AjaxResult.success;

/**
 * (DeviceDO)表控制层
 *
 * @author chenzhangyue
 * @since 2023-12-28 11:11:54
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX + "/device")
@Slf4j
public class DeviceController {
    /**
     * 服务对象
     */
    @Resource
    private DeviceService deviceService;

    /**
     * 分页查询所有数据
     *
     * @param page   分页对象
     * @param device 查询实体
     * @return 所有数据
     */
    @GetMapping
    public AjaxResult selectAll(Page<DeviceDO> page, DeviceDO device) {
        return success(this.deviceService.page(page, new QueryWrapper<>(device)));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("{id}")
    public AjaxResult selectOne(@PathVariable Serializable id) {
        return success(this.deviceService.getById(id));
    }

    /**
     * 新增数据
     *
     * @param device 实体对象
     * @return 新增结果
     */
    @PostMapping
    public AjaxResult insert(@RequestBody DeviceDO device) {
        return success(this.deviceService.save(device));
    }

    /**
     * 修改数据
     *
     * @param device 实体对象
     * @return 修改结果
     */
    @PutMapping
    public AjaxResult update(@RequestBody DeviceDO device) {
        return success(this.deviceService.updateById(device));
    }

    /**
     * 删除数据
     *
     * @param idList 主键结合
     * @return 删除结果
     */
    @DeleteMapping
    public AjaxResult delete(@RequestParam("idList") List<Long> idList) {
        return success(this.deviceService.removeByIds(idList));
    }
}
