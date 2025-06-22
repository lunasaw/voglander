package io.github.lunasaw.voglander.client.domain.excel.dto;

import java.io.Serializable;
import java.util.Map;

import lombok.Data;

/**
 * @author luna
 */
@Data

public class GeneTempDTO implements Serializable {


    private Map<String, String> readSetMap;


    private Map<String, String> exampleMap;


    private String              explainStr;


    private String              filePath;
}
