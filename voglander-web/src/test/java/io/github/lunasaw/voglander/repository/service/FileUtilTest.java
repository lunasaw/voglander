package io.github.lunasaw.voglander.repository.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson2.JSON;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.lunasaw.voglander.web.ApplicationWeb;
import lombok.extern.slf4j.Slf4j;

/**
 * @author luna
 * @date 2024/3/17
 */

@Slf4j
@SpringBootTest(classes = ApplicationWeb.class)
public class FileUtilTest {

    public static void main(String[] args) throws IOException {
        List<Path> collect = Files.list(Paths.get("/Users/weidian/Downloads/2月出差/3月出差")).collect(Collectors.toList());
        System.out.println(JSON.toJSONString(collect));

        List<String> collect1 = collect.stream().map(e -> {
            Path fileName = e.getFileName();
            return fileName.toString();
        }).collect(Collectors.toList());
        System.out.println(collect1);
    }
}
