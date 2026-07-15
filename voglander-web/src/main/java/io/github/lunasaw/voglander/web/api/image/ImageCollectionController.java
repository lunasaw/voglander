package io.github.lunasaw.voglander.web.api.image;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskCommandDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageCollectionEnrichedDTO;
import io.github.lunasaw.voglander.service.image.ImageCollectionApplicationService;
import io.github.lunasaw.voglander.service.image.ImageCollectionCreateCommand;
import io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties;
import io.github.lunasaw.voglander.web.api.image.assembler.ImageCollectionWebAssembler;
import io.github.lunasaw.voglander.web.api.image.req.ImageCollectionCreateReq;
import io.github.lunasaw.voglander.web.api.image.req.ImageCollectionQueryReq;
import io.github.lunasaw.voglander.web.api.image.req.ImageCollectionRescheduleReq;
import io.github.lunasaw.voglander.web.api.image.vo.ImageAssetConstraintsVO;
import io.github.lunasaw.voglander.web.api.image.vo.ImageCollectionCreateVO;
import io.github.lunasaw.voglander.web.api.image.vo.ImageCollectionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Image-specific task creation and enriched query facade. Generic controls stay in BusinessTaskController. */
@RestController
@ConditionalOnProperty(prefix = "voglander.image", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping(ApiConstant.API_INDEX_V1 + "/image-collection-tasks")
@Tag(name = "图像采集")
public class ImageCollectionController {
    private final ImageActorResolver actorResolver;
    private final ImageCollectionApplicationService applicationService;
    private final ImageCollectionWebAssembler assembler;
    private final ImageProperties properties;

    public ImageCollectionController(ImageActorResolver actorResolver,
        ImageCollectionApplicationService applicationService, ImageCollectionWebAssembler assembler,
        ImageProperties properties) {
        this.actorResolver = actorResolver; this.applicationService = applicationService; this.assembler = assembler; this.properties = properties;
    }

    @GetMapping("/constraints")
    @Operation(summary = "图像采集约束")
    public AjaxResult<Map<String, Object>> constraints(@RequestHeader("Authorization") String authorization) {
        UserDTO actor = actorResolver.resolve(authorization); actorResolver.require(actor, ImageConstant.PERMISSION_COLLECTION_QUERY);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("minIntervalSeconds", properties.getCollection().getMinIntervalSeconds());
        result.put("maxPlannedCount", properties.getCollection().getMaxPlannedCount());
        result.put("modes", new String[] {"ONCE", "SCHEDULED"});
        result.put("retentionPolicies", new String[] {ImageConstant.RETENTION_PERMANENT});
        return AjaxResult.success(result);
    }

    @PostMapping
    @Operation(summary = "创建图像采集任务")
    public AjaxResult<ImageCollectionCreateVO> create(@RequestHeader("Authorization") String authorization,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @RequestBody ImageCollectionCreateReq request) {
        UserDTO actor = actorResolver.resolve(authorization); actorResolver.require(actor, ImageConstant.PERMISSION_COLLECTION_CREATE);
        if (request == null || actor.getId() == null) throw new ServiceException(ServiceExceptionEnum.PARAM_ERROR);
        ImageCollectionCreateCommand command = new ImageCollectionCreateCommand(request.getTaskName(), request.getCollectionMode(),
            request.getDeviceId(), request.getChannelId(), request.scheduleStart(), request.scheduleEnd(), request.getIntervalSeconds(),
            request.getRetentionPolicy(), "USER", actor.getId().toString(), null, idempotencyKey);
        return AjaxResult.success(assembler.toCreateVO(applicationService.create(command)));
    }

    @PostMapping("/getPage")
    @Operation(summary = "图像采集任务分页")
    public AjaxResult<Map<String, Object>> page(@RequestHeader("Authorization") String authorization,
        @RequestBody(required = false) ImageCollectionQueryReq request,
        @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        UserDTO actor = actorResolver.resolve(authorization); actorResolver.require(actor, ImageConstant.PERMISSION_COLLECTION_QUERY);
        Page<ImageCollectionEnrichedDTO> source = applicationService.getEnrichedPage(request == null ? null : request.getTaskName(),
            request == null ? null : request.getCollectionMode(), request == null ? null : request.getState(),
            request == null ? null : request.getDeviceId(), request == null ? null : request.getChannelId(), null, null, page, size);
        List<ImageCollectionVO> items = new ArrayList<>(); for (ImageCollectionEnrichedDTO item : source.getRecords()) items.add(assembler.toVO(item));
        Map<String, Object> response = new LinkedHashMap<>(); response.put("total", source.getTotal()); response.put("items", items);
        return AjaxResult.success(response);
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "图像采集任务详情")
    public AjaxResult<ImageCollectionVO> detail(@RequestHeader("Authorization") String authorization, @PathVariable String taskId) {
        UserDTO actor = actorResolver.resolve(authorization); actorResolver.require(actor, ImageConstant.PERMISSION_COLLECTION_QUERY);
        ImageCollectionVO result = assembler.toVO(applicationService.getEnrichedDetail(taskId, null, null));
        if (result == null) throw new ServiceException(ServiceExceptionEnum.TASK_NOT_FOUND);
        return AjaxResult.success(result);
    }

    @PostMapping("/{taskId}:reschedule")
    @Operation(summary = "重排程图像采集任务")
    public AjaxResult<ImageCollectionVO> reschedule(@RequestHeader("Authorization") String authorization,
        @PathVariable String taskId, @RequestBody ImageCollectionRescheduleReq request) {
        UserDTO actor = actorResolver.resolve(authorization);
        actorResolver.require(actor, ImageConstant.PERMISSION_COLLECTION_CONTROL);
        if (request == null) throw new ServiceException(ServiceExceptionEnum.IMAGE_COLLECTION_SCHEDULE_INVALID);
        BizTaskCommandDTO command = new BizTaskCommandDTO();
        command.setTaskId(taskId); command.setExpectedVersion(request.getExpectedVersion());
        command.setScheduleStartTime(request.scheduleStart()); command.setScheduleEndTime(request.scheduleEnd());
        command.setIntervalSeconds(request.getIntervalSeconds()); command.setReason(request.getReason());
        command.setActorType("USER"); command.setActorId(actor.getId() == null ? null : actor.getId().toString());
        command.setRequestedAt(java.time.LocalDateTime.now());
        BizTaskDTO updated = applicationService.reschedule(command);
        ImageCollectionVO vo = assembler.toVO(applicationService.getEnrichedDetail(updated.getTaskId(), null, null));
        return AjaxResult.success(vo);
    }
}
