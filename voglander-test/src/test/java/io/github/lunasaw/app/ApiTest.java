package io.github.lunasaw.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.alibaba.fastjson.JSON;

import io.github.lunasaw.voglander.manager.manager.ExportTaskManager;
import io.github.lunasaw.voglander.repository.entity.ExportTaskDO;
import io.github.lunasaw.voglander.web.ApplicationWeb;

/**
 * @author luna
 * @version 1.0
 * @date 2023/12/3
 * @description:
 */
@SpringBootTest(classes = ApplicationWeb.class)
public class ApiTest {

    @Autowired
    private ExportTaskManager exportTaskManager;

    @Test
    public void atest() {
        ExportTaskDO taskDO = exportTaskManager.getById(null);
        System.out.println(JSON.toJSONString(taskDO));
    }

}
