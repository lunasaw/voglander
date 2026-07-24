package io.github.lunasaw.voglander.web.api.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageCollectionConfigDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageCollectionEnrichedDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.web.api.image.assembler.ImageCollectionWebAssembler;
import io.github.lunasaw.voglander.web.api.image.vo.ImageCollectionVO;

class ImageCollectionWebAssemblerTest {

    private final ImageCollectionWebAssembler assembler = new ImageCollectionWebAssembler();

    @Test
    void collectionVo_shouldExposeTaskVersionCapabilitiesAndSanitizedResultFacts() {
        BizTaskDTO task = new BizTaskDTO();
        task.setTaskId("btask-1");
        task.setVersion(8);
        task.setScheduleVersion(3);
        task.setLastExecutionId("bexec-1");
        task.setResultRefType("IMAGE_ASSET");
        task.setResultRefId("img-1");
        task.setResultSummary("{\"message\":\"done\",\"path\":\"/private/a.jpg\",\"stack\":\"secret\"}");
        task.setCancelledCount(2);
        task.setProgressMessage("capturing");
        task.setProgressRevision(9L);
        ImageCollectionConfigDTO config = new ImageCollectionConfigDTO();
        config.setTaskId("btask-1");
        config.setDeviceId("device-1");
        config.setChannelId("channel-1");
        ImageCollectionEnrichedDTO source = new ImageCollectionEnrichedDTO();
        source.setTask(task);
        source.setConfig(config);
        source.setCapabilities(Arrays.asList("PAUSE", "RESCHEDULE"));

        ImageCollectionVO result = assembler.toVO(source);

        assertEquals(8, result.getVersion());
        assertEquals(3, result.getScheduleVersion());
        assertEquals(Arrays.asList("PAUSE", "RESCHEDULE"), result.getCapabilities());
        assertEquals("bexec-1", result.getLastExecutionId());
        assertEquals("IMAGE_ASSET", result.getResultRefType());
        assertEquals("img-1", result.getResultRefId());
        assertEquals(2, result.getCancelledCount());
        assertEquals("capturing", result.getProgressMessage());
        assertEquals(9L, result.getProgressRevision());
        assertFalse(result.getResultSummary().contains("path"));
        assertFalse(result.getResultSummary().contains("stack"));
    }
}
