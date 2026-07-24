package io.github.lunasaw.voglander.service.image;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lunasaw.voglander.client.domain.task.TaskCreateCommand;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageCollectionConfigDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskCreateResultDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.manager.ImageCollectionConfigManager;
import io.github.lunasaw.voglander.service.task.BizTaskCreateService;
import lombok.RequiredArgsConstructor;

/** Commits a collection task, its first execution and immutable camera config as one unit. */
@Service
@RequiredArgsConstructor
public class ImageCollectionCreateTransaction {

    private final BizTaskCreateService taskCreateService;
    private final ImageCollectionConfigManager configManager;

    @Transactional(rollbackFor = Exception.class)
    public BizTaskDTO create(TaskCreateCommand taskCommand, ImageCollectionConfigDTO config) {
        BizTaskCreateResultDTO result = taskCreateService.createResultByDatabaseArbitration(taskCommand);
        BizTaskDTO task = result.getAcceptedTask();
        if (!result.isCreated()) {
            requireHistoricalConfig(task);
            return task;
        }
        config.setTaskId(task.getTaskId());
        configManager.create(config);
        return task;
    }

    private void requireHistoricalConfig(BizTaskDTO task) {
        if (task == null || configManager.getByTaskId(task.getTaskId()) == null) {
            throw new IllegalStateException("accepted image collection task is missing its immutable config");
        }
    }
}
