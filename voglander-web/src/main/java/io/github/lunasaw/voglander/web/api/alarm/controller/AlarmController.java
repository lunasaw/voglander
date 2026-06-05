package io.github.lunasaw.voglander.web.api.alarm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.domaon.dto.AlarmDTO;
import io.github.lunasaw.voglander.manager.manager.AlarmManager;
import io.github.lunasaw.voglander.web.api.alarm.assembler.AlarmWebAssembler;
import io.github.lunasaw.voglander.web.api.alarm.domain.AlarmListResp;
import io.github.lunasaw.voglander.web.api.alarm.domain.AlarmQueryReq;
import io.github.lunasaw.voglander.web.api.alarm.domain.AlarmVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/alarm")
@Tag(name = "告警管理")
public class AlarmController {

    @Autowired private AlarmManager      alarmManager;
    @Autowired private AlarmWebAssembler alarmWebAssembler;

    @PostMapping("/getPage")
    public AjaxResult<AlarmListResp> getPage(@Valid @RequestBody AlarmQueryReq req) {
        AlarmDTO dto = alarmWebAssembler.queryReqToDto(req);
        Page<AlarmDTO> page = alarmManager.getPageWithTimeRange(
            dto,
            alarmWebAssembler.parseTime(req.getStartTime()),
            alarmWebAssembler.parseTime(req.getEndTime()),
            req.getPage(), req.getSize());
        return AjaxResult.success(alarmWebAssembler.toListResp(page));
    }

    @GetMapping("/get/{id}")
    public AjaxResult<AlarmVO> get(@PathVariable Long id) {
        return AjaxResult.success(alarmWebAssembler.dtoToVo(alarmManager.getById(id)));
    }

    @PostMapping("/ack")
    public AjaxResult<Boolean> ack(@RequestBody Map<String, Long> body) {
        return AjaxResult.success(alarmManager.ack(body.get("id")));
    }
}
