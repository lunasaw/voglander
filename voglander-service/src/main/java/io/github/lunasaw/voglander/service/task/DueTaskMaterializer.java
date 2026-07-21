package io.github.lunasaw.voglander.service.task;

import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;

/** Transactional callback that materializes the current persisted cursor of one due task. */
interface DueTaskMaterializer {

    void materialize(BizTaskDTO task);
}
