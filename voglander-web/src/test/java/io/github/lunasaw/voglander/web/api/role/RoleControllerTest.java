package io.github.lunasaw.voglander.web.api.role;

import io.github.lunasaw.voglander.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * 角色控制器测试
 *
 * @author luna
 */
@SpringBootTest
@AutoConfigureWebMvc
public class RoleControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetRoleList() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/system/role/list")
            .param("pageNum", "1")
            .param("pageSize", "10")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(0));
    }

    @Test
    public void testCreateRole() throws Exception {
        String requestBody = """
            {
                "name": "测试角色",
                "remark": "测试角色描述",
                "status": 1
            }
            """;

        mockMvc.perform(MockMvcRequestBuilders.post("/system/role")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(0));
    }
}