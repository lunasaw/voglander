package io.github.lunasaw.voglander.repository.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;

@DisplayName("Removed ExportTask routes")
class LegacyExportTaskRouteNegativeTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RemainingRouteProbe()).build();
    }

    static Stream<Arguments> removedRoutes() {
        return Stream.of(
            Arguments.of("GET", "/api/v1/exportTask/get/1"),
            Arguments.of("GET", "/api/v1/exportTask/get"),
            Arguments.of("GET", "/api/v1/exportTask/getBizId/1"),
            Arguments.of("GET", "/api/v1/exportTask/list"),
            Arguments.of("GET", "/api/v1/exportTask/pageListByEntity/1/10"),
            Arguments.of("GET", "/api/v1/exportTask/pageList/1/10"),
            Arguments.of("POST", "/api/v1/exportTask/insert"),
            Arguments.of("POST", "/api/v1/exportTask/insertBatch"),
            Arguments.of("PUT", "/api/v1/exportTask/update"),
            Arguments.of("PUT", "/api/v1/exportTask/updateBatch"),
            Arguments.of("PUT", "/api/v1/exportTask/updateStatus/1/1"),
            Arguments.of("PUT", "/api/v1/exportTask/markCompleted/1"),
            Arguments.of("PUT", "/api/v1/exportTask/markError/1"),
            Arguments.of("DELETE", "/api/v1/exportTask/delete/1"),
            Arguments.of("DELETE", "/api/v1/exportTask/deleteBizId/1"),
            Arguments.of("DELETE", "/api/v1/exportTask/deleteIds"),
            Arguments.of("GET", "/api/v1/exportTask/count"),
            Arguments.of("GET", "/api/v1/exportTask/countByEntity"));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("removedRoutes")
    @DisplayName("every former ExportTask route returns HTTP 404")
    void removedRoute_shouldReturnNotFound(String method, String path) throws Exception {
        var request = switch (method) {
            case "POST" -> post(path).contentType(MediaType.APPLICATION_JSON).content("{}");
            case "PUT" -> put(path).contentType(MediaType.APPLICATION_JSON).content("{}");
            case "DELETE" -> delete(path);
            default -> get(path);
        };
        mockMvc.perform(request).andExpect(status().isNotFound());
    }

    private static class RemainingRouteProbe {
        @GetMapping("/api/v1/health")
        public String health() {
            return "UP";
        }
    }
}
